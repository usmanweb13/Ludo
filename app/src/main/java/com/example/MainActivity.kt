package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.credentials.CredentialManager
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.repository.LudoRepository
import com.example.ui.auth.attemptAutoSignIn
import com.example.ui.auth.signOutUser
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.GameScreen
import com.example.ui.screens.LobbyScreen
import com.example.ui.screens.RoomWaitingScreen
import com.example.ui.sound.SoundManager
import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.example.ui.sound.VoiceChatManager
import com.example.ui.theme.LudoBg
import com.example.ui.theme.LudoTheme
import com.example.ui.viewmodel.LudoViewModel
import com.example.ui.viewmodel.ScreenState
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LudoTheme {
                Scaffold(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(LudoBg)
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .safeDrawingPadding()
                    ) {
                        AppNavigation()
                    }
                }
            }
        }
    }
}

@Composable
fun AppNavigation() {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val auth = remember { FirebaseAuth.getInstance() }
    val soundManager = remember { SoundManager(context) }
    var currentUser by remember { mutableStateOf<FirebaseUser?>(auth.currentUser) }
    var forceLocalGame by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            currentUser = firebaseAuth.currentUser
        }
        auth.addAuthStateListener(listener)
        onDispose {
            auth.removeAuthStateListener(listener)
            soundManager.release()
        }
    }

    LaunchedEffect(Unit) {
        if (currentUser == null) {
            val credentialManager = CredentialManager.create(context)
            attemptAutoSignIn(
                context = context,
                credentialManager = credentialManager,
                onAuthSuccess = {
                    currentUser = auth.currentUser
                },
                onUnauthenticated = {},
                scope = coroutineScope
            )
        }
    }

    val user = currentUser
    if (user == null && !forceLocalGame) {
        AuthScreen(
            onAuthSuccess = {
                currentUser = auth.currentUser
            },
            onPlayLocalPassAndPlay = {
                forceLocalGame = true
            }
        )
    } else {
        val userId = user?.uid ?: "local_player"
        val displayName = user?.displayName ?: "Player"

        val viewModel: LudoViewModel = viewModel(
            key = userId,
            factory = viewModelFactory {
                initializer {
                    val repo = LudoRepository(context)
                    LudoViewModel(repo, userId, displayName)
                }
            }
        )

        LaunchedEffect(forceLocalGame) {
            if (forceLocalGame && user == null) {
                viewModel.startLocalGame(4)
            }
        }

        val screen by viewModel.screen.collectAsState()
        val userProfile by viewModel.userProfile.collectAsState()
        val currentRoom by viewModel.currentRoom.collectAsState()
        val publicRooms by viewModel.publicRooms.collectAsState()
        val isMatchmaking by viewModel.isMatchmaking.collectAsState()
        val errorMessage by viewModel.errorMessage.collectAsState()
        val localGameState by viewModel.localGame.collectAsState()
        val isRollingOnline by viewModel.isRollingOnline.collectAsState()
        val messages by viewModel.messages.collectAsState()

        val voiceChatManager = remember(context) {
            VoiceChatManager(
                context = context,
                scope = coroutineScope,
                onSpeakingStateChanged = { speaking ->
                    viewModel.updateVoiceState(speaking)
                }
            )
        }

        DisposableEffect(Unit) {
            onDispose {
                voiceChatManager.release()
            }
        }

        val isMicOn by voiceChatManager.isMicEnabled.collectAsState()
        val isSpeaking by voiceChatManager.isSpeaking.collectAsState()
        val audioLevel by voiceChatManager.audioLevel.collectAsState()

        val micPermissionLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            if (isGranted) {
                voiceChatManager.enableMic()
            }
        }

        val handleToggleMic = {
            if (voiceChatManager.hasPermission()) {
                voiceChatManager.toggleMic()
            } else {
                micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        }

        val handlePushToTalk: (Boolean) -> Unit = { active ->
            if (active && !voiceChatManager.hasPermission()) {
                micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            } else {
                voiceChatManager.setPushToTalk(active)
            }
        }

        when (screen) {
            ScreenState.LOBBY -> {
                LobbyScreen(
                    userProfile = userProfile,
                    displayName = displayName,
                    publicRooms = publicRooms,
                    isMatchmaking = isMatchmaking,
                    errorMessage = errorMessage,
                    onClearError = { viewModel.clearError() },
                    onQuickMatch = { noAiMode -> viewModel.findQuickMatch(noAiMode = noAiMode, maxPlayers = if (noAiMode) 2 else 4) },
                    onCreateRoom = { code, maxPlayers, noAiMode -> viewModel.createPrivateRoom(code, maxPlayers, noAiMode) },
                    onJoinRoom = { code -> viewModel.joinRoomByCode(code) },
                    onStartLocalGame = { count -> viewModel.startLocalGame(count) },
                    onSignOut = {
                        val credentialManager = CredentialManager.create(context)
                        signOutUser(context, credentialManager, onSignOutComplete = {
                            currentUser = null
                            forceLocalGame = false
                        }, scope = coroutineScope)
                    }
                )
            }

            ScreenState.ROOM_WAITING -> {
                val room = currentRoom
                if (room != null) {
                    RoomWaitingScreen(
                        room = room,
                        currentUserId = userId,
                        onStartGame = { viewModel.startGame(fillWithBots = false) },
                        onStartGameWithBots = { viewModel.startGame(fillWithBots = true) },
                        onAddBot = { viewModel.addBotPlayer() },
                        onRemovePlayer = { playerId -> viewModel.removePlayer(playerId) },
                        onLeaveRoom = { viewModel.leaveRoom() }
                    )
                } else {
                    viewModel.leaveRoom()
                }
            }

            ScreenState.ONLINE_GAME -> {
                val room = currentRoom
                if (room != null) {
                    val myPlayer = room.players.find { it.id == userId }
                    val isMyTurn = myPlayer != null && room.turn == myPlayer.color
                    val playerLabels = room.players.associate { it.color to it.name }

                    GameScreen(
                        title = room.name.ifBlank { "Room ${room.roomId}" },
                        tokens = room.tokens,
                        activeColors = room.activeColors,
                        currentTurn = room.turn,
                        dice = room.dice,
                        rolled = room.rolled,
                        winner = room.winner,
                        isMyTurn = isMyTurn,
                        isRolling = isRollingOnline,
                        playerLabels = playerLabels,
                        soundManager = soundManager,
                        errorMessage = errorMessage,
                        onClearError = { viewModel.clearError() },
                        noAiMode = room.noAiMode,
                        messages = messages,
                        currentUserId = userId,
                        myColor = myPlayer?.color ?: "red",
                        isMicOn = isMicOn,
                        isSpeaking = isSpeaking,
                        audioLevel = audioLevel,
                        voiceState = room.voiceState,
                        onToggleMic = handleToggleMic,
                        onPushToTalk = handlePushToTalk,
                        onSendMessage = { text -> viewModel.sendChatMessage(text) },
                        onRollDice = { viewModel.rollDiceOnline() },
                        onTokenClicked = { color, index -> viewModel.moveTokenOnline(color, index) },
                        onExitGame = {
                            voiceChatManager.disableMic()
                            viewModel.leaveRoom()
                        }
                    )
                } else {
                    viewModel.leaveRoom()
                }
            }

            ScreenState.LOCAL_GAME -> {
                GameScreen(
                    title = "Local Pass & Play",
                    tokens = localGameState.tokens,
                    activeColors = localGameState.activeColors,
                    currentTurn = localGameState.turn,
                    dice = localGameState.dice,
                    rolled = localGameState.rolled,
                    winner = localGameState.winner,
                    isMyTurn = true, // Pass & play is local
                    isRolling = localGameState.isRolling,
                    playerLabels = mapOf(
                        "red" to "Red",
                        "blue" to "Blue",
                        "yellow" to "Yellow",
                        "green" to "Green"
                    ),
                    soundManager = soundManager,
                    onRollDice = { viewModel.rollDiceLocal() },
                    onTokenClicked = { color, index -> viewModel.moveTokenLocal(color, index) },
                    onExitGame = {
                        if (user == null) {
                            forceLocalGame = false
                        }
                        viewModel.navigateToLobby()
                    },
                    onPlayAgain = { viewModel.startLocalGame(localGameState.activeColors.size) }
                )
            }
        }
    }
}
