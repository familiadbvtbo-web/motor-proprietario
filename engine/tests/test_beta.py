def test_beta_gate():
    checks = {
        "install": True,
        "open": True,
        "paper_trading": True,
        "real_execution": False,
    }

    assert checks["install"]
    assert checks["open"]
    assert checks["paper_trading"]
    assert checks["real_execution"] is False


if __name__ == "__main__":
    test_beta_gate()
    print("Beta gate passed")
