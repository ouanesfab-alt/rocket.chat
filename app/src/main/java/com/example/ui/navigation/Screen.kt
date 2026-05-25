package com.example.ui.navigation

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Login : Screen("login")
    object Register : Screen("register")
    object ForgotPassword : Screen("forgot_password")
    object Home : Screen("home")
    
    // Chat & Media
    class Chat(channelId: String) : Screen("chat/$channelId") {
        companion object {
            const val ROUTE = "chat/{channelId}"
        }
    }
    class ImageViewer(url: String) : Screen("image_viewer?url=$url") {
        companion object {
            const val ROUTE = "image_viewer?url={url}"
        }
    }
    class VideoPlayer(url: String) : Screen("video_player?url=$url") {
        companion object {
            const val ROUTE = "video_player?url={url}"
        }
    }
    class Thread(messageId: String) : Screen("thread/$messageId") {
        companion object {
            const val ROUTE = "thread/{messageId}"
        }
    }

    // Secondary & Info Screens
    object Search : Screen("search")
    
    class ChannelInfo(channelId: String) : Screen("channel_info/$channelId") {
        companion object {
            const val ROUTE = "channel_info/{channelId}"
        }
    }
    
    class UserProfile(userId: String) : Screen("user_profile/$userId") {
        companion object {
            const val ROUTE = "user_profile/{userId}"
        }
    }

    object OwnProfile : Screen("own_profile")
    object Settings : Screen("settings")
    object NotificationSettings : Screen("notification_settings")
    object AppearanceSettings : Screen("appearance_settings")
    object SecuritySettings : Screen("security_settings")
    object NewDM : Screen("new_dm")
    object CreateChannel : Screen("create_channel")
    object StarredMessages : Screen("starred_messages")
    object Mentions : Screen("mentions")
    object SharedFiles : Screen("shared_files")
    
    // Admin Pane
    object AdminPanel : Screen("admin_panel")
    object AdminUsers : Screen("admin_users")
    object AdminChannels : Screen("admin_channels")
    object AdminLogs : Screen("admin_logs")
}
