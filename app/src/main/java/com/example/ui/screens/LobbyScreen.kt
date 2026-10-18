package com.example.ui.screens

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
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import com.example.data.model.LudoRoom
import com.example.data.model.UserProfile
import com.example.ui.theme.LudoAccent
import com.example.ui.theme.LudoAccentDim
import com.example.ui.theme.LudoBg
import com.example.ui.theme.LudoMuted
import com.example.ui.theme.LudoPanel
import com.example.ui.theme.LudoPanelLight
import com.example.ui.theme.LudoText
import kotlin.random.Random

@Composable
fun LobbyScreen(
    userProfile: UserProfile?,
    displayName: String,
    publicRooms: List<LudoRoom>,
    isMatchmaking: Boolean,
    errorMessage: String?,
    onClearError: () -> Unit,
    onQuickMatch: (noAiMode: Boolean) -> Unit,
    onCreateRoom: (code: String, maxPlayers: Int, noAiMode: Boolean) -> Unit,
    onJoinRoom: (code: String) -> Unit,
    onStartLocalGame: (playerCount: Int) -> Unit,
    onSignOut: () -> Unit
) {
    var showGuide by remember { mutableStateOf(false) }
    var joinCodeInput by remember { mutableStateOf("") }
    var showCreateDialog by remember { mutableStateOf(false) }
    var showLocalDialog by remember { mutableStateOf(false) }

    if (showGuide) {
        GuideDialog(onDismiss = { showGuide = false })
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LudoBg)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(LudoAccentDim.copy(alpha = 0.4f))
                        .border(1.dp, LudoAccent, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = (userProfile?.displayName ?: displayName).take(1).uppercase(),
                        fontWeight = FontWeight.Bold,
                        color = LudoAccent,
                        fontSize = 18.sp
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = userProfile?.displayName ?: displayName,
                        style = MaterialTheme.typography.titleMedium,
                        color = LudoText
                    )
                    Text(
                        text = "Played: ${userProfile?.gamesPlayed ?: 0}  |  Won: ${userProfile?.gamesWon ?: 0}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = LudoMuted
                    )
                }
            }

            Row {
                IconButton(
                    onClick = { showGuide = true },
                    modifier = Modifier.testTag("guide_icon_button")
                ) {
                    Icon(Icons.Default.HelpOutline, contentDescription = "Game Guide", tint = LudoAccent)
                }
                IconButton(
                    onClick = onSignOut,
                    modifier = Modifier.testTag("sign_out_button")
                ) {
                    Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Sign out", tint = LudoMuted)
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Hero Title
        Text(
            text = "🎲",
            fontSize = 42.sp
        )
        Text(
            text = "Ludo",
            fontFamily = FontFamily.Serif,
            fontSize = 36.sp,
            fontWeight = FontWeight.Bold,
            color = LudoAccent,
            letterSpacing = 1.sp
        )
        Text(
            text = "Roll a six to start. First home with all four wins.",
            color = LudoMuted,
            fontSize = 13.sp,
            textAlign = TextAlign.Center
        )

        if (errorMessage != null) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0x33D05353)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(errorMessage, color = Color(0xFFFFB4AB), fontSize = 13.sp, modifier = Modifier.weight(1f))
                    Button(onClick = onClearError, colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent)) {
                        Text("✕", color = Color.White)
                    }
                }
            }
        }

        // Action Buttons
        // Action Buttons
        Button(
            onClick = { onQuickMatch(true) },
            enabled = !isMatchmaking,
            colors = ButtonDefaults.buttonColors(containerColor = LudoAccent, contentColor = Color(0xFF1C140C)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .testTag("quick_match_2p_button")
        ) {
            if (isMatchmaking) {
                CircularProgressIndicator(modifier = Modifier.size(22.dp), color = Color(0xFF1C140C), strokeWidth = 2.dp)
                Spacer(modifier = Modifier.width(10.dp))
                Text("Finding Match...", fontWeight = FontWeight.Bold)
            } else {
                Icon(Icons.Default.Group, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("2 Players (Without AI Assistant)", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text("Game do bando k darmayan • Voice & Text Chat", fontSize = 11.sp, color = Color(0xFF2C2216))
                }
            }
        }

        Button(
            onClick = { onQuickMatch(false) },
            enabled = !isMatchmaking,
            colors = ButtonDefaults.buttonColors(containerColor = LudoPanelLight, contentColor = LudoText),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .border(1.dp, LudoAccentDim, RoundedCornerShape(12.dp))
                .testTag("quick_match_4p_button")
        ) {
            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = LudoAccent)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Quick Match (4 Players Classic)", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
        }

        Button(
            onClick = { showLocalDialog = true },
            colors = ButtonDefaults.buttonColors(containerColor = LudoPanel, contentColor = LudoText),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .border(1.dp, LudoAccentDim, RoundedCornerShape(12.dp))
                .testTag("local_pass_play_button")
        ) {
            Icon(Icons.Default.SportsEsports, contentDescription = null, tint = LudoAccent)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Local Pass & Play", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
        }

        // Private Room Controls
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = LudoPanel),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, LudoAccentDim.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Play with Friends",
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = LudoAccent
                )

                // 2-Player (Without AI) Room Creation
                Button(
                    onClick = {
                        val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ"
                        val code = (1..4).map { chars.random() }.joinToString("")
                        onCreateRoom(code, 2, true)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = LudoAccent.copy(alpha = 0.15f), contentColor = LudoAccent),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.5.dp, LudoAccent, RoundedCornerShape(8.dp))
                        .testTag("create_2p_room_button")
                ) {
                    Icon(Icons.Default.Group, contentDescription = null, tint = LudoAccent)
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Create 2-Player Room (Without AI Assistant)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text("Game do bando k darmayan kheli jaye", fontSize = 10.sp, color = LudoMuted)
                    }
                }

                // 4-Player Standard Room Creation
                Button(
                    onClick = {
                        val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ"
                        val code = (1..4).map { chars.random() }.joinToString("")
                        onCreateRoom(code, 4, false)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = LudoPanelLight, contentColor = LudoText),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, LudoAccentDim, RoundedCornerShape(8.dp))
                        .testTag("create_room_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = LudoAccent)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Create 4-Player Room", fontSize = 13.sp)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = joinCodeInput,
                        onValueChange = { if (it.length <= 6) joinCodeInput = it.uppercase() },
                        placeholder = { Text("CODE", color = LudoMuted) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = LudoAccent,
                            unfocusedBorderColor = LudoAccentDim,
                            focusedTextColor = LudoText,
                            unfocusedTextColor = LudoText
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("room_code_input")
                    )

                    Button(
                        onClick = { onJoinRoom(joinCodeInput) },
                        enabled = joinCodeInput.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = LudoAccent, contentColor = Color(0xFF1C140C)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("join_room_button")
                    ) {
                        Text("Join", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Open Public Rooms List
        if (publicRooms.isNotEmpty()) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Open Matchmaking Lobbies",
                    fontFamily = FontFamily.Serif,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = LudoAccent
                )

                publicRooms.take(5).forEach { room ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = LudoPanel),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, LudoAccentDim.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = room.name.ifBlank { "Room ${room.roomId}" },
                                    fontWeight = FontWeight.SemiBold,
                                    color = LudoText
                                )
                                Text(
                                    text = "${room.players.size}/${room.maxPlayers} Players",
                                    color = LudoMuted,
                                    fontSize = 12.sp
                                )
                            }
                            Button(
                                onClick = { onJoinRoom(room.roomId) },
                                colors = ButtonDefaults.buttonColors(containerColor = LudoAccent, contentColor = Color(0xFF1C140C)),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Join")
                            }
                        }
                    }
                }
            }
        }
    }

    // Local Game Player Count Dialog
    if (showLocalDialog) {
        androidx.compose.ui.window.Dialog(onDismissRequest = { showLocalDialog = false }) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = LudoPanel),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, LudoAccentDim, RoundedCornerShape(16.dp))
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Local Pass & Play",
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = LudoAccent
                    )
                    Text(
                        text = "Select number of players:",
                        color = LudoMuted,
                        fontSize = 14.sp
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        listOf(2, 3, 4).forEach { count ->
                            Button(
                                onClick = {
                                    showLocalDialog = false
                                    onStartLocalGame(count)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = LudoAccentDim, contentColor = LudoText),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.testTag("local_players_${count}_button")
                            ) {
                                Text("$count Players", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}
