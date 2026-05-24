package com.securevault.presentation.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.fragment.app.FragmentActivity
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.securevault.security.RootDetectionService
import com.securevault.security.SessionManager

sealed class Screen(val route: String) {
    object Auth   : Screen("auth")
    object Home   : Screen("home")
    object Detail : Screen("detail/{recordId}") {
        fun createRoute(id: String) = "detail/$id"
    }
}

@Composable
fun SecureVaultNavHost(
    activity: FragmentActivity,
    sessionManager: SessionManager,
    rootDetectionService: RootDetectionService
) {
    val navController: NavHostController = rememberNavController()
    val startDestination = if (sessionManager.isLoggedIn()) Screen.Home.route
                           else Screen.Auth.route

    // Слушаем события logout → перенаправляем на экран аутентификации
    LaunchedEffect(Unit) {
        sessionManager.logoutEvent.collect { reason ->
            navController.navigate(Screen.Auth.route) {
                popUpTo(0) { inclusive = true }
            }
        }
    }

    NavHost(
        navController   = navController,
        startDestination = startDestination
    ) {
        composable(Screen.Auth.route) {
            AuthScreen(
                activity        = activity,
                sessionManager  = sessionManager,
                onAuthSuccess   = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Auth.route) { inclusive = true }
                    }
                }
            )
        }
        composable(Screen.Home.route) {
            HomeScreen(
                rootDetectionService = rootDetectionService,
                onLogout = {
                    sessionManager.logout()
                },
                onRecordClick = { id ->
                    navController.navigate(Screen.Detail.createRoute(id))
                }
            )
        }
        composable(Screen.Detail.route) { backStack ->
            val recordId = backStack.arguments?.getString("recordId") ?: return@composable
            DetailScreen(
                recordId    = recordId,
                onBack      = { navController.popBackStack() }
            )
        }
    }
}
