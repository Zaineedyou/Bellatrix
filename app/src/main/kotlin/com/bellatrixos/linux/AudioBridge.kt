package com.bellatrixos.linux

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Process
import android.os.SystemClock
import android.util.Log
import java.io.File
import java.io.IOException
import kotlin.concurrent.thread

private const val TAG = "audio"
private const val RATE = 48000
private const val FRAME_BYTES = 4 // s16le stereo

/** How much the track holds before it underruns; more rides out the guest stalling for a moment (proot is slow), at the cost of delay. */
private const val TRACK_BUFFER_MS = 150

/** Restarts of a crashing PulseAudio back off up to this. */
private const val MAX_BACKOFF_MS = 60_000L

/**
 * Plays guest audio: PulseAudio in the guest renders into a FIFO in the shared
 * runtime dir (module-pipe-sink) and we copy that stream into an AudioTrack.
 * Blocking AudioTrack writes pace the pipe, and so PulseAudio's clock.
 */
class AudioBridge(
    private val proot: Proot,
) {
    private val fifo = File(proot.runtimeDir, "audio.fifo")
    private val runtimeDir = Proot.GUEST_RUNTIME_DIR

    /** PulseAudio config: only the socket clients use and the pipe sink we read. */
    private val config =
        """
        load-module module-native-protocol-unix auth-anonymous=1 socket=$runtimeDir/pulse/native
        load-module module-pipe-sink file=$runtimeDir/${fifo.name} format=s16le rate=$RATE channels=2 sink_name=android sink_properties=device.description=Android
        load-module module-suspend-on-idle timeout=1
        set-default-sink android
        """.trimIndent() + "\n"

    private val lock = Object()
    private var playing = false

    @Volatile
    private var lastData = 0L

    private val track by lazy {
        val minBuffer = AudioTrack.getMinBufferSize(RATE, AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_16BIT)
        AudioTrack
            .Builder()
            .setAudioAttributes(
                AudioAttributes
                    .Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build(),
            ).setAudioFormat(
                AudioFormat
                    .Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(RATE)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                    .build(),
            ).setBufferSizeInBytes(maxOf(minBuffer, RATE * FRAME_BYTES * TRACK_BUFFER_MS / 1000))
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()
    }

    /** Whether PulseAudio should run; off while Linux is being removed. */
    @Volatile
    private var running = false
    private var started = false

    /** Starts the bridge, or resumes it after [stop]. */
    fun start() {
        running = true
        if (started) return
        started = true
        proot.hostFile("/tmp/pulse.pa").writeText(config)
        File(proot.runtimeDir, "pulse").mkdirs()
        thread(name = "pulseaudio", isDaemon = true) { superviseDaemon() }
        thread(name = "audio-bridge", isDaemon = true) { pump() }
        thread(name = "audio-idle", isDaemon = true) { pauseWhenIdle() }
    }

    /** Stops restarting PulseAudio; the caller ends the running one along with the other guest processes. */
    fun stop() {
        running = false
    }

    /** Keeps PulseAudio running; it runs as the (fake) root under proot, which it warns about harmlessly. */
    private fun superviseDaemon() {
        var backoff = 1000L
        while (true) {
            while (!running) Thread.sleep(1000)
            val started = SystemClock.elapsedRealtime()
            val code =
                runCatching {
                    proot.run("exec pulseaudio -n -F /tmp/pulse.pa --daemonize=no --use-pid-file=no --exit-idle-time=-1 --disable-shm=yes") {
                        Log.i(TAG, "[pulseaudio] $it")
                    }
                }.getOrElse { -1 }
            if (SystemClock.elapsedRealtime() - started > MAX_BACKOFF_MS) backoff = 1000L
            Log.w(TAG, "pulseaudio exited $code; restarting in ${backoff / 1000} s")
            Thread.sleep(backoff)
            backoff = (backoff * 2).coerceAtMost(MAX_BACKOFF_MS)
        }
    }

    /**
     * Copies the sink's FIFO into the track; PulseAudio (re)creates the FIFO when it loads the sink. A read can stop
     * inside a frame: PulseAudio's writes to a pipe that is nearly full are partial, and it carries on from the byte it
     * reached. Such a read's last bytes are kept for the next one and written with it, as dropping them would put every
     * later sample out of step with its channel, which sounds like crackle and noise.
     */
    private fun pump() {
        runCatching { Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_AUDIO) }
        val buf = ByteArray(RATE * FRAME_BYTES / 50) // 20 ms
        while (true) {
            if (!fifo.exists()) {
                Thread.sleep(500)
                continue
            }
            try {
                // Opening a FIFO for reading blocks until PulseAudio opens it for writing.
                fifo.inputStream().use { input ->
                    // Bytes at the start of buf that end a frame the last read began.
                    var held = 0
                    while (true) {
                        val n = input.read(buf, held, buf.size - held)
                        if (n < 0) break
                        if (n == 0) continue
                        lastData = SystemClock.uptimeMillis()
                        synchronized(lock) {
                            if (!playing) {
                                track.play()
                                playing = true
                                lock.notify()
                            }
                        }
                        val total = held + n
                        val whole = total - total % FRAME_BYTES
                        writeFully(buf, whole)
                        held = total - whole
                        if (held > 0) System.arraycopy(buf, whole, buf, 0, held)
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "audio pipe failed", e)
                Thread.sleep(1000)
            }
        }
    }

    /** Writes the first [length] bytes of [buf] to the track, all of them: a blocking write can still come back short. */
    private fun writeFully(
        buf: ByteArray,
        length: Int,
    ) {
        var offset = 0
        while (offset < length) {
            val written = track.write(buf, offset, length - offset)
            if (written < 0) throw IOException("AudioTrack.write returned $written")
            if (written == 0) Thread.sleep(2)
            offset += written
        }
    }

    /** Releases the audio output when the guest goes quiet (the sink suspends and stops writing); sleeps while paused. */
    private fun pauseWhenIdle() {
        while (true) {
            synchronized(lock) { while (!playing) lock.wait() }
            Thread.sleep(500)
            synchronized(lock) {
                if (playing && SystemClock.uptimeMillis() - lastData > 1000) {
                    track.pause()
                    playing = false
                }
            }
        }
    }
}
