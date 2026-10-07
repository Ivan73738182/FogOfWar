package com.ivangames.fogofwar.game

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import com.ivangames.fogofwar.data.MapData
import com.ivangames.fogofwar.data.TileType
import com.ivangames.fogofwar.data.UnitType
import kotlin.math.abs
import kotlin.math.sqrt

class GameView(context: Context) : SurfaceView(context), SurfaceHolder.Callback, Runnable {

    // === Поток ===
    private var thread: Thread? = null
    @Volatile private var running = false

    // === Данные мира ===
    private val map = MapData().apply { generate() }
    private val camera = Camera(map)
    private val units = mutableListOf<Unit>()

    // === Отрисовка ===
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

    // === Обработка ввода ===
    private var touchDownX = 0f
    private var touchDownY = 0f
    private var lastTouchX = 0f
    private var lastTouchY = 0f
    private var moved = false
    private var multiTouch = false

    // Цвета тайлов
    private val colorGrass = Color.rgb(61, 90, 61)
    private val colorGrassDark = Color.rgb(47, 70, 47)
    private val colorRoad = Color.rgb(122, 110, 90)
    private val colorRock = Color.rgb(90, 90, 90)
    private val colorWater = Color.rgb(42, 74, 107)

    init {
        holder.addCallback(this)
        isFocusable = true
    }

    // ================== ЖИЗНЕННЫЙ ЦИКЛ ==================

    override fun surfaceCreated(holder: SurfaceHolder) {
        // Создаём одного солдата игрока у своей базы
        val tileSize = camera.tile
        val soldierX = (map.ownBaseX + 1) * tileSize + tileSize / 2
        val soldierY = (map.ownBaseY + 1) * tileSize + tileSize / 2
        units.add(Unit(UnitType.SOLDIER, soldierX, soldierY, Unit.Team.PLAYER))

        running = true
        thread = Thread(this).also { it.start() }
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        camera.screenWidth = width.toFloat()
        camera.screenHeight = height.toFloat()
        // Центрируем на своём солдате при старте
        units.firstOrNull()?.let {
            camera.centerOn(it.x, it.y)
        }
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        running = false
        thread?.join()
        thread = null
    }

    // ================== ИГРОВОЙ ЦИКЛ ==================

    override fun run() {
        var lastTime = System.nanoTime()

        while (running) {
            val now = System.nanoTime()
            val dt = ((now - lastTime) / 1_000_000_000f).coerceAtMost(0.05f)
            lastTime = now

            update(dt)
            draw()

            // ~60 FPS
            val frameTime = (System.nanoTime() - now) / 1_000_000
            val sleepTime = 16 - frameTime
            if (sleepTime > 0) {
                try { Thread.sleep(sleepTime) } catch (_: InterruptedException) {}
            }
        }
    }

    private fun update(dt: Float) {
        for (u in units) {
            if (u.isAlive) u.update(dt, map)
        }
        // Убираем мёртвых
        units.removeAll { !it.isAlive }
    }

    private fun draw() {
        val canvas = holder.lockCanvas() ?: return
        try {
            canvas.drawColor(Color.rgb(15, 20, 25))

            // 1. Карта
            drawMap(canvas)

            // 2. Юниты
            for (u in units) drawUnit(canvas, u)

            // 3. Выделение
            for (u in units) {
                if (u.selected) drawSelection(canvas, u)
                if (u.targetX != null && u.targetY != null && u.selected) {
                    drawTarget(canvas, u)
                }
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
                val (sx, sy) = camera.worldToScreen(x * ts, y * ts)
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
        val half = u.type.size / 2f

        // Тень
        unitPaint.color = Color.argb(80, 0, 0, 0)
        canvas.drawRect(sx - half + 2, sy - half + 2, sx + half + 2, sy + half + 2, unitPaint)

        // Тело
        unitPaint.color = u.type.color
        canvas.drawRect(sx - half, sy - half, sx + half, sy + half, unitPaint)

        // Обводка
        unitPaint.color = Color.WHITE
        unitPaint.style = Paint.Style.STROKE
        unitPaint.strokeWidth = 2f
        canvas.drawRect(sx - half, sy - half, sx + half, sy + half, unitPaint)
        unitPaint.style = Paint.Style.FILL
    }

    private fun drawSelection(canvas: Canvas, u: Unit) {
        val (sx, sy) = camera.worldToScreen(u.x, u.y)
        val r = u.type.size * 1.2f
        canvas.drawCircle(sx, sy, r, selectionPaint)
    }

    private fun drawTarget(canvas: Canvas, u: Unit) {
        val tx = u.targetX ?: return
        val ty = u.targetY ?: return
        val (sx, sy) = camera.worldToScreen(tx, ty)
        canvas.drawCircle(sx, sy, 10f, targetPaint)
        // Крестик
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
                multiTouch = false
            }

            MotionEvent.ACTION_POINTER_DOWN -> {
                multiTouch = true
            }

            MotionEvent.ACTION_MOVE -> {
                if (multiTouch) {
                    // Пока игнорируем мультитач (сделаем позже)
                    return true
                }
                val dx = event.x - lastTouchX
                val dy = event.y - lastTouchY

                // Если сдвинулись больше 15px — это скролл
                if (!moved && (abs(event.x - touchDownX) > 15 || abs(event.y - touchDownY) > 15)) {
                    moved = true
                }
                if (moved) {
                    camera.move(-dx, -dy)
                }
                lastTouchX = event.x
                lastTouchY = event.y
            }

            MotionEvent.ACTION_UP -> {
                if (!moved && !multiTouch) {
                    // Это был тап
                    handleTap(event.x, event.y)
                }
            }
        }
        return true
    }

    private fun handleTap(screenX: Float, screenY: Float) {
        // 1. Проверяем, попали ли в юнита
        val (wx, wy) = camera.screenToWorld(screenX, screenY)
        val hit = units.firstOrNull { u ->
            val dx = u.x - wx
            val dy = u.y - wy
            sqrt((dx * dx + dy * dy).toDouble()) < u.type.size
        }

        if (hit != null && hit.team == Unit.Team.PLAYER) {
            // Выделяем только его (одиночное выделение)
            for (u in units) u.selected = false
            hit.selected = true
            return
        }

        // 2. Тап по карте — приказ выделенному юниту идти туда
        val selected = units.filter { it.selected && it.team == Unit.Team.PLAYER }
        if (selected.isNotEmpty() && map.isPassable((wx / camera.tile).toInt(), (wy / camera.tile).toInt())) {
            for (u in selected) {
                u.moveTo(wx, wy)
            }
            return
        }

        // 3. Тап по пустому — снять выделение
        for (u in units) u.selected = false
    }
}
