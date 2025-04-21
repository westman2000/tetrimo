package com.example.basicxr


import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.xr.compose.platform.LocalHasXrSpatialFeature
import androidx.xr.compose.platform.LocalSession
import androidx.xr.compose.platform.LocalSpatialCapabilities


@Composable
fun TetrisApp(viewModel: TetrisViewModel) {
    val gameState by viewModel.gameState.collectAsState()
    val score by viewModel.score.collectAsState()

    Surface(
        modifier = Modifier
            .width(1200.dp)
            .height(1700.dp),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Absolute.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Game title
                    Text(
                        text = "TETRIS",
                        style = MaterialTheme.typography.headlineLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(vertical = 16.dp)
                    )

                    if (LocalHasXrSpatialFeature.current && !LocalSpatialCapabilities.current.isSpatialUiEnabled) {
                        val session = LocalSession.current
                        FullSpaceModeIconButton(
                            onClick = { session?.spatialEnvironment?.requestFullSpaceMode() },
                            modifier = Modifier.padding(32.dp)
                        )
                    }
                }

                // Game content
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    // Game board
                    TetrisBoard(
                        viewModel = viewModel,
                        modifier = Modifier.padding(end = 16.dp)
                    )

                    // Game information
                    Column(
                        verticalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        // Next piece display
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "NEXT",
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            NextPieceDisplay(viewModel = viewModel)
                        }

                        // Game stats
                        GameStats(viewModel = viewModel)
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Game controls
                GameControls(
                    viewModel = viewModel,
                    modifier = Modifier.fillMaxWidth(0.8f)
                )
            }

            // Game over overlay
            if (gameState == GameState.GAME_OVER) {
                GameOverOverlay(
                    onRestartClick = { viewModel.resetGame() },
                    score = score.score
                )
            }
        }
    }
}


// Component to display the game board
@Composable
fun TetrisBoard(
    viewModel: TetrisViewModel,
    modifier: Modifier = Modifier
) {
    val board by viewModel.board.collectAsState()

    val cellSize = 24.dp
    val boardWidth = cellSize * TetrisEngine.BOARD_WIDTH
    val boardHeight = cellSize * TetrisEngine.BOARD_HEIGHT

    Box(
        modifier = modifier
            .size(width = boardWidth, height = boardHeight)
            .clip(RoundedCornerShape(4.dp))
            .border(2.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(4.dp))
            .background(Color(0xFF121212))
    ) {
        // Draw grid cells
        Canvas(
            modifier = Modifier.fillMaxSize()
        ) {
            val cellSizePx = cellSize.toPx()

            // Draw board grid
            for (y in board.indices) {
                for (x in 0 until TetrisEngine.BOARD_WIDTH) {
                    val cell = board[y][x]

                    // Draw cell
                    drawRect(
                        color = if (cell.filled)
                            cell.color
                        else
                            Color(0xFF1D1D1D),
                        topLeft = Offset(x * cellSizePx, y * cellSizePx),
                        size = Size(cellSizePx, cellSizePx)
                    )

                    // Draw cell border
                    drawRect(
                        color = Color(0xFF2A2A2A),
                        topLeft = Offset(x * cellSizePx, y * cellSizePx),
                        size = Size(cellSizePx, cellSizePx),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1f)
                    )
                }
            }
        }
    }
}

// Component to display the next piece
@Composable
fun NextPieceDisplay(
    viewModel: TetrisViewModel,
    modifier: Modifier = Modifier
) {
    val nextTetromino by viewModel.nextTetromino.collectAsState()

    Box(
        modifier = modifier
            .size(100.dp)
            .clip(RoundedCornerShape(4.dp))
            .border(2.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(4.dp))
            .background(Color(0xFF121212))
            .padding(4.dp),
        contentAlignment = Alignment.Center
    ) {
        if (nextTetromino != null) {
            val cellSize = 18.dp

            Canvas(
                modifier = Modifier
                    .size(80.dp)
                    .align(Alignment.Center)
            ) {
                val cellSizePx = cellSize.toPx()
                val shape = nextTetromino!!.getCurrentShape()

                // Center the piece within the display
                val minX = shape.minOfOrNull { it.x } ?: 0
                val maxX = shape.maxOfOrNull { it.x } ?: 0
                val minY = shape.minOfOrNull { it.y } ?: 0
                val maxY = shape.maxOfOrNull { it.y } ?: 0

                val width = maxX - minX + 1
                val height = maxY - minY + 1

                val offsetX = (4 - width) / 2
                val offsetY = (4 - height) / 2

                // Draw each cell of the tetromino
                for (cell in shape) {
                    val x = cell.x - minX + offsetX
                    val y = cell.y - minY + offsetY

                    // Draw cell
                    drawRect(
                        color = nextTetromino!!.getColor(),
                        topLeft = Offset(x * cellSizePx, y * cellSizePx),
                        size = Size(cellSizePx, cellSizePx)
                    )

                    // Draw cell border
                    drawRect(
                        color = Color(0xFF2A2A2A),
                        topLeft = Offset(x * cellSizePx, y * cellSizePx),
                        size = Size(cellSizePx, cellSizePx),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1f)
                    )
                }
            }
        }
    }
}

// Component to display game statistics
@Composable
fun GameStats(
    viewModel: TetrisViewModel,
    modifier: Modifier = Modifier
) {
    val score by viewModel.score.collectAsState()

    Column(
        modifier = modifier
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "SCORE",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
        Text(
            text = "${score.score}",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "LINES",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
        Text(
            text = "${score.lines}",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "LEVEL",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
        Text(
            text = "${score.level}",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

// Game control buttons
@Composable
fun GameControls(
    viewModel: TetrisViewModel,
    modifier: Modifier = Modifier
) {
    val gameState by viewModel.gameState.collectAsState()

    Box(
        modifier = modifier
            .width(500.dp)
            .height(500.dp),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Game control buttons (Start/Pause/Reset)
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        when (gameState) {
                            GameState.PLAYING -> viewModel.pauseGame()
                            else -> viewModel.startGame()
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = when (gameState) {
                            GameState.PLAYING -> "PAUSE"
                            GameState.PAUSED -> "RESUME"
                            else -> "START"
                        }
                    )
                }

                Button(
                    onClick = { viewModel.resetGame() },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                ) {
                    Text("RESET")
                }

                if (LocalSpatialCapabilities.current.isSpatialUiEnabled) {
                    val session = LocalSession.current
                    HomeSpaceModeIconButton(
                        onClick = { session?.spatialEnvironment?.requestHomeSpaceMode() },
                        modifier = Modifier.padding(32.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Tetromino movement controls
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = { viewModel.moveLeft() },
                    enabled = gameState == GameState.PLAYING,
                    modifier = Modifier.size(64.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text("←", fontSize = 24.sp)
                }

                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Button(
                        onClick = { viewModel.rotate() },
                        enabled = gameState == GameState.PLAYING,
                        modifier = Modifier.size(64.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text("↻", fontSize = 24.sp)
                    }

                    Button(
                        onClick = { viewModel.moveDown() },
                        enabled = gameState == GameState.PLAYING,
                        modifier = Modifier.size(64.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text("↓", fontSize = 24.sp)
                    }

                    Button(
                        onClick = { viewModel.hardDrop() },
                        enabled = gameState == GameState.PLAYING,
                        modifier = Modifier.size(64.dp),
                        contentPadding = PaddingValues(0.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                            contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    ) {
                        Text("⤓", fontSize = 24.sp)
                    }
                }

                Button(
                    onClick = { viewModel.moveRight() },
                    enabled = gameState == GameState.PLAYING,
                    modifier = Modifier.size(64.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text("→", fontSize = 24.sp)
                }
            }
        }
    }

}

// Game over overlay
@Composable
fun GameOverOverlay(
    onRestartClick: () -> Unit,
    score: Int,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xAA000000)),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .width(280.dp)
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "GAME OVER",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.error
                )

                Text(
                    text = "Score: $score",
                    style = MaterialTheme.typography.titleLarge
                )

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = onRestartClick,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("PLAY AGAIN")
                }
            }
        }
    }
}


@Composable
fun FullSpaceModeIconButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    IconButton(onClick = onClick, modifier = modifier) {
        Icon(
            painter = painterResource(id = R.drawable.ic_full_space_mode_switch),
            contentDescription = stringResource(R.string.switch_to_full_space_mode)
        )
    }
}

@Composable
fun HomeSpaceModeIconButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    FilledTonalIconButton(onClick = onClick, modifier = modifier) {
        Icon(
            painter = painterResource(id = R.drawable.ic_home_space_mode_switch),
            contentDescription = stringResource(R.string.switch_to_home_space_mode)
        )
    }
}