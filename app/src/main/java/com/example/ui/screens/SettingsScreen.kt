package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.model.QualityLevel
import com.example.data.settings.GameSettings
import com.example.ui.theme.SolarGold

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settings: GameSettings,
    onUpdateSettings: ((GameSettings) -> GameSettings) -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Configurações") },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("settings_back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Áudio e Dublagem PT-BR", style = MaterialTheme.typography.titleLarge, color = SolarGold)
                    Spacer(modifier = Modifier.height(10.dp))

                    Text("Música Nordestina: ${(settings.musicVolume * 100).toInt()}%")
                    Slider(
                        value = settings.musicVolume,
                        onValueChange = { v -> onUpdateSettings { it.copy(musicVolume = v) } }
                    )

                    Text("Efeitos Sonoros (SFX): ${(settings.sfxVolume * 100).toInt()}%")
                    Slider(
                        value = settings.sfxVolume,
                        onValueChange = { v -> onUpdateSettings { it.copy(sfxVolume = v) } }
                    )

                    Text("Vozes / Dublagem: ${(settings.voiceVolume * 100).toInt()}%")
                    Slider(
                        value = settings.voiceVolume,
                        onValueChange = { v -> onUpdateSettings { it.copy(voiceVolume = v) } }
                    )
                }
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Gráficos 3D e Interface", style = MaterialTheme.typography.titleLarge, color = SolarGold)
                    Spacer(modifier = Modifier.height(10.dp))

                    Text("Qualidade Gráfica", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        QualityLevel.entries.forEach { q ->
                            FilterChip(
                                selected = settings.qualityLevel == q,
                                onClick = { onUpdateSettings { it.copy(qualityLevel = q) } },
                                label = { Text(q.displayName) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Sensibilidade da Câmera: ${"%.1f".format(settings.cameraSensitivity)}x")
                    Slider(
                        value = settings.cameraSensitivity,
                        valueRange = 0.4f..2.0f,
                        onValueChange = { v -> onUpdateSettings { it.copy(cameraSensitivity = v) } }
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Legendas Animadas nos Diálogos")
                        Switch(
                            checked = settings.subtitlesEnabled,
                            onCheckedChange = { checked -> onUpdateSettings { it.copy(subtitlesEnabled = checked) } }
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Exibir Minimapa Tático")
                        Switch(
                            checked = settings.showMinimap,
                            onCheckedChange = { checked -> onUpdateSettings { it.copy(showMinimap = checked) } }
                        )
                    }
                }
            }
        }
    }
}
