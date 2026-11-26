package com.example.data.firebase

import android.content.Context
import com.example.R
import com.example.data.model.GameSaveSlot
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class SaveRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) : SaveStorage {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("Usuário precisa estar conectado com Google antes de salvar.")
    }

    override fun observeSaveSlots(userId: String): Flow<List<GameSaveSlot>> = flow {
        require(userId.isNotBlank()) { "userId inválido" }
        val path = "users/$userId/save_slots"
        emitAll(
            db.collection("users").document(userId).collection("save_slots")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        GameSaveSlot::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.updatedAt ?: it.createdAt }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == userId) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    override suspend fun saveSlot(userId: String, slotId: String, slot: GameSaveSlot): Result<Unit> {
        val safeSlotId = slotId.trim().replace(Regex("[^a-zA-Z0-9_\\-]"), "_").take(64)
        var docPath = "users/$userId/save_slots/$safeSlotId"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Usuário autenticado mudou; gravação recusada." }
            require(safeSlotId.isNotBlank()) { "Identificador do slot não pode ser vazio." }

            val docRef = db.collection("users").document(uid)
                .collection("save_slots").document(safeSlotId)
            docPath = docRef.path

            val existingSnap = docRef.get().await()
            if (existingSnap.exists()) {
                docRef.update(slot.toUpdateMap()).await()
            } else {
                docRef.set(slot.toCreateMap(uid)).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    override suspend fun deleteSlot(userId: String, slotId: String): Result<Unit> {
        var docPath = "users/$userId/save_slots/$slotId"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Usuário autenticado mudou; exclusão recusada." }
            require(slotId.isNotBlank()) { "Slot inválido." }
            val docRef = db.collection("users").document(uid)
                .collection("save_slots").document(slotId)
            docPath = docRef.path
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }
}
