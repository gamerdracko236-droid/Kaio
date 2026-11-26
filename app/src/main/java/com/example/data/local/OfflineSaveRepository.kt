package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.firebase.SaveStorage
import com.example.data.model.GameSaveSlot
import com.google.firebase.Timestamp
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject
import java.util.Date

private val Context.offlineSavesDataStore: DataStore<Preferences> by preferencesDataStore(name = "sertao_offline_saves")

class OfflineSaveRepository(private val context: Context) : SaveStorage {

    private val slotsJsonKey = stringPreferencesKey("offline_save_slots_json")

    override fun observeSaveSlots(userId: String): Flow<List<GameSaveSlot>> =
        context.offlineSavesDataStore.data.map { prefs ->
            decodeSlots(prefs[slotsJsonKey]).sortedByDescending { it.updatedAt ?: it.createdAt }
        }

    override suspend fun saveSlot(userId: String, slotId: String, slot: GameSaveSlot): Result<Unit> {
        val safeSlotId = slotId.trim().replace(Regex("[^a-zA-Z0-9_\\-]"), "_").take(64)
        if (safeSlotId.isBlank()) {
            return Result.failure(IllegalArgumentException("Identificador do slot inválido."))
        }
        return runCatching {
            val nowMs = System.currentTimeMillis()
            context.offlineSavesDataStore.edit { prefs ->
                val list = decodeSlots(prefs[slotsJsonKey]).toMutableList()
                val existingIdx = list.indexOfFirst { it.id == safeSlotId }
                val existingCreatedMs = if (existingIdx >= 0) {
                    list[existingIdx].createdAt?.toDate()?.time ?: nowMs
                } else {
                    nowMs
                }
                val saved = slot.copy(
                    id = safeSlotId,
                    userId = "offline_local",
                    createdAt = Timestamp(Date(existingCreatedMs)),
                    updatedAt = Timestamp(Date(nowMs))
                )
                if (existingIdx >= 0) {
                    list[existingIdx] = saved
                } else {
                    list.add(saved)
                }
                prefs[slotsJsonKey] = encodeSlots(list)
            }
        }
    }

    override suspend fun deleteSlot(userId: String, slotId: String): Result<Unit> = runCatching {
        context.offlineSavesDataStore.edit { prefs ->
            val filtered = decodeSlots(prefs[slotsJsonKey]).filterNot { it.id == slotId }
            prefs[slotsJsonKey] = encodeSlots(filtered)
        }
    }

    private fun encodeSlots(slots: List<GameSaveSlot>): String {
        val arr = JSONArray()
        slots.forEach { s ->
            val obj = JSONObject().apply {
                put("id", s.id)
                put("userId", s.userId)
                put("slotLabel", s.slotLabel)
                put("heroName", s.heroName)
                put("hatStyle", s.hatStyle)
                put("outfitStyle", s.outfitStyle)
                put("bladeColor", s.bladeColor)
                put("activeStyle", s.activeStyle)
                put("level", s.level)
                put("xp", s.xp)
                put("coins", s.coins)
                put("currentChapter", s.currentChapter)
                put("currentRegionId", s.currentRegionId)
                put("skillPoints", s.skillPoints)
                put("attackBonus", s.attackBonus)
                put("defenseBonus", s.defenseBonus)
                put("energyBonus", s.energyBonus)
                put("weaponUpgrade", s.weaponUpgrade)
                put("playTimeSeconds", s.playTimeSeconds)
                put("unlockedRegions", JSONArray(s.unlockedRegions))
                put("completedQuests", JSONArray(s.completedQuests))
                put("unlockedSkills", JSONArray(s.unlockedSkills))
                put("unlockedAchievements", JSONArray(s.unlockedAchievements))
                put("inventoryEntries", JSONArray(s.inventoryEntries))
                put("createdAtMs", s.createdAt?.toDate()?.time ?: 0L)
                put("updatedAtMs", s.updatedAt?.toDate()?.time ?: 0L)
            }
            arr.put(obj)
        }
        return arr.toString()
    }

    private fun decodeSlots(raw: String?): List<GameSaveSlot> {
        if (raw.isNullOrBlank()) return emptyList()
        return try {
            val arr = JSONArray(raw)
            buildList {
                for (i in 0 until arr.length()) {
                    val o = arr.optJSONObject(i) ?: continue
                    val createdMs = o.optLong("createdAtMs", 0L)
                    val updatedMs = o.optLong("updatedAtMs", 0L)
                    add(
                        GameSaveSlot(
                            id = o.optString("id", "autosave"),
                            userId = o.optString("userId", "offline_local"),
                            slotLabel = o.optString("slotLabel", "Salvamento Local"),
                            heroName = o.optString("heroName", "Kauan"),
                            hatStyle = o.optString("hatStyle", "VAQUEIRO"),
                            outfitStyle = o.optString("outfitStyle", "GIBAO_COURO"),
                            bladeColor = o.optString("bladeColor", "BRASA_VIVA"),
                            activeStyle = o.optString("activeStyle", "CHAMA"),
                            level = o.optLong("level", 1L),
                            xp = o.optLong("xp", 0L),
                            coins = o.optLong("coins", 60L),
                            currentChapter = o.optLong("currentChapter", 1L),
                            currentRegionId = o.optString("currentRegionId", "vila_das_almas"),
                            skillPoints = o.optLong("skillPoints", 1L),
                            attackBonus = o.optLong("attackBonus", 0L),
                            defenseBonus = o.optLong("defenseBonus", 0L),
                            energyBonus = o.optLong("energyBonus", 0L),
                            weaponUpgrade = o.optLong("weaponUpgrade", 1L),
                            playTimeSeconds = o.optLong("playTimeSeconds", 0L),
                            unlockedRegions = jsonArrayToList(o.optJSONArray("unlockedRegions")).ifEmpty { listOf("vila_das_almas") },
                            completedQuests = jsonArrayToList(o.optJSONArray("completedQuests")),
                            unlockedSkills = jsonArrayToList(o.optJSONArray("unlockedSkills")).ifEmpty { listOf("chama_1") },
                            unlockedAchievements = jsonArrayToList(o.optJSONArray("unlockedAchievements")),
                            inventoryEntries = jsonArrayToList(o.optJSONArray("inventoryEntries")),
                            createdAt = if (createdMs > 0L) Timestamp(Date(createdMs)) else null,
                            updatedAt = if (updatedMs > 0L) Timestamp(Date(updatedMs)) else null
                        )
                    )
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun jsonArrayToList(arr: JSONArray?): List<String> {
        if (arr == null) return emptyList()
        return buildList {
            for (i in 0 until arr.length()) {
                val v = arr.optString(i, "")
                if (v.isNotEmpty()) add(v)
            }
        }
    }
}
