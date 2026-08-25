from dataclasses import dataclass
from typing import Optional

from mt5_gateway import MT5Gateway, MarketBar


@dataclass
class MarketSnapshot:
    asset: str
    timestamp: int
    price: float
    bid: float
    ask: float
    spread: float
    data_quality: str


@dataclass
class MultiTimeframeSnapshot:
    asset: str
    timestamp: int
    tick: MarketSnapshot
    bars: dict[str, list[MarketBar]]


class MarketBridge:

    def __init__(self):
        self.gateway = MT5Gateway()

    def connect(self) -> bool:
        return self.gateway.connect()

    def disconnect(self):
        self.gateway.disconnect()

    def list_assets(
        self,
        group: Optional[str] = None
    ) -> list[str]:

        return self.gateway.list_symbols(
            group=group
        )

    def select_asset(
        self,
        symbol: str
    ) -> bool:

        return self.gateway.select_symbol(
            symbol
        )

    def snapshot(
        self,
        symbol: Optional[str] = None
    ) -> MarketSnapshot:

        tick = self.gateway.get_tick(
            symbol
        )

        price = (
            tick.bid + tick.ask
        ) / 2.0

        quality = "GOOD"

        if (
            tick.bid <= 0.0 or
            tick.ask <= 0.0 or
            tick.ask < tick.bid
        ):
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

    def multi_timeframe_snapshot(
        self,
        symbol: Optional[str] = None,
        timeframes: Optional[list[str]] = None,
        count: int = 200
    ) -> MultiTimeframeSnapshot:

        tick = self.snapshot(
            symbol
        )

        bars = self.gateway.get_multi_timeframe(
            symbol=tick.asset,
            timeframes=timeframes,
            count=count
        )

        return MultiTimeframeSnapshot(
            asset=tick.asset,
            timestamp=tick.timestamp,
            tick=tick,
            bars=bars
        )

    def symbol_info(
        self,
        symbol: Optional[str] = None
    ):

        return self.gateway.get_symbol_info(
            symbol
        )
