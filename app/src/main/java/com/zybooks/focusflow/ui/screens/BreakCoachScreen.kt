package com.zybooks.focusflow.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.zybooks.focusflow.Routes
import com.zybooks.focusflow.ui.viewmodel.BreakCoachViewModel
import com.zybooks.focusflow.ui.viewmodel.BreathingPattern
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BreakCoachScreen(nav: NavController) {
    val vm: BreakCoachViewModel = viewModel()
    val state by vm.uiState.collectAsState()

    var walkRemainingSec by remember { mutableStateOf(0) }

    LaunchedEffect(walkRemainingSec) {
        if (walkRemainingSec > 0) {
            while (walkRemainingSec > 0) {
                delay(1_000)
                walkRemainingSec -= 1
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Break Coach") },
                navigationIcon = {
                    IconButton(onClick = { nav.navigate(Routes.HOME) }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { inner ->
        Column(
            modifier = Modifier
                .padding(inner)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Breathing Exercise", style = MaterialTheme.typography.titleMedium)

                    BreathingCircle(
                        isRunning = state.isBreathingRunning,
                        label = state.currentStepLabel,
                        onPhaseChange = { newLabel -> vm.setBreathLabel(newLabel) },
                        pattern = state.selectedPattern
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        BreathingSegment(
                            label = "4-4-4",
                            selected = state.selectedPattern == BreathingPattern.FourFourFour,
                            onClick = { vm.selectPattern(BreathingPattern.FourFourFour) }
                        )
                        BreathingSegment(
                            label = "4-7-8",
                            selected = state.selectedPattern == BreathingPattern.FourSevenEight,
                            onClick = { vm.selectPattern(BreathingPattern.FourSevenEight) }
                        )
                        BreathingSegment(
                            label = "Custom",
                            selected = state.selectedPattern == BreathingPattern.Custom,
                            onClick = { vm.selectPattern(BreathingPattern.Custom) }
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(onClick = { vm.startBreathing() }) { Text("Start") }
                        OutlinedButton(onClick = { vm.stopBreathing() }) { Text("Stop") }
                    }
                }
            }

            Card(
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Movement Nudge", style = MaterialTheme.typography.titleMedium)
                    Text("Try a short walk")

                    OutlinedButton(
                        onClick = {
                            if (walkRemainingSec == 0) {
                                walkRemainingSec = 2 * 60
                            } else {
                                walkRemainingSec = 0
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(999.dp)
                    ) {
                        if (walkRemainingSec > 0) {
                            val m = walkRemainingSec / 60
                            val s = walkRemainingSec % 60
                            Text("Walking… %d:%02d".format(m, s))
                        } else {
                            Text("Start 2 min walk")
                        }
                    }
                }
            }

            Spacer(Modifier.weight(1f))

            Button(
                onClick = { nav.navigate(Routes.HOME) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White
                )
            ) {
                Text("End Break")
            }
        }
    }
}

@Composable
private fun BreathingCircle(
    isRunning: Boolean,
    label: String,
    onPhaseChange: (String) -> Unit,
    pattern: BreathingPattern
) {
    val (inhaleMs, holdMs, exhaleMs) = when (pattern) {
        BreathingPattern.FourFourFour -> Triple(4000, 0, 4000)
        BreathingPattern.FourSevenEight -> Triple(4000, 3000, 8000)
        BreathingPattern.Custom -> Triple(4000, 0, 4000)
    }

    var phase by remember { mutableStateOf("idle") }

    val bigScale = 1f
    val smallScale = 0.7f
    val prepDuration = 300

    val targetScale = when (phase) {
        "idle" -> bigScale
        "pre" -> smallScale
        "inhale" -> bigScale
        "exhale" -> smallScale
        else -> bigScale
    }

    val animatedScale by animateFloatAsState(
        targetValue = targetScale,
        animationSpec = tween(
            durationMillis = when (phase) {
                "pre" -> prepDuration
                "inhale" -> inhaleMs
                "exhale" -> exhaleMs
                else -> 250
            },
            easing = LinearEasing
        ),
        label = "breath-scale"
    )

    LaunchedEffect(isRunning, pattern) {
        if (isRunning) {
            phase = "pre"
            onPhaseChange("Inhale")
            delay(prepDuration.toLong())

            while (true) {
                phase = "inhale"
                onPhaseChange("Inhale")
                delay(inhaleMs.toLong())

                if (holdMs > 0) {
                    onPhaseChange("Hold")
                    delay(holdMs.toLong())
                }

                phase = "exhale"
                onPhaseChange("Exhale")
                delay(exhaleMs.toLong())
            }
        } else {
            phase = "idle"
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(150.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(999.dp),
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f),
            modifier = Modifier.size((110 * animatedScale).dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(label)
            }
        }
    }
}

@Composable
private fun BreathingSegment(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    if (selected) {
        Button(
            onClick = onClick,
            shape = RoundedCornerShape(999.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
        ) {
            Text(label)
        }
    } else {
        OutlinedButton(
            onClick = onClick,
            shape = RoundedCornerShape(999.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
        ) {
            Text(label)
        }
    }
}