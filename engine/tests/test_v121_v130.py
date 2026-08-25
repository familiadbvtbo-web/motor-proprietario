import sys
from pathlib import Path
sys.path.insert(0,str(Path(__file__).parents[2]))
from engine.core.degraded import safe_mode
from engine.core.portfolio import portfolio_exposure, simulated_pnl
from engine.core.trace import public_trace

def test_modes():
    assert safe_mode("GOOD",True)=="LIVE"
    assert safe_mode("BAD",True)=="DEGRADED"
    assert safe_mode("GOOD",False)=="OFFLINE"

def test_portfolio():
    assert portfolio_exposure([{"notional":100},{"notional":50}])==150
    assert simulated_pnl("LONG",100,110,2)==20

def test_trace():
    x=public_trace(82,"EXPANSION","MEDIUM","GOOD",[("FSI",90),("trend",80),("volume",70),("secret",100)])
    assert x["private_formula_exposed"] is False
    assert len(x["factors"])==3

if __name__=="__main__":
    test_modes();test_portfolio();test_trace()
    print("3/3 V121-V130 tests passed")
