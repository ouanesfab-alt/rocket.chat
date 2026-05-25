package com.example.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NamedNavArgument
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.di.AppContainer
import com.example.ui.screens.*

@Composable
fun AppNavGraph(
    navController: NavHostController,
    appContainer: AppContainer,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Splash.route,
        modifier = modifier
    ) {
        // 1. Splash Screen Destination
        composable(Screen.Splash.route) {
            val splashViewModel: SplashViewModel = viewModel(
                factory = object : ViewModelProvider.Factory {
                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
                        return SplashViewModel(appContainer.authRepository) as T
                    }
                }
            )
            SplashScreen(splashViewModel) { route ->
                navController.navigate(route) {
                    popUpTo(Screen.Splash.route) { inclusive = true }
                }
            }
        }

        // 2. Login Screen Destination
        composable(Screen.Login.route) {
            val authViewModel: AuthViewModel = viewModel(
                factory = object : ViewModelProvider.Factory {
                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
                        return AuthViewModel(appContainer.authRepository) as T
                    }
                }
            )
            LoginScreen(
                viewModel = authViewModel,
                onNavigateToRegister = { navController.navigate(Screen.Register.route) },
                onNavigateToForgotPassword = { navController.navigate(Screen.ForgotPassword.route) },
                onNavigateToHome = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }

        // 3. Register Screen Destination
        composable(Screen.Register.route) {
            val authViewModel: AuthViewModel = viewModel(
                factory = object : ViewModelProvider.Factory {
                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
                        return AuthViewModel(appContainer.authRepository) as T
                    }
                }
            )
            RegisterScreen(
                viewModel = authViewModel,
                onNavigateToLogin = { navController.navigateUp() },
                onNavigateToHome = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }

        // 4. Forgot Password Screen
        composable(Screen.ForgotPassword.route) {
            val authViewModel: AuthViewModel = viewModel(
                factory = object : ViewModelProvider.Factory {
                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
                        return AuthViewModel(appContainer.authRepository) as T
                    }
                }
            )
            ForgotPasswordScreen(
                viewModel = authViewModel,
                onNavigateToLogin = { navController.navigateUp() }
            )
        }

        // 5. Unified Home Dashboard
        composable(Screen.Home.route) {
            val homeViewModel: HomeViewModel = viewModel(
                factory = object : ViewModelProvider.Factory {
                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
                        return HomeViewModel(
                            appContainer.channelRepository,
                            appContainer.userRepository,
                            appContainer.messageRepository
                        ) as T
                    }
                }
            )
            HomeScreen(
                viewModel = homeViewModel,
                onNavigateToChat = { navController.navigate("chat/$it") },
                onNavigateToSearch = { navController.navigate(Screen.Search.route) },
                onNavigateToProfile = { navController.navigate(Screen.OwnProfile.route) },
                onNavigateToCreateChannel = { navController.navigate(Screen.CreateChannel.route) },
                onNavigateToNewDM = { navController.navigate(Screen.NewDM.route) },
                onNavigateToStarred = { navController.navigate(Screen.StarredMessages.route) },
                onNavigateToMentions = { navController.navigate(Screen.Mentions.route) },
                onNavigateToSharedFiles = { navController.navigate(Screen.SharedFiles.route) },
                onNavigateToAdmin = { navController.navigate(Screen.AdminPanel.route) },
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) }
            )
        }

        // 6. Dynamic Chat Screen (With reverse messages LazyColumn & typing meters)
        composable(
            route = Screen.Chat.ROUTE,
            arguments = listOf(navArgument("channelId") { type = NavType.StringType })
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getString("channelId") ?: "general"
            val chatViewModel: ChatViewModel = viewModel(
                key = id,
                factory = object : ViewModelProvider.Factory {
                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
                        return ChatViewModel(
                            channelId = id,
                            messageRepository = appContainer.messageRepository,
                            channelRepository = appContainer.channelRepository
                        ) as T
                    }
                }
            )
            ChatScreen(
                viewModel = chatViewModel,
                onNavigateBack = { navController.navigateUp() },
                onNavigateToInfo = { navController.navigate("channel_info/$it") },
                onNavigateToThread = { navController.navigate("thread/$it") },
                onNavigateToUserProfile = { navController.navigate("user_profile/$it") },
                onNavigateToImageViewer = { navController.navigate(Screen.ImageViewer(it).route) },
                onNavigateToVideoViewer = { navController.navigate(Screen.VideoPlayer(it).route) }
            )
        }

        // 7. Full Screen Zoomable Image Attachment Viewer
        composable(
            route = Screen.ImageViewer.ROUTE,
            arguments = listOf(navArgument("url") { type = NavType.StringType })
        ) { backStackEntry ->
            val url = backStackEntry.arguments?.getString("url") ?: "https://picsum.photos/400"
            ImageViewerScreen(url = url) { navController.navigateUp() }
        }

        // 8. Streaming Playback Video Controller
        composable(
            route = Screen.VideoPlayer.ROUTE,
            arguments = listOf(navArgument("url") { type = NavType.StringType })
        ) { backStackEntry ->
            val url = backStackEntry.arguments?.getString("url") ?: ""
            VideoPlayerScreen(url = url) { navController.navigateUp() }
        }

        // 9. Thread replies dialogue space
        composable(
            route = Screen.Thread.ROUTE,
            arguments = listOf(navArgument("messageId") { type = NavType.StringType })
        ) { backStackEntry ->
            val messageId = backStackEntry.arguments?.getString("messageId") ?: ""
            ThreadScreen(
                messageId = messageId,
                messageRepository = appContainer.messageRepository
            ) { navController.navigateUp() }
        }

        // 10. Core Search Panel (Autocomplete matching filter sets)
        composable(Screen.Search.route) {
            val searchViewModel: SearchViewModel = viewModel(
                factory = object : ViewModelProvider.Factory {
                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
                        return SearchViewModel(
                            appContainer.messageRepository,
                            appContainer.channelRepository,
                            appContainer.userRepository
                        ) as T
                    }
                }
            )
            SearchScreen(
                viewModel = searchViewModel,
                onNavigateToChat = { navController.navigate("chat/$it") },
                onNavigateToUserProfile = { navController.navigate("user_profile/$it") },
                onNavigateBack = { navController.navigateUp() }
            )
        }

        // 11. Channel detail specifications metrics room tabs
        composable(
            route = Screen.ChannelInfo.ROUTE,
            arguments = listOf(navArgument("channelId") { type = NavType.StringType })
        ) { backStackEntry ->
            val channelId = backStackEntry.arguments?.getString("channelId") ?: ""
            val infoViewModel: ChannelInfoViewModel = viewModel(
                key = channelId,
                factory = object : ViewModelProvider.Factory {
                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
                        return ChannelInfoViewModel(
                            channelId = channelId,
                            channelRepository = appContainer.channelRepository,
                            userRepository = appContainer.userRepository,
                            messageRepository = appContainer.messageRepository
                        ) as T
                    }
                }
            )
            ChannelInfoScreen(
                viewModel = infoViewModel,
                onNavigateToUserProfile = { navController.navigate("user_profile/$it") },
                onNavigateBack = { navController.navigateUp() }
            )
        }

        // 12. External user profile statistics block list
        composable(
            route = Screen.UserProfile.ROUTE,
            arguments = listOf(navArgument("userId") { type = NavType.StringType })
        ) { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId") ?: ""
            UserProfileScreen(
                userId = userId,
                userRepository = appContainer.userRepository,
                onNavigateToChat = { navController.navigate("chat/$it") },
                onNavigateBack = { navController.navigateUp() }
            )
        }

        // 13. Own Profile Form Editor
        composable(Screen.OwnProfile.route) {
            val ownProfileViewModel: ProfileViewModel = viewModel(
                factory = object : ViewModelProvider.Factory {
                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
                        return ProfileViewModel(appContainer.userRepository) as T
                    }
                }
            )
            OwnProfileScreen(
                viewModel = ownProfileViewModel,
                onNavigateBack = { navController.navigateUp() }
            )
        }

        // 14. Configuration list categories (Appearance slider triggers, notification vibration tones)
        composable(Screen.Settings.route) {
            SettingsScreen(
                onNavigateToProfile = { navController.navigate(Screen.OwnProfile.route) },
                onNavigateToNotifications = { navController.navigate(Screen.NotificationSettings.route) },
                onNavigateToAppearance = { navController.navigate(Screen.AppearanceSettings.route) },
                onNavigateToSecurity = { navController.navigate(Screen.SecuritySettings.route) },
                onLogout = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                },
                onNavigateBack = { navController.navigateUp() }
            )
        }

        // 15. In-App Sounds settings vibration LED
        composable(Screen.NotificationSettings.route) {
            NotificationSettingsScreen { navController.navigateUp() }
        }

        // 16. Accent colors density modifiers slider
        composable(Screen.AppearanceSettings.route) {
            AppearanceSettingsScreen { navController.navigateUp() }
        }

        // 17. TOTP Authenticator locks active terminates trace
        composable(Screen.SecuritySettings.route) {
            SecuritySettingsScreen { navController.navigateUp() }
        }

        // 18. Assemble private Channels
        composable(Screen.CreateChannel.route) {
            val homeViewModel: HomeViewModel = viewModel(
                factory = object : ViewModelProvider.Factory {
                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
                        return HomeViewModel(
                            appContainer.channelRepository,
                            appContainer.userRepository,
                            appContainer.messageRepository
                        ) as T
                    }
                }
            )
            CreateChannelScreen(
                viewModel = homeViewModel,
                onNavigateToChat = {
                    navController.navigate("chat/$it") {
                        popUpTo(Screen.CreateChannel.route) { inclusive = true }
                    }
                },
                onNavigateBack = { navController.navigateUp() }
            )
        }

        // 19. Open direct encrypted dialogue list
        composable(Screen.NewDM.route) {
            val homeViewModel: HomeViewModel = viewModel(
                factory = object : ViewModelProvider.Factory {
                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
                        return HomeViewModel(
                            appContainer.channelRepository,
                            appContainer.userRepository,
                            appContainer.messageRepository
                        ) as T
                    }
                }
            )
            NewDMScreen(
                viewModel = homeViewModel,
                onNavigateToChat = {
                    navController.navigate("chat/$it") {
                        popUpTo(Screen.NewDM.route) { inclusive = true }
                    }
                },
                onNavigateBack = { navController.navigateUp() }
            )
        }

        // 20. Starred important bookmarks lists
        composable(Screen.StarredMessages.route) {
            val homeViewModel: HomeViewModel = viewModel(
                factory = object : ViewModelProvider.Factory {
                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
                        return HomeViewModel(
                            appContainer.channelRepository,
                            appContainer.userRepository,
                            appContainer.messageRepository
                        ) as T
                    }
                }
            )
            StarredMessagesScreen(
                viewModel = homeViewModel,
                onNavigateToChat = { navController.navigate("chat/$it") },
                onNavigateBack = { navController.navigateUp() }
            )
        }

        // 21. Mentions logs alarms
        composable(Screen.Mentions.route) {
            val homeViewModel: HomeViewModel = viewModel(
                factory = object : ViewModelProvider.Factory {
                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
                        return HomeViewModel(
                            appContainer.channelRepository,
                            appContainer.userRepository,
                            appContainer.messageRepository
                        ) as T
                    }
                }
            )
            MentionsScreen(
                viewModel = homeViewModel,
                onNavigateToChat = { navController.navigate("chat/$it") },
                onNavigateBack = { navController.navigateUp() }
            )
        }

        // 22. Shared Media attachment grid lists files
        composable(Screen.SharedFiles.route) {
            val homeViewModel: HomeViewModel = viewModel(
                factory = object : ViewModelProvider.Factory {
                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
                        return HomeViewModel(
                            appContainer.channelRepository,
                            appContainer.userRepository,
                            appContainer.messageRepository
                        ) as T
                    }
                }
            )
            SharedFilesScreen(
                viewModel = homeViewModel,
                onNavigateToChat = { navController.navigate("chat/$it") },
                onNavigateBack = { navController.navigateUp() }
            )
        }

        // 23. Admin Operational Panel main dashboards
        composable(Screen.AdminPanel.route) {
            val adminViewModel: AdminViewModel = viewModel(
                factory = object : ViewModelProvider.Factory {
                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
                        return AdminViewModel(
                            appContainer.userRepository,
                            appContainer.channelRepository
                        ) as T
                    }
                }
            )
            AdminPanelScreen(
                viewModel = adminViewModel,
                onNavigateToUsers = { navController.navigate(Screen.AdminUsers.route) },
                onNavigateToChannels = { navController.navigate(Screen.AdminChannels.route) },
                onNavigateToLogs = { navController.navigate(Screen.AdminLogs.route) },
                onNavigateBack = { navController.navigateUp() }
            )
        }

        // 24. Admin Users management active database
        composable(Screen.AdminUsers.route) {
            val adminViewModel: AdminViewModel = viewModel(
                factory = object : ViewModelProvider.Factory {
                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
                        return AdminViewModel(
                            appContainer.userRepository,
                            appContainer.channelRepository
                        ) as T
                    }
                }
            )
            AdminUsersScreen(
                viewModel = adminViewModel,
                onNavigateBack = { navController.navigateUp() }
            )
        }

        // 25. Admin channels archive audits lists
        composable(Screen.AdminChannels.route) {
            val adminViewModel: AdminViewModel = viewModel(
                factory = object : ViewModelProvider.Factory {
                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
                        return AdminViewModel(
                            appContainer.userRepository,
                            appContainer.channelRepository
                        ) as T
                    }
                }
            )
            AdminChannelsScreen(
                viewModel = adminViewModel,
                onNavigateBack = { navController.navigateUp() }
            )
        }

        // 26. Admin Trace logs Auditing trace logs list
        composable(Screen.AdminLogs.route) {
            val adminViewModel: AdminViewModel = viewModel(
                factory = object : ViewModelProvider.Factory {
                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
                        return AdminViewModel(
                            appContainer.userRepository,
                            appContainer.channelRepository
                        ) as T
                    }
                }
            )
            AdminLogsScreen(
                viewModel = adminViewModel,
                onNavigateBack = { navController.navigateUp() }
            )
        }
    }
}
