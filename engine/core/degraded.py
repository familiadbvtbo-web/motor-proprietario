def safe_mode(data_quality, connected):
    if not connected:
        return "OFFLINE"
    if data_quality != "GOOD":
        return "DEGRADED"
    return "LIVE"
