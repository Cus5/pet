package com.petfather.gyeonsimi2.ui

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.petfather.gyeonsimi2.ui.ai.AiScreen
import com.petfather.gyeonsimi2.ui.community.CommunityScreen
import com.petfather.gyeonsimi2.ui.community.PostDetailScreen
import com.petfather.gyeonsimi2.ui.community.WritePostScreen
import com.petfather.gyeonsimi2.ui.data.DataNoticeHost
import com.petfather.gyeonsimi2.ui.feed.FeedListScreen
import com.petfather.gyeonsimi2.ui.feedback.FeedbackScreen
import com.petfather.gyeonsimi2.ui.health.HealthScreen
import com.petfather.gyeonsimi2.ui.home.HomeScreen
import com.petfather.gyeonsimi2.ui.pet.PetSheet
import com.petfather.gyeonsimi2.ui.register.RegisterScreen

enum class Tab(val route: String, val label: String, val icon: ImageVector, val selectedIcon: ImageVector) {
    HOME("home", "홈", Icons.Outlined.Home, Icons.Filled.Home),
    COMMUNITY("community", "커뮤니티", Icons.Outlined.Groups, Icons.Filled.Groups),
    REGISTER("register", "등록", Icons.Outlined.AddCircleOutline, Icons.Filled.AddCircle),
    HEALTH("health", "건강", Icons.Outlined.Insights, Icons.Filled.Insights),
    AI("ai", "AI 분석", Icons.Outlined.AutoAwesome, Icons.Filled.AutoAwesome),
}

private object Routes {
    const val FEEDS = "feeds"
    const val WRITE = "write"
    const val FEEDBACK = "feedback"
    const val POST = "post/{id}"
    const val EDIT_PET = "pet/edit/{id}"
    fun post(id: Long) = "post/$id"
    fun editPet(id: Long) = "pet/edit/$id"
}

@Composable
fun GyeonsimiApp(nav: NavHostController = rememberNavController()) {
    val backStack by nav.currentBackStackEntryAsState()
    val route = backStack?.destination?.route
    val currentTab = Tab.entries.firstOrNull { it.route == route }
    var showPetSheet by rememberSaveable { mutableStateOf(false) }

    fun goTab(tab: Tab) {
        nav.navigate(tab.route) {
            popUpTo(nav.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (currentTab != null) GyeonsimiBottomBar(currentTab, ::goTab)
        },
    ) { padding ->
        val onPaw = { showPetSheet = true }
        NavHost(
            navController = nav,
            startDestination = Tab.HOME.route,
            modifier = Modifier.padding(padding).consumeWindowInsets(padding),
        ) {
            composable(Tab.HOME.route) {
                HomeScreen(
                    onPawClick = onPaw,
                    onRegister = { goTab(Tab.REGISTER) },
                    onFeedList = { nav.navigate(Routes.FEEDS) },
                    onFeedback = { nav.navigate(Routes.FEEDBACK) },
                )
            }
            composable(Tab.COMMUNITY.route) {
                CommunityScreen(onWrite = { nav.navigate(Routes.WRITE) }, onOpenPost = { nav.navigate(Routes.post(it)) })
            }
            composable(Tab.REGISTER.route) {
                RegisterScreen(editId = null, onSaved = { goTab(Tab.HOME) }, onBack = null)
            }
            composable(Tab.HEALTH.route) { HealthScreen(onPawClick = onPaw, onRegister = { goTab(Tab.REGISTER) }) }
            composable(Tab.AI.route) { AiScreen(onPawClick = onPaw, onRegister = { goTab(Tab.REGISTER) }) }
            composable(Routes.FEEDS) { FeedListScreen(onBack = { nav.popBackStack() }) }
            composable(Routes.WRITE) { WritePostScreen(onDone = { nav.popBackStack() }) }
            composable(Routes.FEEDBACK) { FeedbackScreen(onBack = { nav.popBackStack() }) }
            composable(Routes.POST, arguments = listOf(navArgument("id") { type = NavType.LongType })) { entry ->
                PostDetailScreen(postId = entry.arguments?.getLong("id") ?: 0L, onBack = { nav.popBackStack() })
            }
            composable(Routes.EDIT_PET, arguments = listOf(navArgument("id") { type = NavType.LongType })) { entry ->
                val id = entry.arguments?.getLong("id") ?: 0L
                RegisterScreen(editId = id, onSaved = { nav.popBackStack() }, onBack = { nav.popBackStack() })
            }
        }
    }

    if (showPetSheet) {
        PetSheet(
            onDismiss = { showPetSheet = false },
            onAddPet = { showPetSheet = false; goTab(Tab.REGISTER) },
            onEditPet = { id -> showPetSheet = false; nav.navigate(Routes.editPet(id)) },
            onFeedback = { showPetSheet = false; nav.navigate(Routes.FEEDBACK) },
        )
    }

    // 첫 실행 때 한 번: 무엇을 서버로 보내는지 알리고 선택을 받습니다.
    DataNoticeHost()
}

@Composable
fun GyeonsimiBottomBar(currentTab: Tab, onSelect: (Tab) -> Unit) {
    NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainer) {
        Tab.entries.forEach { tab ->
            val selected = tab == currentTab
            NavigationBarItem(
                selected = selected,
                onClick = { onSelect(tab) },
                icon = { Icon(if (selected) tab.selectedIcon else tab.icon, contentDescription = null) },
                label = { Text(tab.label) },
            )
        }
    }
}
