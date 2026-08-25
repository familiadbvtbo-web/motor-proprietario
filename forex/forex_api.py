import json
import os
from http.server import BaseHTTPRequestHandler, HTTPServer

from mt5_gateway import MT5Gateway


HOST = "0.0.0.0"
PORT = 8080

SYMBOL = os.getenv("FOREX_SYMBOL", "EURUSD")

API_TOKEN = os.getenv("FOREX_API_TOKEN", "")

gateway = MT5Gateway(SYMBOL)


class ForexApiHandler(BaseHTTPRequestHandler):

    def _send_json(self, status, payload):

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

    def _authorized(self):

        if not API_TOKEN:
            return True

        received = self.headers.get(
            "X-API-Token",
            ""
        )

        return received == API_TOKEN

    def do_GET(self):

        if not self._authorized():

            self._send_json(
                401,
                {
                    "ok": False,
                    "error": "UNAUTHORIZED"
                }
            )

            return

        if self.path == "/health":

            self._send_json(
                200,
                {
                    "ok": True,
                    "service": "FOREX_GATEWAY",
                    "symbol": SYMBOL,
                    "paper_mode": True,
                    "live_trading": False
                }
            )

            return

        if self.path == "/tick":

            try:

                tick = gateway.get_tick()

                self._send_json(
                    200,
                    {
                        "ok": True,
                        "symbol": tick.symbol,
                        "timestamp": tick.timestamp,
                        "bid": tick.bid,
                        "ask": tick.ask,
                        "spread": tick.spread,
                        "data_quality": "GOOD"
                    }
                )

            except Exception as error:

                self._send_json(
                    503,
                    {
                        "ok": False,
                        "error": str(error)
                    }
                )

            return

        self._send_json(
            404,
            {
                "ok": False,
                "error": "NOT_FOUND"
            }
        )

    def log_message(self, format, *args):
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
        f"{HOST}:{PORT} | "
        f"{SYMBOL} | "
        f"PAPER=True | "
        f"LIVE=False"
    )

    try:

        server.serve_forever()

    except KeyboardInterrupt:

        print(
            "FOREX API STOPPED"
        )

    finally:

        server.server_close()
        gateway.disconnect()


if __name__ == "__main__":
    main()
