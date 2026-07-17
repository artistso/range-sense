package com.soquarky.rangesense.core

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CoreStressTests {
    @Test
    fun deterministicSimulationMaintainsNumericalInvariants() {
        val validSubsets = subsets().filter { set ->
            set.sumOf { it.computeCost } <= 28
        }

        repeat(2_000) { seedInt ->
            val seed = seedInt.toLong()
            val mission = GrayTowerMissionFactory.create(seed)
            val readings = SensorType.entries.associateWith { type ->
                SensorSimulation.simulate(mission, type)
            }
            val gates = readings.mapValues { (_, reading) -> FisherGate.evaluate(reading) }

            readings.values.forEach { reading ->
                assertTrue(reading.estimateMeters.isFinite() && reading.estimateMeters > 0.0)
                assertTrue(reading.sigmaMeters.isFinite() && reading.sigmaMeters > 0.0)
                assertTrue(reading.observedInformationPerMeterSquared.isFinite())
                assertTrue(reading.conditionalInformationPerMeterSquared.isFinite())
                assertTrue(reading.conditionalInformationPerMeterSquared >= 0.0)
                assertTrue(
                    reading.conditionalInformationPerMeterSquared <=
                        reading.observedInformationPerMeterSquared + 1e-18,
                )
                assertTrue(reading.retainedFraction in 0.0..1.0)
            }

            validSubsets.forEach { subset ->
                val fusion = SensorFusion.fuse(subset, readings, gates)
                val estimate = fusion.estimateMeters
                if (estimate == null) {
                    assertTrue(fusion.contributors.isEmpty())
                } else {
                    val sigma = requireNotNull(fusion.sigmaMeters)
                    assertTrue(estimate.isFinite() && estimate > 0.0)
                    assertTrue(sigma.isFinite() && sigma > 0.0)
                    assertTrue(fusion.contributors.isNotEmpty())
                    assertTrue(abs(fusion.contributors.sumOf { it.normalizedWeight } - 1.0) < 1e-9)
                    assertTrue(fusion.contributors.all { it.sensorType in subset })
                    assertTrue(fusion.normalizedResidual != null && fusion.normalizedResidual.isFinite())

                    val minimumContributor = fusion.contributors.minOf { it.estimateMeters }
                    val maximumContributor = fusion.contributors.maxOf { it.estimateMeters }
                    val hullTolerance = maxOf(1.0, abs(estimate)) * 1e-12
                    assertTrue(estimate >= minimumContributor - hullTolerance)
                    assertTrue(estimate <= maximumContributor + hullTolerance)
                }
            }
        }
    }

    @Test
    fun fusionIsStableUnderSensorIterationOrder() {
        val validSubsets = subsets().filter { set ->
            set.sumOf { it.computeCost } <= 28
        }

        repeat(500) { seedInt ->
            val mission = GrayTowerMissionFactory.create(seedInt.toLong())
            val readings = SensorType.entries.associateWith { type ->
                SensorSimulation.simulate(mission, type)
            }
            val gates = readings.mapValues { (_, reading) -> FisherGate.evaluate(reading) }

            validSubsets.forEach { subset ->
                val ordered = subset.toList()
                val forward = SensorFusion.fuse(LinkedHashSet(ordered), readings, gates)
                val reverse = SensorFusion.fuse(LinkedHashSet(ordered.asReversed()), readings, gates)
                val forwardEstimate = forward.estimateMeters
                val reverseEstimate = reverse.estimateMeters

                if (forwardEstimate == null || reverseEstimate == null) {
                    assertEquals(forwardEstimate, reverseEstimate)
                } else {
                    val estimateScale = maxOf(1.0, abs(forwardEstimate), abs(reverseEstimate))
                    assertTrue(abs(forwardEstimate - reverseEstimate) <= estimateScale * 1e-12)

                    val forwardSigma = requireNotNull(forward.sigmaMeters)
                    val reverseSigma = requireNotNull(reverse.sigmaMeters)
                    val sigmaScale = maxOf(1.0, abs(forwardSigma), abs(reverseSigma))
                    assertTrue(abs(forwardSigma - reverseSigma) <= sigmaScale * 1e-12)
                }
            }
        }
    }

    private fun subsets(): List<Set<SensorType>> {
        val types = SensorType.entries
        return (0 until (1 shl types.size)).map { mask ->
            types.filterIndexed { index, _ ->
                mask and (1 shl index) != 0
            }.toSet()
        }
    }
}
