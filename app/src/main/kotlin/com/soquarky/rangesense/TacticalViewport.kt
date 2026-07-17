package com.soquarky.rangesense

import android.view.MotionEvent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.soquarky.rangesense.core.GameState
import kotlin.math.max

private data class StylusTelemetry(
    val hover: Offset? = null,
    val stroke: List<Offset> = emptyList(),
    val pressure: Float = 0f,
    val eraser: Boolean = false,
    val down: Boolean = false,
)

@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun TacticalViewport(state: GameState, enabled: Boolean, modifier: Modifier = Modifier) {
    var telemetry by remember(state.mission.seed) { mutableStateOf(StylusTelemetry()) }

    Box(
        modifier
            .background(Color(0xFF03110A), RoundedCornerShape(10.dp))
            .border(1.dp, Color(0xFF245D37), RoundedCornerShape(10.dp))
            .pointerInteropFilter { event ->
                if (!enabled) return@pointerInteropFilter false
                val tool = event.getToolType(0)
                val stylus = tool == MotionEvent.TOOL_TYPE_STYLUS || tool == MotionEvent.TOOL_TYPE_ERASER
                if (!stylus && telemetry.down) return@pointerInteropFilter true
                val point = Offset(event.x, event.y)

                when (event.actionMasked) {
                    MotionEvent.ACTION_HOVER_ENTER, MotionEvent.ACTION_HOVER_MOVE -> {
                        if (stylus) telemetry = telemetry.copy(hover = point, eraser = tool == MotionEvent.TOOL_TYPE_ERASER)
                    }
                    MotionEvent.ACTION_HOVER_EXIT -> telemetry = telemetry.copy(hover = null)
                    MotionEvent.ACTION_DOWN -> if (stylus) {
                        telemetry = StylusTelemetry(
                            hover = point,
                            stroke = if (tool == MotionEvent.TOOL_TYPE_ERASER) emptyList() else listOf(point),
                            pressure = event.pressure,
                            eraser = tool == MotionEvent.TOOL_TYPE_ERASER,
                            down = true,
                        )
                    }
                    MotionEvent.ACTION_MOVE -> if (stylus) {
                        telemetry = telemetry.copy(
                            hover = point,
                            stroke = if (tool == MotionEvent.TOOL_TYPE_ERASER) emptyList() else telemetry.stroke + point,
                            pressure = event.pressure,
                            eraser = tool == MotionEvent.TOOL_TYPE_ERASER,
                            down = true,
                        )
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> if (stylus) {
                        telemetry = telemetry.copy(hover = point, pressure = event.pressure, down = false)
                    }
                }
                stylus
            },
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val horizon = size.height * 0.58f
            drawRect(Color(0xFF04130B))
            drawRect(
                Color(0xFF0B2416),
                topLeft = Offset(0f, horizon),
                size = androidx.compose.ui.geometry.Size(size.width, size.height - horizon),
            )

            val haze = (1.0 - state.mission.environment.visibilityKm / 25.0).coerceIn(0.05, 0.75).toFloat()
            repeat(7) { index ->
                val y = horizon - index * size.height * 0.055f
                drawLine(TacticalGreen.copy(alpha = haze / (index + 2)), Offset(0f, y), Offset(size.width, y), 2f)
            }

            val towerHeight = (size.height * 0.35f * (2_500.0 / state.mission.target.rangeMeters).coerceIn(0.28, 1.0)).toFloat()
            val towerWidth = max(18f, towerHeight * 0.22f)
            drawRect(
                Color(0xFF405449),
                topLeft = Offset(size.width * 0.53f - towerWidth / 2f, horizon - towerHeight),
                size = androidx.compose.ui.geometry.Size(towerWidth, towerHeight),
            )

            drawLine(TacticalGreen.copy(alpha = 0.5f), Offset(size.width / 2f, 0f), Offset(size.width / 2f, size.height), 1f)
            drawLine(TacticalGreen.copy(alpha = 0.5f), Offset(0f, size.height / 2f), Offset(size.width, size.height / 2f), 1f)

            if (telemetry.stroke.size >= 2) {
                val path = Path().apply {
                    moveTo(telemetry.stroke.first().x, telemetry.stroke.first().y)
                    telemetry.stroke.drop(1).forEach { lineTo(it.x, it.y) }
                }
                drawPath(path, TacticalAmber, style = Stroke(2f + 8f * telemetry.pressure.coerceIn(0f, 1f)))
            }

            telemetry.hover?.let { point ->
                drawLine(TacticalGreen, Offset(point.x - 14f, point.y), Offset(point.x + 14f, point.y), 1.5f)
                drawLine(TacticalGreen, Offset(point.x, point.y - 14f), Offset(point.x, point.y + 14f), 1.5f)
            }
        }

        Column(Modifier.align(Alignment.TopStart).padding(10.dp)) {
            Text("PASSIVE VIEW / ROI", fontFamily = FontFamily.Monospace, fontSize = 12.sp)
            Text(
                if (telemetry.eraser) "S PEN ERASER" else "PRESSURE ${one(telemetry.pressure.toDouble())}",
                color = TacticalAmber,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
            )
        }
    }
}
