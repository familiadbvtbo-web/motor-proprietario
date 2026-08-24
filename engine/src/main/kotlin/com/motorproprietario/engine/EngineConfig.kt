package com.motorproprietario.engine

data class EngineConfig(
    val falseSignalThreshold: Double = 0.002,
    val strongSequenceThreshold: Int = 5,
    val moderateSequenceThreshold: Int = 3,
    val minimumScoreForSignal: Double = 0.65,
    val minimumScoreForStrongSignal: Double = 0.80
)
