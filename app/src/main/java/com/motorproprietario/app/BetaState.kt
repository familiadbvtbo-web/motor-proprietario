package com.motorproprietario.app

data class BetaState(
    val connected: Boolean,
    val dataQuality: String,
    val paperMode: Boolean,
    val lastUpdate: Long
)
