import com.soquarky.rangesense.core.*
import kotlin.math.abs

private fun subsets(): List<Set<SensorType>> {
    val types = SensorType.entries
    return (0 until (1 shl types.size)).map { mask ->
        types.filterIndexed { index, _ -> mask and (1 shl index) != 0 }.toSet()
    }
}

fun main() {
    val validSubsets = subsets().filter { set -> set.sumOf { it.computeCost } <= 28 }
    var missions = 0
    var gateOpenCount = 0
    var gateClosedCount = 0
    var fusionCount = 0
    var noCueMissions = 0
    val rangeCounts = mutableMapOf<Double, Int>()

    for (seed in 0L until 2_000L) {
        val mission = GrayTowerMissionFactory.create(seed)
        missions += 1
        rangeCounts[mission.target.rangeMeters] = rangeCounts.getOrDefault(mission.target.rangeMeters, 0) + 1

        val readings = SensorType.entries.associateWith { SensorSimulation.simulate(mission, it) }
        val gates = readings.mapValues { FisherGate.evaluate(it.value) }
        if (gates.values.none { it.open }) noCueMissions += 1

        readings.values.forEach { reading ->
            require(reading.estimateMeters.isFinite() && reading.estimateMeters > 0.0)
            require(reading.sigmaMeters.isFinite() && reading.sigmaMeters > 0.0)
            require(reading.observedInformationPerMeterSquared.isFinite())
            require(reading.conditionalInformationPerMeterSquared.isFinite())
            require(reading.conditionalInformationPerMeterSquared >= 0.0)
            require(reading.conditionalInformationPerMeterSquared <= reading.observedInformationPerMeterSquared + 1e-18)
            require(reading.retainedFraction in 0.0..1.0)
        }

        gates.values.forEach { gate ->
            if (gate.open) {
                gateOpenCount += 1
                require(gate.retainedFraction >= 0.15 - 1e-12)
            } else {
                gateClosedCount += 1
            }
        }

        validSubsets.forEach { subset ->
            val fusion = SensorFusion.fuse(subset, readings, gates)
            if (fusion.estimateMeters != null) {
                fusionCount += 1
                require(fusion.estimateMeters.isFinite() && fusion.estimateMeters > 0.0)
                require(fusion.sigmaMeters != null && fusion.sigmaMeters.isFinite() && fusion.sigmaMeters > 0.0)
                require(fusion.contributors.isNotEmpty())
                require(abs(fusion.contributors.sumOf { it.normalizedWeight } - 1.0) < 1e-9)
                require(fusion.contributors.all { it.sensorType in subset })
                require(fusion.normalizedResidual != null && fusion.normalizedResidual.isFinite())
            } else {
                require(fusion.contributors.isEmpty())
            }
        }

        var state = GameEngine.reduce(GameEngine.initial(seed), GameAction.Start)
        SensorType.entries.forEach { type ->
            state = GameEngine.reduce(state, GameAction.ToggleSensor(type))
            require(state.computeUsed <= state.mission.computeBudget)
        }

        val abstained = GameEngine.reduce(GameEngine.reduce(GameEngine.initial(seed), GameAction.Start), GameAction.Abstain)
        val anyAvailable = gates.values.any { it.open }
        if (anyAvailable) require(abstained.result!!.gameScore <= 0)
        else require(abstained.result!!.gameScore > 0)
    }

    require(CidarInspiredTierScorer.points(2_500.0, 0.5) == 4)
    require(CidarInspiredTierScorer.points(5_000.0, 2.0) == 6)
    require(CidarInspiredTierScorer.points(7_500.0, 5.0) == 3)
    require(CidarInspiredTierScorer.points(10_000.0, 15.0) == 0)

    println("RANGE SENSE stress test passed")
    println("Missions: $missions")
    println("Range distribution: ${rangeCounts.toSortedMap()}")
    println("Gate open decisions: $gateOpenCount")
    println("Gate closed decisions: $gateClosedCount")
    println("Missions with no informative cue: $noCueMissions")
    println("Valid compute-budget subsets: ${validSubsets.size}")
    println("Finite fusion results checked: $fusionCount")
}
