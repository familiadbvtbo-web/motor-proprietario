def build_identity(version, commit="unknown", apk_sha256=None):
    if not version:
        raise ValueError("version required")
    return {
        "version": version,
        "commit": commit,
        "apk_sha256": apk_sha256,
        "real_execution": False
    }
