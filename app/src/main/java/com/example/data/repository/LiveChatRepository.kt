package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.R
import com.example.data.model.ChatMessage
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest

enum class OperationType(val value: String) {
    CREATE("create"),
    UPDATE("update"),
    DELETE("delete"),
    LIST("list"),
    GET("get"),
    WRITE("write"),
}

fun handleFirestoreError(exception: Throwable, operationType: OperationType, path: String?): String {
    val causes = generateSequence(exception) { it.cause }
    val rootError = causes.firstOrNull { it is FirebaseFirestoreException }
        ?: causes.firstOrNull { it !is CancellationException }
        ?: exception

    val auth = Firebase.auth
    val currentUser = auth.currentUser

    val providerInfoList = currentUser?.providerData?.map { provider ->
        JSONObject().apply {
            put("providerId", provider.providerId)
            put("email", provider.email)
        }
    } ?: emptyList()

    val authInfoJson = JSONObject().apply {
        put("userId", currentUser?.uid)
        put("email", currentUser?.email)
        put("emailVerified", currentUser?.isEmailVerified)
        put("tenantId", currentUser?.tenantId)
        put("providerInfo", JSONArray(providerInfoList))
    }

    val errorInfoJson = JSONObject().apply {
        put("error", rootError.message ?: rootError.toString())
        put("operationType", operationType.value)
        put("path", path)
        put("authInfo", authInfoJson)
    }

    val jsonString = errorInfoJson.toString()
    Log.e("FirestoreError", "Firestore Error: $jsonString")
    return jsonString
}

class LiveChatRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        db = FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        ),
        auth = Firebase.auth
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    fun observeRoomMessages(rawRoomId: String, limit: Long = 80L): Flow<List<ChatMessage>> = flow {
        val uid = requireUserId()
        val roomId = sanitizeRoomId(rawRoomId)
        val path = "match_rooms/$roomId/messages"
        emitAll(
            db.collection("match_rooms")
                .document(roomId)
                .collection("messages")
                .orderBy("createdAt", Query.Direction.ASCENDING)
                .limitToLast(limit)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        ChatMessage::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun sendMessage(
        rawRoomId: String,
        text: String,
        senderNameOverride: String? = null,
        photoUrlOverride: String? = null
    ): Result<String> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        val cleanText = text.trim()
        if (cleanText.isEmpty()) {
            return Result.failure(IllegalArgumentException("متن پیام نمی‌تواند خالی باشد."))
        }
        val boundedText = cleanText.take(300)
        val roomId = sanitizeRoomId(rawRoomId)
        val docRef = db.collection("match_rooms")
            .document(roomId)
            .collection("messages")
            .document()

        val displayName = (senderNameOverride?.takeIf { it.isNotBlank() }
            ?: auth.currentUser?.displayName?.takeIf { it.isNotBlank() }
            ?: "تماشاگر خط بازی")
            .trim()
            .take(60)

        val resolvedPhoto = (photoUrlOverride?.takeIf { it.isNotBlank() }
            ?: auth.currentUser?.photoUrl?.toString()?.takeIf { it.isNotBlank() })
            ?.take(500)

        val payload = buildMap<String, Any> {
            put("roomId", roomId)
            put("userId", uid)
            put("senderName", displayName)
            put("text", boundedText)
            put("createdAt", FieldValue.serverTimestamp())
            if (!resolvedPhoto.isNullOrBlank()) {
                put("photoUrl", resolvedPhoto)
            }
        }

        return try {
            docRef.set(payload).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun deleteMessage(rawRoomId: String, messageId: String): Result<Unit> {
        try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        val roomId = sanitizeRoomId(rawRoomId)
        val cleanMsgId = messageId.trim()
        if (!SAFE_ID_REGEX.matches(cleanMsgId)) {
            return Result.failure(IllegalArgumentException("شناسهٔ پیام معتبر نیست."))
        }
        val docRef = db.collection("match_rooms")
            .document(roomId)
            .collection("messages")
            .document(cleanMsgId)
        return try {
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            Result.failure(e)
        }
    }

    companion object {
        private val SAFE_ID_REGEX = Regex("^[a-zA-Z0-9_\\-]{1,128}$")

        fun sanitizeRoomId(rawRoomKey: String): String {
            val trimmed = rawRoomKey.trim()
            if (trimmed.isEmpty()) return "match_live_lobby"
            if (SAFE_ID_REGEX.matches(trimmed)) return trimmed
            val digest = MessageDigest.getInstance("SHA-256")
                .digest(trimmed.toByteArray(Charsets.UTF_8))
                .joinToString("") { "%02x".format(it) }
                .take(24)
            return "room_$digest"
        }
    }
}
