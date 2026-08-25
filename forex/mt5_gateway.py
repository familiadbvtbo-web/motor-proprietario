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

        selected = mt5.symbol_select(
            self.symbol,
            True
        )

        return bool(selected)

    def disconnect(self):
        mt5.shutdown()

    def get_tick(self) -> ForexTick:

        tick = mt5.symbol_info_tick(
            self.symbol
        )

        if tick is None:
            raise RuntimeError(
                "MT5_TICK_UNAVAILABLE"
            )

        bid = float(tick.bid)
        ask = float(tick.ask)

        return ForexTick(
            symbol=self.symbol,
            timestamp=int(time.time() * 1000),
            bid=bid,
            ask=ask,
            spread=ask - bid
        )


def main():

    gateway = MT5Gateway("EURUSD")

    if not gateway.connect():
        raise RuntimeError(
            "MT5_CONNECTION_FAILED"
        )

    try:

        tick = gateway.get_tick()

        print(
            f"{tick.symbol} "
            f"BID={tick.bid} "
            f"ASK={tick.ask} "
            f"SPREAD={tick.spread}"
        )

    finally:

        gateway.disconnect()


if __name__ == "__main__":
    main()
