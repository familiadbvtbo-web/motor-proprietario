from dataclasses import dataclass


@dataclass(frozen=True)
class DeviceGate:
    device_id: str
    owner_id: str
    authorized: bool
    mode: str = "OWNER"


def validate_device(gate: DeviceGate) -> bool:
    if not gate.device_id:
        return False

    if not gate.owner_id:
        return False

    if gate.mode not in {"OWNER", "PAPER", "READ_ONLY"}:
        return False

    return gate.authorized
