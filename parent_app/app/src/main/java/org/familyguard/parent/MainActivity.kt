package org.familyguard.parent

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import org.familyguard.parent.ui.screens.*
import org.familyguard.parent.ui.theme.FamilyGuardParentTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FamilyGuardParentTheme {
                ParentAppNavHost()
            }
        }
    }
}

@Composable
fun ParentAppNavHost() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "login") {
        composable("login") {
            LoginScreen(
                onNavigateToRegister = { navController.navigate("register") },
                onLoginSuccess = {
                    navController.navigate("dashboard") {
                        popUpTo("login") { inclusive = true }
                    }
                }
            )
        }

        composable("register") {
            RegisterScreen(
                onNavigateBack = { navController.popBackStack() },
                onRegisterSuccess = {
                    navController.navigate("dashboard") {
                        popUpTo("login") { inclusive = true }
                    }
                }
            )
        }

        composable("dashboard") {
            DashboardScreen(
                onNavigateToCreateChild = { navController.navigate("create_child") },
                onNavigateToPairing = { childId -> navController.navigate("pairing/$childId") },
                onNavigateToProvisioningGuide = { navController.navigate("provisioning_guide") },
                onNavigateToAppManagement = { childId -> navController.navigate("apps/$childId") },
                onNavigateToGamingControl = { childId -> navController.navigate("gaming/$childId") },
                onNavigateToReports = { childId -> navController.navigate("reports/$childId") },
                onNavigateToWebFiltering = { childId -> navController.navigate("web_filter/$childId") },
                onNavigateToYouTubeStudio = { childId -> navController.navigate("youtube_studio/$childId") },
                onNavigateToLocation = { childId -> navController.navigate("location_radar/$childId") },
                onNavigateToNotifications = { navController.navigate("notifications") },
                onLogout = {
                    navController.navigate("login") {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable("web_filter/{childId}") { backStackEntry ->
            val childId = backStackEntry.arguments?.getString("childId") ?: ""
            WebFilteringScreen(
                childId = childId,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable("youtube_studio/{childId}") { backStackEntry ->
            val childId = backStackEntry.arguments?.getString("childId") ?: ""
            YouTubeStudioScreen(
                childId = childId,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable("location_radar/{childId}") { backStackEntry ->
            val childId = backStackEntry.arguments?.getString("childId") ?: ""
            LocationRadarScreen(
                childId = childId,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable("notifications") {
            NotificationsCenterScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable("apps/{childId}") { backStackEntry ->
            val childId = backStackEntry.arguments?.getString("childId") ?: ""
            AppManagementScreen(
                childId = childId,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable("gaming/{childId}") { backStackEntry ->
            val childId = backStackEntry.arguments?.getString("childId") ?: ""
            GamingControlScreen(
                childId = childId,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable("reports/{childId}") { backStackEntry ->
            val childId = backStackEntry.arguments?.getString("childId") ?: ""
            ActivityReportsScreen(
                childId = childId,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable("provisioning_guide") {
            ProvisioningGuideScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable("create_child") {
            CreateChildScreen(
                onNavigateBack = { navController.popBackStack() },
                onChildCreated = { navController.popBackStack() }
            )
        }

        composable("pairing/{childId}") { backStackEntry ->
            val childId = backStackEntry.arguments?.getString("childId") ?: ""
            PairingScreen(
                childId = childId,
                onNavigateBack = { navController.popBackStack() },
                onPairingCompleted = {
                    navController.navigate("dashboard") {
                        popUpTo("dashboard") { inclusive = true }
                    }
                }
            )
        }
    }
}
