package com.example.composeballssimulation

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color

data class Ball(
    val position: Offset,
    val velocity: Offset,
    val radius: Float,
    val color: Color
)