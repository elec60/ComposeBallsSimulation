package com.example.composeballssimulation

import androidx.compose.ui.geometry.Offset
import kotlin.math.sqrt

object PhysicsEngine {
    const val GRAVITY = 800f           // Gravity acceleration (scaled for pixels)
    const val BOUNCE_FACTOR = 0.65f    // Energy loss on bounce (0 = no bounce, 1 = perfect bounce)
    const val AIR_FRICTION = 0.98f     // Air resistance (1 = no air resistance)
    const val GROUND_FRICTION = 0.92f  // Ground friction (should be < AIR_FRICTION)
    const val MINIMUM_VELOCITY = 10f   // Minimum velocity before coming to rest
    const val VELOCITY_SLEEP_THRESHOLD = 20f  // Velocity threshold for sleep state
    const val POSITION_SLEEP_THRESHOLD = 1f   // Distance threshold for sleep state

    fun calculateDistance(point1: Offset, point2: Offset): Float {
        val dx = point2.x - point1.x
        val dy = point2.y - point1.y
        return sqrt(dx * dx + dy * dy)
    }

    fun normalize(vector: Offset): Offset {
        val magnitude = sqrt(vector.x * vector.x + vector.y * vector.y)
        return if (magnitude != 0f) {
            Offset(vector.x / magnitude, vector.y / magnitude)
        } else {
            vector
        }
    }

    fun dotProduct(v1: Offset, v2: Offset): Float {
        return v1.x * v2.x + v1.y * v2.y
    }

    fun getMagnitude(velocity: Offset): Float {
        return sqrt(velocity.x * velocity.x + velocity.y * velocity.y)
    }
}
