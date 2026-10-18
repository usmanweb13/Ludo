package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val photoUrl: String? = null,
    val gamesPlayed: Long = 0L,
    val gamesWon: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class LudoPlayer(
    val id: String = "",
    val name: String = "",
    val color: String = "red",
    val isHost: Boolean = false,
    val isReady: Boolean = true,
    val isBot: Boolean = false
)

@IgnoreExtraProperties
data class LudoRoom(
    @DocumentId val id: String = "",
    val roomId: String = "",
    val name: String = "",
    val hostId: String = "",
    val status: String = "waiting", // "waiting", "playing", "finished"
    val isPublic: Boolean = true,
    val maxPlayers: Long = 4L,
    val playerIds: List<String> = emptyList(),
    val players: List<LudoPlayer> = emptyList(),
    val turn: String = "red",
    val dice: Long = 0L,
    val rolled: Boolean = false,
    val winner: String? = null,
    val tokens: Map<String, List<Long>> = emptyMap(),
    val activeColors: List<String> = emptyList(),
    val noAiMode: Boolean = false,
    val recentChat: Map<String, String>? = null,
    val voiceState: Map<String, Boolean>? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class ChatMessage(
    @DocumentId val id: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val senderColor: String = "red",
    val message: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

object LudoEngine {
    val COLORS = listOf("red", "blue", "yellow", "green")
    val START = mapOf("red" to 0, "blue" to 13, "yellow" to 26, "green" to 39)
    val SAFE = listOf(0, 8, 13, 21, 26, 34, 39, 47)

    fun absRing(color: String, steps: Long): Int {
        val start = START[color] ?: 0
        return ((start + steps - 1) % 52).toInt()
    }

    fun isMovable(steps: Long, dice: Long): Boolean {
        if (steps == 0L && dice == 6L) return true
        if (steps > 0L && steps < 58L && steps + dice <= 58L) return true
        return false
    }

    fun getMovableTokenIndices(tokens: List<Long>, dice: Long): List<Int> {
        val indices = mutableListOf<Int>()
        tokens.forEachIndexed { index, steps ->
            if (isMovable(steps, dice)) {
                indices.add(index)
            }
        }
        return indices
    }

    fun createFreshTokens(colors: List<String>): Map<String, List<Long>> {
        return colors.associateWith { listOf(0L, 0L, 0L, 0L) }
    }
}
