package com.soquarky.rangesense.core

import java.security.MessageDigest

/**
 * Canonical, deterministic mission evidence intended for replay, audit, and defect analysis.
 *
 * This is an engineering evidence record for the game simulation. It is not a cryptographic
 * signature, an identity assertion, or proof that the sensor models represent field performance.
 */
data class AssuranceEvidence(
    val schemaVersion: String,
    val canonicalRecord: String,
    val sha256Hex: String,
) {
    val shortId: String
        get() = sha256Hex.take(16)
}

object AssuranceEvidenceBuilder {
    const val SCHEMA_VERSION: String = "range-sense-assurance-v1"

    fun build(state: GameState): AssuranceEvidence {
        require(state.phase == RoundPhase.DEBRIEF) { "Evidence requires a completed mission" }
        val result = requireNotNull(state.result) { "Evidence requires a round result" }
        val mission = state.mission
        val target = mission.target
        val environment = mission.environment
        val fusion = state.fusion

        val lines = mutableListOf<String>()
        fun put(key: String, value: String) {
            require('\n' !in key && '=' !in key) { "Invalid evidence key" }
            lines += "$key=${escape(value)}"
        }

        put("schema", SCHEMA_VERSION)
        put("mission.id", mission.id)
        put("mission.title", mission.title)
        put("mission.seed", mission.seed.toString())
        put("mission.computeBudget", mission.computeBudget.toString())
        put("mission.timeLimitSeconds", mission.timeLimitSeconds.toString())

        put("target.rangeMeters", exact(target.rangeMeters))
        put("target.type", target.type.name)
        put("target.reflectance", exact(target.reflectance))
        put("target.angularSizeDegrees", exact(target.angularSizeDegrees))

        put("environment.visibilityKm", exact(environment.visibilityKm))
        put("environment.humidity", exact(environment.humidity))
        put("environment.contrastRatio", exact(environment.contrastRatio))
        put("environment.turbulenceCn2", exact(environment.turbulenceCn2))
        put("environment.illumination", environment.illumination.name)
        put("environment.weather", environment.weather.name)
        put("environment.registrationErrorPixels", exact(environment.registrationErrorPixels))
        put("environment.backgroundClutter", exact(environment.backgroundClutter))

        val selected = state.selectedSensors.sortedBy { it.name }
        put("decision.selectedSensors", selected.joinToString(",") { it.name })
        put("decision.computeUsed", state.computeUsed.toString())
        put("decision.playerEstimateMeters", exact(state.playerEstimateMeters))
        put("decision.intervalHalfWidthMeters", exact(state.intervalHalfWidthMeters))

        SensorType.entries.sortedBy { it.name }.forEach { type ->
            state.readings[type]?.let { reading ->
                val prefix = "reading.${type.name}"
                put("$prefix.estimateMeters", exact(reading.estimateMeters))
                put("$prefix.sigmaMeters", exact(reading.sigmaMeters))
                put("$prefix.observedInformationPerMeterSquared", exact(reading.observedInformationPerMeterSquared))
                put("$prefix.nuisanceCoupling", exact(reading.nuisanceCoupling))
                put("$prefix.conditionalInformationPerMeterSquared", exact(reading.conditionalInformationPerMeterSquared))
                put("$prefix.hardFailure", reading.hardFailure.toString())
                put("$prefix.failureReason", reading.failureReason ?: "null")
            }
            state.gates[type]?.let { gate ->
                val prefix = "gate.${type.name}"
                put("$prefix.open", gate.open.toString())
                put("$prefix.retainedFraction", exact(gate.retainedFraction))
                put("$prefix.reason", gate.reason)
            }
        }

        put("fusion.estimateMeters", nullableExact(fusion.estimateMeters))
        put("fusion.sigmaMeters", nullableExact(fusion.sigmaMeters))
        put("fusion.normalizedResidual", nullableExact(fusion.normalizedResidual))
        put("fusion.trustworthy", fusion.trustworthy.toString())
        put("fusion.reason", fusion.reason)
        fusion.contributors.sortedBy { it.sensorType.name }.forEachIndexed { index, contributor ->
            val prefix = "fusion.contributor.$index"
            put("$prefix.sensorType", contributor.sensorType.name)
            put("$prefix.normalizedWeight", exact(contributor.normalizedWeight))
            put("$prefix.estimateMeters", exact(contributor.estimateMeters))
            put("$prefix.sigmaMeters", exact(contributor.sigmaMeters))
        }

        put("result.trueRangeMeters", exact(result.trueRangeMeters))
        put("result.submittedEstimateMeters", nullableExact(result.submittedEstimateMeters))
        put("result.absoluteErrorMeters", nullableExact(result.absoluteErrorMeters))
        put("result.intervalHalfWidthMeters", nullableExact(result.intervalHalfWidthMeters))
        put("result.intervalCoveredTruth", result.intervalCoveredTruth?.toString() ?: "null")
        put("result.officialTierPoints", result.officialTierPoints.toString())
        put("result.gameScore", result.gameScore.toString())
        put("result.abstained", result.abstained.toString())
        put("result.explanation", result.explanation)

        val canonicalRecord = lines.joinToString(separator = "\n", postfix = "\n")
        return AssuranceEvidence(
            schemaVersion = SCHEMA_VERSION,
            canonicalRecord = canonicalRecord,
            sha256Hex = sha256(canonicalRecord),
        )
    }

    private fun exact(value: Double): String {
        require(value.isFinite()) { "Evidence cannot encode non-finite values" }
        return java.lang.Double.toHexString(value)
    }

    private fun nullableExact(value: Double?): String = value?.let(::exact) ?: "null"

    private fun escape(value: String): String = buildString(value.length) {
        value.forEach { character ->
            when (character) {
                '%' -> append("%25")
                '\n' -> append("%0A")
                '\r' -> append("%0D")
                '=' -> append("%3D")
                else -> append(character)
            }
        }
    }

    private fun sha256(value: String): String = MessageDigest
        .getInstance("SHA-256")
        .digest(value.toByteArray(Charsets.UTF_8))
        .joinToString(separator = "") { byte ->
            (byte.toInt() and 0xff).toString(16).padStart(2, '0')
        }
}
