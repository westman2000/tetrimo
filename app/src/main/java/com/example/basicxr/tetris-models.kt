package com.example.basicxr


import androidx.compose.ui.graphics.Color

// Represents a position on the game board
data class Position(val x: Int, val y: Int) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Position) return false
        return x == other.x && y == other.y
    }

    override fun hashCode(): Int {
        return 31 * x + y
    }
}

// Represents a single cell on the board
data class Cell(
    val filled: Boolean = false,
    val locked: Boolean = false,
    val color: Color = Color.Transparent
) {
    // Data classes automatically implement equals and hashCode,
    // but we'll explicitly define copy to ensure it works as expected
    fun copy(): Cell = Cell(filled = filled, locked = locked, color = color)
}

// Enum representing different types of Tetromino (Tetris pieces)
enum class TetrominoType {
    I, O, T, S, Z, J, L
}

// Represents a Tetris piece
class Tetromino(val type: TetrominoType) {
    // Position and rotation
    var position = Position(0, 0)
    var rotationIndex = 0

    // Each tetromino type has a specific shape represented by relative positions
    private val shapes: Map<TetrominoType, List<List<Position>>> = mapOf(
        TetrominoType.I to listOf(
            listOf(Position(0, 0), Position(0, 1), Position(0, 2), Position(0, 3)),
            listOf(Position(0, 0), Position(1, 0), Position(2, 0), Position(3, 0)),
            listOf(Position(0, 0), Position(0, 1), Position(0, 2), Position(0, 3)),
            listOf(Position(0, 0), Position(1, 0), Position(2, 0), Position(3, 0))
        ),
        TetrominoType.O to listOf(
            listOf(Position(0, 0), Position(1, 0), Position(0, 1), Position(1, 1)),
            listOf(Position(0, 0), Position(1, 0), Position(0, 1), Position(1, 1)),
            listOf(Position(0, 0), Position(1, 0), Position(0, 1), Position(1, 1)),
            listOf(Position(0, 0), Position(1, 0), Position(0, 1), Position(1, 1))
        ),
        TetrominoType.T to listOf(
            listOf(Position(0, 0), Position(1, 0), Position(2, 0), Position(1, 1)),
            listOf(Position(1, 0), Position(0, 1), Position(1, 1), Position(1, 2)),
            listOf(Position(1, 0), Position(0, 1), Position(1, 1), Position(2, 1)),
            listOf(Position(0, 0), Position(0, 1), Position(1, 1), Position(0, 2))
        ),
        TetrominoType.S to listOf(
            listOf(Position(1, 0), Position(2, 0), Position(0, 1), Position(1, 1)),
            listOf(Position(0, 0), Position(0, 1), Position(1, 1), Position(1, 2)),
            listOf(Position(1, 0), Position(2, 0), Position(0, 1), Position(1, 1)),
            listOf(Position(0, 0), Position(0, 1), Position(1, 1), Position(1, 2))
        ),
        TetrominoType.Z to listOf(
            listOf(Position(0, 0), Position(1, 0), Position(1, 1), Position(2, 1)),
            listOf(Position(1, 0), Position(0, 1), Position(1, 1), Position(0, 2)),
            listOf(Position(0, 0), Position(1, 0), Position(1, 1), Position(2, 1)),
            listOf(Position(1, 0), Position(0, 1), Position(1, 1), Position(0, 2))
        ),
        TetrominoType.J to listOf(
            listOf(Position(0, 0), Position(0, 1), Position(1, 1), Position(2, 1)),
            listOf(Position(1, 0), Position(2, 0), Position(1, 1), Position(1, 2)),
            listOf(Position(0, 0), Position(1, 0), Position(2, 0), Position(2, 1)),
            listOf(Position(0, 0), Position(0, 1), Position(0, 2), Position(1, 0))
        ),
        TetrominoType.L to listOf(
            listOf(Position(2, 0), Position(0, 1), Position(1, 1), Position(2, 1)),
            listOf(Position(0, 0), Position(1, 0), Position(1, 1), Position(1, 2)),
            listOf(Position(0, 0), Position(1, 0), Position(2, 0), Position(0, 1)),
            listOf(Position(0, 0), Position(0, 1), Position(0, 2), Position(1, 2))
        )
    )

    // Colors for each tetromino type
    private val colors: Map<TetrominoType, Color> = mapOf(
        TetrominoType.I to Color(0xFF00F0F0), // Cyan
        TetrominoType.O to Color(0xFFF0F000), // Yellow
        TetrominoType.T to Color(0xFFA000F0), // Purple
        TetrominoType.S to Color(0xFF00F000), // Green
        TetrominoType.Z to Color(0xFFF00000), // Red
        TetrominoType.J to Color(0xFF0000F0), // Blue
        TetrominoType.L to Color(0xFFF0A000)  // Orange
    )

    // Get current shape based on rotation
    fun getCurrentShape(): List<Position> {
        return shapes[type]!![rotationIndex % 4]
    }

    // Get absolute positions of the tetromino on the board
    fun getAbsolutePositions(): List<Position> {
        return getCurrentShape().map {
            Position(position.x + it.x, position.y + it.y)
        }
    }

    // Rotate the tetromino clockwise
    fun rotate() {
        rotationIndex = (rotationIndex + 1) % 4
    }

    // Get the color of this tetromino
    fun getColor(): Color {
        return colors[type]!!
    }
}

// Game state enum
enum class GameState {
    READY, PLAYING, PAUSED, GAME_OVER
}

// Represents a game score
data class Score(
    val lines: Int = 0,
    val score: Int = 0,
    val level: Int = 1
)