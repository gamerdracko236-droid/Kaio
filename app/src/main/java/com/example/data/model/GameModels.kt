package com.example.data.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class CombatStyle(
    val displayName: String,
    val subtitle: String,
    val colorHex: Long,
    val damageMultiplier: Float,
    val speedMultiplier: Float,
    val blockReduction: Float,
    val critBonus: Float
) {
    CHAMA("Estilo da Chama", "Queimadura solar contínua", 0xFFFF6B2B, 1.15f, 1.0f, 0.65f, 0.08f),
    TEMPESTADE("Estilo da Tempestade", "Velocidade elétrica e atordoamento", 0xFF38BDF8, 1.05f, 1.28f, 0.50f, 0.14f),
    CAATINGA("Estilo da Caatinga", "Defesa firme e contra-ataque brutal", 0xFF4ADE80, 1.12f, 0.92f, 0.85f, 0.06f),
    AGUAS("Estilo das Águas", "Fluxo contínuo e controle de área", 0xFF2563EB, 1.00f, 1.12f, 0.72f, 0.09f),
    LUA("Estilo da Lua", "Cortes crescentes e crítico letal", 0xFFA855F7, 1.20f, 1.08f, 0.58f, 0.22f);

    companion object {
        fun fromRaw(raw: String?): CombatStyle =
            entries.firstOrNull { it.name == raw } ?: CHAMA
    }
}

enum class HatStyle(val displayName: String) {
    VAQUEIRO("Chapéu de Vaqueiro"),
    CANGACEIRO("Chapéu de Cangaceiro"),
    BANDANA("Bandana Escarlate"),
    SEM_CHAPEU("Cabelo Livre");

    companion object {
        fun fromRaw(raw: String?): HatStyle =
            entries.firstOrNull { it.name == raw } ?: VAQUEIRO
    }
}

enum class OutfitStyle(val displayName: String, val primaryRgb: Triple<Float, Float, Float>) {
    GIBAO_COURO("Gibão de Couro", Triple(0.45f, 0.25f, 0.14f)),
    MANTO_CHAMA("Manto da Chama", Triple(0.75f, 0.22f, 0.10f)),
    MANTO_AGUAS("Manto das Águas", Triple(0.12f, 0.34f, 0.68f)),
    MANTO_NOITE("Manto do Eclipse", Triple(0.16f, 0.14f, 0.22f));

    companion object {
        fun fromRaw(raw: String?): OutfitStyle =
            entries.firstOrNull { it.name == raw } ?: GIBAO_COURO
    }
}

enum class BladeColor(val displayName: String, val rgb: Triple<Float, Float, Float>) {
    SOLAR_NEGRA("Lâmina Negra Solar", Triple(0.18f, 0.18f, 0.22f)),
    BRASA_VIVA("Brasa Viva", Triple(1.0f, 0.42f, 0.12f)),
    TROVAO_DOURADO("Trovão Dourado", Triple(0.98f, 0.82f, 0.20f)),
    AZUL_ENXURRADA("Azul Enxurrada", Triple(0.20f, 0.65f, 1.0f)),
    LUA_PRATA("Prata Lunar", Triple(0.76f, 0.52f, 1.0f));

    companion object {
        fun fromRaw(raw: String?): BladeColor =
            entries.firstOrNull { it.name == raw } ?: BRASA_VIVA
    }
}

enum class QualityLevel(val displayName: String) {
    BAIXA("Leve (Android)"),
    MEDIA("Equilibrada"),
    ALTA("Cinematográfica")
}

@Keep
@IgnoreExtraProperties
data class GameSaveSlot(
    @DocumentId val id: String = "",
    val userId: String = "",
    val slotLabel: String = "Salvamento Automático",
    val heroName: String = "Kauan",
    val hatStyle: String = HatStyle.VAQUEIRO.name,
    val outfitStyle: String = OutfitStyle.GIBAO_COURO.name,
    val bladeColor: String = BladeColor.BRASA_VIVA.name,
    val activeStyle: String = CombatStyle.CHAMA.name,
    val level: Long = 1L,
    val xp: Long = 0L,
    val coins: Long = 60L,
    val currentChapter: Long = 1L,
    val currentRegionId: String = "vila_das_almas",
    val skillPoints: Long = 1L,
    val attackBonus: Long = 0L,
    val defenseBonus: Long = 0L,
    val energyBonus: Long = 0L,
    val weaponUpgrade: Long = 1L,
    val playTimeSeconds: Long = 0L,
    val unlockedRegions: List<String> = listOf("vila_das_almas"),
    val completedQuests: List<String> = emptyList(),
    val unlockedSkills: List<String> = listOf("chama_1"),
    val unlockedAchievements: List<String> = emptyList(),
    val inventoryEntries: List<String> = listOf("pocao:3", "antidoto:1", "kunai:3", "rapadura:2"),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun inventoryMap(): Map<String, Int> = buildMap {
        inventoryEntries.forEach { entry ->
            val parts = entry.split(":")
            if (parts.size == 2) {
                val qty = parts[1].toIntOrNull() ?: 0
                if (qty > 0) put(parts[0], qty)
            }
        }
    }

    fun toCreateMap(uid: String): Map<String, Any> = mapOf(
        "userId" to uid,
        "slotLabel" to slotLabel.take(50).ifBlank { "Salvamento" },
        "heroName" to heroName.trim().take(24).ifBlank { "Kauan" },
        "hatStyle" to HatStyle.fromRaw(hatStyle).name,
        "outfitStyle" to OutfitStyle.fromRaw(outfitStyle).name,
        "bladeColor" to BladeColor.fromRaw(bladeColor).name,
        "activeStyle" to CombatStyle.fromRaw(activeStyle).name,
        "level" to level.coerceIn(1L, 99L),
        "xp" to xp.coerceIn(0L, 9999999L),
        "coins" to coins.coerceIn(0L, 9999999L),
        "currentChapter" to currentChapter.coerceIn(1L, 20L),
        "currentRegionId" to currentRegionId.take(40).ifBlank { "vila_das_almas" },
        "skillPoints" to skillPoints.coerceIn(0L, 999L),
        "attackBonus" to attackBonus.coerceIn(0L, 100L),
        "defenseBonus" to defenseBonus.coerceIn(0L, 100L),
        "energyBonus" to energyBonus.coerceIn(0L, 100L),
        "weaponUpgrade" to weaponUpgrade.coerceIn(1L, 10L),
        "playTimeSeconds" to playTimeSeconds.coerceIn(0L, 99999999L),
        "unlockedRegions" to unlockedRegions.distinct().take(15),
        "completedQuests" to completedQuests.distinct().take(50),
        "unlockedSkills" to unlockedSkills.distinct().take(30),
        "unlockedAchievements" to unlockedAchievements.distinct().take(30),
        "inventoryEntries" to inventoryEntries.take(30),
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )

    fun toUpdateMap(): Map<String, Any> = mapOf(
        "slotLabel" to slotLabel.take(50).ifBlank { "Salvamento" },
        "heroName" to heroName.trim().take(24).ifBlank { "Kauan" },
        "hatStyle" to HatStyle.fromRaw(hatStyle).name,
        "outfitStyle" to OutfitStyle.fromRaw(outfitStyle).name,
        "bladeColor" to BladeColor.fromRaw(bladeColor).name,
        "activeStyle" to CombatStyle.fromRaw(activeStyle).name,
        "level" to level.coerceIn(1L, 99L),
        "xp" to xp.coerceIn(0L, 9999999L),
        "coins" to coins.coerceIn(0L, 9999999L),
        "currentChapter" to currentChapter.coerceIn(1L, 20L),
        "currentRegionId" to currentRegionId.take(40).ifBlank { "vila_das_almas" },
        "skillPoints" to skillPoints.coerceIn(0L, 999L),
        "attackBonus" to attackBonus.coerceIn(0L, 100L),
        "defenseBonus" to defenseBonus.coerceIn(0L, 100L),
        "energyBonus" to energyBonus.coerceIn(0L, 100L),
        "weaponUpgrade" to weaponUpgrade.coerceIn(1L, 10L),
        "playTimeSeconds" to playTimeSeconds.coerceIn(0L, 99999999L),
        "unlockedRegions" to unlockedRegions.distinct().take(15),
        "completedQuests" to completedQuests.distinct().take(50),
        "unlockedSkills" to unlockedSkills.distinct().take(30),
        "unlockedAchievements" to unlockedAchievements.distinct().take(30),
        "inventoryEntries" to inventoryEntries.take(30),
        "updatedAt" to FieldValue.serverTimestamp()
    )
}

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}
