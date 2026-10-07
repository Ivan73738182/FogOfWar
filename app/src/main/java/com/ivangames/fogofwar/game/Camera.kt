package com.ivangames.fogofwar.game

import com.ivangames.fogofwar.data.MapData

class Camera(
    private val map: MapData,
    private val baseTileSize: Float = 32f
) {

    var offsetX: Float = 0f
    var offsetY: Float = 0f

    var screenWidth: Float = 0f
    var screenHeight: Float = 0f

    /** Масштаб: 1.0 — стандарт, >1 — приближено, <1 — отдалено */
    var scale: Float = 1f
        private set

    companion object {
        const val MIN_SCALE = 0.4f
        const val MAX_SCALE = 3.0f
    }

    /** Итоговый размер тайла на экране */
    val tile: Float get() = baseTileSize * scale

    val worldWidth: Float get() = map.width * tile
    val worldHeight: Float get() = map.height * tile

    fun centerOn(worldX: Float, worldY: Float) {
        offsetX = worldX - screenWidth / 2f
        offsetY = worldY - screenHeight / 2f
        clamp()
    }

    private fun clamp() {
        val maxX = (worldWidth - screenWidth).coerceAtLeast(0f)
        val maxY = (worldHeight - screenHeight).coerceAtLeast(0f)
        offsetX = offsetX.coerceIn(0f, maxX)
        offsetY = offsetY.coerceIn(0f, maxY)
    }

    fun move(dx: Float, dy: Float) {
        offsetX += dx
        offsetY += dy
        clamp()
    }

    /**
     * Изменить масштаб. Точка (focusScreenX, focusScreenY) остаётся на месте.
     */
    fun zoomBy(factor: Float, focusScreenX: Float, focusScreenY: Float) {
        val oldScale = scale
        val newScale = (scale * factor).coerceIn(MIN_SCALE, MAX_SCALE)
        if (newScale == oldScale) return

        // Мировая точка под пальцами — сохраняем её позицию на экране
        val worldFocusX = offsetX + focusScreenX / oldScale
        val worldFocusY = offsetY + focusScreenY / oldScale

        scale = newScale

        // Пересчитываем offset так, чтобы мировая точка оказалась под теми же экранными координатами
        offsetX = worldFocusX - focusScreenX / newScale
        offsetY = worldFocusY - focusScreenY / newScale
        clamp()
    }

    fun worldToScreen(worldX: Float, worldY: Float): Pair<Float, Float> {
        return Pair(
            (worldX - offsetX) * scale,
            (worldY - offsetY) * scale
        )
    }

    fun screenToWorld(screenX: Float, screenY: Float): Pair<Float, Float> {
        return Pair(
            screenX / scale + offsetX,
            screenY / scale + offsetY
        )
    }

    fun screenToTile(screenX: Float, screenY: Float): Pair<Int, Int> {
        val (wx, wy) = screenToWorld(screenX, screenY)
        val tx = (wx / baseTileSize).toInt()
        val ty = (wy / baseTileSize).toInt()
        return Pair(tx, ty)
    }

    fun isTileVisible(tx: Int, ty: Int): Boolean {
        val px = tx * tile
        val py = ty * tile
        return px + tile >= 0 &&
                px <= screenWidth &&
                py + tile >= 0 &&
                py <= screenHeight
    }
}
