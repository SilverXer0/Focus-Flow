package com.zybooks.focusflow.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zybooks.focusflow.data.SettingsRepository
import com.zybooks.focusflow.ui.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val repo = remember { SettingsRepository(context) }
    val vm: SettingsViewModel = viewModel(factory = SettingsViewModel.factory(repo))
    val state by vm.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
            )
        }
    ) { inner ->
        Column(
            modifier = Modifier
                .padding(inner)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Session Lengths", style = MaterialTheme.typography.titleMedium)

            NumberField(
                label = "Focus Length (minutes)",
                value = state.focusMinutes,
                onValue = { vm.setFocusMinutes(it) }
            )
            NumberField(
                label = "Short Break (minutes)",
                value = state.shortBreakMinutes,
                onValue = { vm.setShortBreakMinutes(it) }
            )
            NumberField(
                label = "Long Break (minutes)",
                value = state.longBreakMinutes,
                onValue = { vm.setLongBreakMinutes(it) }
            )
            NumberField(
                label = "Long Break Every N Sessions",
                value = state.longBreakEvery,
                onValue = { vm.setLongBreakEvery(it) }
            )

            Divider()

            Text("Preferences", style = MaterialTheme.typography.titleMedium)

            ToggleCard(
                title = "Auto-start next interval",
                checked = state.autoStartNext,
                onCheckedChange = vm::setAutoStart
            )
            ToggleCard(
                title = "Vibrations",
                checked = state.vibrations,
                onCheckedChange = vm::setVibrations
            )
            ToggleCard(
                title = "Movement nudge",
                checked = state.movementNudge,
                onCheckedChange = vm::setMovementNudge
            )

            Divider()

            Text("Advanced", style = MaterialTheme.typography.titleMedium)
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Use foreground service for timers (not implemented yet).",
                    modifier = Modifier.padding(12.dp)
                )
            }

            Divider()

            Text("Data", style = MaterialTheme.typography.titleMedium)

            OutlinedButton(
                onClick = { vm.resetStats() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Reset Stats")
            }

            OutlinedButton(
                onClick = { vm.exportSummary() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Export Summary to Clipboard")
            }

            state.exportResult?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun NumberField(
    label: String,
    value: Int,
    onValue: (Int) -> Unit
) {
    val displayText = if (value == 0) "" else value.toString()

    OutlinedTextField(
        value = displayText,
        onValueChange = { txt ->
            val digitsOnly = txt.filter { it.isDigit() }
            val n = digitsOnly.toIntOrNull() ?: 0
            if (n >= 0) {
                onValue(n)
            }
        },
        label = { Text(label) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun ToggleCard(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Card(
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(title, fontWeight = FontWeight.Medium)
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        }
    }
}