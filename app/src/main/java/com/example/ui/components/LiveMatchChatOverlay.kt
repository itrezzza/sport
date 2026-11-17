package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Forum
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ChatMessage
import com.example.data.network.SportsRepository
import com.example.ui.theme.BroadcastCyan
import com.example.ui.theme.ElectricLime
import com.example.ui.theme.LiveCrimson
import com.example.ui.theme.MidnightBg
import com.example.ui.theme.MidnightBorder
import com.example.ui.theme.VazirmatnFontFamily
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.Locale

sealed interface LiveChatFeedState {
    data object Idle : LiveChatFeedState
    data object Loading : LiveChatFeedState
    data class Ready(val messages: List<ChatMessage>) : LiveChatFeedState
    data class Error(val message: String) : LiveChatFeedState
}

@Composable
fun BoxScope.LiveMatchChatSlideOutOverlay(
    isOpen: Boolean,
    isCompactInline: Boolean,
    programTitle: String,
    currentUserId: String?,
    currentUserName: String?,
    feedState: LiveChatFeedState,
    isSending: Boolean,
    isSigningIn: Boolean,
    authError: String?,
    onGoogleSignIn: () -> Unit,
    onSignOut: () -> Unit,
    onSendMessage: (String) -> Unit,
    onDeleteMessage: (String) -> Unit,
    onClose: () -> Unit
) {
    AnimatedVisibility(
        visible = isOpen,
        enter = slideInHorizontally(
            animationSpec = tween(260),
            initialOffsetX = { -it }
        ) + fadeIn(tween(220)),
        exit = slideOutHorizontally(
            animationSpec = tween(200),
            targetOffsetX = { -it }
        ) + fadeOut(tween(180)),
        modifier = Modifier
            .align(Alignment.CenterEnd)
            .fillMaxHeight()
    ) {
        val listState = rememberLazyListState()
        var draft by remember { mutableStateOf("") }
        val messages = (feedState as? LiveChatFeedState.Ready)?.messages.orEmpty()

        LaunchedEffect(messages.size) {
            if (messages.isNotEmpty()) {
                listState.animateScrollToItem(messages.lastIndex)
            }
        }

        val submitMessage: () -> Unit = {
            val cleaned = draft.trim()
            if (cleaned.isNotEmpty() && !isSending) {
                onSendMessage(cleaned)
                draft = ""
            }
        }

        Surface(
            modifier = Modifier
                .fillMaxHeight()
                .widthIn(min = 236.dp, max = 336.dp)
                .fillMaxWidth(if (isCompactInline) 0.72f else 0.42f)
                .then(
                    if (!isCompactInline) {
                        Modifier
                            .statusBarsPadding()
                            .navigationBarsPadding()
                            .padding(vertical = 8.dp, horizontal = 8.dp)
                    } else {
                        Modifier.padding(vertical = 4.dp, horizontal = 4.dp)
                    }
                )
                .imePadding()
                .testTag("live_chat_slide_out_panel"),
            shape = RoundedCornerShape(16.dp),
            color = MidnightBg.copy(alpha = 0.68f),
            border = BorderStroke(1.dp, ElectricLime.copy(alpha = 0.25f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                MidnightBg.copy(alpha = 0.45f),
                                MidnightBg.copy(alpha = 0.72f)
                            )
                        )
                    )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MidnightBg.copy(alpha = 0.55f))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        LiveBeaconLottie(size = 13.dp)
                        Column {
                            Text(
                                text = "گفتگوی زنده",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White,
                                maxLines = 1
                            )
                            if (programTitle.isNotBlank()) {
                                Text(
                                    text = programTitle,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.68f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (currentUserId != null) {
                            IconButton(
                                onClick = onSignOut,
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .size(36.dp)
                                    .testTag("chat_sign_out_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.Logout,
                                    contentDescription = "خروج از حساب گوگل",
                                    tint = Color.White.copy(alpha = 0.78f),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        IconButton(
                            onClick = onClose,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .size(36.dp)
                                .testTag("chat_overlay_close_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "بستن چت زنده",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                if (currentUserId == null) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = ElectricLime.copy(alpha = 0.16f)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Forum,
                                    contentDescription = null,
                                    tint = ElectricLime,
                                    modifier = Modifier
                                        .padding(10.dp)
                                        .size(24.dp)
                                )
                            }
                            Text(
                                text = "برای چت لحظه‌ای دربارهٔ بازی با حساب گوگل وارد شو",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.92f),
                                textAlign = TextAlign.Center
                            )
                            if (!authError.isNullOrBlank()) {
                                Text(
                                    text = authError,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = LiveCrimson,
                                    textAlign = TextAlign.Center,
                                    maxLines = 2
                                )
                            }
                            Button(
                                onClick = onGoogleSignIn,
                                enabled = !isSigningIn,
                                shape = RoundedCornerShape(99.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                ),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                                modifier = Modifier
                                    .height(44.dp)
                                    .testTag("chat_google_sign_in_button")
                            ) {
                                if (isSigningIn) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Rounded.AccountCircle,
                                        contentDescription = null,
                                        modifier = Modifier.size(17.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "ورود با گوگل",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                        when (feedState) {
                            LiveChatFeedState.Idle, LiveChatFeedState.Loading -> {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                        color = ElectricLime,
                                        strokeWidth = 2.5.dp
                                    )
                                }
                            }

                            is LiveChatFeedState.Error -> {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(12.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = feedState.message,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White.copy(alpha = 0.85f),
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }

                            is LiveChatFeedState.Ready -> {
                                if (messages.isEmpty()) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(14.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "اولین پیام را دربارهٔ این مسابقه بفرست!",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color.White.copy(alpha = 0.78f),
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                } else {
                                    LazyColumn(
                                        state = listState,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .testTag("live_chat_message_list"),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        items(
                                            items = messages,
                                            key = { it.id.ifBlank { "${it.userId}_${it.text.hashCode()}" } }
                                        ) { msg ->
                                            LiveChatBubbleRow(
                                                message = msg,
                                                isMine = msg.userId == currentUserId,
                                                onDelete = {
                                                    if (msg.id.isNotBlank()) {
                                                        onDeleteMessage(msg.id)
                                                    }
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("⚽ گل!", "🔥 حساس شد", "👏 عجب شوتی", "🧤 سیو طلایی").forEach { quick ->
                            Surface(
                                shape = RoundedCornerShape(99.dp),
                                color = Color.White.copy(alpha = 0.14f),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(99.dp))
                                    .clickable(enabled = !isSending) {
                                        onSendMessage(quick)
                                    }
                            ) {
                                Text(
                                    text = quick,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.Black.copy(alpha = 0.34f))
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(99.dp),
                            color = Color.White.copy(alpha = 0.14f),
                            border = BorderStroke(0.6.dp, Color.White.copy(alpha = 0.2f))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 9.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                if (draft.isEmpty()) {
                                    Text(
                                        text = if (!currentUserName.isNullOrBlank()) {
                                            "پیام به نام ${currentUserName.take(16)}…"
                                        } else {
                                            "پیام زنده بنویس…"
                                        },
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White.copy(alpha = 0.56f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                BasicTextField(
                                    value = draft,
                                    onValueChange = { if (it.length <= 300) draft = it },
                                    singleLine = true,
                                    textStyle = TextStyle(
                                        fontFamily = VazirmatnFontFamily,
                                        fontSize = 12.sp,
                                        color = Color.White
                                    ),
                                    cursorBrush = SolidColor(ElectricLime),
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                                    keyboardActions = KeyboardActions(onSend = { submitMessage() }),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("chat_message_input")
                                )
                            }
                        }

                        Surface(
                            shape = CircleShape,
                            color = if (draft.isNotBlank() && !isSending) ElectricLime else Color.White.copy(alpha = 0.18f),
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .clickable(enabled = draft.isNotBlank() && !isSending) {
                                    submitMessage()
                                }
                                .testTag("chat_send_button")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                if (isSending) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        color = Color.White,
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Rounded.Send,
                                        contentDescription = "ارسال پیام",
                                        tint = if (draft.isNotBlank()) Color(0xFF091203) else Color.White.copy(alpha = 0.7f),
                                        modifier = Modifier.size(17.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LiveChatBubbleRow(
    message: ChatMessage,
    isMine: Boolean,
    onDelete: () -> Unit
) {
    val nameAccent = remember(message.userId, isMine) {
        if (isMine) {
            ElectricLime
        } else {
            val palette = listOf(
                BroadcastCyan,
                Color(0xFFFBBF24),
                Color(0xFFF472B6),
                Color(0xFF34D399),
                Color(0xFFA78BFA)
            )
            palette[(message.userId.hashCode() and 0x7FFFFFFF) % palette.size]
        }
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (isMine) ElectricLime.copy(alpha = 0.16f) else MidnightBg.copy(alpha = 0.56f),
        border = BorderStroke(
            0.7.dp,
            if (isMine) ElectricLime.copy(alpha = 0.45f) else MidnightBorder.copy(alpha = 0.65f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("chat_message_${message.id}")
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Text(
                        text = message.senderName.ifBlank { "تماشاگر" },
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = nameAccent,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    val formattedTime = formatChatTime(message.createdAt)
                    if (formattedTime.isNotBlank()) {
                        Text(
                            text = formattedTime,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = Color.White.copy(alpha = 0.5f)
                        )
                    }
                }
                if (isMine && message.id.isNotBlank()) {
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(22.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.DeleteOutline,
                            contentDescription = "حذف پیام",
                            tint = Color.White.copy(alpha = 0.65f),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
            Text(
                text = message.text,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp, lineHeight = 18.sp),
                color = Color.White
            )
        }
    }
}

private fun formatChatTime(timestamp: Timestamp?): String {
    val date = timestamp?.toDate() ?: return ""
    val raw = runCatching {
        SimpleDateFormat("HH:mm", Locale.US).format(date)
    }.getOrDefault("")
    return SportsRepository.toPersianDigits(raw)
}
