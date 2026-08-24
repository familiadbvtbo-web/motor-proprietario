def win_rate(results):
    if not results:
        return 0.0

    wins = sum(1 for result in results if result > 0)
    return wins / len(results)


def expectancy(results):
    if not results:
        return 0.0

    return sum(results) / len(results)


def max_drawdown(results):
    equity = 0.0
    peak = 0.0
    drawdown = 0.0

    for result in results:
        equity += result
        peak = max(peak, equity)
        drawdown = max(drawdown, peak - equity)

    return drawdown
