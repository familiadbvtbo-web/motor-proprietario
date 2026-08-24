def alert_reason(previous, current):
    reasons=[]
    if current.mis-previous.mis >= 10: reasons.append("MIS_UP")
    if current.fsi-previous.fsi >= 15: reasons.append("FSI_SHIFT")
    if current.pfs-previous.pfs >= 15: reasons.append("PFS_SHIFT")
    if current.regime != previous.regime: reasons.append("REGIME_CHANGE")
    return reasons
