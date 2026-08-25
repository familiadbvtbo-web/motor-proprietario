import time

from config import (
    SYMBOL,
    POLL_INTERVAL_SECONDS,
    PAPER_MODE
)

from market_bridge import MarketBridge


def main():

    bridge = MarketBridge(SYMBOL)

    if not bridge.connect():

        raise RuntimeError(
            "FOREX_GATEWAY_CONNECTION_FAILED"
        )

    print(
        f"FOREX GATEWAY ONLINE | "
        f"{SYMBOL} | "
        f"PAPER={PAPER_MODE}"
    )

    try:

        while True:

            snapshot = bridge.snapshot()

            print(
                f"{snapshot.asset} | "
                f"PRICE={snapshot.price} | "
                f"BID={snapshot.bid} | "
                f"ASK={snapshot.ask} | "
                f"SPREAD={snapshot.spread} | "
                f"QUALITY={snapshot.data_quality}"
            )

            time.sleep(
                POLL_INTERVAL_SECONDS
            )

    except KeyboardInterrupt:

        print(
            "FOREX GATEWAY STOPPED"
        )

    finally:

        bridge.disconnect()


if __name__ == "__main__":
    main()
