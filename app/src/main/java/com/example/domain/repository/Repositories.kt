package com.example.domain.repository

import com.example.domain.model.*
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    fun login(serverUrl: String, user: String, pass: String): Flow<Result<User>>
    fun register(serverUrl: String, name: String, email: String, username: String, pass: String): Flow<Result<User>>
    fun logout(): Flow<Result<Unit>>
    fun isLoggedIn(): Flow<Boolean>
    fun getCurrentUser(): Flow<User?>
    fun getAuthToken(): String?
    fun getUserId(): String?
    fun getServerUrl(): String
    fun saveServerUrl(url: String)
}

interface MessageRepository {
    fun getMessagesForChannel(channelId: String): Flow<List<Message>>
    suspend fun sendMessage(channelId: String, text: String, replyToId: String? = null, replyToText: String? = null, replyToSenderName: String? = null, attachment: Attachment? = null): Result<Message>
    suspend fun editMessage(messageId: String, newText: String): Result<Unit>
    suspend fun deleteMessage(messageId: String): Result<Unit>
    suspend fun reactToMessage(messageId: String, emoji: String): Result<Unit>
    suspend fun pinMessage(messageId: String, isPinned: Boolean): Result<Unit>
    suspend fun starMessage(messageId: String, isStarred: Boolean): Result<Unit>
    fun getPinnedMessages(channelId: String): Flow<List<Message>>
    fun getStarredMessages(): Flow<List<Message>>
}

interface ChannelRepository {
    fun getChannels(): Flow<List<Channel>>
    fun getChannelById(channelId: String): Flow<Channel?>
    suspend fun createChannel(name: String, description: String?, topic: String?, isPrivate: Boolean, inviteMembers: List<String>): Result<Channel>
    suspend fun joinChannel(channelId: String): Result<Unit>
    suspend fun leaveChannel(channelId: String): Result<Unit>
    suspend fun toggleMute(channelId: String): Result<Unit>
}

interface UserRepository {
    fun getUsers(): Flow<List<User>>
    fun getUserById(userId: String): Flow<User?>
    suspend fun updateProfile(name: String, username: String, email: String, bio: String): Result<User>
    suspend fun setStatus(status: UserStatus, emoji: String?, text: String?): Result<Unit>
}
