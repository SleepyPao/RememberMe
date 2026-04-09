package com.int4074.wordduo.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.int4074.wordduo.data.PracticeMode

@Composable
fun WordDuoApp() {
    val context = LocalContext.current
    val vm: AppViewModel = viewModel(factory = AppViewModel.factory(context))
    val library by vm.library.collectAsStateWithLifecycle()
    val authState by vm.authState.collectAsStateWithLifecycle()
    val session by vm.session.collectAsStateWithLifecycle()
    val battle by vm.battle.collectAsStateWithLifecycle()
    val essayReview by vm.essayReview.collectAsStateWithLifecycle()
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }

    if (!authState.isLoggedIn) {
        AuthEntryScreen(
            authState = authState,
            onLogin = vm::login,
            onRegister = vm::register
        )
        return
    }

    LaunchedEffect(session.feedback) {
        session.feedback?.let { snackbarHostState.showSnackbar(it) }
    }
    LaunchedEffect(battle.statusMessage) {
        battle.statusMessage?.let { snackbarHostState.showSnackbar(it) }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        bottomBar = {
            val current by navController.currentBackStackEntryAsState()
            val route = current?.destination?.route
            BottomNavBar(route = route) { destination ->
                navController.navigate(destination) {
                    popUpTo(navController.graph.findStartDestination().id) {
                        saveState = true
                    }
                    restoreState = true
                    launchSingleTop = true
                }
            }
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = Modifier.padding(padding)
        ) {
            composable("home") {
                HomeScreen(
                    library = library,
                    onStartMode = {
                        vm.startSession(it)
                        navController.navigate("practice")
                    }
                )
            }
            composable("practice") {
                PracticeScreen(
                    library = library,
                    session = session,
                    onModeSelected = vm::startSession,
                    onReciteMark = vm::markRecite,
                    onTypedInput = vm::updateInput,
                    onSubmitTyped = vm::submitTypedAnswer,
                    onReveal = vm::revealAnswer,
                    onRecognition = vm::setRecognition,
                    onSubmitPronunciation = vm::submitPronunciationResult,
                    onNextBatch = { vm.startSession(session.mode) },
                    onBackHome = {
                        navController.navigate("home") {
                            popUpTo("home") { inclusive = false }
                            launchSingleTop = true
                        }
                    }
                )
            }
            composable("duel") {
                OnlineBattleScreen(
                    battle = battle,
                    onHostLan = vm::hostLanBattle,
                    onJoinCodeChange = vm::updateBattleJoinCode,
                    onJoinLan = vm::joinLanBattle,
                    onStartAi = vm::startAiBattle,
                    onInputChange = vm::updateBattleInput,
                    onSubmitAnswer = vm::submitBattleAnswer,
                    onLeave = {
                        vm.leaveBattle()
                        navController.navigate("home") {
                            popUpTo("home") { inclusive = false }
                            launchSingleTop = true
                        }
                    }
                )
            }
            composable("essay-review") {
                EssayReviewScreen(
                    essayState = essayReview,
                    onTextChange = vm::updateEssayText,
                    onAnalyze = vm::analyzeEssay,
                    onClear = vm::clearEssayReview,
                    onImportText = vm::importEssayText,
                    onStatus = vm::setEssayStatus,
                    onBackHome = {
                        navController.navigate("home") {
                            popUpTo("home") { inclusive = false }
                            launchSingleTop = true
                        }
                    }
                )
            }
            composable("mistakes") {
                MistakesScreen(
                    library = library,
                    onReview = {
                        vm.startSession(PracticeMode.MistakeReview)
                        navController.navigate("practice")
                    },
                    onBack = {
                        navController.navigate("profile") {
                            popUpTo("profile") { inclusive = false }
                            launchSingleTop = true
                        }
                    }
                )
            }
            composable("library") {
                LibraryScreen(
                    library = library,
                    onBack = {
                        navController.navigate("profile") {
                            popUpTo("profile") { inclusive = false }
                            launchSingleTop = true
                        }
                    }
                )
            }
            composable("profile") {
                ProfileScreen(
                    stats = library.stats,
                    onGoalChanged = vm::updateGoal,
                    onLogout = vm::logout,
                    currentUser = authState.currentUser,
                    avatarUri = authState.avatarUri,
                    onAvatarSelected = vm::updateAvatar,
                    onOpenMistakes = { navController.navigate("mistakes") },
                    onOpenLibrary = { navController.navigate("library") },
                    onBackHome = {
                        navController.navigate("home") {
                            popUpTo("home") { inclusive = false }
                            launchSingleTop = true
                        }
                    }
                )
            }
        }
    }
}

private data class NavItem(
    val route: String,
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val accent: Color
)

private val navItems = listOf(
    NavItem("home", "首页", Icons.Default.Home, Color(0xFF978BFF)),
    NavItem("practice", "练习", Icons.Default.Headphones, Color(0xFFFF8D72)),
    NavItem("duel", "对战", Icons.Default.Groups, Color(0xFFFF9E7A)),
    NavItem("essay-review", "AI检测", Icons.Default.AutoAwesome, Color(0xFF8F86FF)),
    NavItem("profile", "我的", Icons.Default.Person, Color(0xFF9DB2FF))
)

@Composable
private fun BottomNavBar(route: String?, onNavigate: (String) -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Surface(
            color = Color(0xFFFDF7EF),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(32.dp),
            shadowElevation = 18.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                navItems.forEach { item ->
                    val selected = when (item.route) {
                        "profile" -> route == "profile" || route == "mistakes" || route == "library"
                        else -> route == item.route
                    }
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onNavigate(item.route) }
                            .padding(horizontal = 2.dp, vertical = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        GlossyIconBubble(
                            icon = item.icon,
                            accent = item.accent,
                            selected = selected,
                            size = if (selected) 50.dp else 42.dp
                        )
                        Text(
                            text = item.label,
                            fontSize = 11.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                            color = if (selected) item.accent else Color(0xFF968A80)
                        )
                    }
                }
            }
        }
    }
}



