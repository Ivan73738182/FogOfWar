package com.ivangames.fogofwar.game

import com.ivangames.fogofwar.data.MapData

class Camera(
    private val map: MapData,
    private val tileSize: Float = 32f
) {

    /** Смещение камеры в пикселях карты (левый верхний угол экрана) */
    var offsetX: Float = 0f
    var offsetY: Float = 0f

    /** Размер экрана в пикселях */
    var screenWidth: Float = 0f
    var screenHeight: Float = 0f

    /** Общий размер карты в пикселях */
    val worldWidth: Float get() = map.width * tileSize
    val worldHeight: Float get() = map.height * tileSize

    /** Размер тайла (для отрисовки) */
    val tile: Float get() = tileSize

    /** Центрировать камеру на точке (в пикселях карты) */
    fun centerOn(worldX: Float, worldY: Float) {
        offsetX = worldX - screenWidth / 2f
        offsetY = worldY - screenHeight / 2f
        clamp()
    }

    /** Ограничить камеру краями карты */
    private fun clamp() {
        val maxX = (worldWidth - screenWidth).coerceAtLeast(0f)
        val maxY = (worldHeight - screenHeight).coerceAtLeast(0f)
        offsetX = offsetX.coerceIn(0f, maxX)
        offsetY = offsetY.coerceIn(0f, maxY)
    }

    /** Сдвинуть камеру на dx, dy (в пикселях экрана) */
    fun move(dx: Float, dy: Float) {
        offsetX += dx
        offsetY += dy
        clamp()
    }

    /** Мировые координаты → экранные */
    fun worldToScreen(worldX: Float, worldY: Float): Pair<Float, Float> {
        return Pair(worldX - offsetX, worldY - offsetY)
    }

    /** Экранные координаты → мировые */
    fun screenToWorld(screenX: Float, screenY: Float): Pair<Float, Float> {
        return Pair(screenX + offsetX, screenY + offsetY)
    }

    /** Экранные координаты → клетка карты */
    fun screenToTile(screenX: Float, screenY: Float): Pair<Int, Int> {
        val (wx, wy) = screenToWorld(screenX, screenY)
        val tx = (wx / tileSize).toInt()
        val ty = (wy / tileSize).toInt()
        return Pair(tx, ty)
    }

    /** Видна ли клетка на экране (для оптимизации отрисовки) */
    fun isTileVisible(tx: Int, ty: Int): Boolean {
        val px = tx * tileSize
        val py = ty * tileSize
        return px + tileSize >= offsetX &&
                px <= offsetX + screenWidth &&
                py + tileSize >= offsetY &&
                py <= offsetY + screenHeight
    }
}
