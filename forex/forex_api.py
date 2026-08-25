import json
import os
from http.server import BaseHTTPRequestHandler, HTTPServer
from urllib.parse import parse_qs, urlparse

from mt5_gateway import MT5Gateway
from market_analyzer import MarketAnalyzer


HOST = "0.0.0.0"
PORT = 8080

DEFAULT_TIMEFRAMES = [
    "M1",
    "M5",
    "M15",
    "M30",
    "H1",
    "H4",
    "D1"
]

gateway = MT5Gateway()
analyzer = MarketAnalyzer(
    gateway
)

API_TOKEN = os.getenv(
    "FOREX_API_TOKEN",
    ""
)


def authorized(handler):

    if not API_TOKEN:
        return True

    return (
        handler.headers.get(
            "X-API-Token",
            ""
        ) == API_TOKEN
    )


def serialize_analysis(analysis):

    timeframes = {}

    for name, item in analysis.timeframes.items():

        timeframes[name] = {
            "timeframe": item.timeframe,
            "price": item.price,
            "trend": item.trend,
            "momentum": item.momentum,
            "volatility": item.volatility,
            "volume": item.volume
        }

    return {
        "asset": analysis.asset,
        "timestamp": analysis.timestamp,
        "price": analysis.price,
        "bid": analysis.bid,
        "ask": analysis.ask,
        "spread": analysis.spread,
        "structure": analysis.structure,
        "trend": analysis.trend,
        "momentum": analysis.momentum,
        "volume": analysis.volume,
        "volatility": analysis.volatility,
        "multi_timeframe": analysis.multi_timeframe,
        "data_quality": analysis.data_quality,
        "timeframes": timeframes
    }


class ForexApiHandler(
    BaseHTTPRequestHandler
):

    def send_json(
        self,
        status,
        payload
    ):

        body = json.dumps(
            payload,
            ensure_ascii=False
        ).encode("utf-8")

        self.send_response(status)

        self.send_header(
            "Content-Type",
            "application/json; charset=utf-8"
        )

        self.send_header(
            "Content-Length",
            str(len(body))
        )

        self.end_headers()

        self.wfile.write(body)

    def do_GET(self):

        if not authorized(self):

            self.send_json(
                401,
                {
                    "ok": False,
                    "error": "UNAUTHORIZED"
                }
            )

            return

        parsed = urlparse(
            self.path
        )

        path = parsed.path

        params = parse_qs(
            parsed.query
        )

        try:

            if path == "/health":

                self.send_json(
                    200,
                    {
                        "ok": True,
                        "service": "FOREX_GATEWAY",
                        "mt5": True,
                        "paper_mode": True,
                        "live_trading": False
                    }
                )

                return

            if path == "/assets":

                group = params.get(
                    "group",
                    [None]
                )[0]

                assets = gateway.list_symbols(
                    group=group
                )

                self.send_json(
                    200,
                    {
                        "ok": True,
                        "count": len(assets),
                        "assets": assets
                    }
                )

                return

            if path == "/tick":

                symbol = params.get(
                    "symbol",
                    [None]
                )[0]

                tick = gateway.get_tick(
                    symbol
                )

                self.send_json(
                    200,
                    {
                        "ok": True,
                        "symbol": tick.symbol,
                        "timestamp": tick.timestamp,
                        "bid": tick.bid,
                        "ask": tick.ask,
                        "spread": tick.spread,
                        "price": (
                            tick.bid + tick.ask
                        ) / 2.0,
                        "data_quality": "GOOD"
                    }
                )

                return

            if path == "/analysis":

                symbol = params.get(
                    "symbol",
                    [None]
                )[0]

                if not symbol:

                    self.send_json(
                        400,
                        {
                            "ok": False,
                            "error": "SYMBOL_REQUIRED"
                        }
                    )

                    return

                timeframes = params.get(
                    "timeframes",
                    [",".join(
                        DEFAULT_TIMEFRAMES
                    )]
                )[0]

                selected_timeframes = [
                    item.strip().upper()
                    for item in timeframes.split(",")
                    if item.strip()
                ]

                count = int(
                    params.get(
                        "count",
                        ["200"]
                    )[0]
                )

                analysis = analyzer.analyze(
                    symbol=symbol,
                    timeframes=selected_timeframes,
                    count=count
                )

                self.send_json(
                    200,
                    {
                        "ok": True,
                        "analysis":
                            serialize_analysis(
                                analysis
                            )
                    }
                )

                return

            self.send_json(
                404,
                {
                    "ok": False,
                    "error": "NOT_FOUND"
                }
            )

        except Exception as error:

            self.send_json(
                503,
                {
                    "ok": False,
                    "error": str(error)
                }
            )

    def log_message(
        self,
        format,
        *args
    ):

        print(
            f"[FOREX_API] {format % args}"
        )


def main():

    if not gateway.connect():

        raise RuntimeError(
            "MT5_CONNECTION_FAILED"
        )

    server = HTTPServer(
        (HOST, PORT),
        ForexApiHandler
    )

    print(
        f"FOREX API ONLINE | "
        f"{HOST}:{PORT}"
    )

    print(
        "PAPER MODE = TRUE"
    )

    print(
        "LIVE TRADING = FALSE"
    )

    try:

        server.serve_forever()

    except KeyboardInterrupt:

        pass

    finally:

        server.server_close()
        gateway.disconnect()


if __name__ == "__main__":
    main()
