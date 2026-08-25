package com.motorproprietario.app

class KillSwitch(
    initiallyActive: Boolean = false
) {

    var active: Boolean = initiallyActive
        private set

    fun activate() {
        active = true
    }

    fun reset() {
        active = false
    }
}
