package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.LudoRoom
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

@Composable
fun RoomWaitingScreen(
    room: LudoRoom,
    currentUserId: String,
    onStartGame: () -> Unit,
    onStartGameWithBots: () -> Unit,
    onAddBot: () -> Unit,
    onRemovePlayer: (String) -> Unit,
    onLeaveRoom: () -> Unit
) {
    val context = LocalContext.current
    val isHost = room.hostId == currentUserId
    val canStart = isHost && room.players.size >= 2

    BackHandler {
        onLeaveRoom()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LudoBg)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Lobby",
                fontFamily = FontFamily.Serif,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = LudoAccent
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Room Code Card
            Card(
                colors = CardDefaults.cardColors(containerColor = LudoPanel),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.5.dp, LudoAccentDim, RoundedCornerShape(16.dp))
                    .clickable {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("Ludo Room Code", room.roomId)
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(context, "Room code copied: ${room.roomId}", Toast.LENGTH_SHORT).show()
                    }
                    .testTag("room_code_card")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "ROOM CODE",
                        color = LudoMuted,
                        fontSize = 12.sp,
                        letterSpacing = 2.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = room.roomId,
                            fontFamily = FontFamily.Serif,
                            fontSize = 38.sp,
                            fontWeight = FontWeight.Bold,
                            color = LudoAccent,
                            letterSpacing = 4.sp
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy code",
                            tint = LudoAccentDim,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Text(
                        text = "Tap to copy and share with friends",
                        color = LudoMuted,
                        fontSize = 11.sp
                    )
                }
            }

            if (room.noAiMode) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = LudoPanelLight),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                        .border(1.dp, LudoAccent, RoundedCornerShape(12.dp))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("⚔️", fontSize = 24.sp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "2 Players (Without AI Assistant)",
                                fontWeight = FontWeight.Bold,
                                color = LudoAccent,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "Game do bando k darmayan kheli jaye gi without any AI assistant.",
                                fontSize = 11.sp,
                                color = LudoMuted
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Player Slots
            Text(
                text = "PLAYERS (${room.players.size}/${room.maxPlayers})",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = LudoMuted,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                room.players.forEach { player ->
                    val playerColor = when (player.color) {
                        "red" -> LudoRed
                        "green" -> LudoGreen
                        "yellow" -> LudoYellow
                        "blue" -> LudoBlue
                        else -> LudoAccent
                    }
                    Card(
                        colors = CardDefaults.cardColors(containerColor = LudoPanelLight),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, playerColor.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(playerColor),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = if (player.id == currentUserId) "${player.name} (You)" else player.name,
                                        fontWeight = FontWeight.SemiBold,
                                        color = LudoText,
                                        fontSize = 15.sp
                                    )
                                    val roleText = when {
                                        player.isHost -> "Host"
                                        player.isBot -> "AI Bot"
                                        else -> "Ready"
                                    }
                                    Text(
                                        text = "${player.color.uppercase()} • $roleText",
                                        color = playerColor,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (player.isHost) {
                                    Text(text = "👑", fontSize = 16.sp)
                                } else if (isHost && player.isBot) {
                                    Button(
                                        onClick = { onRemovePlayer(player.id) },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent, contentColor = LudoMuted),
                                        contentPadding = androidx.compose.foundation.layout.PaddingValues(4.dp),
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Text("✕", fontSize = 14.sp)
                                    }
                                }
                            }
                        }
                    }
                }

                // Add AI Bot button for host when room not full (disabled in 2-Player no AI mode)
                if (!room.noAiMode && isHost && room.players.size < room.maxPlayers) {
                    OutlinedButton(
                        onClick = onAddBot,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = LudoAccent),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .border(1.dp, LudoAccentDim, RoundedCornerShape(12.dp))
                            .testTag("add_bot_button")
                    ) {
                        Text("+ Add AI Player", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    }
                }

                // Empty slots
                val emptyCount = (room.maxPlayers - room.players.size).toInt()
                for (i in 0 until emptyCount) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = LudoPanel.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, Color(0x18FFFFFF), RoundedCornerShape(12.dp))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(Color(0x1AFFFFFF)),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = LudoAccentDim,
                                    strokeWidth = 2.dp
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = if (room.noAiMode) "Empty slot • Waiting for 2nd player to join..." else "Empty slot • Waiting for player or bot...",
                                color = LudoMuted,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
        }

        // Action Buttons
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (isHost) {
                if (canStart) {
                    Button(
                        onClick = onStartGame,
                        colors = ButtonDefaults.buttonColors(containerColor = LudoAccent, contentColor = Color(0xFF1C140C)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("start_game_button")
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (room.noAiMode) "Start 2-Player Match (No AI)" else "Start Game (${room.players.size} Players)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                } else if (!room.noAiMode) {
                    Button(
                        onClick = onStartGameWithBots,
                        colors = ButtonDefaults.buttonColors(containerColor = LudoAccent, contentColor = Color(0xFF1C140C)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("start_game_with_bots_button")
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Start Game with AI Bots",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                } else {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = LudoPanel),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), color = LudoAccent, strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Waiting for 2nd human player to join...",
                                color = LudoText,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            } else {
                Card(
                    colors = CardDefaults.cardColors(containerColor = LudoPanel),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = LudoAccent, strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Waiting for host to start...",
                            color = LudoText,
                            fontSize = 14.sp
                        )
                    }
                }
            }

            OutlinedButton(
                onClick = onLeaveRoom,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = LudoText),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .border(1.dp, LudoAccentDim, RoundedCornerShape(12.dp))
                    .testTag("leave_room_button")
            ) {
                Text("Leave Room")
            }
        }
    }
}
