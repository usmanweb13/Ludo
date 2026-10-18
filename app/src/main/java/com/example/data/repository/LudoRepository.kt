package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.ChatMessage
import com.example.data.model.LudoEngine
import com.example.data.model.LudoPlayer
import com.example.data.model.LudoRoom
import com.example.data.model.UserProfile
import com.example.data.util.OperationType
import com.example.data.util.handleFirestoreError
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlin.random.Random

class LudoRepository(private val db: FirebaseFirestore) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val auth = FirebaseAuth.getInstance()

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    suspend fun getOrCreateUserProfile(userId: String, displayName: String): Result<UserProfile> {
        val path = "users/$userId"
        val docRef = db.collection("users").document(userId)
        return try {
            val snapshot = docRef.get().await()
            if (snapshot.exists()) {
                val profile = snapshot.toObject(UserProfile::class.java)
                Result.success(profile ?: UserProfile(id = userId, userId = userId, displayName = displayName))
            } else {
                val newProfile = UserProfile(
                    id = userId,
                    userId = userId,
                    displayName = displayName.ifBlank { "Player_${userId.take(5)}" },
                    gamesPlayed = 0L,
                    gamesWon = 0L
                )
                val payload = mapOf(
                    "userId" to userId,
                    "displayName" to newProfile.displayName,
                    "gamesPlayed" to 0L,
                    "gamesWon" to 0L,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(payload).await()
                Result.success(newProfile)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    fun observeUserProfile(userId: String): Flow<UserProfile?> {
        val path = "users/$userId"
        return db.collection("users").document(userId)
            .snapshots()
            .map { snap -> snap.toObject(UserProfile::class.java) }
            .catch { e ->
                if (e is Exception) handleFirestoreError(e, OperationType.GET, path)
                throw e
            }
    }

    fun observeRoom(roomId: String): Flow<LudoRoom?> {
        val path = "rooms/$roomId"
        return db.collection("rooms").document(roomId)
            .snapshots()
            .map { snap ->
                if (snap.exists()) snap.toObject(LudoRoom::class.java) else null
            }
            .catch { e ->
                if (e is Exception) handleFirestoreError(e, OperationType.GET, path)
                throw e
            }
    }

    fun observePublicRooms(): Flow<List<LudoRoom>> {
        val path = "rooms"
        return db.collection("rooms")
            .whereEqualTo("isPublic", true)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(LudoRoom::class.java)
                    .filter { it.status == "waiting" && it.players.size < it.maxPlayers }
            }
            .catch { e ->
                if (e is Exception) handleFirestoreError(e, OperationType.LIST, path)
                throw e
            }
    }

    suspend fun createRoom(
        roomCode: String,
        name: String,
        isPublic: Boolean,
        maxPlayers: Int = 4,
        userId: String,
        displayName: String,
        noAiMode: Boolean = false
    ): Result<String> {
        val path = "rooms/$roomCode"
        val docRef = db.collection("rooms").document(roomCode)
        return try {
            val initialPlayer = LudoPlayer(
                id = userId,
                name = displayName.ifBlank { "Host" },
                color = "red",
                isHost = true,
                isReady = true
            )
            val tokens = mapOf("red" to listOf(0L, 0L, 0L, 0L))
            val payload = mapOf(
                "roomId" to roomCode,
                "name" to name.ifBlank { "Room $roomCode" },
                "hostId" to userId,
                "status" to "waiting",
                "isPublic" to isPublic,
                "maxPlayers" to maxPlayers.toLong(),
                "noAiMode" to noAiMode,
                "playerIds" to listOf(userId),
                "players" to listOf(
                    mapOf(
                        "id" to initialPlayer.id,
                        "name" to initialPlayer.name,
                        "color" to initialPlayer.color,
                        "isHost" to true,
                        "isReady" to true
                    )
                ),
                "turn" to "red",
                "dice" to 0L,
                "rolled" to false,
                "winner" to "",
                "tokens" to tokens,
                "activeColors" to listOf("red"),
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            Result.success(roomCode)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun joinRoom(roomCode: String, userId: String, displayName: String): Result<String> {
        val path = "rooms/$roomCode"
        val docRef = db.collection("rooms").document(roomCode)
        return try {
            val snap = docRef.get().await()
            if (!snap.exists()) {
                return Result.failure(IllegalArgumentException("Room $roomCode not found."))
            }
            val room = snap.toObject(LudoRoom::class.java)
                ?: return Result.failure(IllegalStateException("Invalid room data."))

            if (room.playerIds.contains(userId)) {
                return Result.success(roomCode)
            }
            if (room.status != "waiting") {
                return Result.failure(IllegalStateException("Game has already started."))
            }
            if (room.players.size >= room.maxPlayers) {
                return Result.failure(IllegalStateException("Room is already full."))
            }

            val usedColors = room.players.map { it.color }
            val nextColor = LudoEngine.COLORS.firstOrNull { !usedColors.contains(it) } ?: "blue"

            val updatedPlayerIds = room.playerIds + userId
            val updatedPlayers = room.players + LudoPlayer(
                id = userId,
                name = displayName.ifBlank { "Player ${room.players.size + 1}" },
                color = nextColor,
                isHost = false,
                isReady = true
            )
            val updatedActiveColors = (room.activeColors + nextColor).distinct()
            val updatedTokens = room.tokens.toMutableMap().apply {
                this[nextColor] = listOf(0L, 0L, 0L, 0L)
            }

            docRef.update(
                mapOf(
                    "playerIds" to updatedPlayerIds,
                    "players" to updatedPlayers.map {
                        mapOf(
                            "id" to it.id,
                            "name" to it.name,
                            "color" to it.color,
                            "isHost" to it.isHost,
                            "isReady" to it.isReady
                        )
                    },
                    "activeColors" to updatedActiveColors,
                    "tokens" to updatedTokens,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(roomCode)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun findQuickMatch(userId: String, displayName: String, maxPlayers: Int = 4, noAiMode: Boolean = false): Result<String> {
        return try {
            val querySnapshot = db.collection("rooms")
                .whereEqualTo("isPublic", true)
                .get()
                .await()

            val openRoom = querySnapshot.documents.mapNotNull { doc ->
                doc.toObject(LudoRoom::class.java)
            }.firstOrNull { room ->
                room.status == "waiting" &&
                room.players.size < room.maxPlayers &&
                !room.playerIds.contains(userId) &&
                room.maxPlayers == maxPlayers.toLong() &&
                room.noAiMode == noAiMode
            }

            if (openRoom != null) {
                joinRoom(openRoom.roomId, userId, displayName)
            } else {
                val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ"
                val newCode = (1..4).map { chars.random() }.joinToString("")
                val roomTitle = if (noAiMode) "2P Match (No AI)" else "Quick Match"
                createRoom(
                    roomCode = newCode,
                    name = roomTitle,
                    isPublic = true,
                    maxPlayers = maxPlayers,
                    userId = userId,
                    displayName = displayName,
                    noAiMode = noAiMode
                )
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, "rooms")
            Result.failure(e)
        }
    }

    suspend fun addBotPlayer(roomId: String, hostId: String): Result<Unit> {
        val path = "rooms/$roomId"
        val docRef = db.collection("rooms").document(roomId)
        return try {
            val snap = docRef.get().await()
            val room = snap.toObject(LudoRoom::class.java)
                ?: return Result.failure(IllegalStateException("Room not found."))

            if (room.hostId != hostId) {
                return Result.failure(IllegalStateException("Only the host can add bots."))
            }
            if (room.noAiMode) {
                return Result.failure(IllegalStateException("AI Bots cannot be added in 2-Player (No AI) match."))
            }
            if (room.status != "waiting") {
                return Result.failure(IllegalStateException("Game already started."))
            }
            if (room.players.size >= room.maxPlayers) {
                return Result.failure(IllegalStateException("Room is full."))
            }

            val usedColors = room.players.map { it.color }
            val nextColor = LudoEngine.COLORS.firstOrNull { !usedColors.contains(it) }
                ?: return Result.failure(IllegalStateException("No available colors."))

            val botId = "bot_${nextColor}"
            val botName = "Bot (${nextColor.replaceFirstChar { it.uppercase() }})"
            val updatedPlayerIds = room.playerIds + botId
            val updatedPlayers = room.players + LudoPlayer(
                id = botId,
                name = botName,
                color = nextColor,
                isHost = false,
                isReady = true,
                isBot = true
            )
            val updatedActiveColors = (room.activeColors + nextColor).distinct()
            val updatedTokens = room.tokens.toMutableMap().apply {
                this[nextColor] = listOf(0L, 0L, 0L, 0L)
            }

            docRef.update(
                mapOf(
                    "playerIds" to updatedPlayerIds,
                    "players" to updatedPlayers.map {
                        mapOf(
                            "id" to it.id,
                            "name" to it.name,
                            "color" to it.color,
                            "isHost" to it.isHost,
                            "isReady" to it.isReady,
                            "isBot" to it.isBot
                        )
                    },
                    "activeColors" to updatedActiveColors,
                    "tokens" to updatedTokens,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun removePlayer(roomId: String, playerId: String, hostId: String): Result<Unit> {
        val path = "rooms/$roomId"
        val docRef = db.collection("rooms").document(roomId)
        return try {
            val snap = docRef.get().await()
            val room = snap.toObject(LudoRoom::class.java)
                ?: return Result.failure(IllegalStateException("Room not found."))

            if (room.hostId != hostId && playerId != hostId) {
                return Result.failure(IllegalStateException("Not authorized to remove player."))
            }
            if (room.status != "waiting") {
                return Result.failure(IllegalStateException("Game already started."))
            }

            val updatedPlayers = room.players.filterNot { it.id == playerId }
            val updatedPlayerIds = room.playerIds.filterNot { it == playerId }
            val removedPlayer = room.players.find { it.id == playerId }
            val updatedActiveColors = if (removedPlayer != null) {
                room.activeColors.filterNot { it == removedPlayer.color }
            } else room.activeColors
            val updatedTokens = room.tokens.toMutableMap().apply {
                if (removedPlayer != null) remove(removedPlayer.color)
            }

            docRef.update(
                mapOf(
                    "playerIds" to updatedPlayerIds,
                    "players" to updatedPlayers.map {
                        mapOf(
                            "id" to it.id,
                            "name" to it.name,
                            "color" to it.color,
                            "isHost" to it.isHost,
                            "isReady" to it.isReady,
                            "isBot" to it.isBot
                        )
                    },
                    "activeColors" to updatedActiveColors,
                    "tokens" to updatedTokens,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun startGame(roomId: String, userId: String, fillWithBots: Boolean = false): Result<Unit> {
        val path = "rooms/$roomId"
        val docRef = db.collection("rooms").document(roomId)
        return try {
            val snap = docRef.get().await()
            val room = snap.toObject(LudoRoom::class.java)
                ?: return Result.failure(IllegalStateException("Room not found."))

            if (room.hostId != userId) {
                return Result.failure(IllegalStateException("Only room host can start the game."))
            }

            var currentPlayers = room.players
            var currentPlayerIds = room.playerIds

            if (!room.noAiMode && fillWithBots && currentPlayers.size < 2) {
                val needed = (2 - currentPlayers.size).coerceAtLeast(1)
                val usedColors = currentPlayers.map { it.color }.toMutableSet()
                val addedPlayers = mutableListOf<LudoPlayer>()
                val addedIds = mutableListOf<String>()

                for (i in 0 until needed) {
                    val nextColor = LudoEngine.COLORS.firstOrNull { !usedColors.contains(it) } ?: break
                    usedColors.add(nextColor)
                    val botId = "bot_${nextColor}"
                    val botName = "Bot (${nextColor.replaceFirstChar { it.uppercase() }})"
                    addedPlayers.add(LudoPlayer(id = botId, name = botName, color = nextColor, isHost = false, isReady = true, isBot = true))
                    addedIds.add(botId)
                }
                currentPlayers = currentPlayers + addedPlayers
                currentPlayerIds = currentPlayerIds + addedIds
            }

            if (currentPlayers.size < 2) {
                val errorMsg = if (room.noAiMode) {
                    "Game requires 2 human players. Waiting for the second player to join!"
                } else {
                    "At least 2 players are needed to start. Tap 'Add AI Bot' or invite friends!"
                }
                return Result.failure(IllegalStateException(errorMsg))
            }

            val colors = currentPlayers.map { it.color }
            val initialTokens = colors.associateWith { listOf(0L, 0L, 0L, 0L) }

            docRef.update(
                mapOf(
                    "status" to "playing",
                    "turn" to colors.first(),
                    "dice" to 0L,
                    "rolled" to false,
                    "tokens" to initialTokens,
                    "activeColors" to colors,
                    "players" to currentPlayers.map {
                        mapOf(
                            "id" to it.id,
                            "name" to it.name,
                            "color" to it.color,
                            "isHost" to it.isHost,
                            "isReady" to it.isReady,
                            "isBot" to it.isBot
                        )
                    },
                    "playerIds" to currentPlayerIds,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun rollDice(roomId: String, color: String, userId: String): Result<Int> {
        val path = "rooms/$roomId"
        val docRef = db.collection("rooms").document(roomId)
        return try {
            val snap = docRef.get().await()
            val room = snap.toObject(LudoRoom::class.java)
                ?: return Result.failure(IllegalStateException("Room not found."))

            if (room.status != "playing") {
                return Result.failure(IllegalStateException("Game is not in playing state."))
            }
            if (room.turn != color) {
                return Result.failure(IllegalStateException("It is not $color's turn."))
            }
            if (room.rolled) {
                return Result.failure(IllegalStateException("Dice already rolled for this turn."))
            }

            val player = room.players.find { it.color == color }
            val isHost = room.hostId == userId
            val isBot = player?.isBot == true || player?.id?.startsWith("bot_") == true
            if (!isHost && player?.id != userId && !isBot) {
                return Result.failure(IllegalStateException("You are not player $color."))
            }

            val diceValue = Random.nextInt(1, 7)
            docRef.update(
                mapOf(
                    "dice" to diceValue.toLong(),
                    "rolled" to true,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            Result.success(diceValue)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun passTurn(roomId: String, color: String, userId: String): Result<Unit> {
        val path = "rooms/$roomId"
        val docRef = db.collection("rooms").document(roomId)
        return try {
            val snap = docRef.get().await()
            val room = snap.toObject(LudoRoom::class.java)
                ?: return Result.failure(IllegalStateException("Room not found."))

            if (room.turn != color) {
                return Result.failure(IllegalStateException("It is not $color's turn."))
            }

            val nextColorIndex = (room.activeColors.indexOf(room.turn) + 1) % room.activeColors.size
            val nextTurn = room.activeColors[nextColorIndex]

            docRef.update(
                mapOf(
                    "turn" to nextTurn,
                    "dice" to 0L,
                    "rolled" to false,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun repeatTurnForSix(roomId: String, color: String, userId: String): Result<Unit> {
        val path = "rooms/$roomId"
        val docRef = db.collection("rooms").document(roomId)
        return try {
            val snap = docRef.get().await()
            val room = snap.toObject(LudoRoom::class.java)
                ?: return Result.failure(IllegalStateException("Room not found."))

            if (room.turn != color) {
                return Result.failure(IllegalStateException("It is not $color's turn."))
            }

            docRef.update(
                mapOf(
                    "dice" to 0L,
                    "rolled" to false,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun moveToken(roomId: String, color: String, tokenIndex: Int, userId: String): Result<Unit> {
        val path = "rooms/$roomId"
        val docRef = db.collection("rooms").document(roomId)
        return try {
            val snap = docRef.get().await()
            val room = snap.toObject(LudoRoom::class.java)
                ?: return Result.failure(IllegalStateException("Room not found."))

            if (room.turn != color) {
                return Result.failure(IllegalStateException("Not $color's turn."))
            }
            if (!room.rolled || room.dice <= 0L) {
                return Result.failure(IllegalStateException("Must roll dice first."))
            }

            val dice = room.dice
            val currentTokens = (room.tokens[color] ?: listOf(0L, 0L, 0L, 0L)).toMutableList()
            var steps = currentTokens[tokenIndex]

            steps = if (steps == 0L) 1L else steps + dice
            if (steps > 58L) {
                return Result.failure(IllegalStateException("Cannot overshoot home."))
            }
            currentTokens[tokenIndex] = steps

            val updatedTokens = room.tokens.mapValues { it.value.toMutableList() }.toMutableMap()
            updatedTokens[color] = currentTokens

            // Check if landed on opponent
            if (steps in 1L..51L) {
                val ring = LudoEngine.absRing(color, steps)
                if (!LudoEngine.SAFE.contains(ring)) {
                    room.activeColors.forEach { otherColor ->
                        if (otherColor != color) {
                            val opponentTokens = (updatedTokens[otherColor] ?: listOf(0L, 0L, 0L, 0L)).toMutableList()
                            var captured = false
                            opponentTokens.forEachIndexed { idx, oppSteps ->
                                if (oppSteps in 1L..51L && LudoEngine.absRing(otherColor, oppSteps) == ring) {
                                    opponentTokens[idx] = 0L
                                    captured = true
                                }
                            }
                            if (captured) {
                                updatedTokens[otherColor] = opponentTokens
                            }
                        }
                    }
                }
            }

            val won = currentTokens.all { it == 58L }
            val nextTurn = if (won || dice == 6L) {
                color
            } else {
                val nextIndex = (room.activeColors.indexOf(room.turn) + 1) % room.activeColors.size
                room.activeColors[nextIndex]
            }

            val status = if (won) "finished" else "playing"
            val winner = if (won) color else ""

            docRef.update(
                mapOf(
                    "tokens" to updatedTokens,
                    "turn" to nextTurn,
                    "dice" to 0L,
                    "rolled" to false,
                    "status" to status,
                    "winner" to winner,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            // Update stats if won
            if (won) {
                room.players.forEach { p ->
                    try {
                        val userDoc = db.collection("users").document(p.id)
                        val isWinner = p.color == color
                        userDoc.update(
                            mapOf(
                                "gamesPlayed" to FieldValue.increment(1L),
                                "gamesWon" to if (isWinner) FieldValue.increment(1L) else FieldValue.increment(0L),
                                "updatedAt" to FieldValue.serverTimestamp()
                            )
                        ).await()
                    } catch (_: Exception) {}
                }
            }

            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun sendChatMessage(roomId: String, message: ChatMessage): Result<Unit> {
        val path = "rooms/$roomId/messages"
        return try {
            val msgDoc = db.collection("rooms").document(roomId).collection("messages").document()
            val msgData = mapOf(
                "id" to msgDoc.id,
                "senderId" to message.senderId,
                "senderName" to message.senderName,
                "senderColor" to message.senderColor,
                "message" to message.message,
                "timestamp" to System.currentTimeMillis()
            )
            msgDoc.set(msgData).await()

            // Also update recentChat map on room document for instant headline sync
            val chatMap = mapOf(
                "senderId" to message.senderId,
                "senderName" to message.senderName,
                "senderColor" to message.senderColor,
                "message" to message.message,
                "time" to System.currentTimeMillis().toString()
            )
            db.collection("rooms").document(roomId).update(
                mapOf(
                    "recentChat" to chatMap,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    fun observeMessages(roomId: String): Flow<List<ChatMessage>> {
        val path = "rooms/$roomId/messages"
        return db.collection("rooms").document(roomId).collection("messages")
            .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.ASCENDING)
            .limitToLast(50)
            .snapshots()
            .map { snap ->
                snap.documents.mapNotNull { doc ->
                    doc.toObject(ChatMessage::class.java)?.copy(id = doc.id)
                }
            }
            .catch { e ->
                if (e is Exception) handleFirestoreError(e, OperationType.LIST, path)
                throw e
            }
    }

    suspend fun updateVoiceState(roomId: String, userId: String, isTalking: Boolean): Result<Unit> {
        val path = "rooms/$roomId"
        return try {
            db.collection("rooms").document(roomId).update(
                mapOf(
                    "voiceState.$userId" to isTalking,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            Result.success(Unit) // Non-fatal voice status
        }
    }
}
