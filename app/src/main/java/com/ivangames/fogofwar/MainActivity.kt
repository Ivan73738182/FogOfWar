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

// Базовый класс для наших юнитов
open class Unit(var x: Float, var y: Float) {
    var targetX = x
    var targetY = y
    var isMoving = false
    var angle = 0f
    var isDead = false
    var isSelected = false
    var lastShotTime = 0L
    var fireRate = 1000L
    var visionRadius = 300f
}

class Tank(x: Float, y: Float) : Unit(x, y) {
    var turretAngle = 0f
    init {
        fireRate = 1000L
        visionRadius = 350f
    }
}

class Helicopter(x: Float, y: Float) : Unit(x, y) {
    var rotorAngle = 0f // Угол вращения лопастей
    init {
        fireRate = 300L // Вертолет стреляет очередями
        visionRadius = 400f // У него лучший обзор
    }
}

class EnemyTank(var x: Float, var y: Float) {
    var angle = 0f
    var turretAngle = 0f
    var isDead = false
    var lastShotTime = 0L
    val fireRate = 2000L
}

class Bullet(var x: Float, var y: Float, var dx: Float, var dy: Float, var isEnemy: Boolean)

class GameView(context: Context) : View(context) {
    private val tank = Tank(500f, 500f)
    private val helicopter = Helicopter(600f, 600f)
    private val units = listOf(tank, helicopter)
    
    // Выбранный юнит (по умолчанию танк)
    private var selectedUnit: Unit? = tank

    // Настройки
    private val speedTank = 3f
    private val speedHeli = 5f // Вертолет быстрее
    private val turretSpeed = 0.04f

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

        // 2. Враги
        for (enemy in enemies) {
            if (!enemy.isDead) {
                canvas.save()
                canvas.translate(enemy.x, enemy.y)
                canvas.rotate(enemy.angle)
                drawEnemyTank(canvas)
                canvas.restore()
            }
        }

        // 3. Наши юниты
        // Танк
        if (!tank.isDead) {
            canvas.save()
            canvas.translate(tank.x, tank.y)
            canvas.save()
            canvas.rotate(tank.angle)
            drawTankHull(canvas, tank.isSelected)
            canvas.restore()
            canvas.save()
            canvas.rotate(tank.turretAngle)
            drawTankTurret(canvas)
            canvas.restore()
            canvas.restore()
        }

        // Вертолет
        if (!helicopter.isDead) {
            canvas.save()
            canvas.translate(helicopter.x, helicopter.y)
            canvas.rotate(helicopter.angle)
            drawHelicopter(canvas, helicopter.isSelected, helicopter.rotorAngle)
            canvas.restore()
        }

        // 4. Пули
        for (bullet in bullets) {
            paint.color = if (bullet.isEnemy) Color.RED else Color.YELLOW
            canvas.drawCircle(bullet.x, bullet.y, 5f, paint)
        }

        // 5. Туман (вокруг выбранного юнита)
        val vX = selectedUnit?.x ?: tank.x
        val vY = selectedUnit?.y ?: tank.y
        val vR = selectedUnit?.visionRadius ?: 300f

        val gradientPaint = Paint().apply {
            shader = android.graphics.RadialGradient(
                vX, vY, vR,
                intArrayOf(Color.TRANSPARENT, Color.TRANSPARENT, Color.BLACK),
                floatArrayOf(0.0f, 0.8f, 1.0f),
                android.graphics.Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), gradientPaint)

        updateGame()
    }

    // --- Отрисовка ---

    private fun drawEnemyTank(canvas: Canvas) {
        val px = 12f
        paint.color = Color.parseColor("#1A1A1A")
        canvas.drawRect(-5 * px, -6 * px, -3 * px, 6 * px, paint)
        canvas.drawRect(3 * px, -6 * px, 5 * px, 6 * px, paint)

        paint.color = Color.parseColor("#757575")
        canvas.drawRect(-3 * px, -5 * px, 3 * px, 5 * px, paint)
        paint.style = Paint.Style.STROKE
        paint.color = Color.BLACK
        paint.strokeWidth = 4f
        canvas.drawRect(-3 * px, -5 * px, 3 * px, 5 * px, paint)
        paint.style = Paint.Style.FILL

        paint.color = Color.parseColor("#9E9E9E")
        canvas.drawCircle(0f, 0f, 2.5f * px, paint)
        paint.style = Paint.Style.STROKE
        paint.color = Color.BLACK
        canvas.drawCircle(0f, 0f, 2.5f * px, paint)
        paint.style = Paint.Style.FILL

        paint.color = Color.parseColor("#424242")
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
            paint.strokeWidth = 3f
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

    // Отрисовка вертолета
    private fun drawHelicopter(canvas: Canvas, isSelected: Boolean, rotorAngle: Float) {
        val px = 10f
        
        // Тень (чтобы казалось, что он летит)
        paint.color = Color.argb(100, 0, 0, 0)
        canvas.drawOval(-6 * px, 6 * px, 6 * px, 10 * px, paint)

        // Хвост
        paint.color = Color.parseColor("#3E4A2D")
        canvas.drawRect(-1.5f * px, 0f, 1.5f * px, 6 * px, paint)
        paint.style = Paint.Style.STROKE
        paint.color = Color.BLACK
        paint.strokeWidth = 2f
        canvas.drawRect(-1.5f * px, 0f, 1.5f * px, 6 * px, paint)

        // Хвостовой винт
        paint.style = Paint.Style.FILL
        paint.color = Color.DKGRAY
        canvas.drawRect(-0.5f * px, 5 * px, 0.5f * px, 7 * px, paint)

        // Корпус
        paint.color = Color.parseColor("#4B5320")
        canvas.drawOval(-3 * px, -4 * px, 3 * px, 2 * px, paint)
        paint.style = Paint.Style.STROKE
        paint.color = Color.BLACK
        canvas.drawOval(-3 * px, -4 * px, 3 * px, 2 * px, paint)

        // Кабина (стекло)
        paint.style = Paint.Style.FILL
        paint.color = Color.parseColor("#87CEEB")
        canvas.drawOval(-2 * px, -3 * px, 1 * px, 0f, paint)

        // Лопасти (крутятся)
        canvas.save()
        canvas.translate(0f, -3 * px)
        canvas.rotate(rotorAngle)
        paint.color = Color.DKGRAY
        canvas.drawRect(-8 * px, -0.5f * px, 8 * px, 0.5f * px, paint)
        canvas.drawRect(-0.5f * px, -8 * px, 0.5f * px, 8 * px, paint)
        canvas.restore()

        // Выделение
        if (isSelected) {
            paint.style = Paint.Style.STROKE
            paint.color = Color.YELLOW
            paint.strokeWidth = 3f
            canvas.drawCircle(0f, 0f, 8 * px, paint)
            paint.style = Paint.Style.FILL
        }
    }

    // --- Логика ---

    private fun updateGame() {
        // Анимация винта
        helicopter.rotorAngle += 30f

        // Обновление наших юнитов
        for (unit in units) {
            if (unit.isDead) continue

            // Движение
            if (unit.isMoving) {
                val dx = unit.targetX - unit.x
                val dy = unit.targetY - unit.y
                val distance = sqrt(dx * dx + dy * dy)

                if (distance > 10f) {
                    val targetAngle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat() + 90f
                    var diff = targetAngle - unit.angle
                    while (diff > 180) diff -= 360f
                    while (diff < -180) diff += 360f
                    unit.angle += diff * 0.1f

                    val speed = if (unit is Tank) speedTank else speedHeli
                    unit.x += (dx / distance) * speed
                    unit.y += (dy / distance) * speed
                } else {
                    unit.isMoving = false
                }
            }

            // Стрельба по врагам
            var closestEnemy: EnemyTank? = null
            var minDist = unit.visionRadius
            for (enemy in enemies) {
                if (enemy.isDead) continue
                val dist = sqrt((enemy.x - unit.x) * (enemy.x - unit.x) + (enemy.y - unit.y) * (enemy.y - unit.y))
                if (dist < minDist) {
                    minDist = dist
                    closestEnemy = enemy
                }
            }

            if (closestEnemy != null) {
                val angleToTarget = Math.toDegrees(atan2((closestEnemy.y - unit.y).toDouble(), (closestEnemy.x - unit.x).toDouble())).toFloat() + 90f
                var diff = angleToTarget - unit.angle
                while (diff > 180) diff -= 360f
                while (diff < -180) diff += 360f
                
                if (unit is Tank) {
                    // Танк поворачивает башню
                    var tDiff = angleToTarget - unit.turretAngle
                    while (tDiff > 180) tDiff -= 360f
                    while (tDiff < -180) tDiff += 360f
                    unit.turretAngle += tDiff * turretSpeed

                    if (System.currentTimeMillis() - unit.lastShotTime > unit.fireRate && Math.abs(tDiff) < 5f) {
                        val rad = Math.toRadians(unit.turretAngle.toDouble() - 90.0)
                        val startX = unit.x + cos(rad).toFloat() * 84f
                        val startY = unit.y + sin(rad).toFloat() * 84f
                        bullets.add(Bullet(startX, startY, cos(rad).toFloat() * 15f, sin(rad).toFloat() * 15f, false))
                        unit.lastShotTime = System.currentTimeMillis()
                    }
                } else if (unit is Helicopter) {
                    // Вертолет поворачивается всем корпусом
                    unit.angle += diff * 0.1f
                    if (System.currentTimeMillis() - unit.lastShotTime > unit.fireRate && Math.abs(diff) < 10f) {
                        val rad = Math.toRadians(unit.angle.toDouble() - 90.0)
                        val startX = unit.x + cos(rad).toFloat() * 40f
                        val startY = unit.y + sin(rad).toFloat() * 40f
                        bullets.add(Bullet(startX, startY, cos(rad).toFloat() * 20f, sin(rad).toFloat() * 20f, false))
                        unit.lastShotTime = System.currentTimeMillis()
                    }
                }
            }
        }

        // Враги стреляют
        for (enemy in enemies) {
            if (enemy.isDead) continue
            val distToTank = sqrt((tank.x - enemy.x) * (tank.x - enemy.x) + (tank.y - enemy.y) * (tank.y - enemy.y))
            val distToHeli = sqrt((helicopter.x - enemy.x) * (helicopter.x - enemy.x) + (helicopter.y - enemy.y) * (helicopter.y - enemy.y))
            
            // Враг целится в того, кто ближе
            val targetX = if (distToTank < distToHeli) tank.x else helicopter.x
            val targetY = if (distToTank < distToHeli) tank.y else helicopter.y
            val dist = if (distToTank < distToHeli) distToTank else distToHeli

            if (dist < 400f && System.currentTimeMillis() - enemy.lastShotTime > enemy.fireRate) {
                val angleToTarget = Math.toDegrees(atan2((targetY - enemy.y).toDouble(), (targetX - enemy.x).toDouble())).toFloat() + 90f
                var diff = angleToTarget - enemy.turretAngle
                while (diff > 180) diff -= 360f
                while (diff < -180) diff += 360f
                enemy.turretAngle += diff * 0.05f

                if (Math.abs(diff) < 10f) {
                    val rad = Math.toRadians(enemy.turretAngle.toDouble() - 90.0)
                    val startX = enemy.x + cos(rad).toFloat() * 84f
                    val startY = enemy.y + sin(rad).toFloat() * 84f
                    bullets.add(Bullet(startX, startY, cos(rad).toFloat() * 15f, sin(rad).toFloat() * 15f, true))
                    enemy.lastShotTime = System.currentTimeMillis()
                }
            }
        }

        // Пули
        val bulletsToRemove = mutableListOf<Bullet>()
        for (bullet in bullets) {
            bullet.x += bullet.dx
            bullet.y += bullet.dy

            if (bullet.x < 0 || bullet.x > width || bullet.y < 0 || bullet.y > height) {
                bulletsToRemove.add(bullet)
            } else if (bullet.isEnemy) {
                if (sqrt((tank.x - bullet.x) * (tank.x - bullet.x) + (tank.y - bullet.y) * (tank.y - bullet.y)) < 30f) bulletsToRemove.add(bullet)
                if (sqrt((helicopter.x - bullet.x) * (helicopter.x - bullet.x) + (helicopter.y - bullet.y) * (helicopter.y - bullet.y)) < 30f) bulletsToRemove.add(bullet)
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

            // Ищем, на какой юнит нажали
            var clickedUnit: Unit? = null
            if (sqrt((touchX - tank.x) * (touchX - tank.x) + (touchY - tank.y) * (touchY - tank.y)) < 60f) clickedUnit = tank
            else if (sqrt((touchX - helicopter.x) * (touchX - helicopter.x) + (touchY - helicopter.y) * (touchY - helicopter.y)) < 60f) clickedUnit = helicopter

            if (clickedUnit != null) {
                // Выделяем
                for (u in units) u.isSelected = false
                clickedUnit.isSelected = true
                selectedUnit = clickedUnit
            } else {
                // Едем
                selectedUnit?.let {
                    it.targetX = touchX
                    it.targetY = touchY
                    it.isMoving = true
                }
            }
            invalidate()
            return true
        }
        return super.onTouchEvent(event)
    }
}
