package com.bellatrixos.theme

import android.app.WallpaperColors
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ImageDecoder
import android.graphics.SurfaceTexture
import android.graphics.drawable.AnimatedImageDrawable
import android.graphics.drawable.Drawable
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.opengl.EGL14
import android.opengl.EGLConfig
import android.opengl.EGLContext
import android.opengl.EGLDisplay
import android.opengl.EGLSurface
import android.opengl.GLES11Ext
import android.opengl.GLES20
import android.opengl.GLUtils
import android.opengl.Matrix
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.service.wallpaper.WallpaperService
import android.util.Log
import android.view.Surface
import android.view.SurfaceHolder
import com.bellatrixos.ui.decodeAnimated
import com.bellatrixos.ui.wallpaperCrop
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Android live wallpaper showing the current Bellatrix background: a still image, a GIF or a video. It is the one that
 * moves: a background picked in style as a video or GIF shows on the phone only through this wallpaper, which the
 * launcher asks Android to make the phone's once. Its other purpose is [Engine.onComputeColors]: Android 12+ derives
 * the system color scheme (quick settings, notifications, Settings, keyboard, dynamic-color apps) from the colors the
 * wallpaper reports, so we report the theme's palette instead of colors sampled from the image.
 *
 * Everything is drawn with OpenGL on one render thread, and nothing ever draws on the surface with a canvas. A surface
 * a canvas has drawn on is connected to the CPU producer, and Android then refuses to connect a video decoder to it:
 * the video never showed and only the background color stayed (a black screen). Here the video goes into a
 * [SurfaceTexture] and is drawn by GL like the image and the GIF, so which kind of background it is never matters.
 */
class BellatrixWallpaperService : WallpaperService() {
    override fun onCreateEngine(): Engine = BellatrixEngine()

    private inner class BellatrixEngine : Engine() {
        private val main = Handler(Looper.getMainLooper())
        private val decoder = Executors.newSingleThreadExecutor()
        private val thread = HandlerThread("bellatrix-wallpaper").also { it.start() }

        /** Every GL object, the player and the animation below belong to this thread alone. */
        private val gl = Handler(thread.looper)

        @Volatile private var colors: Map<String, String> = emptyMap()

        @Volatile private var visible = false

        // Used by the decoder thread alone: what is already loaded, to tell a changed background from the same one.
        private var loadedPath: String? = null
        private var loadedStamp = 0L

        // ---- Render thread state ----
        private var display: EGLDisplay = EGL14.EGL_NO_DISPLAY
        private var eglContext: EGLContext = EGL14.EGL_NO_CONTEXT
        private var eglSurface: EGLSurface = EGL14.EGL_NO_SURFACE
        private var boundSurface: Surface? = null
        private var ready = false
        private var width = 0
        private var height = 0
        private var program2d = 0
        private var programOes = 0

        /** Where the launcher's pages are scrolled, 0 to 1: a still background slides along. */
        private var xOffset = 0.5f

        /** The still image showing, in software memory so GL can read it. */
        private var still: Bitmap? = null
        private var stillTex = 0

        /** The GIF showing: each frame is drawn into [gifBitmap], which is uploaded as a texture. */
        private var animated: AnimatedImageDrawable? = null
        private var gifBitmap: Bitmap? = null
        private var gifCanvas: Canvas? = null
        private var gifTex = 0
        private var gifUploaded = false

        /** The video showing: the player draws into [videoTexture], which GL then draws on the screen. */
        private var video: File? = null
        private var player: MediaPlayer? = null
        private var videoTexture: SurfaceTexture? = null
        private var videoSurface: Surface? = null
        private var videoTex = 0
        private var videoWidth = 0
        private var videoHeight = 0
        private var videoFrame = false
        private val videoMatrix = FloatArray(16).also { Matrix.setIdentityM(it, 0) }
        private val identity = FloatArray(16).also { Matrix.setIdentityM(it, 0) }

        private val quadPos = floatBuffer(floatArrayOf(-1f, -1f, 1f, -1f, -1f, 1f, 1f, 1f))
        private val quadTex = floatBuffer(FloatArray(8))

        private val renderGif = Runnable {
            if (ready && visible && animated != null) {
                drawGifFrame()
                render()
            }
        }

        private val frames =
            object : Drawable.Callback {
                override fun invalidateDrawable(who: Drawable) {
                    gl.post(renderGif)
                }

                override fun scheduleDrawable(
                    who: Drawable,
                    what: Runnable,
                    `when`: Long,
                ) {
                    gl.postAtTime(what, who, `when`)
                }

                override fun unscheduleDrawable(
                    who: Drawable,
                    what: Runnable,
                ) {
                    gl.removeCallbacks(what, who)
                }
            }

        private val onThemeChanged: () -> Unit = { main.post { reload() } }

        override fun onCreate(surfaceHolder: SurfaceHolder) {
            super.onCreate(surfaceHolder)
            setOffsetNotificationsEnabled(true)
            ThemeStore.changeListeners.add(onThemeChanged)
            reload()
        }

        override fun onDestroy() {
            ThemeStore.changeListeners.remove(onThemeChanged)
            decoder.shutdownNow()
            val done = CountDownLatch(1)
            val posted =
                gl.post {
                    releaseContent()
                    releaseGl()
                    done.countDown()
                }
            if (posted) done.await(2, TimeUnit.SECONDS)
            thread.quitSafely()
            super.onDestroy()
        }

        override fun onSurfaceChanged(
            holder: SurfaceHolder,
            format: Int,
            width: Int,
            height: Int,
        ) {
            super.onSurfaceChanged(holder, format, width, height)
            val surface = holder.surface
            gl.post { setSurface(surface, width, height) }
        }

        override fun onSurfaceDestroyed(holder: SurfaceHolder) {
            // Everything that draws into this surface has to be gone before it is: wait for the render thread.
            val done = CountDownLatch(1)
            val posted =
                gl.post {
                    releaseGl()
                    done.countDown()
                }
            if (posted) done.await(2, TimeUnit.SECONDS)
            super.onSurfaceDestroyed(holder)
        }

        override fun onOffsetsChanged(
            xOffset: Float,
            yOffset: Float,
            xOffsetStep: Float,
            yOffsetStep: Float,
            xPixelOffset: Int,
            yPixelOffset: Int,
        ) {
            gl.post {
                if (xOffset == this.xOffset) return@post
                this.xOffset = xOffset
                if (visible && video == null && animated == null) render()
            }
        }

        override fun onVisibilityChanged(visible: Boolean) {
            this.visible = visible
            // Nothing moves while the wallpaper is covered: no battery goes on frames nobody sees.
            gl.post {
                player?.let { p -> runCatching { if (visible) p.start() else p.pause() } }
                animated?.let { if (visible) it.start() else it.stop() }
                if (visible) render()
            }
        }

        // ---- Loading ----

        /** Reads the saved theme off the main thread, then shows its background, whatever kind it is. */
        private fun reload() {
            // A change posted just before the engine was destroyed.
            if (decoder.isShutdown) return
            runCatching {
                decoder.execute {
                    val (themeColors, file) =
                        runCatching { ThemeStore.savedSelection(this@BellatrixWallpaperService) }
                            .onFailure { Log.w(TAG, "the saved background can't be read", it) }
                            .getOrNull() ?: (emptyMap<String, String>() to null)
                    Log.i(TAG, "background: ${file?.path ?: "none"} (${themeColors.size} theme colors)")
                    val stamp = file?.lastModified() ?: 0L
                    val changed = file?.path != loadedPath || stamp != loadedStamp
                    loadedPath = file?.path
                    loadedStamp = stamp
                    val image = file?.takeIf { changed && !it.isVideoBackground() }
                    val gif =
                        image?.takeIf { it.extension.lowercase() in ANIMATED_TYPES }?.let { decodeAnimated(it, this@BellatrixWallpaperService) } as? AnimatedImageDrawable
                    val movie = file?.takeIf { changed && it.isVideoBackground() }
                    // A video shows its first frame at once, as a picture, and moves when the player has frames: the
                    // screen is never left on the plain color while the player starts, or if it can't play this file.
                    val picture = if (image != null && gif == null) decodeStill(image) else movie?.let { posterFrame(it) }
                    gl.post { show(themeColors, changed, picture, gif, movie) }
                }
            }
        }

        /** The first frame of a video, scaled like [decodeStill], or null when this phone can't read one. */
        private fun posterFrame(file: File): Bitmap? {
            val retriever = MediaMetadataRetriever()
            return try {
                retriever.setDataSource(file.path)
                val frame = retriever.getFrameAtTime(0, MediaMetadataRetriever.OPTION_CLOSEST_SYNC) ?: return null
                val screen = resources.displayMetrics
                val side = max(screen.widthPixels, screen.heightPixels) * 3 / 4
                val scale = min(min(1f, side.toFloat() / min(frame.width, frame.height)), MAX_TEXTURE.toFloat() / max(frame.width, frame.height))
                val sized =
                    if (scale < 1f) {
                        Bitmap.createScaledBitmap(frame, (frame.width * scale).roundToInt().coerceAtLeast(1), (frame.height * scale).roundToInt().coerceAtLeast(1), true)
                    } else {
                        frame
                    }
                if (sized.config == Bitmap.Config.HARDWARE) sized.copy(Bitmap.Config.ARGB_8888, false) else sized
            } catch (e: Exception) {
                Log.w(TAG, "no first frame for the video", e)
                null
            } finally {
                runCatching { retriever.release() }
            }
        }

        /** A still image as a software bitmap (GL can't read a hardware one), covering the screen with room to slide. */
        private fun decodeStill(file: File): Bitmap? {
            val screen = resources.displayMetrics
            val side = max(screen.widthPixels, screen.heightPixels) * 3 / 4
            return runCatching {
                ImageDecoder.decodeBitmap(ImageDecoder.createSource(file)) { decoder, info, _ ->
                    val (w, h) = info.size.width to info.size.height
                    val scale = min(min(1f, side.toFloat() / min(w, h)), MAX_TEXTURE.toFloat() / max(w, h))
                    if (scale < 1f) decoder.setTargetSize((w * scale).roundToInt().coerceAtLeast(1), (h * scale).roundToInt().coerceAtLeast(1))
                    decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                }
            }.getOrNull()
        }

        /** On the render thread: takes what [reload] decoded and shows it. */
        private fun show(
            themeColors: Map<String, String>,
            changed: Boolean,
            picture: Bitmap?,
            gif: AnimatedImageDrawable?,
            movie: File?,
        ) {
            val colorsChanged = themeColors != colors
            colors = themeColors
            if (changed) {
                releaseContent()
                still = picture
                video = movie
                if (gif != null) {
                    gif.callback = frames
                    gif.repeatCount = AnimatedImageDrawable.REPEAT_INFINITE
                    animated = gif
                }
                if (ready) applyContent()
            }
            render()
            if (colorsChanged) main.post { notifyColorsChanged() }
        }

        // ---- EGL ----

        /** A new surface (or the same one resized): the GL context is made for it and the background put on it. */
        private fun setSurface(
            surface: Surface,
            w: Int,
            h: Int,
        ) {
            if (!surface.isValid) return
            if (ready && boundSurface === surface) {
                if (w != width || h != height) {
                    width = w
                    height = h
                    gifBitmap = null
                    gifCanvas = null
                    gifUploaded = false
                    if (animated != null) drawGifFrame()
                }
                render()
                return
            }
            releaseGl()
            width = w
            height = h
            if (!createEgl(surface)) {
                releaseGl()
                return
            }
            boundSurface = surface
            ready = true
            program2d = buildProgram(FRAGMENT_2D)
            programOes = buildProgram(FRAGMENT_OES)
            applyContent()
            render()
        }

        private fun createEgl(surface: Surface): Boolean {
            val d = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
            if (d == EGL14.EGL_NO_DISPLAY) return false
            val version = IntArray(2)
            if (!EGL14.eglInitialize(d, version, 0, version, 1)) return false
            display = d
            val wanted =
                intArrayOf(
                    EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
                    EGL14.EGL_SURFACE_TYPE, EGL14.EGL_WINDOW_BIT,
                    EGL14.EGL_RED_SIZE, 8,
                    EGL14.EGL_GREEN_SIZE, 8,
                    EGL14.EGL_BLUE_SIZE, 8,
                    EGL14.EGL_ALPHA_SIZE, 8,
                    EGL14.EGL_NONE,
                )
            val configs = arrayOfNulls<EGLConfig>(1)
            val count = IntArray(1)
            if (!EGL14.eglChooseConfig(d, wanted, 0, configs, 0, 1, count, 0) || count[0] < 1) return false
            val config = configs[0] ?: return false
            eglContext = EGL14.eglCreateContext(d, config, EGL14.EGL_NO_CONTEXT, intArrayOf(EGL14.EGL_CONTEXT_CLIENT_VERSION, 2, EGL14.EGL_NONE), 0)
            if (eglContext == EGL14.EGL_NO_CONTEXT) return false
            eglSurface = EGL14.eglCreateWindowSurface(d, config, surface, intArrayOf(EGL14.EGL_NONE), 0)
            if (eglSurface == EGL14.EGL_NO_SURFACE) return false
            return EGL14.eglMakeCurrent(d, eglSurface, eglSurface, eglContext)
        }

        /** Lets go of the player, the textures and EGL, so the surface is free; the background itself is kept to start again. */
        private fun releaseGl() {
            releasePlayer()
            animated?.stop()
            if (ready && display != EGL14.EGL_NO_DISPLAY) {
                deleteTexture(stillTex)
                deleteTexture(gifTex)
                if (program2d != 0) GLES20.glDeleteProgram(program2d)
                if (programOes != 0) GLES20.glDeleteProgram(programOes)
            }
            stillTex = 0
            gifTex = 0
            program2d = 0
            programOes = 0
            gifBitmap = null
            gifCanvas = null
            gifUploaded = false
            if (display != EGL14.EGL_NO_DISPLAY) {
                EGL14.eglMakeCurrent(display, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT)
                if (eglSurface != EGL14.EGL_NO_SURFACE) EGL14.eglDestroySurface(display, eglSurface)
                if (eglContext != EGL14.EGL_NO_CONTEXT) EGL14.eglDestroyContext(display, eglContext)
                EGL14.eglTerminate(display)
            }
            display = EGL14.EGL_NO_DISPLAY
            eglContext = EGL14.EGL_NO_CONTEXT
            eglSurface = EGL14.EGL_NO_SURFACE
            boundSurface = null
            ready = false
        }

        // ---- Content ----

        /** Puts the loaded background on the screen: the still image, the GIF or the video. Needs GL ready. */
        private fun applyContent() {
            val file = video
            val gif = animated
            val picture = still
            if (picture != null && !picture.isRecycled) {
                stillTex = newTexture(GLES20.GL_TEXTURE_2D)
                GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, picture, 0)
            }
            when {
                file != null -> startVideo(file)
                gif != null -> {
                    drawGifFrame()
                    if (visible) gif.start()
                }
            }
        }

        /** Stops whatever moves and forgets the background: the video, the GIF, the image. */
        private fun releaseContent() {
            releasePlayer()
            video = null
            animated?.let {
                it.stop()
                it.callback = null
            }
            animated = null
            gl.removeCallbacks(renderGif)
            if (ready) {
                deleteTexture(stillTex)
                deleteTexture(gifTex)
            }
            stillTex = 0
            gifTex = 0
            gifBitmap = null
            gifCanvas = null
            gifUploaded = false
            still = null
        }

        /** Plays [file] into a texture, muted and looping; every new frame is drawn on the screen by [render]. */
        private fun startVideo(file: File) {
            if (player != null) return
            videoTex = newTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES)
            videoFrame = false
            videoWidth = 0
            videoHeight = 0
            val texture = SurfaceTexture(videoTex)
            texture.setOnFrameAvailableListener({ onVideoFrame(texture) }, gl)
            val surface = Surface(texture)
            videoTexture = texture
            videoSurface = surface
            val p = MediaPlayer()
            player = p
            runCatching {
                p.setSurface(surface)
                p.setDataSource(file.path)
                p.isLooping = true
                p.setVolume(0f, 0f)
                p.setOnVideoSizeChangedListener { _, w, h ->
                    videoWidth = w
                    videoHeight = h
                }
                p.setOnPreparedListener { prepared ->
                    Log.i(TAG, "video ready ${prepared.videoWidth}x${prepared.videoHeight}, visible=$visible")
                    if (visible) runCatching { prepared.start() }
                }
                // A video this phone can't play: the player goes, and the plain background color stays.
                p.setOnErrorListener { _, what, extra ->
                    Log.w(TAG, "the video can't be played ($what, $extra)")
                    if (player === p) releasePlayer()
                    true
                }
                p.prepareAsync()
            }.onFailure {
                Log.w(TAG, "the video can't be opened", it)
                releasePlayer()
            }
        }

        private fun onVideoFrame(texture: SurfaceTexture) {
            if (!ready || videoTexture !== texture) return
            runCatching {
                texture.updateTexImage()
                texture.getTransformMatrix(videoMatrix)
                if (!videoFrame) Log.i(TAG, "first video frame")
                videoFrame = true
            }
            if (visible) render()
        }

        private fun releasePlayer() {
            val p = player
            player = null
            if (p != null) {
                runCatching { p.setOnErrorListener(null) }
                runCatching { p.stop() }
                runCatching { p.release() }
            }
            runCatching { videoSurface?.release() }
            runCatching { videoTexture?.release() }
            videoSurface = null
            videoTexture = null
            videoFrame = false
            if (ready) deleteTexture(videoTex)
            videoTex = 0
        }

        // ---- Drawing ----

        private fun render() {
            if (!ready || width <= 0 || height <= 0) return
            val bg = color("background", Color.BLACK)
            GLES20.glViewport(0, 0, width, height)
            GLES20.glClearColor(Color.red(bg) / 255f, Color.green(bg) / 255f, Color.blue(bg) / 255f, 1f)
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
            val picture = still
            when {
                video != null && videoFrame && videoTex != 0 -> drawVideo()
                animated != null && gifTex != 0 && gifUploaded -> drawQuad(program2d, GLES20.GL_TEXTURE_2D, gifTex, FULL, identity)
                picture != null && stillTex != 0 && !picture.isRecycled -> {
                    val src = wallpaperCrop(picture.width, picture.height, width, height, xOffset)
                    val l = src.left / picture.width.toFloat()
                    val r = src.right / picture.width.toFloat()
                    val t = src.top / picture.height.toFloat()
                    val b = src.bottom / picture.height.toFloat()
                    drawQuad(program2d, GLES20.GL_TEXTURE_2D, stillTex, floatArrayOf(l, b, r, b, l, t, r, t), identity)
                }
            }
            if (!EGL14.eglSwapBuffers(display, eglSurface)) Log.w(TAG, "swap failed: 0x" + Integer.toHexString(EGL14.eglGetError()))
        }

        /** The video fills the screen from its center, like a still one. */
        private fun drawVideo() {
            var u0 = 0f
            var u1 = 1f
            var v0 = 0f
            var v1 = 1f
            if (videoWidth > 0 && videoHeight > 0) {
                val videoRatio = videoWidth.toFloat() / videoHeight
                val screenRatio = width.toFloat() / height
                if (videoRatio > screenRatio) {
                    val f = screenRatio / videoRatio
                    u0 = (1f - f) / 2f
                    u1 = u0 + f
                } else {
                    val f = videoRatio / screenRatio
                    v0 = (1f - f) / 2f
                    v1 = v0 + f
                }
            }
            drawQuad(programOes, GLES11Ext.GL_TEXTURE_EXTERNAL_OES, videoTex, floatArrayOf(u0, v0, u1, v0, u0, v1, u1, v1), videoMatrix)
        }

        /** Draws the GIF's current frame, covering the screen from its center, into [gifBitmap] and uploads that. */
        private fun drawGifFrame() {
            val gif = animated ?: return
            if (!ready || width <= 0 || height <= 0 || gif.intrinsicWidth <= 0 || gif.intrinsicHeight <= 0) return
            var bmp = gifBitmap
            var canvas = gifCanvas
            if (bmp == null || canvas == null) {
                bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                canvas = Canvas(bmp)
                gifBitmap = bmp
                gifCanvas = canvas
                gifUploaded = false
            }
            bmp.eraseColor(color("background", Color.BLACK))
            val scale = max(width.toFloat() / gif.intrinsicWidth, height.toFloat() / gif.intrinsicHeight)
            val w = (gif.intrinsicWidth * scale).roundToInt()
            val h = (gif.intrinsicHeight * scale).roundToInt()
            val left = (width - w) / 2
            val top = (height - h) / 2
            gif.setBounds(left, top, left + w, top + h)
            gif.draw(canvas)
            if (gifTex == 0) gifTex = newTexture(GLES20.GL_TEXTURE_2D)
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, gifTex)
            if (gifUploaded) {
                GLUtils.texSubImage2D(GLES20.GL_TEXTURE_2D, 0, 0, 0, bmp)
            } else {
                GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bmp, 0)
                gifUploaded = true
            }
        }

        /** [uv] holds the texture position of the quad's corners: bottom left, bottom right, top left, top right. */
        private fun drawQuad(
            program: Int,
            target: Int,
            texture: Int,
            uv: FloatArray,
            matrix: FloatArray,
        ) {
            if (program == 0) return
            GLES20.glUseProgram(program)
            val aPos = GLES20.glGetAttribLocation(program, "aPos")
            val aTex = GLES20.glGetAttribLocation(program, "aTex")
            quadTex.clear()
            quadTex.put(uv)
            quadTex.position(0)
            quadPos.position(0)
            GLES20.glVertexAttribPointer(aPos, 2, GLES20.GL_FLOAT, false, 0, quadPos)
            GLES20.glEnableVertexAttribArray(aPos)
            GLES20.glVertexAttribPointer(aTex, 2, GLES20.GL_FLOAT, false, 0, quadTex)
            GLES20.glEnableVertexAttribArray(aTex)
            GLES20.glUniformMatrix4fv(GLES20.glGetUniformLocation(program, "uTex"), 1, false, matrix, 0)
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
            GLES20.glBindTexture(target, texture)
            GLES20.glUniform1i(GLES20.glGetUniformLocation(program, "uSampler"), 0)
            GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)
            GLES20.glDisableVertexAttribArray(aPos)
            GLES20.glDisableVertexAttribArray(aTex)
        }

        private fun newTexture(target: Int): Int {
            val id = IntArray(1)
            GLES20.glGenTextures(1, id, 0)
            GLES20.glBindTexture(target, id[0])
            GLES20.glTexParameteri(target, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
            GLES20.glTexParameteri(target, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
            GLES20.glTexParameteri(target, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
            GLES20.glTexParameteri(target, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)
            return id[0]
        }

        private fun deleteTexture(id: Int) {
            if (id != 0) GLES20.glDeleteTextures(1, intArrayOf(id), 0)
        }

        private fun buildProgram(fragment: String): Int {
            val program = GLES20.glCreateProgram()
            GLES20.glAttachShader(program, compile(GLES20.GL_VERTEX_SHADER, VERTEX))
            GLES20.glAttachShader(program, compile(GLES20.GL_FRAGMENT_SHADER, fragment))
            GLES20.glLinkProgram(program)
            val linked = IntArray(1)
            GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, linked, 0)
            if (linked[0] == 0) {
                Log.w(TAG, "shader link failed: " + GLES20.glGetProgramInfoLog(program))
                GLES20.glDeleteProgram(program)
                return 0
            }
            return program
        }

        private fun compile(
            type: Int,
            source: String,
        ): Int {
            val shader = GLES20.glCreateShader(type)
            GLES20.glShaderSource(shader, source)
            GLES20.glCompileShader(shader)
            return shader
        }

        private fun color(
            key: String,
            fallback: Int,
        ): Int = colors[key]?.let { runCatching { Color.parseColor(it) }.getOrNull() } ?: fallback

        /** The theme palette, accent first: Android seeds its system colors from the most prominent one. */
        override fun onComputeColors(): WallpaperColors? {
            val palette = colors
            if (palette.isEmpty()) return null
            fun pick(
                key: String,
                fallback: Int,
            ): Int = palette[key]?.let { runCatching { Color.parseColor(it) }.getOrNull() } ?: fallback
            val primary = Color.valueOf(pick("accent", Color.BLUE))
            val secondary = Color.valueOf(pick("magenta", pick("accent", Color.BLUE)))
            val tertiary = Color.valueOf(pick("background", Color.BLACK))
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return WallpaperColors(primary, secondary, tertiary)
            val hints =
                if (palette["mode"] == "light") WallpaperColors.HINT_SUPPORTS_DARK_TEXT else WallpaperColors.HINT_SUPPORTS_DARK_THEME
            return WallpaperColors(primary, secondary, tertiary, hints)
        }
    }

    private companion object {
        const val TAG = "BellatrixWallpaper"

        /** Every GPU can hold a texture this big. */
        const val MAX_TEXTURE = 4096

        /** Files that may hold an animation; a still WebP is told apart when it is decoded. */
        val ANIMATED_TYPES = setOf("gif", "webp")

        /** The whole texture, corners in the order bottom left, bottom right, top left, top right (image top is v = 0). */
        val FULL = floatArrayOf(0f, 1f, 1f, 1f, 0f, 0f, 1f, 0f)

        const val VERTEX =
            "attribute vec4 aPos;\n" +
                "attribute vec2 aTex;\n" +
                "uniform mat4 uTex;\n" +
                "varying vec2 vTex;\n" +
                "void main() {\n" +
                "  gl_Position = aPos;\n" +
                "  vTex = (uTex * vec4(aTex, 0.0, 1.0)).xy;\n" +
                "}\n"

        const val FRAGMENT_2D =
            "precision mediump float;\n" +
                "varying vec2 vTex;\n" +
                "uniform sampler2D uSampler;\n" +
                "void main() {\n" +
                "  gl_FragColor = texture2D(uSampler, vTex);\n" +
                "}\n"

        const val FRAGMENT_OES =
            "#extension GL_OES_EGL_image_external : require\n" +
                "precision mediump float;\n" +
                "varying vec2 vTex;\n" +
                "uniform samplerExternalOES uSampler;\n" +
                "void main() {\n" +
                "  gl_FragColor = texture2D(uSampler, vTex);\n" +
                "}\n"

        fun floatBuffer(values: FloatArray): FloatBuffer =
            ByteBuffer
                .allocateDirect(values.size * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
                .apply {
                    put(values)
                    position(0)
                }
    }
}
