package com.example.engine3d

import com.example.data.model.BladeColor
import com.example.data.model.CombatStyle
import com.example.data.model.HatStyle
import com.example.data.model.OutfitStyle
import com.example.data.model.QualityLevel

enum class EnemyVisualKind {
    COMMON, FAST, TANK, FLYING, BOSS
}

data class EnemyRenderEntity(
    val id: String,
    val name: String = "",
    val x: Float,
    val y: Float,
    val z: Float,
    val yawRad: Float,
    val kind: EnemyVisualKind,
    val isWindup: Boolean,
    val isStunned: Boolean,
    val isHitFlash: Boolean,
    val isLocked: Boolean,
    val hpRatio: Float
)

data class VfxRenderBurst(
    val x: Float,
    val y: Float,
    val z: Float,
    val radius: Float,
    val rgb: Triple<Float, Float, Float>,
    val alpha: Float
)

data class WorldRenderSnapshot(
    val playerX: Float = 0f,
    val playerY: Float = 0f,
    val playerZ: Float = 0f,
    val playerYawRad: Float = 0f,
    val isMoving: Boolean = false,
    val isSprinting: Boolean = false,
    val attackProgress: Float = 0f,
    val isBlocking: Boolean = false,
    val isChargingEnergy: Boolean = false,
    val isDialogueActive: Boolean = false,
    val isDodging: Boolean = false,
    val hatStyle: HatStyle = HatStyle.VAQUEIRO,
    val outfitStyle: OutfitStyle = OutfitStyle.GIBAO_COURO,
    val bladeColor: BladeColor = BladeColor.BRASA_VIVA,
    val activeStyle: CombatStyle = CombatStyle.CHAMA,
    val weaponUpgrade: Int = 1,
    val npcName: String = "Mestre Severino",
    val npcX: Float = 6f,
    val npcZ: Float = -6f,
    val portalX: Float = 0f,
    val portalZ: Float = -24f,
    val portalUnlocked: Boolean = false,
    val enemies: List<EnemyRenderEntity> = emptyList(),
    val vfxBursts: List<VfxRenderBurst> = emptyList(),
    val skyTopRgb: Triple<Float, Float, Float> = Triple(0.18f, 0.42f, 0.78f),
    val skyBottomRgb: Triple<Float, Float, Float> = Triple(0.68f, 0.84f, 0.96f),
    val groundRgb: Triple<Float, Float, Float> = Triple(0.62f, 0.44f, 0.28f),
    val isNight: Boolean = false,
    val regionSeed: Int = 1,
    val qualityLevel: QualityLevel = QualityLevel.MEDIA,
    val lockedTargetPos: Triple<Float, Float, Float>? = null
)
