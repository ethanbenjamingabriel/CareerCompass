package dev.hungrymonkey.careercompass.components

import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.compose.runtime.getValue


@Composable
fun NavigationBar(navController: NavHostController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: ""

    NavigationBar {
        NavigationBarItem(
            selected = currentRoute == "home",
            onClick = { 
                if (currentRoute != "home") {
                    navController.navigate("home") {
                        popUpTo("home") { inclusive = true }
                        launchSingleTop = true
                    }
                }
            },
            icon = { Icon(
                    if (currentRoute == "home") Icons.Filled.Home else Icons.Outlined.Home,
                    contentDescription = "Home"
                ) 
            },
            label = { Text("Home") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = MaterialTheme.colorScheme.primary,
                selectedTextColor = MaterialTheme.colorScheme.primary,
                indicatorColor = MaterialTheme.colorScheme.primaryContainer
            )
        )

        NavigationBarItem(
            selected = currentRoute == "dashboard",
            onClick = {
                        if (currentRoute != "dashboard") {
                        navController.navigate("dashboard") {
                        launchSingleTop = true
                    }
                }
            },
            icon = { 
                Icon(
                    if (currentRoute == "dashboard") Icons.Filled.Analytics else Icons.Outlined.Analytics,
                    contentDescription = "Dashboard"
                    )
            },
            label = { Text("Dashboard") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = MaterialTheme.colorScheme.primary,
                selectedTextColor = MaterialTheme.colorScheme.primary,
                indicatorColor = MaterialTheme.colorScheme.primaryContainer
            )
        )
        NavigationBarItem(
            selected = currentRoute == "account",
            onClick = {
                if (currentRoute != "account") {
                        navController.navigate("account") {
                        launchSingleTop = true
                    }
                }
            },
            icon = {
                Icon(
                    if(currentRoute == "account") Icons.Filled.Person else Icons.Outlined.Person,
                    contentDescription = "account"
                )
            },
            label = { Text("Account") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = MaterialTheme.colorScheme.primary,
                selectedTextColor = MaterialTheme.colorScheme.primary,
                indicatorColor = MaterialTheme.colorScheme.primaryContainer
            )
        )
    }
}