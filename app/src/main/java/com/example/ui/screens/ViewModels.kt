package com.example.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.domain.model.*
import com.example.domain.repository.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

// Splash Screen ViewModel
class SplashViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {
    private val _navigateToMatch = MutableSharedFlow<String>()
    val navigateToMatch: SharedFlow<String> = _navigateToMatch

    init {
        checkAuth()
    }

    private fun checkAuth() {
        viewModelScope.launch {
            kotlinx.coroutines.delay(1500) // 1.5s splash animation delay
            authRepository.isLoggedIn().collectLatest { isLoggedIn ->
                if (isLoggedIn) {
                    _navigateToMatch.emit("home")
                } else {
                    _navigateToMatch.emit("login")
                }
            }
        }
    }
}

// Authentication ViewModel (Login, Register & Password Recovery)
class AuthViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {
    var serverUrl = MutableStateFlow("open.rocket.chat")
    var email = MutableStateFlow("")
    var username = MutableStateFlow("")
    var password = MutableStateFlow("")
    var confirmPassword = MutableStateFlow("")
    var displayName = MutableStateFlow("")
    var rememberMe = MutableStateFlow(true)
    var isPasswordVisible = MutableStateFlow(false)
    var termsAccepted = MutableStateFlow(false)

    private val _uiState = MutableStateFlow<UiState<User>>(UiState.Idle)
    val uiState: StateFlow<UiState<User>> = _uiState

    fun togglePasswordVisibility() {
        isPasswordVisible.value = !isPasswordVisible.value
    }

    fun login() {
        if (email.value.isEmpty() || password.value.isEmpty()) {
            _uiState.value = UiState.Error("Please enter all login credentials")
            return
        }
        _uiState.value = UiState.Loading
        viewModelScope.launch {
            authRepository.login(serverUrl.value, email.value, password.value)
                .catch { _uiState.value = UiState.Error(it.message ?: "Login Failed") }
                .collect { result ->
                    result.fold(
                        onSuccess = { _uiState.value = UiState.Success(it) },
                        onFailure = { _uiState.value = UiState.Error(it.message ?: "Authentication failed") }
                    )
                }
        }
    }

    fun register() {
        if (displayName.value.isEmpty() || username.value.isEmpty() || email.value.isEmpty() || password.value.isEmpty()) {
            _uiState.value = UiState.Error("Please fill in all registration fields")
            return
        }
        if (password.value != confirmPassword.value) {
            _uiState.value = UiState.Error("Passwords do not match")
            return
        }
        if (!termsAccepted.value) {
            _uiState.value = UiState.Error("Please accept Terms and Privacy Policy")
            return
        }
        _uiState.value = UiState.Loading
        viewModelScope.launch {
            authRepository.register(serverUrl.value, displayName.value, email.value, username.value, password.value)
                .catch { _uiState.value = UiState.Error(it.message ?: "Registration Failed") }
                .collect { result ->
                    result.fold(
                        onSuccess = { _uiState.value = UiState.Success(it) },
                        onFailure = { _uiState.value = UiState.Error(it.message ?: "Create account failed") }
                    )
                }
        }
    }

    fun forgotPassword() {
        if (email.value.isEmpty()) {
            _uiState.value = UiState.Error("Please provide your email address")
            return
        }
        _uiState.value = UiState.Loading
        viewModelScope.launch {
            kotlinx.coroutines.delay(1000)
            _uiState.value = UiState.Error("Reset instructions sent! Please verify your inbox.")
        }
    }

    fun resetState() {
        _uiState.value = UiState.Idle
    }
}

// Main Home Screen State Management (Unified tabs: Chats / Channels / DMs / Mentions)
class HomeViewModel(
    private val channelRepository: ChannelRepository,
    private val userRepository: UserRepository,
    private val messageRepository: MessageRepository
) : ViewModel() {

    val channels: StateFlow<List<Channel>> = channelRepository.getChannels()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val users: StateFlow<List<User>> = userRepository.getUsers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val chats: StateFlow<List<Channel>> = channels.map { list ->
        list.sortedByDescending { it.lastMessageTimestamp }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val dms: StateFlow<List<Channel>> = channels.map { list ->
        list.filter { it.type == ChannelType.PRIVATE }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val starredMessages: StateFlow<List<Message>> = messageRepository.getStarredMessages()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun toggleMuteChannel(channelId: String) {
        viewModelScope.launch {
            channelRepository.toggleMute(channelId)
        }
    }
}

// Chat screen (Detailed chat messaging logic)
class ChatViewModel(
    private val channelId: String,
    private val messageRepository: MessageRepository,
    private val channelRepository: ChannelRepository
) : ViewModel() {

    val channel: StateFlow<Channel?> = channelRepository.getChannelById(channelId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val messages: StateFlow<List<Message>> = messageRepository.getMessagesForChannel(channelId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val pinnedMessages: StateFlow<List<Message>> = messageRepository.getPinnedMessages(channelId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    var replyMessage = MutableStateFlow<Message?>(null)
    var textInput = MutableStateFlow("")
    var isTyping = MutableStateFlow(false)
    var typingUser = MutableStateFlow<String?>(null)

    // Draft state preservation per channel
    companion object {
        private val drafts = mutableMapOf<String, String>()
    }

    init {
        textInput.value = drafts[channelId] ?: ""
    }

    fun saveDraft(text: String) {
        drafts[channelId] = text
        textInput.value = text
    }

    fun toggleTyping(active: Boolean) {
        isTyping.value = active
        typingUser.value = if (active) "Alice" else null
    }

    fun sendMessage() {
        val text = textInput.value.trim()
        if (text.isEmpty() && replyMessage.value == null) return

        viewModelScope.launch {
            val reply = replyMessage.value
            messageRepository.sendMessage(
                channelId = channelId,
                text = text,
                replyToId = reply?.id,
                replyToText = reply?.message,
                replyToSenderName = reply?.senderName
            )
            textInput.value = ""
            drafts.remove(channelId)
            replyMessage.value = null
        }
    }

    fun sendAudioMessage(recordingPath: String) {
        viewModelScope.launch {
            messageRepository.sendMessage(
                channelId = channelId,
                text = "Sent voice note...",
                attachment = Attachment("Voice Note", "audio", recordingPath, "180 KB")
            )
        }
    }

    fun sendFileAttachment(name: String, path: String, size: String, type: String) {
        viewModelScope.launch {
            messageRepository.sendMessage(
                channelId = channelId,
                text = "Shared item: $name",
                attachment = Attachment(name, type, path, size)
            )
        }
    }

    fun setReplyTo(message: Message?) {
        replyMessage.value = message
    }

    fun deleteMessage(messageId: String) {
        viewModelScope.launch {
            messageRepository.deleteMessage(messageId)
        }
    }

    fun addReaction(messageId: String, emoji: String) {
        viewModelScope.launch {
            messageRepository.reactToMessage(messageId, emoji)
        }
    }
}

// Search ViewModel
class SearchViewModel(
    private val messageRepository: MessageRepository,
    private val channelRepository: ChannelRepository,
    private val userRepository: UserRepository
) : ViewModel() {
    val searchQuery = MutableStateFlow("")
    val activeFilter = MutableStateFlow("All") // "All", "Messages", "Files", "Channels", "People"

    private val _searchResults = MutableStateFlow<List<Any>>(emptyList())
    val searchResults: StateFlow<List<Any>> = _searchResults

    init {
        // Simple search query evaluation
        searchQuery
            .debounce(300)
            .combine(activeFilter) { query, filter ->
                if (query.isEmpty()) {
                    _searchResults.value = emptyList()
                    return@combine
                }
                
                viewModelScope.launch {
                    val results = mutableListOf<Any>()
                    // Filter and add channels
                    if (filter == "All" || filter == "Channels") {
                        val channels = channelRepository.getChannels().first()
                        results.addAll(channels.filter { it.name.contains(query, ignoreCase = true) })
                    }
                    // Filter and add users
                    if (filter == "All" || filter == "People") {
                        val users = userRepository.getUsers().first()
                        results.addAll(users.filter { it.name.contains(query, ignoreCase = true) || it.username.contains(query, ignoreCase = true) })
                    }
                    _searchResults.value = results
                }
            }.launchIn(viewModelScope)
    }
}

// Profile Information ViewModel (Me / Own & other Users)
class ProfileViewModel(
    private val userRepository: UserRepository
) : ViewModel() {
    val currentUser: StateFlow<User?> = userRepository.getUserById("me")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    var name = MutableStateFlow("")
    var username = MutableStateFlow("")
    var email = MutableStateFlow("")
    var bio = MutableStateFlow("")
    val isSaving = MutableStateFlow(false)

    val customStatusEmoji = MutableStateFlow("🚀")
    val customStatusText = MutableStateFlow("Coding standard applets")
    val presenceState = MutableStateFlow(UserStatus.ONLINE)

    init {
        viewModelScope.launch {
            currentUser.collectLatest { user ->
                user?.let {
                    name.value = it.name
                    username.value = it.username
                    email.value = it.email ?: "me@example.com"
                    bio.value = it.bio ?: ""
                    customStatusEmoji.value = it.customStatusEmoji ?: "🚀"
                    customStatusText.value = it.customStatusText ?: ""
                    presenceState.value = it.status
                }
            }
        }
    }

    fun saveProfile() {
        isSaving.value = true
        viewModelScope.launch {
            userRepository.updateProfile(name.value, username.value, email.value, bio.value)
            userRepository.setStatus(presenceState.value, customStatusEmoji.value, customStatusText.value)
            kotlinx.coroutines.delay(800)
            isSaving.value = false
        }
    }

    fun updatePresence(status: UserStatus) {
        presenceState.value = status
        viewModelScope.launch {
            userRepository.setStatus(status, customStatusEmoji.value, customStatusText.value)
        }
    }
}

// Detailed Channel Info, members, pinned feed VM
class ChannelInfoViewModel(
    private val channelId: String,
    private val channelRepository: ChannelRepository,
    private val userRepository: UserRepository,
    private val messageRepository: MessageRepository
) : ViewModel() {
    val channel: StateFlow<Channel?> = channelRepository.getChannelById(channelId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val members: StateFlow<List<User>> = userRepository.getUsers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val pinnedMessages: StateFlow<List<Message>> = messageRepository.getPinnedMessages(channelId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val starredMessages: StateFlow<List<Message>> = messageRepository.getStarredMessages()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun leaveChannel() {
        viewModelScope.launch {
            channelRepository.leaveChannel(channelId)
        }
    }
}

// Moderation Logs & System Admin Panel ViewModel
class AdminViewModel(
    private val userRepository: UserRepository,
    private val channelRepository: ChannelRepository
) : ViewModel() {
    val totalUsers = MutableStateFlow(48)
    val onlineNow = MutableStateFlow(12)
    val activeChannelsList = MutableStateFlow(14)
    val totalStorage = MutableStateFlow("2.4 GB")

    val users: StateFlow<List<User>> = userRepository.getUsers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val channels: StateFlow<List<Channel>> = channelRepository.getChannels()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Mock logs representing chronological action feed
    val logs = MutableStateFlow<List<ModerationLog>>(
        listOf(
            ModerationLog("1", "alice_martin", "Alice Martin", "Banned malicious user test_user_spamer", System.currentTimeMillis() - 2 * 60 * 1000),
            ModerationLog("2", "bob_chen", "Bob Chen", "Archived inactive channel #development-legacy", System.currentTimeMillis() - 10 * 60 * 1000),
            ModerationLog("3", "alice_martin", "Alice Martin", "Created private VIP group room #vip-team", System.currentTimeMillis() - 30 * 60 * 1000)
        )
    )

    fun changeUserRole(userId: String, newRole: String) {
        // Change role mock
    }

    fun toggleUserActivation(userId: String) {
        // Toggle mock
    }
}

// Sealed Ui State wrapper
sealed interface UiState<out T> {
    object Idle : UiState<Nothing>
    object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val error: String) : UiState<Nothing>
}

data class ModerationLog(
    val id: String,
    val modUserId: String,
    val modName: String,
    val description: String,
    val timestamp: Long
)
