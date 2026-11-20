package com.bellatrixos.wayland

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.InputDevice
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import kotlin.math.hypot
import kotlin.math.roundToInt

private const val WHEEL_STEP = 15f

/**
 * Routes pointer input over a Linux window's view to its surfaces. Mouse input
 * is forwarded as-is. Touch is mapped for a phone: tap = left click, long
 * press = right click, drag = scroll; clients with wl_touch (foot, GTK) get
 * real touch. A tap on the text-cursor row (a terminal's prompt) also calls
 * [onCursorTap], after the program gets the event.
 */
class LinuxWindowInputView(
    context: Context,
    private val wm: LinuxWindowManager,
    private val window: XdgToplevel,
) : View(context) {
    var onCursorTap: () -> Unit = {}

    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop
    private val longPressMs = ViewConfiguration.getLongPressTimeout().toLong()
    private val handler = Handler(Looper.getMainLooper())
    private var mouseButtons = 0
    private var activeId = -1
    private var startX = 0f
    private var startY = 0f
    private var lastX = 0f
    private var lastY = 0f
    private var hit: SurfaceHit? = null
    private var touchClient = false
    private var scrolling = false
    private var longPressed = false
    private var moved = false
    private var downAt = 0L

    private val longPress =
        Runnable {
            longPressed = true
            hit?.let { wm.click(it, BTN_RIGHT) }
        }

    init {
        isClickable = true
        isFocusable = false
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.isFromSource(InputDevice.SOURCE_MOUSE)) return mouse(event)

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                val index = event.actionIndex
                if (activeId != -1) return true
                activeId = event.getPointerId(index)
                startX = event.getX(index)
                startY = event.getY(index)
                lastX = startX
                lastY = startY
                downAt = event.eventTime
                val h = window.renderer?.hitTest(startX, startY)
                hit = h
                if (h == null || !wm.onPress(window, h)) {
                    reset()
                    return true
                }
                touchClient = wm.wantsTouch(h.surface)
                scrolling = false
                longPressed = false
                moved = false
                if (touchClient) {
                    wm.touchDown(h, activeId)
                } else {
                    handler.postDelayed(longPress, longPressMs)
                }
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                if (activeId == -1) return false
                val index = event.findPointerIndex(activeId).takeIf { it >= 0 } ?: return true
                val x = event.getX(index)
                val y = event.getY(index)
                val h = hit ?: return true
                if (touchClient) {
                    if (hypot(x - startX, y - startY) > touchSlop) moved = true
                    wm.touchMotion(activeId, h.x + (x - startX) / h.scale, h.y + (y - startY) / h.scale)
                } else {
                    if (!scrolling && !longPressed && hypot(x - startX, y - startY) > touchSlop) {
                        scrolling = true
                        handler.removeCallbacks(longPress)
                        wm.pointerMove(h)
                    }
                    if (scrolling) wm.scroll(-(x - lastX) / h.scale, -(y - lastY) / h.scale, AXIS_SOURCE_FINGER)
                }
                lastX = x
                lastY = y
                return true
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (activeId == -1) return false
                val index = event.findPointerIndex(activeId).takeIf { it >= 0 }
                val x = index?.let(event::getX) ?: lastX
                val y = index?.let(event::getY) ?: lastY
                val cancelled = event.actionMasked == MotionEvent.ACTION_CANCEL
                handler.removeCallbacks(longPress)
                val h = hit
                if (touchClient) {
                    wm.touchUp(activeId)
                    if (!cancelled && !moved && event.eventTime - downAt < longPressMs && h != null && wm.onTextCursorRow(h)) onCursorTap()
                } else {
                    if (scrolling) {
                        wm.scrollStop()
                    } else if (!cancelled && !longPressed && h != null) {
                        wm.click(h, BTN_LEFT)
                        if (wm.onTextCursorRow(h)) onCursorTap()
                    }
                }
                // Keep the last coordinates so the lifted pointer is where it was dragged to.
                lastX = x
                lastY = y
                reset()
                return true
            }

            else -> return false
        }
    }

    private fun reset() {
        activeId = -1
        hit = null
        touchClient = false
        scrolling = false
        longPressed = false
        moved = false
    }

    private fun mouse(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_SCROLL -> {
                val dx = event.getAxisValue(MotionEvent.AXIS_HSCROLL) * WHEEL_STEP
                val dy = event.getAxisValue(MotionEvent.AXIS_VSCROLL) * WHEEL_STEP
                wm.scroll(dx, dy, AXIS_SOURCE_WHEEL, dy.roundToInt())
                return true
            }

            MotionEvent.ACTION_HOVER_MOVE, MotionEvent.ACTION_MOVE -> {
                val h = window.renderer?.hitTest(event.x, event.y)
                if (mouseButtons == 0 || wm.server.input.pointerFocus == h?.surface) wm.pointerMove(h)
                return true
            }

            MotionEvent.ACTION_BUTTON_PRESS, MotionEvent.ACTION_BUTTON_RELEASE -> {
                val h = window.renderer?.hitTest(event.x, event.y)
                val buttons = mouseMask(event.buttonState)
                val pressed = event.actionMasked == MotionEvent.ACTION_BUTTON_PRESS
                if (pressed && mouseButtons == 0 && (h == null || !wm.onPress(window, h))) return true
                wm.pointerMove(h)
                for ((bit, code) in MOUSE_BUTTONS) {
                    val was = mouseButtons and bit != 0
                    val pressedNow = buttons and bit != 0
                    if (!was && pressedNow) wm.pointerButton(code, true)
                    if (was && !pressedNow) wm.pointerButton(code, false)
                }
                mouseButtons = buttons
                return true
            }

            MotionEvent.ACTION_UP -> {
                mouseButtons = 0
                return true
            }

            else -> return false
        }
    }

    private fun mouseMask(state: Int) =
        (if (state and MotionEvent.BUTTON_PRIMARY != 0) 1 else 0) or
            (if (state and MotionEvent.BUTTON_SECONDARY != 0) 2 else 0) or
            (if (state and MotionEvent.BUTTON_TERTIARY != 0) 4 else 0)

    private companion object {
        val MOUSE_BUTTONS = listOf(1 to BTN_LEFT, 2 to BTN_RIGHT, 4 to BTN_MIDDLE)
    }
}
