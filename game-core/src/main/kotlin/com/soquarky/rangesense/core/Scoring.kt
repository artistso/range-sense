package com.soquarky.rangesense.core

import kotlin.math.abs
import kotlin.math.roundToInt

object CidarInspiredTierScorer {
    private val ranges = doubleArrayOf(2_500.0, 5_000.0, 7_500.0, 10_000.0)
    private val tierPointRows = arrayOf(
        intArrayOf(5, 10, 20, 40),
        intArrayOf(4, 8, 12, 16),
        intArrayOf(3, 6, 9, 12),
        intArrayOf(1, 2, 3, 4),
    )

    fun points(nominalRangeMeters: Double, absoluteErrorMeters: Double): Int {
        require(absoluteErrorMeters >= 0.0)
        val rangeIndex = ranges.indices.minBy { abs(ranges[it] - nominalRangeMeters) }
        return when {
            absoluteErrorMeters < 0.5 -> tierPointRows[0][rangeIndex]
            absoluteErrorMeters < 2.0 -> tierPointRows[1][rangeIndex]
            absoluteErrorMeters < 5.0 -> tierPointRows[2][rangeIndex]
            absoluteErrorMeters < 15.0 -> tierPointRows[3][rangeIndex]
            else -> 0
        }
    }
}

object RoundScorer {
    fun scoreSubmission(
        mission: Mission,
        submission: PlayerSubmission,
        computeUsed: Int,
    ): RoundResult {
        val truth = mission.target.rangeMeters
        val error = abs(submission.estimateMeters - truth)
        val covered = error <= submission.intervalHalfWidthMeters
        val officialPoints = CidarInspiredTierScorer.points(truth, error)

        val base = officialPoints * 100
        val intervalRelativeWidth = submission.intervalHalfWidthMeters / truth
        val sharpness = (1.0 / (1.0 + 8.0 * intervalRelativeWidth)).coerceIn(0.15, 1.0)
        val calibrationAdjustment = if (covered) {
            (150.0 * sharpness).roundToInt()
        } else {
            val missRatio = error / submission.intervalHalfWidthMeters.coerceAtLeast(0.1)
            -(100.0 * missRatio.coerceAtMost(5.0)).roundToInt()
        }
        val computeBonus = ((mission.computeBudget - computeUsed).coerceAtLeast(0) * 4)
        val total = base + calibrationAdjustment + computeBonus

        return RoundResult(
            trueRangeMeters = truth,
            submittedEstimateMeters = submission.estimateMeters,
            absoluteErrorMeters = error,
            intervalHalfWidthMeters = submission.intervalHalfWidthMeters,
            intervalCoveredTruth = covered,
            officialTierPoints = officialPoints,
            gameScore = total,
            abstained = false,
            explanation = buildString {
                append("Official tier: $officialPoints points. ")
                append(if (covered) "The declared interval covered truth. " else "The declared interval missed truth. ")
                append("Compute used: $computeUsed/${mission.computeBudget}.")
            },
        )
    }

    fun scoreAbstention(
        mission: Mission,
        gates: Map<SensorType, GateDecision>,
    ): RoundResult {
        val openCount = gates.values.count { it.open }
        val correct = openCount == 0
        val score = if (correct) 300 else -75 * openCount
        return RoundResult(
            trueRangeMeters = mission.target.rangeMeters,
            submittedEstimateMeters = null,
            absoluteErrorMeters = null,
            intervalHalfWidthMeters = null,
            intervalCoveredTruth = null,
            officialTierPoints = 0,
            gameScore = score,
            abstained = true,
            explanation = if (correct) {
                "Correct abstention: no active cue passed the information gate."
            } else {
                "Spurious abstention: $openCount available cue(s) remained informative."
            },
        )
    }
}
