package com.soquarky.rangesense.core

import kotlin.math.max
import kotlin.math.pow

/**
 * Deterministic game simulation. The formulas preserve units and confound structure,
 * but are not calibrated models of any real sensor suite.
 */
object SensorSimulation {
    fun simulate(mission: Mission, sensorType: SensorType): SensorReading {
        val sensorSeed = mission.seed xor (sensorType.ordinal.toLong() * -7046029254386353131L)
        val rng = DeterministicRng(sensorSeed)
        val target = mission.target
        val env = mission.environment
        val trueRange = target.rangeMeters

        val baseRelativeSigma = when (sensorType) {
            SensorType.GEOMETRIC -> 0.004
            SensorType.SPECTRAL -> 0.005
            SensorType.PSF -> 0.008
            SensorType.POLARIMETRIC -> 0.010
            SensorType.TEMPORAL -> 0.012
        }

        val nuisance = NuisanceModel.coupling(sensorType, env, target)
        val hardFailure = NuisanceModel.hardFailure(sensorType, env, target)
        val rangeFactor = when (sensorType) {
            SensorType.GEOMETRIC -> (trueRange / 2_500.0).pow(1.35).coerceAtLeast(0.65)
            SensorType.SPECTRAL -> (trueRange / 5_000.0).pow(0.55).coerceAtLeast(0.65)
            SensorType.PSF -> (trueRange / 2_000.0).pow(1.05).coerceAtLeast(0.7)
            SensorType.POLARIMETRIC -> (trueRange / 4_000.0).pow(0.75).coerceAtLeast(0.7)
            SensorType.TEMPORAL -> (trueRange / 5_000.0).pow(0.80).coerceAtLeast(0.7)
        }

        val environmentalInflation = 1.0 + 5.0 * nuisance
        val sigma = max(0.25, trueRange * baseRelativeSigma * rangeFactor * environmentalInflation)

        val biasDirection = if (rng.nextDouble() < 0.5) -1.0 else 1.0
        val systematicBias = biasDirection * trueRange * baseRelativeSigma * nuisance.pow(1.4)
        val estimate = max(100.0, trueRange + systematicBias + rng.gaussian() * sigma)

        val observedInfo = 1.0 / (sigma * sigma)
        val retainedSensitivity = (1.0 - nuisance).coerceIn(0.0, 1.0)
        val conditionalInfo = if (hardFailure.first) {
            0.0
        } else {
            observedInfo * retainedSensitivity * retainedSensitivity
        }

        return SensorReading(
            sensorType = sensorType,
            estimateMeters = estimate,
            sigmaMeters = sigma,
            observedInformationPerMeterSquared = observedInfo,
            nuisanceCoupling = nuisance,
            conditionalInformationPerMeterSquared = conditionalInfo,
            hardFailure = hardFailure.first,
            failureReason = hardFailure.second,
        )
    }
}

object NuisanceModel {
    fun coupling(type: SensorType, env: Environment, target: Target): Double {
        return when (type) {
            SensorType.GEOMETRIC -> {
                val registration = (env.registrationErrorPixels / 3.0).coerceIn(0.0, 0.95)
                val longRange = ((target.rangeMeters - 3_000.0) / 7_000.0).coerceIn(0.0, 1.0)
                (0.7 * registration + 0.3 * longRange).coerceIn(0.0, 0.98)
            }

            SensorType.SPECTRAL -> {
                val visibility = (1.0 - env.visibilityKm / 15.0).coerceIn(0.0, 1.0)
                val humidity = ((env.humidity - 0.5) / 0.5).coerceIn(0.0, 1.0)
                val reflectance = ((0.3 - target.reflectance) / 0.3).coerceIn(0.0, 1.0)
                (0.55 * visibility + 0.20 * humidity + 0.25 * reflectance).coerceIn(0.0, 0.98)
            }

            SensorType.PSF -> {
                val visibility = (1.0 - env.visibilityKm / 10.0).coerceIn(0.0, 1.0)
                val turbulence = (env.turbulenceCn2 / 1e-13).coerceIn(0.0, 1.0)
                val lowLight = when (env.illumination) {
                    Illumination.DIRECT_SUN -> 0.0
                    Illumination.OVERCAST -> 0.15
                    Illumination.TWILIGHT -> 0.65
                    Illumination.NIGHT -> 0.9
                }
                (0.45 * visibility + 0.35 * turbulence + 0.20 * lowLight).coerceIn(0.0, 0.98)
            }

            SensorType.POLARIMETRIC -> {
                val contrast = ((1.5 - env.contrastRatio) / 0.5).coerceIn(0.0, 1.0)
                val clutter = env.backgroundClutter
                val reflectance = ((0.25 - target.reflectance) / 0.25).coerceIn(0.0, 1.0)
                val lowLight = when (env.illumination) {
                    Illumination.DIRECT_SUN -> 0.0
                    Illumination.OVERCAST -> 0.15
                    Illumination.TWILIGHT -> 0.55
                    Illumination.NIGHT -> 0.95
                }
                (0.25 * contrast + 0.30 * clutter + 0.20 * reflectance + 0.25 * lowLight)
                    .coerceIn(0.0, 0.99)
            }

            SensorType.TEMPORAL -> {
                val tooCalm = ((5e-15 - env.turbulenceCn2) / 5e-15).coerceIn(0.0, 1.0)
                val visibility = (1.0 - env.visibilityKm / 10.0).coerceIn(0.0, 1.0)
                val lowLight = when (env.illumination) {
                    Illumination.DIRECT_SUN -> 0.0
                    Illumination.OVERCAST -> 0.1
                    Illumination.TWILIGHT -> 0.5
                    Illumination.NIGHT -> 0.9
                }
                val precipitation = when (env.weather) {
                    Weather.CLEAR -> 0.0
                    Weather.HAZE -> 0.25
                    Weather.FOG -> 0.8
                    Weather.LIGHT_RAIN -> 0.7
                }
                (0.35 * tooCalm + 0.25 * visibility + 0.20 * lowLight + 0.20 * precipitation)
                    .coerceIn(0.0, 0.99)
            }
        }
    }

    fun hardFailure(type: SensorType, env: Environment, target: Target): Pair<Boolean, String?> {
        return when (type) {
            SensorType.GEOMETRIC -> when {
                env.registrationErrorPixels >= 3.0 -> true to "Registration error exceeds geometric cue limit"
                target.rangeMeters >= 10_000.0 && env.contrastRatio < 1.2 -> true to "Disparity is below the simulated registration floor"
                else -> false to null
            }

            SensorType.SPECTRAL -> when {
                env.weather == Weather.FOG && env.visibilityKm < 1.5 -> true to "Fog removes the simulated differential transmission cue"
                target.reflectance < 0.10 -> true to "Target signal is below the simulated spectral threshold"
                else -> false to null
            }

            SensorType.PSF -> when {
                env.visibilityKm < 1.5 -> true to "Atmospheric blur dominates the simulated PSF cue"
                env.illumination == Illumination.NIGHT -> true to "Insufficient simulated illumination for PSF estimation"
                else -> false to null
            }

            SensorType.POLARIMETRIC -> when {
                env.illumination == Illumination.NIGHT -> true to "No usable simulated linear-polarization signal"
                target.reflectance < 0.10 -> true to "Surface response is below the simulated polarization threshold"
                else -> false to null
            }

            SensorType.TEMPORAL -> when {
                env.turbulenceCn2 < 5e-16 -> true to "Temporal cue is below the simulated turbulence floor"
                env.weather == Weather.FOG && env.visibilityKm < 1.2 -> true to "Fog extinguishes the simulated temporal signal"
                else -> false to null
            }
        }
    }
}
