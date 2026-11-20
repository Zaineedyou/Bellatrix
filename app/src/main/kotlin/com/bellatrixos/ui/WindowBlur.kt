package com.bellatrixos.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import android.view.Window
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.bellatrixos.App
import com.bellatrixos.widgets.render.FrostCache
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import kotlinx.coroutines.flow.distinctUntilChanged
import java.util.function.Consumer
import kotlin.math.roundToInt

/** How blurred the wallpaper is at an amount of 1, in dp; Android's blur goes soft long before it runs out. */
private const val MAX_RADIUS_DP = 48

private tailrec fun Context.activity(): Activity? =
    when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.activity()
        else -> null
    }

/**
 * Blurs what is behind this activity's window, the wallpaper, by [amount] from 0 to 1: Android 12's window blur. It
 * follows [amount] as it changes (the drawer opening grows it), and ends with this composable, so a window that is
 * going away isn't left blurred. The window has to be translucent for the blur to show, which the launcher's are.
 *
 * Before Android 12, or where the phone doesn't blur windows (battery saver, or the option off in developer settings),
 * it does nothing, and takes effect again if the phone starts to.
 */
@Composable
fun WindowBlur(amount: () -> Float) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
    val window = LocalContext.current.activity()?.window ?: return
    WindowBlurS(window, amount)
}

@RequiresApi(Build.VERSION_CODES.S)
@Composable
private fun WindowBlurS(
    window: Window,
    amount: () -> Float,
) {
    val density = LocalDensity.current.density
    val enabled = windowBlurEnabled(window)
    DisposableEffect(window) { onDispose { window.setBackgroundBlurRadius(0) } }
    LaunchedEffect(window, density) {
        snapshotFlow { if (enabled.value) (amount().coerceIn(0f, 1f) * MAX_RADIUS_DP * density).roundToInt() else 0 }
            .distinctUntilChanged()
            .collect { window.setBackgroundBlurRadius(it) }
    }
}

/** Whether the phone blurs [window] right now, kept up to date: it changes with battery saver and the developer option. */
@RequiresApi(Build.VERSION_CODES.S)
@Composable
private fun windowBlurEnabled(window: Window): androidx.compose.runtime.State<Boolean> {
    val enabled = remember(window) { mutableStateOf(window.windowManager.isCrossWindowBlurEnabled) }
    DisposableEffect(window) {
        val listener = Consumer<Boolean> { enabled.value = it }
        window.windowManager.addCrossWindowBlurEnabledListener(listener)
        onDispose { window.windowManager.removeCrossWindowBlurEnabledListener(listener) }
    }
    return enabled
}

/**
 * The same blur, drawn by the launcher, for the phones that won't do Android's: before Android 12, or with window blur off
 * (a skin that disables it, battery saver, the developer option). The theme's wallpaper is softened by [LauncherStyle.blur]
 * and drawn over the real one, fading in with [amount]; where the phone does blur windows it draws nothing, so the two
 * never add up. It isn't placed exactly like a live wallpaper that the phone moves its own way, but blurred, that doesn't show.
 * Goes behind the content it blurs for: first in the window, or right after the wallpaper the launcher draws.
 */
@Composable
fun BlurredWallpaper(
    offset: () -> Float,
    amount: () -> Float,
) {
    val context = LocalContext.current
    val runtime = (context.applicationContext as? App)?.runtime ?: return
    val window = context.activity()?.window
    val phoneBlurs =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && window != null) windowBlurEnabled(window).value else false
    // Over another app's live wallpaper the theme's image isn't what's behind home, so drawing it blurred would show another picture.
    if (phoneBlurs || LauncherStyle.blur <= 0.01f || runtime.themes.foreignLiveWallpaper) return
    val file = runtime.themes.background ?: return
    val density = LocalDensity.current.density
    val radius = FrostCache.radius(LauncherStyle.blur * MAX_RADIUS_DP.toDouble(), density)
    val blurred by produceState<ImageBitmap?>(null, file, radius) { value = FrostCache.get(file, radius, context) }
    val bitmap = blurred ?: return
    Canvas(Modifier.fillMaxSize()) {
        val a = amount().coerceIn(0f, 1f)
        if (a <= 0.001f) return@Canvas
        val w = size.width.toInt()
        val h = size.height.toInt()
        val src = wallpaperCrop(bitmap.width, bitmap.height, w, h, offset())
        drawImage(
            bitmap,
            srcOffset = IntOffset(src.left, src.top),
            srcSize = IntSize(src.width(), src.height()),
            dstSize = IntSize(w, h),
            alpha = a,
            filterQuality = FilterQuality.Medium,
        )
    }
}
