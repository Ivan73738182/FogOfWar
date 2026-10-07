package com.ivangames.fogofwar.data

import kotlin.random.Random

enum class TileType {
    GRASS,
    GRASS_DARK,
    ROAD,
    ROCK,
    WATER
}

data class Tile(
    val x: Int,
    val y: Int,
    var type: TileType,
    var passable: Boolean = true   // можно ли пройти
)

class MapData(val width: Int = 48, val height: Int = 48) {

    val tiles: Array<Array<Tile>> = Array(width) { x ->
        Array(height) { y ->
            // Чередуем траву для пиксельного разнообразия
            val type = if ((x + y) % 2 == 0) TileType.GRASS else TileType.GRASS_DARK
            Tile(x, y, type, passable = true)
        }
    }

    /** Позиция своей базы (левый верхний угол) */
    val ownBaseX: Int = 3
    val ownBaseY: Int = 3

    /** Позиция базы врага (правый нижний угол) */
    val enemyBaseX: Int = width - 4
    val enemyBaseY: Int = height - 4

    /**
     * Генерируем карту: рисуем дороги от своей базы к базе врага.
     * Пока простой вариант — крест-накрест и немного кривых.
     */
    fun generate() {
        // Горизонтальная дорога через центр
        val midY = height / 2
        for (x in 0 until width) {
            tiles[x][midY].type = TileType.ROAD
        }

        // Вертикальная дорога через центр
        val midX = width / 2
        for (y in 0 until height) {
            tiles[midX][y].type = TileType.ROAD
        }

        // Немного случайных препятствий (скалы)
        val rng = Random(42) // фикс. seed — карта всегда одна и та же
        var placed = 0
        while (placed < 30) {
            val x = rng.nextInt(2, width - 2)
            val y = rng.nextInt(2, height - 2)
            val tile = tiles[x][y]
            // Не ставим скалы на дорогу и вблизи баз
            if (tile.type == TileType.ROAD) continue
            if (isNearBase(x, y)) continue
            tile.type = TileType.ROCK
            tile.passable = false
            placed++
        }
    }

    private fun isNearBase(x: Int, y: Int): Boolean {
        val nearOwn = Math.abs(x - ownBaseX) < 4 && Math.abs(y - ownBaseY) < 4
        val nearEnemy = Math.abs(x - enemyBaseX) < 4 && Math.abs(y - enemyBaseY) < 4
        return nearOwn || nearEnemy
    }

    /** Проверка: можно ли встать в клетку (не выйти за карту и не в скалу) */
    fun isPassable(x: Int, y: Int): Boolean {
        if (x < 0 || y < 0 || x >= width || y >= height) return false
        return tiles[x][y].passable
    }

    /** Получить тайл по координатам (в клетках) */
    fun tileAt(x: Int, y: Int): Tile? {
        if (x < 0 || y < 0 || x >= width || y >= height) return null
        return tiles[x][y]
    }
}
