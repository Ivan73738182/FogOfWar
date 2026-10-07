package com.ivangames.fogofwar.game

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import com.ivangames.fogofwar.data.MapData
import com.ivangames.fogofwar.data.TileType
import com.ivangames.fogofwar.data.UnitType
import kotlin.math.abs
import kotlin.math.sqrt

class GameView(context: Context) : SurfaceView(context), SurfaceHolder.Callback, Runnable {

    private var thread: Thread? = null
    @Volatile private var running = false

    private val map = MapData().apply { generate() }
    private val camera = Camera(map)
    private val units = mutableListOf<Unit>()

    private val tilePaint = Paint()
    private val unitPaint = Paint().apply { isAntiAlias = true }
    private val selectionPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeWidth = 3f
        color = Color.rgb(80, 200, 255)
    }
    private val targetPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeWidth = 2f
        color = Color.rgb(80, 200, 255)
    }

    // Ввод — одиночный тап / скролл
    private var touchDownX = 0f
    private var touchDownY = 0f
    private var lastTouchX = 0f
    private var lastTouchY = 0f
    private var moved = false

    // Зум двумя пальцами
    private var isZooming = false
    private var lastZoomDist = 0f

    private val colorGrass = Color.rgb(61, 90, 61)
    private val colorGrassDark = Color.rgb(47, 70, 47)
    private val colorRoad = Color.rgb(122, 110, 90)
    private val colorRock = Color.rgb(90, 90, 90)
    private val colorWater = Color.rgb(42, 74, 107)

    init {
        holder.addCallback(this)
        isFocusable = true
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        val ts = 32f
        val soldierX = (map.ownBaseX + 1) * ts + ts / 2
        val soldierY = (map.ownBaseY + 1) * ts + ts / 2
        units.add(Unit(UnitType.SOLDIER, soldierX, soldierY, Unit.Team.PLAYER))

        running = true
        thread = Thread(this).also { it.start() }
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        camera.screenWidth = width.toFloat()
        camera.screenHeight = height.toFloat()
        units.firstOrNull()?.let { camera.centerOn(it.x, it.y) }
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        running = false
        thread?.join()
        thread = null
    }

    override fun run() {
        var lastTime = System.nanoTime()
        while (running) {
            val now = System.nanoTime()
            val dt = ((now - lastTime) / 1_000_000_000f).coerceAtMost(0.05f)
            lastTime = now

            update(dt)
            draw()

            val frameTime = (System.nanoTime() - now) / 1_000_000
            val sleepTime = 16 - frameTime
            if (sleepTime > 0) {
                try { Thread.sleep(sleepTime) } catch (_: InterruptedException) {}
            }
        }
    }

    private fun update(dt: Float) {
        for (u in units) if (u.isAlive) u.update(dt, map)
        units.removeAll { !it.isAlive }
    }

    private fun draw() {
        val canvas = holder.lockCanvas() ?: return
        try {
            canvas.drawColor(Color.rgb(15, 20, 25))
            drawMap(canvas)
            for (u in units) drawUnit(canvas, u)
            for (u in units) {
                if (u.selected) drawSelection(canvas, u)
                if (u.selected && u.targetX != null && u.targetY != null) drawTarget(canvas, u)
            }
        } finally {
            holder.unlockCanvasAndPost(canvas)
        }
    }

    private fun drawMap(canvas: Canvas) {
        val ts = camera.tile
        for (x in 0 until map.width) {
            for (y in 0 until map.height) {
                if (!camera.isTileVisible(x, y)) continue
                val (sx, sy) = camera.worldToScreen(x * 32f, y * 32f)
                val tile = map.tiles[x][y]
                tilePaint.color = when (tile.type) {
                    TileType.GRASS -> colorGrass
                    TileType.GRASS_DARK -> colorGrassDark
                    TileType.ROAD -> colorRoad
                    TileType.ROCK -> colorRock
                    TileType.WATER -> colorWater
                }
                canvas.drawRect(sx, sy, sx + ts, sy + ts, tilePaint)
            }
        }
    }

    private fun drawUnit(canvas: Canvas, u: Unit) {
        val (sx, sy) = camera.worldToScreen(u.x, u.y)
        val half = u.type.size * camera.scale / 2f

        unitPaint.color = Color.argb(80, 0, 0, 0)
        canvas.drawRect(sx - half + 2, sy - half + 2, sx + half + 2, sy + half + 2, unitPaint)

        unitPaint.color = u.type.color
        canvas.drawRect(sx - half, sy - half, sx + half, sy + half, unitPaint)

        unitPaint.color = Color.WHITE
        unitPaint.style = Paint.Style.STROKE
        unitPaint.strokeWidth = 2f
        canvas.drawRect(sx - half, sy - half, sx + half, sy + half, unitPaint)
        unitPaint.style = Paint.Style.FILL
    }

    private fun drawSelection(canvas: Canvas, u: Unit) {
        val (sx, sy) = camera.worldToScreen(u.x, u.y)
        val r = u.type.size * 1.2f * camera.scale
        canvas.drawCircle(sx, sy, r, selectionPaint)
    }

    private fun drawTarget(canvas: Canvas, u: Unit) {
        val tx = u.targetX ?: return
        val ty = u.targetY ?: return
        val (sx, sy) = camera.worldToScreen(tx, ty)
        canvas.drawCircle(sx, sy, 10f, targetPaint)
        canvas.drawLine(sx - 8, sy, sx + 8, sy, targetPaint)
        canvas.drawLine(sx, sy - 8, sx, sy + 8, targetPaint)
    }

    // ================== ВВОД ==================

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                touchDownX = event.x
                touchDownY = event.y
                lastTouchX = event.x
                lastTouchY = event.y
                moved = false
                isZooming = false
            }

            MotionEvent.ACTION_POINTER_DOWN -> {
                if (event.pointerCount >= 2) {
                    isZooming = true
                    lastZoomDist = distance(event)
                }
            }

            MotionEvent.ACTION_MOVE -> {
                if (isZooming && event.pointerCount >= 2) {
                    val d = distance(event)
                    if (lastZoomDist > 0f) {
                        val factor = d / lastZoomDist
                        // Центр между пальцами — точка фокуса
                        val cx = (event.getX(0) + event.getX(1)) / 2f
                        val cy = (event.getY(0) + event.getY(1)) / 2f
                        camera.zoomBy(factor, cx, cy)
                    }
                    lastZoomDist = d
                    return true
                }

                // Обычный скролл одним пальцем
                val dx = event.x - lastTouchX
                val dy = event.y - lastTouchY
                if (!moved && (abs(event.x - touchDownX) > 15 || abs(event.y - touchDownY) > 15)) {
                    moved = true
                }
                if (moved) camera.move(-dx, -dy)
                lastTouchX = event.x
                lastTouchY = event.y
            }

            MotionEvent.ACTION_POINTER_UP -> {
                // Остался один палец — сбрасываем зум
                isZooming = false
                // Переинициализируем lastTouch по оставшемуся пальцу
                val idx = if (event.actionIndex == 0) 1 else 0
                lastTouchX = event.getX(idx)
                lastTouchY = event.getY(idx)
            }

            MotionEvent.ACTION_UP -> {
                if (!moved && !isZooming) {
                    handleTap(event.x, event.y)
                }
                isZooming = false
            }
        }
        return true
    }

    private fun distance(event: MotionEvent): Float {
        if (event.pointerCount < 2) return 0f
        val dx = event.getX(0) - event.getX(1)
        val dy = event.getY(0) - event.getY(1)
        return sqrt((dx * dx + dy * dy).toDouble()).toFloat()
    }

    private fun handleTap(screenX: Float, screenY: Float) {
        val (wx, wy) = camera.screenToWorld(screenX, screenY)
        val hit = units.firstOrNull { u ->
            val dx = u.x - wx
            val dy = u.y - wy
            sqrt((dx * dx + dy * dy).toDouble()) < u.type.size
        }

        if (hit != null && hit.team == Unit.Team.PLAYER) {
            for (u in units) u.selected = false
            hit.selected = true
            return
        }

        val selected = units.filter { it.selected && it.team == Unit.Team.PLAYER }
        if (selected.isNotEmpty() && map.isPassable((wx / 32f).toInt(), (wy / 32f).toInt())) {
            for (u in selected) u.moveTo(wx, wy)
            return
        }

        for (u in units) u.selected = false
    }
}
