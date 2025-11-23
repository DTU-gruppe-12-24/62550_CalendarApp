package com.group4.calendarapplication

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.navigation.compose.composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.NavHost
import com.group4.calendarapplication.models.Group
import com.group4.calendarapplication.views.CalendarView
import com.group4.calendarapplication.views.HomeView
import com.group4.calendarapplication.views.NotificationsView

enum class Destination(
    val route: String,
    val label: String,
    val icon: ImageVector,
    val contentDescription: String
) {
    HOME("home", "Home", Icons.Default.Home, "Home"),
    CALENDAR("calendar", "Calendar", Icons.Default.DateRange, "Calendar"),
}

@Composable
fun NavHost(
    groups: List<Group>,
    navController: NavHostController,
    startDestination: Destination,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController,
        startDestination = startDestination.route
    ) {
        Destination.entries.forEach { destination ->
            composable(destination.route) {
                when (destination) {
                    Destination.HOME -> HomeView(groups, modifier)
                    Destination.CALENDAR -> CalendarView(groups, modifier)
                }
            }
        }
    }
}

@Composable
fun NavBar(navController: NavHostController) {
    var selectedDestination by rememberSaveable { mutableStateOf("") }
    navController.addOnDestinationChangedListener { controller, destination, arguments ->
        selectedDestination = destination.route ?: ""
    }
    NavigationBar(windowInsets = NavigationBarDefaults.windowInsets) {
        Destination.entries.forEachIndexed { index, destination ->
            NavigationBarItem(
                selected = destination.route == selectedDestination,
                onClick = {
                    navController.navigate(destination.route)
                },
                icon = {
                    Icon(
                        destination.icon,
                        contentDescription = destination.contentDescription
                    )
                },
                label = { Text(destination.label) }
            )
        }
    }
}

@Composable
fun App(groups: List<Group>) {
    val navController = rememberNavController()
    val startDestination = Destination.HOME

    Scaffold(
        modifier = Modifier.fillMaxSize().statusBarsPadding(),
        bottomBar = {
            NavBar(navController)
        }
    ) { innerPadding ->
        NavHost(groups, navController, startDestination, modifier = Modifier.padding(innerPadding))
    }
}