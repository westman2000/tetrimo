package com.example.basicxr

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject

@HiltViewModel
class TetrisViewModel @Inject constructor(
    private val tetrisEngine: TetrisEngine
) : ViewModel() {

    // Game state from engine
    val gameState: StateFlow<GameState> = tetrisEngine.gameState
    val board: StateFlow<Array<Array<Cell>>> = tetrisEngine.board
    val score: StateFlow<Score> = tetrisEngine.score
    val nextTetromino: StateFlow<Tetromino?> = tetrisEngine.nextTetromino

    // Game tick job
    private var gameTickJob: Job? = null

    init {
        // Observe game state changes to manage game tick job
        viewModelScope.launch {
            gameState.collect { state ->
                when (state) {
                    GameState.PLAYING -> startGameTick()
                    else -> stopGameTick()
                }
            }
        }
    }

    // Game controls
    fun startGame() {
        tetrisEngine.startGame()
    }

    fun pauseGame() {
        tetrisEngine.pauseGame()
    }

    fun resetGame() {
        stopGameTick()
        tetrisEngine.resetGame()
    }

    fun moveLeft() {
        tetrisEngine.moveLeft()
    }

    fun moveRight() {
        tetrisEngine.moveRight()
    }

    fun rotate() {
        tetrisEngine.rotate()
    }

    fun moveDown() {
        tetrisEngine.moveDown()
    }

    fun hardDrop() {
        tetrisEngine.hardDrop()
    }

    // Start the game tick coroutine
    private fun startGameTick() {
        stopGameTick()

        gameTickJob = viewModelScope.launch {
            // Short initial delay before first tick
            delay(200)

            while (isActive && gameState.value == GameState.PLAYING) {
                tetrisEngine.gameTick()
                delay(tetrisEngine.getGameSpeed())
            }
        }
    }

    // Stop the game tick coroutine
    private fun stopGameTick() {
        gameTickJob?.cancel()
        gameTickJob = null
    }

    override fun onCleared() {
        super.onCleared()
        stopGameTick()
    }
}