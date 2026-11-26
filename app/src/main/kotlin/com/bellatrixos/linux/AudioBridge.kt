package com.bellatrixos.linux

import android.Manifest
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.os.Build
import android.os.Process
import android.os.SystemClock
import android.system.Os
import android.util.Log
import com.bellatrixos.system.hasPermission
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import kotlin.concurrent.thread

private const val TAG = "audio"
private const val RATE = 48000
private const val FRAME_BYTES = 4 // s16le stereo

/** How much the track holds before it underruns; more rides out the guest stalling for a moment (proot is slow), at the cost of delay. */
private const val TRACK_BUFFER_MS = 150

/** 20 ms of the microphone: mono, s16le. */
private const val MIC_BLOCK = RATE * 2 / 50

/** This much unread in the microphone's pipe means nothing reads it any more. */
private const val MIC_STALL_BYTES = MIC_BLOCK * 4

/** How often an idle microphone looks for something in Linux that wants it. */
private const val MIC_POLL_MS = 100L

/** Where Linux can read what the microphone is doing: `cat /tmp/mic-status.txt`. */
private const val MIC_STATUS_FILE = "/tmp/mic-status.txt"

/** How often what the pump saw is written to [STATS_FILE]. */
private const val STATS_MS = 2000L

/** Where Linux can read it: `cat /tmp/audio-stats.txt`. */
private const val STATS_FILE = "/tmp/audio-stats.txt"

/** Restarts of a crashing PulseAudio back off up to this. */
private const val MAX_BACKOFF_MS = 60_000L

/**
 * Plays guest audio: PulseAudio in the guest renders into a FIFO in the shared
 * runtime dir (module-pipe-sink) and we copy that stream into an AudioTrack.
 * Blocking AudioTrack writes pace the pipe, and so PulseAudio's clock. The other way, the phone's microphone is
 * PulseAudio's default source (module-pipe-source), fed from an AudioRecord only while something in Linux records.
 */
class AudioBridge(
    private val proot: Proot,
    private val context: Context,
) {
    private val fifo = File(proot.runtimeDir, "audio.fifo")
    private val micFifo = File(proot.runtimeDir, "mic.fifo")
    private val runtimeDir = Proot.GUEST_RUNTIME_DIR

    /** PulseAudio config: only the socket clients use and the pipe sink we read. */
    private val config =
        """
        load-module module-native-protocol-unix auth-anonymous=1 socket=$runtimeDir/pulse/native
        load-module module-pipe-sink file=$runtimeDir/${fifo.name} format=s16le rate=$RATE channels=2 sink_name=android sink_properties=device.description=Android
        load-module module-suspend-on-idle timeout=1
        set-default-sink android
        load-module module-pipe-source file=$runtimeDir/${micFifo.name} format=s16le rate=$RATE channels=1 source_name=android_mic source_properties=device.description=Microphone
        set-default-source android_mic
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
        thread(name = "audio-mic", isDaemon = true) { mic() }
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
        val stats = Stats()
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
                        val backlog = input.available()
                        val beforeRead = System.nanoTime()
                        val n = input.read(buf, held, buf.size - held)
                        val afterRead = System.nanoTime()
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
                        stats.add(n, backlog, afterRead - beforeRead, System.nanoTime() - afterRead)
                        if (stats.due()) report(stats)
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "audio pipe failed", e)
                Thread.sleep(1000)
            }
        }
    }

    /**
     * What the pump saw since the last report, for finding where sound breaks up: a pipe that runs empty and reads that
     * wait long mean PulseAudio (or the app feeding it) is late; the track underrunning means the pump is.
     */
    private class Stats {
        private var since = System.nanoTime()
        private var bytes = 0L
        private var reads = 0
        private var backlogMin = Int.MAX_VALUE
        private var backlogMax = 0
        private var waitMaxNs = 0L
        private var writeMaxNs = 0L
        private var lastUnderruns = 0
        private var lastAdd = since

        fun add(
            n: Int,
            backlog: Int,
            waitNs: Long,
            writeNs: Long,
        ) {
            val now = System.nanoTime()
            // Sound after a silence: the window starts here, or it would count the silence as slow.
            if (now - lastAdd > 500_000_000L) reset(now)
            lastAdd = now
            bytes += n
            reads++
            if (backlog < backlogMin) backlogMin = backlog
            if (backlog > backlogMax) backlogMax = backlog
            if (waitNs > waitMaxNs) waitMaxNs = waitNs
            if (writeNs > writeMaxNs) writeMaxNs = writeNs
        }

        fun due() = System.nanoTime() - since >= STATS_MS * 1_000_000

        /** The report's line, and the window starts over. */
        fun take(underruns: Int): String {
            val now = System.nanoTime()
            val seconds = (now - since) / 1e9
            val line =
                "last %.1f s: %.0f%% of real time, %d reads, pipe backlog %d..%d B (full is 65536), " +
                    "waited on PulseAudio up to %d ms, track full up to %d ms, track underruns +%d (total %d)"
            val text =
                line.format(
                    seconds,
                    bytes / (seconds * RATE * FRAME_BYTES) * 100,
                    reads,
                    if (backlogMin == Int.MAX_VALUE) 0 else backlogMin,
                    backlogMax,
                    waitMaxNs / 1_000_000,
                    writeMaxNs / 1_000_000,
                    underruns - lastUnderruns,
                    underruns,
                )
            reset(now)
            lastUnderruns = underruns
            return text
        }

        private fun reset(now: Long) {
            since = now
            bytes = 0
            reads = 0
            backlogMin = Int.MAX_VALUE
            backlogMax = 0
            waitMaxNs = 0
            writeMaxNs = 0
        }
    }

    /** Writes [stats]' window to [STATS_FILE] (and the log if something looked wrong). */
    private fun report(stats: Stats) {
        // Underruns are counted from API 24.
        val underruns = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) track.underrunCount else 0
        val text = stats.take(underruns)
        runCatching { proot.hostFile(STATS_FILE).writeText("audio bridge, track ${TRACK_BUFFER_MS} ms: $text\n") }
        if (Regex("underruns \\+[1-9]").containsMatchIn(text)) Log.w(TAG, text)
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

    private var micShown = ""

    /** Says what the microphone is doing in [MIC_STATUS_FILE], when that changes. */
    private fun micStatus(text: String) {
        if (text == micShown) return
        micShown = text
        runCatching { proot.hostFile(MIC_STATUS_FILE).writeText("microphone: $text\n") }
    }

    /** Keeps [serveMic] going: PulseAudio makes the FIFO again when it restarts, and it may fail now and then. */
    private fun mic() {
        runCatching { Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_AUDIO) }
        while (true) {
            if (!running || !micFifo.exists()) {
                micStatus("off")
                Thread.sleep(500)
                continue
            }
            try {
                serveMic()
            } catch (e: Exception) {
                Log.w(TAG, "microphone failed", e)
                micStatus("failed: ${e.message}")
                Thread.sleep(1000)
            }
        }
    }

    /**
     * Waits for something in Linux to record: PulseAudio reads the microphone's FIFO only while its source runs, which
     * it does while a client records from it. So a block of silence is left in the FIFO, and when it has been read
     * the source is wanted; the microphone then runs until the FIFO stops being read.
     */
    private fun serveMic() {
        val inode = Os.stat(micFifo.path).st_ino
        // PulseAudio holds the FIFO open for reading and writing, so neither open waits for the other end.
        FileInputStream(micFifo).use { probe ->
            FileOutputStream(micFifo).use { out ->
                var primed = false
                micStatus("idle")
                while (running) {
                    // PulseAudio started again: this FIFO isn't the one it reads.
                    if (!micFifo.exists() || Os.stat(micFifo.path).st_ino != inode) return
                    if (!primed) {
                        out.write(ByteArray(MIC_BLOCK))
                        primed = true
                    } else if (probe.available() == 0) {
                        primed = false
                        feedMic(probe, out)
                        micStatus("idle")
                    }
                    Thread.sleep(MIC_POLL_MS)
                }
            }
        }
    }

    /**
     * Copies the microphone into [out], the FIFO, until nothing reads it ([probe] sees what is waiting in it). Without
     * the microphone permission it is silence at the same pace, so the app recording carries on; allowing it is for the
     * launcher's settings (permissions), as nothing here asks.
     */
    private fun feedMic(
        probe: FileInputStream,
        out: FileOutputStream,
    ) {
        val recorder = if (context.hasPermission(Manifest.permission.RECORD_AUDIO)) startRecorder() else null
        micStatus(if (recorder != null) "recording" else "wanted by Linux, but the launcher has no microphone permission (Settings, Permissions): silence")
        try {
            val buf = ByteArray(MIC_BLOCK)
            var next = SystemClock.elapsedRealtime()
            while (running) {
                if (probe.available() > MIC_STALL_BYTES) break
                if (recorder != null) {
                    var got = 0
                    while (got < buf.size) {
                        val n = recorder.read(buf, got, buf.size - got)
                        if (n < 0) throw IOException("AudioRecord.read returned $n")
                        got += n
                    }
                } else {
                    next += 20
                    val wait = next - SystemClock.elapsedRealtime()
                    if (wait > 0) Thread.sleep(wait)
                }
                out.write(buf)
            }
        } finally {
            recorder?.stop()
            recorder?.release()
        }
    }

    /**
     * The phone's microphone, started. VOICE_COMMUNICATION gets Android's own echo cancelling, which hears what the
     * track plays; Linux has no way to know that sound comes back after this bridge's delay.
     */
    private fun startRecorder(): AudioRecord {
        val minBuffer = AudioRecord.getMinBufferSize(RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
        val recorder =
            AudioRecord
                .Builder()
                .setAudioSource(MediaRecorder.AudioSource.VOICE_COMMUNICATION)
                .setAudioFormat(
                    AudioFormat
                        .Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(RATE)
                        .setChannelMask(AudioFormat.CHANNEL_IN_MONO)
                        .build(),
                ).setBufferSizeInBytes(maxOf(minBuffer, MIC_BLOCK * 10))
                .build()
        try {
            check(recorder.state == AudioRecord.STATE_INITIALIZED) { "the microphone didn't start (is another app using it?)" }
            recorder.startRecording()
        } catch (e: Exception) {
            recorder.release()
            throw e
        }
        return recorder
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
