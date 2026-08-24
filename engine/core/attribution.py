def score_bucket(score):
    if score >= 80:
        return "80-100"
    if score >= 65:
        return "65-79"
    if score >= 50:
        return "50-64"
    return "0-49"


def bucket_results(scores, results):
    if len(scores) != len(results) or not scores:
        raise ValueError("scores/results mismatch")

    out = {}

    for s, r in zip(scores, results):
        out.setdefault(score_bucket(s), []).append(r)

    return {
        k: {
            "n": len(v),
            "expectancy": sum(v) / len(v)
        }
        for k, v in out.items()
    }
