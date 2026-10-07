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

    var scale: Float = 1f
        private set

    var minScale: Float = 0.4f
        private set

    val maxScale: Float = 3.0f

    val baseTile: Float = baseTileSize

    val tile: Float get() = baseTileSize * scale

    val worldWidth: Float get() = map.width * baseTileSize
    val worldHeight: Float get() = map.height * baseTileSize

    val viewWidth: Float get() = screenWidth / scale
    val viewHeight: Float get() = screenHeight / scale

    fun updateMinScale() {
        if (screenWidth <= 0f || screenHeight <= 0f) return
        val w = worldWidth / screenWidth
        val h = worldHeight / screenHeight
        minScale = maxOf(w, h).coerceAtLeast(0.4f)
        if (scale < minScale) scale = minScale
    }

    fun centerOn(worldX: Float, worldY: Float) {
        offsetX = worldX - viewWidth / 2f
        offsetY = worldY - viewHeight / 2f
        clamp()
    }

    private fun clamp() {
        val maxX = (worldWidth - viewWidth).coerceAtLeast(0f)
        val maxY = (worldHeight - viewHeight).coerceAtLeast(0f)
        offsetX = offsetX.coerceIn(0f, maxX)
        offsetY = offsetY.coerceIn(0f, maxY)
    }

    fun move(dxScreen: Float, dyScreen: Float) {
        offsetX += dxScreen / scale
        offsetY += dyScreen / scale
        clamp()
    }

    fun zoomBy(factor: Float, focusScreenX: Float, focusScreenY: Float) {
        val oldScale = scale
        val newScale = (scale * factor).coerceIn(minScale, maxScale)
        if (newScale == oldScale) return

        val worldFocusX = offsetX + focusScreenX / oldScale
        val worldFocusY = offsetY + focusScreenY / oldScale

        scale = newScale

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
        return Pair((wx / baseTileSize).toInt(), (wy / baseTileSize).toInt())
    }

    fun isTileVisible(tx: Int, ty: Int): Boolean {
        val px = tx * baseTileSize
        val py = ty * baseTileSize
        return px + baseTileSize >= offsetX &&
                px <= offsetX + viewWidth &&
                py + baseTileSize >= offsetY &&
                py <= offsetY + viewHeight
    }
}
