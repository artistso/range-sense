package com.soquarky.rangesense.core

object GrayTowerMissionFactory {
    private val nominalRanges = doubleArrayOf(2_500.0, 5_000.0, 7_500.0, 10_000.0)

    fun create(seed: Long): Mission {
        val rng = DeterministicRng(seed)
        val range = nominalRanges[rng.nextInt(nominalRanges.size)]
        val weather = when (rng.nextInt(4)) {
            0 -> Weather.CLEAR
            1 -> Weather.HAZE
            2 -> Weather.FOG
            else -> Weather.LIGHT_RAIN
        }
        val illumination = when (rng.nextInt(4)) {
            0 -> Illumination.DIRECT_SUN
            1 -> Illumination.OVERCAST
            2 -> Illumination.TWILIGHT
            else -> Illumination.NIGHT
        }

        val visibility = when (weather) {
            Weather.CLEAR -> rng.nextDouble(12.0, 25.0)
            Weather.HAZE -> rng.nextDouble(5.0, 12.0)
            Weather.FOG -> rng.nextDouble(0.8, 4.0)
            Weather.LIGHT_RAIN -> rng.nextDouble(3.0, 9.0)
        }

        val environment = Environment(
            visibilityKm = visibility,
            humidity = when (weather) {
                Weather.CLEAR -> rng.nextDouble(0.25, 0.65)
                Weather.HAZE -> rng.nextDouble(0.45, 0.8)
                Weather.FOG -> rng.nextDouble(0.8, 0.99)
                Weather.LIGHT_RAIN -> rng.nextDouble(0.7, 0.98)
            },
            contrastRatio = rng.nextDouble(1.05, 3.2),
            turbulenceCn2 = rng.nextDouble(1e-16, 2e-13),
            illumination = illumination,
            weather = weather,
            registrationErrorPixels = rng.nextDouble(0.1, 3.2),
            backgroundClutter = rng.nextDouble(0.05, 0.85),
        )

        val target = Target(
            rangeMeters = range,
            type = TargetType.PANEL,
            reflectance = rng.nextDouble(0.08, 0.8),
            angularSizeDegrees = 0.7 * (2_500.0 / range),
        )

        return Mission(
            id = "gray-tower-$seed",
            title = "The Gray Tower",
            seed = seed,
            target = target,
            environment = environment,
            computeBudget = 28,
            timeLimitSeconds = 90,
        )
    }
}
