@file:OptIn(ExperimentalMaterial3Api::class)
package com.example.ui.screens

import android.widget.Toast
import kotlinx.coroutines.flow.collectLatest
import androidx.compose.ui.graphics.graphicsLayer
import com.example.domain.repository.*

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.domain.model.*
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

// Global Mock Avatar Fallbacks
@Composable
fun UserAvatar(name: String, size: Int = 40, status: UserStatus? = null) {
    val initial = name.firstOrNull()?.uppercase() ?: "U"
    val colorIndex = name.hashCode().absoluteValue % 6
    val bgColors = listOf(
        Color(0xFFEF4444), Color(0xFF3B82F6), Color(0xFF10B981),
        Color(0xFFF59E0B), Color(0xFF8B5CF6), Color(0xFFEC4899)
    )
    val bgColor = bgColors[colorIndex]

    Box(
        modifier = Modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(bgColor),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initial,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = (size * 0.45).sp
        )

        // Presence indicator
        if (status != null) {
            val indicatorColor = when (status) {
                UserStatus.ONLINE -> OnlineGreen
                UserStatus.AWAY -> AwayYellow
                UserStatus.BUSY -> BusyRed
                UserStatus.OFFLINE -> Color.Gray
            }
            Box(
                modifier = Modifier
                    .size((size * 0.3).dp.coerceAtLeast(8.dp))
                    .align(Alignment.BottomEnd)
                    .clip(CircleShape)
                    .background(Color.DarkGray)
                    .padding(1.5.dp)
                    .clip(CircleShape)
                    .background(indicatorColor)
            )
        }
    }
}

// 1. SPLASH SCREEN
@Composable
fun SplashScreen(viewModel: SplashViewModel, onNavigate: (String) -> Unit) {
    val scope = rememberCoroutineScope()
    var scale by remember { mutableStateOf(0.4f) }
    var opacity by remember { mutableStateOf(0f) }

    LaunchedEffect(Unit) {
        animate(
            initialValue = 0.4f,
            targetValue = 1.0f,
            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
        ) { value, _ -> scale = value }
    }

    LaunchedEffect(Unit) {
        animate(initialValue = 0f, targetValue = 1f, animationSpec = tween(800)) { v, _ -> opacity = v }
    }

    LaunchedEffect(Unit) {
        viewModel.navigateToMatch.collectLatest { onNavigate(it) }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(DarkBackground, DarkSurface)
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.scale(scale).alpha(opacity)
        ) {
            Icon(
                imageVector = Icons.Filled.Send,
                contentDescription = "Logo",
                tint = AccentIndigo,
                modifier = Modifier.size(100.dp)
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "ROCKETCHAT",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 4.sp,
                    color = Color.White
                )
            )
            Text(
                text = "WhatsApp Dynamic Experience",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = TextSecondary,
                    letterSpacing = 1.sp
                ),
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

// 2. LOGIN SCREEN
@Composable
fun LoginScreen(
    viewModel: AuthViewModel,
    onNavigateToRegister: () -> Unit,
    onNavigateToForgotPassword: () -> Unit,
    onNavigateToHome: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val serverUrl by viewModel.serverUrl.collectAsStateWithLifecycle()
    val email by viewModel.email.collectAsStateWithLifecycle()
    val password by viewModel.password.collectAsStateWithLifecycle()
    val rememberMe by viewModel.rememberMe.collectAsStateWithLifecycle()
    val isPasswordVisible by viewModel.isPasswordVisible.collectAsStateWithLifecycle()

    val context = LocalContext.current

    LaunchedEffect(uiState) {
        if (uiState is UiState.Success) {
            viewModel.resetState()
            onNavigateToHome()
        } else if (uiState is UiState.Error) {
            Toast.makeText(context, (uiState as UiState.Error).error, Toast.LENGTH_LONG).show()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = DarkBackground
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Send,
                contentDescription = "Rocket Logo",
                tint = AccentIndigo,
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Welcome Back",
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Login to connect with Rocket.Chat server",
                color = TextSecondary,
                fontSize = 14.sp
            )

            Spacer(modifier = Modifier.height(32.dp))

            OutlinedTextField(
                value = serverUrl,
                onValueChange = { viewModel.serverUrl.value = it },
                label = { Text("Server URL") },
                modifier = Modifier.fillMaxWidth().testTag("server_url_input"),
                leadingIcon = { Icon(Icons.Default.Share, "Server") },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AccentIndigo,
                    unfocusedBorderColor = DividerColor,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = email,
                onValueChange = { viewModel.email.value = it },
                label = { Text("Username or Email") },
                modifier = Modifier.fillMaxWidth().testTag("username_input"),
                leadingIcon = { Icon(Icons.Default.Person, "User") },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AccentIndigo,
                    unfocusedBorderColor = DividerColor,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { viewModel.password.value = it },
                label = { Text("Password") },
                modifier = Modifier.fillMaxWidth().testTag("password_input"),
                leadingIcon = { Icon(Icons.Default.Lock, "Lock") },
                trailingIcon = {
                    IconButton(onClick = { viewModel.togglePasswordVisibility() }) {
                        Icon(
                            imageVector = if (isPasswordVisible) Icons.Filled.Star else Icons.Outlined.Star,
                            contentDescription = "Show Password"
                        )
                    }
                },
                visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AccentIndigo,
                    unfocusedBorderColor = DividerColor,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                )
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = rememberMe,
                        onCheckedChange = { viewModel.rememberMe.value = it }
                    )
                    Text("Remember Me", color = TextSecondary, fontSize = 14.sp)
                }

                TextButton(onClick = onNavigateToForgotPassword) {
                    Text("Forgot Password?", color = AccentIndigoLight)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = { viewModel.login() },
                modifier = Modifier.fillMaxWidth().height(50.dp).testTag("login_button"),
                colors = ButtonDefaults.buttonColors(containerColor = AccentIndigo),
                enabled = uiState !is UiState.Loading
            ) {
                if (uiState is UiState.Loading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                } else {
                    Text("Login", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Don't have an account?", color = TextSecondary)
                TextButton(onClick = onNavigateToRegister) {
                    Text("Register Here", color = AccentIndigoLight)
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
            Text("Alternative Login methods coming soon", color = TextMuted, fontSize = 12.sp)
        }
    }
}

// 3. REGISTER SCREEN
@Composable
fun RegisterScreen(
    viewModel: AuthViewModel,
    onNavigateToLogin: () -> Unit,
    onNavigateToHome: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val displayName by viewModel.displayName.collectAsStateWithLifecycle()
    val username by viewModel.username.collectAsStateWithLifecycle()
    val email by viewModel.email.collectAsStateWithLifecycle()
    val password by viewModel.password.collectAsStateWithLifecycle()
    val confirmPassword by viewModel.confirmPassword.collectAsStateWithLifecycle()
    val termsAccepted by viewModel.termsAccepted.collectAsStateWithLifecycle()
    
    val context = LocalContext.current

    LaunchedEffect(uiState) {
        if (uiState is UiState.Success) {
            viewModel.resetState()
            onNavigateToHome()
        } else if (uiState is UiState.Error) {
            Toast.makeText(context, (uiState as UiState.Error).error, Toast.LENGTH_LONG).show()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = DarkBackground
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Register Account",
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Connect with teams worldwide",
                color = TextSecondary,
                fontSize = 14.sp
            )

            Spacer(modifier = Modifier.height(24.dp))

            OutlinedTextField(
                value = displayName,
                onValueChange = { viewModel.displayName.value = it },
                label = { Text("Display Name") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AccentIndigo,
                    unfocusedBorderColor = DividerColor,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = username,
                onValueChange = { viewModel.username.value = it },
                label = { Text("Username") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AccentIndigo,
                    unfocusedBorderColor = DividerColor,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = email,
                onValueChange = { viewModel.email.value = it },
                label = { Text("Email Address") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AccentIndigo,
                    unfocusedBorderColor = DividerColor,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { viewModel.password.value = it },
                label = { Text("Password") },
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = PasswordVisualTransformation(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AccentIndigo,
                    unfocusedBorderColor = DividerColor,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = confirmPassword,
                onValueChange = { viewModel.confirmPassword.value = it },
                label = { Text("Confirm Password") },
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = PasswordVisualTransformation(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AccentIndigo,
                    unfocusedBorderColor = DividerColor,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                )
            )

            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = termsAccepted,
                    onCheckedChange = { viewModel.termsAccepted.value = it }
                )
                Text("I accept the Terms and Privacy Policy", color = TextSecondary, fontSize = 14.sp)
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = { viewModel.register() },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AccentIndigo),
                enabled = uiState !is UiState.Loading
            ) {
                if (uiState is UiState.Loading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                } else {
                    Text("Register Now", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Already registered?", color = TextSecondary)
                TextButton(onClick = onNavigateToLogin) {
                    Text("Login Here", color = AccentIndigoLight)
                }
            }
        }
    }
}

// 4. FORGOT PASSWORD SCREEN
@Composable
fun ForgotPasswordScreen(
    viewModel: AuthViewModel,
    onNavigateToLogin: () -> Unit
) {
    val email by viewModel.email.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(uiState) {
        if (uiState is UiState.Error) {
            Toast.makeText(context, (uiState as UiState.Error).error, Toast.LENGTH_LONG).show()
            viewModel.resetState()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = DarkBackground
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.MailOutline,
                contentDescription = "Mail",
                tint = AccentIndigo,
                modifier = Modifier.size(80.dp)
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Reset Password",
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "We will send instructions to configure your new passcode",
                color = TextSecondary,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 24.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))

            OutlinedTextField(
                value = email,
                onValueChange = { viewModel.email.value = it },
                label = { Text("Registered Email Address") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AccentIndigo,
                    unfocusedBorderColor = DividerColor,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                )
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = { viewModel.forgotPassword() },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AccentIndigo)
            ) {
                Text("Request Recovery Links", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }

            Spacer(modifier = Modifier.height(16.dp))

            TextButton(onClick = onNavigateToLogin) {
                Text("Back to log-in", color = AccentIndigoLight)
            }
        }
    }
}

// 5. HOME SCREEN (Tabbed Chats, Channels, DMs, Mentions, More Settings Navigation)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onNavigateToChat: (String) -> Unit,
    onNavigateToSearch: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToCreateChannel: () -> Unit,
    onNavigateToNewDM: () -> Unit,
    onNavigateToStarred: () -> Unit,
    onNavigateToMentions: () -> Unit,
    onNavigateToSharedFiles: () -> Unit,
    onNavigateToAdmin: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val chats by viewModel.chats.collectAsStateWithLifecycle()
    val channels by viewModel.channels.collectAsStateWithLifecycle()
    val dms by viewModel.dms.collectAsStateWithLifecycle()
    val users by viewModel.users.collectAsStateWithLifecycle()

    var selectedTab by remember { mutableStateOf(0) }
    val tabTitles = listOf("Chats", "Channels", "Direct Messages", "More")

    var showBottomSheet by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            @OptIn(ExperimentalMaterial3Api::class)
            TopAppBar(
                title = { Text("RocketChat", color = Color.White, fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkSurface),
                actions = {
                    IconButton(onClick = onNavigateToSearch) {
                        Icon(imageVector = Icons.Default.Search, contentDescription = "Search", tint = Color.White)
                    }
                    IconButton(onClick = { showBottomSheet = true }) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = "Add", tint = Color.White)
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar(containerColor = DarkSurface) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.Home, "Chats") },
                    label = { Text("Chats") }
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Default.Menu, "Channels") },
                    label = { Text("Channels") }
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Default.Person, "DMs") },
                    label = { Text("DMs") }
                )
                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    icon = { Icon(Icons.Default.Settings, "More") },
                    label = { Text("More") }
                )
            }
        },
        floatingActionButton = {
            if (selectedTab < 3) {
                FloatingActionButton(
                    onClick = { showBottomSheet = true },
                    containerColor = AccentIndigo,
                    contentColor = Color.White
                ) {
                    Icon(Icons.Filled.Edit, "New Dialogue")
                }
            }
        },
        containerColor = DarkBackground
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                0 -> ChatsTabContent(chats, onNavigateToChat)
                1 -> ChannelsTabContent(channels, onNavigateToChat)
                2 -> DMsTabContent(chats.filter { it.type == ChannelType.PRIVATE }, onNavigateToChat)
                3 -> MoreConfigurationsContent(
                    onNavigateToProfile = onNavigateToProfile,
                    onNavigateToStarred = onNavigateToStarred,
                    onNavigateToMentions = onNavigateToMentions,
                    onNavigateToSharedFiles = onNavigateToSharedFiles,
                    onNavigateToAdmin = onNavigateToAdmin,
                    onNavigateToSettings = onNavigateToSettings
                )
            }

            if (showBottomSheet) {
                ModalBottomSheet(
                    onDismissRequest = { showBottomSheet = false },
                    containerColor = DarkSurface
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp)
                    ) {
                        Text(
                            text = "Create Dialogue",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = Color.White,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                        ListItem(
                            headlineContent = { Text("New Channels Pack", color = Color.White) },
                            supportingContent = { Text("Public or private discussion spaces", color = TextSecondary) },
                            leadingContent = { Icon(Icons.Default.AddCircle, null, tint = AccentIndigo) },
                            modifier = Modifier.clickable {
                                showBottomSheet = false
                                onNavigateToCreateChannel()
                            },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                        )
                        ListItem(
                            headlineContent = { Text("New Direct Message", color = Color.White) },
                            supportingContent = { Text("One-on-one encrypted chat", color = TextSecondary) },
                            leadingContent = { Icon(Icons.Default.Person, null, tint = AccentIndigo) },
                            modifier = Modifier.clickable {
                                showBottomSheet = false
                                onNavigateToNewDM()
                            },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ChatsTabContent(chats: List<Channel>, onNavigateToChat: (String) -> Unit) {
    if (chats.isEmpty()) {
        EmptyStateBox(title = "No Conversations Yet", sub = "Start sharing updates with your team members!")
    } else {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(chats) { chat ->
                ChatItemRow(chat, onNavigateToChat)
                HorizontalDivider(color = DividerColor, modifier = Modifier.padding(horizontal = 16.dp))
            }
        }
    }
}

@Composable
fun ChannelsTabContent(channels: List<Channel>, onNavigateToChat: (String) -> Unit) {
    val publics = channels.filter { it.type == ChannelType.PUBLIC }
    if (publics.isEmpty()) {
        EmptyStateBox(title = "No Channels", sub = "Create a public space to organize your channels list.")
    } else {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(publics) { channel ->
                ChatItemRow(channel, onNavigateToChat)
                HorizontalDivider(color = DividerColor, modifier = Modifier.padding(horizontal = 16.dp))
            }
        }
    }
}

@Composable
fun DMsTabContent(dms: List<Channel>, onNavigateToChat: (String) -> Unit) {
    if (dms.isEmpty()) {
        EmptyStateBox(title = "No Direct Messages", sub = "Send an invite to colleagues to begin chat logs.")
    } else {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(dms) { dm ->
                ChatItemRow(dm, onNavigateToChat)
                HorizontalDivider(color = DividerColor, modifier = Modifier.padding(horizontal = 16.dp))
            }
        }
    }
}

@Composable
fun ChatItemRow(channel: Channel, onClick: (String) -> Unit) {
    val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
    val formattedTime = if (channel.lastMessageTimestamp > 0) sdf.format(Date(channel.lastMessageTimestamp)) else ""

    ListItem(
        headlineContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (channel.type == ChannelType.PRIVATE) "🔒 ${channel.name}" else "# ${channel.name}",
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (channel.isMuted) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(Icons.Default.Clear, "Muted", tint = TextMuted, modifier = Modifier.size(14.dp))
                }
            }
        },
        supportingContent = {
            Text(
                text = channel.lastMessageText ?: "No messages in history",
                color = TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        leadingContent = {
            UserAvatar(name = channel.name, size = 48)
        },
        trailingContent = {
            Column(horizontalAlignment = Alignment.End) {
                Text(formattedTime, fontSize = 12.sp, color = TextMuted)
                if (channel.unreadCount > 0) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(AccentIndigo)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (channel.unreadCount > 99) "99+" else channel.unreadCount.toString(),
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick(channel.id) }
            .testTag("chat_item_${channel.id}"),
        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
    )
}

// More tab config menu (Settings list, Starred, Mentions)
@Composable
fun MoreConfigurationsContent(
    onNavigateToProfile: () -> Unit,
    onNavigateToStarred: () -> Unit,
    onNavigateToMentions: () -> Unit,
    onNavigateToSharedFiles: () -> Unit,
    onNavigateToAdmin: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface)
        ) {
            ListItem(
                headlineContent = { Text("Demo Admin Profile", color = Color.White, fontWeight = FontWeight.Bold) },
                supportingContent = { Text("Edit bios, custom emoji statuses, and tags", color = TextSecondary) },
                leadingContent = { UserAvatar(name = "Demo User", size = 50, status = UserStatus.ONLINE) },
                modifier = Modifier.clickable { onNavigateToProfile() },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Card(colors = CardDefaults.cardColors(containerColor = DarkSurface)) {
            Column {
                PreferenceNavigationItem(Icons.Default.Star, "Starred Messages", "Saved files, code notes, & bookmarks") { onNavigateToStarred() }
                HorizontalDivider(color = DividerColor)
                PreferenceNavigationItem(Icons.Default.Notifications, "Mentions Log", "Alert logs referencing you directly") { onNavigateToMentions() }
                HorizontalDivider(color = DividerColor)
                PreferenceNavigationItem(Icons.Default.Share, "Shared Workspace Files", "View images grids & PDF attachment arrays") { onNavigateToSharedFiles() }
                HorizontalDivider(color = DividerColor)
                PreferenceNavigationItem(Icons.Default.Settings, "Application Settings", "Display sizing theme densities, sound overrides") { onNavigateToSettings() }
                HorizontalDivider(color = DividerColor)
                PreferenceNavigationItem(Icons.Default.Lock, "Admin Control Board", "Control users status modifications & channel blocks") { onNavigateToAdmin() }
            }
        }
    }
}

@Composable
fun PreferenceNavigationItem(
    icon: ImageVector,
    title: String,
    desc: String,
    onClick: () -> Unit
) {
    ListItem(
        headlineContent = { Text(title, color = Color.White, fontWeight = FontWeight.SemiBold) },
        supportingContent = { Text(desc, color = TextSecondary) },
        leadingContent = { Icon(icon, null, tint = AccentIndigo) },
        modifier = Modifier.clickable { onClick() },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
    )
}

// 6. CHAT SCREEN (High Complex Chat Window supporting reactions, replies, voice waveform inputs)
@Composable
fun ChatScreen(
    viewModel: ChatViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToInfo: (String) -> Unit,
    onNavigateToThread: (String) -> Unit,
    onNavigateToUserProfile: (String) -> Unit,
    onNavigateToImageViewer: (String) -> Unit,
    onNavigateToVideoViewer: (String) -> Unit
) {
    val channel by viewModel.channel.collectAsStateWithLifecycle()
    val messages by viewModel.messages.collectAsStateWithLifecycle()
    val textInput by viewModel.textInput.collectAsStateWithLifecycle()
    val replyMessage by viewModel.replyMessage.collectAsStateWithLifecycle()
    val isTyping by viewModel.isTyping.collectAsStateWithLifecycle()
    val typingUser by viewModel.typingUser.collectAsStateWithLifecycle()

    var showMediaSheet by remember { mutableStateOf(false) }
    var voiceRecordingActive by remember { mutableStateOf(false) }
    var recordingTimer by remember { mutableStateOf(0) }

    LaunchedEffect(voiceRecordingActive) {
        if (voiceRecordingActive) {
            recordingTimer = 0
            while (voiceRecordingActive) {
                delay(1000)
                recordingTimer++
            }
        }
    }

    Scaffold(
        topBar = {
            @OptIn(ExperimentalMaterial3Api::class)
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { channel?.let { onNavigateToInfo(it.id) } }
                    ) {
                        UserAvatar(name = channel?.name ?: "Chat", size = 36)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(channel?.name ?: "Loading...", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            Text(
                                text = if (isTyping) "$typingUser is typing..." else "${channel?.memberCount ?: 0} members",
                                color = if (isTyping) OnlineGreen else TextSecondary,
                                fontSize = 12.sp
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = { channel?.let { onNavigateToInfo(it.id) } }) {
                        Icon(Icons.Default.Info, "Information", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkSurface)
            )
        },
        containerColor = DarkBackground
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Message List Area
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                reverseLayout = true
            ) {
                items(messages) { message ->
                    MessageBubbleItem(
                        message = message,
                        onReplyClick = { viewModel.setReplyTo(message) },
                        onDeleteClick = { viewModel.deleteMessage(message.id) },
                        onThreadClick = { onNavigateToThread(message.id) },
                        onUserClick = { onNavigateToUserProfile(message.senderId) },
                        onImageClick = { onNavigateToImageViewer(it) },
                        onVideoClick = { onNavigateToVideoViewer(it) },
                        onReactClick = { emoji -> viewModel.addReaction(message.id, emoji) }
                    )
                }
            }

            // Typing feedback
            if (isTyping) {
                Text(
                    text = "✍️ Typing indicator...",
                    color = OnlineGreen,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(start = 16.dp, bottom = 4.dp)
                )
            }

            // Replying to alert banner
            if (replyMessage != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Replying to @${replyMessage?.senderName}", color = AccentIndigoLight, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Text(replyMessage?.message ?: "", color = Color.White, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        IconButton(onClick = { viewModel.setReplyTo(null) }) {
                            Icon(Icons.Default.Close, "Cancel Reply", tint = Color.White)
                        }
                    }
                }
            }

            // Chat Input Bar Space
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DarkSurface)
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { showMediaSheet = true }) {
                    Icon(Icons.Default.Add, "Attach Files", tint = Color.White)
                }

                // Wave recorder animation bar
                if (voiceRecordingActive) {
                    Row(
                        modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(BusyRed)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Recording: ${recordingTimer}s", color = Color.White, fontSize = 14.sp)
                        }
                        TextButton(onClick = { voiceRecordingActive = false }) {
                            Text("Cancel", color = TextSecondary)
                        }
                    }
                } else {
                    OutlinedTextField(
                        value = textInput,
                        onValueChange = {
                            viewModel.saveDraft(it)
                            viewModel.toggleTyping(it.isNotEmpty())
                        },
                        placeholder = { Text("Message...", color = TextSecondary) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("chat_input_textfield"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent
                        ),
                        singleLine = false
                    )
                }

                // Send/Voice record controls
                if (textInput.isNotEmpty()) {
                    IconButton(
                        onClick = { viewModel.sendMessage() },
                        modifier = Modifier.testTag("chat_send_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, "Send", tint = AccentIndigo)
                    }
                } else {
                    IconButton(
                        onClick = {
                            if (voiceRecordingActive) {
                                voiceRecordingActive = false
                                viewModel.sendAudioMessage("https://www.soundhelix.com/examples/mp3/SoundHelix-Song-2.mp3")
                            } else {
                                voiceRecordingActive = true
                            }
                        }
                    ) {
                        Icon(
                            imageVector = if (voiceRecordingActive) Icons.Default.Check else Icons.Default.PlayArrow,
                            contentDescription = "Voice note icon",
                            tint = if (voiceRecordingActive) OnlineGreen else Color.White
                        )
                    }
                }
            }
        }

        // Attach dropdown modal sheet
        if (showMediaSheet) {
            ModalBottomSheet(
                onDismissRequest = { showMediaSheet = false },
                containerColor = DarkSurface
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                ) {
                    Text("Select Attachment", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 18.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        AttachmentOption(Icons.Default.Star, "Photo") {
                            showMediaSheet = false
                            viewModel.sendFileAttachment("Snapshot.jpg", "https://picsum.photos/400/300", "204 KB", "image")
                        }
                        AttachmentOption(Icons.Default.Place, "Video") {
                            showMediaSheet = false
                            viewModel.sendFileAttachment("Clip.mp4", "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4", "5.4 MB", "video")
                        }
                        AttachmentOption(Icons.Default.Share, "Document") {
                            showMediaSheet = false
                            viewModel.sendFileAttachment("Report.pdf", "https://example.com/spec.pdf", "102 KB", "file")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AttachmentOption(icon: ImageVector, label: String, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .size(50.dp)
                .clip(CircleShape)
                .background(AccentIndigo)
        ) {
            Icon(icon, null, tint = Color.White, modifier = Modifier.align(Alignment.Center))
        }
        Text(label, color = TextSecondary, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
    }
}

// 7. COMPACT MESSAGE BUBBLE RENDERING W/ TYPES COMPILER
@Composable
fun MessageBubbleItem(
    message: Message,
    onReplyClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onThreadClick: () -> Unit,
    onUserClick: () -> Unit,
    onImageClick: (String) -> Unit,
    onVideoClick: (String) -> Unit,
    onReactClick: (String) -> Unit
) {
    val isMe = message.senderId == "me"
    val bubbleColor = if (isMe) MyMessageBubble else OtherMessageBubble
    val align = if (isMe) Alignment.End else Alignment.Start

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalAlignment = align
    ) {
        // System Event formatting fallback
        if (message.systemType != null) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.DarkGray)
                    .padding(horizontal = 12.dp, vertical = 6.dp)
                    .align(Alignment.CenterHorizontally)
            ) {
                Text(message.message, color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            return@Column
        }

        Row(
            verticalAlignment = Alignment.Top,
            modifier = Modifier.padding(horizontal = 8.dp)
        ) {
            if (!isMe) {
                Box(modifier = Modifier.clickable { onUserClick() }) {
                    UserAvatar(name = message.senderName, size = 32)
                }
                Spacer(modifier = Modifier.width(8.dp))
            }

            Column(horizontalAlignment = align) {
                // Sender title tag
                if (!isMe) {
                    Text(message.senderName, color = AccentIndigoLight, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }

                Card(
                    modifier = Modifier.padding(top = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = bubbleColor),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        // Quotes reply render
                        if (message.replyToText != null) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(DarkBackground)
                                    .padding(8.dp)
                            ) {
                                Column {
                                    Text("@${message.replyToSenderName ?: "User"}", color = AccentIndigoLight, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                    Text(message.replyToText ?: "", color = TextSecondary, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        // Attachment visualizers
                        if (message.attachment != null) {
                            val media = message.attachment
                            when (media.type) {
                                "image" -> {
                                    AsyncImage(
                                        model = media.url,
                                        contentDescription = "Image capture",
                                        modifier = Modifier
                                            .size(width = 200.dp, height = 150.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable { onImageClick(media.url) },
                                        contentScale = ContentScale.Crop
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                }
                                "video" -> {
                                    Box(
                                        modifier = Modifier
                                            .size(width = 200.dp, height = 150.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color.Black)
                                            .clickable { onVideoClick(media.url) }
                                    ) {
                                        Icon(
                                            Icons.Default.PlayArrow,
                                            null,
                                            tint = Color.White,
                                            modifier = Modifier.size(48.dp).align(Alignment.Center)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                }
                                "audio" -> {
                                    AudioPlaybackRow(media.url)
                                    Spacer(modifier = Modifier.height(4.dp))
                                }
                                "file" -> {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color.DarkGray)
                                            .padding(8.dp)
                                    ) {
                                        Icon(Icons.Default.Share, null, tint = Color.White)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(media.name, color = Color.White, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            Text(media.size, color = TextSecondary, fontSize = 10.sp)
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                }
                            }
                        }

                        // Message Text Body
                        Text(message.message, color = Color.White, fontSize = 14.sp)

                        // Time stamps metadata row
                        Row(
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.align(Alignment.End).padding(top = 4.dp)
                        ) {
                            val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
                            Text(sdf.format(Date(message.timestamp)), color = TextMuted, fontSize = 10.sp)
                            if (isMe) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Read metrics",
                                    tint = if (message.status == MessageStatus.READ) OnlineGreen else TextMuted,
                                    modifier = Modifier.size(10.dp)
                                )
                            }
                        }
                    }
                }

                // Reaction chip metrics
                if (message.reactions.isNotEmpty()) {
                    Row(
                        modifier = Modifier.padding(top = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        message.reactions.forEach { reaction ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(DividerColor)
                                    .clickable { onReactClick(reaction.emoji) }
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text("${reaction.emoji} ${reaction.count}", color = Color.White, fontSize = 11.sp)
                            }
                        }
                    }
                }

                // Interaction controls context row
                Row(
                    modifier = Modifier.padding(top = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Reply", color = AccentIndigoLight, fontSize = 11.sp, modifier = Modifier.clickable { onReplyClick() })
                    Text("Thread", color = AccentIndigoLight, fontSize = 11.sp, modifier = Modifier.clickable { onThreadClick() })
                    if (isMe) {
                        Text("Delete", color = BusyRed, fontSize = 11.sp, modifier = Modifier.clickable { onDeleteClick() })
                    }
                }
            }
        }
    }
}

// Visual Waveform Player Composable (AudioMessagePlayer)
@Composable
fun AudioPlaybackRow(url: String) {
    var isPlaying by remember { mutableStateOf(false) }
    var playbackProgress by remember { mutableStateOf(0.4f) }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(DarkBackground)
            .padding(8.dp)
            .width(200.dp)
    ) {
        IconButton(
            onClick = { isPlaying = !isPlaying },
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Default.Close else Icons.Default.PlayArrow,
                contentDescription = "Trigger Audio",
                tint = Color.White
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            // Simulated audio amplitude waves
            Row(
                modifier = Modifier.fillMaxWidth().height(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                for (i in 0..15) {
                    val height = if (isPlaying) (8..16).random().dp else 6.dp
                    Box(
                        modifier = Modifier
                            .width(3.dp)
                            .height(height)
                            .clip(RoundedCornerShape(1.dp))
                            .background(if (i < 8) AccentIndigo else TextSecondary)
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text("00:42", color = TextMuted, fontSize = 10.sp)
        }
    }
}

// 8. IMAGE VIEW SCREEN
@Composable
fun ImageViewerScreen(url: String, onNavigateBack: () -> Unit) {
    Scaffold(
        topBar = {
            @OptIn(ExperimentalMaterial3Api::class)
            TopAppBar(
                title = { Text("Image attachment", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Black)
            )
        },
        containerColor = Color.Black
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = url,
                contentDescription = "Zoomed attachment",
                modifier = Modifier.fillMaxWidth(),
                contentScale = ContentScale.Fit
            )
        }
    }
}

// 9. VIDEO PLAYER SCREEN
@Composable
fun VideoPlayerScreen(url: String, onNavigateBack: () -> Unit) {
    Scaffold(
        topBar = {
            @OptIn(ExperimentalMaterial3Api::class)
            TopAppBar(
                title = { Text("Video presentation", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Black)
            )
        },
        containerColor = Color.Black
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.Place, "Video Player Icon", tint = AccentIndigo, modifier = Modifier.size(64.dp))
                Spacer(modifier = Modifier.height(16.dp))
                Text("Streaming Video Capture URL", color = Color.White, fontWeight = FontWeight.Bold)
                Text(url, color = TextSecondary, fontSize = 12.sp, modifier = Modifier.padding(16.dp))
                CircularProgressIndicator(color = AccentIndigo)
            }
        }
    }
}

// 10. THREAD SCREEN (Original anchor message + sequential replies list)
@Composable
fun ThreadScreen(
    messageId: String,
    messageRepository: MessageRepository,
    onNavigateBack: () -> Unit
) {
    // Collect thread replies mock list
    var textInput by remember { mutableStateOf("") }
    val mockReplies = remember {
        mutableStateListOf(
            Message("t1", "general", "bob_chen", "Bob Chen", null, "Thread reply 1: This design looks incredibly clean.", System.currentTimeMillis() - 400000),
            Message("t2", "general", "carol_jones", "Carol Jones", null, "Thread reply 2: Agreed! Ripple feedback works so elegantly on emoji chips.", System.currentTimeMillis() - 200000)
        )
    }

    Scaffold(
        topBar = {
            @OptIn(ExperimentalMaterial3Api::class)
            TopAppBar(
                title = { Text("Join Thread replies", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkSurface)
            )
        },
        containerColor = DarkBackground
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Anchor Message Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("@alice_martin", color = AccentIndigoLight, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Yes! We support full-bleed design and offline caching via Room tables.", color = Color.White)
                }
            }

            HorizontalDivider(color = DividerColor)

            // Thread Replies lazy col
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                items(mockReplies) { reply ->
                    ListItem(
                        headlineContent = { Text(reply.senderName, color = AccentIndigoLight, fontWeight = FontWeight.Bold, fontSize = 13.sp) },
                        supportingContent = { Text(reply.message, color = Color.White, fontSize = 14.sp) },
                        leadingContent = { UserAvatar(name = reply.senderName, size = 32) },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )
                    HorizontalDivider(color = DividerColor)
                }
            }

            // Input reply footer bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DarkSurface)
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = textInput,
                    onValueChange = { textInput = it },
                    placeholder = { Text("Reply in thread...", color = TextSecondary) },
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = {
                    if (textInput.trim().isNotEmpty()) {
                        mockReplies.add(
                            Message(
                                id = UUID.randomUUID().toString(),
                                channelId = "general",
                                senderId = "me",
                                senderName = "Demo User",
                                message = textInput,
                                timestamp = System.currentTimeMillis()
                            )
                        )
                        textInput = ""
                    }
                }) {
                    Icon(Icons.AutoMirrored.Filled.Send, "Send thread", tint = AccentIndigo)
                }
            }
        }
    }
}

// 11. SEARCH SCREEN
@Composable
fun SearchScreen(
    viewModel: SearchViewModel,
    onNavigateToChat: (String) -> Unit,
    onNavigateToUserProfile: (String) -> Unit,
    onNavigateBack: () -> Unit
) {
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val activeFilter by viewModel.activeFilter.collectAsStateWithLifecycle()
    val results by viewModel.searchResults.collectAsStateWithLifecycle()

    val filters = listOf("All", "Channels", "People")

    Scaffold(
        topBar = {
            @OptIn(ExperimentalMaterial3Api::class)
            TopAppBar(
                title = {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { viewModel.searchQuery.value = it },
                        placeholder = { Text("Search Rocketchat...", color = TextSecondary) },
                        modifier = Modifier.fillMaxWidth().testTag("search_text_input"),
                        singleLine = true
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkSurface)
            )
        },
        containerColor = DarkBackground
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Options chips row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                filters.forEach { filter ->
                    val isSelected = activeFilter == filter
                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.activeFilter.value = filter },
                        label = { Text(filter) }
                    )
                }
            }

            if (results.isEmpty() && searchQuery.isNotEmpty()) {
                EmptyStateBox(title = "No search hits", sub = "Verify spelling filters details")
            } else if (results.isEmpty()) {
                EmptyStateBox(title = "Find anything", sub = "Search channels or active people by entering user slugs")
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(results) { item ->
                        when (item) {
                            is Channel -> {
                                ListItem(
                                    headlineContent = { Text("# " + item.name, color = Color.White, fontWeight = FontWeight.Bold) },
                                    supportingContent = { Text(item.description ?: "Public Channel", color = TextSecondary) },
                                    leadingContent = { Icon(Icons.Default.Menu, null, tint = AccentIndigo) },
                                    modifier = Modifier.clickable { onNavigateToChat(item.id) },
                                    colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                                )
                            }
                            is User -> {
                                ListItem(
                                    headlineContent = { Text(item.name, color = Color.White, fontWeight = FontWeight.Bold) },
                                    supportingContent = { Text("@" + item.username + " — " + item.role, color = TextSecondary) },
                                    leadingContent = { UserAvatar(name = item.name, size = 36, status = item.status) },
                                    modifier = Modifier.clickable { onNavigateToUserProfile(item.id) },
                                    colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                                )
                            }
                        }
                        HorizontalDivider(color = DividerColor)
                    }
                }
            }
        }
    }
}

// 12. OWN PROFILE VIEW / EDIT DYNAMIC FORM
@Composable
fun OwnProfileScreen(viewModel: ProfileViewModel, onNavigateBack: () -> Unit) {
    val name by viewModel.name.collectAsStateWithLifecycle()
    val username by viewModel.username.collectAsStateWithLifecycle()
    val email by viewModel.email.collectAsStateWithLifecycle()
    val bio by viewModel.bio.collectAsStateWithLifecycle()
    val isSaving by viewModel.isSaving.collectAsStateWithLifecycle()

    val customStatusEmoji by viewModel.customStatusEmoji.collectAsStateWithLifecycle()
    val customStatusText by viewModel.customStatusText.collectAsStateWithLifecycle()
    val presenceState by viewModel.presenceState.collectAsStateWithLifecycle()

    var showPresenceSheet by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            @OptIn(ExperimentalMaterial3Api::class)
            TopAppBar(
                title = { Text("Edit Bio Profile", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.saveProfile() }) {
                        if (isSaving) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                        } else {
                            Icon(Icons.Default.Check, "Save Profile", tint = Color.White)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkSurface)
            )
        },
        containerColor = DarkBackground
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box {
                UserAvatar(name = name.ifEmpty { "Demo User" }, size = 100, status = presenceState)
                IconButton(
                    onClick = { showPresenceSheet = true },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .clip(CircleShape)
                        .background(AccentIndigo)
                        .size(32.dp)
                ) {
                    Icon(Icons.Default.Edit, "Edit Indicator", tint = Color.White, modifier = Modifier.size(16.dp))
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Custom Status Form Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = DarkSurface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Current status", color = Color.White, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = customStatusEmoji,
                            onValueChange = { viewModel.customStatusEmoji.value = it },
                            modifier = Modifier.width(60.dp),
                            placeholder = { Text("😊") }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        OutlinedTextField(
                            value = customStatusText,
                            onValueChange = { viewModel.customStatusText.value = it },
                            placeholder = { Text("Whatcha doing?", color = TextMuted) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = name,
                onValueChange = { viewModel.name.value = it },
                label = { Text("Display Name") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = username,
                onValueChange = { viewModel.username.value = it },
                label = { Text("Username") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = email,
                onValueChange = { viewModel.email.value = it },
                label = { Text("Email Address") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = bio,
                onValueChange = { viewModel.bio.value = it },
                label = { Text("Short Bio") },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 4
            )

            if (showPresenceSheet) {
                ModalBottomSheet(
                    onDismissRequest = { showPresenceSheet = false },
                    containerColor = DarkSurface
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp)
                    ) {
                        Text("Select Presence Mode", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 18.sp)
                        Spacer(modifier = Modifier.height(16.dp))
                        PresenceMenuItem("🟢 Online", UserStatus.ONLINE) {
                            viewModel.updatePresence(UserStatus.ONLINE)
                            showPresenceSheet = false
                        }
                        PresenceMenuItem("🟡 Away", UserStatus.AWAY) {
                            viewModel.updatePresence(UserStatus.AWAY)
                            showPresenceSheet = false
                        }
                        PresenceMenuItem("🔴 Busy", UserStatus.BUSY) {
                            viewModel.updatePresence(UserStatus.BUSY)
                            showPresenceSheet = false
                        }
                        PresenceMenuItem("⚫ Invisible / Offline", UserStatus.OFFLINE) {
                            viewModel.updatePresence(UserStatus.OFFLINE)
                            showPresenceSheet = false
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PresenceMenuItem(label: String, state: UserStatus, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(label, color = Color.White, fontWeight = FontWeight.SemiBold) },
        modifier = Modifier.clickable { onClick() },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
    )
}

// 13. USER PROFILE SCREEN (External other User's overview layout)
@Composable
fun UserProfileScreen(
    userId: String,
    userRepository: UserRepository,
    onNavigateToChat: (String) -> Unit,
    onNavigateBack: () -> Unit
) {
    val userFlow = remember(userId) { userRepository.getUserById(userId) }
    val user by userFlow.collectAsStateWithLifecycle(initialValue = null)

    Scaffold(
        topBar = {
            @OptIn(ExperimentalMaterial3Api::class)
            TopAppBar(
                title = { Text("Details User Info", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkSurface)
            )
        },
        containerColor = DarkBackground
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            UserAvatar(name = user?.name ?: "User", size = 120, status = user?.status)
            Spacer(modifier = Modifier.height(16.dp))
            Text(user?.name ?: "Loading...", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Text("@${user?.username ?: "unknown"}", color = TextSecondary, fontSize = 16.sp)

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("User Info details", fontWeight = FontWeight.Bold, color = Color.White)
                    Spacer(modifier = Modifier.height(8.dp))
                    RowDetail("Status Msg", "${user?.customStatusEmoji ?: "💬"} ${user?.customStatusText ?: "Available"}")
                    RowDetail("Role Tag", user?.role ?: "User")
                    RowDetail("TimeZone", user?.timezone ?: "UTC+0")
                    RowDetail("Bio Description", user?.bio ?: "No bio defined yet.")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { onNavigateToChat(userId) },
                colors = ButtonDefaults.buttonColors(containerColor = AccentIndigo),
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                Icon(Icons.Default.MailOutline, null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Open Secure DM dialogue")
            }
        }
    }
}

@Composable
fun RowDetail(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = TextMuted, fontSize = 14.sp)
        Text(value, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
    }
}

// 14. CHANNEL INFO SCREEN
@Composable
fun ChannelInfoScreen(
    viewModel: ChannelInfoViewModel,
    onNavigateToUserProfile: (String) -> Unit,
    onNavigateBack: () -> Unit
) {
    val channel by viewModel.channel.collectAsStateWithLifecycle()
    val members by viewModel.members.collectAsStateWithLifecycle()
    val pinned by viewModel.pinnedMessages.collectAsStateWithLifecycle()
    val starred by viewModel.starredMessages.collectAsStateWithLifecycle()

    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Members", "Pinned", "Starred")

    val context = LocalContext.current

    Scaffold(
        topBar = {
            @OptIn(ExperimentalMaterial3Api::class)
            TopAppBar(
                title = { Text(channel?.name ?: "Details info", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkSurface)
            )
        },
        containerColor = DarkBackground
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Topic section
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Topic space details", fontWeight = FontWeight.Bold, color = Color.White)
                    Text(channel?.topic ?: "Default topics not set", color = TextSecondary, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Description", fontWeight = FontWeight.Bold, color = Color.White)
                    Text(channel?.description ?: "No description provided", color = TextSecondary, fontSize = 14.sp)
                }
            }

            // Tab rows
            TabRow(selectedTabIndex = selectedTab, containerColor = DarkSurface) {
                tabs.forEachIndexed { idx, label ->
                    Tab(
                        selected = selectedTab == idx,
                        onClick = { selectedTab = idx },
                        text = { Text(label) }
                    )
                }
            }

            Box(modifier = Modifier.weight(1f)) {
                when (selectedTab) {
                    0 -> {
                        LazyColumn {
                            items(members) { user ->
                                ListItem(
                                    headlineContent = { Text(user.name, color = Color.White) },
                                    supportingContent = { Text("@" + user.username, color = TextSecondary) },
                                    leadingContent = { UserAvatar(name = user.name, size = 32, status = user.status) },
                                    modifier = Modifier.clickable { onNavigateToUserProfile(user.id) },
                                    colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                                )
                                HorizontalDivider(color = DividerColor)
                            }
                        }
                    }
                    1 -> {
                        if (pinned.isEmpty()) {
                            EmptyStateBox(title = "No pinned items", sub = "Long press some text within chats to pin them")
                        } else {
                            LazyColumn {
                                items(pinned) { pin ->
                                    ListItem(
                                        headlineContent = { Text(pin.senderName, color = AccentIndigoLight) },
                                        supportingContent = { Text(pin.message, color = Color.White) },
                                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                                    )
                                    HorizontalDivider(color = DividerColor)
                                }
                            }
                        }
                    }
                    2 -> {
                        if (starred.isEmpty()) {
                            EmptyStateBox(title = "No starred items", sub = "Star messages to access secure clips")
                        } else {
                            LazyColumn {
                                items(starred) { star ->
                                    ListItem(
                                        headlineContent = { Text(star.senderName, color = AccentIndigoLight) },
                                        supportingContent = { Text(star.message, color = Color.White) },
                                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                                    )
                                    HorizontalDivider(color = DividerColor)
                                }
                            }
                        }
                    }
                }
            }

            Button(
                onClick = {
                    viewModel.leaveChannel()
                    Toast.makeText(context, "Leaved Channel successfully!", Toast.LENGTH_SHORT).show()
                    onNavigateBack()
                },
                colors = ButtonDefaults.buttonColors(containerColor = BusyRed),
                modifier = Modifier
                    .fillModifier()
                    .padding(16.dp)
                    .height(48.dp)
            ) {
                Text("Leave Channel space")
            }
        }
    }
}

// 15. SETTINGS SCREEN
@Composable
fun SettingsScreen(
    onNavigateToProfile: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    onNavigateToAppearance: () -> Unit,
    onNavigateToSecurity: () -> Unit,
    onLogout: () -> Unit,
    onNavigateBack: () -> Unit
) {
    Scaffold(
        topBar = {
            @OptIn(ExperimentalMaterial3Api::class)
            TopAppBar(
                title = { Text("Settings configurations", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkSurface)
            )
        },
        containerColor = DarkBackground
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            ListItem(
                headlineContent = { Text("Profile adjustments", color = Color.White, fontWeight = FontWeight.Bold) },
                supportingContent = { Text("Edit display names, usernames, and email", color = TextSecondary) },
                leadingContent = { Icon(Icons.Default.Person, null, tint = AccentIndigo) },
                modifier = Modifier.clickable { onNavigateToProfile() }
            )
            HorizontalDivider(color = DividerColor)

            ListItem(
                headlineContent = { Text("Sound notifications settings", color = Color.White, fontWeight = FontWeight.Bold) },
                supportingContent = { Text("Quiets overrides, sounds vibration patterns", color = TextSecondary) },
                leadingContent = { Icon(Icons.Default.Notifications, null, tint = AccentIndigo) },
                modifier = Modifier.clickable { onNavigateToNotifications() }
            )
            HorizontalDivider(color = DividerColor)

            ListItem(
                headlineContent = { Text("Appearance modifications", color = Color.White, fontWeight = FontWeight.Bold) },
                supportingContent = { Text("Adjust layout density, font modifiers, sizes", color = TextSecondary) },
                leadingContent = { Icon(Icons.Default.Refresh, null, tint = AccentIndigo) },
                modifier = Modifier.clickable { onNavigateToAppearance() }
            )
            HorizontalDivider(color = DividerColor)

            ListItem(
                headlineContent = { Text("Access & Security credentials", color = Color.White, fontWeight = FontWeight.Bold) },
                supportingContent = { Text("Biometrics, TOTP, session terminations", color = TextSecondary) },
                leadingContent = { Icon(Icons.Default.Lock, null, tint = AccentIndigo) },
                modifier = Modifier.clickable { onNavigateToSecurity() }
            )
            HorizontalDivider(color = DividerColor)

            Spacer(modifier = Modifier.height(48.dp))

            Button(
                onClick = onLogout,
                colors = ButtonDefaults.buttonColors(containerColor = BusyRed),
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Text("Logout Account Session", fontWeight = FontWeight.Bold)
            }
        }
    }
}

// 16. NOTIFICATION SETTINGS SCREEN
@Composable
fun NotificationSettingsScreen(onNavigateBack: () -> Unit) {
    var globalPush by remember { mutableStateOf(true) }
    var inAppSounds by remember { mutableStateOf(true) }
    var vibrationsMode by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            @OptIn(ExperimentalMaterial3Api::class)
            TopAppBar(
                title = { Text("Notifications Config", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkSurface)
            )
        },
        containerColor = DarkBackground
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Enable All Push Notifications", color = Color.White)
                Switch(checked = globalPush, onCheckedChange = { globalPush = it })
            }
            HorizontalDivider(color = DividerColor)

            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Play in-app audio alerts", color = Color.White)
                Switch(checked = inAppSounds, onCheckedChange = { inAppSounds = it })
            }
            HorizontalDivider(color = DividerColor)

            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Vibrations triggers", color = Color.White)
                Switch(checked = vibrationsMode, onCheckedChange = { vibrationsMode = it })
            }
        }
    }
}

// 17. APPEARANCE SETTINGS SCREEN
@Composable
fun AppearanceSettingsScreen(onNavigateBack: () -> Unit) {
    var fontScaleSlider by remember { mutableStateOf(14f) }
    var compactModeState by remember { mutableStateOf(true) }

    Scaffold(
        topBar = {
            @OptIn(ExperimentalMaterial3Api::class)
            TopAppBar(
                title = { Text("Appearance modifiers", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkSurface)
            )
        },
        containerColor = DarkBackground
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp)
        ) {
            Text("Adjust Text Font Sizing", color = Color.White, fontWeight = FontWeight.Bold)
            Slider(
                value = fontScaleSlider,
                onValueChange = { fontScaleSlider = it },
                valueRange = 10f..24f
            )
            Text("Selected Size: ${fontScaleSlider.toInt()} sp", color = TextSecondary, fontSize = 12.sp)

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Enable Compact Chat Bubble Layout", color = Color.White)
                Switch(checked = compactModeState, onCheckedChange = { compactModeState = it })
            }
        }
    }
}

// 18. SECURITY SETTINGS SCREEN
@Composable
fun SecuritySettingsScreen(onNavigateBack: () -> Unit) {
    var totpAuthenticatorState by remember { mutableStateOf(false) }
    var biometricLockState by remember { mutableStateOf(true) }

    Scaffold(
        topBar = {
            @OptIn(ExperimentalMaterial3Api::class)
            TopAppBar(
                title = { Text("Access Rules Security", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkSurface)
            )
        },
        containerColor = DarkBackground
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Require Two-Factor Authenticator (TOTP)", color = Color.White)
                Switch(checked = totpAuthenticatorState, onCheckedChange = { totpAuthenticatorState = it })
            }
            HorizontalDivider(color = DividerColor)

            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Fingerprint Biometric system locks", color = Color.White)
                Switch(checked = biometricLockState, onCheckedChange = { biometricLockState = it })
            }
        }
    }
}

// 19. NEW DIRECT MESSAGE WINDOW (Start Dialogue selector)
@Composable
fun NewDMScreen(
    viewModel: HomeViewModel,
    onNavigateToChat: (String) -> Unit,
    onNavigateBack: () -> Unit
) {
    val users by viewModel.users.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            @OptIn(ExperimentalMaterial3Api::class)
            TopAppBar(
                title = { Text("Select User conversation", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkSurface)
            )
        },
        containerColor = DarkBackground
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            items(users) { usr ->
                ListItem(
                    headlineContent = { Text(usr.name, color = Color.White, fontWeight = FontWeight.Bold) },
                    supportingContent = { Text("@" + usr.username, color = TextSecondary) },
                    leadingContent = { UserAvatar(name = usr.name, size = 40, status = usr.status) },
                    modifier = Modifier.clickable {
                        // Create dm thread with userId
                        onNavigateToChat("general") // Default chat map
                    }
                )
                HorizontalDivider(color = DividerColor)
            }
        }
    }
}

// 20. CREATE CHANNEL SCREEN
@Composable
fun CreateChannelScreen(
    viewModel: HomeViewModel,
    onNavigateToChat: (String) -> Unit,
    onNavigateBack: () -> Unit
) {
    var nameInput by remember { mutableStateOf("") }
    var descriptionInput by remember { mutableStateOf("") }
    var privateChannelType by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            @OptIn(ExperimentalMaterial3Api::class)
            TopAppBar(
                title = { Text("Assemble New Channels", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkSurface)
            )
        },
        containerColor = DarkBackground
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp)
        ) {
            OutlinedTextField(
                value = nameInput,
                onValueChange = { nameInput = it },
                label = { Text("Channel Slug-name (e.g. general)") },
                modifier = Modifier.fillMaxWidth().testTag("create_channel_name")
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = descriptionInput,
                onValueChange = { descriptionInput = it },
                label = { Text("Workspace presentation topic description") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Private Channels type access controls", color = Color.White)
                Switch(checked = privateChannelType, onCheckedChange = { privateChannelType = it })
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    if (nameInput.trim().isNotEmpty()) {
                        Toast.makeText(context, "Channel #$nameInput created!", Toast.LENGTH_SHORT).show()
                        onNavigateBack()
                    }
                },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AccentIndigo)
            ) {
                Text("Compile Channels parameters Group")
            }
        }
    }
}

// 21. STARRED MESSAGES LOGS FEED SCREEN
@Composable
fun StarredMessagesScreen(
    viewModel: HomeViewModel,
    onNavigateToChat: (String) -> Unit,
    onNavigateBack: () -> Unit
) {
    val starred by viewModel.starredMessages.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            @OptIn(ExperimentalMaterial3Api::class)
            TopAppBar(
                title = { Text("Starred Bookmarks", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkSurface)
            )
        },
        containerColor = DarkBackground
    ) { innerPadding ->
        if (starred.isEmpty()) {
            EmptyStateBox(title = "No starred logs found", sub = "Tag important clips to retrieve them quickly later")
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                items(starred) { text ->
                    ListItem(
                        headlineContent = { Text(text.senderName, color = AccentIndigoLight, fontWeight = FontWeight.Bold) },
                        supportingContent = { Text(text.message, color = Color.White) },
                        modifier = Modifier.clickable { onNavigateToChat(text.channelId) }
                    )
                    HorizontalDivider(color = DividerColor)
                }
            }
        }
    }
}

// 22. MENTIONS LOGS SCREEN
@Composable
fun MentionsScreen(
    viewModel: HomeViewModel,
    onNavigateToChat: (String) -> Unit,
    onNavigateBack: () -> Unit
) {
    Scaffold(
        topBar = {
            @OptIn(ExperimentalMaterial3Api::class)
            TopAppBar(
                title = { Text("Mentions Timeline", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkSurface)
            )
        },
        containerColor = DarkBackground
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(Icons.Default.Notifications, null, tint = AccentIndigo, modifier = Modifier.size(64.dp))
            Spacer(modifier = Modifier.height(16.dp))
            Text("No alert notifications yet", color = Color.White, fontWeight = FontWeight.Bold)
            Text("When users ping @me inside chats, they will log chronological timeline items here.", color = TextSecondary, textAlign = TextAlign.Center, modifier = Modifier.padding(16.dp))
        }
    }
}

// 23. SHARED FILES GRID AND ATTACHMENTS REPOSITORY SCREEN
@Composable
fun SharedFilesScreen(
    viewModel: HomeViewModel,
    onNavigateToChat: (String) -> Unit,
    onNavigateBack: () -> Unit
) {
    val itemsList = listOf(
        Attachment("Mockup_Design.jpg", "image", "https://picsum.photos/400/300?random=2", "150 KB"),
        Attachment("Client_Agreement.pdf", "file", "https://example.com/spec.pdf", "240 KB"),
        Attachment("SoundNotes.mp3", "audio", "https://example.com/audio", "380 KB")
    )

    Scaffold(
        topBar = {
            @OptIn(ExperimentalMaterial3Api::class)
            TopAppBar(
                title = { Text("Shared Media Archives", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkSurface)
            )
        },
        containerColor = DarkBackground
    ) { innerPadding ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(itemsList) { attachment ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    modifier = Modifier.clickable { onNavigateToChat("general") }
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        if (attachment.type == "image") {
                            AsyncImage(
                                model = attachment.url,
                                contentDescription = null,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(100.dp)
                                    .clip(RoundedCornerShape(6.dp))
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(100.dp)
                                    .background(Color.DarkGray),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Share, null, tint = Color.White)
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(attachment.name, color = Color.White, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(attachment.size, color = TextMuted, fontSize = 10.sp)
                    }
                }
            }
        }
    }
}

// 24. ADMIN CONTROL COHESIVE DASHBOARD SCREEN
@Composable
fun AdminPanelScreen(
    viewModel: AdminViewModel,
    onNavigateToUsers: () -> Unit,
    onNavigateToChannels: () -> Unit,
    onNavigateToLogs: () -> Unit,
    onNavigateBack: () -> Unit
) {
    val totalUsers by viewModel.totalUsers.collectAsStateWithLifecycle()
    val onlineNow by viewModel.onlineNow.collectAsStateWithLifecycle()
    val totalStorage by viewModel.totalStorage.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            @OptIn(ExperimentalMaterial3Api::class)
            TopAppBar(
                title = { Text("Admin Panel workspace", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkSurface)
            )
        },
        containerColor = DarkBackground
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp)
        ) {
            Text("Workspace operational metrics", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 18.sp)
            Spacer(modifier = Modifier.height(16.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                AdminMetricCard("Total Users count", totalUsers.toString(), modifier = Modifier.weight(1f))
                AdminMetricCard("Active Online nodes", onlineNow.toString(), modifier = Modifier.weight(1f))
            }
            Spacer(modifier = Modifier.height(12.dp))
            AdminMetricCard("Mocks Storage consumption", totalStorage, modifier = Modifier.fillMaxWidth())

            Spacer(modifier = Modifier.height(24.dp))

            Card(colors = CardDefaults.cardColors(containerColor = DarkSurface)) {
                Column {
                    PreferenceNavigationItem(Icons.Default.Person, "Manage Active Users Database", "Change security role clearance variables") { onNavigateToUsers() }
                    HorizontalDivider(color = DividerColor)
                    PreferenceNavigationItem(Icons.Default.Menu, "Audit Channels and Pools", "Archive obsolete channels, prune message arrays") { onNavigateToChannels() }
                    HorizontalDivider(color = DividerColor)
                    PreferenceNavigationItem(Icons.Default.MailOutline, "View Audit Logs Trace", "Trace security activity records chronologically") { onNavigateToLogs() }
                }
            }
        }
    }
}

@Composable
fun AdminMetricCard(title: String, score: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = DarkSurface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, color = TextSecondary, fontSize = 12.sp)
            Spacer(modifier = Modifier.height(6.dp))
            Text(score, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        }
    }
}

// 25. ADMIN USERS MANAGEMENT SCREEN
@Composable
fun AdminUsersScreen(viewModel: AdminViewModel, onNavigateBack: () -> Unit) {
    val users by viewModel.users.collectAsStateWithLifecycle()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            @OptIn(ExperimentalMaterial3Api::class)
            TopAppBar(
                title = { Text("Database Users status controls", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkSurface)
            )
        },
        containerColor = DarkBackground
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            items(users) { user ->
                ListItem(
                    headlineContent = { Text(user.name, color = Color.White) },
                    supportingContent = { Text("@" + user.username + " — " + user.role, color = TextSecondary) },
                    leadingContent = { UserAvatar(name = user.name, size = 36, status = user.status) },
                    trailingContent = {
                        TextButton(onClick = {
                            Toast.makeText(context, "Clearance updated for ${user.name}", Toast.LENGTH_SHORT).show()
                        }) {
                            Text("Edit Role", color = AccentIndigoLight)
                        }
                    }
                )
                HorizontalDivider(color = DividerColor)
            }
        }
    }
}

// 26. ADMIN CHANNELS MANAGEMENT SCREEN
@Composable
fun AdminChannelsScreen(viewModel: AdminViewModel, onNavigateBack: () -> Unit) {
    val channels by viewModel.channels.collectAsStateWithLifecycle()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            @OptIn(ExperimentalMaterial3Api::class)
            TopAppBar(
                title = { Text("Audit channel pools", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkSurface)
            )
        },
        containerColor = DarkBackground
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            items(channels) { channel ->
                ListItem(
                    headlineContent = { Text("# " + channel.name, color = Color.White) },
                    supportingContent = { Text("${channel.memberCount} active nodes in thread", color = TextSecondary) },
                    trailingContent = {
                        TextButton(onClick = {
                            Toast.makeText(context, "Archived channel successfully!", Toast.LENGTH_SHORT).show()
                        }) {
                            Text("Archive", color = BusyRed)
                        }
                    }
                )
                HorizontalDivider(color = DividerColor)
            }
        }
    }
}

// 27. ADMIN SECURE LOGS AUDITING SCREEN
@Composable
fun AdminLogsScreen(viewModel: AdminViewModel, onNavigateBack: () -> Unit) {
    val logs by viewModel.logs.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            @OptIn(ExperimentalMaterial3Api::class)
            TopAppBar(
                title = { Text("Audit secure active logs", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkSurface)
            )
        },
        containerColor = DarkBackground
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            items(logs) { log ->
                val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                val time = sdf.format(Date(log.timestamp))

                ListItem(
                    headlineContent = { Text(log.description, color = Color.White, fontSize = 14.sp) },
                    supportingContent = { Text("Triggered by @${log.modName} at $time", color = TextSecondary) },
                    leadingContent = { Icon(Icons.Default.PlayArrow, null, tint = AccentIndigo) }
                )
                HorizontalDivider(color = DividerColor)
            }
        }
    }
}

// Common Empty States components
@Composable
fun EmptyStateBox(title: String, sub: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Default.Notifications, null, tint = AccentIndigo, modifier = Modifier.size(50.dp))
        Spacer(modifier = Modifier.height(16.dp))
        Text(title, color = Color.White, fontWeight = FontWeight.Bold)
        Text(sub, color = TextSecondary, textAlign = TextAlign.Center, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 24.dp))
    }
}

private inline fun Modifier.fillModifier(): Modifier = this.fillMaxWidth()

private val Int.absoluteValue: Int
    get() = if (this < 0) -this else this

// Scale and alpha animation extension for layout transitions
private fun Modifier.scale(scale: Float): Modifier = this.graphicsLayer(scaleX = scale, scaleY = scale)
private fun Modifier.alpha(alpha: Float): Modifier = this.graphicsLayer(alpha = alpha)
