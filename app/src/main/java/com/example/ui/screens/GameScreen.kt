package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.LudoEngine
import com.example.ui.components.DiceView
import com.example.ui.components.LudoBoard
import com.example.ui.theme.LudoAccent
import com.example.ui.theme.LudoAccentDim
import com.example.ui.theme.LudoBg
import com.example.ui.theme.LudoBlue
import com.example.ui.theme.LudoGreen
import com.example.ui.theme.LudoMuted
import com.example.ui.theme.LudoPanel
import com.example.ui.theme.LudoPanelLight
import com.example.ui.theme.LudoRed
import com.example.ui.theme.LudoText
import com.example.ui.theme.LudoYellow

import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.runtime.LaunchedEffect
import com.example.data.model.ChatMessage
import com.example.ui.sound.SoundManager
import kotlinx.coroutines.delay

@Composable
fun GameScreen(
    title: String,
    tokens: Map<String, List<Long>>,
    activeColors: List<String>,
    currentTurn: String,
    dice: Long,
    rolled: Boolean,
    winner: String?,
    isMyTurn: Boolean,
    isRolling: Boolean,
    playerLabels: Map<String, String> = emptyMap(),
    soundManager: SoundManager? = null,
    errorMessage: String? = null,
    onClearError: (() -> Unit)? = null,
    noAiMode: Boolean = false,
    messages: List<ChatMessage> = emptyList(),
    currentUserId: String = "",
    myColor: String = "red",
    isMicOn: Boolean = false,
    isSpeaking: Boolean = false,
    audioLevel: Float = 0f,
    voiceState: Map<String, Boolean>? = null,
    onToggleMic: () -> Unit = {},
    onPushToTalk: (Boolean) -> Unit = {},
    onSendMessage: (String) -> Unit = {},
    onRollDice: () -> Unit,
    onTokenClicked: (color: String, index: Int) -> Unit,
    onExitGame: () -> Unit,
    onPlayAgain: (() -> Unit)? = null
) {
    var showGuide by remember { mutableStateOf(false) }
    var showExitConfirm by remember { mutableStateOf(false) }
    var showHelper by remember { mutableStateOf(false) }
    var showChat by remember { mutableStateOf(false) }
    var latestMessageToast by remember { mutableStateOf<ChatMessage?>(null) }
    var soundEnabled by remember { mutableStateOf(soundManager?.isSoundEnabled ?: true) }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            val last = messages.last()
            if (last.senderId != currentUserId) {
                latestMessageToast = last
                delay(4000L)
                if (latestMessageToast == last) {
                    latestMessageToast = null
                }
            }
        }
    }

    LaunchedEffect(dice) {
        if (dice == 6L) {
            soundManager?.playRollSix()
        }
    }

    LaunchedEffect(winner) {
        if (winner != null && winner.isNotBlank()) {
            soundManager?.playVictory()
        }
    }

    BackHandler {
        showExitConfirm = true
    }

    if (showGuide) {
        GuideDialog(onDismiss = { showGuide = false })
    }

    if (showExitConfirm) {
        AlertDialog(
            onDismissRequest = { showExitConfirm = false },
            title = {
                Text(
                    "Leave Game?",
                    color = LudoAccent,
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    "Are you sure you want to exit to the lobby?",
                    color = LudoText
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showExitConfirm = false
                        onExitGame()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = LudoRed),
                    modifier = Modifier.testTag("confirm_exit_button")
                ) {
                    Text("Leave")
                }
            },
            dismissButton = {
                Button(
                    onClick = { showExitConfirm = false },
                    colors = ButtonDefaults.buttonColors(containerColor = LudoPanel)
                ) {
                    Text("Stay")
                }
            },
            containerColor = LudoPanel
        )
    }

    if (showHelper) {
        HelperAssistantDialog(
            currentTurn = currentTurn,
            dice = dice,
            rolled = rolled,
            isMyTurn = isMyTurn,
            tokens = tokens,
            winner = winner,
            noAiMode = noAiMode,
            playerLabels = playerLabels,
            onDismiss = { showHelper = false }
        )
    }

    if (showChat) {
        InGameChatSheet(
            messages = messages,
            currentUserId = currentUserId,
            myColor = myColor,
            isMicOn = isMicOn,
            isSpeaking = isSpeaking,
            audioLevel = audioLevel,
            voiceState = voiceState,
            onToggleMic = onToggleMic,
            onPushToTalk = onPushToTalk,
            onSendMessage = onSendMessage,
            onDismiss = { showChat = false }
        )
    }

    val turnColor = when (currentTurn) {
        "red" -> LudoRed
        "green" -> LudoGreen
        "yellow" -> LudoYellow
        "blue" -> LudoBlue
        else -> LudoAccent
    }

    val currentTurnTokens = tokens[currentTurn] ?: listOf(0L, 0L, 0L, 0L)
    val movableTokens = if (rolled && dice > 0L) {
        LudoEngine.getMovableTokenIndices(currentTurnTokens, dice)
    } else emptyList()

    val statusText = when {
        winner != null && winner.isNotBlank() -> "${winner.uppercase()} WINS!"
        !isMyTurn -> {
            val label = playerLabels[currentTurn] ?: currentTurn.uppercase()
            "$label's Turn (Waiting...)"
        }
        !rolled -> "Your Turn! Roll the die."
        movableTokens.isEmpty() -> if (dice == 6L) "Rolled 6! No moves, rolling again..." else if (dice > 0L) "Rolled $dice! No moves possible. Passing turn..." else "Your Turn! Roll the die."
        else -> "Rolled $dice! Tap a glowing token to move."
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LudoBg)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { showExitConfirm = true },
                modifier = Modifier.testTag("game_back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = LudoAccent
                )
            }

            Text(
                text = title,
                fontFamily = FontFamily.Serif,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = LudoAccent
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                // AI Helper button
                IconButton(
                    onClick = { showHelper = true },
                    modifier = Modifier.testTag("game_helper_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Psychology,
                        contentDescription = "Helper AI",
                        tint = LudoAccent
                    )
                }

                // Chat button
                IconButton(
                    onClick = { showChat = true },
                    modifier = Modifier.testTag("game_chat_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Chat,
                        contentDescription = "Chat",
                        tint = if (messages.isNotEmpty()) LudoAccent else LudoMuted
                    )
                }

                // Voice Mic button
                IconButton(
                    onClick = onToggleMic,
                    modifier = Modifier.testTag("game_mic_button")
                ) {
                    Icon(
                        imageVector = if (isMicOn) Icons.Default.Mic else Icons.Default.MicOff,
                        contentDescription = "Mic",
                        tint = if (isSpeaking) Color(0xFF4CAF50) else if (isMicOn) LudoAccent else LudoMuted
                    )
                }

                IconButton(
                    onClick = {
                        soundEnabled = !soundEnabled
                        soundManager?.isSoundEnabled = soundEnabled
                    },
                    modifier = Modifier.testTag("sound_toggle_button")
                ) {
                    Icon(
                        imageVector = if (soundEnabled) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                        contentDescription = "Toggle sound",
                        tint = if (soundEnabled) LudoAccent else LudoMuted
                    )
                }

                IconButton(
                    onClick = { showGuide = true },
                    modifier = Modifier.testTag("game_guide_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.HelpOutline,
                        contentDescription = "Guide",
                        tint = LudoAccent
                    )
                }
            }
        }

        if (errorMessage != null) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0x44D05353)),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = errorMessage,
                        color = Color(0xFFFFB4AB),
                        fontSize = 12.sp,
                        modifier = Modifier.weight(1f)
                    )
                    if (onClearError != null) {
                        Button(
                            onClick = onClearError,
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(4.dp)
                        ) {
                            Text("✕", color = Color.White, fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // Status Card
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = LudoPanel),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, turnColor.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(turnColor)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = statusText,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = LudoText,
                    textAlign = TextAlign.Center
                )
            }
        }

        // Floating Opponent Voice & Chat Banner
        val opponentSpeaking = voiceState?.any { it.value && it.key != currentUserId } == true
        if (opponentSpeaking || latestMessageToast != null) {
            Card(
                colors = CardDefaults.cardColors(containerColor = LudoPanelLight),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, LudoAccent, RoundedCornerShape(10.dp))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    if (opponentSpeaking) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("🎙️", fontSize = 16.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Opponent is speaking...", color = Color(0xFF4CAF50), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    } else if (latestMessageToast != null) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("💬", fontSize = 14.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("${latestMessageToast?.senderName}: ${latestMessageToast?.message}", color = LudoText, fontSize = 12.sp)
                        }
                    }

                    Button(
                        onClick = { showChat = true },
                        colors = ButtonDefaults.buttonColors(containerColor = LudoAccent.copy(alpha = 0.2f), contentColor = LudoAccent),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text("Chat", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Board
        LudoBoard(
            tokens = tokens,
            activeColors = activeColors,
            currentTurn = currentTurn,
            dice = dice,
            rolled = rolled,
            winner = winner,
            isMyTurn = isMyTurn,
            onTokenClicked = { color, index ->
                soundManager?.playTokenStep()
                onTokenClicked(color, index)
            },
            modifier = Modifier.padding(vertical = 4.dp)
        )

        // Helper AI & Chat quick trigger bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = { showHelper = true },
                colors = ButtonDefaults.buttonColors(containerColor = LudoPanelLight, contentColor = LudoAccent),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .border(1.dp, LudoAccentDim, RoundedCornerShape(20.dp))
                    .testTag("open_helper_button")
            ) {
                Icon(Icons.Default.Psychology, contentDescription = null, modifier = Modifier.size(18.dp), tint = LudoAccent)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Helper AI (مددگار)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }

            Button(
                onClick = { showChat = true },
                colors = ButtonDefaults.buttonColors(containerColor = LudoPanelLight, contentColor = LudoText),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .border(1.dp, LudoAccentDim, RoundedCornerShape(20.dp))
                    .testTag("open_chat_button")
            ) {
                Icon(Icons.Default.Chat, contentDescription = null, modifier = Modifier.size(16.dp), tint = LudoAccent)
                Spacer(modifier = Modifier.width(6.dp))
                Text(if (isMicOn) "Voice ON 🎙️" else "Chat & Voice 💬", fontSize = 12.sp)
            }
        }

        // Bottom Controls
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(turnColor)
            )

            Spacer(modifier = Modifier.width(20.dp))

            DiceView(
                diceValue = dice,
                isRolling = isRolling,
                enabled = isMyTurn && !rolled && winner == null,
                onRollClicked = {
                    soundManager?.playDiceRoll()
                    onRollDice()
                }
            )

            Spacer(modifier = Modifier.width(20.dp))

            Button(
                onClick = {
                    soundManager?.playDiceRoll()
                    onRollDice()
                },
                enabled = isMyTurn && !rolled && winner == null,
                colors = ButtonDefaults.buttonColors(containerColor = LudoAccent, contentColor = Color(0xFF1C140C)),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("roll_dice_action_button")
            ) {
                Text("Roll", fontWeight = FontWeight.Bold)
            }
        }

        // Winner Banner
        AnimatedVisibility(
            visible = winner != null && winner.isNotBlank(),
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = LudoPanel),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(2.dp, LudoAccent, RoundedCornerShape(16.dp))
                    .padding(vertical = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("👑", fontSize = 36.sp)
                    Text(
                        text = "${winner?.uppercase()} WINS!",
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp,
                        color = LudoAccent
                    )
                    Text(
                        text = "Congratulations! All 4 tokens brought safely home.",
                        color = LudoMuted,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (onPlayAgain != null) {
                            Button(
                                onClick = onPlayAgain,
                                colors = ButtonDefaults.buttonColors(containerColor = LudoAccent, contentColor = Color(0xFF1C140C)),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Play Again", fontWeight = FontWeight.Bold)
                            }
                        }
                        Button(
                            onClick = onExitGame,
                            colors = ButtonDefaults.buttonColors(containerColor = LudoPanelDim(), contentColor = LudoText),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .border(1.dp, LudoAccentDim, RoundedCornerShape(8.dp))
                        ) {
                            Text("Lobby")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LudoPanelDim(): Color = Color(0xFF2A2016)
