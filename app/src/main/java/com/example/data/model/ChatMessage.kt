package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class ChatMessage(
    @DocumentId val id: String = "",
    val roomId: String = "",
    val userId: String = "",
    val senderName: String = "",
    val photoUrl: String = "",
    val text: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)
