def public_trace(score, regime, risk, data_quality, factors):
    return {
        "score": score,
        "regime": regime,
        "risk": risk,
        "data_quality": data_quality,
        "factors": sorted(factors, key=lambda x:x[1], reverse=True)[:3],
        "private_formula_exposed": False
    }
