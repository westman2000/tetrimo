package com.example.basicxr

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations

@ExperimentalCoroutinesApi
class TetrisViewModelTest {

    @Mock
    private lateinit var tetrisEngine: TetrisEngine

    private lateinit var viewModel: TetrisViewModel

    // Use StandardTestDispatcher instead of the deprecated TestCoroutineDispatcher
    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        Dispatchers.setMain(testDispatcher)

        // Set up default behaviors for mocked engine
        `when`(tetrisEngine.gameState).thenReturn(MutableStateFlow(GameState.READY))
        `when`(tetrisEngine.getGameSpeed()).thenReturn(1000L)

        viewModel = TetrisViewModel(tetrisEngine)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `test start game delegates to engine`() {
        viewModel.startGame()
        verify(tetrisEngine).startGame()
    }

    @Test
    fun `test pause game delegates to engine`() {
        viewModel.pauseGame()
        verify(tetrisEngine).pauseGame()
    }

    @Test
    fun `test reset game delegates to engine`() {
        viewModel.resetGame()
        verify(tetrisEngine).resetGame()
    }

    @Test
    fun `test move left delegates to engine`() {
        viewModel.moveLeft()
        verify(tetrisEngine).moveLeft()
    }

    @Test
    fun `test move right delegates to engine`() {
        viewModel.moveRight()
        verify(tetrisEngine).moveRight()
    }

    @Test
    fun `test rotate delegates to engine`() {
        viewModel.rotate()
        verify(tetrisEngine).rotate()
    }

    @Test
    fun `test move down delegates to engine`() {
        viewModel.moveDown()
        verify(tetrisEngine).moveDown()
    }

    @Test
    fun `test hard drop delegates to engine`() {
        viewModel.hardDrop()
        verify(tetrisEngine).hardDrop()
    }

    @Test
    fun `test game tick job starts when game state changes to playing`() = testScope.runTest {
        // Change game state to PLAYING
        val gameStateFlow = MutableStateFlow(GameState.PLAYING)
        `when`(tetrisEngine.gameState).thenReturn(gameStateFlow)

        // Create new viewModel to trigger the state collection
        val testViewModel = TetrisViewModel(tetrisEngine)

        // Advance time to allow coroutine to run
        advanceTimeBy(500)

        // Verify that gameTick was called at least once
        verify(tetrisEngine, atLeastOnce()).gameTick()
    }

    @Test
    fun `test game tick job stops when game state changes from playing`() = testScope.runTest {
        // Start with PLAYING state
        val gameStateFlow = MutableStateFlow(GameState.PLAYING)
        `when`(tetrisEngine.gameState).thenReturn(gameStateFlow)

        // Create new viewModel to trigger the state collection
        val testViewModel = TetrisViewModel(tetrisEngine)

        // Advance time to allow coroutine to run
        advanceTimeBy(500)

        // Verify gameTick was called
        verify(tetrisEngine, atLeastOnce()).gameTick()

        // Reset mock to clear invocation count
        clearInvocations(tetrisEngine)

        // Change state to PAUSED
        gameStateFlow.value = GameState.PAUSED

        // Advance time again
        advanceTimeBy(2000)

        // Verify gameTick was not called after state change
        verify(tetrisEngine, never()).gameTick()
    }
}