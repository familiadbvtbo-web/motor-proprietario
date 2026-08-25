import time
from dataclasses import dataclass
from typing import Optional

import MetaTrader5 as mt5


@dataclass
class ForexTick:
    symbol: str
    timestamp: int
    bid: float
    ask: float
    spread: float


@dataclass
class MarketBar:
    symbol: str
    timeframe: str
    timestamp: int
    open: float
    high: float
    low: float
    close: float
    tick_volume: float
    spread: float
    real_volume: float


class MT5Gateway:

    TIMEFRAMES = {
        "M1": mt5.TIMEFRAME_M1,
        "M2": mt5.TIMEFRAME_M2,
        "M3": mt5.TIMEFRAME_M3,
        "M5": mt5.TIMEFRAME_M5,
        "M10": mt5.TIMEFRAME_M10,
        "M15": mt5.TIMEFRAME_M15,
        "M30": mt5.TIMEFRAME_M30,
        "H1": mt5.TIMEFRAME_H1,
        "H2": mt5.TIMEFRAME_H2,
        "H4": mt5.TIMEFRAME_H4,
        "H6": mt5.TIMEFRAME_H6,
        "H8": mt5.TIMEFRAME_H8,
        "H12": mt5.TIMEFRAME_H12,
        "D1": mt5.TIMEFRAME_D1,
        "W1": mt5.TIMEFRAME_W1,
        "MN1": mt5.TIMEFRAME_MN1,
    }

    def __init__(self, symbol: Optional[str] = None):
        self.symbol = symbol

    def connect(self) -> bool:

        if not mt5.initialize():
            return False

        return True

    def disconnect(self):
        mt5.shutdown()

    def list_symbols(
        self,
        group: Optional[str] = None
    ) -> list[str]:

        symbols = (
            mt5.symbols_get(group=group)
            if group
            else mt5.symbols_get()
        )

        if symbols is None:
            error = mt5.last_error()

            raise RuntimeError(
                f"MT5_SYMBOLS_UNAVAILABLE: {error}"
            )

        return [
            symbol.name
            for symbol in symbols
        ]

    def select_symbol(
        self,
        symbol: str
    ) -> bool:

        if not symbol:
            raise ValueError(
                "SYMBOL_EMPTY"
            )

        selected = mt5.symbol_select(
            symbol,
            True
        )

        if not selected:
            return False

        self.symbol = symbol

        return True

    def get_tick(
        self,
        symbol: Optional[str] = None
    ) -> ForexTick:

        selected_symbol = (
            symbol or self.symbol
        )

        if not selected_symbol:
            raise ValueError(
                "SYMBOL_NOT_SELECTED"
            )

        tick = mt5.symbol_info_tick(
            selected_symbol
        )

        if tick is None:
            error = mt5.last_error()

            raise RuntimeError(
                f"MT5_TICK_UNAVAILABLE: {error}"
            )

        bid = float(tick.bid)
        ask = float(tick.ask)

        if bid <= 0.0 or ask <= 0.0:
            raise RuntimeError(
                "MT5_INVALID_TICK"
            )

        timestamp = getattr(
            tick,
            "time_msc",
            0
        )

        if timestamp <= 0:
            timestamp = int(
                tick.time * 1000
            )

        return ForexTick(
            symbol=selected_symbol,
            timestamp=int(timestamp),
            bid=bid,
            ask=ask,
            spread=ask - bid
        )

    def get_bars(
        self,
        symbol: Optional[str] = None,
        timeframe: str = "M1",
        count: int = 200
    ) -> list[MarketBar]:

        selected_symbol = (
            symbol or self.symbol
        )

        if not selected_symbol:
            raise ValueError(
                "SYMBOL_NOT_SELECTED"
            )

        if timeframe not in self.TIMEFRAMES:
            raise ValueError(
                f"TIMEFRAME_UNSUPPORTED: {timeframe}"
            )

        if count <= 0:
            raise ValueError(
                "BAR_COUNT_INVALID"
            )

        rates = mt5.copy_rates_from_pos(
            selected_symbol,
            self.TIMEFRAMES[timeframe],
            0,
            count
        )

        if rates is None:
            error = mt5.last_error()

            raise RuntimeError(
                f"MT5_BARS_UNAVAILABLE: {error}"
            )

        bars = []

        for rate in rates:

            bars.append(
                MarketBar(
                    symbol=selected_symbol,
                    timeframe=timeframe,
                    timestamp=int(
                        rate["time"] * 1000
                    ),
                    open=float(rate["open"]),
                    high=float(rate["high"]),
                    low=float(rate["low"]),
                    close=float(rate["close"]),
                    tick_volume=float(
                        rate["tick_volume"]
                    ),
                    spread=float(
                        rate["spread"]
                    ),
                    real_volume=float(
                        rate["real_volume"]
                    )
                )
            )

        return bars

    def get_multi_timeframe(
        self,
        symbol: Optional[str] = None,
        timeframes: Optional[list[str]] = None,
        count: int = 200
    ) -> dict[str, list[MarketBar]]:

        selected_symbol = (
            symbol or self.symbol
        )

        if not selected_symbol:
            raise ValueError(
                "SYMBOL_NOT_SELECTED"
            )

        selected_timeframes = (
            timeframes
            if timeframes
            else [
                "M1",
                "M5",
                "M15",
                "M30",
                "H1",
                "H4",
                "D1"
            ]
        )

        result = {}

        for timeframe in selected_timeframes:

            result[timeframe] = self.get_bars(
                symbol=selected_symbol,
                timeframe=timeframe,
                count=count
            )

        return result

    def get_symbol_info(
        self,
        symbol: Optional[str] = None
    ):

        selected_symbol = (
            symbol or self.symbol
        )

        if not selected_symbol:
            raise ValueError(
                "SYMBOL_NOT_SELECTED"
            )

        info = mt5.symbol_info(
            selected_symbol
        )

        if info is None:
            error = mt5.last_error()

            raise RuntimeError(
                f"MT5_SYMBOL_INFO_UNAVAILABLE: {error}"
            )

        return info
