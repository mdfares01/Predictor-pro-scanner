package com.proscanner.app.service

import android.content.Context
import android.util.Log
import android.view.MotionEvent
import android.view.ViewConfiguration
import android.view.WindowManager
import android.widget.FrameLayout

/**
 * Custom FrameLayout overlay container that provides smooth, reliable dragging of the
 * expanded scanner panel via its header area while ensuring 100% immediate touch dispatch
 * to child Compose buttons (Scan, Settings, Minimize, Close, Stop, Retry).
 */
class DraggablePanelLayout(
    context: Context,
    private val windowManager: WindowManager,
    private val panelParams: WindowManager.LayoutParams,
    private val onOutsideClick: () -> Unit
) : FrameLayout(context) {

    companion object {
        private const val TAG = "DraggablePanelLayout"
    }

    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop
    // Header drag zone: Top 60dp, excluding the rightmost 112dp containing 48dp Minimize & Close buttons
    private val headerHeightPx = (60 * context.resources.displayMetrics.density).toInt()
    private val controlsWidthPx = (112 * context.resources.displayMetrics.density).toInt()

    private var initialTouchX = 0f
    private var initialTouchY = 0f
    private var initialWindowX = 0
    private var initialWindowY = 0
    private var isDragging = false
    private var isInDragRegion = false

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        if (ev.action == MotionEvent.ACTION_OUTSIDE) {
            onOutsideClick()
            return true
        }
        return super.dispatchTouchEvent(ev)
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                initialTouchX = ev.rawX
                initialTouchY = ev.rawY
                initialWindowX = panelParams.x
                initialWindowY = panelParams.y
                isDragging = false
                // Only allow drag initiation if touch starts strictly in the header drag area
                // and excludes the top-right window controls (Minimize and Close buttons).
                val effectiveWidth = if (width > 0) width else panelParams.width.coerceAtLeast(300)
                isInDragRegion = (ev.y in 0f..headerHeightPx.toFloat()) && (ev.x >= 0f && ev.x < (effectiveWidth - controlsWidthPx))
            }
            MotionEvent.ACTION_MOVE -> {
                if (isInDragRegion && !isDragging) {
                    val dx = Math.abs(ev.rawX - initialTouchX)
                    val dy = Math.abs(ev.rawY - initialTouchY)
                    if (dx > touchSlop || dy > touchSlop) {
                        isDragging = true
                        parent?.requestDisallowInterceptTouchEvent(true)
                        return true // Intercept the drag gesture
                    }
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                isDragging = false
                isInDragRegion = false
            }
        }
        // If not dragging in header, pass all events directly to child views without delay
        return isDragging
    }

    override fun onTouchEvent(ev: MotionEvent): Boolean {
        when (ev.actionMasked) {
            MotionEvent.ACTION_MOVE -> {
                if (isDragging) {
                    val dx = (ev.rawX - initialTouchX).toInt()
                    val dy = (ev.rawY - initialTouchY).toInt()
                    val metrics = resources.displayMetrics
                    val panelWidth = if (width > 0) width else panelParams.width
                    val panelHeight = if (height > 0) height else panelParams.height
                    val maxX = (metrics.widthPixels - panelWidth).coerceAtLeast(0)
                    val maxY = (metrics.heightPixels - panelHeight.coerceAtLeast(150)).coerceAtLeast(0)
                    panelParams.x = (initialWindowX + dx).coerceIn(0, maxX)
                    panelParams.y = (initialWindowY + dy).coerceIn(0, maxY)
                    try {
                        if (isAttachedToWindow) {
                            windowManager.updateViewLayout(this, panelParams)
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error updating panel position: ${e.message}")
                    }
                    return true
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                isDragging = false
                isInDragRegion = false
                return true
            }
        }
        return super.onTouchEvent(ev)
    }
}
