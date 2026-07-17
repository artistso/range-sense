package com.soquarky.rangesense.core

object FisherGate {
    private const val MIN_RETAINED_FRACTION = 0.15
    private const val MAX_RELATIVE_SIGMA = 0.35

    fun evaluate(reading: SensorReading): GateDecision {
        if (reading.hardFailure) {
            return GateDecision(
                sensorType = reading.sensorType,
                open = false,
                retainedFraction = 0.0,
                reason = reading.failureReason ?: "Cue failed its validity checks",
            )
        }

        val relativeSigma = reading.sigmaMeters / reading.estimateMeters
        if (relativeSigma > MAX_RELATIVE_SIGMA) {
            return GateDecision(
                sensorType = reading.sensorType,
                open = false,
                retainedFraction = reading.retainedFraction,
                reason = "Relative uncertainty exceeds ${(MAX_RELATIVE_SIGMA * 100).toInt()}%",
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
}
