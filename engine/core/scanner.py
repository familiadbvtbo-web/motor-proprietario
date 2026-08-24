def rank_assets(states):
    return sorted(
        [s for s in states if s.data_quality != "BAD"],
        key=lambda s: s.mis,
        reverse=True
    )
