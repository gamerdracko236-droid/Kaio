package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.model.GameSaveSlot
import com.example.data.model.UiState
import com.example.data.model.WorldCatalog
import com.example.game.GameHudState
import com.example.ui.theme.SertaoObsidian
import com.example.ui.theme.SolarGold

enum class RpgOverlayTab {
    INVENTARIO_LOJA, HABILIDADES, DIARIO_CONQUISTAS, SALVAMENTO
}

@Composable
fun RpgMenuOverlay(
    initialTab: RpgOverlayTab,
    hudState: GameHudState,
    saveSlotsState: UiState<List<GameSaveSlot>>,
    onBuyItem: (String) -> Unit,
    onUseItem: (String) -> Unit,
    onUpgradeWeapon: () -> Unit,
    onInvestAttr: (String) -> Unit,
    onUnlockSkill: (String) -> Unit,
    onSaveSlot: (String, String) -> Unit,
    onLoadSlot: (GameSaveSlot) -> Unit,
    onDeleteSlot: (String) -> Unit,
    onDismissMessage: () -> Unit,
    onClose: () -> Unit
) {
    var selectedTab by remember(initialTab) { mutableIntStateOf(initialTab.ordinal) }
    val tabTitles = listOf("Loja & Bolsa", "Habilidades", "Diário", "Salvar")

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SertaoObsidian.copy(alpha = 0.88f))
            .safeDrawingPadding()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .widthIn(max = 760.dp)
                .fillMaxSize()
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "${hudState.heroName} • Nível ${hudState.level}",
                            style = MaterialTheme.typography.titleLarge,
                            color = SolarGold
                        )
                        Text(
                            text = "${hudState.coins} Patacas • Lâmina +${hudState.weaponUpgrade} • Pontos: ${hudState.skillPoints}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onClose, modifier = Modifier.testTag("overlay_close_button")) {
                        Icon(Icons.Filled.Close, contentDescription = "Fechar menu RPG")
                    }
                }

                if (hudState.statusMessage != null) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(hudState.statusMessage, style = MaterialTheme.typography.bodyMedium, color = SolarGold)
                            OutlinedButton(onClick = onDismissMessage) { Text("OK") }
                        }
                    }
                }

                TabRow(selectedTabIndex = selectedTab) {
                    tabTitles.forEachIndexed { idx, title ->
                        Tab(
                            selected = selectedTab == idx,
                            onClick = { selectedTab = idx },
                            text = { Text(title, style = MaterialTheme.typography.labelLarge) }
                        )
                    }
                }

                when (RpgOverlayTab.entries[selectedTab]) {
                    RpgOverlayTab.INVENTARIO_LOJA -> InventoryShopTab(
                        hudState = hudState,
                        onBuyItem = onBuyItem,
                        onUseItem = onUseItem,
                        onUpgradeWeapon = onUpgradeWeapon
                    )
                    RpgOverlayTab.HABILIDADES -> SkillsTab(
                        hudState = hudState,
                        onInvestAttr = onInvestAttr,
                        onUnlockSkill = onUnlockSkill
                    )
                    RpgOverlayTab.DIARIO_CONQUISTAS -> JournalAchievementsTab(hudState = hudState)
                    RpgOverlayTab.SALVAMENTO -> SaveLoadTab(
                        saveSlotsState = saveSlotsState,
                        onSaveSlot = onSaveSlot,
                        onLoadSlot = onLoadSlot,
                        onDeleteSlot = onDeleteSlot
                    )
                }
            }
        }
    }
}

@Composable
private fun InventoryShopTab(
    hudState: GameHudState,
    onBuyItem: (String) -> Unit,
    onUseItem: (String) -> Unit,
    onUpgradeWeapon: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Forja de Scheelita (${hudState.bladeColor.displayName})", style = MaterialTheme.typography.titleMedium, color = SolarGold)
                        Text("Melhoria atual: +${hudState.weaponUpgrade} / +10", style = MaterialTheme.typography.bodyMedium)
                    }
                    Button(
                        onClick = onUpgradeWeapon,
                        enabled = hudState.weaponUpgrade < 10,
                        modifier = Modifier.testTag("upgrade_blade_button")
                    ) {
                        Text(if (hudState.weaponUpgrade < 10) "Forjar (${hudState.weaponUpgrade * 45})" else "Máximo")
                    }
                }
            }
        }

        items(WorldCatalog.shopItems, key = { it.id }) { item ->
            val owned = hudState.inventory[item.id] ?: 0
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("${item.name} (Na bolsa: $owned)", style = MaterialTheme.typography.titleMedium)
                        Text(item.description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Button(onClick = { onBuyItem(item.id) }) {
                            Text("Comprar (${item.priceCoins})")
                        }
                        if (owned > 0) {
                            OutlinedButton(onClick = { onUseItem(item.id) }) {
                                Text("Usar")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SkillsTab(
    hudState: GameHudState,
    onInvestAttr: (String) -> Unit,
    onUnlockSkill: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Atributos do Guardião (Pontos livres: ${hudState.skillPoints})", style = MaterialTheme.typography.titleMedium, color = SolarGold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { onInvestAttr("atk") },
                            enabled = hudState.skillPoints > 0,
                            modifier = Modifier.weight(1f)
                        ) { Text("Força +1 (${hudState.attackBonus})") }
                        Button(
                            onClick = { onInvestAttr("def") },
                            enabled = hudState.skillPoints > 0,
                            modifier = Modifier.weight(1f)
                        ) { Text("Vigor +1 (${hudState.defenseBonus})") }
                        Button(
                            onClick = { onInvestAttr("eng") },
                            enabled = hudState.skillPoints > 0,
                            modifier = Modifier.weight(1f)
                        ) { Text("Fôlego +1 (${hudState.energyBonus})") }
                    }
                }
            }
        }

        items(WorldCatalog.skills, key = { it.id }) { skill ->
            val unlocked = skill.id in hudState.unlockedSkills
            val accent = Color(skill.style.colorHex)
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("${skill.name} (${skill.style.displayName})", style = MaterialTheme.typography.titleMedium, color = accent)
                        Text(skill.description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Dano base: ${skill.damage.toInt()} • Custo Energia: ${skill.energyCost.toInt()}", style = MaterialTheme.typography.labelSmall)
                    }
                    if (unlocked) {
                        Text("ATIVA", style = MaterialTheme.typography.labelLarge, color = SolarGold)
                    } else {
                        Button(
                            onClick = { onUnlockSkill(skill.id) },
                            enabled = hudState.skillPoints > 0
                        ) {
                            Text("Aprender (1 pt)")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun JournalAchievementsTab(hudState: GameHudState) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text("Diário de Aventuras", style = MaterialTheme.typography.titleLarge, color = SolarGold)
        }
        items(WorldCatalog.journalEntries, key = { it.id }) { entry ->
            val unlocked = hudState.currentRegion.index >= entry.unlockChapter || hudState.completedQuests.isNotEmpty()
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(entry.title, style = MaterialTheme.typography.titleMedium, color = SolarGold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (unlocked) entry.body else "Avance na campanha para revelar este capítulo do diário.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(8.dp))
            Text("Conquistas (${hudState.unlockedAchievements.size}/${WorldCatalog.achievements.size})", style = MaterialTheme.typography.titleLarge, color = SolarGold)
        }
        items(WorldCatalog.achievements, key = { it.id }) { ach ->
            val done = ach.id in hudState.unlockedAchievements
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(ach.title, style = MaterialTheme.typography.titleMedium, color = if (done) SolarGold else MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(ach.description, style = MaterialTheme.typography.bodyMedium)
                    }
                    Text(if (done) "CONCLUÍDA" else "Bloqueada", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

@Composable
private fun SaveLoadTab(
    saveSlotsState: UiState<List<GameSaveSlot>>,
    onSaveSlot: (String, String) -> Unit,
    onLoadSlot: (GameSaveSlot) -> Unit,
    onDeleteSlot: (String) -> Unit
) {
    val manualSlots = listOf("slot_1" to "Slot Manual 1", "slot_2" to "Slot Manual 2", "slot_3" to "Slot Manual 3")
    val existing = (saveSlotsState as? UiState.Success)?.data ?: emptyList()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text("Salvamento Manual na Nuvem", style = MaterialTheme.typography.titleLarge, color = SolarGold)
        }
        items(manualSlots) { (slotId, label) ->
            val saved = existing.firstOrNull { it.id == slotId }
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(label, style = MaterialTheme.typography.titleMedium, color = SolarGold)
                        if (saved != null) {
                            val reg = WorldCatalog.regionById(saved.currentRegionId)
                            Text("${saved.heroName} • Nv.${saved.level} • ${reg.name}", style = MaterialTheme.typography.bodyMedium)
                        } else {
                            Text("Vazio", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Button(
                            onClick = { onSaveSlot(slotId, label) },
                            modifier = Modifier.testTag("save_$slotId")
                        ) {
                            Icon(Icons.Filled.Save, contentDescription = "Salvar em $label")
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Salvar")
                        }
                        if (saved != null) {
                            OutlinedButton(onClick = { onLoadSlot(saved) }) { Text("Carregar") }
                            IconButton(onClick = { onDeleteSlot(slotId) }) {
                                Icon(Icons.Filled.Delete, contentDescription = "Apagar $label")
                            }
                        }
                    }
                }
            }
        }
    }
}
