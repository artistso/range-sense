package com.soquarky.rangesense.core

object FisherGate {
    private const val MIN_RETAINED_FRACTION = 0.15
    private const val MAX_RELATIVE_SIGMA = 0.35
    private const val INFORMATION_EPSILON = 1e-18

    fun evaluate(reading: SensorReading): GateDecision {
        if (reading.hardFailure) {
            return closed(
                reading = reading,
                reason = reading.failureReason ?: "Cue failed its validity checks",
                retainedFraction = 0.0,
            )
        }

        numericalFailureReason(reading)?.let { reason ->
            return closed(reading, reason, retainedFraction = 0.0)
        }

        val relativeSigma = reading.sigmaMeters / reading.estimateMeters
        if (!relativeSigma.isFinite() || relativeSigma > MAX_RELATIVE_SIGMA) {
            return closed(
                reading = reading,
                reason = "Relative uncertainty exceeds ${(MAX_RELATIVE_SIGMA * 100).toInt()}%",
                retainedFraction = reading.retainedFraction,
            )
        }

        val retained = reading.retainedFraction
        val open = retained >= MIN_RETAINED_FRACTION
        return GateDecision(
            sensorType = reading.sensorType,
            open = open,
            retainedFraction = retained,
            reason = if (open) {
                "Conditional information retained"
            } else {
                "Nuisance coupling removes too much range information"
            },
        )
    }

    private fun numericalFailureReason(reading: SensorReading): String? {
        return when {
            !reading.estimateMeters.isFinite() || reading.estimateMeters <= 0.0 ->
                "Cue estimate is non-finite or non-positive"

            !reading.sigmaMeters.isFinite() || reading.sigmaMeters <= 0.0 ->
                "Cue uncertainty is non-finite or non-positive"

            !reading.observedInformationPerMeterSquared.isFinite() ||
                reading.observedInformationPerMeterSquared <= 0.0 ->
                "Observed information is non-finite or non-positive"

            !reading.conditionalInformationPerMeterSquared.isFinite() ||
                reading.conditionalInformationPerMeterSquared < 0.0 ->
                "Conditional information is non-finite or negative"

            reading.conditionalInformationPerMeterSquared >
                reading.observedInformationPerMeterSquared + INFORMATION_EPSILON ->
                "Conditional information exceeds observed information"

            !reading.nuisanceCoupling.isFinite() || reading.nuisanceCoupling !in 0.0..1.0 ->
                "Nuisance coupling is outside the closed unit interval"

            !reading.retainedFraction.isFinite() || reading.retainedFraction !in 0.0..1.0 ->
                "Retained information fraction is outside the closed unit interval"

            else -> null
        }
    }

    private fun closed(
        reading: SensorReading,
        reason: String,
        retainedFraction: Double,
    ): GateDecision {
        return GateDecision(
            sensorType = reading.sensorType,
            open = false,
            retainedFraction = retainedFraction,
            reason = reason,
        )
    }
}
