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

// Враг теперь тоже танк
class EnemyTank(var x: Float, var y: Float) {
    var angle = 0f
    var turretAngle = 0f
    var isDead = false
    var lastShotTime = 0L
    val fireRate = 2000L // Враги стреляют медленнее
}

class Bullet(var x: Float, var y: Float, var dx: Float, var dy: Float, var isEnemy: Boolean)

class GameView(context: Context) : View(context) {
    private var tankX = 500f
    private var tankY = 500f
    private var tankAngle = 0f
    private var turretAngle = 0f
    private var targetX = 500f
    private var targetY = 500f
    private var isMoving = false
    private var isSelected = false // Выделен ли наш танк
    private var lastShotTime = 0L

    // Настройки (замедлено по просьбе)
    private val speed = 3f // Было 6f
    private val turretSpeed = 0.04f // Было 0.2f (теперь башня крутится медленно)
    private val fireRate = 1000L // Наш танк стреляет раз в секунду
    private val visionRadius = 350f

    private val paint = Paint()
    private val gridPaint = Paint().apply {
        color = Color.parseColor("#1E3A1E")
        strokeWidth = 2f
    }

    private val enemies = mutableListOf<EnemyTank>()
    private val bullets = mutableListOf<Bullet>()

    init {
        for (i in 0..5) {
            enemies.add(EnemyTank(Random.nextFloat() * 1500f + 100f, Random.nextFloat() * 800f + 100f))
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

        // 2. Враги (серые танки)
        for (enemy in enemies) {
            if (!enemy.isDead) {
                canvas.save()
                canvas.translate(enemy.x, enemy.y)
                canvas.rotate(enemy.angle)
                drawTank(canvas, isEnemy = true, isSelected = false)
                canvas.restore()
            }
        }

        // 3. Наш танк
        canvas.save()
        canvas.translate(tankX, tankY)
        // Корпус
        canvas.save()
        canvas.rotate(tankAngle)
        drawTankHull(canvas, isSelected)
        canvas.restore()
        // Башня
        canvas.save()
        canvas.rotate(turretAngle)
        drawTankTurret(canvas)
        canvas.restore()
        canvas.restore()

        // 4. Пули
        for (bullet in bullets) {
            paint.color = if (bullet.isEnemy) Color.RED else Color.YELLOW
            canvas.drawCircle(bullet.x, bullet.y, 5f, paint)
        }

        // 5. Туман
        val gradientPaint = Paint().apply {
            shader = android.graphics.RadialGradient(
                tankX, tankY, visionRadius,
                intArrayOf(Color.TRANSPARENT, Color.TRANSPARENT, Color.BLACK),
                floatArrayOf(0.0f, 0.8f, 1.0f),
                android.graphics.Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), gradientPaint)

        updateGame()
    }

    // Рисуем танк (используется и для врагов, и для нас)
    private fun drawTank(canvas: Canvas, isEnemy: Boolean, isSelected: Boolean) {
        val px = 12f
        val hullColor = if (isEnemy) Color.parseColor("#757575") else Color.parseColor("#4B5320") // Серый или оливковый
        val turretColor = if (isEnemy) Color.parseColor("#9E9E9E") else Color.parseColor("#5C6B2E")
        val gunColor = if (isEnemy) Color.parseColor("#424242") else Color.parseColor("#3A401A")

        // Гусеницы
        paint.color = Color.parseColor("#1A1A1A")
        canvas.drawRect(-5 * px, -6 * px, -3 * px, 6 * px, paint)
        canvas.drawRect(3 * px, -6 * px, 5 * px, 6 * px, paint)

        // Корпус
        paint.color = hullColor
        canvas.drawRect(-3 * px, -5 * px, 3 * px, 5 * px, paint)
        paint.style = Paint.Style.STROKE
        paint.color = Color.BLACK
        paint.strokeWidth = 4f
        canvas.drawRect(-3 * px, -5 * px, 3 * px, 5 * px, paint)
        paint.style = Paint.Style.FILL

        // Башня
        paint.color = turretColor
        canvas.drawCircle(0f, 0f, 2.5f * px, paint)
        paint.style = Paint.Style.STROKE
        paint.color = Color.BLACK
        canvas.drawCircle(0f, 0f, 2.5f * px, paint)
        paint.style = Paint.Style.FILL

        // Пушка
        paint.color = gunColor
        canvas.drawRect(-0.5f * px, -7 * px, 0.5f * px, -2 * px, paint)
        paint.style = Paint.Style.STROKE
        paint.color = Color.BLACK
        canvas.drawRect(-0.5f * px, -7 * px, 0.5f * px, -2 * px, paint)
        paint.style = Paint.Style.FILL
    }

    private fun drawTankHull(canvas: Canvas, isSelected: Boolean) {
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

        if (isSelected) {
            paint.style = Paint.Style.STROKE
            paint.color = Color.YELLOW
            paint.strokeWidth = 4f
            canvas.drawRect(-7 * px, -8 * px, 7 * px, 8 * px, paint)
            paint.style = Paint.Style.FILL
        }
    }

    private fun drawTankTurret(canvas: Canvas) {
        val px = 12f
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
        // 1. Движение танка
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

                tankX += (dx / distance) * speed
                tankY += (dy / distance) * speed
            } else {
                isMoving = false
            }
        }

        // 2. Поиск цели для нашего танка
        var closestEnemy: EnemyTank? = null
        var minDist = visionRadius

        for (enemy in enemies) {
            if (enemy.isDead) continue
            val dist = sqrt((enemy.x - tankX) * (enemy.x - tankX) + (enemy.y - tankY) * (enemy.y - tankY))
            if (dist < minDist) {
                minDist = dist
                closestEnemy = enemy
            }
        }

        if (closestEnemy != null) {
            val angleToTarget = Math.toDegrees(atan2((closestEnemy.y - tankY).toDouble(), (closestEnemy.x - tankX).toDouble())).toFloat() + 90f
            var diff = angleToTarget - turretAngle
            while (diff > 180) diff -= 360f
            while (diff < -180) diff += 360f
            turretAngle += diff * turretSpeed // Замедленный поворот башни

            if (System.currentTimeMillis() - lastShotTime > fireRate && Math.abs(diff) < 5f) {
                // СТРЕЛЬБА С КОНЦА ПУШКИ
                val rad = Math.toRadians(turretAngle.toDouble() - 90.0)
                // Смещение на кончик ствола (примерно 7 * 12 = 84 пикселя)
                val gunLength = 84f
                val startX = tankX + cos(rad).toFloat() * gunLength
                val startY = tankY + sin(rad).toFloat() * gunLength
                
                bullets.add(Bullet(startX, startY, cos(rad).toFloat() * 15f, sin(rad).toFloat() * 15f, false))
                lastShotTime = System.currentTimeMillis()
            }
        } else {
            var diff = tankAngle - turretAngle
            while (diff > 180) diff -= 360f
            while (diff < -180) diff += 360f
            turretAngle += diff * 0.05f
        }

        // 3. Враги стреляют (если мы близко)
        for (enemy in enemies) {
            if (enemy.isDead) continue
            val distToTank = sqrt((tankX - enemy.x) * (tankX - enemy.x) + (tankY - enemy.y) * (tankY - enemy.y))
            if (distToTank < 400f && System.currentTimeMillis() - enemy.lastShotTime > enemy.fireRate) {
                // Наводим башню врага на нас
                val angleToTank = Math.toDegrees(atan2((tankY - enemy.y).toDouble(), (tankX - enemy.x).toDouble())).toFloat() + 90f
                var diff = angleToTank - enemy.turretAngle
                while (diff > 180) diff -= 360f
                while (diff < -180) diff += 360f
                enemy.turretAngle += diff * 0.05f

                if (Math.abs(diff) < 10f) {
                    val rad = Math.toRadians(enemy.turretAngle.toDouble() - 90.0)
                    val gunLength = 84f
                    val startX = enemy.x + cos(rad).toFloat() * gunLength
                    val startY = enemy.y + sin(rad).toFloat() * gunLength
                    bullets.add(Bullet(startX, startY, cos(rad).toFloat() * 15f, sin(rad).toFloat() * 15f, true))
                    enemy.lastShotTime = System.currentTimeMillis()
                }
            }
        }

        // 4. Движение пуль
        val bulletsToRemove = mutableListOf<Bullet>()
        for (bullet in bullets) {
            bullet.x += bullet.dx
            bullet.y += bullet.dy

            if (bullet.x < 0 || bullet.x > width || bullet.y < 0 || bullet.y > height) {
                bulletsToRemove.add(bullet)
            } else if (bullet.isEnemy) {
                val dist = sqrt((tankX - bullet.x) * (tankX - bullet.x) + (tankY - bullet.y) * (tankY - bullet.y))
                if (dist < 30f) bulletsToRemove.add(bullet)
            } else {
                for (enemy in enemies) {
                    if (enemy.isDead) continue
                    val dist = sqrt((enemy.x - bullet.x) * (enemy.x - bullet.x) + (enemy.y - bullet.y) * (enemy.y - bullet.y))
                    if (dist < 30f) {
                        enemy.isDead = true
                        bulletsToRemove.add(bullet)
                        break
                    }
                }
            }
        }
        bullets.removeAll(bulletsToRemove)
        
        invalidate()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN) {
            val touchX = event.x
            val touchY = event.y

            // 1. Проверяем, нажали ли на наш танк
            if (sqrt((touchX - tankX) * (touchX - tankX) + (touchY - tankY) * (touchY - tankY)) < 60f) {
                isSelected = !isSelected // Переключаем выделение (тап по танку)
            } else if (isSelected) {
                // Если танк выделен и нажали на карту - едем
                targetX = touchX
                targetY = touchY
                isMoving = true
            }
            invalidate()
            return true
        }
        return super.onTouchEvent(event)
    }
}
