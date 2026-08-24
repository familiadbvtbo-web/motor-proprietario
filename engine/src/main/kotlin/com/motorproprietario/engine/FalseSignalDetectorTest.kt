package com.motorproprietario.engine

class FalseSignalDetectorTest {

    fun run(): Boolean {
        val detector = FalseSignalDetector()

        val result = detector.analyze(
            SignalInput(
                price = 100.0,
                previousPrice = 100.5,
                volume = 400.0,
                averageVolume = 1000.0
            )
        )

        return result.confidence in 0.0..1.0
    }
}
