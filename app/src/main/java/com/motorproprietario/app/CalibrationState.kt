package com.motorproprietario.app

/**
 * Estado persistível da calibração do Motor Proprietário.
 *
 * Este objeto representa somente uma calibração que já foi
 * calculada pelo CalibrationEngine.
 *
 * Ele não executa calibração e não altera o Motor sozinho.
 */
data class CalibrationState(

    /**
     * Versão do formato do estado.
     *
     * Permite evoluir a estrutura futuramente sem
     * perder compatibilidade com estados antigos.
     */
    val version: Int = 1,

    /**
     * Indica se esta calibração foi aprovada pelo
     * processo de treino, validação e teste.
     */
    val accepted: Boolean = false,

    /**
     * Peso aplicado à camada probabilística.
     *
     * Deve permanecer entre 0.0 e 1.0.
     */
    val probabilityWeight: Double = 0.50,

    /**
     * Peso aplicado à camada determinística.
     *
     * Deve permanecer entre 0.0 e 1.0.
     */
    val deterministicWeight: Double = 0.50,

    /**
     * Métricas utilizadas para auditoria.
     */
    val trainingMetrics: BacktestMetrics =
        BacktestMetrics(),

    val validationMetrics: BacktestMetrics =
        BacktestMetrics(),

    val testMetrics: BacktestMetrics =
        BacktestMetrics(),

    /**
     * Quantidade de configurações avaliadas
     * pelo CalibrationEngine.
     */
    val candidatesEvaluated: Int = 0,

    /**
     * Motivo pelo qual a calibração foi aceita
     * ou rejeitada.
     */
    val reason: String =
        "CALIBRACAO_NAO_EXECUTADA",

    /**
     * Momento em que este estado foi criado.
     */
    val calibratedAt: Long = 0L
) {

    /**
     * Garante que os pesos estejam dentro do intervalo
     * matematicamente válido e somem 1.0.
     */
    fun normalized(): CalibrationState {

        val probability =
            probabilityWeight
                .coerceIn(
                    0.0,
                    1.0
                )

        val deterministic =
            deterministicWeight
                .coerceIn(
                    0.0,
                    1.0
                )

        val total =
            probability +
                deterministic

        if (
            total <= 0.0
        ) {

            return copy(
                probabilityWeight =
                    0.50,

                deterministicWeight =
                    0.50
            )
        }

        return copy(
            probabilityWeight =
                probability /
                    total,

            deterministicWeight =
                deterministic /
                    total
        )
    }

    /**
     * Verifica se o estado pode ser utilizado
     * pelo Motor em produção.
     */
    fun isUsable(): Boolean {

        val normalized =
            normalized()

        return (

            version >= 1 &&

            accepted &&

            calibratedAt > 0L &&

            candidatesEvaluated > 0 &&

            normalized.probabilityWeight
                .isFinite() &&

            normalized.deterministicWeight
                .isFinite()

        )
    }

    /**
     * Converte um CalibrationResult produzido pelo
     * CalibrationEngine em um estado persistível.
     */
    companion object {

        fun fromResult(
            result:
                CalibrationResult
        ): CalibrationState {

            return CalibrationState(

                version =
                    1,

                accepted =
                    result.accepted,

                probabilityWeight =
                    result.selectedProbabilityWeight,

                deterministicWeight =
                    result.selectedDeterministicWeight,

                trainingMetrics =
                    result.trainingMetrics,

                validationMetrics =
                    result.validationMetrics,

                testMetrics =
                    result.testMetrics,

                candidatesEvaluated =
                    result.candidatesEvaluated,

                reason =
                    result.reason,

                calibratedAt =
                    result.calibratedAt
            ).normalized()
        }

        /**
         * Estado inicial seguro.
         *
         * Enquanto não houver calibração aceita,
         * o Motor permanece em 50/50.
         */
        fun initial():
            CalibrationState {

            return CalibrationState(

                version =
                    1,

                accepted =
                    false,

                probabilityWeight =
                    0.50,

                deterministicWeight =
                    0.50,

                candidatesEvaluated =
                    0,

                reason =
                    "CALIBRACAO_AINDA_NAO_EXECUTADA",

                calibratedAt =
                    0L
            )
        }
    }
}
