from dataclasses import dataclass


@dataclass(frozen=True)
class Alert:
    alert_id: str
    asset: str
    timeframe: str
    level: str
    message: str
    timestamp: int


def valid_alert(alert: Alert) -> bool:
    return bool(
        alert.alert_id
        and alert.asset
        and alert.timeframe
        and alert.level
        and alert.message
        and alert.timestamp > 0
    )
