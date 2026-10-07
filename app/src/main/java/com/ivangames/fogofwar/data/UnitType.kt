package com.ivangames.fogofwar.data

enum class UnitType(
    val displayName: String,
    val hp: Int,
    val damage: Int,
    val speed: Float,       // пикселей в секунду (по карте)
    val visionRadius: Float, // радиус обзора в пикселях карты
    val size: Float,        // размер юнита в пикселях карты
    val color: Int          // цвет для отрисовки (ARGB)
) {
    SOLDIER(
        displayName = "Солдат",
        hp = 50,
        damage = 10,
        speed = 60f,        // ~1 клетка в секунду (клетка 32px)
        visionRadius = 100f,
        size = 16f,
        color = 0xFF4A90D9.toInt()  // синий
    ),
    TANK(
        displayName = "Танк",
        hp = 150,
        damage = 30,
        speed = 40f,
        visionRadius = 200f,
        size = 24f,
        color = 0xFF3A6FA9.toInt()
    ),
    PLANE(
        displayName = "Самолёт",
        hp = 80,
        damage = 25,
        speed = 120f,
        visionRadius = 50f,     // у самолёта маленький радиус, но он быстро летает
        size = 18f,
        color = 0xFF6BA3E0.toInt()
    );

    companion object {
        /** Урон по пехоте (солдатам) */
        fun damageVsInfantry(type: UnitType): Int {
            return when (type) {
                PLANE -> 50  // самолёт бьёт пехоту сильно
                else -> type.damage
            }
        }

        /** Урон по технике */
        fun damageVsVehicle(type: UnitType): Int {
            return when (type) {
                PLANE -> 25  // самолёт бьёт технику слабо
                else -> type.damage
            }
        }
    }
}
