import time
from dataclasses import dataclass

import MetaTrader5 as mt5


@dataclass
class ForexTick:
    symbol: str
    timestamp: int
    bid: float
    ask: float
    spread: float


class MT5Gateway:

    def __init__(self, symbol: str = "EURUSD"):
        self.symbol = symbol

    def connect(self) -> bool:

        if not mt5.initialize():
            return False

        if not mt5.symbol_select(self.symbol, True):
            mt5.shutdown()
            return False

        return True

    def disconnect(self):
        mt5.shutdown()

    def get_tick(self) -> ForexTick:

        tick = mt5.symbol_info_tick(self.symbol)

        if tick is None:
            error = mt5.last_error()
            raise RuntimeError(
                f"MT5_TICK_UNAVAILABLE: {error}"
            )

        bid = float(tick.bid)
        ask = float(tick.ask)

        if bid <= 0 or ask <= 0:
            raise RuntimeError(
                "MT5_INVALID_TICK"
            )

        return ForexTick(
            symbol=self.symbol,
            timestamp=int(time.time() * 1000),
            bid=bid,
            ask=ask,
            spread=ask - bid
        )
