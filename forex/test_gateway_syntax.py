import py_compile
from pathlib import Path


BASE_DIR = Path(__file__).resolve().parent

FILES = [
    BASE_DIR / "config.py",
    BASE_DIR / "mt5_gateway.py",
    BASE_DIR / "market_bridge.py",
    BASE_DIR / "forex_runner.py",
    BASE_DIR / "forex_api.py",
]


def main():
    for file_path in FILES:
        if not file_path.exists():
            raise FileNotFoundError(
                f"Arquivo não encontrado: {file_path}"
            )

        py_compile.compile(
            str(file_path),
            doraise=True
        )

        print(f"OK: {file_path.name}")

    print("FOREX PYTHON SYNTAX: OK")


if __name__ == "__main__":
    main()
