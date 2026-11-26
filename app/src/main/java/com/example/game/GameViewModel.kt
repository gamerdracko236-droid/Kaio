package com.example.game

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.SertaoAudioEngine
import com.example.audio.SfxType
import com.example.data.firebase.SaveStorage
import com.example.data.model.BladeColor
import com.example.data.model.CombatStyle
import com.example.data.model.DialogueChoice
import com.example.data.model.DialogueLine
import com.example.data.model.GameSaveSlot
import com.example.data.model.HatStyle
import com.example.data.model.OutfitStyle
import com.example.data.model.RegionDefinition
import com.example.data.model.UiState
import com.example.data.model.WorldCatalog
import com.example.data.settings.GameSettings
import com.example.data.settings.SettingsRepository
import com.example.engine3d.EnemyRenderEntity
import com.example.engine3d.EnemyVisualKind
import com.example.engine3d.GameRenderer
import com.example.engine3d.VfxRenderBurst
import com.example.engine3d.WorldRenderSnapshot
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

enum class EnemyAiState {
    PATROL, CHASE, WINDUP, ATTACK, RECOVER, STUNNED, DEAD
}

data class SimEnemy(
    val id: String,
    val name: String,
    val kind: EnemyVisualKind,
    var x: Float,
    var z: Float,
    var yawRad: Float,
    var hp: Float,
    val maxHp: Float,
    var posture: Float,
    val maxPosture: Float,
    val damage: Float,
    val xpReward: Int,
    val coinReward: Int,
    var aiState: EnemyAiState = EnemyAiState.PATROL,
    var stateTimer: Float = 1.0f,
    var hitFlashTimer: Float = 0f,
    var phase: Int = 1,
    val spawnX: Float = x,
    val spawnZ: Float = z
)

data class SimVfx(
    val x: Float,
    val y: Float,
    val z: Float,
    val maxRadius: Float,
    val rgb: Triple<Float, Float, Float>,
    var age: Float = 0f,
    val duration: Float = 0.45f
)

data class BossHudInfo(
    val name: String,
    val title: String,
    val hp: Float,
    val maxHp: Float,
    val posture: Float,
    val maxPosture: Float,
    val phase: Int
)

data class MinimapBlip(
    val relX: Float,
    val relZ: Float,
    val isBoss: Boolean,
    val isNpc: Boolean,
    val isPortal: Boolean
)

data class ActiveDialogueState(
    val lines: List<DialogueLine>,
    val index: Int = 0,
    val onFinishAction: String = ""
) {
    val currentLine: DialogueLine? get() = lines.getOrNull(index)
}

data class GameHudState(
    val heroName: String = "Kauan",
    val hatStyle: HatStyle = HatStyle.VAQUEIRO,
    val outfitStyle: OutfitStyle = OutfitStyle.GIBAO_COURO,
    val bladeColor: BladeColor = BladeColor.BRASA_VIVA,
    val activeStyle: CombatStyle = CombatStyle.CHAMA,
    val level: Int = 1,
    val xp: Int = 0,
    val xpToNext: Int = 120,
    val coins: Int = 60,
    val skillPoints: Int = 1,
    val attackBonus: Int = 0,
    val defenseBonus: Int = 0,
    val energyBonus: Int = 0,
    val weaponUpgrade: Int = 1,
    val currentChapter: Int = 1,
    val currentRegion: RegionDefinition = WorldCatalog.regions.first(),
    val unlockedRegions: List<String> = listOf("vila_das_almas"),
    val completedQuests: List<String> = emptyList(),
    val unlockedSkills: List<String> = listOf("chama_1"),
    val unlockedAchievements: List<String> = emptyList(),
    val inventory: Map<String, Int> = mapOf("pocao" to 3, "antidoto" to 1, "kunai" to 3, "rapadura" to 2),
    val playTimeSeconds: Long = 0L,
    val hp: Float = 160f,
    val maxHp: Float = 160f,
    val energy: Float = 100f,
    val maxEnergy: Float = 100f,
    val stamina: Float = 100f,
    val maxStamina: Float = 100f,
    val skill1Cooldown: Float = 0f,
    val skill2Cooldown: Float = 0f,
    val ultCooldown: Float = 0f,
    val comboStep: Int = 0,
    val comboHits: Int = 0,
    val isBlocking: Boolean = false,
    val isChargingEnergy: Boolean = false,
    val isLockedOn: Boolean = false,
    val isNearNpc: Boolean = false,
    val isNearPortal: Boolean = false,
    val portalUnlocked: Boolean = false,
    val bossHud: BossHudInfo? = null,
    val activeDialogue: ActiveDialogueState? = null,
    val combatBanner: String? = null,
    val statusMessage: String? = null,
    val isPaused: Boolean = false,
    val isGameOver: Boolean = false,
    val campaignCompleted: Boolean = false,
    val minimapBlips: List<MinimapBlip> = emptyList()
)

class GameViewModel(
    private val saveRepository: SaveStorage,
    private val settingsRepository: SettingsRepository,
    private val audioEngine: SertaoAudioEngine,
    private val currentUserId: String
) : ViewModel() {

    val renderer: GameRenderer = GameRenderer()

    val saveSlotsState: StateFlow<UiState<List<GameSaveSlot>>> = saveRepository
        .observeSaveSlots(currentUserId)
        .map<List<GameSaveSlot>, UiState<List<GameSaveSlot>>> { UiState.Success(it) }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val rootError = firestoreError ?: error
            Log.w(TAG, "Error observing save slots", rootError)
            emit(UiState.Error(rootError.message ?: "Erro ao carregar salvamentos na nuvem."))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = UiState.Loading
        )

    val settingsState: StateFlow<GameSettings> = settingsRepository.settingsFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = GameSettings()
        )

    private val _hudState = MutableStateFlow(GameHudState())
    val hudState: StateFlow<GameHudState> = _hudState.asStateFlow()

    // Live simulation physics state
    private var playerX = 0f
    private var playerY = 0f
    private var playerZ = 8f
    private var playerVy = 0f
    private var playerYaw = 0f
    private var joyX = 0f
    private var joyY = 0f
    private var isSprinting = false
    private var isDodging = false
    private var dodgeTimer = 0f
    private var attackTimer = 0f
    private var parryWindowTimer = 0f
    private var comboResetTimer = 0f
    private var comboHitsTimer = 0f
    private var parryCount = 0
    private var bannerTimer = 0f
    private var chargeVfxTimer = 0f
    private var lockedEnemyId: String? = null
    private var secAccumulator = 0f
    private var regionBossDefeated = false

    private val simEnemies = mutableListOf<SimEnemy>()
    private val simVfx = mutableListOf<SimVfx>()
    private var loopJob: Job? = null

    init {
        viewModelScope.launch {
            settingsState.collect { s ->
                audioEngine.updateVolumes(s.musicVolume, s.sfxVolume, s.voiceVolume)
            }
        }
        audioEngine.startMusic("menu")
    }

    fun startNewCampaign() {
        val fresh = GameHudState()
        _hudState.value = fresh
        loadRegion(WorldCatalog.regions.first(), showIntro = true)
        triggerAutoSave()
    }

    fun loadFromSaveSlot(slot: GameSaveSlot) {
        val region = WorldCatalog.regionById(slot.currentRegionId)
        val level = slot.level.toInt().coerceIn(1, 99)
        val maxHp = calcMaxHp(level, slot.defenseBonus.toInt())
        val maxEnergy = calcMaxEnergy(slot.energyBonus.toInt())

        _hudState.value = GameHudState(
            heroName = slot.heroName.ifBlank { "Kauan" },
            hatStyle = HatStyle.fromRaw(slot.hatStyle),
            outfitStyle = OutfitStyle.fromRaw(slot.outfitStyle),
            bladeColor = BladeColor.fromRaw(slot.bladeColor),
            activeStyle = CombatStyle.fromRaw(slot.activeStyle),
            level = level,
            xp = slot.xp.toInt(),
            xpToNext = level * 120,
            coins = slot.coins.toInt(),
            skillPoints = slot.skillPoints.toInt(),
            attackBonus = slot.attackBonus.toInt(),
            defenseBonus = slot.defenseBonus.toInt(),
            energyBonus = slot.energyBonus.toInt(),
            weaponUpgrade = slot.weaponUpgrade.toInt().coerceIn(1, 10),
            currentChapter = slot.currentChapter.toInt().coerceIn(1, 20),
            currentRegion = region,
            unlockedRegions = slot.unlockedRegions.ifEmpty { listOf("vila_das_almas") },
            completedQuests = slot.completedQuests,
            unlockedSkills = slot.unlockedSkills.ifEmpty { listOf("chama_1") },
            unlockedAchievements = slot.unlockedAchievements,
            inventory = slot.inventoryMap().ifEmpty { mapOf("pocao" to 3) },
            playTimeSeconds = slot.playTimeSeconds,
            hp = maxHp,
            maxHp = maxHp,
            energy = maxEnergy,
            maxEnergy = maxEnergy
        )
        loadRegion(region, showIntro = false)
    }

    fun startChapter(chapterNumber: Int) {
        val region = WorldCatalog.regionForChapter(chapterNumber)
        _hudState.update {
            it.copy(
                currentChapter = chapterNumber,
                currentRegion = region,
                unlockedRegions = (it.unlockedRegions + region.id).distinct(),
                isGameOver = false,
                campaignCompleted = false
            )
        }
        loadRegion(region, showIntro = true)
    }

    fun loadRegion(region: RegionDefinition, showIntro: Boolean) {
        playerX = 0f
        playerY = 0f
        playerZ = 10f
        playerVy = 0f
        playerYaw = 0f
        lockedEnemyId = null
        comboHitsTimer = 0f
        regionBossDefeated = _hudState.value.completedQuests.contains(region.mainQuestId)

        simEnemies.clear()
        simVfx.clear()

        val scaleFactor = 1f + (region.index - 1) * 0.28f
        if (!regionBossDefeated) {
            simEnemies += SimEnemy(
                id = "e_common_1",
                name = "Encantado da Sombra",
                kind = EnemyVisualKind.COMMON,
                x = -5f, z = -3f, yawRad = 0f,
                hp = 75f * scaleFactor, maxHp = 75f * scaleFactor,
                posture = 0f, maxPosture = 50f,
                damage = 12f * scaleFactor,
                xpReward = 40, coinReward = 15
            )
            simEnemies += SimEnemy(
                id = "e_fast_1",
                name = if (region.index >= 4) "Espectro das Sombras" else "Fera Ligeira",
                kind = if (region.index >= 6) EnemyVisualKind.FLYING else EnemyVisualKind.FAST,
                x = 6f, z = -9f, yawRad = 0f,
                hp = 68f * scaleFactor, maxHp = 68f * scaleFactor,
                posture = 0f, maxPosture = 42f,
                damage = 14f * scaleFactor,
                xpReward = 50, coinReward = 20
            )
            if (region.index >= 3) {
                simEnemies += SimEnemy(
                    id = "e_tank_1",
                    name = "Colosso de Pedra",
                    kind = EnemyVisualKind.TANK,
                    x = -3f, z = -14f, yawRad = 0f,
                    hp = 125f * scaleFactor, maxHp = 125f * scaleFactor,
                    posture = 0f, maxPosture = 80f,
                    damage = 18f * scaleFactor,
                    xpReward = 70, coinReward = 28
                )
            }
            simEnemies += SimEnemy(
                id = "boss_${region.index}",
                name = region.bossName,
                kind = EnemyVisualKind.BOSS,
                x = 0f, z = -20f, yawRad = 0f,
                hp = region.bossMaxHp, maxHp = region.bossMaxHp,
                posture = 0f, maxPosture = 110f + region.index * 15f,
                damage = 20f * scaleFactor,
                xpReward = 180 + region.index * 45,
                coinReward = 80 + region.index * 20
            )
        }

        val maxHp = calcMaxHp(_hudState.value.level, _hudState.value.defenseBonus)
        val maxEn = calcMaxEnergy(_hudState.value.energyBonus)
        _hudState.update {
            it.copy(
                currentRegion = region,
                currentChapter = region.chapterRange.first,
                hp = maxHp,
                maxHp = maxHp,
                energy = maxEn,
                maxEnergy = maxEn,
                stamina = 100f,
                portalUnlocked = regionBossDefeated,
                comboStep = 0,
                comboHits = 0,
                isPaused = false,
                isGameOver = false,
                bossHud = null
            )
        }

        audioEngine.startMusic(region.musicTheme)
        if (showIntro && region.introDialogue.isNotEmpty()) {
            startDialogue(region.introDialogue)
        }
        startSimulationLoop()
    }

    fun enterMenuMusic() {
        loopJob?.cancel()
        audioEngine.stopVoice()
        audioEngine.startMusic("menu")
    }

    fun enterCreditsMusic() {
        loopJob?.cancel()
        audioEngine.stopVoice()
        audioEngine.startMusic("creditos")
    }

    private fun startSimulationLoop() {
        if (loopJob?.isActive == true) return
        loopJob = viewModelScope.launch {
            var lastTime = System.nanoTime()
            while (isActive) {
                delay(16L)
                val now = System.nanoTime()
                val dt = ((now - lastTime) / 1_000_000_000f).coerceIn(0.005f, 0.05f)
                lastTime = now
                tickSimulation(dt)
            }
        }
    }

    private fun tickSimulation(dt: Float) {
        val state = _hudState.value
        if (state.isPaused || state.isGameOver || state.activeDialogue != null || state.campaignCompleted) {
            pushRenderSnapshot()
            return
        }

        secAccumulator += dt
        var newPlayTime = state.playTimeSeconds
        if (secAccumulator >= 1f) {
            secAccumulator -= 1f
            newPlayTime += 1L
        }

        // Timers
        if (parryWindowTimer > 0f) parryWindowTimer -= dt
        if (attackTimer > 0f) attackTimer = (attackTimer - dt).coerceAtLeast(0f)
        if (comboResetTimer > 0f) {
            comboResetTimer -= dt
            if (comboResetTimer <= 0f) {
                _hudState.update { it.copy(comboStep = 0) }
            }
        }
        if (comboHitsTimer > 0f) {
            comboHitsTimer -= dt
            if (comboHitsTimer <= 0f) {
                _hudState.update { it.copy(comboHits = 0) }
            }
        }
        if (bannerTimer > 0f) {
            bannerTimer -= dt
            if (bannerTimer <= 0f) {
                _hudState.update { it.copy(combatBanner = null) }
            }
        }

        var stamina = state.stamina
        var energy = state.energy

        // Dodge movement
        var moving = false
        if (isDodging) {
            dodgeTimer -= dt
            val dSpeed = 13.5f
            playerX = (playerX + sin(playerYaw) * dSpeed * dt).coerceIn(-35f, 35f)
            playerZ = (playerZ + cos(playerYaw) * dSpeed * dt).coerceIn(-35f, 35f)
            if (dodgeTimer <= 0f) isDodging = false
        } else {
            val mag = hypot(joyX, joyY)
            if (mag > 0.08f) {
                moving = true
                val camYaw = renderer.cameraYaw
                val fwdX = sin(camYaw)
                val fwdZ = cos(camYaw)
                val rightX = cos(camYaw)
                val rightZ = -sin(camYaw)

                // joyY negative = forward on stick
                val moveX = fwdX * (-joyY) + rightX * joyX
                val moveZ = fwdZ * (-joyY) + rightZ * joyX
                val len = hypot(moveX, moveZ).coerceAtLeast(0.001f)
                val dirX = moveX / len
                val dirZ = moveZ / len

                val speedMult = state.activeStyle.speedMultiplier
                val baseSpeed = if (isSprinting && stamina > 5f) 8.4f else 5.2f
                val finalSpeed = if (state.isBlocking) baseSpeed * 0.45f else baseSpeed * speedMult
                if (isSprinting && stamina > 5f) {
                    stamina = (stamina - 16f * dt).coerceAtLeast(0f)
                }

                playerX = (playerX + dirX * finalSpeed * dt).coerceIn(-35f, 35f)
                playerZ = (playerZ + dirZ * finalSpeed * dt).coerceIn(-35f, 35f)
                playerYaw = atan2(dirX, dirZ)
            }
        }

        // Gravity & Jump
        if (playerY > 0f || playerVy > 0f) {
            playerVy -= 20f * dt
            playerY += playerVy * dt
            if (playerY <= 0f) {
                playerY = 0f
                playerVy = 0f
            }
        }

        // Regenerate stamina and energy (or rapid charge when holding Técnica/Carregar)
        if (!state.isBlocking && !isDodging) {
            stamina = (stamina + 26f * dt).coerceAtMost(state.maxStamina)
        }
        if (state.isChargingEnergy && !isDodging) {
            energy = (energy + 44f * dt).coerceAtMost(state.maxEnergy)
            chargeVfxTimer -= dt
            if (chargeVfxTimer <= 0f) {
                chargeVfxTimer = 0.22f
                simVfx += SimVfx(
                    x = playerX,
                    y = playerY + 1.0f,
                    z = playerZ,
                    maxRadius = 1.85f,
                    rgb = hexToTriple(state.activeStyle.colorHex),
                    duration = 0.28f
                )
            }
        } else {
            energy = (energy + 6.5f * dt).coerceAtMost(state.maxEnergy)
        }

        // Update Enemies AI
        var activeBoss: BossHudInfo? = null
        val region = state.currentRegion
        for (enemy in simEnemies) {
            if (enemy.aiState == EnemyAiState.DEAD) continue
            if (enemy.hitFlashTimer > 0f) enemy.hitFlashTimer = (enemy.hitFlashTimer - dt).coerceAtLeast(0f)

            val dx = playerX - enemy.x
            val dz = playerZ - enemy.z
            val dist = hypot(dx, dz)

            if (enemy.kind == EnemyVisualKind.BOSS && dist < 24f) {
                activeBoss = BossHudInfo(
                    name = region.bossName,
                    title = region.bossTitle,
                    hp = enemy.hp,
                    maxHp = enemy.maxHp,
                    posture = enemy.posture,
                    maxPosture = enemy.maxPosture,
                    phase = enemy.phase
                )
            }

            when (enemy.aiState) {
                EnemyAiState.PATROL -> {
                    if (dist < 13f) {
                        enemy.aiState = EnemyAiState.CHASE
                    } else {
                        val angle = atan2(enemy.spawnX - enemy.x, enemy.spawnZ - enemy.z)
                        enemy.stateTimer -= dt
                        if (enemy.stateTimer <= 0f) {
                            enemy.yawRad = angle + (Math.random().toFloat() - 0.5f) * 2.5f
                            enemy.stateTimer = 2.0f
                        }
                        enemy.x += sin(enemy.yawRad) * 1.4f * dt
                        enemy.z += cos(enemy.yawRad) * 1.4f * dt
                    }
                }
                EnemyAiState.CHASE -> {
                    if (dist > 22f && enemy.kind != EnemyVisualKind.BOSS) {
                        enemy.aiState = EnemyAiState.PATROL
                    } else if (dist <= if (enemy.kind == EnemyVisualKind.BOSS) 2.7f else 2.2f) {
                        enemy.aiState = EnemyAiState.WINDUP
                        enemy.stateTimer = if (enemy.phase >= 2) 0.48f else 0.65f
                    } else {
                        val chaseSpeed = when (enemy.kind) {
                            EnemyVisualKind.FAST, EnemyVisualKind.FLYING -> 4.6f
                            EnemyVisualKind.BOSS -> 3.6f + enemy.phase * 0.4f
                            EnemyVisualKind.TANK -> 2.5f
                            EnemyVisualKind.COMMON -> 3.4f
                        }
                        enemy.yawRad = atan2(dx, dz)
                        enemy.x += (dx / dist) * chaseSpeed * dt
                        enemy.z += (dz / dist) * chaseSpeed * dt
                    }
                }
                EnemyAiState.WINDUP -> {
                    enemy.yawRad = atan2(dx, dz)
                    enemy.stateTimer -= dt
                    if (enemy.stateTimer <= 0f) {
                        enemy.aiState = EnemyAiState.ATTACK
                        enemy.stateTimer = 0.20f
                        if (dist <= if (enemy.kind == EnemyVisualKind.BOSS) 3.1f else 2.5f) {
                            applyDamageToPlayer(enemy.damage, enemy)
                        }
                    }
                }
                EnemyAiState.ATTACK -> {
                    enemy.stateTimer -= dt
                    if (enemy.stateTimer <= 0f) {
                        enemy.aiState = EnemyAiState.RECOVER
                        enemy.stateTimer = if (enemy.phase >= 2) 0.75f else 1.10f
                    }
                }
                EnemyAiState.RECOVER, EnemyAiState.STUNNED -> {
                    enemy.stateTimer -= dt
                    if (enemy.stateTimer <= 0f) {
                        enemy.aiState = EnemyAiState.CHASE
                    }
                }
                EnemyAiState.DEAD -> {}
            }
        }

        // Update VFX
        val vfxIter = simVfx.iterator()
        while (vfxIter.hasNext()) {
            val v = vfxIter.next()
            v.age += dt
            if (v.age >= v.duration) vfxIter.remove()
        }

        // Proximity checks & minimap blips
        val npcDist = hypot(playerX - 6f, playerZ - (-6f))
        val portalDist = hypot(playerX - 0f, playerZ - (-24f))
        val blips = buildList {
            add(MinimapBlip((6f - playerX) / 28f, (-6f - playerZ) / 28f, isBoss = false, isNpc = true, isPortal = false))
            add(MinimapBlip((0f - playerX) / 28f, (-24f - playerZ) / 28f, isBoss = false, isNpc = false, isPortal = true))
            simEnemies.filter { it.aiState != EnemyAiState.DEAD }.forEach { e ->
                add(MinimapBlip((e.x - playerX) / 28f, (e.z - playerZ) / 28f, isBoss = e.kind == EnemyVisualKind.BOSS, isNpc = false, isPortal = false))
            }
        }

        _hudState.update {
            it.copy(
                playTimeSeconds = newPlayTime,
                stamina = stamina,
                energy = energy,
                skill1Cooldown = (it.skill1Cooldown - dt).coerceAtLeast(0f),
                skill2Cooldown = (it.skill2Cooldown - dt).coerceAtLeast(0f),
                ultCooldown = (it.ultCooldown - dt).coerceAtLeast(0f),
                isNearNpc = npcDist < 3.4f,
                isNearPortal = portalDist < 3.6f,
                bossHud = activeBoss,
                minimapBlips = blips
            )
        }

        pushRenderSnapshot(isMoving = moving)
    }

    private fun pushRenderSnapshot(isMoving: Boolean = false) {
        val state = _hudState.value
        val lockedEnemy = simEnemies.firstOrNull { it.id == lockedEnemyId && it.aiState != EnemyAiState.DEAD }
        if (lockedEnemy == null && lockedEnemyId != null) {
            lockedEnemyId = null
            _hudState.update { it.copy(isLockedOn = false) }
        }

        val region = state.currentRegion
        renderer.snapshot = WorldRenderSnapshot(
            playerX = playerX,
            playerY = playerY,
            playerZ = playerZ,
            playerYawRad = playerYaw,
            isMoving = isMoving,
            isSprinting = isSprinting && isMoving,
            attackProgress = if (attackTimer > 0f) (1f - attackTimer / 0.28f).coerceIn(0f, 1f) else 0f,
            isBlocking = state.isBlocking,
            isChargingEnergy = state.isChargingEnergy,
            isDialogueActive = state.activeDialogue != null,
            isDodging = isDodging,
            hatStyle = state.hatStyle,
            outfitStyle = state.outfitStyle,
            bladeColor = state.bladeColor,
            activeStyle = state.activeStyle,
            weaponUpgrade = state.weaponUpgrade,
            npcName = region.npcName,
            npcX = 6f,
            npcZ = -6f,
            portalX = 0f,
            portalZ = -24f,
            portalUnlocked = state.portalUnlocked,
            enemies = simEnemies.filter { it.aiState != EnemyAiState.DEAD }.map { e ->
                EnemyRenderEntity(
                    id = e.id,
                    x = e.x,
                    y = 0f,
                    z = e.z,
                    yawRad = e.yawRad,
                    kind = e.kind,
                    isWindup = e.aiState == EnemyAiState.WINDUP,
                    isStunned = e.aiState == EnemyAiState.STUNNED,
                    isHitFlash = e.hitFlashTimer > 0f,
                    isLocked = e.id == lockedEnemyId,
                    hpRatio = (e.hp / e.maxHp).coerceIn(0f, 1f)
                )
            },
            vfxBursts = simVfx.map { v ->
                val p = (v.age / v.duration).coerceIn(0f, 1f)
                VfxRenderBurst(
                    x = v.x,
                    y = v.y,
                    z = v.z,
                    radius = v.maxRadius * (0.35f + p * 0.85f),
                    rgb = v.rgb,
                    alpha = (1f - p) * 0.72f
                )
            },
            skyTopRgb = region.skyTopRgb,
            skyBottomRgb = region.skyBottomRgb,
            groundRgb = region.groundRgb,
            isNight = region.isNight,
            regionSeed = region.index,
            qualityLevel = settingsState.value.qualityLevel,
            lockedTargetPos = lockedEnemy?.let { Triple(it.x, 1.2f, it.z) }
        )
    }

    // --- Controles em Tempo Real ---

    fun setJoystickInput(x: Float, y: Float, sprint: Boolean = false) {
        joyX = x.coerceIn(-1f, 1f)
        joyY = y.coerceIn(-1f, 1f)
        isSprinting = sprint || hypot(joyX, joyY) > 0.88f
    }

    fun triggerJump() {
        if (playerY <= 0.02f) {
            playerVy = 8.8f
            // Arrancada da Chama ao pular em movimento
            if (hypot(joyX, joyY) > 0.25f) {
                playerX = (playerX + sin(playerYaw) * 1.15f).coerceIn(-35f, 35f)
                playerZ = (playerZ + cos(playerYaw) * 1.15f).coerceIn(-35f, 35f)
                simVfx += SimVfx(
                    x = playerX,
                    y = 0.7f,
                    z = playerZ,
                    maxRadius = 1.6f,
                    rgb = hexToTriple(_hudState.value.activeStyle.colorHex),
                    duration = 0.30f
                )
            }
            audioEngine.playSfx(SfxType.DODGE)
        }
    }

    fun triggerDodge() {
        val state = _hudState.value
        if (isDodging || state.stamina < 16f) return
        isDodging = true
        dodgeTimer = 0.25f
        _hudState.update { it.copy(stamina = (it.stamina - 16f).coerceAtLeast(0f), isChargingEnergy = false) }
        audioEngine.playSfx(SfxType.DODGE)
    }

    fun setBlocking(active: Boolean) {
        if (active && !_hudState.value.isBlocking) {
            parryWindowTimer = 0.30f
        }
        _hudState.update { it.copy(isBlocking = active, isChargingEnergy = if (active) false else it.isChargingEnergy) }
    }

    fun setChargingEnergy(active: Boolean) {
        if (active && !_hudState.value.isChargingEnergy) {
            chargeVfxTimer = 0f
            audioEngine.playSfx(SfxType.UI_CLICK)
        }
        _hudState.update { it.copy(isChargingEnergy = active) }
    }

    fun triggerLightAttack() {
        if (isDodging) return
        val state = _hudState.value
        val nextStep = (state.comboStep % 4) + 1
        comboResetTimer = 1.15f
        attackTimer = 0.26f
        _hudState.update { it.copy(comboStep = nextStep) }
        audioEngine.playSfx(SfxType.SLASH)

        val baseDmg = (18f + nextStep * 5f + state.attackBonus * 3f + state.weaponUpgrade * 4f) *
            state.activeStyle.damageMultiplier
        performMeleeHit(baseDmg, range = 3.2f, postureMult = 0.8f)
    }

    fun triggerHeavyAttack() {
        val state = _hudState.value
        if (isDodging || state.stamina < 20f) return
        attackTimer = 0.36f
        _hudState.update { it.copy(stamina = (it.stamina - 20f).coerceAtLeast(0f)) }
        audioEngine.playSfx(SfxType.HEAVY_HIT)

        val baseDmg = (38f + state.attackBonus * 4f + state.weaponUpgrade * 5f) *
            state.activeStyle.damageMultiplier
        performMeleeHit(baseDmg, range = 3.6f, postureMult = 1.8f)
    }

    fun triggerSkill(slot: Int) {
        val state = _hudState.value
        val styleSkills = WorldCatalog.skills.filter { it.style == state.activeStyle && !it.isUltimate }
        val skill = styleSkills.getOrNull(slot - 1) ?: return
        val cd = if (slot == 1) state.skill1Cooldown else state.skill2Cooldown
        if (cd > 0f || state.energy < skill.energyCost) {
            showBanner("Energia insuficiente ou recarregando")
            return
        }

        attackTimer = 0.34f
        _hudState.update {
            it.copy(
                energy = (it.energy - skill.energyCost).coerceAtLeast(0f),
                skill1Cooldown = if (slot == 1) skill.cooldownSeconds else it.skill1Cooldown,
                skill2Cooldown = if (slot == 2) skill.cooldownSeconds else it.skill2Cooldown
            )
        }
        audioEngine.playStyleSkillSfx(state.activeStyle)
        showBanner(skill.name)

        val dmg = (skill.damage + state.attackBonus * 4f + state.weaponUpgrade * 5f) *
            state.activeStyle.damageMultiplier
        performMeleeHit(dmg, range = 4.6f, postureMult = 1.6f)
    }

    fun triggerUltimate() {
        val state = _hudState.value
        val ult = WorldCatalog.skills.firstOrNull { it.style == state.activeStyle && it.isUltimate } ?: return
        if (state.ultCooldown > 0f || state.energy < ult.energyCost) {
            showBanner("Energia insuficiente para Suprema!")
            return
        }

        attackTimer = 0.45f
        _hudState.update {
            it.copy(
                energy = (it.energy - ult.energyCost).coerceAtLeast(0f),
                ultCooldown = ult.cooldownSeconds
            )
        }
        audioEngine.playStyleSkillSfx(state.activeStyle)
        showBanner("SUPREMA: ${ult.name.uppercase()}!")

        val dmg = (ult.damage + state.attackBonus * 6f + state.weaponUpgrade * 8f) *
            state.activeStyle.damageMultiplier
        performMeleeHit(dmg, range = 6.5f, postureMult = 2.5f)
    }

    fun cycleCombatStyle() {
        val all = CombatStyle.entries
        val nextIdx = (all.indexOf(_hudState.value.activeStyle) + 1) % all.size
        val nextStyle = all[nextIdx]
        _hudState.update { it.copy(activeStyle = nextStyle) }
        audioEngine.playSfx(SfxType.UI_CLICK)
        showBanner(nextStyle.displayName)
    }

    fun toggleTargetLock() {
        if (lockedEnemyId != null) {
            lockedEnemyId = null
            _hudState.update { it.copy(isLockedOn = false) }
            return
        }
        val nearest = simEnemies
            .filter { it.aiState != EnemyAiState.DEAD }
            .minByOrNull { hypot(it.x - playerX, it.z - playerZ) }
        if (nearest != null) {
            lockedEnemyId = nearest.id
            _hudState.update { it.copy(isLockedOn = true) }
        }
    }

    fun useConsumable(itemId: String) {
        val state = _hudState.value
        val qty = state.inventory[itemId] ?: 0
        if (qty <= 0) return

        val updatedInv = state.inventory.toMutableMap()
        updatedInv[itemId] = qty - 1
        if ((updatedInv[itemId] ?: 0) <= 0) updatedInv.remove(itemId)

        when (itemId) {
            "pocao" -> {
                _hudState.update {
                    it.copy(
                        hp = (it.hp + 65f).coerceAtMost(it.maxHp),
                        inventory = updatedInv
                    )
                }
                audioEngine.playSfx(SfxType.HEAL)
                showBanner("+65 Vida (Chá de Aroeira)")
            }
            "antidoto" -> {
                _hudState.update {
                    it.copy(
                        hp = (it.hp + 25f).coerceAtMost(it.maxHp),
                        inventory = updatedInv
                    )
                }
                audioEngine.playSfx(SfxType.HEAL)
                showBanner("Purificado! +25 Vida")
            }
            "rapadura" -> {
                _hudState.update {
                    it.copy(
                        energy = (it.energy + 60f).coerceAtMost(it.maxEnergy),
                        stamina = (it.stamina + 60f).coerceAtMost(it.maxStamina),
                        inventory = updatedInv
                    )
                }
                audioEngine.playSfx(SfxType.LEVEL_UP)
                showBanner("+60 Energia e Stamina!")
            }
            "kunai" -> {
                _hudState.update { it.copy(inventory = updatedInv) }
                audioEngine.playSfx(SfxType.SLASH)
                performMeleeHit(55f, range = 9.0f, postureMult = 1.4f)
                showBanner("Faca de Ipê-Roxo!")
            }
        }
    }

    fun interactWithWorld() {
        val state = _hudState.value
        if (state.isNearNpc) {
            playerYaw = atan2(6f - playerX, -6f - playerZ)
            startDialogue(state.currentRegion.npcDialogue)
            return
        }
        if (state.isNearPortal && state.portalUnlocked) {
            val nextIdx = state.currentRegion.index
            if (nextIdx < WorldCatalog.regions.size) {
                val nextRegion = WorldCatalog.regions[nextIdx]
                _hudState.update {
                    it.copy(unlockedRegions = (it.unlockedRegions + nextRegion.id).distinct())
                }
                loadRegion(nextRegion, showIntro = true)
                triggerAutoSave()
            } else {
                _hudState.update { it.copy(campaignCompleted = true) }
                enterCreditsMusic()
            }
        }
    }

    // --- Dialogos & Dublagem PT-BR ---

    fun startDialogue(lines: List<DialogueLine>, onFinishAction: String = "") {
        if (lines.isEmpty()) return
        val first = lines.first()
        _hudState.update { it.copy(activeDialogue = ActiveDialogueState(lines, 0, onFinishAction)) }
        audioEngine.speakLine(first.speaker, first.text)
    }

    fun advanceDialogue(choice: DialogueChoice? = null) {
        val dlg = _hudState.value.activeDialogue ?: return
        audioEngine.stopVoice()

        if (choice != null && choice.bonusXp > 0) {
            grantRewards(choice.bonusXp, 0)
        }

        val nextIdx = choice?.nextLineIndex ?: (dlg.index + 1)
        if (nextIdx < dlg.lines.size && choice == null || (choice != null && nextIdx in dlg.lines.indices)) {
            val nextLine = dlg.lines[nextIdx]
            _hudState.update { it.copy(activeDialogue = dlg.copy(index = nextIdx)) }
            audioEngine.speakLine(nextLine.speaker, nextLine.text)
        } else {
            val action = dlg.onFinishAction
            _hudState.update { it.copy(activeDialogue = null) }
            if (action == "campaign_complete") {
                _hudState.update { it.copy(campaignCompleted = true) }
                enterCreditsMusic()
            }
        }
    }

    fun replayCurrentVoice() {
        val line = _hudState.value.activeDialogue?.currentLine ?: return
        audioEngine.speakLine(line.speaker, line.text)
    }

    // --- Combate Interno ---

    private fun performMeleeHit(damage: Float, range: Float, postureMult: Float) {
        val state = _hudState.value
        val styleRgb = hexToTriple(state.activeStyle.colorHex)
        val hitCenterX = playerX + sin(playerYaw) * (range * 0.45f)
        val hitCenterZ = playerZ + cos(playerYaw) * (range * 0.45f)

        var hitsLanded = 0
        for (enemy in simEnemies) {
            if (enemy.aiState == EnemyAiState.DEAD) continue
            val d = hypot(enemy.x - hitCenterX, enemy.z - hitCenterZ)
            if (d <= range * 0.72f) {
                hitsLanded += 1
                val isCrit = Math.random() < state.activeStyle.critBonus
                val finalDmg = damage * (if (isCrit) 1.5f else 1f) *
                    (if (enemy.aiState == EnemyAiState.STUNNED) 1.35f else 1f)
                enemy.hp = (enemy.hp - finalDmg).coerceAtLeast(0f)
                enemy.posture += finalDmg * 0.55f * postureMult
                enemy.hitFlashTimer = 0.14f

                val pushX = enemy.x - playerX
                val pushZ = enemy.z - playerZ
                val pLen = hypot(pushX, pushZ).coerceAtLeast(0.01f)
                enemy.x += (pushX / pLen) * 0.55f
                enemy.z += (pushZ / pLen) * 0.55f

                simVfx += SimVfx(enemy.x, 1.2f, enemy.z, 1.8f, styleRgb)
                if (isCrit) showBanner("GOLPE CRÍTICO!")

                // Multi-phase Boss check
                if (enemy.kind == EnemyVisualKind.BOSS) {
                    val hpPct = enemy.hp / enemy.maxHp
                    if (enemy.phase == 1 && hpPct <= 0.55f && enemy.hp > 0f) {
                        enemy.phase = 2
                        showBanner("${enemy.name}: FASE 2!")
                    } else if (enemy.phase == 2 && hpPct <= 0.25f && enemy.hp > 0f) {
                        enemy.phase = 3
                        showBanner("${enemy.name}: FASE FINAL!")
                    }
                }

                if (enemy.hp <= 0f) {
                    onEnemyDefeated(enemy)
                } else if (enemy.posture >= enemy.maxPosture) {
                    enemy.posture = 0f
                    enemy.aiState = EnemyAiState.STUNNED
                    enemy.stateTimer = 2.2f
                    showBanner("POSTURA QUEBRADA!")
                }
            }
        }
        if (hitsLanded > 0) {
            comboHitsTimer = 2.4f
            _hudState.update { it.copy(comboHits = it.comboHits + hitsLanded) }
            audioEngine.playSfx(SfxType.ENEMY_HIT)
        }
    }

    private fun applyDamageToPlayer(rawDamage: Float, attacker: SimEnemy) {
        if (isDodging) {
            showBanner("ESQUIVA PERFEITA!")
            return
        }
        val state = _hudState.value
        if (state.isBlocking && parryWindowTimer > 0f) {
            audioEngine.playSfx(SfxType.PARRY)
            attacker.aiState = EnemyAiState.STUNNED
            attacker.stateTimer = 2.4f
            attacker.posture += attacker.maxPosture * 0.6f
            parryCount += 1
            simVfx += SimVfx(playerX, 1.2f, playerZ, 2.2f, Triple(1.0f, 0.90f, 0.35f))
            _hudState.update {
                it.copy(energy = (it.energy + 25f).coerceAtMost(it.maxEnergy))
            }
            if (parryCount >= 3) unlockAchievement("ach_parry")
            showBanner("PARRY PERFEITO!")
            return
        }

        val reduction = if (state.isBlocking) state.activeStyle.blockReduction else 0f
        val defMitigation = (1f - (state.defenseBonus * 0.03f)).coerceIn(0.45f, 1f)
        val finalDmg = rawDamage * (1f - reduction) * defMitigation
        val nextHp = (state.hp - finalDmg).coerceAtLeast(0f)

        _hudState.update {
            it.copy(
                hp = nextHp,
                isGameOver = nextHp <= 0f
            )
        }
    }

    private fun onEnemyDefeated(enemy: SimEnemy) {
        enemy.aiState = EnemyAiState.DEAD
        grantRewards(enemy.xpReward, enemy.coinReward)
        unlockAchievement("ach_first_blood")

        if (enemy.kind == EnemyVisualKind.BOSS) {
            regionBossDefeated = true
            val region = _hudState.value.currentRegion
            val nextIdx = region.index
            val nextRegionId = WorldCatalog.regions.getOrNull(nextIdx)?.id
            _hudState.update {
                it.copy(
                    portalUnlocked = true,
                    currentChapter = region.chapterRange.last,
                    completedQuests = (it.completedQuests + region.mainQuestId).distinct(),
                    unlockedRegions = if (nextRegionId != null) (it.unlockedRegions + nextRegionId).distinct() else it.unlockedRegions
                )
            }
            if (region.index == 5) unlockAchievement("ach_act1")
            if (region.index == 10) unlockAchievement("ach_umbra")

            val finishAction = if (region.index == 10) "campaign_complete" else ""
            startDialogue(region.victoryDialogue, onFinishAction = finishAction)
            triggerAutoSave()
        }
    }

    private fun grantRewards(xpGain: Int, coinGain: Int) {
        if (xpGain <= 0 && coinGain <= 0) return
        _hudState.update { state ->
            var lvl = state.level
            var xp = state.xp + xpGain
            var xpNext = state.xpToNext
            var sp = state.skillPoints
            var leveled = false
            while (xp >= xpNext && lvl < 99) {
                xp -= xpNext
                lvl += 1
                sp += 1
                xpNext = lvl * 120
                leveled = true
            }
            val newMaxHp = calcMaxHp(lvl, state.defenseBonus)
            if (leveled) {
                audioEngine.playSfx(SfxType.LEVEL_UP)
            }
            state.copy(
                level = lvl,
                xp = xp,
                xpToNext = xpNext,
                coins = state.coins + coinGain,
                skillPoints = sp,
                maxHp = newMaxHp,
                hp = if (leveled) newMaxHp else state.hp,
                combatBanner = if (leveled) "SUBIU PARA O NÍVEL $lvl!" else state.combatBanner
            )
        }
    }

    // --- RPG Menus: Customizacao, Loja, Habilidades, Save/Load ---

    fun updateCustomization(
        heroName: String? = null,
        hatStyle: HatStyle? = null,
        outfitStyle: OutfitStyle? = null,
        bladeColor: BladeColor? = null,
        activeStyle: CombatStyle? = null
    ) {
        _hudState.update { s ->
            s.copy(
                heroName = heroName?.take(24)?.ifBlank { "Kauan" } ?: s.heroName,
                hatStyle = hatStyle ?: s.hatStyle,
                outfitStyle = outfitStyle ?: s.outfitStyle,
                bladeColor = bladeColor ?: s.bladeColor,
                activeStyle = activeStyle ?: s.activeStyle
            )
        }
    }

    fun investAttribute(attr: String) {
        val state = _hudState.value
        if (state.skillPoints <= 0) return
        _hudState.update { s ->
            val atk = if (attr == "atk") s.attackBonus + 1 else s.attackBonus
            val def = if (attr == "def") s.defenseBonus + 1 else s.defenseBonus
            val eng = if (attr == "eng") s.energyBonus + 1 else s.energyBonus
            val newMaxHp = calcMaxHp(s.level, def)
            val newMaxEn = calcMaxEnergy(eng)
            s.copy(
                skillPoints = s.skillPoints - 1,
                attackBonus = atk,
                defenseBonus = def,
                energyBonus = eng,
                maxHp = newMaxHp,
                maxEnergy = newMaxEn
            )
        }
        audioEngine.playSfx(SfxType.LEVEL_UP)
    }

    fun unlockSkill(skillId: String) {
        val state = _hudState.value
        if (state.skillPoints <= 0 || skillId in state.unlockedSkills) return
        val updated = (state.unlockedSkills + skillId).distinct()
        _hudState.update {
            it.copy(
                skillPoints = it.skillPoints - 1,
                unlockedSkills = updated
            )
        }
        val stylesCovered = WorldCatalog.skills.filter { it.id in updated }.map { it.style }.distinct()
        if (stylesCovered.size >= 5) unlockAchievement("ach_styles")
        audioEngine.playSfx(SfxType.LEVEL_UP)
    }

    fun buyShopItem(itemId: String) {
        val def = WorldCatalog.shopItems.firstOrNull { it.id == itemId } ?: return
        val state = _hudState.value
        if (state.coins < def.priceCoins) {
            setStatusMessage("Patacas insuficientes!")
            return
        }
        val inv = state.inventory.toMutableMap()
        inv[itemId] = (inv[itemId] ?: 0) + 1
        _hudState.update {
            it.copy(
                coins = it.coins - def.priceCoins,
                inventory = inv,
                statusMessage = "${def.name} comprado!"
            )
        }
        audioEngine.playSfx(SfxType.UI_CLICK)
    }

    fun upgradeWeapon() {
        val state = _hudState.value
        val cost = state.weaponUpgrade * 45
        if (state.weaponUpgrade >= 10) {
            setStatusMessage("Sua Lâmina Solar já está no nível máximo (+10)!")
            return
        }
        if (state.coins < cost) {
            setStatusMessage("Precisa de $cost Patacas para temperar a lâmina.")
            return
        }
        _hudState.update {
            it.copy(
                coins = it.coins - cost,
                weaponUpgrade = it.weaponUpgrade + 1,
                statusMessage = "Lâmina Solar temperada para +${it.weaponUpgrade + 1}!"
            )
        }
        audioEngine.playSfx(SfxType.PARRY)
    }

    fun saveToSlot(slotId: String, label: String) {
        val state = _hudState.value
        val slot = GameSaveSlot(
            id = slotId,
            userId = currentUserId,
            slotLabel = label,
            heroName = state.heroName,
            hatStyle = state.hatStyle.name,
            outfitStyle = state.outfitStyle.name,
            bladeColor = state.bladeColor.name,
            activeStyle = state.activeStyle.name,
            level = state.level.toLong(),
            xp = state.xp.toLong(),
            coins = state.coins.toLong(),
            currentChapter = state.currentChapter.toLong(),
            currentRegionId = state.currentRegion.id,
            skillPoints = state.skillPoints.toLong(),
            attackBonus = state.attackBonus.toLong(),
            defenseBonus = state.defenseBonus.toLong(),
            energyBonus = state.energyBonus.toLong(),
            weaponUpgrade = state.weaponUpgrade.toLong(),
            playTimeSeconds = state.playTimeSeconds,
            unlockedRegions = state.unlockedRegions,
            completedQuests = state.completedQuests,
            unlockedSkills = state.unlockedSkills,
            unlockedAchievements = state.unlockedAchievements,
            inventoryEntries = state.inventory.entries.map { "${it.key}:${it.value}" }
        )
        viewModelScope.launch {
            val res = saveRepository.saveSlot(currentUserId, slotId, slot)
            setStatusMessage(
                if (res.isSuccess) "Progresso salvo em $label!"
                else "Falha ao salvar: ${res.exceptionOrNull()?.localizedMessage ?: "erro desconhecido"}"
            )
        }
    }

    fun triggerAutoSave() {
        saveToSlot("autosave", "Salvamento Automático")
    }

    fun deleteSaveSlot(slotId: String) {
        viewModelScope.launch {
            val res = saveRepository.deleteSlot(currentUserId, slotId)
            setStatusMessage(if (res.isSuccess) "Salvamento removido." else "Erro ao apagar salvamento.")
        }
    }

    fun setPaused(paused: Boolean) {
        _hudState.update { it.copy(isPaused = paused) }
    }

    fun retryCurrentRegion() {
        loadRegion(_hudState.value.currentRegion, showIntro = false)
    }

    fun setStatusMessage(msg: String?) {
        _hudState.update { it.copy(statusMessage = msg) }
    }

    fun updateSettings(transform: (GameSettings) -> GameSettings) {
        viewModelScope.launch {
            settingsRepository.updateSettings(transform)
        }
    }

    private fun unlockAchievement(achId: String) {
        val state = _hudState.value
        if (achId in state.unlockedAchievements) return
        val def = WorldCatalog.achievements.firstOrNull { it.id == achId }
        _hudState.update {
            it.copy(
                unlockedAchievements = it.unlockedAchievements + achId,
                combatBanner = def?.let { d -> "Conquista: ${d.title}" } ?: it.combatBanner
            )
        }
    }

    private fun showBanner(text: String) {
        bannerTimer = 2.0f
        _hudState.update { it.copy(combatBanner = text) }
    }

    private fun calcMaxHp(level: Int, defBonus: Int): Float = 150f + (level - 1) * 18f + defBonus * 15f
    private fun calcMaxEnergy(engBonus: Int): Float = 100f + engBonus * 12f

    private fun hexToTriple(hex: Long): Triple<Float, Float, Float> {
        val r = ((hex shr 16) and 0xFF) / 255f
        val g = ((hex shr 8) and 0xFF) / 255f
        val b = (hex and 0xFF) / 255f
        return Triple(r, g, b)
    }

    override fun onCleared() {
        super.onCleared()
        loopJob?.cancel()
    }

    private companion object {
        const val TAG = "GameViewModel"
    }
}
