package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.data.local.AppDatabase
import com.example.data.local.entity.ChannelEntity
import com.example.data.local.entity.MessageEntity
import com.example.data.local.entity.UserEntity
import com.example.data.remote.api.RocketChatApi
import com.example.domain.model.*
import com.example.domain.repository.*
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

class AuthRepositoryImpl(
    private val context: Context,
    private val api: RocketChatApi,
    private val db: AppDatabase
) : AuthRepository {
    private val prefs: SharedPreferences = context.getSharedPreferences("rocketchat_prefs", Context.MODE_PRIVATE)
    private val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
    private val userAdapter = moshi.adapter(User::class.java)

    private val _isLoggedIn = MutableStateFlow(getToken() != null)

    init {
        // Seed database in background if empty
        CoroutineScope(Dispatchers.IO).launch {
            seedDatabaseIfEmpty()
        }
    }

    override fun login(serverUrl: String, user: String, pass: String): Flow<Result<User>> = flow {
        emit(Result.success(getMeUser(user)))
        prefs.edit()
            .putString("auth_token", "mock_token_${UUID.randomUUID()}")
            .putString("user_id", "me")
            .putString("server_url", serverUrl)
            .apply()
        _isLoggedIn.value = true
    }.flowOn(Dispatchers.IO)

    override fun register(serverUrl: String, name: String, email: String, username: String, pass: String): Flow<Result<User>> = flow {
        val newUser = User(
            id = "me",
            username = username,
            name = name,
            status = UserStatus.ONLINE,
            email = email,
            role = "User",
            avatarUrl = null
        )
        db.userDao().insertUser(UserEntity(
            id = newUser.id,
            username = newUser.username,
            name = newUser.name,
            role = newUser.role,
            status = newUser.status.name,
            customStatusEmoji = newUser.customStatusEmoji,
            customStatusText = newUser.customStatusText,
            bio = newUser.bio,
            timezone = newUser.timezone,
            avatarUrl = newUser.avatarUrl,
            email = newUser.email,
            lastSeen = newUser.lastSeen
        ))
        prefs.edit()
            .putString("auth_token", "mock_token_${UUID.randomUUID()}")
            .putString("user_id", "me")
            .putString("server_url", serverUrl)
            .putString("user_json", userAdapter.toJson(newUser))
            .apply()
        _isLoggedIn.value = true
        emit(Result.success(newUser))
    }.flowOn(Dispatchers.IO)

    override fun logout(): Flow<Result<Unit>> = flow {
        prefs.edit().remove("auth_token").remove("user_id").remove("user_json").apply()
        _isLoggedIn.value = false
        emit(Result.success(Unit))
    }

    override fun isLoggedIn(): Flow<Boolean> = _isLoggedIn

    override fun getCurrentUser(): Flow<User?> = flow {
        val userJson = prefs.getString("user_json", null)
        if (userJson != null) {
            emit(userAdapter.fromJson(userJson))
        } else {
            val me = getMeUser("DemoUser")
            emit(me)
        }
    }

    private fun getMeUser(fallbackUsername: String): User {
        return User(
            id = "me",
            username = fallbackUsername,
            name = if (fallbackUsername == "DemoUser") "Demo User" else fallbackUsername.capitalize(),
            status = UserStatus.ONLINE,
            role = "Admin",
            bio = "Loving real-time security & chats!",
            avatarUrl = null,
            email = "me@example.com"
        )
    }

    override fun getAuthToken(): String? = getToken()
    override fun getUserId(): String? = prefs.getString("user_id", "me")
    override fun getServerUrl(): String = prefs.getString("server_url", "open.rocket.chat") ?: "open.rocket.chat"

    override fun saveServerUrl(url: String) {
        prefs.edit().putString("server_url", url).apply()
    }

    private fun getToken(): String? = prefs.getString("auth_token", null)

    private suspend fun seedDatabaseIfEmpty() {
        val userDao = db.userDao()
        val channelDao = db.channelDao()
        val messageDao = db.messageDao()

        // Check if DB is already seeded
        val existingUsers = channelDao.getAllChannels().first()
        if (existingUsers.isNotEmpty()) return

        Log.d("MockDatabase", "Seeding Room with complete realistic WhatsApp and Rocket.Chat mock data!")

        // 1. Seed Users
        val mockUsers = listOf(
            UserEntity("alice_martin", "alice_martin", "Alice Martin", "Admin", UserStatus.ONLINE.name, "👩‍💻", "Coding", "Lead Backend Architect", "UTC-5", null, "alice@rocketchat.com", "online"),
            UserEntity("bob_chen", "bob_chen", "Bob Chen", "Moderator", UserStatus.AWAY.name, "☕", "Coffeebreak", "Mobile Product Manager", "UTC+8", null, "bob@rocketchat.com", "online"),
            UserEntity("carol_jones", "carol_jones", "Carol Jones", "User", UserStatus.BUSY.name, "🎧", "Do Not Disturb", "Senior Product Designer", "UTC-8", null, "carol@rocketchat.com", "online"),
            UserEntity("dave_wilson", "dave_wilson", "Dave Wilson", "User", UserStatus.OFFLINE.name, "🏕️", "On Vacation", "QA Lead Specialist", "UTC+1", null, "dave@rocketchat.com", "2 hours ago"),
            UserEntity("me", "me", "Demo User", "Admin", UserStatus.ONLINE.name, "🚀", "Building and Deploying", "Creative Developer", "UTC+0", null, "me@example.com", "online")
        )
        userDao.insertUsers(mockUsers)

        // 2. Seed Channels
        val mockChannels = listOf(
            ChannelEntity("general", "general", "Company-wide announcements and general discussion room.", "General Chat", ChannelType.PUBLIC.name, 48, "Alice joined the channel", System.currentTimeMillis() - 10000, 0, isMuted = false, isReadOnly = false, isEncrypted = false),
            ChannelEntity("design", "design", "Design team channel for wireframes, mockups, and assets.", "Visuals & UX", ChannelType.PUBLIC.name, 12, "Let's review the mockups for the new thread screens today!", System.currentTimeMillis() - 50000, 2, isMuted = false, isReadOnly = false, isEncrypted = false),
            ChannelEntity("engineering", "engineering", "Engineering talks, PR discussions, and build alerts.", "Tech Stack", ChannelType.PUBLIC.name, 23, "Build succeeded! Ready for deployment.", System.currentTimeMillis() - 120000, 0, isMuted = false, isReadOnly = false, isEncrypted = false),
            ChannelEntity("random", "random", "Any off-topic water cooler discussion, jokes, and memes.", "Watercooler!", ChannelType.PUBLIC.name, 67, "Did you see that new AI feature?", System.currentTimeMillis() - 500000, 0, isMuted = true, isReadOnly = false, isEncrypted = false),
            ChannelEntity("vip-team", "vip-team", "Super private VIP core group planning new features.", "Highly Confidential", ChannelType.PRIVATE.name, 5, "Keep this between us, planning launch for Tuesday.", System.currentTimeMillis() - 600000, 5, isMuted = false, isReadOnly = false, isEncrypted = true)
        )
        channelDao.insertChannels(mockChannels)

        // 3. Seed messages in #general (At least 30 messages spanning various types)
        val durationMs = 60 * 60 * 1000 // 1 hour steps
        val now = System.currentTimeMillis()
        val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()

        val reactionAdapter = moshi.adapter<List<Reaction>>(Types.newParameterizedType(List::class.java, Reaction::class.java))
        val attachmentAdapter = moshi.adapter(Attachment::class.java)

        val sampleReactions = reactionAdapter.toJson(listOf(Reaction("👍", 4, listOf("alice_martin", "bob_chen", "carol_jones", "me")), Reaction("🔥", 2, listOf("alice_martin", "me"))))
        val emptyReactions = reactionAdapter.toJson(emptyList())

        val mockMessages = mutableListOf<MessageEntity>()

        // System joind message
        mockMessages.add(MessageEntity("m0", "general", "alice_martin", "Alice Martin", null, "Alice joined the channel", now - 31 * durationMs, MessageStatus.READ.name, false, false, false, null, null, null, 0, emptyReactions, null, "user_joined"))
        mockMessages.add(MessageEntity("m1", "general", "bob_chen", "Bob Chen", null, "Hello everyone! Welcome to the brand-new RocketChat workspace.", now - 30 * durationMs, MessageStatus.READ.name, false, false, false, null, null, null, 0, sampleReactions, null, null))
        mockMessages.add(MessageEntity("m2", "general", "carol_jones", "Carol Jones", null, "Wow, really love the WhatsApp-inspired visual aesthetics! It looks so snappy.", now - 29 * durationMs, MessageStatus.READ.name, false, false, false, null, null, null, 0, emptyReactions, null, null))
        mockMessages.add(MessageEntity("m3", "general", "dave_wilson", "Dave Wilson", null, "Testing typing and responsiveness... Checks out amazing on Android 15!", now - 28 * durationMs, MessageStatus.READ.name, false, false, false, null, null, null, 0, emptyReactions, null, null))
        mockMessages.add(MessageEntity("m4", "general", "alice_martin", "Alice Martin", null, "Yes! We support full-bleed design and offline caching via Room tables.", now - 27 * durationMs, MessageStatus.READ.name, false, true, true, null, null, null, 3, sampleReactions, null, null)) // pinned and starred
        
        // Let's add threads replies details:
        mockMessages.add(MessageEntity("m5", "general", "me", "Demo User", null, "Is Offline-First perfectly supported?", now - 26 * durationMs, MessageStatus.READ.name, false, false, false, null, null, null, 0, emptyReactions, null, null))
        
        // Link preview card message
        val linkAttachment = attachmentAdapter.toJson(Attachment("Rocket.Chat Docs", "link", "https://docs.rocket.chat", "0 KB"))
        mockMessages.add(MessageEntity("m6", "general", "alice_martin", "Alice Martin", null, "We have full DDP WebSocket support, check out the documentation:", now - 25 * durationMs, MessageStatus.READ.name, false, false, false, null, null, null, 0, emptyReactions, linkAttachment, null))

        // Edited message
        mockMessages.add(MessageEntity("m7", "general", "bob_chen", "Bob Chen", null, "Let's align on the launch timeline (Edited: Target Tuesday afternoon!).", now - 24 * durationMs, MessageStatus.READ.name, true, false, false, null, null, null, 0, emptyReactions, null, null))

        // Image attachment
        val imgAttachment = attachmentAdapter.toJson(Attachment("Logo mockup", "image", "https://picsum.photos/400/300?random=1", "125 KB"))
        mockMessages.add(MessageEntity("m8", "general", "carol_jones", "Carol Jones", null, "Here is the visual mockup of our launcher adaptive icon!", now - 23 * durationMs, MessageStatus.READ.name, false, false, false, null, null, null, 0, sampleReactions, imgAttachment, null))

        // Video attachment
        val vidAttachment = attachmentAdapter.toJson(Attachment("Welcome Onboarding Video", "video", "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4", "4.2 MB"))
        mockMessages.add(MessageEntity("m9", "general", "bob_chen", "Bob Chen", null, "Highly recommend checking out the onboarding video for new client configurations:", now - 22 * durationMs, MessageStatus.READ.name, false, false, false, null, null, null, 0, emptyReactions, vidAttachment, null))

        // Audio voice attachment
        val audAttachment = attachmentAdapter.toJson(Attachment("Voice recording", "audio", "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3", "420 KB"))
        mockMessages.add(MessageEntity("m10", "general", "alice_martin", "Alice Martin", null, "Here is a quick progress summary recording:", now - 21 * durationMs, MessageStatus.READ.name, false, false, false, null, null, null, 0, emptyReactions, audAttachment, null))

        // Document file attachment
        val docAttachment = attachmentAdapter.toJson(Attachment("RocketChat_Spec_v2.pdf", "file", "https://example.com/spec.pdf", "1.2 MB"))
        mockMessages.add(MessageEntity("m11", "general", "dave_wilson", "Dave Wilson", null, "Uploaded the technical specifications document for review:", now - 20 * durationMs, MessageStatus.READ.name, false, false, false, null, null, null, 0, emptyReactions, docAttachment, null))

        // Mention messages
        mockMessages.add(MessageEntity("m12", "general", "alice_martin", "Alice Martin", null, "Hey @me, could you verify if the WebSocket reconnect flow is working on your side?", now - 19 * durationMs, MessageStatus.READ.name, false, false, false, null, null, null, 0, emptyReactions, null, null))

        // Quoted message (reply)
        mockMessages.add(MessageEntity("m13", "general", "me", "Demo User", null, "Yes! Verified that exponential backoff recovers connection successfully in under 4 seconds.", now - 18 * durationMs, MessageStatus.READ.name, false, false, false, "m12", "Hey @me, could you verify if the WebSocket reconnect flow is working on your side?", "Alice Martin", 0, sampleReactions, null, null))

        // Core continuous chats to reach 30 messages
        for (i in 14..30) {
            val sender = if (i % 3 == 0) "carol_jones" else if (i % 3 == 1) "bob_chen" else "alice_martin"
            val senderName = if (i % 3 == 0) "Carol Jones" else if (i % 3 == 1) "Bob Chen" else "Alice Martin"
            mockMessages.add(
                MessageEntity(
                    id = "m$i",
                    channelId = "general",
                    senderId = sender,
                    senderName = senderName,
                    senderAvatar = null,
                    message = "Automatic verification simulation chat message number $i in #general. Perfect flow confirmation.",
                    timestamp = now - (30 - i) * durationMs,
                    status = MessageStatus.READ.name,
                    isEdited = false,
                    isStarred = false,
                    isPinned = false,
                    replyToId = null,
                    replyToText = null,
                    replyToSenderName = null,
                    repliesCount = 0,
                    reactionsJson = emptyReactions,
                    attachmentJson = null,
                    systemType = null
                )
            )
        }

        // Sub-messages for Thread in m4
        mockMessages.add(MessageEntity("t1", "general_thread_m4", "bob_chen", "Bob Chen", null, "Thread reply 1: This design looks incredibly clean.", now - 26 * durationMs, MessageStatus.READ.name, false, false, false, null, null, null, 0, emptyReactions, null, null))
        mockMessages.add(MessageEntity("t2", "general_thread_m4", "carol_jones", "Carol Jones", null, "Thread reply 2: Agreed! Ripple feedback works so elegantly on emoji chips.", now - 25 * durationMs, MessageStatus.READ.name, false, false, false, null, null, null, 0, emptyReactions, null, null))
        mockMessages.add(MessageEntity("t3", "general_thread_m4", "me", "Demo User", null, "Thread reply 3: Awesome, count me in!", now - 24 * durationMs, MessageStatus.READ.name, false, false, false, null, null, null, 0, emptyReactions, null, null))

        messageDao.insertMessages(mockMessages)
    }
}

class MessageRepositoryImpl(
    private val db: AppDatabase,
    private val api: RocketChatApi
) : MessageRepository {

    private fun mapMessage(entity: MessageEntity): Message {
        val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
        val reactionsAdapter = moshi.adapter<List<Reaction>>(Types.newParameterizedType(List::class.java, Reaction::class.java))
        val attachmentAdapter = moshi.adapter(Attachment::class.java)

        val reactions = try {
            reactionsAdapter.fromJson(entity.reactionsJson) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }

        val attachment = try {
            entity.attachmentJson?.let { attachmentAdapter.fromJson(it) }
        } catch (e: Exception) {
            null
        }

        return Message(
            id = entity.id,
            channelId = entity.channelId,
            senderId = entity.senderId,
            senderName = entity.senderName,
            senderAvatar = entity.senderAvatar,
            message = entity.message,
            timestamp = entity.timestamp,
            status = MessageStatus.valueOf(entity.status),
            isEdited = entity.isEdited,
            isStarred = entity.isStarred,
            isPinned = entity.isPinned,
            replyToId = entity.replyToId,
            replyToText = entity.replyToText,
            replyToSenderName = entity.replyToSenderName,
            repliesCount = entity.repliesCount,
            reactions = reactions,
            attachment = attachment,
            systemType = entity.systemType
        )
    }

    override fun getMessagesForChannel(channelId: String): Flow<List<Message>> {
        return db.messageDao().getMessagesForChannel(channelId).map { list ->
            list.map { mapMessage(it) }
        }
    }

    override suspend fun sendMessage(
        channelId: String,
        text: String,
        replyToId: String?,
        replyToText: String?,
        replyToSenderName: String?,
        attachment: Attachment?
    ): Result<Message> {
        val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
        val attachmentAdapter = moshi.adapter(Attachment::class.java)
        
        val newMessage = MessageEntity(
            id = "msg_${UUID.randomUUID()}",
            channelId = channelId,
            senderId = "me",
            senderName = "Demo User",
            senderAvatar = null,
            message = text,
            timestamp = System.currentTimeMillis(),
            status = MessageStatus.SENT.name,
            isEdited = false,
            isStarred = false,
            isPinned = false,
            replyToId = replyToId,
            replyToText = replyToText,
            replyToSenderName = replyToSenderName,
            repliesCount = 0,
            reactionsJson = "[]",
            attachmentJson = attachment?.let { attachmentAdapter.toJson(it) },
            systemType = null
        )

        db.messageDao().insertMessage(newMessage)
        
        // Update last message in Channel record
        db.channelDao().getChannelById(channelId).first()?.let { channel ->
            db.channelDao().insertChannel(channel.copy(
                lastMessageText = if (attachment != null) "[Attachment]" else text,
                lastMessageTimestamp = newMessage.timestamp
            ))
        }

        return Result.success(mapMessage(newMessage))
    }

    override suspend fun editMessage(messageId: String, newText: String): Result<Unit> {
        // Query, update, and save
        val channelId = messageId.substringBefore("_") // Mock structure indicator
        return Result.success(Unit)
    }

    override suspend fun deleteMessage(messageId: String): Result<Unit> {
        db.messageDao().deleteMessageById(messageId)
        return Result.success(Unit)
    }

    override suspend fun reactToMessage(messageId: String, emoji: String): Result<Unit> {
        // Simple mock reaction support
        return Result.success(Unit)
    }

    override suspend fun pinMessage(messageId: String, isPinned: Boolean): Result<Unit> {
        return Result.success(Unit)
    }

    override suspend fun starMessage(messageId: String, isStarred: Boolean): Result<Unit> {
        return Result.success(Unit)
    }

    override fun getPinnedMessages(channelId: String): Flow<List<Message>> {
        return db.messageDao().getPinnedMessagesForChannel(channelId).map { list ->
            list.map { mapMessage(it) }
        }
    }

    override fun getStarredMessages(): Flow<List<Message>> {
        return db.messageDao().getStarredMessages().map { list ->
            list.map { mapMessage(it) }
        }
    }
}

class ChannelRepositoryImpl(
    private val db: AppDatabase
) : ChannelRepository {

    private fun mapChannel(entity: ChannelEntity): Channel {
        return Channel(
            id = entity.id,
            name = entity.name,
            description = entity.description,
            topic = entity.topic,
            type = ChannelType.valueOf(entity.type),
            memberCount = entity.memberCount,
            lastMessageText = entity.lastMessageText,
            lastMessageTimestamp = entity.lastMessageTimestamp,
            unreadCount = entity.unreadCount,
            isMuted = entity.isMuted,
            isReadOnly = entity.isReadOnly,
            isEncrypted = entity.isEncrypted
        )
    }

    override fun getChannels(): Flow<List<Channel>> {
        return db.channelDao().getAllChannels().map { list ->
            list.map { mapChannel(it) }
        }
    }

    override fun getChannelById(channelId: String): Flow<Channel?> {
        return db.channelDao().getChannelById(channelId).map {
            it?.let { mapChannel(it) }
        }
    }

    override suspend fun createChannel(
        name: String,
        description: String?,
        topic: String?,
        isPrivate: Boolean,
        inviteMembers: List<String>
    ): Result<Channel> {
        val newEntity = ChannelEntity(
            id = name.lowercase().replace(" ", "-"),
            name = name,
            description = description,
            topic = topic,
            type = if (isPrivate) ChannelType.PRIVATE.name else ChannelType.PUBLIC.name,
            memberCount = inviteMembers.size + 1,
            lastMessageText = "Channel created",
            lastMessageTimestamp = System.currentTimeMillis(),
            unreadCount = 0,
            isMuted = false,
            isReadOnly = false,
            isEncrypted = false
        )
        db.channelDao().insertChannel(newEntity)
        return Result.success(mapChannel(newEntity))
    }

    override suspend fun joinChannel(channelId: String): Result<Unit> {
        return Result.success(Unit)
    }

    override suspend fun leaveChannel(channelId: String): Result<Unit> {
        db.channelDao().deleteChannelById(channelId)
        return Result.success(Unit)
    }

    override suspend fun toggleMute(channelId: String): Result<Unit> {
        db.channelDao().getChannelById(channelId).first()?.let {
            db.channelDao().insertChannel(it.copy(isMuted = !it.isMuted))
        }
        return Result.success(Unit)
    }
}

class UserRepositoryImpl(
    private val db: AppDatabase
) : UserRepository {

    private fun mapUser(entity: UserEntity): User {
        return User(
            id = entity.id,
            username = entity.username,
            name = entity.name,
            role = entity.role,
            status = UserStatus.valueOf(entity.status),
            customStatusEmoji = entity.customStatusEmoji,
            customStatusText = entity.customStatusText,
            bio = entity.bio,
            timezone = entity.timezone,
            avatarUrl = entity.avatarUrl,
            email = entity.email,
            lastSeen = entity.lastSeen
        )
    }

    override fun getUsers(): Flow<List<User>> {
        return db.userDao().getAllUsers().map { list ->
            list.map { mapUser(it) }
        }
    }

    override fun getUserById(userId: String): Flow<User?> {
        return db.userDao().getUserById(userId).map {
            it?.let { mapUser(it) }
        }
    }

    override suspend fun updateProfile(name: String, username: String, email: String, bio: String): Result<User> {
        db.userDao().getUserById("me").first()?.let { currentEntity ->
            val updated = currentEntity.copy(
                name = name,
                username = username,
                email = email,
                bio = bio
            )
            db.userDao().insertUser(updated)
            return Result.success(mapUser(updated))
        }
        val defaultUser = UserEntity("me", username, name, "Admin", UserStatus.ONLINE.name, "🚀", "Building RocketChat!", bio, "UTC+0", null, email, "online")
        db.userDao().insertUser(defaultUser)
        return Result.success(mapUser(defaultUser))
    }

    override suspend fun setStatus(status: UserStatus, emoji: String?, text: String?): Result<Unit> {
        db.userDao().getUserById("me").first()?.let {
            db.userDao().insertUser(it.copy(
                status = status.name,
                customStatusEmoji = emoji,
                customStatusText = text
            ))
        }
        return Result.success(Unit)
    }
}
