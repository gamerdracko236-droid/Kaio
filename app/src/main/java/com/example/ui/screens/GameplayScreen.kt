package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Backpack
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.DialogueChoice
import com.example.data.model.GameSaveSlot
import com.example.data.model.UiState
import com.example.data.settings.GameSettings
import com.example.engine3d.Sertao3DSurfaceView
import com.example.game.ActiveDialogueState
import com.example.game.BossHudInfo
import com.example.game.GameHudState
import com.example.game.GameViewModel
import com.example.game.MinimapBlip
import com.example.ui.theme.BossCrimson
import com.example.ui.theme.EmberOrange
import com.example.ui.theme.EnergyAmber
import com.example.ui.theme.HealthGreen
import com.example.ui.theme.PostureYellow
import com.example.ui.theme.SertaoObsidian
import com.example.ui.theme.SolarGold
import com.example.ui.theme.StaminaCyan
import kotlinx.coroutines.delay
import kotlin.math.hypot
import kotlin.math.roundToInt

private val StormGoldBorder = Brush.linearGradient(
    listOf(Color(0xFFFFE082), Color(0xFFD4A024), Color(0xFF8C5E14))
)

private val StormParchmentBg = Brush.verticalGradient(
    listOf(Color(0xFFF6E7C8), Color(0xFFE7CE9F))
)

@Composable
fun GameplayScreen(
    viewModel: GameViewModel,
    hudState: GameHudState,
    settings: GameSettings,
    saveSlotsState: UiState<List<GameSaveSlot>>,
    onExitToMenu: () -> Unit,
    onOpenCredits: () -> Unit
) {
    var activeOverlay by remember { mutableStateOf<RpgOverlayTab?>(null) }

    BackHandler(enabled = !hudState.campaignCompleted) {
        when {
            activeOverlay != null -> activeOverlay = null
            hudState.isGameOver -> onExitToMenu()
            else -> viewModel.setPaused(!hudState.isPaused)
        }
    }

    // Redirect automatically to Credits/Epilogue when campaign finishes
    LaunchedEffect(hudState.campaignCompleted) {
        if (hudState.campaignCompleted) {
            onOpenCredits()
        }
    }

    // Hardware keyboard support for PC / Emulator testing
    val heldKeys = remember { mutableSetOf<Key>() }
    fun updateKeyboardVector() {
        var kx = 0f
        var ky = 0f
        if (Key.A in heldKeys || Key.DirectionLeft in heldKeys) kx -= 1f
        if (Key.D in heldKeys || Key.DirectionRight in heldKeys) kx += 1f
        if (Key.W in heldKeys || Key.DirectionUp in heldKeys) ky -= 1f
        if (Key.S in heldKeys || Key.DirectionDown in heldKeys) ky += 1f
        viewModel.setJoystickInput(kx, ky, sprint = Key.ShiftLeft in heldKeys)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onPreviewKeyEvent { ev ->
                if (ev.type == KeyEventType.KeyDown) {
                    heldKeys.add(ev.key)
                    when (ev.key) {
                        Key.W, Key.A, Key.S, Key.D, Key.DirectionUp, Key.DirectionDown,
                        Key.DirectionLeft, Key.DirectionRight, Key.ShiftLeft -> {
                            updateKeyboardVector(); true
                        }
                        Key.J -> { viewModel.triggerLightAttack(); true }
                        Key.K -> { viewModel.triggerDodge(); true }
                        Key.L -> { viewModel.setBlocking(true); true }
                        Key.U -> { viewModel.triggerSkill(1); true }
                        Key.I -> { viewModel.triggerSkill(2); true }
                        Key.O -> { viewModel.triggerUltimate(); true }
                        Key.R -> { viewModel.triggerHeavyAttack(); true }
                        Key.Spacebar -> { viewModel.triggerJump(); true }
                        Key.Tab -> { viewModel.cycleCombatStyle(); true }
                        Key.F -> { viewModel.interactWithWorld(); true }
                        else -> false
                    }
                } else if (ev.type == KeyEventType.KeyUp) {
                    heldKeys.remove(ev.key)
                    when (ev.key) {
                        Key.W, Key.A, Key.S, Key.D, Key.DirectionUp, Key.DirectionDown,
                        Key.DirectionLeft, Key.DirectionRight, Key.ShiftLeft -> {
                            updateKeyboardVector(); true
                        }
                        Key.L -> { viewModel.setBlocking(false); true }
                        else -> false
                    }
                } else false
            }
    ) {
        // 1. Live 3D Cel-Shaded Sertão World
        Sertao3DSurfaceView(
            renderer = viewModel.renderer,
            sensitivity = settings.cameraSensitivity,
            modifier = Modifier.fillMaxSize()
        )

        // 2. Responsive Horizontal HUD Layer (zero-overlap layout for Android landscape & tablets)
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(horizontal = 10.dp, vertical = 8.dp)
        ) {
            val isCompactHeight = maxHeight < 460.dp

            // Top-Left: Character Portrait, Vitals, Minimap & Quest (side-by-side on compact landscape!)
            if (isCompactHeight) {
                Row(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .widthIn(max = 420.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    HeroStatusCard(
                        hudState = hudState,
                        compact = true,
                        onCycleStyle = { viewModel.cycleCombatStyle() },
                        modifier = Modifier.width(210.dp)
                    )
                    if (settings.showMinimap) {
                        MinimapRadar(blips = hudState.minimapBlips, sizeDp = 58)
                    }
                    QuestTrackerChip(hudState = hudState, modifier = Modifier.widthIn(max = 138.dp))
                }
            } else {
                Column(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .widthIn(max = 268.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    HeroStatusCard(
                        hudState = hudState,
                        compact = false,
                        onCycleStyle = { viewModel.cycleCombatStyle() }
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        if (settings.showMinimap) {
                            MinimapRadar(blips = hudState.minimapBlips, sizeDp = 72)
                        }
                        QuestTrackerChip(hudState = hudState)
                    }
                }
            }

            // Top-Center: Boss Bar & Combat Banner
            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 2.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                hudState.bossHud?.let { boss ->
                    BossHealthPanel(boss = boss)
                }
                hudState.combatBanner?.let { banner ->
                    Spacer(modifier = Modifier.height(6.dp))
                    Surface(
                        color = SertaoObsidian.copy(alpha = 0.85f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.border(1.dp, SolarGold, RoundedCornerShape(12.dp))
                    ) {
                        Text(
                            text = banner,
                            style = MaterialTheme.typography.titleMedium,
                            color = SolarGold,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            // Top-Right: Coins + RPG Menu Shortcuts
            Column(
                modifier = Modifier.align(Alignment.TopEnd),
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Surface(
                    color = SertaoObsidian.copy(alpha = 0.78f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "${hudState.coins} Patacas",
                            style = MaterialTheme.typography.labelLarge,
                            color = SolarGold
                        )
                        IconButton(
                            onClick = { viewModel.setPaused(true) },
                            modifier = Modifier
                                .size(36.dp)
                                .testTag("hud_pause_button")
                        ) {
                            Icon(Icons.Filled.Pause, contentDescription = "Pausar", tint = Color.White)
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    HudMiniIconBtn(Icons.Filled.Backpack, "Loja & Bolsa", "hud_bag_button") {
                        activeOverlay = RpgOverlayTab.INVENTARIO_LOJA
                    }
                    HudMiniIconBtn(Icons.Filled.Whatshot, "Habilidades", "hud_skills_button") {
                        activeOverlay = RpgOverlayTab.HABILIDADES
                    }
                    HudMiniIconBtn(Icons.AutoMirrored.Filled.MenuBook, "Diário", "hud_journal_button") {
                        activeOverlay = RpgOverlayTab.DIARIO_CONQUISTAS
                    }
                    HudMiniIconBtn(Icons.Filled.Save, "Salvar", "hud_save_button") {
                        activeOverlay = RpgOverlayTab.SALVAMENTO
                    }
                    HudMiniIconBtn(
                        icon = Icons.Filled.CenterFocusStrong,
                        desc = "Travar Alvo",
                        tag = "hud_lock_button",
                        highlighted = hudState.isLockedOn
                    ) {
                        viewModel.toggleTargetLock()
                    }
                }
            }

            // Mid-Left: Animated Storm Combo Hit Counter (non-overlapping above joystick)
            if (hudState.activeDialogue == null) {
                StormComboCounter(
                    comboCount = hudState.comboHits,
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = 10.dp, bottom = if (isCompactHeight) 36.dp else 44.dp)
                )
            }

            // Bottom-Left: Box Overlay containing VirtualJoystick + Quick Consumables
            if (hudState.activeDialogue == null) {
                Box(
                    modifier = Modifier.align(Alignment.BottomStart),
                    contentAlignment = Alignment.BottomStart
                ) {
                    if (isCompactHeight) {
                        Row(
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            VirtualJoystick(
                                padSizeDp = 112,
                                onMove = { x, y -> viewModel.setJoystickInput(x, y) }
                            )
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    ConsumableButton("Chá", hudState.inventory["pocao"] ?: 0) {
                                        viewModel.useConsumable("pocao")
                                    }
                                    ConsumableButton("Folha", hudState.inventory["antidoto"] ?: 0) {
                                        viewModel.useConsumable("antidoto")
                                    }
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    ConsumableButton("Faca", hudState.inventory["kunai"] ?: 0) {
                                        viewModel.useConsumable("kunai")
                                    }
                                    ConsumableButton("Rapad.", hudState.inventory["rapadura"] ?: 0) {
                                        viewModel.useConsumable("rapadura")
                                    }
                                }
                            }
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                ConsumableButton("Chá", hudState.inventory["pocao"] ?: 0) {
                                    viewModel.useConsumable("pocao")
                                }
                                ConsumableButton("Folha", hudState.inventory["antidoto"] ?: 0) {
                                    viewModel.useConsumable("antidoto")
                                }
                                ConsumableButton("Faca", hudState.inventory["kunai"] ?: 0) {
                                    viewModel.useConsumable("kunai")
                                }
                                ConsumableButton("Rapad.", hudState.inventory["rapadura"] ?: 0) {
                                    viewModel.useConsumable("rapadura")
                                }
                            }
                            VirtualJoystick(
                                padSizeDp = 132,
                                onMove = { x, y -> viewModel.setJoystickInput(x, y) }
                            )
                        }
                    }
                }

                // Bottom-Right: Naruto Storm Action Buttons (Ataque, Pulo, Técnica, Esquiva via IconButton)
                CombatActionArc(
                    hudState = hudState,
                    compact = isCompactHeight,
                    onLightAttack = { viewModel.triggerLightAttack() },
                    onHeavyAttack = { viewModel.triggerHeavyAttack() },
                    onBlockChange = { viewModel.setBlocking(it) },
                    onChargeEnergyChange = { viewModel.setChargingEnergy(it) },
                    onDodge = { viewModel.triggerDodge() },
                    onJump = { viewModel.triggerJump() },
                    onSkill1 = { viewModel.triggerSkill(1) },
                    onSkill2 = { viewModel.triggerSkill(2) },
                    onUltimate = { viewModel.triggerUltimate() },
                    onInteract = { viewModel.interactWithWorld() },
                    modifier = Modifier.align(Alignment.BottomEnd)
                )
            }

            // Bottom-Center: Dialogue Box with Small Portrait + Voice + Choices
            hudState.activeDialogue?.let { dlg ->
                DialogueBoxOverlay(
                    dialogueState = dlg,
                    subtitlesEnabled = settings.subtitlesEnabled,
                    onReplayVoice = { viewModel.replayCurrentVoice() },
                    onNext = { viewModel.advanceDialogue(null) },
                    onChoice = { ch -> viewModel.advanceDialogue(ch) },
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }
        }

        // 3. Pause Overlay
        if (hudState.isPaused) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(SertaoObsidian.copy(alpha = 0.85f)),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.widthIn(max = 360.dp).padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(22.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("Jogo Pausado", style = MaterialTheme.typography.headlineMedium, color = SolarGold)
                        Text("${hudState.currentRegion.name} • Cap. ${hudState.currentChapter}", style = MaterialTheme.typography.bodyMedium)
                        Button(
                            onClick = { viewModel.setPaused(false) },
                            modifier = Modifier.fillMaxWidth().height(48.dp)
                        ) { Text("Continuar") }
                        OutlinedButton(
                            onClick = {
                                viewModel.setPaused(false)
                                activeOverlay = RpgOverlayTab.SALVAMENTO
                            },
                            modifier = Modifier.fillMaxWidth().height(48.dp)
                        ) { Text("Salvar Progresso") }
                        OutlinedButton(
                            onClick = {
                                viewModel.triggerAutoSave()
                                viewModel.setPaused(false)
                                onExitToMenu()
                            },
                            modifier = Modifier.fillMaxWidth().height(48.dp)
                        ) { Text("Salvar e Voltar ao Menu") }
                    }
                }
            }
        }

        // 4. Game Over Overlay
        if (hudState.isGameOver) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xCC1A0404)),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.widthIn(max = 380.dp).padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("Você Tombou no Sertão", style = MaterialTheme.typography.headlineMedium, color = BossCrimson)
                        Text(
                            text = "A chama dos Guardiões ainda queima. Levante-se e enfrente ${hudState.currentRegion.bossName} novamente!",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Button(
                            onClick = { viewModel.retryCurrentRegion() },
                            modifier = Modifier.fillMaxWidth().height(48.dp)
                        ) { Text("Tentar Novamente") }
                        OutlinedButton(
                            onClick = onExitToMenu,
                            modifier = Modifier.fillMaxWidth().height(48.dp)
                        ) { Text("Menu Inicial") }
                    }
                }
            }
        }

        // 5. RPG Modals (Loja, Habilidades, Diário, Salvamento)
        activeOverlay?.let { tab ->
            RpgMenuOverlay(
                initialTab = tab,
                hudState = hudState,
                saveSlotsState = saveSlotsState,
                onBuyItem = { viewModel.buyShopItem(it) },
                onUseItem = { viewModel.useConsumable(it) },
                onUpgradeWeapon = { viewModel.upgradeWeapon() },
                onInvestAttr = { viewModel.investAttribute(it) },
                onUnlockSkill = { viewModel.unlockSkill(it) },
                onSaveSlot = { id, lbl -> viewModel.saveToSlot(id, lbl) },
                onLoadSlot = { slot ->
                    viewModel.loadFromSaveSlot(slot)
                    activeOverlay = null
                },
                onDeleteSlot = { id -> viewModel.deleteSaveSlot(id) },
                onDismissMessage = { viewModel.setStatusMessage(null) },
                onClose = { activeOverlay = null }
            )
        }
    }
}

@Composable
private fun HeroStatusCard(
    hudState: GameHudState,
    compact: Boolean,
    onCycleStyle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val frameShape = CutCornerShape(topStart = 10.dp, bottomEnd = 10.dp, topEnd = 4.dp, bottomStart = 4.dp)
    val portraitShape = CutCornerShape(8.dp)
    Surface(
        color = SertaoObsidian.copy(alpha = 0.84f),
        shape = frameShape,
        modifier = modifier.border(BorderStroke(2.dp, StormGoldBorder), frameShape)
    ) {
        Row(
            modifier = Modifier.padding(if (compact) 6.dp else 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(if (compact) 44.dp else 54.dp)
                    .clip(portraitShape)
                    .background(Color(0xFF1E140E))
                    .border(BorderStroke(2.5.dp, StormGoldBorder), portraitShape)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_portrait_kauan),
                    contentDescription = hudState.heroName,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${hudState.heroName} • Nv.${hudState.level}",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color(0xFFFFF8E1),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "XP ${hudState.xp}/${hudState.xpToNext}",
                        style = MaterialTheme.typography.labelSmall,
                        color = SolarGold
                    )
                }
                Spacer(modifier = Modifier.height(3.dp))
                StormBevelBar(
                    progress = (hudState.hp / hudState.maxHp).coerceIn(0f, 1f),
                    fillBrush = Brush.horizontalGradient(listOf(Color(0xFF2E7D32), Color(0xFF76FF03))),
                    heightDp = if (compact) 8 else 10
                )
                Spacer(modifier = Modifier.height(2.dp))
                StormBevelBar(
                    progress = (hudState.energy / hudState.maxEnergy).coerceIn(0f, 1f),
                    fillBrush = Brush.horizontalGradient(listOf(Color(0xFF0277BD), Color(0xFF29B6F6))),
                    heightDp = 6
                )
                Spacer(modifier = Modifier.height(2.dp))
                StormBevelBar(
                    progress = (hudState.stamina / hudState.maxStamina).coerceIn(0f, 1f),
                    fillBrush = Brush.horizontalGradient(listOf(Color(0xFFEF6C00), Color(0xFFFFCA28))),
                    heightDp = 4
                )
                Spacer(modifier = Modifier.height(3.dp))
                Surface(
                    color = Color(hudState.activeStyle.colorHex).copy(alpha = 0.25f),
                    shape = CutCornerShape(6.dp),
                    modifier = Modifier
                        .clickable { onCycleStyle() }
                        .testTag("hud_style_switch")
                ) {
                    Text(
                        text = "${hudState.activeStyle.displayName} ↻",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(hudState.activeStyle.colorHex),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun StormBevelBar(
    progress: Float,
    fillBrush: Brush,
    heightDp: Int
) {
    val barShape = CutCornerShape(topEnd = 5.dp, bottomStart = 5.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(heightDp.dp)
            .clip(barShape)
            .background(Color(0xCC18120E))
            .border(BorderStroke(1.2.dp, StormGoldBorder), barShape)
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(progress.coerceIn(0f, 1f))
                .background(fillBrush)
        )
    }
}

@Composable
private fun StormComboCounter(
    comboCount: Int,
    modifier: Modifier = Modifier
) {
    val popScale by animateFloatAsState(
        targetValue = if (comboCount >= 2) 1.0f else 0.72f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "stormComboScale"
    )
    AnimatedVisibility(
        visible = comboCount >= 2,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .scale(popScale)
                .testTag("hud_combo_counter"),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = "$comboCount",
                style = TextStyle(
                    fontSize = 38.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFFFFB300),
                    shadow = Shadow(
                        color = Color(0xFF3E1900),
                        offset = Offset(2f, 3f),
                        blurRadius = 4f
                    )
                )
            )
            Surface(
                color = Color(0xDD231811),
                shape = CutCornerShape(6.dp),
                border = BorderStroke(1.5.dp, StormGoldBorder)
            ) {
                Text(
                    text = "GOLPES!",
                    color = Color(0xFFFFF8E1),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }
        }
    }
}

@Composable
private fun MinimapRadar(blips: List<MinimapBlip>, sizeDp: Int = 72) {
    Box(
        modifier = Modifier
            .size(sizeDp.dp)
            .clip(CircleShape)
            .background(SertaoObsidian.copy(alpha = 0.75f))
            .border(1.5.dp, SolarGold.copy(alpha = 0.7f), CircleShape)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val c = Offset(size.width / 2f, size.height / 2f)
            val rad = size.minDimension / 2f - 4f
            drawCircle(Color.White.copy(alpha = 0.14f), radius = rad * 0.5f, center = c)
            // Player at center
            drawCircle(Color.White, radius = 4f, center = c)
            blips.forEach { b ->
                val bx = (c.x + b.relX.coerceIn(-1f, 1f) * rad)
                val by = (c.y + b.relZ.coerceIn(-1f, 1f) * rad)
                val color = when {
                    b.isNpc -> Color(0xFF38BDF8)
                    b.isPortal -> SolarGold
                    b.isBoss -> BossCrimson
                    else -> Color(0xFFFF7043)
                }
                drawCircle(color, radius = if (b.isBoss) 5.5f else 3.5f, center = Offset(bx, by))
            }
        }
    }
}

@Composable
private fun QuestTrackerChip(
    hudState: GameHudState,
    modifier: Modifier = Modifier
) {
    Surface(
        color = SertaoObsidian.copy(alpha = 0.72f),
        shape = RoundedCornerShape(10.dp),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)) {
            Text(
                text = hudState.currentRegion.name,
                style = MaterialTheme.typography.labelSmall,
                color = SolarGold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = if (hudState.portalUnlocked) "Vá ao Portal Dourado avançar!" else hudState.currentRegion.mainQuestObjective,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun BossHealthPanel(boss: BossHudInfo) {
    val bossShape = CutCornerShape(topStart = 8.dp, topEnd = 8.dp, bottomEnd = 12.dp, bottomStart = 12.dp)
    Surface(
        color = SertaoObsidian.copy(alpha = 0.86f),
        shape = bossShape,
        modifier = Modifier
            .widthIn(max = 330.dp)
            .border(BorderStroke(2.dp, StormGoldBorder), bossShape)
    ) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("◆ ${boss.name} (Fase ${boss.phase})", style = MaterialTheme.typography.labelLarge, color = Color(0xFFFFF8E1))
                Text(boss.title, style = MaterialTheme.typography.labelSmall, color = SolarGold)
            }
            Spacer(modifier = Modifier.height(4.dp))
            StormBevelBar(
                progress = (boss.hp / boss.maxHp).coerceIn(0f, 1f),
                fillBrush = Brush.horizontalGradient(listOf(Color(0xFF991B1B), Color(0xFFEF4444))),
                heightDp = 9
            )
            Spacer(modifier = Modifier.height(3.dp))
            StormBevelBar(
                progress = (boss.posture / boss.maxPosture).coerceIn(0f, 1f),
                fillBrush = Brush.horizontalGradient(listOf(Color(0xFFD97706), Color(0xFFFACC15))),
                heightDp = 5
            )
        }
    }
}

@Composable
private fun HudMiniIconBtn(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    desc: String,
    tag: String,
    highlighted: Boolean = false,
    onClick: () -> Unit
) {
    Surface(
        color = if (highlighted) EmberOrange else SertaoObsidian.copy(alpha = 0.78f),
        shape = CircleShape,
        modifier = Modifier
            .size(48.dp)
            .clickable { onClick() }
            .testTag(tag)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = desc, tint = Color.White, modifier = Modifier.size(22.dp))
        }
    }
}

@Composable
private fun ConsumableButton(label: String, count: Int, onClick: () -> Unit) {
    Surface(
        color = SertaoObsidian.copy(alpha = 0.80f),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier
            .size(width = 50.dp, height = 48.dp)
            .border(1.dp, SolarGold.copy(alpha = if (count > 0) 0.7f else 0.25f), RoundedCornerShape(10.dp))
            .clickable(enabled = count > 0) { onClick() }
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = if (count > 0) Color.White else Color.Gray, fontSize = 10.sp)
            Text("x$count", style = MaterialTheme.typography.labelSmall, color = SolarGold, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun VirtualJoystick(
    padSizeDp: Int = 120,
    onMove: (Float, Float) -> Unit
) {
    var knobOffset by remember { mutableStateOf(Offset.Zero) }
    var isActive by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .size(padSizeDp.dp)
            .clip(CircleShape)
            .background(SertaoObsidian.copy(alpha = if (isActive) 0.62f else 0.42f))
            .border(
                width = 2.dp,
                color = if (hypot(knobOffset.x, knobOffset.y) > 40f) SolarGold else Color.White.copy(alpha = 0.38f),
                shape = CircleShape
            )
            .pointerInput(Unit) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val maxRadiusPx = (size.width / 2f) * 0.74f

                fun updateFromPointer(pos: Offset) {
                    val delta = pos - center
                    val dist = hypot(delta.x, delta.y)
                    val clamped = if (dist > maxRadiusPx && dist > 0.001f) {
                        Offset(delta.x / dist * maxRadiusPx, delta.y / dist * maxRadiusPx)
                    } else {
                        delta
                    }
                    knobOffset = clamped
                    onMove(clamped.x / maxRadiusPx, clamped.y / maxRadiusPx)
                }

                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    down.consume()
                    isActive = true
                    updateFromPointer(down.position)
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id } ?: event.changes.firstOrNull()
                        if (change == null || !change.pressed) {
                            break
                        }
                        change.consume()
                        updateFromPointer(change.position)
                    }
                    isActive = false
                    knobOffset = Offset.Zero
                    onMove(0f, 0f)
                }
            }
            .testTag("virtual_joystick"),
        contentAlignment = Alignment.Center
    ) {
        // Inner directional guide ring
        Box(
            modifier = Modifier
                .size((padSizeDp * 0.56f).dp)
                .clip(CircleShape)
                .border(1.dp, Color.White.copy(alpha = 0.16f), CircleShape)
        )
        // Movable thumbstick
        Box(
            modifier = Modifier
                .offset { IntOffset(knobOffset.x.roundToInt(), knobOffset.y.roundToInt()) }
                .size((padSizeDp * 0.42f).dp)
                .clip(CircleShape)
                .background(if (isActive) EmberOrange else EmberOrange.copy(alpha = 0.80f))
                .border(2.dp, SolarGold, CircleShape)
        )
    }
}

@Composable
private fun CombatActionArc(
    hudState: GameHudState,
    compact: Boolean,
    onLightAttack: () -> Unit,
    onHeavyAttack: () -> Unit,
    onBlockChange: (Boolean) -> Unit,
    onChargeEnergyChange: (Boolean) -> Unit,
    onDodge: () -> Unit,
    onJump: () -> Unit,
    onSkill1: () -> Unit,
    onSkill2: () -> Unit,
    onUltimate: () -> Unit,
    onInteract: () -> Unit,
    modifier: Modifier = Modifier
) {
    val styleColor = Color(hudState.activeStyle.colorHex)
    val gap = if (compact) 6.dp else 8.dp
    val mainAtkSize = if (compact) 66 else 74
    val diamondBtnSize = if (compact) 52 else 58
    val miniBtnSize = if (compact) 48 else 52

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(gap)
    ) {
        if (hudState.isNearNpc || (hudState.isNearPortal && hudState.portalUnlocked)) {
            Button(
                onClick = onInteract,
                colors = ButtonDefaults.buttonColors(containerColor = SolarGold, contentColor = SertaoObsidian),
                modifier = Modifier
                    .height(48.dp)
                    .testTag("hud_interact_button")
            ) {
                Text(
                    text = if (hudState.isNearNpc) "Conversar (${hudState.currentRegion.npcName})" else "Avançar Região!",
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }

        // Secondary Utility Strip (Energia hold, Defesa/Parry hold, Forte, Hab 2, SUPR)
        Row(
            horizontalArrangement = Arrangement.spacedBy(gap),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Segurar para Carregar Energia
            Box(
                modifier = Modifier
                    .size(miniBtnSize.dp)
                    .clip(CircleShape)
                    .background(if (hudState.isChargingEnergy) SolarGold else SertaoObsidian.copy(alpha = 0.84f))
                    .border(1.5.dp, EnergyAmber, CircleShape)
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onPress = {
                                onChargeEnergyChange(true)
                                tryAwaitRelease()
                                onChargeEnergyChange(false)
                            }
                        )
                    }
                    .testTag("btn_charge_energy"),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Filled.Bolt,
                        contentDescription = "Carregar Energia",
                        tint = if (hudState.isChargingEnergy) SertaoObsidian else EnergyAmber,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Energia",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (hudState.isChargingEnergy) SertaoObsidian else Color.White,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Segurar para Defender / Parry
            Box(
                modifier = Modifier
                    .size(miniBtnSize.dp)
                    .clip(CircleShape)
                    .background(if (hudState.isBlocking) SolarGold else Color(0xFF475569).copy(alpha = 0.90f))
                    .border(1.5.dp, Color.White.copy(alpha = 0.72f), CircleShape)
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onPress = {
                                onBlockChange(true)
                                tryAwaitRelease()
                                onBlockChange(false)
                            }
                        )
                    }
                    .testTag("btn_block_parry"),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Filled.Shield,
                        contentDescription = "Defender / Parry",
                        tint = if (hudState.isBlocking) SertaoObsidian else Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Defesa",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (hudState.isBlocking) SertaoObsidian else Color.White,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            ActionCircleButton(
                label = "Forte",
                bg = BossCrimson.copy(alpha = 0.88f),
                sizeDp = miniBtnSize,
                tag = "btn_heavy_attack",
                onClick = onHeavyAttack
            )
            ActionCircleButton(
                label = if (hudState.skill2Cooldown > 0f) "${hudState.skill2Cooldown.toInt()}s" else "Hab 2",
                bg = styleColor.copy(alpha = 0.85f),
                sizeDp = miniBtnSize,
                tag = "btn_skill_2",
                onClick = onSkill2
            )
            ActionCircleButton(
                label = if (hudState.ultCooldown > 0f) "${hudState.ultCooldown.toInt()}s" else "SUPR",
                bg = SolarGold.copy(alpha = 0.92f),
                textColor = SertaoObsidian,
                sizeDp = miniBtnSize,
                tag = "btn_ultimate",
                onClick = onUltimate
            )
        }

        // Naruto Storm 4-Button Layout using IconButton + Custom Icons:
        // Top-Left: Técnica   | Top-Right: Esquiva
        // Bottom-Left: Pulo   | Bottom-Right: Ataque (Primary)
        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(gap)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(gap),
                verticalAlignment = Alignment.CenterVertically
            ) {
                StormIconActionButton(
                    iconRes = R.drawable.ic_action_technique,
                    label = if (hudState.skill1Cooldown > 0f) "${hudState.skill1Cooldown.toInt()}s" else "Técnica",
                    contentDesc = "Técnica",
                    bg = styleColor.copy(alpha = 0.90f),
                    borderColor = Color.White.copy(alpha = 0.82f),
                    sizeDp = diamondBtnSize,
                    iconSizeDp = 24,
                    tag = "btn_skill_1",
                    onClick = onSkill1
                )

                StormIconActionButton(
                    iconRes = R.drawable.ic_action_dodge,
                    label = "Esquiva",
                    contentDesc = "Esquiva",
                    bg = StaminaCyan.copy(alpha = 0.88f),
                    borderColor = Color.White.copy(alpha = 0.82f),
                    sizeDp = diamondBtnSize,
                    iconSizeDp = 24,
                    tag = "btn_dodge",
                    onClick = onDodge
                )
            }

            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(gap)
            ) {
                StormIconActionButton(
                    iconRes = R.drawable.ic_action_jump,
                    label = "Pulo",
                    contentDesc = "Pulo",
                    bg = SertaoObsidian.copy(alpha = 0.88f),
                    borderColor = SolarGold.copy(alpha = 0.85f),
                    sizeDp = diamondBtnSize,
                    iconSizeDp = 24,
                    tag = "btn_jump",
                    onClick = onJump
                )

                StormIconActionButton(
                    iconRes = R.drawable.ic_action_attack,
                    label = if (hudState.comboStep > 0) "Combo ${hudState.comboStep}" else "Ataque",
                    contentDesc = "Ataque",
                    bg = EmberOrange,
                    borderColor = SolarGold,
                    sizeDp = mainAtkSize,
                    iconSizeDp = if (compact) 28 else 32,
                    borderWidthDp = 3f,
                    tag = "btn_light_attack",
                    onClick = onLightAttack
                )
            }
        }
    }
}

@Composable
private fun StormIconActionButton(
    iconRes: Int,
    label: String,
    contentDesc: String,
    bg: Color,
    borderColor: Color,
    sizeDp: Int,
    iconSizeDp: Int,
    tag: String,
    borderWidthDp: Float = 2f,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(sizeDp.dp)
            .clip(CircleShape)
            .background(bg)
            .border(borderWidthDp.dp, borderColor, CircleShape)
            .testTag(tag)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Image(
                painter = painterResource(id = iconRes),
                contentDescription = contentDesc,
                modifier = Modifier.size(iconSizeDp.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun ActionCircleButton(
    label: String,
    bg: Color,
    textColor: Color = Color.White,
    sizeDp: Int,
    tag: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(sizeDp.dp)
            .clip(CircleShape)
            .background(bg)
            .border(1.5.dp, Color.White.copy(alpha = 0.65f), CircleShape)
            .clickable { onClick() }
            .testTag(tag),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = textColor,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun DialogueBoxOverlay(
    dialogueState: ActiveDialogueState,
    subtitlesEnabled: Boolean,
    onReplayVoice: () -> Unit,
    onNext: () -> Unit,
    onChoice: (DialogueChoice) -> Unit,
    modifier: Modifier = Modifier
) {
    val line = dialogueState.currentLine ?: return
    var shownChars by remember(dialogueState.index, line.text) { mutableStateOf(0) }

    LaunchedEffect(dialogueState.index, line.text) {
        shownChars = 0
        val total = line.text.length
        while (shownChars < total) {
            delay(18L)
            shownChars = (shownChars + 2).coerceAtMost(total)
        }
    }

    val portraitShape = CutCornerShape(8.dp)
    Box(
        modifier = modifier
            .widthIn(max = 680.dp)
            .fillMaxWidth(0.86f)
            .clickable {
                if (shownChars < line.text.length) {
                    shownChars = line.text.length
                } else if (line.choices.isEmpty()) {
                    onNext()
                }
            }
            .testTag("dialogue_box")
    ) {
        // Caixa de pergaminho estilo Naruto Storm
        Column(
            modifier = Modifier
                .padding(top = 15.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(StormParchmentBg)
                .border(BorderStroke(2.2.dp, StormGoldBorder), RoundedCornerShape(10.dp))
                .padding(start = 94.dp, end = 14.dp, top = 20.dp, bottom = 12.dp)
        ) {
            if (subtitlesEnabled) {
                Text(
                    text = line.text.take(shownChars),
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color(0xFF2B1B10),
                    fontSize = 14.sp,
                    lineHeight = 19.sp
                )
            }

            if (line.choices.isNotEmpty() && shownChars >= line.text.length) {
                Spacer(modifier = Modifier.height(8.dp))
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    line.choices.forEach { ch ->
                        OutlinedButton(
                            onClick = { onChoice(ch) },
                            border = BorderStroke(1.5.dp, Color(0xFF8C5E14)),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = Color(0xFFFFF8E8),
                                contentColor = Color(0xFF2B1B10)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(ch.label, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            } else if (line.choices.isEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Toque para continuar ▶",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF8C5E14),
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.End)
                )
            }
        }

        // Barra superior escura com emblema, nome, humor e botão de voz PT-BR
        Surface(
            color = Color(0xFF231811),
            shape = CutCornerShape(6.dp),
            border = BorderStroke(1.5.dp, StormGoldBorder),
            modifier = Modifier.padding(start = 86.dp, end = 12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(start = 12.dp, top = 3.dp, end = 6.dp, bottom = 3.dp)
            ) {
                Text(
                    text = "◆ ${line.speaker.displayName}",
                    color = Color(0xFFFFE082),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Surface(
                    color = EmberOrange.copy(alpha = 0.28f),
                    shape = CutCornerShape(4.dp)
                ) {
                    Text(
                        text = line.expression,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFFFB74D),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                    )
                }
                IconButton(
                    onClick = onReplayVoice,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                        contentDescription = "Ouvir dublagem",
                        tint = Color(0xFFFFE082),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // Retrato pequeno moldurado ao lado da fala
        Box(
            modifier = Modifier
                .padding(top = 20.dp, start = 12.dp)
                .size(72.dp)
                .clip(portraitShape)
                .background(Color(0xFF231811))
                .border(BorderStroke(2.2.dp, StormGoldBorder), portraitShape)
        ) {
            Image(
                painter = painterResource(id = line.speaker.portraitRes),
                contentDescription = line.speaker.displayName,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
