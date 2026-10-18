package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.ChatMessage
import com.example.data.model.LudoEngine
import com.example.data.model.LudoRoom
import com.example.data.model.UserProfile
import com.example.data.repository.LudoRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.random.Random

enum class ScreenState {
    LOBBY,
    ROOM_WAITING,
    ONLINE_GAME,
    LOCAL_GAME
}

data class LocalGameState(
    val tokens: Map<String, List<Long>> = mapOf(
        "red" to listOf(0L, 0L, 0L, 0L),
        "blue" to listOf(0L, 0L, 0L, 0L),
        "yellow" to listOf(0L, 0L, 0L, 0L),
        "green" to listOf(0L, 0L, 0L, 0L)
    ),
    val activeColors: List<String> = listOf("red", "blue", "yellow", "green"),
    val turn: String = "red",
    val dice: Long = 0L,
    val rolled: Boolean = false,
    val winner: String? = null,
    val isRolling: Boolean = false
)

class LudoViewModel(
    private val repository: LudoRepository,
    val currentUserId: String,
    val initialDisplayName: String
) : ViewModel() {

    private val _screen = MutableStateFlow(ScreenState.LOBBY)
    val screen: StateFlow<ScreenState> = _screen.asStateFlow()

    private val _isMatchmaking = MutableStateFlow(false)
    val isMatchmaking: StateFlow<Boolean> = _isMatchmaking.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _currentRoom = MutableStateFlow<LudoRoom?>(null)
    val currentRoom: StateFlow<LudoRoom?> = _currentRoom.asStateFlow()

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _isRollingOnline = MutableStateFlow(false)
    val isRollingOnline: StateFlow<Boolean> = _isRollingOnline.asStateFlow()

    private var roomObservationJob: Job? = null
    private var autoTurnJob: Job? = null
    private var messagesObservationJob: Job? = null

    val userProfile: StateFlow<UserProfile?> = repository.observeUserProfile(currentUserId)
        .catch { emit(null) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = null
        )

    val publicRooms: StateFlow<List<LudoRoom>> = repository.observePublicRooms()
        .catch { emit(emptyList()) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyList()
        )

    // Local Pass & Play State
    private val _localGame = MutableStateFlow(LocalGameState())
    val localGame: StateFlow<LocalGameState> = _localGame.asStateFlow()

    init {
        // Ensure user profile exists
        viewModelScope.launch {
            repository.getOrCreateUserProfile(currentUserId, initialDisplayName)
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun startLocalGame(playerCount: Int = 4) {
        val colors = LudoEngine.COLORS.take(playerCount.coerceIn(2, 4))
        val initialTokens = colors.associateWith { listOf(0L, 0L, 0L, 0L) }
        _localGame.value = LocalGameState(
            tokens = initialTokens,
            activeColors = colors,
            turn = colors.first(),
            dice = 0L,
            rolled = false,
            winner = null
        )
        _screen.value = ScreenState.LOCAL_GAME
    }

    fun rollDiceLocal() {
        val state = _localGame.value
        if (state.winner != null || state.rolled || state.isRolling) return

        viewModelScope.launch {
            _localGame.value = _localGame.value.copy(isRolling = true)
            delay(350)
            val roll = Random.nextLong(1, 7)
            val tokens = state.tokens[state.turn] ?: listOf(0L, 0L, 0L, 0L)
            val movable = LudoEngine.getMovableTokenIndices(tokens, roll)

            if (movable.isEmpty()) {
                if (roll == 6L) {
                    _localGame.value = state.copy(
                        dice = roll,
                        rolled = false,
                        isRolling = false
                    )
                } else {
                    val nextIndex = (state.activeColors.indexOf(state.turn) + 1) % state.activeColors.size
                    _localGame.value = state.copy(
                        dice = roll,
                        rolled = true,
                        isRolling = false
                    )
                    delay(700)
                    _localGame.value = _localGame.value.copy(
                        turn = state.activeColors[nextIndex],
                        dice = 0L,
                        rolled = false
                    )
                }
            } else {
                _localGame.value = state.copy(
                    dice = roll,
                    rolled = true,
                    isRolling = false
                )
            }
        }
    }

    fun moveTokenLocal(color: String, index: Int) {
        val state = _localGame.value
        if (state.turn != color || !state.rolled || state.dice <= 0L || state.winner != null) return

        val dice = state.dice
        val currentTokens = (state.tokens[color] ?: listOf(0L, 0L, 0L, 0L)).toMutableList()
        var steps = currentTokens[index]

        steps = if (steps == 0L) 1L else steps + dice
        if (steps > 58L) return
        currentTokens[index] = steps

        val updatedTokens = state.tokens.mapValues { it.value.toMutableList() }.toMutableMap()
        updatedTokens[color] = currentTokens

        // Handle capture
        if (steps in 1L..51L) {
            val ring = LudoEngine.absRing(color, steps)
            if (!LudoEngine.SAFE.contains(ring)) {
                state.activeColors.forEach { other ->
                    if (other != color) {
                        val opponentTokens = (updatedTokens[other] ?: listOf(0L, 0L, 0L, 0L)).toMutableList()
                        opponentTokens.forEachIndexed { i, s ->
                            if (s in 1L..51L && LudoEngine.absRing(other, s) == ring) {
                                opponentTokens[i] = 0L
                            }
                        }
                        updatedTokens[other] = opponentTokens
                    }
                }
            }
        }

        val won = currentTokens.all { it == 58L }
        val nextTurn = if (won || dice == 6L) {
            color
        } else {
            val nextIndex = (state.activeColors.indexOf(state.turn) + 1) % state.activeColors.size
            state.activeColors[nextIndex]
        }

        _localGame.value = state.copy(
            tokens = updatedTokens,
            turn = nextTurn,
            dice = 0L,
            rolled = false,
            winner = if (won) color else null
        )
    }

    fun findQuickMatch(noAiMode: Boolean = false, maxPlayers: Int = 4) {
        _isMatchmaking.value = true
        _errorMessage.value = null
        viewModelScope.launch {
            val name = userProfile.value?.displayName ?: initialDisplayName
            val result = repository.findQuickMatch(currentUserId, name, maxPlayers, noAiMode)
            _isMatchmaking.value = false
            if (result.isSuccess) {
                val roomCode = result.getOrThrow()
                observeRoomAndTransition(roomCode)
            } else {
                _errorMessage.value = result.exceptionOrNull()?.localizedMessage ?: "Failed to find match"
            }
        }
    }

    fun createPrivateRoom(roomCode: String, maxPlayers: Int = 4, noAiMode: Boolean = false) {
        val code = roomCode.trim().uppercase()
        if (code.length < 3) {
            _errorMessage.value = "Room code must be at least 3 characters."
            return
        }
        _isMatchmaking.value = true
        _errorMessage.value = null
        viewModelScope.launch {
            val name = userProfile.value?.displayName ?: initialDisplayName
            val roomName = if (noAiMode) "2P Match ($code)" else "Room $code"
            val result = repository.createRoom(
                roomCode = code,
                name = roomName,
                isPublic = false,
                maxPlayers = maxPlayers,
                userId = currentUserId,
                displayName = name,
                noAiMode = noAiMode
            )
            _isMatchmaking.value = false
            if (result.isSuccess) {
                observeRoomAndTransition(code)
            } else {
                _errorMessage.value = result.exceptionOrNull()?.localizedMessage ?: "Failed to create room"
            }
        }
    }

    fun joinRoomByCode(code: String) {
        val trimmed = code.trim().uppercase()
        if (trimmed.isEmpty()) {
            _errorMessage.value = "Enter a valid room code."
            return
        }
        _isMatchmaking.value = true
        _errorMessage.value = null
        viewModelScope.launch {
            val name = userProfile.value?.displayName ?: initialDisplayName
            val result = repository.joinRoom(trimmed, currentUserId, name)
            _isMatchmaking.value = false
            if (result.isSuccess) {
                observeRoomAndTransition(trimmed)
            } else {
                _errorMessage.value = result.exceptionOrNull()?.localizedMessage ?: "Room not found or cannot join"
            }
        }
    }

    private fun observeRoomAndTransition(roomId: String) {
        roomObservationJob?.cancel()
        autoTurnJob?.cancel()
        messagesObservationJob?.cancel()
        messagesObservationJob = viewModelScope.launch {
            repository.observeMessages(roomId).collect { list ->
                _messages.value = list
            }
        }
        roomObservationJob = viewModelScope.launch {
            repository.observeRoom(roomId).collect { room ->
                _currentRoom.value = room
                if (room != null) {
                    if (room.status == "playing" || room.status == "finished") {
                        _screen.value = ScreenState.ONLINE_GAME
                        handleOnlineTurnProgression(room)
                    } else if (room.status == "waiting") {
                        autoTurnJob?.cancel()
                        _screen.value = ScreenState.ROOM_WAITING
                    }
                } else if (_screen.value != ScreenState.LOBBY && _screen.value != ScreenState.LOCAL_GAME) {
                    autoTurnJob?.cancel()
                    _screen.value = ScreenState.LOBBY
                }
            }
        }
    }

    private fun handleOnlineTurnProgression(room: LudoRoom) {
        if (room.status != "playing" || room.winner != null && room.winner.isNotBlank()) {
            autoTurnJob?.cancel()
            return
        }

        val activeColor = room.turn
        val activePlayer = room.players.find { it.color == activeColor }
        val isBot = activePlayer?.isBot == true || activePlayer?.id?.startsWith("bot_") == true
        val isHost = room.hostId == currentUserId
        val isMyTurn = activePlayer?.id == currentUserId

        if (isBot && isHost) {
            // Host handles bot turns
            autoTurnJob?.cancel()
            autoTurnJob = viewModelScope.launch {
                if (!room.rolled) {
                    delay(800L)
                    _isRollingOnline.value = true
                    delay(350L)
                    _isRollingOnline.value = false
                    repository.rollDice(room.roomId, activeColor, currentUserId)
                } else {
                    val tokens = room.tokens[activeColor] ?: listOf(0L, 0L, 0L, 0L)
                    val movable = LudoEngine.getMovableTokenIndices(tokens, room.dice)
                    if (movable.isEmpty()) {
                        delay(1200L)
                        if (room.dice == 6L) {
                            repository.repeatTurnForSix(room.roomId, activeColor, currentUserId)
                        } else {
                            repository.passTurn(room.roomId, activeColor, currentUserId)
                        }
                    } else {
                        delay(900L)
                        val bestIndex = pickBestBotMove(tokens, movable, room.dice)
                        repository.moveToken(room.roomId, activeColor, bestIndex, currentUserId)
                    }
                }
            }
        } else if (isMyTurn && room.rolled) {
            // Human player rolled - check if no moves are possible
            val tokens = room.tokens[activeColor] ?: listOf(0L, 0L, 0L, 0L)
            val movable = LudoEngine.getMovableTokenIndices(tokens, room.dice)
            if (movable.isEmpty()) {
                autoTurnJob?.cancel()
                autoTurnJob = viewModelScope.launch {
                    delay(1200L)
                    if (room.dice == 6L) {
                        repository.repeatTurnForSix(room.roomId, activeColor, currentUserId)
                    } else {
                        repository.passTurn(room.roomId, activeColor, currentUserId)
                    }
                }
            }
        } else {
            // Not host handling bot, nor my turn with auto-pass
            if (!isBot && !isMyTurn) {
                autoTurnJob?.cancel()
            }
        }
    }

    private fun pickBestBotMove(tokens: List<Long>, movable: List<Int>, dice: Long): Int {
        // 1. Can we reach home? (steps + dice == 58)
        movable.find { tokens[it] + dice == 58L }?.let { return it }
        // 2. Can we leave home? (steps == 0 && dice == 6)
        if (dice == 6L) {
            movable.find { tokens[it] == 0L }?.let { return it }
        }
        // 3. Move token furthest along the board
        return movable.maxByOrNull { tokens[it] } ?: movable.first()
    }

    fun addBotPlayer() {
        val room = _currentRoom.value ?: return
        viewModelScope.launch {
            val result = repository.addBotPlayer(room.roomId, currentUserId)
            if (result.isFailure) {
                _errorMessage.value = result.exceptionOrNull()?.localizedMessage
            }
        }
    }

    fun removePlayer(playerId: String) {
        val room = _currentRoom.value ?: return
        viewModelScope.launch {
            val result = repository.removePlayer(room.roomId, playerId, currentUserId)
            if (result.isFailure) {
                _errorMessage.value = result.exceptionOrNull()?.localizedMessage
            }
        }
    }

    fun startGame(fillWithBots: Boolean = false) {
        val room = _currentRoom.value ?: return
        viewModelScope.launch {
            val result = repository.startGame(room.roomId, currentUserId, fillWithBots)
            if (result.isFailure) {
                _errorMessage.value = result.exceptionOrNull()?.localizedMessage ?: "Failed to start game"
            }
        }
    }

    fun rollDiceOnline() {
        val room = _currentRoom.value ?: return
        val player = room.players.find { it.id == currentUserId } ?: return
        if (room.turn != player.color || room.rolled || _isRollingOnline.value) return

        viewModelScope.launch {
            _isRollingOnline.value = true
            _errorMessage.value = null
            delay(350L) // Rolling animation duration
            val result = repository.rollDice(room.roomId, player.color, currentUserId)
            _isRollingOnline.value = false
            if (result.isFailure) {
                _errorMessage.value = result.exceptionOrNull()?.localizedMessage
            }
        }
    }

    fun moveTokenOnline(color: String, index: Int) {
        val room = _currentRoom.value ?: return
        val currentTokens = room.tokens[color] ?: return
        val dice = room.dice
        if (!LudoEngine.isMovable(currentTokens[index], dice)) return

        // 0ms Zero-Lag Optimistic local update
        val updatedTokens = room.tokens.toMutableMap()
        val mutableList = currentTokens.toMutableList()
        val newPos = if (mutableList[index] == 0L && dice == 6L) 1L else (mutableList[index] + dice).coerceAtMost(58L)
        mutableList[index] = newPos
        updatedTokens[color] = mutableList
        _currentRoom.value = room.copy(tokens = updatedTokens)

        viewModelScope.launch {
            val result = repository.moveToken(room.roomId, color, index, currentUserId)
            if (result.isFailure) {
                _errorMessage.value = result.exceptionOrNull()?.localizedMessage
            }
        }
    }

    fun sendChatMessage(text: String) {
        val room = _currentRoom.value ?: return
        val player = room.players.find { it.id == currentUserId }
        val senderName = player?.name ?: userProfile.value?.displayName ?: initialDisplayName
        val senderColor = player?.color ?: "red"
        viewModelScope.launch {
            val msg = ChatMessage(
                senderId = currentUserId,
                senderName = senderName,
                senderColor = senderColor,
                message = text
            )
            repository.sendChatMessage(room.roomId, msg)
        }
    }

    fun updateVoiceState(isTalking: Boolean) {
        val room = _currentRoom.value ?: return
        viewModelScope.launch {
            repository.updateVoiceState(room.roomId, currentUserId, isTalking)
        }
    }

    fun leaveRoom() {
        autoTurnJob?.cancel()
        autoTurnJob = null
        roomObservationJob?.cancel()
        roomObservationJob = null
        messagesObservationJob?.cancel()
        messagesObservationJob = null
        _messages.value = emptyList<ChatMessage>()
        _currentRoom.value = null
        _screen.value = ScreenState.LOBBY
    }

    fun navigateToLobby() {
        autoTurnJob?.cancel()
        autoTurnJob = null
        roomObservationJob?.cancel()
        roomObservationJob = null
        messagesObservationJob?.cancel()
        messagesObservationJob = null
        _messages.value = emptyList<ChatMessage>()
        _screen.value = ScreenState.LOBBY
    }
}
