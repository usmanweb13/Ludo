package com.example.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.LudoEngine
import com.example.ui.theme.BoardCellBorder
import com.example.ui.theme.BoardGoldFrame
import com.example.ui.theme.BoardGoldFrameDark
import com.example.ui.theme.BoardTileWhite
import com.example.ui.theme.LudoAccent
import com.example.ui.theme.LudoBlue
import com.example.ui.theme.LudoGreen
import com.example.ui.theme.LudoRed
import com.example.ui.theme.LudoYellow

@Composable
fun LudoBoard(
    tokens: Map<String, List<Long>>,
    activeColors: List<String>,
    currentTurn: String,
    dice: Long,
    rolled: Boolean,
    winner: String?,
    isMyTurn: Boolean,
    onTokenClicked: (color: String, tokenIndex: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(600),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    BoxWithConstraints(
        modifier = modifier
            .aspectRatio(1f)
            .shadow(18.dp, RoundedCornerShape(24.dp))
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(BoardGoldFrame, BoardGoldFrameDark, BoardGoldFrame)
                )
            )
            .border(4.dp, BoardGoldFrameDark, RoundedCornerShape(24.dp))
            .padding(6.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(BoardTileWhite)
            .border(1.5.dp, Color(0xFF1C140C), RoundedCornerShape(18.dp))
            .testTag("ludo_board")
    ) {
        val boardPx = maxWidth
        val cellSize = boardPx / 15f

        // Draw classic board background and lines using Canvas
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val cell = w / 15f

            // 1. Fill 4 Corner Yards
            // Red Yard (Top-Left): rows 0..5, cols 0..5
            drawRect(
                color = LudoRed,
                topLeft = Offset(0f, 0f),
                size = Size(cell * 6f, cell * 6f)
            )
            // Blue Yard (Top-Right): rows 0..5, cols 9..14
            drawRect(
                color = LudoBlue,
                topLeft = Offset(cell * 9f, 0f),
                size = Size(cell * 6f, cell * 6f)
            )
            // Green Yard (Bottom-Left): rows 9..14, cols 0..5
            drawRect(
                color = LudoGreen,
                topLeft = Offset(0f, cell * 9f),
                size = Size(cell * 6f, cell * 6f)
            )
            // Yellow Yard (Bottom-Right): rows 9..14, cols 9..14
            drawRect(
                color = LudoYellow,
                topLeft = Offset(cell * 9f, cell * 9f),
                size = Size(cell * 6f, cell * 6f)
            )

            // 2. Colored Home Paths (5 cells each)
            // Red Home Path: row 7, cols 1..5
            for (c in 1..5) {
                drawRect(color = LudoRed, topLeft = Offset(cell * c, cell * 7), size = Size(cell, cell))
            }
            // Blue Home Path: col 7, rows 1..5
            for (r in 1..5) {
                drawRect(color = LudoBlue, topLeft = Offset(cell * 7, cell * r), size = Size(cell, cell))
            }
            // Yellow Home Path: row 7, cols 9..13
            for (c in 9..13) {
                drawRect(color = LudoYellow, topLeft = Offset(cell * c, cell * 7), size = Size(cell, cell))
            }
            // Green Home Path: col 7, rows 9..13
            for (r in 9..13) {
                drawRect(color = LudoGreen, topLeft = Offset(cell * 7, cell * r), size = Size(cell, cell))
            }

            // 3. Colored Start Cells
            // Red Start: row 6, col 1
            drawRect(color = LudoRed, topLeft = Offset(cell * 1, cell * 6), size = Size(cell, cell))
            // Blue Start: row 1, col 8
            drawRect(color = LudoBlue, topLeft = Offset(cell * 8, cell * 1), size = Size(cell, cell))
            // Yellow Start: row 8, col 13
            drawRect(color = LudoYellow, topLeft = Offset(cell * 13, cell * 8), size = Size(cell, cell))
            // Green Start: row 13, col 6
            drawRect(color = LudoGreen, topLeft = Offset(cell * 6, cell * 13), size = Size(cell, cell))

            // 4. Center Triangles (rows 6..8, cols 6..8)
            val centerLeft = cell * 6f
            val centerTop = cell * 6f
            val centerRight = cell * 9f
            val centerBottom = cell * 9f
            val midX = cell * 7.5f
            val midY = cell * 7.5f

            // Red triangle (Left)
            drawPath(
                path = Path().apply {
                    moveTo(centerLeft, centerTop)
                    lineTo(midX, midY)
                    lineTo(centerLeft, centerBottom)
                    close()
                },
                color = LudoRed
            )
            // Blue triangle (Top)
            drawPath(
                path = Path().apply {
                    moveTo(centerLeft, centerTop)
                    lineTo(centerRight, centerTop)
                    lineTo(midX, midY)
                    close()
                },
                color = LudoBlue
            )
            // Yellow triangle (Right)
            drawPath(
                path = Path().apply {
                    moveTo(centerRight, centerTop)
                    lineTo(centerRight, centerBottom)
                    lineTo(midX, midY)
                    close()
                },
                color = LudoYellow
            )
            // Green triangle (Bottom)
            drawPath(
                path = Path().apply {
                    moveTo(centerLeft, centerBottom)
                    lineTo(centerRight, centerBottom)
                    lineTo(midX, midY)
                    close()
                },
                color = LudoGreen
            )

            // 5. Draw Track Grid lines
            val strokeWidth = 1.2f
            val gridColor = Color(0xFF222222)

            // Horizontal lines across cross arms
            for (r in 0..15) {
                val y = r * cell
                if (r in 0..6 || r in 9..15) {
                    // Only across center 3 columns (6, 7, 8)
                    drawLine(gridColor, Offset(cell * 6, y), Offset(cell * 9, y), strokeWidth)
                } else if (r in 6..9) {
                    // Full width across left and right arms
                    drawLine(gridColor, Offset(0f, y), Offset(w, y), strokeWidth)
                }
            }

            // Vertical lines across cross arms
            for (c in 0..15) {
                val x = c * cell
                if (c in 0..6 || c in 9..15) {
                    // Only across center 3 rows (6, 7, 8)
                    drawLine(gridColor, Offset(x, cell * 6), Offset(x, cell * 9), strokeWidth)
                } else if (c in 6..9) {
                    // Full height across top and bottom arms
                    drawLine(gridColor, Offset(x, 0f), Offset(x, h), strokeWidth)
                }
            }

            // Outer borders of yards & center box
            drawRect(
                color = gridColor,
                topLeft = Offset(0f, 0f),
                size = Size(cell * 6f, cell * 6f),
                style = Stroke(2f)
            )
            drawRect(
                color = gridColor,
                topLeft = Offset(cell * 9f, 0f),
                size = Size(cell * 6f, cell * 6f),
                style = Stroke(2f)
            )
            drawRect(
                color = gridColor,
                topLeft = Offset(0f, cell * 9f),
                size = Size(cell * 6f, cell * 6f),
                style = Stroke(2f)
            )
            drawRect(
                color = gridColor,
                topLeft = Offset(cell * 9f, cell * 9f),
                size = Size(cell * 6f, cell * 6f),
                style = Stroke(2f)
            )
            drawRect(
                color = gridColor,
                topLeft = Offset(cell * 6f, cell * 6f),
                size = Size(cell * 3f, cell * 3f),
                style = Stroke(2f)
            )
        }

        // 6. Yard Inner White Rounded Cards with 4 Token Base Circles
        ClassicYardCard(
            yardColor = LudoRed,
            leftCol = 0,
            topRow = 0,
            cellSize = cellSize
        )
        ClassicYardCard(
            yardColor = LudoBlue,
            leftCol = 9,
            topRow = 0,
            cellSize = cellSize
        )
        ClassicYardCard(
            yardColor = LudoGreen,
            leftCol = 0,
            topRow = 9,
            cellSize = cellSize
        )
        ClassicYardCard(
            yardColor = LudoYellow,
            leftCol = 9,
            topRow = 9,
            cellSize = cellSize
        )

        // 7. Safe Stars on the track cells
        // Red start star (row 6, col 1) - White star on red
        SafeStarOverlay(row = 6, col = 1, cellSize = cellSize, starColor = Color.White)
        // Red arm safe star (row 8, col 2) - Red star on white
        SafeStarOverlay(row = 8, col = 2, cellSize = cellSize, starColor = LudoRed)

        // Blue start star (row 1, col 8) - White star on blue
        SafeStarOverlay(row = 1, col = 8, cellSize = cellSize, starColor = Color.White)
        // Top arm safe star (row 2, col 6) - Blue star on white
        SafeStarOverlay(row = 2, col = 6, cellSize = cellSize, starColor = LudoBlue)

        // Yellow start star (row 8, col 13) - White star on yellow
        SafeStarOverlay(row = 8, col = 13, cellSize = cellSize, starColor = Color.White)
        // Right arm safe star (row 6, col 12) - Yellow star on white
        SafeStarOverlay(row = 6, col = 12, cellSize = cellSize, starColor = LudoYellow)

        // Green start star (row 13, col 6) - White star on green
        SafeStarOverlay(row = 13, col = 6, cellSize = cellSize, starColor = Color.White)
        // Bottom arm safe star (row 12, col 8) - Green star on white
        SafeStarOverlay(row = 12, col = 8, cellSize = cellSize, starColor = LudoGreen)

        // 8. Draw Player Tokens
        val tokenSize = cellSize * 0.78f

        activeColors.forEach { color ->
            val colorTokens = tokens[color] ?: listOf(0L, 0L, 0L, 0L)
            val movableIndices = if (isMyTurn && currentTurn == color && rolled && dice > 0L && winner == null) {
                LudoEngine.getMovableTokenIndices(colorTokens, dice)
            } else {
                emptyList()
            }

            colorTokens.forEachIndexed { index, steps ->
                val (gridRow, gridCol) = getTokenGridPosition(color, index, steps)
                val isMovable = movableIndices.contains(index)
                val tokenColor = when (color) {
                    "red" -> LudoRed
                    "blue" -> LudoBlue
                    "yellow" -> LudoYellow
                    "green" -> LudoGreen
                    else -> LudoAccent
                }

                val centerX = cellSize * (gridCol + 0.5f)
                val centerY = cellSize * (gridRow + 0.5f)

                Box(
                    modifier = Modifier
                        .size(tokenSize)
                        .offset(
                            x = centerX - (tokenSize / 2),
                            y = centerY - (tokenSize / 2)
                        )
                        .scale(if (isMovable) pulseScale else 1.0f)
                        .shadow(if (isMovable) 8.dp else 4.dp, CircleShape)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(Color.White, tokenColor, tokenColor.copy(alpha = 0.9f))
                            )
                        )
                        .border(
                            width = if (isMovable) 3.dp else 1.8.dp,
                            color = if (isMovable) Color.White else Color(0xDD000000),
                            shape = CircleShape
                        )
                        .clickable(
                            enabled = isMovable,
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            onTokenClicked(color, index)
                        }
                        .testTag("token_${color}_$index"),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(tokenSize * 0.42f)
                            .clip(CircleShape)
                            .background(Color.White)
                            .border(1.dp, tokenColor, CircleShape)
                    )
                }
            }
        }
    }
}

@Composable
private fun ClassicYardCard(
    yardColor: Color,
    leftCol: Int,
    topRow: Int,
    cellSize: Dp
) {
    val cardSize = cellSize * 4.4f
    val offsetX = cellSize * (leftCol + 0.8f)
    val offsetY = cellSize * (topRow + 0.8f)

    Box(
        modifier = Modifier
            .size(cardSize)
            .offset(x = offsetX, y = offsetY)
            .shadow(4.dp, RoundedCornerShape(18.dp))
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White)
            .border(2.dp, yardColor.copy(alpha = 0.6f), RoundedCornerShape(18.dp))
            .padding(cellSize * 0.35f)
    ) {
        val slotSize = cellSize * 1.25f
        val fracs = listOf(
            Pair(0.12f, 0.12f),
            Pair(0.58f, 0.12f),
            Pair(0.12f, 0.58f),
            Pair(0.58f, 0.58f)
        )
        fracs.forEach { (fx, fy) ->
            Box(
                modifier = Modifier
                    .size(slotSize)
                    .offset(x = cardSize * fx, y = cardSize * fy)
                    .clip(CircleShape)
                    .background(yardColor)
                    .border(1.5.dp, Color(0x33000000), CircleShape)
            )
        }
    }
}

@Composable
private fun SafeStarOverlay(
    row: Int,
    col: Int,
    cellSize: Dp,
    starColor: Color
) {
    Box(
        modifier = Modifier
            .size(cellSize)
            .offset(x = cellSize * col.toFloat(), y = cellSize * row.toFloat()),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Star,
            contentDescription = "Safe star",
            tint = starColor,
            modifier = Modifier.size(cellSize * 0.65f)
        )
    }
}

fun getTrackCell(index: Int): Pair<Int, Int> {
    val i = ((index % 52) + 52) % 52
    return when (i) {
        // Red arm going right (5 squares)
        0 -> Pair(6, 1)  // Red Start
        1 -> Pair(6, 2)
        2 -> Pair(6, 3)
        3 -> Pair(6, 4)
        4 -> Pair(6, 5)
        // Turning up into top arm (6 squares)
        5 -> Pair(5, 6)
        6 -> Pair(4, 6)
        7 -> Pair(3, 6)
        8 -> Pair(2, 6)  // Top safe star
        9 -> Pair(1, 6)
        10 -> Pair(0, 6)
        // Crossing top (2 squares)
        11 -> Pair(0, 7)
        12 -> Pair(0, 8)
        // Going down right side of top arm (5 squares)
        13 -> Pair(1, 8) // Blue Start
        14 -> Pair(2, 8)
        15 -> Pair(3, 8)
        16 -> Pair(4, 8)
        17 -> Pair(5, 8)
        // Turning right into right arm (6 squares)
        18 -> Pair(6, 9)
        19 -> Pair(6, 10)
        20 -> Pair(6, 11)
        21 -> Pair(6, 12) // Right safe star
        22 -> Pair(6, 13)
        23 -> Pair(6, 14)
        // Crossing right end (2 squares)
        24 -> Pair(7, 14)
        25 -> Pair(8, 14)
        // Going left along bottom of right arm (5 squares)
        26 -> Pair(8, 13) // Yellow Start
        27 -> Pair(8, 12)
        28 -> Pair(8, 11)
        29 -> Pair(8, 10)
        30 -> Pair(8, 9)
        // Turning down into bottom arm (6 squares)
        31 -> Pair(9, 8)
        32 -> Pair(10, 8)
        33 -> Pair(11, 8)
        34 -> Pair(12, 8) // Bottom safe star
        35 -> Pair(13, 8)
        36 -> Pair(14, 8)
        // Crossing bottom (2 squares)
        37 -> Pair(14, 7)
        38 -> Pair(14, 6)
        // Going up left side of bottom arm (5 squares)
        39 -> Pair(13, 6) // Green Start
        40 -> Pair(12, 6)
        41 -> Pair(11, 6)
        42 -> Pair(10, 6)
        43 -> Pair(9, 6)
        // Turning left into left arm (6 squares)
        44 -> Pair(8, 5)
        45 -> Pair(8, 4)
        46 -> Pair(8, 3)
        47 -> Pair(8, 2)  // Left safe star
        48 -> Pair(8, 1)
        49 -> Pair(8, 0)
        // Crossing left end (2 squares)
        50 -> Pair(7, 0)
        51 -> Pair(6, 0)
        else -> Pair(6, 1)
    }
}

fun getTokenGridPosition(color: String, index: Int, steps: Long): Pair<Float, Float> {
    if (steps == 0L) {
        // In Yard 2x2 white card slots
        return when (color) {
            "red" -> when (index) {
                0 -> Pair(1.85f, 1.85f)
                1 -> Pair(1.85f, 3.85f)
                2 -> Pair(3.85f, 1.85f)
                else -> Pair(3.85f, 3.85f)
            }
            "blue" -> when (index) {
                0 -> Pair(1.85f, 10.85f)
                1 -> Pair(1.85f, 12.85f)
                2 -> Pair(3.85f, 10.85f)
                else -> Pair(3.85f, 12.85f)
            }
            "yellow" -> when (index) {
                0 -> Pair(10.85f, 10.85f)
                1 -> Pair(10.85f, 12.85f)
                2 -> Pair(12.85f, 10.85f)
                else -> Pair(12.85f, 12.85f)
            }
            else -> when (index) { // green
                0 -> Pair(10.85f, 1.85f)
                1 -> Pair(10.85f, 3.85f)
                2 -> Pair(12.85f, 1.85f)
                else -> Pair(12.85f, 3.85f)
            }
        }
    } else if (steps in 1L..51L) {
        val ring = LudoEngine.absRing(color, steps)
        val (r, c) = getTrackCell(ring)
        return Pair(r.toFloat(), c.toFloat())
    } else if (steps < 58L) {
        // Steps 52..57 (Home stretch)
        val stretchIdx = (steps - 52).toInt().coerceIn(0, 5)
        return when (color) {
            "red" -> Pair(7f, (1 + stretchIdx).toFloat())
            "blue" -> Pair((1 + stretchIdx).toFloat(), 7f)
            "yellow" -> Pair(7f, (13 - stretchIdx).toFloat())
            else -> Pair((13 - stretchIdx).toFloat(), 7f) // green
        }
    } else {
        // Step 58 (Center Home Triangle)
        return when (color) {
            "red" -> Pair(7.0f, 6.4f)
            "blue" -> Pair(6.4f, 7.0f)
            "yellow" -> Pair(7.0f, 7.6f)
            else -> Pair(7.6f, 7.0f) // green
        }
    }
}
