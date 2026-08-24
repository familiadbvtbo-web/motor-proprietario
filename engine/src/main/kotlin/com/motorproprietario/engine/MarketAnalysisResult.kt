package com.motorproprietario.engine

data class MarketAnalysisResult(
    val marketScore: MarketScore,
    val cycle: CycleAnalysis,
    val sequence: SignalSequence,
    val timeframes: List<TimeframeResult>
)
