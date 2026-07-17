package com.soquarky.rangesense.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class CoreTests {
    @Test
    fun strictScoringThresholdsAreRespected() {
        assertEquals(40, CidarInspiredTierScorer.points(10_000.0, 0.499999))
        assertEquals(16, CidarInspiredTierScorer.points(10_000.0, 0.5))
        assertEquals(12, CidarInspiredTierScorer.points(10_000.0, 2.0))
        assertEquals(4, CidarInspiredTierScorer.points(10_000.0, 5.0))
        assertEquals(0, CidarInspiredTierScorer.points(10_000.0, 15.0))
    }

    @Test
    fun missionAndReadingsAreDeterministic() {
        val a = GameEngine.initial(8183L)
        val b = GameEngine.initial(8183L)
        assertEquals(a.mission, b.mission)

        val a2 = GameEngine.reduce(GameEngine.reduce(a, GameAction.Start), GameAction.ToggleSensor(SensorType.SPECTRAL))
        val b2 = GameEngine.reduce(GameEngine.reduce(b, GameAction.Start), GameAction.ToggleSensor(SensorType.SPECTRAL))
        assertEquals(a2.readings, b2.readings)
    }

    @Test
    fun differentSeedsProduceDifferentMissions() {
        assertNotEquals(GameEngine.initial(1L).mission, GameEngine.initial(2L).mission)
    }

    @Test
    fun budgetCannotBeExceeded() {
        var state = GameEngine.reduce(GameEngine.initial(83L), GameAction.Start)
        SensorType.entries.forEach { state = GameEngine.reduce(state, GameAction.ToggleSensor(it)) }
        assertTrue(state.computeUsed <= state.mission.computeBudget)
    }

    @Test
    fun hardFailureClosesGate() {
        val reading = SensorReading(
            sensorType = SensorType.GEOMETRIC,
            estimateMeters = 2_500.0,
            sigmaMeters = 10.0,
            observedInformationPerMeterSquared = 0.01,
            nuisanceCoupling = 0.2,
            conditionalInformationPerMeterSquared = 0.0,
            hardFailure = true,
            failureReason = "test",
        )
        assertFalse(FisherGate.evaluate(reading).open)
    }
}
