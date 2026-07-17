package com.soquarky.rangesense.core

import kotlin.math.abs
import kotlin.test.Test
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
                if (fusion.estimateMeters == null) {
                    assertTrue(fusion.contributors.isEmpty())
                } else {
                    assertTrue(fusion.estimateMeters.isFinite() && fusion.estimateMeters > 0.0)
                    assertTrue(fusion.sigmaMeters != null && fusion.sigmaMeters.isFinite() && fusion.sigmaMeters > 0.0)
                    assertTrue(fusion.contributors.isNotEmpty())
                    assertTrue(abs(fusion.contributors.sumOf { it.normalizedWeight } - 1.0) < 1e-9)
                    assertTrue(fusion.contributors.all { it.sensorType in subset })
                    assertTrue(fusion.normalizedResidual != null && fusion.normalizedResidual.isFinite())
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
