package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.theme.LudoAccent
import com.example.ui.theme.LudoAccentDim
import com.example.ui.theme.LudoBg
import com.example.ui.theme.LudoPanel
import com.example.ui.theme.LudoText

@Composable
fun GuideDialog(
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = LudoPanel),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.5.dp, LudoAccentDim, RoundedCornerShape(20.dp))
                .testTag("guide_dialog")
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "How to Play Ludo",
                    fontFamily = FontFamily.Serif,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = LudoAccent
                )

                Text(
                    text = "• Starting Out: Each player begins with 4 tokens resting in their colored corner yard. Roll the die — you need a 6 to bring a token out onto the track.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = LudoText
                )

                Text(
                    text = "• Moving: Tap any glowing, highlighted token on your turn to advance it clockwise around the outer ring by the exact number shown on the dice.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = LudoText
                )

                Text(
                    text = "• Capturing: Land directly on an opponent's token to knock it off the track and send it back to their yard! Safe squares (marked with gold stars) protect tokens from being captured.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = LudoText
                )

                Text(
                    text = "• Lucky Sixes: Rolling a 6 earns you an immediate bonus roll!",
                    style = MaterialTheme.typography.bodyLarge,
                    color = LudoText
                )

                Text(
                    text = "• The Crown: Guide all 4 tokens around the board and into the central HOME to claim royal victory!",
                    style = MaterialTheme.typography.bodyLarge,
                    color = LudoText
                )

                Spacer(modifier = Modifier.height(6.dp))

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = LudoAccent, contentColor = LudoBg),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("guide_close_button")
                ) {
                    Text("Got it", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
