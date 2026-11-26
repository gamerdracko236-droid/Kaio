package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.data.model.BladeColor
import com.example.data.model.CombatStyle
import com.example.data.model.HatStyle
import com.example.data.model.OutfitStyle
import com.example.data.model.SpeakerId
import com.example.game.GameHudState
import com.example.ui.theme.SolarGold

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CharacterCustomizeScreen(
    hudState: GameHudState,
    onUpdateCustomization: (
        heroName: String?,
        hatStyle: HatStyle?,
        outfitStyle: OutfitStyle?,
        bladeColor: BladeColor?,
        activeStyle: CombatStyle?
    ) -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Personagens & Customização") },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("customize_back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Protagonista Personalizável", style = MaterialTheme.typography.titleLarge, color = SolarGold)
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedTextField(
                            value = hudState.heroName,
                            onValueChange = { onUpdateCustomization(it, null, null, null, null) },
                            label = { Text("Nome do Guardião") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("hero_name_input")
                        )

                        Spacer(modifier = Modifier.height(14.dp))
                        Text("Chapéu / Cabeça", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(6.dp))
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            HatStyle.entries.forEach { hat ->
                                FilterChip(
                                    selected = hudState.hatStyle == hat,
                                    onClick = { onUpdateCustomization(null, hat, null, null, null) },
                                    label = { Text(hat.displayName) }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        Text("Traje / Gibão", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(6.dp))
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutfitStyle.entries.forEach { outfit ->
                                FilterChip(
                                    selected = hudState.outfitStyle == outfit,
                                    onClick = { onUpdateCustomization(null, null, outfit, null, null) },
                                    label = { Text(outfit.displayName) }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        Text("Cor da Lâmina Solar", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(6.dp))
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            BladeColor.entries.forEach { blade ->
                                FilterChip(
                                    selected = hudState.bladeColor == blade,
                                    onClick = { onUpdateCustomization(null, null, null, blade, null) },
                                    label = { Text(blade.displayName) }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        Text("Estilo de Combate Principal", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(6.dp))
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            CombatStyle.entries.forEach { style ->
                                val accent = Color(style.colorHex)
                                FilterChip(
                                    selected = hudState.activeStyle == style,
                                    onClick = { onUpdateCustomization(null, null, null, null, style) },
                                    label = { Text(style.displayName, color = if (hudState.activeStyle == style) accent else MaterialTheme.colorScheme.onSurface) }
                                )
                            }
                        }
                    }
                }
            }

            item {
                Text(
                    text = "Elenco da Campanha (Vozes PT-BR)",
                    style = MaterialTheme.typography.titleLarge,
                    color = SolarGold
                )
            }

            val roster = listOf(
                Triple(SpeakerId.KAUAN, "Protagonista • Guardião da Chama", "Jovem sertanejo da Vila das Almas que despertou a lâmina solar de Antônio Brasa."),
                Triple(SpeakerId.SEVERINO, "Mestre da Ordem", "Veterano espadachim que guia os novos Guardiões pela Caatinga e Serra dos Ventos."),
                Triple(SpeakerId.TAINA, "Aliada • Estilo das Águas", "Guerreira ágil que domina o combate fluido contra feras e espectros."),
                Triple(SpeakerId.MATEUS, "Rival & Aliado • Estilo da Tempestade", "Prodígio veloz que supera uma antiga mágoa para lutar pelo sertão."),
                Triple(SpeakerId.CORONEL_UMBRA, "Antagonista • Senhor do Eclipse", "Líder sombrio por trás dos Encantados da Sombra e do Eclipse Perpétuo.")
            )
            items(roster) { (speaker, role, bio) ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(
                            painter = painterResource(id = speaker.portraitRes),
                            contentDescription = speaker.displayName,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(64.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .border(2.dp, SolarGold, RoundedCornerShape(12.dp))
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(speaker.displayName, style = MaterialTheme.typography.titleMedium, color = SolarGold)
                            Text(role, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(bio, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}
