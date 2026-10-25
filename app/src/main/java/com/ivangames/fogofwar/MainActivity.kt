package com.ivangames.fogofwar

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
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

class Tree(var x: Float, var y: Float) {
    var isDestroyed = false
    val radius = 40f
}

class Building(var x: Float, var y: Float) {
    val width = 300f
    val height = 200f
    val doorWidth = 80f
    
    fun getBounds() = RectF(x - width/2, y - height/2, x + width/2, y + height/2)
    
    fun isWallHit(px: Float, py: Float): Boolean {
        val left = x - width/2
        val right = x + width/2
        val top = y - height/2
        val bottom = y + height/2
        val doorLeft = x - doorWidth/2
        val doorRight = x + doorWidth/2
        
        if (px in left..right && py in top..bottom) {
            if (py > bottom - 20f && px in doorLeft..doorRight) return false
            return true
        }
        return false
    }
}

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
    var speed = 3f
    var canBreakTrees = false
}

class Tank(x: Float, y: Float) : Unit(x, y) {
    var turretAngle = 0f
    init { fireRate = 1000L; visionRadius = 350f; speed = 3f; canBreakTrees = true }
}

class Helicopter(x: Float, y: Float) : Unit(x, y) {
    var rotorAngle = 0f
    init { fireRate = 300L; visionRadius = 400f; speed = 5f }
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
    private val tank = Tank(500f, 700f)
    private val helicopter = Helicopter(600f, 800f)
    private val units = listOf(tank, helicopter)
    private var selectedUnit: Unit? = tank

    private val paint = Paint()
    // Убрали gridPaint, так как клеток больше не будет

    private val trees = mutableListOf<Tree>()
    private val buildings = mutableListOf<Building>()
    private val enemies = mutableListOf<EnemyTank>()
    private val bullets = mutableListOf<Bullet>()

    init {
        for (i in 0..20) trees.add(Tree(Random.nextFloat() * 1800f + 100f, Random.nextFloat() * 900f + 100f))
        buildings.add(Building(400f, 400f))
        buildings.add(Building(1200f, 300f))
        buildings.add(Building(800f, 900f))
        for (i in 0..5) enemies.add(EnemyTank(Random.nextFloat() * 1500f + 100f, Random.nextFloat() * 800f + 100f))
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Карта (только трава, без сетки)
        canvas.drawColor(Color.parseColor("#2E7D32"))

        // Здания
        for (b in buildings) drawBuilding(canvas, b)

        // Деревья
        for (t in trees) if (!t.isDestroyed) drawTree(canvas, t)

        // Враги
        for (enemy in enemies) {
            if (!enemy.isDead && isEnemyVisible(enemy.x, enemy.y)) {
                canvas.save()
                canvas.translate(enemy.x, enemy.y)
                canvas.rotate(enemy.angle)
                drawEnemyTank(canvas)
                canvas.restore()
            }
        }

        // Танк
        if (!tank.isDead) {
            canvas.save()
            canvas.translate(tank.x, tank.y)
            canvas.save(); canvas.rotate(tank.angle); drawTankHull(canvas, tank.isSelected); canvas.restore()
            canvas.save(); canvas.rotate(tank.turretAngle); drawTankTurret(canvas); canvas.restore()
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

        // Пули
        for (bullet in bullets) {
            paint.color = if (bullet.isEnemy) Color.RED else Color.YELLOW
            canvas.drawCircle(bullet.x, bullet.y, 5f, paint)
        }

        updateGame()
    }

    private fun drawTree(canvas: Canvas, tree: Tree) {
        paint.color = Color.argb(80, 0, 0, 0)
        canvas.drawOval(tree.x - 30f, tree.y + 20f, tree.x + 30f, tree.y + 40f, paint)
        paint.color = Color.parseColor("#5D4037")
        canvas.drawRect(tree.x - 8f, tree.y - 10f, tree.x + 8f, tree.y + 20f, paint)
        paint.color = Color.parseColor("#2E7D32")
        canvas.drawCircle(tree.x, tree.y - 30f, 35f, paint)
        paint.color = Color.parseColor("#1B5E20")
        canvas.drawCircle(tree.x - 15f, tree.y - 20f, 25f, paint)
        canvas.drawCircle(tree.x + 15f, tree.y - 20f, 25f, paint)
    }

    private fun drawBuilding(canvas: Canvas, b: Building) {
        paint.color = Color.parseColor("#757575")
        val rect = b.getBounds()
        canvas.drawRect(rect, paint)
        paint.color = Color.parseColor("#616161")
        canvas.drawRect(rect.left, rect.top, rect.right, rect.top + 40f, paint)
        paint.style = Paint.Style.STROKE
        paint.color = Color.BLACK
        paint.strokeWidth = 4f
        canvas.drawRect(rect, paint)
        paint.style = Paint.Style.FILL
        paint.color = Color.BLACK
        val doorLeft = b.x - b.doorWidth / 2
        canvas.drawRect(doorLeft, rect.bottom - 15f, doorLeft + b.doorWidth, rect.bottom, paint)
    }

    private fun drawEnemyTank(canvas: Canvas) {
        val px = 12f
        paint.color = Color.parseColor("#1A1A1A")
        canvas.drawRect(-5 * px, -6 * px, -3 * px, 6 * px, paint); canvas.drawRect(3 * px, -6 * px, 5 * px, 6 * px, paint)
        paint.color = Color.parseColor("#757575")
        canvas.drawRect(-3 * px, -5 * px, 3 * px, 5 * px, paint)
        paint.style = Paint.Style.STROKE; paint.color = Color.BLACK; paint.strokeWidth = 4f
        canvas.drawRect(-3 * px, -5 * px, 3 * px, 5 * px, paint); paint.style = Paint.Style.FILL
        paint.color = Color.parseColor("#9E9E9E")
        canvas.drawCircle(0f, 0f, 2.5f * px, paint)
        paint.style = Paint.Style.STROKE; paint.color = Color.BLACK; canvas.drawCircle(0f, 0f, 2.5f * px, paint); paint.style = Paint.Style.FILL
        paint.color = Color.parseColor("#424242")
        canvas.drawRect(-0.5f * px, -7 * px, 0.5f * px, -2 * px, paint)
        paint.style = Paint.Style.STROKE; paint.color = Color.BLACK; canvas.drawRect(-0.5f * px, -7 * px, 0.5f * px, -2 * px, paint); paint.style = Paint.Style.FILL
    }

    private fun drawTankHull(canvas: Canvas, isSelected: Boolean) {
        val px = 12f
        paint.color = Color.parseColor("#1A1A1A")
        canvas.drawRect(-5 * px, -6 * px, -3 * px, 6 * px, paint); canvas.drawRect(3 * px, -6 * px, 5 * px, 6 * px, paint)
        paint.color = Color.parseColor("#4B5320")
        canvas.drawRect(-3 * px, -5 * px, 3 * px, 5 * px, paint)
        paint.style = Paint.Style.STROKE; paint.color = Color.BLACK; paint.strokeWidth = 4f
        canvas.drawRect(-3 * px, -5 * px, 3 * px, 5 * px, paint); paint.style = Paint.Style.FILL
        if (isSelected) {
            paint.style = Paint.Style.STROKE; paint.color = Color.YELLOW; paint.strokeWidth = 3f
            canvas.drawRect(-7 * px, -8 * px, 7 * px, 8 * px, paint); paint.style = Paint.Style.FILL
        }
    }

    private fun drawTankTurret(canvas: Canvas) {
        val px = 12f
        paint.color = Color.parseColor("#5C6B2E"); canvas.drawCircle(0f, 0f, 2.5f * px, paint)
        paint.style = Paint.Style.STROKE; paint.color = Color.BLACK; canvas.drawCircle(0f, 0f, 2.5f * px, paint); paint.style = Paint.Style.FILL
        paint.color = Color.parseColor("#3A401A"); canvas.drawRect(-0.5f * px, -7 * px, 0.5f * px, -2 * px, paint)
        paint.style = Paint.Style.STROKE; paint.color = Color.BLACK; canvas.drawRect(-0.5f * px, -7 * px, 0.5f * px, -2 * px, paint); paint.style = Paint.Style.FILL
    }

    private fun drawHelicopter(canvas: Canvas, isSelected: Boolean, rotorAngle: Float) {
        val px = 10f
        paint.color = Color.argb(100, 0, 0, 0); canvas.drawOval(-6 * px, 6 * px, 6 * px, 10 * px, paint)
        paint.color = Color.parseColor("#3E4A2D"); canvas.drawRect(-1.5f * px, 0f, 1.5f * px, 6 * px, paint)
        paint.style = Paint.Style.STROKE; paint.color = Color.BLACK; paint.strokeWidth = 2f; canvas.drawRect(-1.5f * px, 0f, 1.5f * px, 6 * px, paint)
        paint.style = Paint.Style.FILL; paint.color = Color.DKGRAY; canvas.drawRect(-0.5f * px, 5 * px, 0.5f * px, 7 * px, paint)
        paint.color = Color.parseColor("#4B5320"); canvas.drawOval(-3 * px, -4 * px, 3 * px, 2 * px, paint)
        paint.style = Paint.Style.STROKE; paint.color = Color.BLACK; canvas.drawOval(-3 * px, -4 * px, 3 * px, 2 * px, paint)
        paint.style = Paint.Style.FILL; paint.color = Color.parseColor("#87CEEB"); canvas.drawOval(-2 * px, -3 * px, 1 * px, 0f, paint)
        canvas.save(); canvas.translate(0f, -3 * px); canvas.rotate(rotorAngle)
        paint.color = Color.DKGRAY; canvas.drawRect(-8 * px, -0.5f * px, 8 * px, 0.5f * px, paint); canvas.drawRect(-0.5f * px, -8 * px, 0.5f * px, 8 * px, paint)
        canvas.restore()
        if (isSelected) {
            paint.style = Paint.Style.STROKE; paint.color = Color.YELLOW; paint.strokeWidth = 3f
            canvas.drawCircle(0f, 0f, 8 * px, paint); paint.style = Paint.Style.FILL
        }
    }

    private fun isEnemyVisible(ex: Float, ey: Float): Boolean {
        for (unit in units) {
            if (unit.isDead) continue
            val dist = sqrt((ex - unit.x) * (ex - unit.x) + (ey - unit.y) * (ey - unit.y))
            if (dist < unit.visionRadius) return true
        }
        return false
    }

    private fun updateGame() {
        helicopter.rotorAngle += 30f

        for (unit in units) {
            if (unit.isDead) continue
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

                    if (unit is Tank) {
                        for (tree in trees) {
                            if (!tree.isDestroyed) {
                                val distToTree = sqrt((unit.x - tree.x) * (unit.x - tree.x) + (unit.y - tree.y) * (unit.y - tree.y))
                                if (distToTree < 50f) tree.isDestroyed = true
                            }
                        }
                    }

                    if (unit !is Helicopter) {
                        var hitWall = false
                        for (b in buildings) if (b.isWallHit(unit.x, unit.y)) { hitWall = true; break }
                        if (!hitWall) { unit.x += (dx / distance) * unit.speed; unit.y += (dy / distance) * unit.speed }
                        else unit.isMoving = false
                    } else { unit.x += (dx / distance) * unit.speed; unit.y += (dy / distance) * unit.speed }
                } else unit.isMoving = false
            }

            var closestEnemy: EnemyTank? = null
            var minDist = unit.visionRadius
            for (enemy in enemies) {
                if (enemy.isDead) continue
                val dist = sqrt((enemy.x - unit.x) * (enemy.x - unit.x) + (enemy.y - unit.y) * (enemy.y - unit.y))
                if (dist < minDist) { minDist = dist; closestEnemy = enemy }
            }

            if (closestEnemy != null) {
                val angleToTarget = Math.toDegrees(atan2((closestEnemy.y - unit.y).toDouble(), (closestEnemy.x - unit.x).toDouble())).toFloat() + 90f
                var diff = angleToTarget - unit.angle
                while (diff > 180) diff -= 360f
                while (diff < -180) diff += 360f
                if (unit is Tank) {
                    var tDiff = angleToTarget - unit.turretAngle
                    while (tDiff > 180) tDiff -= 360f
                    while (tDiff < -180) tDiff += 360f
                    unit.turretAngle += tDiff * 0.04f
                    if (System.currentTimeMillis() - unit.lastShotTime > unit.fireRate && Math.abs(tDiff) < 5f) {
                        val rad = Math.toRadians(unit.turretAngle.toDouble() - 90.0)
                        bullets.add(Bullet(unit.x + cos(rad).toFloat() * 84f, unit.y + sin(rad).toFloat() * 84f, cos(rad).toFloat() * 15f, sin(rad).toFloat() * 15f, false))
                        unit.lastShotTime = System.currentTimeMillis()
                    }
                } else if (unit is Helicopter) {
                    unit.angle += diff * 0.1f
                    if (System.currentTimeMillis() - unit.lastShotTime > unit.fireRate && Math.abs(diff) < 10f) {
                        val rad = Math.toRadians(unit.angle.toDouble() - 90.0)
                        bullets.add(Bullet(unit.x + cos(rad).toFloat() * 40f, unit.y + sin(rad).toFloat() * 40f, cos(rad).toFloat() * 20f, sin(rad).toFloat() * 20f, false))
                        unit.lastShotTime = System.currentTimeMillis()
                    }
                }
            }
        }

        for (enemy in enemies) {
            if (enemy.isDead) continue
            var targetX = tank.x; var targetY = tank.y; var minDist = 400f
            val distToHeli = sqrt((helicopter.x - enemy.x) * (helicopter.x - enemy.x) + (helicopter.y - enemy.y) * (helicopter.y - enemy.y))
            if (distToHeli < minDist) { targetX = helicopter.x; targetY = helicopter.y; minDist = distToHeli }
            if (minDist < 400f && System.currentTimeMillis() - enemy.lastShotTime > enemy.fireRate) {
                val angleToTarget = Math.toDegrees(atan2((targetY - enemy.y).toDouble(), (targetX - enemy.x).toDouble())).toFloat() + 90f
                var diff = angleToTarget - enemy.turretAngle
                while (diff > 180) diff -= 360f
                while (diff < -180) diff += 360f
                enemy.turretAngle += diff * 0.05f
                if (Math.abs(diff) < 10f) {
                    val rad = Math.toRadians(enemy.turretAngle.toDouble() - 90.0)
                    bullets.add(Bullet(enemy.x + cos(rad).toFloat() * 84f, enemy.y + sin(rad).toFloat() * 84f, cos(rad).toFloat() * 15f, sin(rad).toFloat() * 15f, true))
                    enemy.lastShotTime = System.currentTimeMillis()
                }
            }
        }

        val bulletsToRemove = mutableListOf<Bullet>()
        for (bullet in bullets) {
            bullet.x += bullet.dx; bullet.y += bullet.dy
            var hitWall = false
            for (b in buildings) if (b.isWallHit(bullet.x, bullet.y)) { hitWall = true; break }
            for (t in trees) {
                if (!t.isDestroyed) {
                    val dist = sqrt((t.x - bullet.x) * (t.x - bullet.x) + (t.y - bullet.y) * (t.y - bullet.y))
                    if (dist < t.radius) { hitWall = true; break }
                }
            }
            if (hitWall || bullet.x < 0 || bullet.x > width || bullet.y < 0 || bullet.y > height) bulletsToRemove.add(bullet)
            else if (bullet.isEnemy) {
                if (!tank.isDead && sqrt((tank.x - bullet.x) * (tank.x - bullet.x) + (tank.y - bullet.y) * (tank.y - bullet.y)) < 30f) bulletsToRemove.add(bullet)
                if (!helicopter.isDead && sqrt((helicopter.x - bullet.x) * (helicopter.x - bullet.x) + (helicopter.y - bullet.y) * (helicopter.y - bullet.y)) < 30f) bulletsToRemove.add(bullet)
            } else {
                for (enemy in enemies) {
                    if (enemy.isDead) continue
                    val dist = sqrt((enemy.x - bullet.x) * (enemy.x - bullet.x) + (enemy.y - bullet.y) * (enemy.y - bullet.y))
                    if (dist < 30f) { enemy.isDead = true; bulletsToRemove.add(bullet); break }
                }
            }
        }
        bullets.removeAll(bulletsToRemove)
        invalidate()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN) {
            val touchX = event.x; val touchY = event.y
            var clickedUnit: Unit? = null
            if (sqrt((touchX - tank.x) * (touchX - tank.x) + (touchY - tank.y) * (touchY - tank.y)) < 60f) clickedUnit = tank
            else if (sqrt((touchX - helicopter.x) * (touchX - helicopter.x) + (touchY - helicopter.y) * (touchY - helicopter.y)) < 60f) clickedUnit = helicopter
            if (clickedUnit != null) {
                for (u in units) u.isSelected = false
                clickedUnit.isSelected = true; selectedUnit = clickedUnit
            } else if (selectedUnit != null) {
                selectedUnit?.let { it.targetX = touchX; it.targetY = touchY; it.isMoving = true }
            }
            invalidate(); return true
        }
        return super.onTouchEvent(event)
    }
}
