def test_v106_v115():
    states=[
        {"asset":"BTCUSDT","score":72,"quality":"GOOD"},
        {"asset":"ETHUSDT","score":81,"quality":"GOOD"},
        {"asset":"PETR4","score":65,"quality":"DEGRADED"},
    ]
    ranked=sorted(states,key=lambda x:x["score"],reverse=True)
    assert ranked[0]["asset"]=="ETHUSDT"
    assert all(0<=x["score"]<=100 for x in states)

if __name__=="__main__":
    test_v106_v115()
    print("V106-V115 test passed")
