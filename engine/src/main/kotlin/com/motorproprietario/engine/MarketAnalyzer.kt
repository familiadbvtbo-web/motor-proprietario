package com.motorproprietario.engine

class MarketAnalyzer {

    private val patternDetector = PatternDetector()
    private val signalEngine = SignalEngine()
    private val timeframeAnalyzer = MultiTimeframeAnalyzer()

    fun analyze(
        timeframes: Map<String, List<Double>>,
        falseSignal: Boolean = false
    ): SignalResult {

        val allValues = timeframes.values.flatten()

        val pattern = patternDetector.analyze(allValues)

        val confluence = ConfluenceAnalyzer().analyze(
            pattern = pattern,
            falseSignal = falseSignal
        )

        val timeframeResults = timeframeAnalyzer.analyze(timeframes)

        return signalEngine.generate(
            pattern = pattern,
            confluence = confluence,
            timeframes = timeframeResults
        )
    }
}
