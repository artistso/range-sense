package com.soquarky.rangesense.core

import kotlin.math.sqrt

object SensorFusion {
    private data class Candidate(
        val reading: SensorReading,
        val rawWeight: Double,
    )

    fun fuse(
        selected: Set<SensorType>,
        readings: Map<SensorType, SensorReading>,
        gates: Map<SensorType, GateDecision>,
    ): FusionResult {
        val candidates = selected.mapNotNull { type ->
            val reading = readings[type] ?: return@mapNotNull null
            val gate = gates[type] ?: return@mapNotNull null
            if (!gate.open) return@mapNotNull null

            val covarianceInflation = 1.0 + 1.5 * reading.nuisanceCoupling
            val effectiveVariance = reading.sigmaMeters * reading.sigmaMeters * covarianceInflation
            if (!effectiveVariance.isFinite() || effectiveVariance <= 0.0) return@mapNotNull null
            Candidate(reading, 1.0 / effectiveVariance)
        }

        if (candidates.isEmpty()) {
            return FusionResult(
                estimateMeters = null,
                sigmaMeters = null,
                contributors = emptyList(),
                normalizedResidual = null,
                trustworthy = false,
                reason = "No selected sensor passed the information gate",
            )
        }

        val totalWeight = candidates.sumOf { it.rawWeight }
        if (!totalWeight.isFinite() || totalWeight <= 0.0) {
            return FusionResult(
                estimateMeters = null,
                sigmaMeters = null,
                contributors = emptyList(),
                normalizedResidual = null,
                trustworthy = false,
                reason = "Fusion weights are numerically invalid",
            )
        }

        val estimate = candidates.sumOf { it.reading.estimateMeters * it.rawWeight } / totalWeight
        val sigma = sqrt(1.0 / totalWeight)
        val contributors = candidates.map {
            FusionContributor(
                sensorType = it.reading.sensorType,
                normalizedWeight = it.rawWeight / totalWeight,
                estimateMeters = it.reading.estimateMeters,
                sigmaMeters = it.reading.sigmaMeters,
            )
        }
        val weightedResidualVariance = contributors.sumOf {
            val delta = it.estimateMeters - estimate
            it.normalizedWeight * delta * delta
        }
        val normalizedResidual = sqrt(weightedResidualVariance) / sigma.coerceAtLeast(1e-9)
        val relativeSigma = sigma / estimate
        val trustworthy = relativeSigma <= 0.30 && normalizedResidual <= 3.0

        return FusionResult(
            estimateMeters = estimate,
            sigmaMeters = sigma,
            contributors = contributors,
            normalizedResidual = normalizedResidual,
            trustworthy = trustworthy,
            reason = when {
                !trustworthy && normalizedResidual > 3.0 -> "Active cues disagree beyond the fused uncertainty"
                !trustworthy -> "Fused uncertainty is too large"
                contributors.size == 1 -> "Single qualified cue; fusion is not cross-validated"
                else -> "Qualified cues agree within the modeled uncertainty"
            },
        )
    }
}
