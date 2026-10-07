package com.ivangames.fogofwar.game

import com.ivangames.fogofwar.data.UnitType

class Unit(
    val type: UnitType,
    var x: Float,           // позиция в пикселях карты (центр юнита)
    var y: Float,
    val team: Team
) {

    enum class Team { PLAYER, ENEMY }

    var hp: Int = type.hp
    var maxHp: Int = type.hp

    /** Цель движения (пиксели карты). null — стоит на месте */
    var targetX: Float? = null
    var targetY: Float? = null

    /** Выделен ли юнит */
    var selected: Boolean = false

    val isAlive: Boolean get() = hp > 0

    /** Обновление каждый кадр (dt — время в секундах) */
    fun update(dt: Float, map: com.ivangames.fogofwar.data.MapData) {
        val tx = targetX ?: return
        val ty = targetY ?: return

        val dx = tx - x
        val dy = ty - y
        val dist = Math.sqrt((dx * dx + dy * dy).toDouble()).toFloat()

        if (dist < 2f) {
            // Дошли
            x = tx
            y = ty
            targetX = null
            targetY = null
            return
        }

        val step = type.speed * dt
        val nx = x + (dx / dist) * step
        val ny = y + (dy / dist) * step

        // Проверка коллизии: не выходим в скалу
        val tileSize = 32f
        val checkX = (nx / tileSize).toInt()
        val checkY = (ny / tileSize).toInt()
        if (map.isPassable(checkX, checkY)) {
            x = nx
            y = ny
        } else {
            // Препятствие — стоп
            targetX = null
            targetY = null
        }
    }

    /** Отдать приказ идти в точку */
    fun moveTo(tx: Float, ty: Float) {
        targetX = tx
        targetY = ty
    }

    /** Получить урон */
    fun takeDamage(amount: Int) {
        hp = (hp - amount).coerceAtLeast(0)
    }
}
