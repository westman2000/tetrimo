package com.example.basicxr

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import kotlin.random.Random

class TetrisEngine @Inject constructor() {
    companion object {
        const val BOARD_WIDTH = 10
        const val BOARD_HEIGHT = 20
        const val INITIAL_DELAY = 800L // milliseconds
    }

    // Game state
    private val _gameState = MutableStateFlow(GameState.READY)
    val gameState: StateFlow<GameState> = _gameState.asStateFlow()

    // Score state
    private val _score = MutableStateFlow(Score())
    val score: StateFlow<Score> = _score.asStateFlow()

    // Board state - stores only fixed blocks
    private val _boardState = Array(BOARD_HEIGHT) { Array(BOARD_WIDTH) { Cell() } }

    // Display board - includes current tetromino and ghost piece
    private val _board = MutableStateFlow(
        Array(BOARD_HEIGHT) { Array(BOARD_WIDTH) { Cell() } }
    )
    val board: StateFlow<Array<Array<Cell>>> = _board.asStateFlow()

    // Current and next tetromino
    private var currentTetromino: Tetromino? = null

    private val _nextTetromino = MutableStateFlow<Tetromino?>(null)
    val nextTetromino: StateFlow<Tetromino?> = _nextTetromino.asStateFlow()

    // Game speed
    private var gameSpeed = INITIAL_DELAY

    // Start the game
    fun startGame() {
        if (_gameState.value == GameState.READY || _gameState.value == GameState.GAME_OVER) {
            // Reset everything
            for (y in 0 until BOARD_HEIGHT) {
                for (x in 0 until BOARD_WIDTH) {
                    _boardState[y][x] = Cell()
                }
            }

            _score.value = Score()
            gameSpeed = INITIAL_DELAY

            // Create first tetromino
            currentTetromino = createNewTetromino()
            _nextTetromino.value = createNewTetromino()

            // Draw the board with the new tetromino
            drawBoard()

            // Set state to playing
            _gameState.value = GameState.PLAYING
        } else if (_gameState.value == GameState.PAUSED) {
            _gameState.value = GameState.PLAYING
        }
    }

    // Pause the game
    fun pauseGame() {
        if (_gameState.value == GameState.PLAYING) {
            _gameState.value = GameState.PAUSED
        }
    }

    // Reset the game
    fun resetGame() {
        _gameState.value = GameState.READY

        // Clear board
        for (y in 0 until BOARD_HEIGHT) {
            for (x in 0 until BOARD_WIDTH) {
                _boardState[y][x] = Cell()
            }
        }

        _score.value = Score()
        currentTetromino = null
        _nextTetromino.value = null

        // Update display
        drawBoard()
    }

    // Create a new random tetromino
    private fun createNewTetromino(): Tetromino {
        val types = TetrominoType.entries.toTypedArray()
        val newType = types[Random.nextInt(types.size)]
        val newTetromino = Tetromino(newType)

        // Position at top-center of board
        newTetromino.position = Position(BOARD_WIDTH / 2 - 1, 0)

        return newTetromino
    }

    // Move left
    fun moveLeft(): Boolean {
        if (_gameState.value != GameState.PLAYING || currentTetromino == null)
            return false

        val oldX = currentTetromino!!.position.x
        currentTetromino!!.position = Position(oldX - 1, currentTetromino!!.position.y)

        if (!isPositionValid(currentTetromino!!)) {
            // Move back if invalid
            currentTetromino!!.position = Position(oldX, currentTetromino!!.position.y)
            return false
        }

        // Update display
        drawBoard()
        return true
    }

    // Move right
    fun moveRight(): Boolean {
        if (_gameState.value != GameState.PLAYING || currentTetromino == null)
            return false

        val oldX = currentTetromino!!.position.x
        currentTetromino!!.position = Position(oldX + 1, currentTetromino!!.position.y)

        if (!isPositionValid(currentTetromino!!)) {
            // Move back if invalid
            currentTetromino!!.position = Position(oldX, currentTetromino!!.position.y)
            return false
        }

        // Update display
        drawBoard()
        return true
    }

    // Rotate
    fun rotate(): Boolean {
        if (_gameState.value != GameState.PLAYING || currentTetromino == null)
            return false

        val oldRotation = currentTetromino!!.rotationIndex
        currentTetromino!!.rotate()

        if (!isPositionValid(currentTetromino!!)) {
            // Simple wall kick attempts
            val oldX = currentTetromino!!.position.x
            val oldY = currentTetromino!!.position.y

            // Try moving left, right, or up to make the rotation work
            val kicks = listOf(
                Position(oldX - 1, oldY),
                Position(oldX + 1, oldY),
                Position(oldX, oldY - 1)
            )

            var kickWorked = false
            for (kick in kicks) {
                currentTetromino!!.position = kick
                if (isPositionValid(currentTetromino!!)) {
                    kickWorked = true
                    break
                }
            }

            if (!kickWorked) {
                // Revert rotation and position
                currentTetromino!!.rotationIndex = oldRotation
                currentTetromino!!.position = Position(oldX, oldY)
                return false
            }
        }

        // Update display
        drawBoard()
        return true
    }

    // Move down
    fun moveDown(): Boolean {
        if (_gameState.value != GameState.PLAYING || currentTetromino == null)
            return false

        val oldY = currentTetromino!!.position.y
        currentTetromino!!.position = Position(currentTetromino!!.position.x, oldY + 1)

        if (!isPositionValid(currentTetromino!!)) {
            // Move back if invalid
            currentTetromino!!.position = Position(currentTetromino!!.position.x, oldY)
            return false
        }

        // Update display
        drawBoard()
        return true
    }

    // Hard drop
    @Suppress("AssignedValueIsNeverRead", "VariableNeverRead")
    fun hardDrop() {
        if (_gameState.value != GameState.PLAYING || currentTetromino == null)
            return

        // Find drop position
        var droppedAtLeastOnce = false
        while (moveDown()) {
            droppedAtLeastOnce = true
        }

        // Lock the piece at its final position
        lockPiece()
    }

    // Game tick - called periodically
    fun gameTick() {
        if (_gameState.value != GameState.PLAYING || currentTetromino == null)
            return

        // Simply move down
        if (!moveDown()) {
            // Couldn't move down, lock the piece
            lockPiece()
        }
    }

    // Check if position is valid (no collisions or out of bounds)
    private fun isPositionValid(tetromino: Tetromino): Boolean {
        val blocks = tetromino.getAbsolutePositions()

        for (pos in blocks) {
            // Check board boundaries
            if (pos.x < 0 || pos.x >= BOARD_WIDTH || pos.y >= BOARD_HEIGHT) {
                return false
            }

            // Check collision with locked pieces
            // (Only check cells that are on the board)
            if (pos.y >= 0 && _boardState[pos.y][pos.x].filled) {
                return false
            }
        }

        return true
    }

    // Lock the current piece in place and create a new piece
    private fun lockPiece() {
        if (currentTetromino == null) return

        // Add blocks to board state
        val blocks = currentTetromino!!.getAbsolutePositions()
        for (pos in blocks) {
            if (pos.y >= 0 && pos.y < BOARD_HEIGHT && pos.x >= 0 && pos.x < BOARD_WIDTH) {
                _boardState[pos.y][pos.x] = Cell(true, currentTetromino!!.getColor())
            }
        }

        // Check for completed lines
        val linesCleared = clearLines()
        if (linesCleared > 0) {
            updateScore(linesCleared)
        }

        // Create new piece
        currentTetromino = _nextTetromino.value
        _nextTetromino.value = createNewTetromino()

        // Check for game over - if new piece overlaps with existing blocks
        if (!isPositionValid(currentTetromino!!)) {
            _gameState.value = GameState.GAME_OVER
        }

        // Update display
        drawBoard()
    }

    // Clear completed lines and return number of lines cleared
    private fun clearLines(): Int {
        // First identify all rows that are filled
        val filledRows = mutableListOf<Int>()

        // Check each row from bottom to top
        for (y in BOARD_HEIGHT - 1 downTo 0) {
            var rowFilled = true

            // Check if this row is completely filled
            for (x in 0 until BOARD_WIDTH) {
                if (!_boardState[y][x].filled) {
                    rowFilled = false
                    break
                }
            }

            if (rowFilled) {
                filledRows.add(y)
            }
        }

        // If no filled rows, return early
        if (filledRows.isEmpty()) {
            return 0
        }

        // Create a new board without the filled rows
        val newBoard = Array(BOARD_HEIGHT) { Array(BOARD_WIDTH) { Cell() } }

        // Start at the bottom of the new board
        var newY = BOARD_HEIGHT - 1

        // Copy rows from old board to new board, skipping filled rows
        for (oldY in BOARD_HEIGHT - 1 downTo 0) {
            if (oldY !in filledRows) {
                // Copy this row to the new board
                for (x in 0 until BOARD_WIDTH) {
                    newBoard[newY][x] = _boardState[oldY][x]
                }
                newY--
            }
        }

        // Update the board state
        for (y in 0 until BOARD_HEIGHT) {
            for (x in 0 until BOARD_WIDTH) {
                _boardState[y][x] = newBoard[y][x]
            }
        }

        return filledRows.size
    }

    // Update score based on lines cleared
    private fun updateScore(linesCleared: Int) {
        val currentScore = _score.value

        // Calculate points based on lines cleared and level
        val points = when (linesCleared) {
            1 -> 100
            2 -> 300
            3 -> 500
            4 -> 800
            else -> 0
        } * currentScore.level

        val newLines = currentScore.lines + linesCleared
        val newLevel = 1 + (newLines / 10) // Level up every 10 lines

        _score.value = Score(
            lines = newLines,
            score = currentScore.score + points,
            level = newLevel
        )

        // Update game speed based on level
        gameSpeed = (INITIAL_DELAY * Math.pow(0.8, (newLevel - 1).toDouble())).toLong()
    }

    // Draw the board with current state, active tetromino, and ghost piece
    private fun drawBoard() {
        // Create a copy of the board state
        val displayBoard = Array(BOARD_HEIGHT) { y ->
            Array(BOARD_WIDTH) { x ->
                _boardState[y][x].copy()
            }
        }

        // Add ghost piece (only if game is playing)
        if (_gameState.value == GameState.PLAYING && currentTetromino != null) {
            // Create ghost piece (copy of current piece)
            val ghost = Tetromino(currentTetromino!!.type)
            ghost.position = Position(currentTetromino!!.position.x, currentTetromino!!.position.y)
            ghost.rotationIndex = currentTetromino!!.rotationIndex

            // Move ghost down until collision
            while (true) {
                ghost.position = Position(ghost.position.x, ghost.position.y + 1)

                if (!isPositionValid(ghost)) {
                    // Move back up one position
                    ghost.position = Position(ghost.position.x, ghost.position.y - 1)
                    break
                }
            }

            // Only draw ghost if it's not at same position as current piece
            if (ghost.position.y > currentTetromino!!.position.y) {
                // Draw ghost piece (semi-transparent)
                val ghostBlocks = ghost.getAbsolutePositions()
                for (pos in ghostBlocks) {
                    if (pos.y >= 0 && pos.y < BOARD_HEIGHT && pos.x >= 0 && pos.x < BOARD_WIDTH) {
                        if (!displayBoard[pos.y][pos.x].filled) {
                            displayBoard[pos.y][pos.x] = Cell(true, currentTetromino!!.getColor().copy(alpha = 0.3f))
                        }
                    }
                }
            }
        }

        // Add current tetromino
        if (currentTetromino != null) {
            val blocks = currentTetromino!!.getAbsolutePositions()
            for (pos in blocks) {
                if (pos.y >= 0 && pos.y < BOARD_HEIGHT && pos.x >= 0 && pos.x < BOARD_WIDTH) {
                    displayBoard[pos.y][pos.x] = Cell(true, currentTetromino!!.getColor())
                }
            }
        }

        // Update the board state flow
        _board.value = displayBoard
    }

    // Get current game speed
    fun getGameSpeed(): Long {
        return gameSpeed
    }
}