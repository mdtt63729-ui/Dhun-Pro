package dev.brahmkshatriya.echo.utils.ui

import android.graphics.drawable.RippleDrawable
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
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

                override fun onSingleTapUp(e: MotionEvent): Boolean {
                    // A normal mini-player tap must not wait for the double-tap timeout.
                    // Only the expanded player has a double-tap action, so single taps can
                    // be dispatched immediately everywhere else.
                    if (listener.onDoubleClick == null) {
                        listener.onClick()
                    }
                    return true
                }

                override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                    if (listener.onDoubleClick != null && !beingDoubleClicked) {
                        listener.onClick()
                    } else if (beingDoubleClicked) {
                        beingDoubleClicked = false
                    }
                    return true
                }

                override fun onDoubleTap(e: MotionEvent): Boolean {
                    val onDoubleClick = listener.onDoubleClick ?: return false
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
            setOnTouchListener { view, event ->
                detector.onTouchEvent(event)
                if (event.actionMasked == MotionEvent.ACTION_UP ||
                    event.actionMasked == MotionEvent.ACTION_CANCEL
                ) {
                    view.performClick()
                }
                true
            }
        }
    }
}
