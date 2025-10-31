package com.zybooks.focusflow.ui.screens

import android.os.SystemClock
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.zybooks.focusflow.Routes
import com.zybooks.focusflow.data.SettingsRepository
import com.zybooks.focusflow.ui.viewmodel.HomeViewModel
import com.zybooks.focusflow.ui.viewmodel.TimerPhaseType
import kotlin.math.max
import kotlinx.coroutines.delay
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    nav: NavController,
    vm: HomeViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    val context = LocalContext.current
    val repo = remember { SettingsRepository(context) }
    val vm: HomeViewModel = viewModel(factory = HomeViewModel.factory(repo))

    val state by vm.uiState.collectAsStateWithLifecycle()
    var menuOpen by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("FocusFlow") },
                actions = {
                    IconButton(onClick = { menuOpen = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "More")
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(
                            text = { Text("Break Coach") },
                            onClick = {
                                menuOpen = false
                                nav.navigate(Routes.BREAK) {
                                    popUpTo(Routes.HOME) { saveState = true }
                                    launchSingleTop = true
                                }
                            }
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { inner ->
        Column(
            modifier = Modifier
                .padding(inner)
                .fillMaxSize()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(8.dp))

            Box(
                modifier = Modifier.size(260.dp),
                contentAlignment = Alignment.Center
            ) {
                if (state.isRunning) {
                    AnimatedTimeIndicator(
                        timeDurationMillis = state.remainingMillis.toInt().coerceAtLeast(1),
                        isRunning = state.isRunning,
                        modifier = Modifier.fillMaxSize(),
                        stroke = 18.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                } else {
                    CircularProgressIndicator(
                        progress = { 1f },
                        strokeWidth = 18.dp,
                        modifier = Modifier.matchParentSize(),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    )
                    CircularProgressIndicator(
                        progress = {
                            (state.remainingMillis.toFloat() /
                                    state.totalMillis.coerceAtLeast(1L).toFloat()).coerceIn(0f, 1f)
                        },
                        strokeWidth = 18.dp,
                        modifier = Modifier.matchParentSize(),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = Color.Transparent,
                        strokeCap = StrokeCap.Butt
                    )
                }
                Text(text = formatMillis(state.remainingMillis), style = MaterialTheme.typography.displaySmall)
            }

            Spacer(Modifier.height(16.dp))

            Text(
                text = "Focus",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(20.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(20.dp), verticalAlignment = Alignment.CenterVertically) {
                val primaryLabel = if (state.isRunning) "Pause" else "Start"
                Button(
                    onClick = { if (state.isRunning) vm.pause() else vm.start() },
                    shape = RoundedCornerShape(28.dp),
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
                ) { Text(primaryLabel) }

                OutlinedButton(
                    onClick = { vm.stop() },
                    shape = RoundedCornerShape(28.dp),
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
                ) { Text("Stop") }
            }

            Spacer(Modifier.height(18.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                AssistChip(onClick = { vm.selectPreset(25, 5) }, label = { Text("25/5") })
                AssistChip(onClick = { vm.selectPreset(50, 10) }, label = { Text("50/10") })
                AssistChip(onClick = { vm.openCustomPicker() }, label = { Text("Custom…") })
            }

            Spacer(Modifier.height(22.dp))

            ToggleCard(
                title = "Auto-start next interval",
                checked = state.autoStartNext,
                onCheckedChange = vm::setAutoStart
            )

            Spacer(Modifier.weight(1f))
            Divider()
            Text(
                text = "Today: ${state.todaySessionCount} sessions, ${state.todayMinutes} min",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
            )
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

@Composable
private fun ToggleCard(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        }
    }
}

fun NavController.navigateHome() {
    val popped = popBackStack(route = Routes.HOME, inclusive = false)
    if (!popped) {
        navigate(Routes.HOME) {
            popUpTo(graph.startDestinationId) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }
}