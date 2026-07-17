package com.soquarky.rangesense.core

import kotlin.math.sqrt

object SensorFusion {
    private const val RESIDUAL_VARIANCE_EPSILON = 1e-12

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
            if (!reading.estimateMeters.isFinite() || reading.estimateMeters <= 0.0) {
                return@mapNotNull null
            }
            if (!reading.sigmaMeters.isFinite() || reading.sigmaMeters <= 0.0) {
                return@mapNotNull null
            }
            if (!reading.nuisanceCoupling.isFinite() || reading.nuisanceCoupling !in 0.0..1.0) {
                return@mapNotNull null
            }

            val covarianceInflation = 1.0 + 1.5 * reading.nuisanceCoupling
            val effectiveVariance = reading.sigmaMeters * reading.sigmaMeters * covarianceInflation
            if (!effectiveVariance.isFinite() || effectiveVariance <= 0.0) return@mapNotNull null

            val rawWeight = 1.0 / effectiveVariance
            if (!rawWeight.isFinite() || rawWeight <= 0.0) return@mapNotNull null
            Candidate(reading, rawWeight)
        }

        if (candidates.isEmpty()) {
            return failure("No selected sensor passed the information gate")
        }

        val totalWeight = candidates.sumOf { it.rawWeight }
        if (!totalWeight.isFinite() || totalWeight <= 0.0) {
            return failure("Fusion weights are numerically invalid")
        }

        val weightedEstimateSum = candidates.sumOf {
            it.reading.estimateMeters * it.rawWeight
        }
        if (!weightedEstimateSum.isFinite()) {
            return failure("Weighted estimate sum is non-finite")
        }

        val estimate = weightedEstimateSum / totalWeight
        val sigma = sqrt(1.0 / totalWeight)
        if (!estimate.isFinite() || estimate <= 0.0 || !sigma.isFinite() || sigma <= 0.0) {
            return failure("Fused estimate or uncertainty is numerically invalid")
        }

        val contributors = candidates.map {
            FusionContributor(
                sensorType = it.reading.sensorType,
                normalizedWeight = it.rawWeight / totalWeight,
                estimateMeters = it.reading.estimateMeters,
                sigmaMeters = it.reading.sigmaMeters,
            )
        }
        if (contributors.any { !it.normalizedWeight.isFinite() || it.normalizedWeight <= 0.0 }) {
            return failure("Normalized fusion weights are numerically invalid")
        }

        val weightedResidualVariance = contributors.sumOf {
            val delta = it.estimateMeters - estimate
            it.normalizedWeight * delta * delta
        }
        if (!weightedResidualVariance.isFinite() ||
            weightedResidualVariance < -RESIDUAL_VARIANCE_EPSILON
        ) {
            return failure("Weighted residual variance is numerically invalid")
        }

        val normalizedResidual =
            sqrt(weightedResidualVariance.coerceAtLeast(0.0)) / sigma.coerceAtLeast(1e-9)
        val relativeSigma = sigma / estimate
        if (!normalizedResidual.isFinite() || !relativeSigma.isFinite()) {
            return failure("Fusion quality metrics are non-finite")
        }

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

    private fun failure(reason: String): FusionResult {
        return FusionResult(
            estimateMeters = null,
            sigmaMeters = null,
            contributors = emptyList(),
            normalizedResidual = null,
            trustworthy = false,
            reason = reason,
        )
    }
}
