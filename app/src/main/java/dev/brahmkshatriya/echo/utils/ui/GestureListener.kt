package dev.brahmkshatriya.echo.utils.ui

import android.graphics.drawable.RippleDrawable
import android.os.SystemClock
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import java.util.Timer
import kotlin.concurrent.schedule

interface GestureListener {
    val onClick: () -> Unit
    val onLongClick: (() -> Unit)?
    val onDoubleClick: (() -> Unit)?

    companion object {
        fun View.handleGestures(listener: GestureListener) {
            val gestureListener = object : GestureDetector.SimpleOnGestureListener() {
                private var timer: Timer? = null
                private var beingDoubleClicked = false

                /** The double-tap action resolved at tap-up, so it can't change mid-gesture. */
                private var doubleClickAction: (() -> Unit)? = null

                /** True when the tap was already handled in [onSingleTapUp]. */
                private var handledOnTapUp = false
                private var lastTapUp = 0L

                /**
                 * [onSingleTapConfirmed] only fires after the double-tap timeout, so when
                 * there is no double-tap action to wait for we act immediately instead.
                 * That ~300 ms wait is what made tapping the mini player feel slow.
                 */
                override fun onSingleTapUp(e: MotionEvent): Boolean {
                    doubleClickAction = listener.onDoubleClick
                    if (doubleClickAction != null) return true
                    val now = SystemClock.uptimeMillis()
                    val isSecondTap = now - lastTapUp < ViewConfiguration.getDoubleTapTimeout()
                    lastTapUp = now
                    if (isSecondTap) return true
                    handledOnTapUp = true
                    listener.onClick()
                    return true
                }

                override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                    if (handledOnTapUp) {
                        handledOnTapUp = false
                        return true
                    }
                    if (doubleClickAction == null) return true
                    if (!beingDoubleClicked) listener.onClick()
                    else return onDoubleTap(e)
                    return true
                }

                override fun onDoubleTap(e: MotionEvent): Boolean {
                    val onDoubleClick =
                        doubleClickAction ?: listener.onDoubleClick ?: return false
                    isPressed = true
                    isPressed = false
                    (background as? RippleDrawable)?.setHotspot(e.x, e.y)
                    onDoubleClick.invoke()
                    beingDoubleClicked = true
                    timer?.cancel()
                    val timer = Timer()
                    this.timer = timer
                    timer.schedule(1000) { beingDoubleClicked = false }
                    return true
                }

                override fun onLongPress(e: MotionEvent) {
                    listener.onLongClick?.invoke()
                }
            }
            val detector = GestureDetector(context, gestureListener)
            setOnTouchListener { _, event ->
                detector.onTouchEvent(event)
                performClick()
                true
            }
        }
    }
}