from dataclasses import dataclass
from typing import Optional

from market_bridge import MarketBridge, MultiTimeframeSnapshot


@dataclass
class TimeframeAnalysis:
    timeframe: str
    price: float
    trend: float
    momentum: float
    volatility: float
    volume: float


@dataclass
class MarketAnalysis:
    asset: str
    timestamp: int
    price: float
    bid: float
    ask: float
    spread: float

    structure: float
    trend: float
    momentum: float
    volume: float
    volatility: float

    multi_timeframe: float
    data_quality: str

    timeframes: dict[str, TimeframeAnalysis]


class MarketAnalyzer:

    def __init__(self, bridge: MarketBridge):
        self.bridge = bridge

    @staticmethod
    def _clamp(value: float) -> float:
        return max(0.0, min(100.0, value))

    @staticmethod
    def _trend_score(
        closes: list[float]
    ) -> float:

        if len(closes) < 10:
            return 0.0

        start = closes[-10]
        end = closes[-1]

        if start <= 0:
            return 0.0

        change = (
            (end - start) / start
        ) * 100.0

        return MarketAnalyzer._clamp(
            50.0 + change * 10.0
        )

    @staticmethod
    def _momentum_score(
        closes: list[float]
    ) -> float:

        if len(closes) < 5:
            return 0.0

        previous = closes[-5]
        current = closes[-1]

        if previous <= 0:
            return 0.0

        change = (
            (current - previous) /
            previous
        ) * 100.0

        return MarketAnalyzer._clamp(
            50.0 + change * 15.0
        )

    @staticmethod
    def _volatility_score(
        closes: list[float]
    ) -> float:

        if len(closes) < 10:
            return 0.0

        returns = []

        for i in range(1, len(closes)):
            previous = closes[i - 1]

            if previous <= 0:
                continue

            returns.append(
                abs(
                    (closes[i] - previous) /
                    previous
                ) * 100.0
            )

        if not returns:
            return 0.0

        average = sum(returns) / len(returns)

        return MarketAnalyzer._clamp(
            average * 20.0
        )

    @staticmethod
    def _volume_score(
        volumes: list[float]
    ) -> float:

        if len(volumes) < 10:
            return 0.0

        recent = volumes[-5:]
        previous = volumes[-10:-5]

        recent_avg = (
            sum(recent) /
            len(recent)
        )

        previous_avg = (
            sum(previous) /
            len(previous)
        )

        if previous_avg <= 0:
            return 0.0

        ratio = (
            recent_avg /
            previous_avg
        )

        return MarketAnalyzer._clamp(
            50.0 + (ratio - 1.0) * 50.0
        )

    def analyze(
        self,
        symbol: str,
        timeframes: Optional[list[str]] = None,
        count: int = 200
    ) -> MarketAnalysis:

        snapshot: MultiTimeframeSnapshot = (
            self.bridge.multi_timeframe_snapshot(
                symbol=symbol,
                timeframes=timeframes,
                count=count
            )
        )

        analyses = {}

        for timeframe, bars in snapshot.bars.items():

            closes = [
                bar.close
                for bar in bars
            ]

            volumes = [
                bar.tick_volume
                for bar in bars
            ]

            if not closes:
                continue

            price = closes[-1]

            trend = self._trend_score(
                closes
            )

            momentum = self._momentum_score(
                closes
            )

            volatility = self._volatility_score(
                closes
            )

            volume = self._volume_score(
                volumes
            )

            analyses[timeframe] = TimeframeAnalysis(
                timeframe=timeframe,
                price=price,
                trend=trend,
                momentum=momentum,
                volatility=volatility,
                volume=volume
            )

        if not analyses:
            return MarketAnalysis(
                asset=snapshot.asset,
                timestamp=snapshot.timestamp,
                price=snapshot.tick.price,
                bid=snapshot.tick.bid,
                ask=snapshot.tick.ask,
                spread=snapshot.tick.spread,
                structure=0.0,
                trend=0.0,
                momentum=0.0,
                volume=0.0,
                volatility=0.0,
                multi_timeframe=0.0,
                data_quality="BAD",
                timeframes={}
            )

        values = list(
            analyses.values()
        )

        trend = sum(
            item.trend
            for item in values
        ) / len(values)

        momentum = sum(
            item.momentum
            for item in values
        ) / len(values)

        volume = sum(
            item.volume
            for item in values
        ) / len(values)

        volatility = sum(
            item.volatility
            for item in values
        ) / len(values)

        multi_timeframe = (
            sum(
                item.trend
                for item in values
            ) / len(values)
        )

        structure = self._clamp(
            (
                trend +
                momentum +
                multi_timeframe
            ) / 3.0
        )

        return MarketAnalysis(
            asset=snapshot.asset,
            timestamp=snapshot.timestamp,
            price=snapshot.tick.price,
            bid=snapshot.tick.bid,
            ask=snapshot.tick.ask,
            spread=snapshot.tick.spread,
            structure=structure,
            trend=self._clamp(trend),
            momentum=self._clamp(momentum),
            volume=self._clamp(volume),
            volatility=self._clamp(volatility),
            multi_timeframe=self._clamp(
                multi_timeframe
            ),
            data_quality=(
                "GOOD"
                if snapshot.tick.data_quality == "GOOD"
                else "BAD"
            ),
            timeframes=analyses
        )
