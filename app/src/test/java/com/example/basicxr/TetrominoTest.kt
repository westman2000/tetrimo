package com.example.basicxr

import org.junit.Assert.*
import org.junit.Test

class TetrominoTest {

    @Test
    fun `test tetromino initialization`() {
        val tetromino = Tetromino(TetrominoType.I)

        assertEquals(TetrominoType.I, tetromino.type)
        assertEquals(0, tetromino.position.x)
        assertEquals(0, tetromino.position.y)
        assertEquals(0, tetromino.rotationIndex)
    }

    @Test
    fun `test tetromino rotation`() {
        val tetromino = Tetromino(TetrominoType.I)

        // Get initial shape
        val initialShape = tetromino.getCurrentShape()

        // Rotate
        tetromino.rotate()
        assertEquals(1, tetromino.rotationIndex)

        // Get new shape
        val rotatedShape = tetromino.getCurrentShape()

        // For I piece, rotation should change the shape
        assertNotEquals(initialShape, rotatedShape)

        // Rotate three more times to get back to initial state
        tetromino.rotate()
        tetromino.rotate()
        tetromino.rotate()

        assertEquals(0, tetromino.rotationIndex)
        assertEquals(initialShape, tetromino.getCurrentShape())
    }

    @Test
    fun `test get absolute positions`() {
        val tetromino = Tetromino(TetrominoType.O)

        // O tetromino is 2x2 square
        val shape = tetromino.getCurrentShape()
        assertEquals(4, shape.size)

        // Initial position is (0, 0)
        var absolutePositions = tetromino.getAbsolutePositions()

        // Should be at (0, 0), (1, 0), (0, 1), (1, 1)
        assertTrue(absolutePositions.contains(Position(0, 0)))
        assertTrue(absolutePositions.contains(Position(1, 0)))
        assertTrue(absolutePositions.contains(Position(0, 1)))
        assertTrue(absolutePositions.contains(Position(1, 1)))

        // Move to (5, 10)
        tetromino.position = Position(5, 10)
        absolutePositions = tetromino.getAbsolutePositions()

        // Should be at (5, 10), (6, 10), (5, 11), (6, 11)
        assertTrue(absolutePositions.contains(Position(5, 10)))
        assertTrue(absolutePositions.contains(Position(6, 10)))
        assertTrue(absolutePositions.contains(Position(5, 11)))
        assertTrue(absolutePositions.contains(Position(6, 11)))
    }

    @Test
    fun `test tetromino color`() {
        // Each tetromino type should have a different color
        val colors = mutableSetOf<Any>()

        for (type in TetrominoType.entries) {
            val tetromino = Tetromino(type)
            val color = tetromino.getColor()

            // Color should not be transparent
            assertNotEquals(0f, color.alpha)

            // Color should be unique for each type
            assertFalse("Duplicate color for $type", colors.contains(color))
            colors.add(color)
        }
    }

    @Test
    fun `test getAbsolutePositions with different rotations`() {
        // Test I tetromino which has significant rotation changes
        val tetromino = Tetromino(TetrominoType.I)

        // Initial position at (5, 5)
        tetromino.position = Position(5, 5)

        // Get initial absolute positions (should be vertical at rotation 0)
        val initialPositions = tetromino.getAbsolutePositions()

        // For the I tetromino at rotation 0, it should be vertical
        val expectedVertical = listOf(
            Position(5, 5), Position(5, 6), Position(5, 7), Position(5, 8)
        )
        assertEquals(expectedVertical, initialPositions)

        // Rotate to rotation 1 (should be horizontal)
        tetromino.rotate()

        // Get positions for rotation 1
        val rotatedPositions = tetromino.getAbsolutePositions()

        // For the I tetromino at rotation 1, it should be horizontal
        val expectedHorizontal = listOf(
            Position(5, 5), Position(6, 5), Position(7, 5), Position(8, 5)
        )
        assertEquals(expectedHorizontal, rotatedPositions)

        // Get positions for all 4 rotations to ensure they cycle properly
        val rotationPositions = mutableListOf<List<Position>>()

        // Reset and collect all 4 rotations
        tetromino.rotationIndex = 0
        repeat(4) { i ->
            rotationPositions.add(tetromino.getAbsolutePositions())
            tetromino.rotate()
        }

        // Each distinct rotation should be different (vertical vs horizontal)
        assertNotEquals(rotationPositions[0], rotationPositions[1])
        assertNotEquals(rotationPositions[1], rotationPositions[2])
        assertNotEquals(rotationPositions[2], rotationPositions[3])

        // The 4th rotation should match the initial rotation (back to vertical)
        assertEquals(rotationPositions[0], rotationPositions[4 % 4])
    }

    @Test
    fun `test all tetromino types have shapes`() {
        for (type in TetrominoType.entries) {
            val tetromino = Tetromino(type)
            val shape = tetromino.getCurrentShape()

            // Each shape should have at least 4 blocks
            assertEquals(4, shape.size)
        }
    }

    @Test
    fun `test Position equals and hashCode`() {
        val pos1 = Position(5, 10)
        val pos2 = Position(5, 10)
        val pos3 = Position(10, 5)

        // Equals should compare x and y values
        assertEquals(pos1, pos2)
        assertNotEquals(pos1, pos3)

        // HashCode should be consistent with equals
        assertEquals(pos1.hashCode(), pos2.hashCode())
        assertNotEquals(pos1.hashCode(), pos3.hashCode())
    }
}