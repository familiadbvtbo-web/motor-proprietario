package com.motorproprietario.engine

class MarketEngine {

    private val falseSignalDetector = FalseSignalDetector()
    private val sequenceCalculator = SequenceCalculator()
    private val cycleAnalyzer = SignalCycleAnalyzer()
    private val scoreEngine = MarketScoreEngine()
    private val timeframeAnalyzer = MultiTimeframeAnalyzer()

    fun analyze(
        observations: List<SignalObservation>,
        timeframes: Map<String, List<Double>>,
        currentDirection: String,
        volatility: Double
    ): MarketAnalysisResult {

        val falseResults = observations.map { observation ->
            val futurePrice = observation.price
            falseSignalDetector.analyze(
                SignalInput(
                    price = observation.price,
                    previousPrice = observation.price,
                    volume = 0.0,
                    averageVolume = 1.0
                )
            )
        }

        val sequence = sequenceCalculator.calculate(falseResults)

        val cycle = cycleAnalyzer.analyze(
            sequence = sequence,
            currentDirection = currentDirection
        )

        val timeframeResults = timeframeAnalyzer.analyze(timeframes)

        val timeframeStrength =
            if (timeframeResults.isEmpty()) 0.0
            else timeframeResults.map { it.strength }.average().coerceIn(0.0, 1.0)

        val confluenceScore =
            if (timeframeResults.count { it.direction == currentDirection } > 0) {
                0.75
            } else {
                0.40
            }

        val marketScore = scoreEngine.calculate(
            confluenceScore = confluenceScore,
            sequenceScore = sequence.sequenceScore,
            timeframeStrength = timeframeStrength,
            reversalProbability = cycle.reversalProbability,
            volatility = volatility
        )

        return MarketAnalysisResult(
            marketScore = marketScore,
            cycle = cycle,
            sequence = sequence,
            timeframes = timeframeResults
        )
    }
}
