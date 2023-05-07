package com.hrd.localvoice.view.customviews

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.util.AttributeSet
import android.view.View
import androidx.appcompat.widget.AppCompatEditText


class LinedEditText(context: Context, attrs: AttributeSet?) :
    AppCompatEditText(context, attrs) {
    private val rect: Rect = Rect()
    private val paint: Paint = Paint()

    init {
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 2f
        paint.color = -0x11111112
    }

    override fun onDraw(canvas: Canvas) {
        val count = lineCount
        val height = (this.parent as View).height
        val lineHeight = lineHeight
        var numberOfLines = height / lineHeight
        if (count > numberOfLines) numberOfLines = count
        var baseLine = getLineBounds(0, rect)
        for (i in 0 until numberOfLines) {
            canvas.drawLine(
                rect.left.toFloat(),
                (baseLine + 1).toFloat(),
                rect.right.toFloat(),
                (baseLine + 1).toFloat(),
                paint
            )
            baseLine += lineHeight
        }
        super.onDraw(canvas)
    }
}