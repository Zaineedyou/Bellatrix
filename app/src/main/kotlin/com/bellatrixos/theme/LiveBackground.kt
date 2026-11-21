package com.bellatrixos.theme

import java.io.File

private val VIDEO_EXTENSIONS = setOf("mp4", "webm", "mkv", "m4v", "3gp", "mov")

/** A background that is a video: played by [BellatrixWallpaperService], so it moves only as the phone's live wallpaper. */
fun File.isVideoBackground() = extension.lowercase() in VIDEO_EXTENSIONS

/** A background that is a GIF, which [BellatrixWallpaperService] animates (an animated WebP does too, once it is the live wallpaper). */
fun File.isGifBackground() = extension.lowercase() == "gif"

/** A background that moves: only the live wallpaper shows it, so picking one asks Android to make it the phone's. */
fun File.isLiveBackground() = isVideoBackground() || isGifBackground()

/** What the user's own backgrounds may be, besides the images Linux reads too. */
val BACKGROUND_EXTENSIONS = listOf("png", "jpg", "jpeg", "webp", "gif", "mp4", "webm", "mkv", "m4v", "3gp", "mov")
