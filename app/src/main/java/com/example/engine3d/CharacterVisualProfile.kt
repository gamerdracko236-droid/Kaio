package com.example.engine3d

import com.example.data.model.BladeColor
import com.example.data.model.CombatStyle
import com.example.data.model.HatStyle
import com.example.data.model.OutfitStyle

enum class HeadgearType { NONE, LEATHER_HAT, CANGACEIRO_CROWN, SHINOBI_BANDANA, HORNED_HELM }
enum class WeaponModelType { NONE, MACHETE_SWORD, DUAL_BLADES, HEAVY_CLUB, DARK_SABER }
enum class BackGarmentType { NONE, FLOWING_SCARF, PONCHO_CAPE, WINGED_CLOAK }

data class CharacterVisualProfile(
    val id: String,
    val displayName: String,
    val scale: Float = 1.0f,
    val shoulderWidthScale: Float = 1.0f,
    val legLengthScale: Float = 1.0f,
    val armLengthScale: Float = 1.0f,
    val skinColor: FloatArray,
    val hairColor: FloatArray,
    val primaryOutfitColor: FloatArray,
    val secondaryOutfitColor: FloatArray,
    val pantsColor: FloatArray,
    val bootsColor: FloatArray,
    val accentColor: FloatArray,
    val weaponColor: FloatArray,
    val outlineColor: FloatArray = floatArrayOf(0.11f, 0.07f, 0.08f, 1.0f),
    val outlineWidth: Float = 0.011f,
    val spikyHair: Boolean = true,
    val demonEyes: Boolean = false,
    val headgear: HeadgearType = HeadgearType.NONE,
    val weaponType: WeaponModelType = WeaponModelType.MACHETE_SWORD,
    val backGarment: BackGarmentType = BackGarmentType.FLOWING_SCARF
)

object SertaoCharacterCatalog {

    private fun hexToRgbArray(hex: Long): FloatArray = floatArrayOf(
        ((hex shr 16) and 0xFF) / 255f,
        ((hex shr 8) and 0xFF) / 255f,
        (hex and 0xFF) / 255f,
        1f
    )

    fun heroKauan(
        hatStyle: HatStyle = HatStyle.VAQUEIRO,
        outfitStyle: OutfitStyle = OutfitStyle.GIBAO_COURO,
        bladeColor: BladeColor = BladeColor.BRASA_VIVA,
        activeStyle: CombatStyle = CombatStyle.CHAMA,
        weaponUpgrade: Int = 1
    ): CharacterVisualProfile {
        val (primary, secondary, pants) = when (outfitStyle) {
            OutfitStyle.GIBAO_COURO -> Triple(
                floatArrayOf(0.84f, 0.43f, 0.16f, 1f),
                floatArrayOf(0.24f, 0.18f, 0.16f, 1f),
                floatArrayOf(0.20f, 0.25f, 0.34f, 1f)
            )
            OutfitStyle.MANTO_CHAMA -> Triple(
                floatArrayOf(0.86f, 0.28f, 0.12f, 1f),
                floatArrayOf(0.92f, 0.72f, 0.22f, 1f),
                floatArrayOf(0.18f, 0.15f, 0.18f, 1f)
            )
            OutfitStyle.MANTO_AGUAS -> Triple(
                floatArrayOf(0.16f, 0.38f, 0.74f, 1f),
                floatArrayOf(0.14f, 0.18f, 0.28f, 1f),
                floatArrayOf(0.14f, 0.18f, 0.26f, 1f)
            )
            OutfitStyle.MANTO_NOITE -> Triple(
                floatArrayOf(0.18f, 0.14f, 0.26f, 1f),
                floatArrayOf(0.68f, 0.20f, 0.26f, 1f),
                floatArrayOf(0.12f, 0.10f, 0.16f, 1f)
            )
        }
        val headgear = when (hatStyle) {
            HatStyle.VAQUEIRO -> HeadgearType.LEATHER_HAT
            HatStyle.CANGACEIRO -> HeadgearType.CANGACEIRO_CROWN
            HatStyle.BANDANA -> HeadgearType.SHINOBI_BANDANA
            HatStyle.SEM_CHAPEU -> HeadgearType.NONE
        }
        val bladeArr = floatArrayOf(bladeColor.rgb.first, bladeColor.rgb.second, bladeColor.rgb.third, 1f)
        val styleArr = hexToRgbArray(activeStyle.colorHex)
        val armorScale = 1.0f + ((weaponUpgrade - 1).coerceAtLeast(0)) * 0.03f

        return CharacterVisualProfile(
            id = "kauan",
            displayName = "Kauan",
            scale = 1.0f,
            shoulderWidthScale = armorScale,
            skinColor = floatArrayOf(0.95f, 0.80f, 0.68f, 1f),
            hairColor = floatArrayOf(0.14f, 0.11f, 0.10f, 1f),
            primaryOutfitColor = primary,
            secondaryOutfitColor = secondary,
            pantsColor = pants,
            bootsColor = floatArrayOf(0.15f, 0.10f, 0.09f, 1f),
            accentColor = styleArr,
            weaponColor = bladeArr,
            outlineColor = floatArrayOf(0.11f, 0.07f, 0.08f, 1.0f),
            outlineWidth = 0.011f,
            spikyHair = true,
            headgear = headgear,
            weaponType = if (weaponUpgrade >= 3) WeaponModelType.DUAL_BLADES else WeaponModelType.MACHETE_SWORD,
            backGarment = BackGarmentType.FLOWING_SCARF
        )
    }

    fun npcForRegion(npcName: String): CharacterVisualProfile = when {
        npcName.contains("Tain", ignoreCase = true) -> CharacterVisualProfile(
            id = "npc_taina",
            displayName = npcName,
            scale = 0.96f,
            shoulderWidthScale = 0.88f,
            skinColor = floatArrayOf(0.92f, 0.78f, 0.67f, 1f),
            hairColor = floatArrayOf(0.14f, 0.16f, 0.30f, 1f),
            primaryOutfitColor = floatArrayOf(0.42f, 0.56f, 0.74f, 1f),
            secondaryOutfitColor = floatArrayOf(0.86f, 0.82f, 0.88f, 1f),
            pantsColor = floatArrayOf(0.16f, 0.19f, 0.34f, 1f),
            bootsColor = floatArrayOf(0.22f, 0.14f, 0.12f, 1f),
            accentColor = floatArrayOf(0.32f, 0.72f, 0.92f, 1f),
            weaponColor = floatArrayOf(0.75f, 0.88f, 0.96f, 1f),
            spikyHair = false,
            headgear = HeadgearType.NONE,
            weaponType = WeaponModelType.NONE,
            backGarment = BackGarmentType.FLOWING_SCARF
        )
        npcName.contains("Joaquim", ignoreCase = true) || npcName.contains("Ferreiro", ignoreCase = true) -> CharacterVisualProfile(
            id = "npc_joaquim",
            displayName = npcName,
            scale = 1.06f,
            shoulderWidthScale = 1.18f,
            skinColor = floatArrayOf(0.76f, 0.56f, 0.42f, 1f),
            hairColor = floatArrayOf(0.18f, 0.15f, 0.14f, 1f),
            primaryOutfitColor = floatArrayOf(0.56f, 0.30f, 0.16f, 1f),
            secondaryOutfitColor = floatArrayOf(0.85f, 0.85f, 0.88f, 1f),
            pantsColor = floatArrayOf(0.20f, 0.19f, 0.18f, 1f),
            bootsColor = floatArrayOf(0.16f, 0.10f, 0.08f, 1f),
            accentColor = floatArrayOf(0.95f, 0.58f, 0.16f, 1f),
            weaponColor = floatArrayOf(0.68f, 0.72f, 0.76f, 1f),
            spikyHair = false,
            headgear = HeadgearType.SHINOBI_BANDANA,
            weaponType = WeaponModelType.NONE,
            backGarment = BackGarmentType.NONE
        )
        npcName.contains("Mateus", ignoreCase = true) -> CharacterVisualProfile(
            id = "npc_mateus",
            displayName = npcName,
            scale = 1.02f,
            shoulderWidthScale = 1.04f,
            skinColor = floatArrayOf(0.88f, 0.73f, 0.61f, 1f),
            hairColor = floatArrayOf(0.12f, 0.12f, 0.14f, 1f),
            primaryOutfitColor = floatArrayOf(0.24f, 0.42f, 0.28f, 1f),
            secondaryOutfitColor = floatArrayOf(0.76f, 0.58f, 0.34f, 1f),
            pantsColor = floatArrayOf(0.18f, 0.20f, 0.22f, 1f),
            bootsColor = floatArrayOf(0.18f, 0.12f, 0.09f, 1f),
            accentColor = floatArrayOf(0.95f, 0.78f, 0.24f, 1f),
            weaponColor = floatArrayOf(0.82f, 0.86f, 0.90f, 1f),
            spikyHair = true,
            headgear = HeadgearType.CANGACEIRO_CROWN,
            weaponType = WeaponModelType.MACHETE_SWORD,
            backGarment = BackGarmentType.PONCHO_CAPE
        )
        else -> CharacterVisualProfile(
            id = "npc_severino",
            displayName = npcName,
            scale = 1.04f,
            shoulderWidthScale = 1.06f,
            skinColor = floatArrayOf(0.85f, 0.68f, 0.55f, 1f),
            hairColor = floatArrayOf(0.84f, 0.85f, 0.88f, 1f),
            primaryOutfitColor = floatArrayOf(0.24f, 0.38f, 0.24f, 1f),
            secondaryOutfitColor = floatArrayOf(0.70f, 0.20f, 0.18f, 1f),
            pantsColor = floatArrayOf(0.22f, 0.21f, 0.20f, 1f),
            bootsColor = floatArrayOf(0.22f, 0.14f, 0.10f, 1f),
            accentColor = floatArrayOf(0.95f, 0.80f, 0.26f, 1f),
            weaponColor = floatArrayOf(0.80f, 0.84f, 0.88f, 1f),
            spikyHair = false,
            headgear = HeadgearType.LEATHER_HAT,
            weaponType = WeaponModelType.NONE,
            backGarment = BackGarmentType.PONCHO_CAPE
        )
    }

    fun forEnemy(kind: EnemyVisualKind): CharacterVisualProfile = when (kind) {
        EnemyVisualKind.FAST -> CharacterVisualProfile(
            id = "enemy_fast",
            displayName = "Espreitador",
            scale = 0.94f,
            shoulderWidthScale = 0.88f,
            skinColor = floatArrayOf(0.48f, 0.44f, 0.56f, 1f),
            hairColor = floatArrayOf(0.18f, 0.12f, 0.22f, 1f),
            primaryOutfitColor = floatArrayOf(0.24f, 0.18f, 0.30f, 1f),
            secondaryOutfitColor = floatArrayOf(0.75f, 0.18f, 0.28f, 1f),
            pantsColor = floatArrayOf(0.14f, 0.12f, 0.18f, 1f),
            bootsColor = floatArrayOf(0.10f, 0.08f, 0.12f, 1f),
            accentColor = floatArrayOf(0.95f, 0.26f, 0.22f, 1f),
            weaponColor = floatArrayOf(0.85f, 0.22f, 0.35f, 1f),
            outlineColor = floatArrayOf(0.12f, 0.04f, 0.08f, 1f),
            spikyHair = true,
            demonEyes = true,
            headgear = HeadgearType.NONE,
            weaponType = WeaponModelType.DUAL_BLADES,
            backGarment = BackGarmentType.FLOWING_SCARF
        )
        EnemyVisualKind.TANK -> CharacterVisualProfile(
            id = "enemy_tank",
            displayName = "Bruto de Lajedo",
            scale = 1.24f,
            shoulderWidthScale = 1.32f,
            skinColor = floatArrayOf(0.56f, 0.46f, 0.42f, 1f),
            hairColor = floatArrayOf(0.14f, 0.12f, 0.12f, 1f),
            primaryOutfitColor = floatArrayOf(0.32f, 0.30f, 0.28f, 1f),
            secondaryOutfitColor = floatArrayOf(0.62f, 0.22f, 0.16f, 1f),
            pantsColor = floatArrayOf(0.20f, 0.18f, 0.16f, 1f),
            bootsColor = floatArrayOf(0.12f, 0.10f, 0.09f, 1f),
            accentColor = floatArrayOf(0.95f, 0.48f, 0.18f, 1f),
            weaponColor = floatArrayOf(0.46f, 0.44f, 0.46f, 1f),
            outlineColor = floatArrayOf(0.10f, 0.06f, 0.05f, 1f),
            outlineWidth = 0.013f,
            spikyHair = false,
            demonEyes = true,
            headgear = HeadgearType.HORNED_HELM,
            weaponType = WeaponModelType.HEAVY_CLUB,
            backGarment = BackGarmentType.NONE
        )
        EnemyVisualKind.FLYING -> CharacterVisualProfile(
            id = "enemy_flying",
            displayName = "Rasga-Mortalha",
            scale = 0.96f,
            shoulderWidthScale = 0.92f,
            skinColor = floatArrayOf(0.62f, 0.56f, 0.72f, 1f),
            hairColor = floatArrayOf(0.24f, 0.16f, 0.36f, 1f),
            primaryOutfitColor = floatArrayOf(0.34f, 0.18f, 0.48f, 1f),
            secondaryOutfitColor = floatArrayOf(0.86f, 0.36f, 0.62f, 1f),
            pantsColor = floatArrayOf(0.16f, 0.12f, 0.24f, 1f),
            bootsColor = floatArrayOf(0.12f, 0.08f, 0.18f, 1f),
            accentColor = floatArrayOf(0.88f, 0.34f, 0.94f, 1f),
            weaponColor = floatArrayOf(0.90f, 0.30f, 0.75f, 1f),
            spikyHair = true,
            demonEyes = true,
            headgear = HeadgearType.HORNED_HELM,
            weaponType = WeaponModelType.DARK_SABER,
            backGarment = BackGarmentType.WINGED_CLOAK
        )
        EnemyVisualKind.BOSS -> CharacterVisualProfile(
            id = "boss_ombra",
            displayName = "Guardião das Sombras",
            scale = 1.44f,
            shoulderWidthScale = 1.36f,
            skinColor = floatArrayOf(0.42f, 0.36f, 0.44f, 1f),
            hairColor = floatArrayOf(0.88f, 0.90f, 0.94f, 1f),
            primaryOutfitColor = floatArrayOf(0.16f, 0.12f, 0.22f, 1f),
            secondaryOutfitColor = floatArrayOf(0.68f, 0.14f, 0.20f, 1f),
            pantsColor = floatArrayOf(0.10f, 0.10f, 0.14f, 1f),
            bootsColor = floatArrayOf(0.08f, 0.06f, 0.08f, 1f),
            accentColor = floatArrayOf(0.98f, 0.24f, 0.28f, 1f),
            weaponColor = floatArrayOf(0.95f, 0.18f, 0.28f, 1f),
            outlineColor = floatArrayOf(0.16f, 0.04f, 0.06f, 1f),
            outlineWidth = 0.015f,
            spikyHair = true,
            demonEyes = true,
            headgear = HeadgearType.HORNED_HELM,
            weaponType = WeaponModelType.DARK_SABER,
            backGarment = BackGarmentType.PONCHO_CAPE
        )
        EnemyVisualKind.COMMON -> CharacterVisualProfile(
            id = "enemy_common",
            displayName = "Capanga Sombrio",
            scale = 1.0f,
            shoulderWidthScale = 1.02f,
            skinColor = floatArrayOf(0.58f, 0.50f, 0.54f, 1f),
            hairColor = floatArrayOf(0.16f, 0.12f, 0.16f, 1f),
            primaryOutfitColor = floatArrayOf(0.28f, 0.20f, 0.28f, 1f),
            secondaryOutfitColor = floatArrayOf(0.58f, 0.26f, 0.20f, 1f),
            pantsColor = floatArrayOf(0.18f, 0.16f, 0.20f, 1f),
            bootsColor = floatArrayOf(0.12f, 0.10f, 0.12f, 1f),
            accentColor = floatArrayOf(0.92f, 0.34f, 0.22f, 1f),
            weaponColor = floatArrayOf(0.70f, 0.72f, 0.78f, 1f),
            spikyHair = false,
            demonEyes = true,
            headgear = HeadgearType.SHINOBI_BANDANA,
            weaponType = WeaponModelType.MACHETE_SWORD,
            backGarment = BackGarmentType.NONE
        )
    }
}
