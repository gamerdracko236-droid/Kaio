package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.audio.SertaoAudioEngine
import com.example.data.firebase.SaveRepository
import com.example.data.firebase.attemptAutoSignIn
import com.example.data.firebase.authStateFlow
import com.example.data.firebase.signOut
import com.example.data.local.OfflineSaveRepository
import com.example.data.settings.SettingsRepository
import com.example.game.GameViewModel
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.ChapterSelectScreen
import com.example.ui.screens.CharacterCustomizeScreen
import com.example.ui.screens.CreditsScreen
import com.example.ui.screens.GameplayScreen
import com.example.ui.screens.MainMenuScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.CacadoresSertaoTheme
import com.example.ui.theme.SertaoObsidian
import com.example.ui.theme.SolarGold
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

const val ROUTE_MENU = "menu"
const val ROUTE_GAMEPLAY = "gameplay"
const val ROUTE_CHAPTERS = "chapters"
const val ROUTE_CUSTOMIZE = "customize"
const val ROUTE_SETTINGS = "settings"
const val ROUTE_CREDITS = "credits/{isEpilogue}"

class MainActivity : ComponentActivity() {

    private lateinit var audioEngine: SertaoAudioEngine

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        audioEngine = SertaoAudioEngine(applicationContext)

        setContent {
            CacadoresSertaoTheme {
                val context = LocalContext.current
                val scope = rememberCoroutineScope()
                val credentialManager = remember(context) { CredentialManager.create(context) }

                val currentUser by Firebase.auth.authStateFlow()
                    .collectAsStateWithLifecycle(initialValue = Firebase.auth.currentUser)

                var isCheckingAutoAuth by remember { mutableStateOf(Firebase.auth.currentUser == null) }
                var isOfflineMode by rememberSaveable { mutableStateOf(false) }

                LaunchedEffect(Unit) {
                    if (Firebase.auth.currentUser == null && !isOfflineMode) {
                        attemptAutoSignIn(
                            context = context,
                            credentialManager = credentialManager,
                            onAuthSuccess = {
                                isOfflineMode = false
                                isCheckingAutoAuth = false
                            },
                            onUnauthenticated = { isCheckingAutoAuth = false },
                            scope = scope
                        )
                    } else {
                        isCheckingAutoAuth = false
                    }
                }

                when {
                    currentUser == null && !isOfflineMode && isCheckingAutoAuth -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(SertaoObsidian),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                CircularProgressIndicator(color = SolarGold)
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "Despertando os Guardiões do Sertão...",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = SolarGold
                                )
                            }
                        }
                    }

                    currentUser == null && !isOfflineMode -> {
                        AuthScreen(
                            onAuthSuccess = {
                                isOfflineMode = false
                                isCheckingAutoAuth = false
                            },
                            onPlayOffline = {
                                isOfflineMode = true
                                isCheckingAutoAuth = false
                            }
                        )
                    }

                    else -> {
                        val activeUserId = currentUser?.uid ?: "offline_local"
                        val offline = currentUser == null && isOfflineMode
                        key(activeUserId) {
                            CacadoresAppNavigation(
                                currentUserId = activeUserId,
                                isOfflineMode = offline,
                                audioEngine = audioEngine,
                                onSignOut = {
                                    audioEngine.stopVoice()
                                    audioEngine.stopMusic()
                                    if (offline) {
                                        isOfflineMode = false
                                    } else {
                                        signOut(credentialManager = credentialManager, scope = scope)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::audioEngine.isInitialized) {
            audioEngine.release()
        }
    }
}

@Composable
private fun CacadoresAppNavigation(
    currentUserId: String,
    isOfflineMode: Boolean,
    audioEngine: SertaoAudioEngine,
    onSignOut: () -> Unit
) {
    val appContext = LocalContext.current.applicationContext
    val viewModel: GameViewModel = viewModel(
        key = currentUserId,
        factory = viewModelFactory {
            initializer {
                val saveStorage = if (isOfflineMode) {
                    OfflineSaveRepository(appContext)
                } else {
                    val databaseId = appContext.getString(R.string.firestore_database_id)
                    SaveRepository(FirebaseFirestore.getInstance(databaseId))
                }
                GameViewModel(
                    saveRepository = saveStorage,
                    settingsRepository = SettingsRepository(appContext),
                    audioEngine = audioEngine,
                    currentUserId = currentUserId
                )
            }
        }
    )

    val navController = rememberNavController()
    val hudState by viewModel.hudState.collectAsStateWithLifecycle()
    val saveSlotsState by viewModel.saveSlotsState.collectAsStateWithLifecycle()
    val settingsState by viewModel.settingsState.collectAsStateWithLifecycle()

    NavHost(
        navController = navController,
        startDestination = ROUTE_MENU
    ) {
        composable(ROUTE_MENU) {
            LaunchedEffect(Unit) {
                viewModel.enterMenuMusic()
            }
            MainMenuScreen(
                saveSlotsState = saveSlotsState,
                isOfflineMode = isOfflineMode,
                onNewGame = {
                    viewModel.startNewCampaign()
                    navController.navigate(ROUTE_GAMEPLAY) {
                        launchSingleTop = true
                    }
                },
                onContinueGame = { slot ->
                    viewModel.loadFromSaveSlot(slot)
                    navController.navigate(ROUTE_GAMEPLAY) {
                        launchSingleTop = true
                    }
                },
                onSelectChapter = {
                    navController.navigate(ROUTE_CHAPTERS) {
                        launchSingleTop = true
                    }
                },
                onCharacters = {
                    navController.navigate(ROUTE_CUSTOMIZE) {
                        launchSingleTop = true
                    }
                },
                onSettings = {
                    navController.navigate(ROUTE_SETTINGS) {
                        launchSingleTop = true
                    }
                },
                onCredits = {
                    navController.navigate("credits/false") {
                        launchSingleTop = true
                    }
                },
                onSignOut = onSignOut
            )
        }

        composable(ROUTE_GAMEPLAY) {
            GameplayScreen(
                viewModel = viewModel,
                hudState = hudState,
                settings = settingsState,
                saveSlotsState = saveSlotsState,
                onExitToMenu = {
                    viewModel.enterMenuMusic()
                    navController.popBackStack(ROUTE_MENU, inclusive = false)
                },
                onOpenCredits = {
                    val isEpilogue = hudState.campaignCompleted
                    navController.navigate("credits/$isEpilogue") {
                        popUpTo(ROUTE_MENU) { inclusive = false }
                        launchSingleTop = true
                    }
                }
            )
        }

        composable(ROUTE_CHAPTERS) {
            ChapterSelectScreen(
                unlockedRegions = hudState.unlockedRegions,
                completedQuests = hudState.completedQuests,
                onStartChapter = { chapter ->
                    viewModel.startChapter(chapter)
                    navController.navigate(ROUTE_GAMEPLAY) {
                        popUpTo(ROUTE_MENU) { inclusive = false }
                        launchSingleTop = true
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(ROUTE_CUSTOMIZE) {
            CharacterCustomizeScreen(
                hudState = hudState,
                onUpdateCustomization = { name, hat, outfit, blade, style ->
                    viewModel.updateCustomization(name, hat, outfit, blade, style)
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(ROUTE_SETTINGS) {
            SettingsScreen(
                settings = settingsState,
                onUpdateSettings = { transform -> viewModel.updateSettings(transform) },
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = ROUTE_CREDITS,
            arguments = listOf(navArgument("isEpilogue") { type = NavType.BoolType })
        ) { backStackEntry ->
            val isEpilogue = backStackEntry.arguments?.getBoolean("isEpilogue") ?: false
            LaunchedEffect(Unit) {
                viewModel.enterCreditsMusic()
            }
            CreditsScreen(
                heroName = hudState.heroName,
                isEpilogue = isEpilogue,
                onReturnToMenu = {
                    viewModel.enterMenuMusic()
                    navController.popBackStack(ROUTE_MENU, inclusive = false)
                }
            )
        }
    }
}
