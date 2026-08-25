import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).parents[2]))

from engine.core.live_state import LiveState, valid_live_state
from engine.core.scanner import rank_assets


def test_live_state():
    state = LiveState(
        "BTCUSDT",
        "15m",
        100.0,
        "GOOD",
        80.0,
        70.0,
        75.0,
        "UP",
        20.0,
        "WATCH",
        1
    )

    assert valid_live_state(state)


def test_rank_assets():
    states = [
        LiveState("BTCUSDT", "15m", 100.0, "GOOD", 70, 60, 65, "UP", 20, "WATCH", 1),
        LiveState("ETHUSDT", "15m", 100.0, "GOOD", 80, 70, 85, "UP", 20, "WATCH", 1),
    ]

    ranked = rank_assets(states)

    assert ranked[0].asset == "ETHUSDT"


if __name__ == "__main__":
    test_live_state()
    test_rank_assets()
    print("V121-V130 tests passed")
