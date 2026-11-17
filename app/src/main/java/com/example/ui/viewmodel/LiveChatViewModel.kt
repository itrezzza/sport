package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.ChatMessage
import com.example.data.repository.LiveChatRepository
import com.example.ui.components.LiveChatFeedState
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class LiveChatViewModel(
    private val repository: LiveChatRepository,
    private val currentUserId: String
) : ViewModel() {

    private val _activeRoomKey = MutableStateFlow<String?>(null)
    val activeRoomKey: StateFlow<String?> = _activeRoomKey.asStateFlow()

    private val _isSending = MutableStateFlow(false)
    val isSending: StateFlow<Boolean> = _isSending.asStateFlow()

    private val _actionError = MutableStateFlow<String?>(null)
    val actionError: StateFlow<String?> = _actionError.asStateFlow()

    val feedState: StateFlow<LiveChatFeedState> = _activeRoomKey
        .flatMapLatest { roomKey ->
            if (roomKey.isNullOrBlank()) {
                flowOf(LiveChatFeedState.Idle)
            } else {
                repository.observeRoomMessages(roomKey)
                    .map<List<ChatMessage>, LiveChatFeedState> { LiveChatFeedState.Ready(it) }
                    .catch { error ->
                        val firestoreError = generateSequence(error) { it.cause }
                            .filterIsInstance<FirebaseFirestoreException>()
                            .firstOrNull()
                        if (firestoreError == null && error is CancellationException) throw error
                        val rootError = firestoreError ?: error
                        Log.w(TAG, "Error observing live match chat for $roomKey", rootError)
                        emit(
                            LiveChatFeedState.Error(
                                "امکان دریافت گفتگوهای زنده وجود ندارد: ${rootError.message ?: "خطا"}"
                            )
                        )
                    }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = LiveChatFeedState.Idle
        )

    fun setRoom(roomKey: String?) {
        val cleaned = roomKey?.trim()?.takeIf { it.isNotEmpty() }
        if (_activeRoomKey.value != cleaned) {
            _activeRoomKey.value = cleaned
        }
    }

    fun sendMessage(
        text: String,
        senderNameOverride: String? = null,
        onFailure: (String) -> Unit = {}
    ) {
        val roomKey = _activeRoomKey.value ?: return
        if (_isSending.value) return
        viewModelScope.launch {
            _isSending.value = true
            _actionError.value = null
            val res = repository.sendMessage(
                rawRoomId = roomKey,
                text = text,
                senderNameOverride = senderNameOverride
            )
            _isSending.value = false
            res.onFailure { err ->
                val msg = err.localizedMessage ?: "ارسال پیام ناموفق بود."
                _actionError.value = msg
                onFailure(msg)
            }
        }
    }

    fun deleteMessage(messageId: String, onFailure: (String) -> Unit = {}) {
        val roomKey = _activeRoomKey.value ?: return
        viewModelScope.launch {
            val res = repository.deleteMessage(roomKey, messageId)
            res.onFailure { err ->
                onFailure(err.localizedMessage ?: "حذف پیام ناموفق بود.")
            }
        }
    }

    private companion object {
        const val TAG = "LiveChatVM"
    }
}
