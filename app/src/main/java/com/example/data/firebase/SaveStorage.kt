package com.example.data.firebase

import com.example.data.model.GameSaveSlot
import kotlinx.coroutines.flow.Flow

interface SaveStorage {
    fun observeSaveSlots(userId: String): Flow<List<GameSaveSlot>>
    suspend fun saveSlot(userId: String, slotId: String, slot: GameSaveSlot): Result<Unit>
    suspend fun deleteSlot(userId: String, slotId: String): Result<Unit>
}
