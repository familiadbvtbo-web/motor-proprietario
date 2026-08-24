from dataclasses import dataclass


@dataclass(frozen=True)
class ScanResult:
    asset: str
    timeframe: str
    score: float
    decision: str


def rank(results):
    return sorted(
        results,
        key=lambda item: item.score,
        reverse=True
    )
