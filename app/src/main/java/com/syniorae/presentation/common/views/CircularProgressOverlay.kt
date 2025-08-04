package com.syniorae.presentation.common.views

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator
import androidx.core.content.ContextCompat
import com.syniorae.R

/**
 * Vue personnalisée pour afficher un cercle de progression lors de l'appui long
 * S'affiche au-dessus de l'icône paramètre
 */
class CircularProgressOverlay @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val progressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 8f
        strokeCap = Paint.Cap.ROUND
        color = ContextCompat.getColor(context, R.color.progress_color)
    }

    private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 8f
        color = ContextCompat.getColor(context, R.color.progress_background)
        alpha = 100
    }

    private val centerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = ContextCompat.getColor(context, R.color.progress_center)
        alpha = 50
    }

    private var progress = 0f
        set(value) {
            field = value.coerceIn(0f, 1f)
            invalidate()
        }

    private var isVisible = false
        set(value) {
            field = value
            visibility = if (value) VISIBLE else GONE
        }

    private val rect = RectF()
    private var centerX = 0f
    private var centerY = 0f
    private var radius = 0f

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)

        centerX = w / 2f
        centerY = h / 2f
        radius = minOf(w, h) / 2f - progressPaint.strokeWidth

        rect.set(
            centerX - radius,
            centerY - radius,
            centerX + radius,
            centerY + radius
        )
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (!isVisible || progress <= 0f) return

        // Dessiner le cercle de fond
        canvas.drawCircle(centerX, centerY, radius + 4f, centerPaint)
        canvas.drawCircle(centerX, centerY, radius, backgroundPaint)

        // Dessiner le progrès
        val sweepAngle = 360f * progress
        canvas.drawArc(rect, -90f, sweepAngle, false, progressPaint)
    }

    /**
     * Met à jour le progrès (0.0 à 1.0)
     */
    fun updateProgress(newProgress: Float) {
        progress = newProgress

        if (newProgress > 0f && !isVisible) {
            show()
        } else if (newProgress <= 0f && isVisible) {
            hide()
        }
    }

    /**
     * Affiche l'overlay
     */
    fun show() {
        isVisible = true
        alpha = 0f
        animate()
            .alpha(1f)
            .setDuration(150)
            .start()
    }

    /**
     * Masque l'overlay
     */
    fun hide() {
        animate()
            .alpha(0f)
            .setDuration(150)
            .withEndAction {
                isVisible = false
                progress = 0f
            }
            .start()
    }

    /**
     * Anime le progrès vers une valeur cible
     */
    fun animateProgressTo(targetProgress: Float, duration: Long = 100) {
        val currentProgress = progress

        ValueAnimator.ofFloat(currentProgress, targetProgress).apply {
            this.duration = duration
            interpolator = LinearInterpolator()
            addUpdateListener { animator ->
                updateProgress(animator.animatedValue as Float)
            }
            start()
        }
    }

    /**
     * Reset l'overlay
     */
    fun reset() {
        progress = 0f
        isVisible = false
        clearAnimation()
    }
}