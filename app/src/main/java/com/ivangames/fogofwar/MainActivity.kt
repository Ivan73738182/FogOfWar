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

// Базовый класс для всех юнитов (чтобы не дублировать код)
open class Unit(var x: Float, var y: Float) {
    var targetX = x
    var targetY = y
    var isMoving = false
    var angle = 0f
    var speed = 4f
    var isSelected = false
    var isDead = false
}

class Soldier(x: Float, y: Float) : Unit(x, y) {
    var lastShotTime = 0L
    val fireRate = 1200L
    val visionRadius = 250f
}

class Tank(x: Float, y: Float) : Unit(x, y) {
    var turretAngle = 0f
    var lastShotTime = 0L
    val fireRate = 800L
    val visionRadius = 350f
}

class Enemy(var x: Float, var y: Float) {
    var isDead = false
}

class Bullet(var x: Float, var y: Float, var dx: Float, var dy: Float, var isEnemy: Boolean)
private val bullets = mutableListOf<Bullet>()

class GameView(context: Context) : View(context) {
    // Наши юниты
    private val tank = Tank(500f, 500f)
    private val soldier = Soldier(600f, 500f)
    private val units = listOf(tank, soldier)
    private var selectedUnit: Unit? = tank // По умолчанию выбран танк

    private val enemies = mutableListOf<Enemy>()
    private val paint = Paint()
    private val gridPaint = Paint().apply {
        color = Color.parseColor("#1E3A1E")
        strokeWidth = 2f
    }

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
            if (!enemy.isDead) drawEnemy(canvas, enemy)
        }

        // 3. Наши юниты (рисуем в порядке: сначала солдат, потом танк)
        // Солдат
        canvas.save()
        canvas.translate(soldier.x, soldier.y)
        canvas.rotate(soldier.angle)
        drawSoldier(canvas, soldier.isSelected)
        canvas.restore()

        // Танк
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

        // 4. Пули
        for (bullet in bullets) {
            paint.color = if (bullet.isEnemy) Color.RED else Color.YELLOW
            canvas.drawCircle(bullet.x, bullet.y, 5f, paint)
        }

        // 5. Туман войны (вокруг выбранного юнита)
        val visionX = selectedUnit?.x ?: tank.x
        val visionY = selectedUnit?.y ?: tank.y
        val visionRad = if (selectedUnit is Tank) 350f else 250f
        
        val gradientPaint = Paint().apply {
            shader = android.graphics.RadialGradient(
                visionX, visionY, visionRad,
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

    private fun drawSoldier(canvas: Canvas, isSelected: Boolean) {
        val px = 8f
        // Тело
        paint.color = Color.parseColor("#2E7D32") // Зеленый
        canvas.drawRect(-2 * px, -3 * px, 2 * px, 3 * px, paint)
        paint.color = Color.parseColor("#1B5E20") // Темно-зеленый (каска)
        canvas.drawCircle(0f, -3 * px, 2 * px, paint)
        // Ружье
        paint.color = Color.BLACK
        canvas.drawRect(0f, -4 * px, 1f * px, -1 * px, paint)

        // Выделение (желтая рамка)
        if (isSelected) {
            paint.style = Paint.Style.STROKE
            paint.color = Color.YELLOW
            paint.strokeWidth = 3f
            canvas.drawRect(-4 * px, -5 * px, 4 * px, 5 * px, paint)
            paint.style = Paint.Style.FILL
        }
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
        // Обновление юнитов
        for (unit in units) {
            if (unit.isDead) continue

            // Движение к цели
            if (unit.isMoving) {
                val dx = unit.targetX - unit.x
                val dy = unit.targetY - unit.y
                val distance = sqrt(dx * dx + dy * dy)

                if (distance > 10f) {
                    val targetAngle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat() + 90f
                    var diff = targetAngle - unit.angle
                    while (diff > 180) diff -= 360f
                    while (diff < -180) diff += 360f
                    unit.angle += diff * 0.15f

                    unit.x += (dx / distance) * unit.speed
                    unit.y += (dy / distance) * unit.speed
                } else {
                    unit.isMoving = false
                }
            }

            // Логика стрельбы (поиск ближайшего врага в радиусе)
            var closestEnemy: Enemy? = null
            var minDist = if (unit is Tank) tank.visionRadius else (unit as Soldier).visionRadius

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
                val rad = Math.toRadians(angleToTarget.toDouble() - 90.0)
                
                if (unit is Tank) {
                    var diff = angleToTarget - unit.turretAngle
                    while (diff > 180) diff -= 360f
                    while (diff < -180) diff += 360f
                    unit.turretAngle += diff * 0.2f

                    if (System.currentTimeMillis() - unit.lastShotTime > unit.fireRate && Math.abs(diff) < 10f) {
                        val tRad = Math.toRadians(unit.turretAngle.toDouble() - 90.0)
                        bullets.add(Bullet(unit.x, unit.y, cos(tRad).toFloat() * 15f, sin(tRad).toFloat() * 15f, false))
                        unit.lastShotTime = System.currentTimeMillis()
                    }
                } else {
                    // Солдат поворачивается всем телом к цели
                    var diff = angleToTarget - unit.angle
                    while (diff > 180) diff -= 360f
                    while (diff < -180) diff += 360f
                    unit.angle += diff * 0.1f

                    if (System.currentTimeMillis() - (unit as Soldier).lastShotTime > unit.fireRate && Math.abs(diff) < 15f) {
                        val rad2 = Math.toRadians(unit.angle.toDouble() - 90.0)
                        bullets.add(Bullet(unit.x, unit.y, cos(rad2).toFloat() * 20f, sin(rad2).toFloat() * 20f, false))
                        unit.lastShotTime = System.currentTimeMillis()
                    }
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
                // Вражеская пуля попадает в наши юниты
                for (unit in units) {
                    val dist = sqrt((unit.x - bullet.x) * (unit.x - bullet.x) + (unit.y - bullet.y) * (unit.y - bullet.y))
                    if (dist < 30f) {
                        bulletsToRemove.add(bullet)
                        break
                    }
                }
            } else {
                // Наша пуля попадает во врагов
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

            // 1. Проверяем, не нажали ли мы на юнита (танк или солдат)
            var clickedUnit: Unit? = null
            
            // Проверяем танк
            if (sqrt((touchX - tank.x) * (touchX - tank.x) + (touchY - tank.y) * (touchY - tank.y)) < 60f) {
                clickedUnit = tank
            }
            // Проверяем солдата
            else if (sqrt((touchX - soldier.x) * (touchX - soldier.x) + (touchY - soldier.y) * (touchY - soldier.y)) < 40f) {
                clickedUnit = soldier
            }

            if (clickedUnit != null) {
                // Выделяем юнита
                for (unit in units) unit.isSelected = false
                clickedUnit.isSelected = true
                selectedUnit = clickedUnit
            } else {
                // Если нажали на карту - едем выбранным юнитом
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
