package com.example.detector.presentation.navigation

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Login : Screen("login")
    object Register : Screen("register")
    object Home : Screen("home")
    object Capture : Screen("capture")
    
    object AiResult : Screen("ai_result/{imagePath}/{prediction}/{confidence}") {
        fun createRoute(imagePath: String, prediction: String, confidence: Float): String {
            return "ai_result/$imagePath/$prediction/$confidence"
        }
    }
    
    object Map : Screen("map")
    object Tracking : Screen("tracking")
    
    object IssueDetails : Screen("issue_details/{issueId}") {
        fun createRoute(issueId: String): String {
            return "issue_details/$issueId"
        }
    }
    
    object Notifications : Screen("notifications")
    object Profile : Screen("profile")
}
