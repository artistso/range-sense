import com.soquarky.rangesense.core.*

fun requireNear(actual: Double, expected: Double, epsilon: Double = 1e-9) {
    require(kotlin.math.abs(actual - expected) <= epsilon) {
        "Expected $expected, got $actual"
    }
}

fun main() {
    require(CidarInspiredTierScorer.points(10_000.0, 0.499999) == 40)
    require(CidarInspiredTierScorer.points(10_000.0, 0.5) == 16)
    require(CidarInspiredTierScorer.points(10_000.0, 2.0) == 12)
    require(CidarInspiredTierScorer.points(10_000.0, 5.0) == 4)
    require(CidarInspiredTierScorer.points(10_000.0, 15.0) == 0)

    var state = GameEngine.reduce(GameEngine.initial(8183L), GameAction.Start)
    SensorType.entries.forEach { type ->
        state = GameEngine.reduce(state, GameAction.ToggleSensor(type))
    }
    require(state.computeUsed <= state.mission.computeBudget)
    require(state.readings.values.all { it.observedInformationPerMeterSquared > 0.0 })
    require(state.readings.values.all { it.conditionalInformationPerMeterSquared >= 0.0 })

    val duplicated = GameEngine.reduce(GameEngine.initial(8183L), GameAction.Start).let { initial ->
        SensorType.entries.fold(initial) { acc, type -> GameEngine.reduce(acc, GameAction.ToggleSensor(type)) }
    }
    require(state.mission == duplicated.mission)
    require(state.readings == duplicated.readings)

    val submitted = GameEngine.reduce(
        GameEngine.reduce(state, GameAction.UseFusedEstimate),
        GameAction.Submit,
    )
    require(submitted.phase == RoundPhase.DEBRIEF)
    require(submitted.result != null)

    val truth = state.mission.target.rangeMeters
    requireNear(truth, listOf(2_500.0, 5_000.0, 7_500.0, 10_000.0).minBy { kotlin.math.abs(it - truth) })

    println("RANGE SENSE core self-test passed")
    println("Mission: ${state.mission.id}")
    println("Truth: ${state.mission.target.rangeMeters} m")
    println("Selected: ${state.selectedSensors.joinToString { it.tag }}")
    println("Compute: ${state.computeUsed}/${state.mission.computeBudget}")
    println("Fusion: ${state.fusion}")
    println("Result: ${submitted.result}")
}
