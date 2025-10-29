package com.zybooks.focusflow

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.zybooks.focusflow.ui.screens.HomeScreen

object Routes {
    const val HOME = "home"
    const val BREAK = "break"
    const val LOG = "log"
    const val STATS = "stats"
    const val SETTINGS = "settings"
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { FocusFlowApp() }
    }
}

@Composable
fun FocusFlowApp() {
    MaterialTheme {
        val nav = rememberNavController()
        NavHost(navController = nav, startDestination = Routes.HOME) {
            composable(Routes.HOME) { HomeScreen(nav) }
            composable(Routes.BREAK) { PlaceholderScreen("Break Coach (coming soon)") }
            composable(Routes.LOG) { PlaceholderScreen("Sessions Log (coming soon)") }
            composable(Routes.STATS) { PlaceholderScreen("Stats (coming soon)") }
            composable(Routes.SETTINGS) { PlaceholderScreen("Settings (coming soon)") }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlaceholderScreen(title: String) {
    Scaffold(
        topBar = { TopAppBar(title = { Text(title) }) }
    ) { inner ->
        Text(
            text = "TODO",
            modifier = Modifier
                .padding(inner)
                .padding(24.dp)
        )
    }
}