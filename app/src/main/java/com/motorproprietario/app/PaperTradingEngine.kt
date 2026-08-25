package com.motorproprietario.app

data class PaperTrade(
    val id: Long,
    val asset: String,
    val side: String,
    val entry: Double,
    val stop: Double,
    val target: Double,
    val quantity: Double,
    val openedAt: Long
)

data class PaperTradeResult(
    val accepted: Boolean,
    val reason: String,
    val trade: PaperTrade?
)

object PaperTradingEngine {

    fun open(
        asset: String,
        side: String,
        entry: Double,
        stop: Double,
        target: Double,
        quantity: Double,
        now: Long
    ): PaperTradeResult {

        if (asset.isBlank()) {
            return PaperTradeResult(
                false,
                "ASSET_INVALID",
                null
            )
        }

        if (side != "COMPRA" && side != "VENDA") {
            return PaperTradeResult(
                false,
                "SIDE_INVALID",
                null
            )
        }

        if (
            entry <= 0.0 ||
            stop <= 0.0 ||
            target <= 0.0 ||
            quantity <= 0.0
        ) {
            return PaperTradeResult(
                false,
                "PARAMETERS_INVALID",
                null
            )
        }

        val trade = PaperTrade(
            id = now,
            asset = asset,
            side = side,
            entry = entry,
            stop = stop,
            target = target,
            quantity = quantity,
            openedAt = now
        )

        return PaperTradeResult(
            true,
            "PAPER_TRADE_OPENED",
            trade
        )
    }
}
