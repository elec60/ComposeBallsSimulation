package com.example.composeballssimulation

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.random.Random


@Composable
fun BallsSimulation(modifier: Modifier) {
    var balls by remember { mutableStateOf(listOf<Ball>()) }

    var screenHeight  by remember { mutableFloatStateOf(0f) }
    var screenWidth  by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(Unit) {
        while (true) {
            balls = updateBallPhysics(balls, screenWidth, screenHeight)
            delay(16)
        }
    }

    Canvas(
        modifier = modifier
            .onSizeChanged {
                screenHeight = it.height.toFloat()
                screenWidth = it.width.toFloat()
            }
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val newBalls = createBallsAtPosition(offset)
                    balls = balls + newBalls
                }
            }
    ) {
        // Adjust drawing position to account for status bar
        balls.forEach { ball ->
            drawCircle(
                color = ball.color,
                radius = ball.radius,
                center = ball.position.copy(y = ball.position.y)
            )
        }
    }
}

private fun createBallsAtPosition(position: Offset): List<Ball> {
    val numBalls = Random.nextInt(3, 8)
    return List(numBalls) {
        Ball(
            position = position,
            velocity = Offset(
                Random.nextFloat() * 400 - 200,  // Random X velocity between -200 and 200
                Random.nextFloat() * -400        // Random upward Y velocity
            ),
            radius = Random.nextFloat() * 30 + 20,  // Random radius between 20 and 50
            color = Color(
                red = Random.nextFloat(),
                green = Random.nextFloat(),
                blue = Random.nextFloat(),
                alpha = 1f
            )
        )
    }
}

private fun updateBallPhysics(
    balls: List<Ball>,
    screenWidth: Float,
    screenHeight: Float
): List<Ball> {
    val deltaTime = 0.016f  // ~60 FPS simulation rate

    return balls.map { ball ->
        var newVelocity = ball.velocity
        var newPosition = ball.position

        // Check if ball is effectively at rest (optimization)
        val isNearFloor =
            screenHeight - (ball.position.y + ball.radius) < PhysicsEngine.POSITION_SLEEP_THRESHOLD
        val hasLowVelocity =
            PhysicsEngine.getMagnitude(ball.velocity) < PhysicsEngine.VELOCITY_SLEEP_THRESHOLD

        // Only update physics if ball is not in "sleep" state
        if (!(isNearFloor && hasLowVelocity)) {
            // 1. Apply gravitational acceleration: Δv = g * Δt
            newVelocity = newVelocity.copy(
                y = newVelocity.y + PhysicsEngine.GRAVITY * deltaTime
            )

            // 2. Apply air resistance to both components
            // Air resistance is proportional to velocity: F_drag ∝ -v
            newVelocity = newVelocity.copy(
                x = newVelocity.x * PhysicsEngine.AIR_FRICTION,
                y = newVelocity.y * PhysicsEngine.AIR_FRICTION
            )

            // 3. Update position using current velocity: Δp = v * Δt
            // Semi-implicit Euler integration
            newPosition = Offset(
                newPosition.x + newVelocity.x * deltaTime,
                newPosition.y + newVelocity.y * deltaTime
            )

            // 4. Floor collision detection and response
            if (newPosition.y + ball.radius > screenHeight) {
                // Constrain position to floor
                newPosition = newPosition.copy(y = screenHeight - ball.radius)

                // Apply bounce with energy loss if velocity is significant
                newVelocity = if (abs(newVelocity.y) < PhysicsEngine.MINIMUM_VELOCITY) {
                    // Stop vertical motion if below threshold
                    newVelocity.copy(y = 0f)
                } else {
                    // Reverse velocity with energy loss: v_new = -v_old * bounce_factor
                    newVelocity.copy(y = -newVelocity.y * PhysicsEngine.BOUNCE_FACTOR)
                }

                // Apply ground friction to horizontal motion
                newVelocity = newVelocity.copy(
                    x = newVelocity.x * PhysicsEngine.GROUND_FRICTION
                )

                // Stop horizontal motion if below threshold
                if (abs(newVelocity.x) < PhysicsEngine.MINIMUM_VELOCITY) {
                    newVelocity = newVelocity.copy(x = 0f)
                }
            }

            // 5. Wall collision detection and response
            // Left wall
            if (newPosition.x - ball.radius < 0) {
                newPosition = newPosition.copy(x = ball.radius)
                newVelocity = if (abs(newVelocity.x) < PhysicsEngine.MINIMUM_VELOCITY) {
                    newVelocity.copy(x = 0f)
                } else {
                    newVelocity.copy(x = -newVelocity.x * PhysicsEngine.BOUNCE_FACTOR)
                }
            }
            // Right wall
            else if (newPosition.x + ball.radius > screenWidth) {
                newPosition = newPosition.copy(x = screenWidth - ball.radius)
                newVelocity = if (abs(newVelocity.x) < PhysicsEngine.MINIMUM_VELOCITY) {
                    newVelocity.copy(x = 0f)
                } else {
                    newVelocity.copy(x = -newVelocity.x * PhysicsEngine.BOUNCE_FACTOR)
                }
            }

            // 6. Ball-to-ball collision detection and response
            balls.forEach { otherBall ->
                if (ball != otherBall) {
                    // Calculate distance between ball centers
                    val distance = PhysicsEngine.calculateDistance(newPosition, otherBall.position)

                    // Check for collision (overlap of radii)
                    if (distance < (ball.radius + otherBall.radius)) {
                        // Calculate collision normal (direction of impact)
                        val normal = PhysicsEngine.normalize(
                            Offset(
                                newPosition.x - otherBall.position.x,
                                newPosition.y - otherBall.position.y
                            )
                        )

                        // Calculate relative velocity
                        val relativeVelocity = Offset(
                            newVelocity.x - otherBall.velocity.x,
                            newVelocity.y - otherBall.velocity.y
                        )

                        // Calculate velocity along the normal
                        val velocityAlongNormal = PhysicsEngine.dotProduct(relativeVelocity, normal)

                        // Only resolve collision if balls are moving toward each other
                        if (velocityAlongNormal < 0) {
                            // Coefficient of restitution (elasticity of collision)
                            val restitution = 0.6f

                            // Calculate impulse scalar
                            // j = -(1 + e)(v1 - v2)⋅n
                            val j = -(1 + restitution) * velocityAlongNormal

                            // Apply impulse to velocity
                            newVelocity = Offset(
                                newVelocity.x + normal.x * j,
                                newVelocity.y + normal.y * j
                            )
                        }

                        // Prevent ball overlap (position correction)
                        val overlap = ball.radius + otherBall.radius - distance
                        if (overlap > 0) {
                            val separationVector = normal * overlap * 0.5f
                            newPosition = Offset(
                                newPosition.x + separationVector.x,
                                newPosition.y + separationVector.y
                            )
                        }
                    }
                }
            }
        }

        // Return updated ball state
        ball.copy(position = newPosition, velocity = newVelocity)
    }
}
