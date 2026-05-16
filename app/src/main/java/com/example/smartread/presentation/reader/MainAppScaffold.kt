package com.example.smartread.presentation.reader


import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
//import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController

// 定义四个导航页面路由
sealed class BottomNavItem(
    val route: String,
    val title: String,
    val icon: @Composable () -> Unit
) {
    object Home : BottomNavItem(
        route = "home",
        title = "首页",
        icon = { Icon(Icons.Filled.Home, contentDescription = null) }
    )
    object Search : BottomNavItem(
        route = "search",
        title = "搜索",
        icon = { Icon(Icons.Filled.Search, contentDescription = null) }
    )
    object BookShelf : BottomNavItem(
        route = "bookshelf",
        title = "书架",
        icon = { Icon(Icons.Filled.Search, contentDescription = null) }
    )
    object Mine : BottomNavItem(
        route = "mine",
        title = "我的",
        icon = { Icon(Icons.Filled.Person, contentDescription = null) }
    )
}

// 应用主页面：顶部内容 + 底部导航
@Composable
fun MainAppScaffold() {
    val navCtrl = rememberNavController()
    val navList = listOf(
        BottomNavItem.Home,
        BottomNavItem.Search,
        BottomNavItem.BookShelf,
        BottomNavItem.Mine
    )
    val currentNav by navCtrl.currentBackStackEntryAsState()
    val currentRoute = currentNav?.destination

    Scaffold(
        bottomBar = {
            NavigationBar {
                navList.forEach { item ->
                    val isSelect = currentRoute?.hierarchy?.any { it.route == item.route } == true
                    NavigationBarItem(
                        selected = isSelect,
                        onClick = {
                            navCtrl.navigate(item.route) {
                                popUpTo(navCtrl.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = item.icon,
                        label = { Text(text = item.title) }
                    )
                }
            }
        }
    ) { paddingValues ->
        // 页面路由容器
        NavHost(
            navController = navCtrl,
            startDestination = BottomNavItem.Home.route,
            modifier = Modifier.fillMaxSize().padding(paddingValues)
        ) {
            composable(BottomNavItem.Home.route) { HomePage() }
            composable(BottomNavItem.Search.route) { SearchPage() }
            composable(BottomNavItem.BookShelf.route) { BookShelfPage() }
            composable(BottomNavItem.Mine.route) { MinePage() }
        }
    }
}