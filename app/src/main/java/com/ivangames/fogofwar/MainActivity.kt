package com.ivangames.fogofwar

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(GameView(this))
    }
}

class GameView(context: Context) : View(context) {
    private var tankX = 500f
    private var tankY = 500f
    private var tankAngle = 0f
    private var targetX = 500f
    private var targetY = 500f
    private var isMoving = false

    private val paint = Paint()
    private val gridPaint = Paint().apply {
        color = Color.parseColor("#1E3A1E")
        strokeWidth = 2f
    }
    
    // Настройки тумана
    private val fogPaint = Paint().apply {
        color = Color.BLACK
        style = Paint.Style.FILL
    }
    private val clearPaint = Paint().apply {
        xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
        style = Paint.Style.FILL
    }
    private val visionRadius = 250f // Радиус обзора танка

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // 1. Рисуем карту (трава + сетка)
        canvas.drawColor(Color.parseColor("#2E7D32"))
        val gridSize = 100f
        for (x in 0..width step gridSize.toInt()) {
            canvas.drawLine(x.toFloat(), 0f, x.toFloat(), height.toFloat(), gridPaint)
        }
        for (y in 0..height step gridSize.toInt()) {
            canvas.drawLine(0f, y.toFloat(), width.toFloat(), y.toFloat(), gridPaint)
        }

        // 2. Рисуем танк
        canvas.save()
        canvas.translate(tankX, tankY)
        canvas.rotate(tankAngle)
        drawTank(canvas)
        canvas.restore()

        // 3. Рисуем Туман Войны (поверх всего)
        // Создаем слой для тумана
        val fogLayer = Canvas()
        // (В Android без Bitmap это сделать сложно, поэтому используем трюк с Xfermode)
        
        // Рисуем черный прямоугольник на весь экран
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), fogPaint)
        
        // "Стираем" черноту вокруг танка (делаем круг прозрачным)
        // Для этого используем слой. Но так как мы рисуем напрямую, 
        // используем простой трюк: рисуем градиентные круги от прозрачного к черному.
        
        // (Это упрощенный вариант, чтобы не усложнять код Bitmap'ами)
        val gradientPaint = Paint().apply {
            shader = android.graphics.RadialGradient(
                tankX, tankY, visionRadius,
                intArrayOf(Color.TRANSPARENT, Color.TRANSPARENT, Color.BLACK),
                floatArrayOf(0.0f, 0.7f, 1.0f),
                android.graphics.Shader.TileMode.CLAMP
            )
        }
        canvas.drawCircle(tankX, tankY, visionRadius, gradientPaint)

        // 4. Логика движения
        if (isMoving) {
            val dx = targetX - tankX
            val dy = targetY - tankY
            val distance = sqrt(dx * dx + dy * dy)

            if (distance > 10f) {
                val targetAngle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat() + 90f
                var diff = targetAngle - tankAngle
                while (diff > 180) diff -= 360f
                while (diff < -180) diff += 360f
                tankAngle += diff * 0.1f

                val speed = 8f
                tankX += (dx / distance) * speed
                tankY += (dy / distance) * speed
                
                invalidate()
            } else {
                isMoving = false
            }
        }
    }

    private fun drawTank(canvas: Canvas) {
        val px = 12f
        paint.color = Color.parseColor("#1A1A1A")
        canvas.drawRect(-5 * px, -6 * px, -3 * px, 6 * px, paint)
        canvas.drawRect(3 * px, -6 * px, 5 * px, 6 * px, paint)

        paint.color = Color.parseColor("#4B5320")
        canvas.drawRect(-3 * px, -5 * px, 3 * px, 5 * px, paint)
        
        paint.style = Paint.Style.STROKE
        paint.color = Color.BLACK
        paint.strokeWidth = 4f
        canvas.drawRect(-3 * px, -5 * px, 3 * px, 5 * px, paint)
        paint.style = Paint.Style.FILL

        paint.color = Color.parseColor("#5C6B2E")
        canvas.drawCircle(0f, 0f, 2.5f * px, paint)
        paint.style = Paint.Style.STROKE
        paint.color = Color.BLACK
        canvas.drawCircle(0f, 0f, 2.5f * px, paint)
        paint.style = Paint.Style.FILL

        paint.color = Color.parseColor("#3A401A")
        canvas.drawRect(-0.5f * px, -7 * px, 0.5f * px, -2 * px, paint)
        paint.style = Paint.Style.STROKE
        paint.color = Color.BLACK
        canvas.drawRect(-0.5f * px, -7 * px, 0.5f * px, -2 * px, paint)
        paint.style = Paint.Style.FILL
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN) {
            targetX = event.x
            targetY = event.y
            isMoving = true
            invalidate()
            return true
        }
        return super.onTouchEvent(event)
    }
}
