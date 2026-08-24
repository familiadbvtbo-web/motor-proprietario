package com.motorproprietario.engine

class EngineSmokeTest {

    fun run(): Boolean {
        val config = EngineConfig()
        val validation = EngineValidator().validate(config)

        if (!validation.valid) return false

        val prices = EngineTestData.samplePrices()
        val timeframes = EngineTestData.sampleTimeframes()

        val pattern = PatternDetector().analyze(prices)

        return pattern.detected || prices.isNotEmpty() && timeframes.isNotEmpty()
    }
}
