package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.HelpCenter
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Psychology
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.LudoEngine
import com.example.ui.theme.LudoAccent
import com.example.ui.theme.LudoAccentDim
import com.example.ui.theme.LudoBg
import com.example.ui.theme.LudoMuted
import com.example.ui.theme.LudoPanel
import com.example.ui.theme.LudoPanelLight
import com.example.ui.theme.LudoText

data class HelperMessage(
    val sender: String, // "helper" or "user"
    val text: String,
    val time: String = "Just now"
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HelperAssistantDialog(
    currentTurn: String,
    dice: Long,
    rolled: Boolean,
    isMyTurn: Boolean,
    tokens: Map<String, List<Long>>,
    winner: String?,
    noAiMode: Boolean,
    playerLabels: Map<String, String>,
    onDismiss: () -> Unit
) {
    val myTokens = tokens[currentTurn] ?: listOf(0L, 0L, 0L, 0L)
    val movable = if (rolled && dice > 0L) LudoEngine.getMovableTokenIndices(myTokens, dice) else emptyList()

    // Generate smart live diagnosis based on exact board state
    val liveDiagnosis = remember(currentTurn, dice, rolled, isMyTurn, winner) {
        when {
            winner != null && winner.isNotBlank() -> {
                val winnerName = playerLabels[winner] ?: winner.uppercase()
                "🏆 Game khatam ho chuki hai! Khiladi '$winnerName' ne apne sabhi 4 tokens ghar pohncha kar jeet haasil kar li hai."
            }
            !isMyTurn -> {
                val currentName = playerLabels[currentTurn] ?: currentTurn.uppercase()
                "⏳ Abhi $currentName ($currentTurn) ki baari hai. Jaise hi unka move mukammal hoga ya time khatam hoga, baari aapke paas aayegi. Thora sabar karein!"
            }
            !rolled -> {
                "🎲 Yeh aapki baari hai! Dice abhi roll nahi hua. Dice slot par tap karein ya 'Roll' button dabayein taake aapka number niklay."
            }
            movable.isEmpty() -> {
                if (dice == 6L) {
                    "🎲 Aapka 6 aaya hai! Agar koi token nahi chal sakta to aapko foran doosra roll mil raha hai."
                } else {
                    val allInBase = myTokens.all { it == 0L }
                    if (allInBase) {
                        "🚪 Aapke sabhi tokens base me band hain. Token bahar nikalne ke liye 6 aana laazmi hai. Aapka dice $dice aaya hai, isliye move mumkin nahi tha aur baari aagay barh gayi."
                    } else {
                        "⛔ Aapka dice $dice aaya hai lekin kisi bhi token ke paas legal move nahi hai (ya home ke liye zyada number hai). Isliye baari aglay khiladi ko pass ho gayi."
                    }
                }
            }
            else -> {
                "✨ Aapka dice $dice aaya hai aur aapke ${movable.size} token chalne ke qabil hain! Board par chamaktay (highlight) token par tap karein."
            }
        }
    }

    val conversation = remember {
        mutableStateListOf(
            HelperMessage(
                sender = "helper",
                text = "Assalam-o-Alaikum! Main aapka Game Helper hoon. Ludo khelte waqt koi bhi masla bane ya samajh na aaye to mujhse poochein. Main foran waja aur hal bataunga!\n\n💡 Haal-e-Haazri:\n$liveDiagnosis"
            )
        )
    }

    var customInput by remember { mutableStateOf("") }

    val quickQuestions = listOf(
        "Dice roll kyu nahi ho raha?",
        "Token bahar kyu nahi aa raha?",
        "Turn achanak pass kyu ho gayi?",
        "Mera token capture/cut kyu hua?",
        "Safe zone (Stars) ka kya rule hai?",
        "2-Player (No AI) mode ke kya rules hain?",
        "Game me lag ya delay khatam karne ka tarika?"
    )

    fun answerQuestion(question: String) {
        val qLower = question.lowercase()
        val response = when {
            qLower.contains("dice") && (qLower.contains("roll") || qLower.contains("nahi") || qLower.contains("chal")) -> {
                if (!isMyTurn) {
                    val turnName = playerLabels[currentTurn] ?: currentTurn.uppercase()
                    "Dice is waqt isliye nahi ghoom raha kyu ke abhi $turnName ki baari hai. Jab aapka turn aayega to dice roshan (enable) ho jayega."
                } else if (rolled) {
                    "Aap is turn ka dice pehle hi roll kar chuke hain ($dice). Ab board par apne token ko tap kar ke chalayein."
                } else {
                    "Aapki hi turn hai! Dice slot par click karein. Agar lag mehsus ho to internet connection check karein, game engine foran update leta hai."
                }
            }
            qLower.contains("token") && (qLower.contains("bahar") || qLower.contains("ghar") || qLower.contains("open") || qLower.contains("nikal")) -> {
                "Ludo ke standard rules ke mutabiq token ko base se bahar nikalne ke liye dice par 6 aana zaroori hai. 1, 2, 3, 4 ya 5 aane par naya token bahar nahi nikal sakta."
            }
            qLower.contains("pass") || qLower.contains("baari") || qLower.contains("turn") -> {
                if (movable.isEmpty() && rolled && dice != 6L) {
                    "Aapka dice $dice aaya tha lekin koi bhi token aagay chalne ke qabil nahi tha, isliye rules ke tehat turn khud-bakhud aglay player ko transfer ho gayi taake waqt zaya na ho."
                } else {
                    "Turn sirf do suraton me pass hoti hai: (1) Dice roll ke baad koi legal move na ho, ya (2) Move mukammal ho chuki ho aur dice 6 na ho."
                }
            }
            qLower.contains("capture") || qLower.contains("cut") || qLower.contains("mara") || qLower.contains("kata") -> {
                "Jab opponent ka token theek usi khane (square) par rukta hai jahan aapka token pehle se mojood ho, to aapka token capture ho jata hai. Sirf Safe Star ⭐ squares par token safe rehte hain."
            }
            qLower.contains("safe") || qLower.contains("star") -> {
                "Board par bane Star ⭐ squares safe zones hain. Star par baithe kisi bhi token ko koi doosra khiladi capture nahi kar sakta."
            }
            qLower.contains("2 player") || qLower.contains("do bando") || qLower.contains("ai") || qLower.contains("assistant") -> {
                if (noAiMode) {
                    "Yeh match khaas '2-Player Pure PvP (Without AI)' hai. Isme koi computer bot ya auto-bot nahi khel sakta. Sirf do haqeeqi khiladi aamne saamne voice aur text chat ke sath khel rahe hain."
                } else {
                    "Aap Lobby se '2 Players (Without AI)' select kar sakte hain jisme koi AI assistant shamil nahi hoga."
                }
            }
            qLower.contains("lag") || qLower.contains("slow") || qLower.contains("speed") -> {
                "Humne local optimistic updates active ki hain jisse dice aur token foran 0ms par move karte hain. Khelte waqt bilkul bhi lag mehsus nahi hoga!"
            }
            else -> {
                "Aapke sawaal ka jawab: Ludo me turn by turn khiladi roll karte hain. Is waqt turn: $currentTurn, Last Dice: $dice. Agar koi aur pareshani ho to batayein!"
            }
        }

        conversation.add(HelperMessage(sender = "user", text = question))
        conversation.add(HelperMessage(sender = "helper", text = response))
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .heightIn(max = 620.dp)
                .clip(RoundedCornerShape(20.dp)),
            color = LudoBg
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(LudoAccent.copy(alpha = 0.2f))
                                .border(1.5.dp, LudoAccent, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Psychology, contentDescription = null, tint = LudoAccent, modifier = Modifier.size(22.dp))
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Helper (مددگار AI)",
                                color = LudoAccent,
                                fontFamily = FontFamily.Serif,
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp
                            )
                            Text(
                                text = "Rules & Game Troubleshooting",
                                color = LudoMuted,
                                fontSize = 11.sp
                            )
                        }
                    }

                    IconButton(onClick = onDismiss, modifier = Modifier.testTag("close_helper_button")) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = LudoMuted)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Chat Log
                val scrollState = rememberScrollState()
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(LudoPanel)
                        .padding(12.dp)
                        .verticalScroll(scrollState),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    conversation.forEach { msg ->
                        val isHelper = msg.sender == "helper"
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = if (isHelper) Arrangement.Start else Arrangement.End
                        ) {
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isHelper) LudoPanelLight else LudoAccent.copy(alpha = 0.25f)
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth(0.9f)
                                    .border(
                                        width = 1.dp,
                                        color = if (isHelper) LudoAccentDim.copy(alpha = 0.5f) else LudoAccent,
                                        shape = RoundedCornerShape(12.dp)
                                    )
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = if (isHelper) "🤖 Helper" else "👤 You",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isHelper) LudoAccent else Color.White
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = msg.text,
                                        fontSize = 13.sp,
                                        color = LudoText,
                                        lineHeight = 18.sp
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Quick Questions Chips
                Text(
                    text = "Aam Masail (Tap to Ask):",
                    fontSize = 11.sp,
                    color = LudoMuted,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(6.dp))
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    quickQuestions.take(4).forEach { q ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(LudoPanelLight)
                                .border(1.dp, LudoAccentDim, RoundedCornerShape(16.dp))
                                .clickable { answerQuestion(q) }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(text = q, fontSize = 11.sp, color = LudoAccent)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Custom Input Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = customInput,
                        onValueChange = { customInput = it },
                        placeholder = { Text("Koi bhi masla poochein...", fontSize = 12.sp, color = LudoMuted) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = LudoAccent,
                            unfocusedBorderColor = LudoAccentDim,
                            focusedTextColor = LudoText,
                            unfocusedTextColor = LudoText
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("helper_input_field")
                    )

                    Button(
                        onClick = {
                            if (customInput.isNotBlank()) {
                                answerQuestion(customInput.trim())
                                customInput = ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = LudoAccent, contentColor = Color(0xFF1C140C)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("helper_send_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}
