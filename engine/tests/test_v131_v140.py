import sys
from pathlib import Path
sys.path.insert(0,str(Path(__file__).parents[2]))
from engine.core.live_state import LiveState, valid_live_state
from engine.core.scanner import rank_assets
from engine.core.alerts import alert_reason

def make(asset,mis,quality="GOOD",fsi=50,pfs=50,regime="UP"):
    return LiveState(asset,"15m",100,quality,fsi,pfs,mis,regime,20,"WATCH",1)

def test_state():
    assert valid_live_state(make("BTC",80))
    assert not valid_live_state(make("BTC",120))

def test_scanner():
    x=rank_assets([make("A",70),make("B",90),make("C",60,"BAD")])
    assert [a.asset for a in x]==["B","A"]

def test_alerts():
    a=make("A",60,fsi=40,pfs=40,regime="UP")
    b=make("A",72,fsi=60,pfs=70,regime="DOWN")
    assert set(alert_reason(a,b))=={"MIS_UP","FSI_SHIFT","PFS_SHIFT","REGIME_CHANGE"}

if __name__=="__main__":
    test_state();test_scanner();test_alerts()
    print("3/3 V131-V140 tests passed")
