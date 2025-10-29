package com.zybooks.focusflow.ui.screens

import android.os.SystemClock
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.StrokeCap
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.zybooks.focusflow.Routes
import com.zybooks.focusflow.ui.viewmodel.HomeViewModel
import com.zybooks.focusflow.ui.viewmodel.TimerPhaseType
import kotlin.math.max
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    nav: NavController,
    vm: HomeViewModel = viewModel()
) {
    val state by vm.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("FocusFlow") },
                actions = {
                    TextButton(onClick = { vm.toggleMenu() }) { Text("⋮") }
                    DropdownMenu(expanded = state.menuExpanded, onDismissRequest = { vm.toggleMenu(false) }) {
                        DropdownMenuItem(text = { Text("Settings") }, onClick = { vm.toggleMenu(false); nav.navigate(Routes.SETTINGS) })
                        DropdownMenuItem(text = { Text("Stats") }, onClick = { vm.toggleMenu(false); nav.navigate(Routes.STATS) })
                        DropdownMenuItem(text = { Text("Log") }, onClick = { vm.toggleMenu(false); nav.navigate(Routes.LOG) })
                    }
                }
            )
        }
    ) { inner ->
        Column(
            modifier = Modifier
                .padding(inner)
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Timer ring
            Box(
                modifier = Modifier.size(240.dp),
                contentAlignment = Alignment.Center
            ) {
                if (state.isRunning) {
                    AnimatedTimeIndicator(
                        timeDurationMillis = state.remainingMillis.toInt().coerceAtLeast(1),
                        isRunning = state.isRunning,
                        modifier = Modifier.fillMaxSize(),
                        stroke = 14.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                } else {
                    CircularProgressIndicator(
                        progress = { 1f },
                        strokeWidth = 14.dp,
                        modifier = Modifier.matchParentSize(),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    )
                    CircularProgressIndicator(
                        progress = {
                            (state.remainingMillis.toFloat() /
                                state.totalMillis.coerceAtLeast(1L).toFloat()).coerceIn(0f, 1f)
                        },
                        strokeWidth = 14.dp,
                        modifier = Modifier.matchParentSize(),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = Color.Transparent
                    )
                }
                Text(
                    text = formatMillis(state.remainingMillis),
                    style = MaterialTheme.typography.headlineMedium
                )
            }
            Text(
                text = when (state.phaseType) {
                    TimerPhaseType.Focus -> "Focus"
                    TimerPhaseType.ShortBreak -> "Short Break"
                    TimerPhaseType.LongBreak -> "Long Break"
                },
                style = MaterialTheme.typography.titleMedium
            )

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                val primaryLabel = if (state.isRunning) "Pause" else "Start"
                Button(onClick = { if (state.isRunning) vm.pause() else vm.start() }) { Text(primaryLabel) }
                OutlinedButton(onClick = { vm.stop() }) { Text("Stop") }
            }

            SimpleChipRow {
                AssistChip(onClick = { vm.selectPreset(25, 5) }, label = { Text("25/5") })
                AssistChip(onClick = { vm.selectPreset(50, 10) }, label = { Text("50/10") })
                AssistChip(onClick = { vm.openCustomPicker() }, label = { Text("Custom…") })
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Auto-start next interval")
                Switch(checked = state.autoStartNext, onCheckedChange = { vm.setAutoStart(it) })
            }

            Spacer(modifier = Modifier.weight(1f))

            Text(
                text = "Today: ${state.todaySessionCount} sessions, ${state.todayMinutes} min",
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            state.snackbarMessage?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
        }
    }

    LaunchedEffect(state.navigateToBreakCoach) {
        if (state.navigateToBreakCoach) {
            nav.navigate(Routes.BREAK)
        }
    }
}

@Composable
private fun SimpleChipRow(content: @Composable RowScope.() -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { content() }
}

private fun formatMillis(ms: Long): String {
    val totalSec = max(0L, ms / 1000)
    val m = totalSec / 60
    val s = totalSec % 60
    return "%d:%02d".format(m, s)
}

@Composable
fun AnimatedTimeIndicator(
    timeDurationMillis: Int,
    isRunning: Boolean,
    modifier: Modifier = Modifier,
    stroke: Dp = 20.dp,
    color: Color = MaterialTheme.colorScheme.primary
) {
    var progress by remember { mutableFloatStateOf(1f) }
    val progressAnimation by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(
            durationMillis = timeDurationMillis,
            easing = LinearEasing
        ),
        label = "Progress indicator"
    )

    CircularProgressIndicator(
        progress = { progressAnimation },
        modifier = modifier,
        strokeWidth = stroke,
        color = color,
        strokeCap = StrokeCap.Butt
    )

    LaunchedEffect(isRunning, timeDurationMillis) {
        if (isRunning) {
            progress = 1f
            delay(1)
            progress = 0f
        }
    }
}