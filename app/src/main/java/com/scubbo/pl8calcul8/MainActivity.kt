package com.scubbo.pl8calcul8

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.scubbo.pl8calcul8.ui.HomeScreen
import com.scubbo.pl8calcul8.ui.history.HistoryScreen
import com.scubbo.pl8calcul8.ui.session.SessionScreen
import com.scubbo.pl8calcul8.ui.settings.SettingsScreen
import com.scubbo.pl8calcul8.ui.strength.StrengthScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    Box(modifier = Modifier.safeDrawingPadding()) {
                        AppNavigation()
                    }
                }
            }
        }
    }
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "home") {
        composable("home") {
            HomeScreen(
                onStartWorkout = { navController.navigate("session") },
                onHistory = { navController.navigate("history") },
                onStrength = { navController.navigate("strength") },
                onSettings = { navController.navigate("settings") },
            )
        }
        composable("session") {
            SessionScreen(onFinished = { navController.popBackStack() })
        }
        composable("history") { HistoryScreen() }
        composable("strength") { StrengthScreen() }
        composable("settings") { SettingsScreen() }
    }
}
