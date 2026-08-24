from dataclasses import dataclass
from typing import Literal

Side = Literal["LONG","SHORT"]

@dataclass(frozen=True)
class PaperTrade:
    trade_id: str
    asset: str
    timeframe: str
    side: Side
    entry: float
    stop: float
    target: float
    opened_at: int
    score: float

def mark_to_market(trade: PaperTrade, price: float) -> float:
    if trade.side == "LONG":
        return price - trade.entry
    return trade.entry - price

def risk_reward(trade: PaperTrade) -> float:
    risk = abs(trade.entry - trade.stop)
    reward = abs(trade.target - trade.entry)
    if risk == 0:
        raise ValueError("stop cannot equal entry")
    return reward / risk
