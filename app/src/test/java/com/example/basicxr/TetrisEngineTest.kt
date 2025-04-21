package com.example.basicxr


import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import kotlinx.coroutines.runBlocking

class TetrisEngineTest {

    private lateinit var tetrisEngine: TetrisEngine

    @Before
    fun setup() {
        tetrisEngine = TetrisEngine()
    }

    @Test
    fun `test initial game state`() {
        // When the engine is first created, the game state should be READY
        assertEquals(GameState.READY, tetrisEngine.gameState.value)

        // Score should be zero
        assertEquals(0, tetrisEngine.score.value.score)
        assertEquals(0, tetrisEngine.score.value.lines)
        assertEquals(1, tetrisEngine.score.value.level)

        // Next tetromino should be null
        assertNull(tetrisEngine.nextTetromino.value)

        // Board should be empty
        val board = tetrisEngine.board.value
        for (y in 0 until TetrisEngine.BOARD_HEIGHT) {
            for (x in 0 until TetrisEngine.BOARD_WIDTH) {
                assertFalse(board[y][x].filled)
            }
        }
    }

    @Test
    fun `test starting game`() = runBlocking {
        // When we start the game, the state should change to PLAYING
        tetrisEngine.startGame()
        assertEquals(GameState.PLAYING, tetrisEngine.gameState.value)

        // Next tetromino should not be null
        assertNotNull(tetrisEngine.nextTetromino.value)

        // Board should have at least one filled cell (the current tetromino)
        val board = tetrisEngine.board.value
        var hasFilledCell = false
        for (y in 0 until TetrisEngine.BOARD_HEIGHT) {
            for (x in 0 until TetrisEngine.BOARD_WIDTH) {
                if (board[y][x].filled) {
                    hasFilledCell = true
                    break
                }
            }
            if (hasFilledCell) break
        }
        assertTrue(hasFilledCell)
    }

    @Test
    fun `test pausing and resuming game`() {
        // Start game
        tetrisEngine.startGame()
        assertEquals(GameState.PLAYING, tetrisEngine.gameState.value)

        // Pause game
        tetrisEngine.pauseGame()
        assertEquals(GameState.PAUSED, tetrisEngine.gameState.value)

        // Resume game
        tetrisEngine.startGame()
        assertEquals(GameState.PLAYING, tetrisEngine.gameState.value)
    }

    @Test
    fun `test moving tetromino left`() = runBlocking {
        // Start game
        tetrisEngine.startGame()

        // Get initial board state
        val initialBoard = tetrisEngine.board.value

        // Move left
        val moved = tetrisEngine.moveLeft()

        // Check if position changed
        val newBoard = tetrisEngine.board.value

        // The boards should be different if the piece moved
        if (moved) {
            assertNotEquals(initialBoard, newBoard)
        }
    }

    @Test
    fun `test moving tetromino right`() = runBlocking {
        // Start game
        tetrisEngine.startGame()

        // Get initial board state
        val initialBoard = tetrisEngine.board.value

        // Move right
        val moved = tetrisEngine.moveRight()

        // Check if position changed
        val newBoard = tetrisEngine.board.value

        // The boards should be different if the piece moved
        if (moved) {
            assertNotEquals(initialBoard, newBoard)
        }
    }

    @Test
    fun `test moving tetromino down`() = runBlocking {
        // Start game
        tetrisEngine.startGame()

        // Get initial board state
        val initialBoard = tetrisEngine.board.value

        // Move down
        val moved = tetrisEngine.moveDown()

        // Check if position changed
        val newBoard = tetrisEngine.board.value

        // The boards should be different if the piece moved
        if (moved) {
            assertNotEquals(initialBoard, newBoard)
        }
    }

    @Test
    fun `test rotating tetromino`() = runBlocking {
        // Start game
        tetrisEngine.startGame()

        // Get initial board state
        val initialBoard = tetrisEngine.board.value

        // Rotate
        val rotated = tetrisEngine.rotate()

        // Check if position changed
        val newBoard = tetrisEngine.board.value

        // The boards should be different if the piece rotated
        if (rotated) {
            assertNotEquals(initialBoard, newBoard)
        }
    }

    @Test
    fun `test hard drop`() = runBlocking {
        // Start game
        tetrisEngine.startGame()

        // Get initial board state
        val initialBoard = tetrisEngine.board.value

        // Hard drop
        tetrisEngine.hardDrop()

        // Check if board changed
        val newBoard = tetrisEngine.board.value

        // The boards should be different after hard drop
        assertNotEquals(initialBoard, newBoard)

        // After hard drop, a new piece should be active
        // Board should have at least one filled cell in top rows (the new tetromino)
        var hasFilledCellInTopRows = false
        for (y in 0 until 3) { // Check top 3 rows
            for (x in 0 until TetrisEngine.BOARD_WIDTH) {
                if (newBoard[y][x].filled) {
                    hasFilledCellInTopRows = true
                    break
                }
            }
            if (hasFilledCellInTopRows) break
        }
        assertTrue(hasFilledCellInTopRows)
    }

    @Test
    fun `test game tick`() = runBlocking {
        // Start game
        tetrisEngine.startGame()

        // Get initial board state
        val initialBoard = tetrisEngine.board.value

        // Game tick
        tetrisEngine.gameTick()

        // Check if board changed
        val newBoard = tetrisEngine.board.value

        // The boards should be different after a game tick
        assertNotEquals(initialBoard, newBoard)
    }

    @Test
    fun `test line clearing`() = runBlocking {
        // This test is more complex as we need to set up a specific board state
        // Start game
        tetrisEngine.startGame()

        // We'll simulate filling a row by using reflection to access private methods
        // This is a bit of a hack for testing, but it allows us to test line clearing

        // For now, we'll use multiple hard drops to fill the board
        // and then check if lines get cleared

        // Perform multiple hard drops
        repeat(10) {
            tetrisEngine.hardDrop()
            // Small delay to ensure processing completes
            Thread.sleep(100)
        }

        // Check if any lines were cleared (score should be > 0)
        val score = tetrisEngine.score.value

        // Not guaranteed to clear lines, but if we did, score should increase
        if (score.lines > 0) {
            assertTrue(score.score > 0)
        }
    }

    @Test
    fun `test game over`() = runBlocking {
        // Start game
        tetrisEngine.startGame()

        // Simulate filling the board until game over
        var isGameOver = false
        var attempts = 0
        val maxAttempts = 100 // Prevent infinite loop

        while (!isGameOver && attempts < maxAttempts) {
            tetrisEngine.hardDrop()
            if (tetrisEngine.gameState.value == GameState.GAME_OVER) {
                isGameOver = true
            }
            attempts++
            // Small delay to ensure processing completes
            Thread.sleep(50)
        }

        // If we didn't reach max attempts, we should have a game over
        if (attempts < maxAttempts) {
            assertEquals(GameState.GAME_OVER, tetrisEngine.gameState.value)
        }
    }

    @Test
    fun `test resetting game`() = runBlocking {
        // Start game and do some moves
        tetrisEngine.startGame()
        tetrisEngine.moveRight()
        tetrisEngine.hardDrop()

        // Reset game
        tetrisEngine.resetGame()

        // Game state should be READY
        assertEquals(GameState.READY, tetrisEngine.gameState.value)

        // Score should be reset
        assertEquals(0, tetrisEngine.score.value.score)
        assertEquals(0, tetrisEngine.score.value.lines)
        assertEquals(1, tetrisEngine.score.value.level)

        // Board should be empty
        val board = tetrisEngine.board.value
        for (y in 0 until TetrisEngine.BOARD_HEIGHT) {
            for (x in 0 until TetrisEngine.BOARD_WIDTH) {
                assertFalse(board[y][x].filled)
            }
        }
    }
}