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
    private var turretAngle = 0f // Угол башни (отдельно от корпуса)
    private var targetX = 500f
    private var targetY = 500f
    private var isMoving = false
    private var lastShotTime = 0L

    // Настройки танка
    private val visionRadius = 350f // Радиус обзора (и стрельбы)
    private val fireRate = 800L // Скорострельность (мс)

    private val paint = Paint()
    private val gridPaint = Paint().apply {
        color = Color.parseColor("#1E3A1E")
        strokeWidth = 2f
    }

    class Enemy(var x: Float, var y: Float) {
        var isDead = false
    }
    private val enemies = mutableListOf<Enemy>()

    class Bullet(var x: Float, var y: Float, var dx: Float, var dy: Float, var isEnemy: Boolean)
    private val bullets = mutableListOf<Bullet>()

    init {
        for (i in 0..6) {
            enemies.add(Enemy(Random.nextFloat() * 1500f + 100f, Random.nextFloat() * 800f + 100f))
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

        // 3. Танк (корпус и башня отдельно)
        canvas.save()
        canvas.translate(tankX, tankY)
        
        // Рисуем корпус (поворачивается при движении)
        canvas.save()
        canvas.rotate(tankAngle)
        drawTankHull(canvas)
        canvas.restore()
        
        // Рисуем башню (поворачивается к цели)
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

        // 5. Туман войны
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

    private fun drawEnemy(canvas: Canvas, enemy: Enemy) {
        val px = 10f
        paint.color = Color.parseColor("#8B0000")
        canvas.save()
        canvas.translate(enemy.x, enemy.y)
        canvas.drawRect(-3 * px, -3 * px, 3 * px, 3 * px, paint)
        paint.color = Color.BLACK
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 3f
        canvas.drawRect(-3 * px, -3 * px, 3 * px, 3 * px, paint)
        paint.style = Paint.Style.FILL
        canvas.restore()
    }

    private fun drawTankHull(canvas: Canvas) {
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
    }

    private fun drawTankTurret(canvas: Canvas) {
        val px = 12f
        // Башня
        paint.color = Color.parseColor("#5C6B2E")
        canvas.drawCircle(0f, 0f, 2.5f * px, paint)
        paint.style = Paint.Style.STROKE
        paint.color = Color.BLACK
        canvas.drawCircle(0f, 0f, 2.5f * px, paint)
        paint.style = Paint.Style.FILL

        // Пушка
        paint.color = Color.parseColor("#3A401A")
        canvas.drawRect(-0.5f * px, -7 * px, 0.5f * px, -2 * px, paint)
        paint.style = Paint.Style.STROKE
        paint.color = Color.BLACK
        canvas.drawRect(-0.5f * px, -7 * px, 0.5f * px, -2 * px, paint)
        paint.style = Paint.Style.FILL
    }

    private fun updateGame() {
        // 1. Движение корпуса
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

                val speed = 6f
                tankX += (dx / distance) * speed
                tankY += (dy / distance) * speed
            } else {
                isMoving = false
            }
        }

        // 2. Поиск цели и авто-стрельба
        var closestEnemy: Enemy? = null
        var minDist = visionRadius

        for (enemy in enemies) {
            if (enemy.isDead) continue
            val dist = sqrt((enemy.x - tankX) * (enemy.x - tankX) + (enemy.y - tankY) * (enemy.y - tankY))
            if (dist < minDist) {
                minDist = dist
                closestEnemy = enemy
            }
        }

        // Если есть цель - поворачиваем башню и стреляем
        if (closestEnemy != null) {
            val angleToTarget = Math.toDegrees(atan2((closestEnemy.y - tankY).toDouble(), (closestEnemy.x - tankX).toDouble())).toFloat() + 90f
            var diff = angleToTarget - turretAngle
            while (diff > 180) diff -= 360f
            while (diff < -180) diff += 360f
            turretAngle += diff * 0.2f // Башня поворачивается быстрее

            // Стрельба
            if (System.currentTimeMillis() - lastShotTime > fireRate && Math.abs(diff) < 10f) {
                val rad = Math.toRadians(turretAngle.toDouble() - 90.0)
                bullets.add(Bullet(tankX, tankY, cos(rad).toFloat() * 15f, sin(rad).toFloat() * 15f, false))
                lastShotTime = System.currentTimeMillis()
            }
        } else {
            // Если цели нет - башня смотрит туда же, куда едет корпус
            var diff = tankAngle - turretAngle
            while (diff > 180) diff -= 360f
            while (diff < -180) diff += 360f
            turretAngle += diff * 0.1f
        }

        // 3. Движение пуль
        val bulletsToRemove = mutableListOf<Bullet>()
        for (bullet in bullets) {
            bullet.x += bullet.dx
            bullet.y += bullet.dy

            if (bullet.x < 0 || bullet.x > width || bullet.y < 0 || bullet.y > height) {
                bulletsToRemove.add(bullet)
            } 
            // Попадание в танк
            else if (bullet.isEnemy) {
                val dist = sqrt((tankX - bullet.x) * (tankX - bullet.x) + (tankY - bullet.y) * (tankY - bullet.y))
                if (dist < 40f) bulletsToRemove.add(bullet)
            }
            // Попадание в врага (наши пули)
            else {
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
            // Просто едем в точку нажатия
            targetX = event.x
            targetY = event.y
            isMoving = true
            invalidate()
            return true
        }
        return super.onTouchEvent(event)
    }
}
