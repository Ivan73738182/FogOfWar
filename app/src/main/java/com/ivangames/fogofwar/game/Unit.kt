package com.ivangames.fogofwar.game

import com.ivangames.fogofwar.data.MapData
import com.ivangames.fogofwar.data.UnitType

class Unit(
    val type: UnitType,
    var x: Float,
    var y: Float,
    val team: Team
) {

    enum class Team { PLAYER, ENEMY }

    enum class Direction { UP, DOWN, LEFT, RIGHT }

    var hp: Int = type.hp
    var maxHp: Int = type.hp

    var targetX: Float? = null
    var targetY: Float? = null

    var selected: Boolean = false

    /** Направление, куда смотрит юнит */
    var direction: Direction = Direction.UP

    val isAlive: Boolean get() = hp > 0

    fun update(dt: Float, map: MapData) {
        val tx = targetX ?: return
        val ty = targetY ?: return

        val dx = tx - x
        val dy = ty - y
        val dist = Math.sqrt((dx * dx + dy * dy).toDouble()).toFloat()

        if (dist < 2f) {
            x = tx
            y = ty
            targetX = null
            targetY = null
            return
        }

        // Обновляем направление по доминирующей оси
        direction = if (Math.abs(dx) > Math.abs(dy)) {
            if (dx > 0) Direction.RIGHT else Direction.LEFT
        } else {
            if (dy > 0) Direction.DOWN else Direction.UP
        }

        val step = type.speed * dt
        val nx = x + (dx / dist) * step
        val ny = y + (dy / dist) * step

        val tileSize = 32f
        val checkX = (nx / tileSize).toInt()
        val checkY = (ny / tileSize).toInt()
        if (map.isPassable(checkX, checkY)) {
            x = nx
            y = ny
        } else {
            targetX = null
            targetY = null
        }
    }

    fun moveTo(tx: Float, ty: Float) {
        targetX = tx
        targetY = ty
    }

    fun takeDamage(amount: Int) {
        hp = (hp - amount).coerceAtLeast(0)
    }
}
