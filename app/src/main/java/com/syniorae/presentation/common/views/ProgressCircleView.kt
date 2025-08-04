package com.syniorae.presentation.common.views

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import kotlin.math.min

/**
 * Vue personnalisée pour afficher un cercle de progression
 * Utilisée pour l'appui long sur l'icône paramètre
 */
class ProgressCircleView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var progress: Float = 0f

    private val paint = Paint().apply {
        isAntiAlias = true
        strokeWidth = 12f
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }

    private val backgroundPaint = Paint().apply {
        isAntiAlias = true
        strokeWidth = 12f
        style = Paint.Style.STROKE
        color = Color.parseColor("#E0E0E0")
        alpha = 80
    }

    private val progressPaint = Paint().apply {
        isAntiAlias = true
        strokeWidth = 12f
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        color = Color.parseColor("#2196F3")
    }

    private val textPaint = Paint().apply {
        isAntiAlias = true
        color = Color.parseColor("#2196F3")
        textSize = 48f
        textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT_BOLD
    }

    private val rect = RectF()

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val centerX = width / 2f
        val centerY = height / 2f
        val radius = min(width, height) / 2f - paint.strokeWidth

        rect.set(
            centerX - radius,
            centerY - radius,
            centerX + radius,
            centerY + radius
        )

        // Dessine le cercle de fond
        canvas.drawCircle(centerX, centerY, radius, backgroundPaint)

        // Dessine le progrès
        if (progress > 0f) {
            val sweepAngle = progress * 360f
            canvas.drawArc(rect, -90f, sweepAngle, false, progressPaint)
        }
    }

    /**
     * Met à jour le progrès (0.0 à 1.0)
     */
    fun setProgress(progress: Float) {
        this.progress = progress.coerceIn(0f, 1f)
        invalidate()
    }

    /**
     * Remet le progrès à zéro
     */
    fun reset() {
        progress = 0f
    }
}