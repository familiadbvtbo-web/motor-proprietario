from dataclasses import dataclass

from mt5_gateway import MT5Gateway


@dataclass
class MarketSnapshot:
    asset: str
    timestamp: int
    price: float
    bid: float
    ask: float
    spread: float
    data_quality: str


class MarketBridge:

    def __init__(self, symbol: str):

        self.gateway = MT5Gateway(symbol)

    def connect(self) -> bool:

        return self.gateway.connect()

    def disconnect(self):

        self.gateway.disconnect()

    def snapshot(self) -> MarketSnapshot:

        tick = self.gateway.get_tick()

        price = (
            tick.bid + tick.ask
        ) / 2.0

        quality = "GOOD"

        if tick.bid <= 0 or tick.ask <= 0:
            quality = "BAD"

        return MarketSnapshot(
            asset=tick.symbol,
            timestamp=tick.timestamp,
            price=price,
            bid=tick.bid,
            ask=tick.ask,
            spread=tick.spread,
            data_quality=quality
        )
