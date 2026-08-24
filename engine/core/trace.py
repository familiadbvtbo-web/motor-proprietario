from dataclasses import dataclass
from typing import Any, Dict


@dataclass(frozen=True)
class DecisionTrace:
    decision_id: str
    asset: str
    timeframe: str
    decision: str
    score: float
    reason: str
    timestamp: int


def trace_dict(trace: DecisionTrace) -> Dict[str, Any]:
    return {
        "decision_id": trace.decision_id,
        "asset": trace.asset,
        "timeframe": trace.timeframe,
        "decision": trace.decision,
        "score": trace.score,
        "reason": trace.reason,
        "timestamp": trace.timestamp,
    }
