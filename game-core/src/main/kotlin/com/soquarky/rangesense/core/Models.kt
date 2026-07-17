package com.soquarky.rangesense.core

import kotlin.math.PI

enum class SensorType(
    val displayName: String,
    val tag: String,
    val computeCost: Int,
) {
    GEOMETRIC("Geometric", "GEO", 12),
    SPECTRAL("Spectral", "SPEC", 8),
    PSF("PSF / Focus", "PSF", 6),
    POLARIMETRIC("Polarimetric", "POL", 5),
    TEMPORAL("Temporal", "TEMP", 10),
}

enum class TargetType {
    PANEL,
    SPHERE,
    VEHICLE,
    CORNER,
}

enum class Illumination {
    DIRECT_SUN,
    OVERCAST,
    TWILIGHT,
    NIGHT,
}

enum class Weather {
    CLEAR,
    HAZE,
    FOG,
    LIGHT_RAIN,
}

data class Target(
    val rangeMeters: Double,
    val type: TargetType,
    val reflectance: Double,
    val angularSizeDegrees: Double,
)

data class Environment(
    val visibilityKm: Double,
    val humidity: Double,
    val contrastRatio: Double,
    val turbulenceCn2: Double,
    val illumination: Illumination,
    val weather: Weather,
    val registrationErrorPixels: Double,
    val backgroundClutter: Double,
) {
    init {
        require(visibilityKm > 0.0)
        require(humidity in 0.0..1.0)
        require(contrastRatio > 0.0)
        require(turbulenceCn2 >= 0.0)
        require(registrationErrorPixels >= 0.0)
        require(backgroundClutter in 0.0..1.0)
    }
}

data class Mission(
    val id: String,
    val title: String,
    val seed: Long,
    val target: Target,
    val environment: Environment,
    val computeBudget: Int,
    val timeLimitSeconds: Int,
)

data class SensorReading(
    val sensorType: SensorType,
    val estimateMeters: Double,
    val sigmaMeters: Double,
    val observedInformationPerMeterSquared: Double,
    val nuisanceCoupling: Double,
    val conditionalInformationPerMeterSquared: Double,
    val hardFailure: Boolean,
    val failureReason: String?,
) {
    init {
        require(estimateMeters.isFinite() && estimateMeters > 0.0)
        require(sigmaMeters.isFinite() && sigmaMeters > 0.0)
        require(observedInformationPerMeterSquared >= 0.0)
        require(nuisanceCoupling in 0.0..1.0)
        require(conditionalInformationPerMeterSquared >= 0.0)
    }

    val retainedFraction: Double
        get() = if (observedInformationPerMeterSquared == 0.0) {
            0.0
        } else {
            conditionalInformationPerMeterSquared / observedInformationPerMeterSquared
        }
}

data class GateDecision(
    val sensorType: SensorType,
    val open: Boolean,
    val retainedFraction: Double,
    val reason: String,
)

data class FusionContributor(
    val sensorType: SensorType,
    val normalizedWeight: Double,
    val estimateMeters: Double,
    val sigmaMeters: Double,
)

data class FusionResult(
    val estimateMeters: Double?,
    val sigmaMeters: Double?,
    val contributors: List<FusionContributor>,
    val normalizedResidual: Double?,
    val trustworthy: Boolean,
    val reason: String,
)

data class PlayerSubmission(
    val estimateMeters: Double,
    val intervalHalfWidthMeters: Double,
)

data class RoundResult(
    val trueRangeMeters: Double,
    val submittedEstimateMeters: Double?,
    val absoluteErrorMeters: Double?,
    val intervalHalfWidthMeters: Double?,
    val intervalCoveredTruth: Boolean?,
    val officialTierPoints: Int,
    val gameScore: Int,
    val abstained: Boolean,
    val explanation: String,
)

enum class RoundPhase {
    BRIEFING,
    PLAYING,
    DEBRIEF,
}

data class GameState(
    val mission: Mission,
    val phase: RoundPhase = RoundPhase.BRIEFING,
    val selectedSensors: Set<SensorType> = emptySet(),
    val readings: Map<SensorType, SensorReading> = emptyMap(),
    val gates: Map<SensorType, GateDecision> = emptyMap(),
    val fusion: FusionResult = FusionResult(
        estimateMeters = null,
        sigmaMeters = null,
        contributors = emptyList(),
        normalizedResidual = null,
        trustworthy = false,
        reason = "No active sensor cues",
    ),
    val playerEstimateMeters: Double = 5_000.0,
    val intervalHalfWidthMeters: Double = 100.0,
    val result: RoundResult? = null,
) {
    val computeUsed: Int
        get() = selectedSensors.sumOf { it.computeCost }

    val computeRemaining: Int
        get() = mission.computeBudget - computeUsed
}

sealed interface GameAction {
    data object Start : GameAction
    data class ToggleSensor(val sensorType: SensorType) : GameAction
    data class SetEstimate(val meters: Double) : GameAction
    data class SetIntervalHalfWidth(val meters: Double) : GameAction
    data object UseFusedEstimate : GameAction
    data object Submit : GameAction
    data object Abstain : GameAction
    data class NewMission(val seed: Long) : GameAction
}

internal object MathConstants {
    const val TWO_PI: Double = 2.0 * PI
}
