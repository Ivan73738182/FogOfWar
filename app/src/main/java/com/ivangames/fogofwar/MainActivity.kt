package com.ivangames.fogofwar

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

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
    private var lastShotTime = 0L

    private val paint = Paint()
    private val gridPaint = Paint().apply {
        color = Color.parseColor("#1E3A1E")
        strokeWidth = 2f
    }

    // Враги
    class Enemy(var x: Float, var y: Float, var patrolAngle: Float) {
        var patrolTime = 0f
        var isDead = false
    }
    private val enemies = mutableListOf<Enemy>()

    // Пули
    class Bullet(var x: Float, var y: Float, var dx: Float, var dy: Float, var isEnemy: Boolean)
    private val bullets = mutableListOf<Bullet>()

    // Обзор
    private val fogPaint = Paint().apply { color = Color.BLACK }
    private val visionRadius = 250f

    init {
        // Создаем врагов в случайных местах
        for (i in 0..4) {
            enemies.add(Enemy(Random.nextFloat() * 1000f + 100f, Random.nextFloat() * 500f + 100f, Random.nextFloat() * 360f))
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // 1. Карта
        canvas.drawColor(Color.parseColor("#2E7D32"))
        val gridSize = 100f
        for (x in 0..width step gridSize.toInt()) {
            canvas.drawLine(x.toFloat(), 0f, x.toFloat(), height.toFloat(), gridPaint)
        }
        for (y in 0..height step gridSize.toInt()) {
            canvas.drawLine(0f, y.toFloat(), width.toFloat(), y.toFloat(), gridPaint)
        }

        // 2. Враги
        for (enemy in enemies) {
            if (!enemy.isDead) {
                drawEnemy(canvas, enemy)
            }
        }

        // 3. Танк
        canvas.save()
        canvas.translate(tankX, tankY)
        canvas.rotate(tankAngle)
        drawTank(canvas)
        canvas.restore()

        // 4. Пули
        for (bullet in bullets) {
            paint.color = if (bullet.isEnemy) Color.RED else Color.YELLOW
            canvas.drawCircle(bullet.x, bullet.y, 5f, paint)
        }

        // 5. Туман войны (градиент)
        val gradientPaint = Paint().apply {
            shader = android.graphics.RadialGradient(
                tankX, tankY, visionRadius,
                intArrayOf(Color.TRANSPARENT, Color.TRANSPARENT, Color.BLACK),
                floatArrayOf(0.0f, 0.7f, 1.0f),
                android.graphics.Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), gradientPaint)

        // 6. Логика
        updateGame()
    }

    private fun drawEnemy(canvas: Canvas, enemy: Enemy) {
        val px = 10f
        paint.color = Color.parseColor("#8B0000") // Темно-красный
        canvas.save()
        canvas.translate(enemy.x, enemy.y)
        canvas.rotate(enemy.patrolAngle)
        canvas.drawRect(-3 * px, -3 * px, 3 * px, 3 * px, paint)
        paint.color = Color.BLACK
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 3f
        canvas.drawRect(-3 * px, -3 * px, 3 * px, 3 * px, paint)
        paint.style = Paint.Style.FILL
        canvas.restore()
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

    private fun updateGame() {
        // Движение танка
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
            } else {
                isMoving = false
            }
        }

        // Патрулирование и стрельба врагов
        for (enemy in enemies) {
            if (enemy.isDead) continue
            
            enemy.patrolTime += 0.05f
            enemy.patrolAngle += sin(enemy.patrolTime) * 2f

            // Враг стреляет, если танк близко
            val distToTank = sqrt((tankX - enemy.x) * (tankX - enemy.x) + (tankY - enemy.y) * (tankY - enemy.y))
            if (distToTank < 300f && System.currentTimeMillis() - lastShotTime > 1000) {
                val angleToTank = atan2((tankY - enemy.y).toDouble(), (tankX - enemy.x).toDouble())
                bullets.add(Bullet(enemy.x, enemy.y, cos(angleToTank).toFloat() * 10f, sin(angleToTank).toFloat() * 10f, true))
                lastShotTime = System.currentTimeMillis()
            }
        }

        // Движение пуль и проверка попаданий
        val bulletsToRemove = mutableListOf<Bullet>()
        for (bullet in bullets) {
            bullet.x += bullet.dx
            bullet.y += bullet.dy

            // Проверка выхода за экран
            if (bullet.x < 0 || bullet.x > width || bullet.y < 0 || bullet.y > height) {
                bulletsToRemove.add(bullet)
            } 
            // Попадание в танк (вражеская пуля)
            else if (bullet.isEnemy) {
                val dist = sqrt((tankX - bullet.x) * (tankX - bullet.x) + (tankY - bullet.y) * (tankY - bullet.y))
                if (dist < 40f) {
                    bulletsToRemove.add(bullet)
                    // Тут можно добавить уменьшение здоровья
                }
            }
        }
        bullets.removeAll(bulletsToRemove)
        
        invalidate()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN) {
            // Если нажали на врага - стреляем в него
            var shotFired = false
            for (enemy in enemies) {
                if (enemy.isDead) continue
                val dist = sqrt((event.x - enemy.x) * (event.x - enemy.x) + (event.y - enemy.y) * (event.y - enemy.y))
                if (dist < 60f) {
                    // Стреляем
                    val angle = atan2((enemy.y - tankY).toDouble(), (enemy.x - tankX).toDouble())
                    bullets.add(Bullet(tankX, tankY, cos(angle).toFloat() * 20f, sin(angle).toFloat() * 20f, false))
                    shotFired = true
                    break
                }
            }
            
            // Если не стреляли - просто едем
            if (!shotFired) {
                targetX = event.x
                targetY = event.y
                isMoving = true
            }
            invalidate()
            return true
        }
        return super.onTouchEvent(event)
    }
}
