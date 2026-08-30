package com.motorproprietario.app

import kotlin.math.abs

enum class SequenceStage {

    S0,
    S1,
    S2,
    S3,
    S4
}

data class SequenceInput(

    /*
     * Existe um sinal direcional válido?
     */
    val signalDetected: Boolean,

    /*
     * O sinal recebeu confirmação MTF?
     */
    val confirmation: Boolean,

    /*
     * O movimento continuou?
     */
    val continuation: Boolean,

    /*
     * Algum fator invalidou a sequência?
     */
    val invalidated: Boolean,

    /*
     * ============================================================
     * NOVOS DADOS DO MOTOR PROPRIETÁRIO
     * ============================================================
     *
     * Valores padrão preservam compatibilidade
     * com chamadas antigas.
     */

    /*
     * Direção atual:
     *
     * COMPRA
     * VENDA
     * NEUTRO
     */
    val direction: String = "NEUTRO",

    /*
     * Score direcional atual.
     *
     * 0 = venda extrema
     * 50 = equilíbrio
     * 100 = compra extrema
     */
    val directionalScore: Double = 50.0,

    /*
     * Probabilidade/força atual da direção.
     */
    val directionalProbability: Double = 0.0,

    /*
     * Risco de falso sinal.
     *
     * 0 = baixo
     * 100 = extremo
     */
    val falseSignalRisk: Double = 0.0,

    /*
     * Confluência entre timeframes.
     */
    val mtfConfluence: Double = 0.0
)

data class SequenceResult(

    /*
     * Novo estágio.
     */
    val stage: SequenceStage,

    /*
     * Sequência realmente confirmada.
     */
    val confirmed: Boolean
)

object SequenceEngine {

    /*
     * ============================================================
     * LIMITES DO MOTOR
     * ============================================================
     */

    private const val MIN_DIRECTIONAL_SCORE =
        60.0

    private const val MAX_FALSE_SIGNAL_RISK =
        45.0

    private const val MIN_MTF_CONFLUENCE =
        60.0

    private const val MIN_DIRECTIONAL_PROBABILITY =
        60.0

    /*
     * ============================================================
     * NORMALIZAÇÃO
     * ============================================================
     */

    private fun normalizeDirection(
        direction: String
    ): String {

        return direction
            .trim()
            .uppercase()
            .let {

                when (it) {

                    "COMPRA" ->
                        "COMPRA"

                    "VENDA" ->
                        "VENDA"

                    else ->
                        "NEUTRO"
                }
            }
    }

    /*
     * ============================================================
     * DIREÇÃO É VÁLIDA?
     * ============================================================
     *
     * O motor não pode considerar um sinal válido
     * simplesmente porque existe uma direção escrita.
     */

    private fun validDirection(
        input: SequenceInput
    ): Boolean {

        val direction =
            normalizeDirection(
                input.direction
            )

        if (
            direction == "NEUTRO"
        ) {
            return false
        }

        val score =
            input.directionalScore
                .coerceIn(
                    0.0,
                    100.0
                )

        val probability =
            input.directionalProbability
                .coerceIn(
                    0.0,
                    100.0
                )

        val risk =
            input.falseSignalRisk
                .coerceIn(
                    0.0,
                    100.0
                )

        /*
         * COMPRA exige score >= 60.
         * VENDA exige score <= 40.
         */

        val scoreValid =
            when (direction) {

                "COMPRA" ->
                    score >=
                        MIN_DIRECTIONAL_SCORE

                "VENDA" ->
                    score <=
                        100.0 -
                        MIN_DIRECTIONAL_SCORE

                else ->
                    false
            }

        /*
         * Se a probabilidade foi fornecida,
         * ela também precisa ser suficiente.
         *
         * Valor 0 mantém compatibilidade com
         * chamadas antigas.
         */

        val probabilityValid =
            probability <= 0.0 ||
            probability >=
                MIN_DIRECTIONAL_PROBABILITY

        /*
         * Falso sinal acima do limite invalida
         * o sinal.
         */

        val riskValid =
            risk <
                MAX_FALSE_SIGNAL_RISK

        return scoreValid &&
            probabilityValid &&
            riskValid
    }

    /*
     * ============================================================
     * CONFIRMAÇÃO MTF VÁLIDA
     * ============================================================
     */

    private fun validConfirmation(
        input: SequenceInput
    ): Boolean {

        if (
            !input.confirmation
        ) {
            return false
        }

        val mtf =
            input.mtfConfluence
                .coerceIn(
                    0.0,
                    100.0
                )

        /*
         * Valor 0 mantém compatibilidade com
         * o fluxo antigo.
         */

        return mtf <= 0.0 ||
            mtf >=
                MIN_MTF_CONFLUENCE
    }

    /*
     * ============================================================
     * CONTINUAÇÃO VÁLIDA
     * ============================================================
     */

    private fun validContinuation(
        input: SequenceInput
    ): Boolean {

        return input.continuation &&
            validDirection(input)
    }

    /*
     * ============================================================
     * AVANÇO DA SEQUÊNCIA
     * ============================================================
     *
     * S0 = sem sinal
     *
     * S1 = sinal detectado
     *
     * S2 = confirmação
     *
     * S3 = continuação
     *
     * S4 = sequência confirmada
     *
     * A sequência somente chega a S4 quando
     * existe direção, confirmação e continuação.
     */

    fun advance(
        current: SequenceStage,
        input: SequenceInput
    ): SequenceResult {

        /*
         * ========================================================
         * 1. INVALIDAÇÃO GLOBAL
         * ========================================================
         */

        if (
            input.invalidated
        ) {

            return SequenceResult(

                stage =
                    SequenceStage.S0,

                confirmed =
                    false
            )
        }

        /*
         * ========================================================
         * 2. SINAL DIRECIONAL INVÁLIDO
         * ========================================================
         */

        if (
            !input.signalDetected ||
            !validDirection(input)
        ) {

            return SequenceResult(

                stage =
                    SequenceStage.S0,

                confirmed =
                    false
            )
        }

        /*
         * ========================================================
         * 3. MÁQUINA DE ESTADOS
         * ========================================================
         */

        return when (
            current
        ) {

            /*
             * ----------------------------------------------------
             * S0
             * ----------------------------------------------------
             *
             * Detectou sinal válido.
             *
             * S0 -> S1
             */

            SequenceStage.S0 -> {

                SequenceResult(

                    stage =
                        SequenceStage.S1,

                    confirmed =
                        false
                )
            }

            /*
             * ----------------------------------------------------
             * S1
             * ----------------------------------------------------
             *
             * Sinal válido.
             *
             * Aguarda confirmação.
             */

            SequenceStage.S1 -> {

                if (
                    validConfirmation(input)
                ) {

                    SequenceResult(

                        stage =
                            SequenceStage.S2,

                        confirmed =
                            false
                    )

                } else {

                    SequenceResult(

                        stage =
                            SequenceStage.S1,

                        confirmed =
                            false
                    )
                }
            }

            /*
             * ----------------------------------------------------
             * S2
             * ----------------------------------------------------
             *
             * Sinal confirmado.
             *
             * Aguarda continuação na mesma direção.
             */

            SequenceStage.S2 -> {

                if (
                    validContinuation(input) &&
                    validConfirmation(input)
                ) {

                    SequenceResult(

                        stage =
                            SequenceStage.S3,

                        confirmed =
                            false
                    )

                } else if (
                    !validConfirmation(input)
                ) {

                    /*
                     * Perdeu confirmação.
                     *
                     * Não mantém S2 artificialmente.
                     */

                    SequenceResult(

                        stage =
                            SequenceStage.S1,

                        confirmed =
                            false
                    )

                } else {

                    SequenceResult(

                        stage =
                            SequenceStage.S2,

                        confirmed =
                            false
                    )
                }
            }

            /*
             * ----------------------------------------------------
             * S3
             * ----------------------------------------------------
             *
             * Já houve confirmação + continuação.
             *
             * Exige novamente os dois fatores para
             * chegar ao S4.
             */

            SequenceStage.S3 -> {

                if (
                    validConfirmation(input) &&
                    validContinuation(input)
                ) {

                    SequenceResult(

                        stage =
                            SequenceStage.S4,

                        confirmed =
                            true
                    )

                } else if (
                    !validConfirmation(input)
                ) {

                    SequenceResult(

                        stage =
                            SequenceStage.S1,

                        confirmed =
                            false
                    )

                } else {

                    SequenceResult(

                        stage =
                            SequenceStage.S3,

                        confirmed =
                            false
                    )
                }
            }

            /*
             * ----------------------------------------------------
             * S4
             * ----------------------------------------------------
             *
             * Sequência confirmada.
             *
             * Para continuar confirmada:
             *
             * 1. sinal continua válido
             * 2. confirmação continua válida
             * 3. continuação continua válida
             * 4. risco continua aceitável
             */

            SequenceStage.S4 -> {

                if (
                    validConfirmation(input) &&
                    validContinuation(input)
                ) {

                    SequenceResult(

                        stage =
                            SequenceStage.S4,

                        confirmed =
                            true
                    )

                } else if (
                    validConfirmation(input)
                ) {

                    /*
                     * Ainda existe confirmação,
                     * mas a continuação parou.
                     *
                     * Volta para S3.
                     */

                    SequenceResult(

                        stage =
                            SequenceStage.S3,

                        confirmed =
                            false
                    )

                } else {

                    /*
                     * Perdeu confirmação.
                     *
                     * Reconstrói a sequência desde S1.
                     */

                    SequenceResult(

                        stage =
                            SequenceStage.S1,

                        confirmed =
                            false
                    )
                }
            }
        }
    }
}
