package com.soquarky.rangesense.core

object GameEngine {
    fun initial(seed: Long = 83L): GameState = GameState(GrayTowerMissionFactory.create(seed))

    fun reduce(state: GameState, action: GameAction): GameState {
        return when (action) {
            GameAction.Start -> state.copy(phase = RoundPhase.PLAYING, result = null)

            is GameAction.ToggleSensor -> {
                if (state.phase != RoundPhase.PLAYING) return state
                val type = action.sensorType
                val alreadySelected = type in state.selectedSensors
                val newSelection = if (alreadySelected) {
                    state.selectedSensors - type
                } else {
                    val newCost = state.computeUsed + type.computeCost
                    if (newCost > state.mission.computeBudget) return state
                    state.selectedSensors + type
                }
                refreshSensors(state.copy(selectedSensors = newSelection))
            }

            is GameAction.SetEstimate -> state.copy(
                playerEstimateMeters = action.meters.coerceIn(500.0, 12_000.0),
            )

            is GameAction.SetIntervalHalfWidth -> state.copy(
                intervalHalfWidthMeters = action.meters.coerceIn(0.1, 2_000.0),
            )

            GameAction.UseFusedEstimate -> {
                val estimate = state.fusion.estimateMeters ?: return state
                state.copy(
                    playerEstimateMeters = estimate.coerceIn(500.0, 12_000.0),
                    intervalHalfWidthMeters = (1.96 * (state.fusion.sigmaMeters ?: state.intervalHalfWidthMeters))
                        .coerceIn(0.1, 2_000.0),
                )
            }

            GameAction.Submit -> {
                if (state.phase != RoundPhase.PLAYING) return state
                val result = RoundScorer.scoreSubmission(
                    mission = state.mission,
                    submission = PlayerSubmission(
                        estimateMeters = state.playerEstimateMeters,
                        intervalHalfWidthMeters = state.intervalHalfWidthMeters,
                    ),
                    computeUsed = state.computeUsed,
                )
                state.copy(phase = RoundPhase.DEBRIEF, result = result)
            }

            GameAction.Abstain -> {
                if (state.phase != RoundPhase.PLAYING) return state
                val allReadings = SensorType.entries.associateWith {
                    SensorSimulation.simulate(state.mission, it)
                }
                val allGates = allReadings.mapValues { FisherGate.evaluate(it.value) }
                val result = RoundScorer.scoreAbstention(
                    mission = state.mission,
                    gates = allGates,
                )
                state.copy(phase = RoundPhase.DEBRIEF, result = result)
            }

            is GameAction.NewMission -> initial(action.seed)
        }
    }

    private fun refreshSensors(state: GameState): GameState {
        val readings = state.selectedSensors.associateWith {
            SensorSimulation.simulate(state.mission, it)
        }
        val gates = readings.mapValues { FisherGate.evaluate(it.value) }
        val fusion = SensorFusion.fuse(state.selectedSensors, readings, gates)
        return state.copy(readings = readings, gates = gates, fusion = fusion)
    }
}
