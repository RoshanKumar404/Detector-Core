package com.example.detector.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.detector.data.ServiceLocator
import com.example.detector.presentation.ViewModelFactory
import com.example.detector.presentation.screens.auth.LoginScreen
import com.example.detector.presentation.screens.auth.RegisterScreen
import com.example.detector.presentation.screens.capture.AiResultScreen
import com.example.detector.presentation.screens.capture.CaptureScreen
import com.example.detector.presentation.screens.home.HomeScreen
import com.example.detector.presentation.screens.issues.AllIssuesScreen
import com.example.detector.presentation.screens.map.MapScreen
import com.example.detector.presentation.screens.notifications.NotificationsScreen
import com.example.detector.presentation.screens.profile.ProfileScreen
import com.example.detector.presentation.screens.splash.SplashScreen
import com.example.detector.presentation.screens.tracking.IssueDetailsScreen
import com.example.detector.presentation.screens.tracking.TrackingScreen

@Composable
fun NavGraph(navController: NavHostController) {
    val factory = ViewModelFactory()

    NavHost(
        navController = navController,
        startDestination = Screen.Splash.route
    ) {
        composable(Screen.Splash.route) {
            SplashScreen(navController = navController)
        }

        composable(Screen.Login.route) {
            LoginScreen(
                navController = navController,
                viewModel = viewModel(factory = factory)
            )
        }

        composable(Screen.Register.route) {
            RegisterScreen(
                navController = navController,
                viewModel = viewModel(factory = factory)
            )
        }

        composable(Screen.Home.route) {
            RequireAuth(navController) {
                HomeScreen(
                    navController = navController,
                    viewModel = viewModel(factory = factory)
                )
            }
        }

        composable(Screen.Capture.route) {
            RequireAuth(navController) {
                CaptureScreen(navController = navController)
            }
        }

        composable(
            route = Screen.AiResult.route,
            arguments = listOf(
                navArgument("imagePath") { type = NavType.StringType },
                navArgument("prediction") { type = NavType.StringType },
                navArgument("confidence") { type = NavType.FloatType }
            )
        ) { backStackEntry ->
            val imagePath = backStackEntry.arguments?.getString("imagePath") ?: ""
            RequireAuth(navController) {
                AiResultScreen(
                    navController = navController,
                    viewModel = viewModel(factory = factory),
                    imagePath = imagePath
                )
            }
        }

        composable(Screen.Map.route) {
            RequireAuth(navController) {
                MapScreen(
                    navController = navController,
                    viewModel = viewModel(factory = factory)
                )
            }
        }

        composable(Screen.AllIssues.route) {
            RequireAuth(navController) {
                AllIssuesScreen(
                    navController = navController,
                    viewModel = viewModel(factory = factory)
                )
            }
        }

        composable(Screen.Tracking.route) {
            RequireAuth(navController) {
                TrackingScreen(
                    navController = navController,
                    viewModel = viewModel(factory = factory)
                )
            }
        }

        composable(
            route = Screen.IssueDetails.route,
            arguments = listOf(
                navArgument("issueId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val issueId = backStackEntry.arguments?.getString("issueId") ?: ""
            RequireAuth(navController) {
                IssueDetailsScreen(
                    navController = navController,
                    viewModel = viewModel(factory = factory),
                    issueId = issueId
                )
            }
        }

        composable(Screen.Notifications.route) {
            RequireAuth(navController) {
                NotificationsScreen(navController = navController)
            }
        }

        composable(Screen.Profile.route) {
            RequireAuth(navController) {
                ProfileScreen(navController = navController)
            }
        }
    }
}

@Composable
private fun RequireAuth(
    navController: NavHostController,
    content: @Composable () -> Unit
) {
    val sessionManager = remember { ServiceLocator.sessionManager }
    val hasToken = !sessionManager.getAuthToken().isNullOrBlank()

    LaunchedEffect(hasToken) {
        if (!hasToken) {
            navController.navigate(Screen.Login.route) {
                popUpTo(Screen.Splash.route) { inclusive = true }
                launchSingleTop = true
            }
        }
    }

    if (hasToken) {
        content()
    }
}
