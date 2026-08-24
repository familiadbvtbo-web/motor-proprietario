package com.motorproprietario.engine

data class ValidationResult(
    val valid: Boolean,
    val errors: List<String>
)

class EngineValidator {

    fun validate(
        config: EngineConfig
    ): ValidationResult {

        val errors = mutableListOf<String>()

        if (config.falseSignalThreshold <= 0.0) {
            errors.add("falseSignalThreshold deve ser maior que zero")
        }

        if (config.strongSequenceThreshold < 1) {
            errors.add("strongSequenceThreshold inválido")
        }

        if (config.moderateSequenceThreshold < 1) {
            errors.add("moderateSequenceThreshold inválido")
        }

        if (config.minimumScoreForSignal !in 0.0..1.0) {
            errors.add("minimumScoreForSignal deve estar entre 0 e 1")
        }

        if (config.minimumScoreForStrongSignal !in 0.0..1.0) {
            errors.add("minimumScoreForStrongSignal deve estar entre 0 e 1")
        }

        return ValidationResult(
            valid = errors.isEmpty(),
            errors = errors
        )
    }
}
