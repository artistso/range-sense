package com.soquarky.rangesense

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.soquarky.rangesense.core.GameAction
import com.soquarky.rangesense.core.GameEngine
import com.soquarky.rangesense.core.GameState
import com.soquarky.rangesense.core.RoundPhase
import com.soquarky.rangesense.core.SensorType
import java.util.Locale

internal val TacticalGreen = Color(0xFF7CFF9A)
internal val TacticalDark = Color(0xFF020805)
internal val TacticalPanel = Color(0xFF07140D)
internal val TacticalAmber = Color(0xFFFFC857)
internal val TacticalRed = Color(0xFFFF6B6B)

@Composable
fun RangeSenseApp() {
    val colors = darkColorScheme(
        primary = TacticalGreen,
        secondary = TacticalAmber,
        background = TacticalDark,
        surface = TacticalPanel,
        onPrimary = TacticalDark,
        onSecondary = TacticalDark,
        onBackground = TacticalGreen,
        onSurface = TacticalGreen,
        error = TacticalRed,
    )

    MaterialTheme(colorScheme = colors) {
        Surface(Modifier.fillMaxSize(), color = TacticalDark) {
            var seed by remember { mutableLongStateOf(8183L) }
            var state by remember { mutableStateOf(GameEngine.initial(seed)) }
            val dispatch: (GameAction) -> Unit = { state = GameEngine.reduce(state, it) }

            when (state.phase) {
                RoundPhase.BRIEFING -> Briefing(state) { dispatch(GameAction.Start) }
                RoundPhase.PLAYING -> MissionConsole(state, dispatch)
                RoundPhase.DEBRIEF -> Debrief(state) {
                    seed += 1
                    state = GameEngine.initial(seed)
                }
            }
        }
    }
}

@Composable
private fun Briefing(state: GameState, onStart: () -> Unit) {
    Row(
        Modifier.fillMaxSize().padding(24.dp),
        horizontalArrangement = Arrangement.spacedBy(24.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TacticalViewport(state, enabled = false, modifier = Modifier.weight(1.6f).fillMaxHeight())
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("RANGE SENSE", fontSize = 34.sp, fontWeight = FontWeight.Bold)
            Text(state.mission.title, fontSize = 24.sp, color = TacticalAmber)
            Text("Native single-player passive-ranging game")
            HorizontalDivider()
            Text("Select sensor cues, stay within the compute budget, estimate range, and declare uncertainty. Abstain only when no cue survives the information gate.")
            Text("Budget ${state.mission.computeBudget} · Seed ${state.mission.seed}")
            Button(onClick = onStart, modifier = Modifier.fillMaxWidth()) { Text("START MISSION") }
            Text(
                "Scientific boundary: these are deterministic game models, not validated field-sensor performance.",
                color = TacticalAmber,
                fontSize = 13.sp,
            )
        }
    }
}

@Composable
private fun MissionConsole(state: GameState, dispatch: (GameAction) -> Unit) {
    Row(
        Modifier.fillMaxSize().padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SensorRail(state, dispatch, Modifier.width(280.dp).fillMaxHeight())
        Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Header(state)
            TacticalViewport(state, enabled = true, modifier = Modifier.weight(1f).fillMaxWidth())
            FusionPanel(state) { dispatch(GameAction.UseFusedEstimate) }
        }
        SolutionPanel(state, dispatch, Modifier.width(310.dp).fillMaxHeight())
    }
}

@Composable
private fun Header(state: GameState) {
    Row(
        Modifier.fillMaxWidth().background(TacticalPanel).padding(10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(state.mission.title, fontWeight = FontWeight.Bold)
        Text("COMPUTE ${state.computeUsed}/${state.mission.computeBudget}", fontFamily = FontFamily.Monospace)
    }
}

@Composable
private fun SensorRail(state: GameState, dispatch: (GameAction) -> Unit, modifier: Modifier) {
    Column(
        modifier.background(TacticalPanel, RoundedCornerShape(10.dp)).padding(10.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text("SENSOR CUES", fontWeight = FontWeight.Bold)
        SensorType.entries.forEach { type ->
            val selected = type in state.selectedSensors
            val reading = state.readings[type]
            val gate = state.gates[type]
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF0A1E12))) {
                Column(Modifier.fillMaxWidth().padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Text(type.tag, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                            Text(type.displayName, fontSize = 13.sp)
                        }
                        Switch(
                            checked = selected,
                            enabled = selected || state.computeRemaining >= type.computeCost,
                            onCheckedChange = { dispatch(GameAction.ToggleSensor(type)) },
                        )
                    }
                    Text("Cost ${type.computeCost}", color = TacticalAmber, fontSize = 12.sp)
                    if (reading != null && gate != null) {
                        Text("Estimate ${meters(reading.estimateMeters)} · σ ${meters(reading.sigmaMeters)}", fontSize = 11.sp)
                        Text(
                            if (gate.open) "GATE OPEN ${(gate.retainedFraction * 100).toInt()}%" else "GATE CLOSED",
                            color = if (gate.open) TacticalGreen else TacticalRed,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                        )
                        Text(gate.reason, fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun FusionPanel(state: GameState, onLoad: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().background(TacticalPanel, RoundedCornerShape(8.dp)).padding(10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text("FUSION", fontWeight = FontWeight.Bold)
            val fusion = state.fusion
            Text(
                fusion.estimateMeters?.let { "${meters(it)} ± ${meters(fusion.sigmaMeters ?: 0.0)}" } ?: fusion.reason,
                color = if (fusion.trustworthy) TacticalGreen else TacticalAmber,
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
            )
        }
        OutlinedButton(onClick = onLoad, enabled = state.fusion.estimateMeters != null) { Text("LOAD") }
    }
}

@Composable
private fun SolutionPanel(state: GameState, dispatch: (GameAction) -> Unit, modifier: Modifier) {
    Column(
        modifier.background(TacticalPanel, RoundedCornerShape(10.dp)).padding(12.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text("RANGE SOLUTION", fontWeight = FontWeight.Bold)
        Text(meters(state.playerEstimateMeters), fontSize = 28.sp, fontFamily = FontFamily.Monospace)
        Slider(
            value = state.playerEstimateMeters.toFloat(),
            onValueChange = { dispatch(GameAction.SetEstimate(it.toDouble())) },
            valueRange = 500f..12_000f,
        )
        Text("95% interval ± ${meters(state.intervalHalfWidthMeters)}")
        Slider(
            value = state.intervalHalfWidthMeters.toFloat(),
            onValueChange = { dispatch(GameAction.SetIntervalHalfWidth(it.toDouble())) },
            valueRange = 0.1f..2_000f,
        )
        HorizontalDivider()
        val env = state.mission.environment
        Text("CONDITIONS", fontWeight = FontWeight.Bold)
        Text("${env.weather.name.replace('_', ' ')} · ${env.illumination.name.replace('_', ' ')}")
        Text("Visibility ${one(env.visibilityKm)} km · Registration ${one(env.registrationErrorPixels)} px")
        Button(onClick = { dispatch(GameAction.Submit) }, modifier = Modifier.fillMaxWidth()) { Text("SUBMIT RANGE") }
        OutlinedButton(onClick = { dispatch(GameAction.Abstain) }, modifier = Modifier.fillMaxWidth()) { Text("ABSTAIN") }
        Text("Thresholds are strict: exactly 0.5 m is not in the <0.5 m tier.", color = TacticalAmber, fontSize = 11.sp)
    }
}

@Composable
private fun Debrief(state: GameState, onNext: () -> Unit) {
    val result = requireNotNull(state.result)
    Row(Modifier.fillMaxSize().padding(24.dp), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
        TacticalViewport(state, enabled = false, modifier = Modifier.weight(1.4f).fillMaxHeight())
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("MISSION DEBRIEF", fontSize = 30.sp, fontWeight = FontWeight.Bold)
            Text("TRUE RANGE ${meters(result.trueRangeMeters)}", color = TacticalAmber, fontSize = 22.sp)
            if (result.abstained) {
                Text("ABSTAINED", fontWeight = FontWeight.Bold)
            } else {
                Text("Submitted ${meters(result.submittedEstimateMeters ?: 0.0)}")
                Text("Absolute error ${meters(result.absoluteErrorMeters ?: 0.0)}")
                Text(
                    if (result.intervalCoveredTruth == true) "INTERVAL COVERED TRUTH" else "FALSE CONFIDENCE: INTERVAL MISSED",
                    color = if (result.intervalCoveredTruth == true) TacticalGreen else TacticalRed,
                    fontWeight = FontWeight.Bold,
                )
            }
            HorizontalDivider()
            Text("TIER POINTS ${result.officialTierPoints}")
            Text("GAME SCORE ${result.gameScore}", fontSize = 26.sp, fontWeight = FontWeight.Bold)
            Text(result.explanation)
            Button(onClick = onNext, modifier = Modifier.fillMaxWidth()) { Text("NEXT MISSION") }
        }
    }
}

internal fun meters(value: Double): String = if (value >= 1_000.0) {
    String.format(Locale.US, "%.3f km", value / 1_000.0)
} else {
    String.format(Locale.US, "%.2f m", value)
}

internal fun one(value: Double): String = String.format(Locale.US, "%.1f", value)
