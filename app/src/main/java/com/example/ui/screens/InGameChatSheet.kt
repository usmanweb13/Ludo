package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.ChatMessage
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun InGameChatSheet(
    messages: List<ChatMessage>,
    currentUserId: String,
    myColor: String,
    isMicOn: Boolean,
    isSpeaking: Boolean,
    audioLevel: Float,
    voiceState: Map<String, Boolean>?,
    onToggleMic: () -> Unit,
    onPushToTalk: (Boolean) -> Unit,
    onSendMessage: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var textInput by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    val quickChats = listOf(
        "🎲 Roll 6 please!",
        "⏳ Khel bhai jaldi!",
        "👏 Well played!",
        "😂 Mera token chhod do!",
        "🔥 Good game!",
        "😎 GG!",
        "⚡ Lag bilkul nahi!"
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .heightIn(max = 620.dp)
                .clip(RoundedCornerShape(20.dp)),
            color = LudoBg
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp)
            ) {
                // Header with Voice Controls
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Live Chat & Voice",
                            fontFamily = FontFamily.Serif,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = LudoAccent
                        )
                        Text(
                            text = if (isSpeaking) "🎙️ Speaking..." else if (isMicOn) "🎙️ Mic On" else "🔇 Mic Off",
                            fontSize = 11.sp,
                            color = if (isSpeaking) Color(0xFF4CAF50) else LudoMuted
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Voice Mic Toggle
                        IconButton(
                            onClick = onToggleMic,
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(if (isMicOn) Color(0x334CAF50) else LudoPanelLight)
                                .border(1.dp, if (isMicOn) Color(0xFF4CAF50) else LudoAccentDim, CircleShape)
                                .testTag("mic_toggle_button")
                        ) {
                            Icon(
                                imageVector = if (isMicOn) Icons.Default.Mic else Icons.Default.MicOff,
                                contentDescription = "Toggle Mic",
                                tint = if (isMicOn) Color(0xFF4CAF50) else LudoMuted
                            )
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        IconButton(onClick = onDismiss, modifier = Modifier.testTag("close_chat_button")) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = LudoMuted)
                        }
                    }
                }

                // Push to Talk / Audio Wave Bar
                Card(
                    colors = CardDefaults.cardColors(containerColor = LudoPanel),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .border(1.dp, LudoAccentDim.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Pulsing wave bars
                            AudioWaveIndicator(isSpeaking = isSpeaking, level = audioLevel)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isSpeaking) "Voice active..." else "Push & hold to talk",
                                fontSize = 12.sp,
                                color = LudoText
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSpeaking) Color(0xFF4CAF50) else LudoAccent)
                                .pointerInput(Unit) {
                                    detectTapGestures(
                                        onPress = {
                                            onPushToTalk(true)
                                            tryAwaitRelease()
                                            onPushToTalk(false)
                                        }
                                    )
                                }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "HOLD TO TALK",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1C140C)
                            )
                        }
                    }
                }

                // Messages List
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(LudoPanel)
                        .padding(8.dp)
                ) {
                    if (messages.isEmpty()) {
                        Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                            Text("No messages yet. Send a quick chat!", color = LudoMuted, fontSize = 12.sp)
                        }
                    } else {
                        LazyColumn(
                            state = listState,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(messages) { msg ->
                                val isMe = msg.senderId == currentUserId
                                val bubbleColor = when (msg.senderColor.lowercase()) {
                                    "red" -> LudoRed.copy(alpha = 0.35f)
                                    "green" -> LudoGreen.copy(alpha = 0.35f)
                                    "yellow" -> LudoYellow.copy(alpha = 0.35f)
                                    "blue" -> LudoBlue.copy(alpha = 0.35f)
                                    else -> LudoPanelLight
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(bubbleColor)
                                            .border(1.dp, LudoAccentDim.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                            .padding(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        Column {
                                            Text(
                                                text = if (isMe) "You" else msg.senderName,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = LudoAccent
                                            )
                                            Text(
                                                text = msg.message,
                                                fontSize = 13.sp,
                                                color = LudoText
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Quick Chat Chips
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    quickChats.forEach { qc ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(LudoPanelLight)
                                .border(1.dp, LudoAccentDim.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                                .clickable { onSendMessage(qc) }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(text = qc, fontSize = 11.sp, color = LudoText)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Input Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    OutlinedTextField(
                        value = textInput,
                        onValueChange = { textInput = it },
                        placeholder = { Text("Type a message...", color = LudoMuted, fontSize = 12.sp) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = LudoAccent,
                            unfocusedBorderColor = LudoAccentDim,
                            focusedTextColor = LudoText,
                            unfocusedTextColor = LudoText
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("chat_input_field")
                    )

                    Button(
                        onClick = {
                            if (textInput.isNotBlank()) {
                                onSendMessage(textInput.trim())
                                textInput = ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = LudoAccent, contentColor = Color(0xFF1C140C)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("send_chat_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun AudioWaveIndicator(isSpeaking: Boolean, level: Float) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        val h1 = if (isSpeaking) (8 + level * 20).dp else 6.dp
        val h2 = if (isSpeaking) (12 + level * 26).dp else 8.dp
        val h3 = if (isSpeaking) (16 + level * 30).dp else 10.dp
        val h4 = if (isSpeaking) (10 + level * 22).dp else 7.dp

        val activeColor = if (isSpeaking) Color(0xFF4CAF50) else LudoMuted

        Box(modifier = Modifier.width(3.dp).height(h1).clip(RoundedCornerShape(2.dp)).background(activeColor))
        Box(modifier = Modifier.width(3.dp).height(h2).clip(RoundedCornerShape(2.dp)).background(activeColor))
        Box(modifier = Modifier.width(3.dp).height(h3).clip(RoundedCornerShape(2.dp)).background(activeColor))
        Box(modifier = Modifier.width(3.dp).height(h4).clip(RoundedCornerShape(2.dp)).background(activeColor))
    }
}
