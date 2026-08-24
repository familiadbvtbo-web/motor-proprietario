def device_gate(checks):
    if not checks:
        return "BLOCKED"
    if any(v is False for v in checks.values()):
        return "BLOCKED"
    return "PASSED"
