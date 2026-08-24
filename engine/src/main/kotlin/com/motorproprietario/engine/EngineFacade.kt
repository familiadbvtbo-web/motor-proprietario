package com.motorproprietario.engine

class EngineFacade(
    private val config: EngineConfig = EngineConfig()
) {

    private val validator = EngineValidator()
    private val engine = MarketEngine()

    fun analyze(
        observations: List<SignalObservation>,
        timeframes: Map<String, List<Double>>,
        currentDirection: String,
        volatility: Double
    ): MarketAnalysisResult {

        val validation = validator.validate(config)

        require(validation.valid) {
            validation.errors.joinToString("; ")
        }

        return engine.analyze(
            observations = observations,
            timeframes = timeframes,
            currentDirection = currentDirection,
            volatility = volatility
        )
    }
}
