package com.group4.calendarapplication

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.navigation.compose.composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.NavHost
import com.group4.calendarapplication.models.Group
import com.group4.calendarapplication.views.CalendarView
import com.group4.calendarapplication.views.HomeView
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember

enum class Destination(
    val route: String,
    val label: String,
    val icon: ImageVector,
    val contentDescription: String
) {
    HOME("home", "Groups", Icons.Default.Create, "Home"),
    CALENDAR("calendar", "Calendar", Icons.Default.DateRange, "Calendar"),
}

@Composable
fun NavHost(
    groups: List<Group>,
    navController: NavHostController,
    navHistory: NavigationHistory,
    startDestination: Destination,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController,
        startDestination = startDestination.route
    ) {
        Destination.entries.forEach { destination ->
            composable(destination.route) {
                LaunchedEffect(destination.route) {
                    navHistory.push(destination.route)
                }
                when (destination) {
                    Destination.HOME -> HomeView(groups, modifier)
                    Destination.CALENDAR -> CalendarView(groups, modifier)
                }
            }
        }
    }
}

@Composable
fun NavBar(navController: NavHostController, navHistory: NavigationHistory) {
    NavigationBar(windowInsets = NavigationBarDefaults.windowInsets) {
        Destination.entries.forEach { destination ->
            NavigationBarItem(
                selected = destination.route == navHistory.current(),
                onClick = {
                    navController.navigate(destination.route) {
                        launchSingleTop = true
                    }
                    navHistory.push(destination.route)
                },
                icon = { Icon(destination.icon, contentDescription = destination.contentDescription) },
                label = { Text(destination.label) }
            )
        }
    }
}

@Composable
fun App(groups: List<Group>) {
    val navController = rememberNavController()
    val startDestination = Destination.HOME
    val navHistory = remember { NavigationHistory(startDestination.route) }

    AppBackHandler(navController)

    BackHandler(enabled = true) {
        val previous = navHistory.pop()
        if (previous != null) {
            navController.navigate(previous) {
                popUpTo(previous) { inclusive = true }
            }
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
        bottomBar = { NavBar(navController, navHistory) }
    ) { innerPadding ->
        NavHost(
            groups,
            navController,
            navHistory,
            startDestination,
            modifier = Modifier.padding(innerPadding)
        )
    }
}
@Composable
fun AppBackHandler(navController: NavHostController) {
    BackHandler(enabled = navController.previousBackStackEntry != null) {
        navController.popBackStack()
    }
}

class NavigationHistory {
    constructor(startItem: String) {
        stack.add(startItem)
    }

    val stack = mutableStateListOf<String>()

    fun push(route: String) {
        if (stack.lastOrNull() != route) stack.add(route)
    }

    fun pop(): String? {
        if (stack.size > 1) {
            stack.removeAt(stack.lastIndex)
            return stack.last()
        }
        return null
    }

    fun current(): String? = stack.lastOrNull()
}