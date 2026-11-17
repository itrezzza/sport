package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.util.UUID

class LiveChatRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun sendMessage_authenticatedUser_createsMessageAndReturnsId() = runBlocking {
        signInTestUser(ALICE_EMAIL)
        val repository = LiveChatRepository(firestore, auth)
        val roomKey = "پخش زنده ${UUID.randomUUID()}"

        val result = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.sendMessage(
                rawRoomId = roomKey,
                text = "گل فوق‌العاده!",
                senderNameOverride = "علی"
            )
        }
        assertTrue(result.isSuccess)
        assertTrue(result.getOrThrow().isNotBlank())
    }

    @Test
    fun observeRoomMessages_authenticatedUser_emitsRealtimeMessages() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val repository = LiveChatRepository(firestore, auth)
        val roomKey = "match_${UUID.randomUUID()}"
        val text = "شروع نیمه دوم"

        val msgId = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.sendMessage(
                rawRoomId = roomKey,
                text = text,
                senderNameOverride = "علی"
            ).getOrThrow()
        }

        val emitted = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeRoomMessages(roomKey).first { list -> list.any { it.id == msgId } }
        }
        val target = emitted.first { it.id == msgId }
        assertEquals(text, target.text)
        assertEquals(aliceUid, target.userId)
        assertEquals("علی", target.senderName)
    }

    @Test
    fun deleteMessage_crossUserDenied_ownerAllowed() = runBlocking {
        signInTestUser(ALICE_EMAIL)
        val aliceRepo = LiveChatRepository(firestore, auth)
        val roomKey = "match_delete_${UUID.randomUUID()}"

        val msgId = withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.sendMessage(
                rawRoomId = roomKey,
                text = "پیام آزمایشی",
                senderNameOverride = "علی"
            ).getOrThrow()
        }

        signInTestUser(BOB_EMAIL)
        val bobRepo = LiveChatRepository(firestore, auth)
        val crossDeleteResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            bobRepo.deleteMessage(roomKey, msgId)
        }
        assertFalse(crossDeleteResult.isSuccess)
        val bobError = crossDeleteResult.exceptionOrNull()?.unwrapFirestoreException()
        assertNotNull(bobError)
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, bobError!!.code)

        signInTestUser(ALICE_EMAIL)
        val ownerDeleteResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.deleteMessage(roomKey, msgId)
        }
        assertTrue(ownerDeleteResult.isSuccess)
    }

    @Test
    fun directFirestoreObserve_unauthenticatedUser_failsWithPermissionDenied() = runBlocking {
        auth.signOut()
        val roomKey = LiveChatRepository.sanitizeRoomId("match_${UUID.randomUUID()}")

        val thrown: Throwable = try {
            withTimeout(FLOW_TIMEOUT_MS) {
                firestore.collection("match_rooms")
                    .document(roomKey)
                    .collection("messages")
                    .orderBy("createdAt", Query.Direction.ASCENDING)
                    .limitToLast(20)
                    .snapshots()
                    .first()
            }
            fail("Expected PERMISSION_DENIED for unauthenticated query")
            IllegalStateException("unreachable")
        } catch (e: Exception) {
            e
        }

        val firestoreException = checkNotNull(thrown.unwrapFirestoreException()) {
            "Expected nested FirebaseFirestoreException, got $thrown"
        }
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreException.code)
    }

    private companion object {
        const val ALICE_EMAIL = "alice@test.com"
        const val BOB_EMAIL = "bob@test.com"
        const val DEFAULT_TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 4000L
    }
}
