package com.motorproprietario.engine

object EngineTestData {

    fun samplePrices(): List<Double> {
        return listOf(
            100.0,
            100.8,
            101.5,
            100.9,
            100.2,
            99.6,
            100.1,
            101.0
        )
    }

    fun sampleTimeframes(): Map<String, List<Double>> {
        return mapOf(
            "1m" to samplePrices(),
            "5m" to listOf(100.0, 100.5, 101.2, 101.8),
            "15m" to listOf(100.0, 100.7, 101.4, 102.0)
        )
    }
}
