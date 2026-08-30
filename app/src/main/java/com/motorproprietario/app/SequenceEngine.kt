package com.motorproprietario.app

enum class SequenceStage {

    S0,
    S1,
    S2,
    S3,
    S4
}

data class SequenceInput(

    /*
     * Existe um sinal inicial válido?
     */
    val signalDetected: Boolean,

    /*
     * O sinal recebeu confirmação?
     */
    val confirmation: Boolean,

    /*
     * O movimento continuou depois da confirmação?
     */
    val continuation: Boolean,

    /*
     * Algum fator invalidou a sequência?
     */
    val invalidated: Boolean
)

data class SequenceResult(

    /*
     * Novo estágio da sequência.
     */
    val stage: SequenceStage,

    /*
     * Indica se a sequência está confirmada
     * para utilização pelo motor.
     */
    val confirmed: Boolean
)

object SequenceEngine {

    /*
     * ============================================================
     * AVANÇO DA SEQUÊNCIA
     * ============================================================
     *
     * S0 = nenhum sinal
     *
     * S1 = sinal detectado
     *
     * S2 = sinal confirmado
     *
     * S3 = continuação observada
     *
     * S4 = sequência totalmente confirmada
     *
     * A sequência nunca deve permanecer confirmada
     * quando o sinal atual deixou de existir ou
     * quando houve invalidação.
     */

    fun advance(
        current: SequenceStage,
        input: SequenceInput
    ): SequenceResult {

        /*
         * ========================================================
         * INVALIDAÇÃO GLOBAL
         * ========================================================
         *
         * Qualquer invalidação devolve imediatamente
         * o motor para S0.
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
         * AUSÊNCIA DO SINAL
         * ========================================================
         *
         * Se não existe mais sinal válido,
         * não podemos manter uma sequência antiga
         * como confirmada.
         */
        if (
            !input.signalDetected
        ) {

            return SequenceResult(

                stage =
                    SequenceStage.S0,

                confirmed =
                    false
            )
        }

        return when (
            current
        ) {

            /*
             * ====================================================
             * S0
             * ====================================================
             *
             * Primeiro evento:
             *
             * sinal detectado
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
             * ====================================================
             * S1
             * ====================================================
             *
             * O sinal existe.
             *
             * Agora aguardamos confirmação.
             *
             * S1 -> S2
             */
            SequenceStage.S1 -> {

                if (
                    input.confirmation
                ) {

                    SequenceResult(

                        stage =
                            SequenceStage.S2,

                        confirmed =
                            false
                    )

                } else {

                    /*
                     * Continua aguardando confirmação.
                     */
                    SequenceResult(

                        stage =
                            SequenceStage.S1,

                        confirmed =
                            false
                    )
                }
            }

            /*
             * ====================================================
             * S2
             * ====================================================
             *
             * Sinal confirmado.
             *
             * Agora precisamos observar continuação.
             *
             * S2 -> S3
             */
            SequenceStage.S2 -> {

                if (
                    input.continuation
                ) {

                    SequenceResult(

                        stage =
                            SequenceStage.S3,

                        confirmed =
                            false
                    )

                } else {

                    /*
                     * Continua aguardando continuação.
                     */
                    SequenceResult(

                        stage =
                            SequenceStage.S2,

                        confirmed =
                            false
                    )
                }
            }

            /*
             * ====================================================
             * S3
             * ====================================================
             *
             * Já houve continuação.
             *
             * Uma nova continuação confirma
             * definitivamente a sequência.
             *
             * S3 -> S4
             */
            SequenceStage.S3 -> {

                if (
                    input.continuation
                ) {

                    SequenceResult(

                        stage =
                            SequenceStage.S4,

                        confirmed =
                            true
                    )

                } else {

                    /*
                     * Ainda não houve confirmação final.
                     */
                    SequenceResult(

                        stage =
                            SequenceStage.S3,

                        confirmed =
                            false
                    )
                }
            }

            /*
             * ====================================================
             * S4
             * ====================================================
             *
             * A sequência chegou ao estágio final.
             *
             * Porém NÃO vamos simplesmente retornar
             * confirmed = true para sempre.
             *
             * O sinal precisa continuar existindo e
             * o evento atual precisa continuar confirmando
             * a sequência.
             */
            SequenceStage.S4 -> {

                if (
                    input.confirmation &&
                    input.continuation
                ) {

                    /*
                     * A confirmação continua válida.
                     */
                    SequenceResult(

                        stage =
                            SequenceStage.S4,

                        confirmed =
                            true
                    )

                } else {

                    /*
                     * Perdeu a confirmação atual.
                     *
                     * Não apagamos necessariamente toda a
                     * sequência imediatamente.
                     *
                     * Voltamos para S3 para exigir novamente
                     * a confirmação final.
                     */
                    SequenceResult(

                        stage =
                            SequenceStage.S3,

                        confirmed =
                            false
                    )
                }
            }
        }
    }
}
