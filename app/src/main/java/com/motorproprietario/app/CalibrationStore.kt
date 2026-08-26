package com.motorproprietario.app

import android.content.Context
import org.json.JSONObject

class CalibrationStore(
    context: Context
) {

    private val preferences =
        context.getSharedPreferences(
            PREFS_NAME,
            Context.MODE_PRIVATE
        )

    fun save(
        state: CalibrationState
    ) {

        val normalized =
            state.normalized()

        val json =
            JSONObject().apply {

                put(
                    "version",
                    normalized.version
                )

                put(
                    "accepted",
                    normalized.accepted
                )

                put(
                    "probabilityWeight",
                    normalized.probabilityWeight
                )

                put(
                    "deterministicWeight",
                    normalized.deterministicWeight
                )

                put(
                    "candidatesEvaluated",
                    normalized.candidatesEvaluated
                )

                put(
                    "reason",
                    normalized.reason
                )

                put(
                    "calibratedAt",
                    normalized.calibratedAt
                )
            }

        preferences
            .edit()
            .putString(
                KEY_STATE,
                json.toString()
            )
            .apply()
    }

    fun load():
        CalibrationState {

        val raw =
            preferences.getString(
                KEY_STATE,
                null
            )
                ?: return CalibrationState.initial()

        return try {

            val json =
                JSONObject(raw)

            CalibrationState(

                version =
                    json.optInt(
                        "version",
                        1
                    ),

                accepted =
                    json.optBoolean(
                        "accepted",
                        false
                    ),

                probabilityWeight =
                    json.optDouble(
                        "probabilityWeight",
                        0.50
                    ),

                deterministicWeight =
                    json.optDouble(
                        "deterministicWeight",
                        0.50
                    ),

                candidatesEvaluated =
                    json.optInt(
                        "candidatesEvaluated",
                        0
                    ),

                reason =
                    json.optString(
                        "reason",
                        "CALIBRACAO_NAO_EXECUTADA"
                    ),

                calibratedAt =
                    json.optLong(
                        "calibratedAt",
                        0L
                    )

            ).normalized()

        } catch (
            error: Exception
        ) {

            CalibrationState.initial()
        }
    }

    fun saveResult(
        result:
            CalibrationResult
    ):
        CalibrationState {

        val state =
            CalibrationState
                .fromResult(
                    result
                )

        save(state)

        return state
    }

    fun clear() {

        preferences
            .edit()
            .remove(
                KEY_STATE
            )
            .apply()
    }

    companion object {

        private const val
            PREFS_NAME =
            "motor_proprietario_calibration"

        private const val
            KEY_STATE =
            "calibration_state"
    }
}
