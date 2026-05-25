package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.domain.model.ChannelType
import com.example.domain.model.MessageStatus
import com.example.domain.model.UserStatus

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val id: String,
    val username: String,
    val name: String,
    val role: String,
    val status: String, // UserStatus string
    val customStatusEmoji: String?,
    val customStatusText: String?,
    val bio: String?,
    val timezone: String?,
    val avatarUrl: String?,
    val email: String?,
    val lastSeen: String
)

@Entity(tableName = "channels")
data class ChannelEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String?,
    val topic: String?,
    val type: String, // ChannelType string
    val memberCount: Int,
    val lastMessageText: String?,
    val lastMessageTimestamp: Long,
    val unreadCount: Int,
    val isMuted: Boolean,
    val isReadOnly: Boolean,
    val isEncrypted: Boolean
)

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey val id: String,
    val channelId: String,
    val senderId: String,
    val senderName: String,
    val senderAvatar: String?,
    val message: String,
    val timestamp: Long,
    val status: String, // MessageStatus string
    val isEdited: Boolean,
    val isStarred: Boolean,
    val isPinned: Boolean,
    val replyToId: String?,
    val replyToText: String?,
    val replyToSenderName: String?,
    val repliesCount: Int,
    val reactionsJson: String, // List<Reaction> as JSON
    val attachmentJson: String?, // Attachment? as JSON
    val systemType: String?
)
