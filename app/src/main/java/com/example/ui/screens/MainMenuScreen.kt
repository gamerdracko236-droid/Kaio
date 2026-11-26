package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stars
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.GameSaveSlot
import com.example.data.model.UiState
import com.example.data.model.WorldCatalog
import com.example.ui.theme.SertaoObsidian
import com.example.ui.theme.SolarGold

@Composable
fun MainMenuScreen(
    saveSlotsState: UiState<List<GameSaveSlot>>,
    isOfflineMode: Boolean = false,
    onNewGame: () -> Unit,
    onContinueGame: (GameSaveSlot) -> Unit,
    onSelectChapter: () -> Unit,
    onCharacters: () -> Unit,
    onSettings: () -> Unit,
    onCredits: () -> Unit,
    onSignOut: () -> Unit
) {
    val latestSave: GameSaveSlot? = (saveSlotsState as? UiState.Success)?.data?.firstOrNull()

    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = R.drawable.img_hero_sertao),
            contentDescription = "Cenário principal dos Caçadores do Sertão",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            SertaoObsidian.copy(alpha = 0.94f),
                            SertaoObsidian.copy(alpha = 0.78f),
                            SertaoObsidian.copy(alpha = 0.45f)
                        )
                    )
                )
        )

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(horizontal = 24.dp, vertical = 18.dp)
        ) {
            val isWide = maxWidth >= 650.dp
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.widthIn(max = 540.dp)) {
                    Text(
                        text = "A LENDA DOS",
                        style = MaterialTheme.typography.titleMedium,
                        color = SolarGold
                    )
                    Text(
                        text = "CAÇADORES DO SERTÃO",
                        style = MaterialTheme.typography.displayLarge,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Edição Completa • 10 Regiões • 20 Capítulos",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (isWide) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(24.dp),
                        verticalAlignment = Alignment.Bottom
                    ) {
                        MenuButtonsColumn(
                            latestSave = latestSave,
                            isOfflineMode = isOfflineMode,
                            onNewGame = onNewGame,
                            onContinueGame = onContinueGame,
                            onSelectChapter = onSelectChapter,
                            onCharacters = onCharacters,
                            onSettings = onSettings,
                            onCredits = onCredits,
                            onSignOut = onSignOut,
                            modifier = Modifier.weight(1f)
                        )
                        SaveStatusCard(
                            saveSlotsState = saveSlotsState,
                            isOfflineMode = isOfflineMode,
                            modifier = Modifier.weight(1f)
                        )
                    }
                } else {
                    Column(
                        modifier = Modifier.widthIn(max = 460.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        SaveStatusCard(
                            saveSlotsState = saveSlotsState,
                            isOfflineMode = isOfflineMode
                        )
                        MenuButtonsColumn(
                            latestSave = latestSave,
                            isOfflineMode = isOfflineMode,
                            onNewGame = onNewGame,
                            onContinueGame = onContinueGame,
                            onSelectChapter = onSelectChapter,
                            onCharacters = onCharacters,
                            onSettings = onSettings,
                            onCredits = onCredits,
                            onSignOut = onSignOut
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SaveStatusCard(
    saveSlotsState: UiState<List<GameSaveSlot>>,
    isOfflineMode: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f)
        ),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = if (isOfflineMode) "DIÁRIO DA ORDEM • CONTA OFFLINE" else "DIÁRIO DA ORDEM • NUVEM GOOGLE",
                style = MaterialTheme.typography.labelLarge,
                color = SolarGold
            )
            Spacer(modifier = Modifier.height(6.dp))
            when (saveSlotsState) {
                is UiState.Loading -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = if (isOfflineMode) "Carregando salvamentos locais..." else "Sincronizando progresso na nuvem...",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                is UiState.Error -> {
                    Text(
                        text = "Aviso: ${saveSlotsState.message}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                is UiState.Success -> {
                    val slot = saveSlotsState.data.firstOrNull()
                    if (slot == null) {
                        Text(
                            text = "Nenhum salvamento encontrado. Inicie um Novo Jogo na Vila das Almas.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        val region = WorldCatalog.regionById(slot.currentRegionId)
                        Text(
                            text = "${slot.heroName} • Nível ${slot.level} • Cap. ${slot.currentChapter}/20",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Região: ${region.name} (${slot.coins} Patacas)",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MenuButtonsColumn(
    latestSave: GameSaveSlot?,
    isOfflineMode: Boolean,
    onNewGame: () -> Unit,
    onContinueGame: (GameSaveSlot) -> Unit,
    onSelectChapter: () -> Unit,
    onCharacters: () -> Unit,
    onSettings: () -> Unit,
    onCredits: () -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (latestSave != null) {
            MenuPrimaryButton(
                label = "Continuar (${latestSave.heroName} • Nv.${latestSave.level})",
                icon = Icons.Filled.PlayArrow,
                testTag = "menu_continue_button",
                onClick = { onContinueGame(latestSave) }
            )
        }
        MenuPrimaryButton(
            label = "Novo Jogo",
            icon = Icons.Filled.AutoStories,
            testTag = "menu_new_game_button",
            isHighlighted = latestSave == null,
            onClick = onNewGame
        )
        MenuSecondaryButton(
            label = "Selecionar Capítulo (1–20)",
            icon = Icons.Filled.Map,
            testTag = "menu_chapters_button",
            onClick = onSelectChapter
        )
        MenuSecondaryButton(
            label = "Personagens & Customização",
            icon = Icons.Filled.Person,
            testTag = "menu_characters_button",
            onClick = onCharacters
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            MenuSecondaryButton(
                label = "Configurações",
                icon = Icons.Filled.Settings,
                testTag = "menu_settings_button",
                onClick = onSettings,
                modifier = Modifier.weight(1f)
            )
            MenuSecondaryButton(
                label = "Créditos",
                icon = Icons.Filled.Stars,
                testTag = "menu_credits_button",
                onClick = onCredits,
                modifier = Modifier.weight(1f)
            )
        }
        OutlinedButton(
            onClick = onSignOut,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("menu_exit_account_button")
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                contentDescription = if (isOfflineMode) "Conectar Google" else "Sair da conta"
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(if (isOfflineMode) "Conectar Conta Google" else "Sair / Trocar Conta")
        }
    }
}

@Composable
private fun MenuPrimaryButton(
    label: String,
    icon: ImageVector,
    testTag: String,
    isHighlighted: Boolean = true,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isHighlighted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
            contentColor = if (isHighlighted) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
        ),
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .testTag(testTag)
    ) {
        Icon(imageVector = icon, contentDescription = label)
        Spacer(modifier = Modifier.width(10.dp))
        Text(text = label, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun MenuSecondaryButton(
    label: String,
    icon: ImageVector,
    testTag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .testTag(testTag)
    ) {
        Icon(imageVector = icon, contentDescription = label)
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = label, style = MaterialTheme.typography.labelLarge)
    }
}
