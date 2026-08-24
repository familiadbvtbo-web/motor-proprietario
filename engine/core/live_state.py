from dataclasses import dataclass

@dataclass(frozen=True)
class LiveState:
    asset: str
    timeframe: str
    price: float
    data_quality: str
    fsi: float
    pfs: float
    mis: float
    regime: str
    anti_trap: float
    decision: str
    timestamp: int

def valid_live_state(s: LiveState):
    if not s.asset or not s.timeframe: return False
    if s.data_quality not in {"GOOD","DEGRADED","BAD"}: return False
    for x in (s.fsi,s.pfs,s.mis,s.anti_trap):
        if not 0 <= x <= 100: return False
    return True
