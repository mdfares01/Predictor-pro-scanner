package com.proscanner.app.service

import android.content.Context
import android.util.Log
import android.view.MotionEvent
import android.view.ViewConfiguration
import android.view.WindowManager
import android.widget.FrameLayout

/**
 * Custom FrameLayout overlay container that provides smooth, reliable dragging of the
 * expanded scanner panel via its dedicated header area while guaranteeing 100% immediate,
 * uninhibited touch event delivery to all child Compose controls (Scan, Settings, Minimize, Close, Stop, Retry).
 */
class DraggablePanelLayout(
    context: Context,
    private val windowManager: WindowManager,
    private val panelParams: WindowManager.LayoutParams
) : FrameLayout(context) {

    companion object {
        private const val TAG = "DraggablePanelLayout"
    }

    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop
    private val density = context.resources.displayMetrics.density

    // Header drag zone: top 64dp (16dp top padding + 48dp header height), excluding the rightmost 120dp containing the 48dp Minimize & Close buttons.
    // The Scan/Settings buttons start at ~78dp from the top, keeping all action buttons isolated from dragging.
    private val maxHeaderYPx = (64 * density).toInt()
    private val controlsWidthPx = (120 * density).toInt()

    private var activePointerId = MotionEvent.INVALID_POINTER_ID
    private var initialTouchRawX = 0f
    private var initialTouchRawY = 0f
    private var initialWindowX = 0
    private var initialWindowY = 0
    private var isDragging = false
    private var touchStartedInHeaderDragZone = false

    /**
     * Determines whether a touch event started inside the dedicated non-clickable header drag region.
     */
    private fun isPointInHeaderDragZone(x: Float, y: Float): Boolean {
        val effectiveWidth = if (width > 0) width else panelParams.width.coerceAtLeast(300)
        val inVerticalBounds = y in 0f..maxHeaderYPx.toFloat()
        val inHorizontalBounds = x in 0f..(effectiveWidth - controlsWidthPx).toFloat()
        return inVerticalBounds && inHorizontalBounds
    }

    private fun resetDragState() {
        isDragging = false
        touchStartedInHeaderDragZone = false
        activePointerId = MotionEvent.INVALID_POINTER_ID
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                activePointerId = ev.getPointerId(0)
                initialTouchRawX = ev.rawX
                initialTouchRawY = ev.rawY
                initialWindowX = panelParams.x
                initialWindowY = panelParams.y
                isDragging = false
                touchStartedInHeaderDragZone = isPointInHeaderDragZone(ev.x, ev.y)
                // Never intercept ACTION_DOWN so child Compose controls receive the down event immediately
                return false
            }
            MotionEvent.ACTION_MOVE -> {
                if (touchStartedInHeaderDragZone && !isDragging && activePointerId != MotionEvent.INVALID_POINTER_ID) {
                    val dx = Math.abs(ev.rawX - initialTouchRawX)
                    val dy = Math.abs(ev.rawY - initialTouchRawY)
                    if (dx > touchSlop || dy > touchSlop) {
                        isDragging = true
                        parent?.requestDisallowInterceptTouchEvent(true)
                        Log.d(TAG, "[Drag] Intercepting drag in header area (dx=$dx, dy=$dy)")
                        return true
                    }
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                resetDragState()
            }
        }
        return isDragging
    }

    override fun onTouchEvent(ev: MotionEvent): Boolean {
        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                activePointerId = ev.getPointerId(0)
                initialTouchRawX = ev.rawX
                initialTouchRawY = ev.rawY
                initialWindowX = panelParams.x
                initialWindowY = panelParams.y
                isDragging = false
                touchStartedInHeaderDragZone = isPointInHeaderDragZone(ev.x, ev.y)
                // If touch started in header drag zone and children (ComposeView) did not consume it,
                // claim the gesture so Android will deliver subsequent ACTION_MOVE and ACTION_UP events.
                return touchStartedInHeaderDragZone
            }
            MotionEvent.ACTION_MOVE -> {
                if (touchStartedInHeaderDragZone) {
                    val dx = ev.rawX - initialTouchRawX
                    val dy = ev.rawY - initialTouchRawY
                    if (!isDragging && (Math.abs(dx) > touchSlop || Math.abs(dy) > touchSlop)) {
                        isDragging = true
                    }
                    if (isDragging) {
                        val metrics = resources.displayMetrics
                        val panelWidth = if (width > 0) width else panelParams.width
                        val panelHeight = if (height > 0) height else (220 * density).toInt()
                        val maxX = (metrics.widthPixels - panelWidth).coerceAtLeast(0)
                        val maxY = (metrics.heightPixels - panelHeight).coerceAtLeast(0)
                        panelParams.x = (initialWindowX + dx.toInt()).coerceIn(0, maxX)
                        panelParams.y = (initialWindowY + dy.toInt()).coerceIn(0, maxY)
                        try {
                            if (isAttachedToWindow) {
                                windowManager.updateViewLayout(this, panelParams)
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "[Drag] Error updating panel position: ${e.message}")
                        }
                        return true
                    }
                }
            }
            MotionEvent.ACTION_POINTER_UP -> {
                val pointerIndex = ev.actionIndex
                if (ev.getPointerId(pointerIndex) == activePointerId) {
                    val wasDragging = isDragging
                    resetDragState()
                    if (wasDragging) return true
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                val wasDragging = isDragging
                resetDragState()
                if (wasDragging) {
                    return true
                }
            }
        }
        return super.onTouchEvent(ev)
    }
}
