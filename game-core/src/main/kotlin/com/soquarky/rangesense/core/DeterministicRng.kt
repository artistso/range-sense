package com.soquarky.rangesense.core

import kotlin.math.ln
import kotlin.math.sqrt
import kotlin.math.cos

/** SplitMix64-based deterministic generator suitable for reproducible game missions. */
class DeterministicRng(seed: Long) {
    private var state: Long = seed

    fun nextLong(): Long {
        state += -7046029254386353131L
        var z = state
        z = (z xor (z ushr 30)) * -4658895280553007687L
        z = (z xor (z ushr 27)) * -7723592293110705685L
        return z xor (z ushr 31)
    }

    fun nextDouble(): Double {
        val bits = nextLong() ushr 11
        return bits.toDouble() / 9_007_199_254_740_992.0
    }

    fun nextDouble(min: Double, max: Double): Double {
        require(max >= min)
        return min + (max - min) * nextDouble()
    }

    fun nextInt(bound: Int): Int {
        require(bound > 0)
        return (nextDouble() * bound).toInt().coerceAtMost(bound - 1)
    }

    fun gaussian(): Double {
        val u1 = nextDouble().coerceAtLeast(1e-12)
        val u2 = nextDouble()
        return sqrt(-2.0 * ln(u1)) * cos(MathConstants.TWO_PI * u2)
    }
}
