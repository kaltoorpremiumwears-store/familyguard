package org.familyguard.child

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import org.familyguard.child.ui.screens.*
import org.familyguard.child.ui.theme.FamilyGuardChildTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FamilyGuardChildTheme {
                ChildAppNavHost()
            }
        }
    }
}

@Composable
fun ChildAppNavHost() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "enrollment") {
        composable("enrollment") {
            EnrollmentScreen(
                onProceedToPairing = { navController.navigate("pairing_claim") }
            )
        }

        composable("pairing_claim") {
            PairingClaimScreen(
                onNavigateBack = { navController.popBackStack() },
                onPairingSuccess = {
                    navController.navigate("child_status") {
                        popUpTo("enrollment") { inclusive = true }
                    }
                }
            )
        }

        composable("child_status") {
            ChildStatusScreen(
                onNavigateToSafeTube = { navController.navigate("safetube") },
                onUnpairOrReset = {
                    navController.navigate("enrollment") {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable("safetube") {
            org.familyguard.child.youtube.SafeTubeScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
