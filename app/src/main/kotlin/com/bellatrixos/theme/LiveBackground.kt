package com.bellatrixos.theme

import java.io.File

/** Video extensions Bellatrix keeps as-is: Android's player picks the codec it supports on this phone. */
val VIDEO_EXTENSIONS =
    setOf(
        "mp4", "m4v", "webm", "mkv", "3gp", "3g2", "mov", "avi", "mpeg", "mpg", "ts", "m2ts",
        "mts", "flv", "wmv", "ogv", "divx", "f4v", "vob", "asf", "rmvb", "mpv", "m2v",
    )

/** Image extensions Bellatrix can decode; animated ones are shown by the live wallpaper. */
val WALLPAPER_IMAGE_EXTENSIONS =
    setOf(
        "png", "jpg", "jpeg", "jpe", "webp", "gif", "apng", "avif", "heic", "heif", "bmp", "tiff",
        "tif", "ico", "dng", "psd",
    )

/** Animated image formats the wallpaper service can play frame by frame on Android 9+. */
val ANIMATED_IMAGE_EXTENSIONS = setOf("gif", "webp", "apng", "heic", "heif", "avif")

/** A background that is a video: played by [BellatrixWallpaperService] through Android's media player. */
fun File.isVideoBackground() = extension.lowercase() in VIDEO_EXTENSIONS

/** A background that animates without a video player (a GIF, an animated WebP or APNG). */
fun File.isAnimatedImageBackground() = extension.lowercase() in ANIMATED_IMAGE_EXTENSIONS

/** A background that moves: only the live wallpaper shows it, so picking one asks Android to make it the phone's. */
fun File.isLiveBackground() = isVideoBackground() || isAnimatedImageBackground()

/** What the user's own backgrounds may be, besides the images Linux reads too. */
val BACKGROUND_EXTENSIONS = WALLPAPER_IMAGE_EXTENSIONS + VIDEO_EXTENSIONS
