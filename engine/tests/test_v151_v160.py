import sys
from pathlib import Path
sys.path.insert(0,str(Path(__file__).parents[2]))
from engine.core.build_info import build_identity
from engine.core.device_gate import device_gate

def test_identity():
    x=build_identity("V160","abc","123")
    assert x["version"]=="V160"
    assert x["real_execution"] is False

def test_gate():
    assert device_gate({"install":True,"open":True,"paper":True})=="PASSED"
    assert device_gate({"install":True,"open":False})=="BLOCKED"

if __name__=="__main__":
    test_identity();test_gate()
    print("2/2 V151-V160 tests passed")
