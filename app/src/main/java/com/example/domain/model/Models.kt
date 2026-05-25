package com.example.domain.model

enum class UserStatus {
    ONLINE, AWAY, BUSY, OFFLINE
}

enum class ChannelType {
    PUBLIC, PRIVATE
}

enum class MessageStatus {
    SENT, DELIVERED, READ
}

data class User(
    val id: String,
    val username: String,
    val name: String,
    val role: String = "User", // "Admin", "Moderator", "User"
    val status: UserStatus = UserStatus.OFFLINE,
    val customStatusEmoji: String? = null,
    val customStatusText: String? = null,
    val bio: String? = null,
    val timezone: String? = "UTC+0",
    val avatarUrl: String? = null,
    val email: String? = null,
    val lastSeen: String = "recently"
)

data class Channel(
    val id: String,
    val name: String,
    val description: String? = null,
    val topic: String? = null,
    val type: ChannelType = ChannelType.PUBLIC,
    val memberCount: Int = 0,
    val lastMessageText: String? = null,
    val lastMessageTimestamp: Long = 0,
    val unreadCount: Int = 0,
    val isMuted: Boolean = false,
    val isReadOnly: Boolean = false,
    val isEncrypted: Boolean = false
)

data class Reaction(
    val emoji: String,
    val count: Int,
    val userIds: List<String>
)

data class Attachment(
    val name: String,
    val type: String, // "image", "video", "audio", "file"
    val url: String,
    val size: String
)

data class Message(
    val id: String,
    val channelId: String,
    val senderId: String,
    val senderName: String,
    val senderAvatar: String? = null,
    val message: String,
    val timestamp: Long,
    val status: MessageStatus = MessageStatus.SENT,
    val isEdited: Boolean = false,
    val isStarred: Boolean = false,
    val isPinned: Boolean = false,
    val replyToId: String? = null,
    val replyToText: String? = null,
    val replyToSenderName: String? = null,
    val repliesCount: Int = 0,
    val reactions: List<Reaction> = emptyList(),
    val attachment: Attachment? = null,
    val systemType: String? = null // if non-null, e.g. "user_joined", "channel_archived"
)
