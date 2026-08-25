def expectancy(results):
    if not results:
        raise ValueError("results cannot be empty")
    return sum(results)/len(results)

def max_drawdown(equity):
    if not equity:
        raise ValueError("equity cannot be empty")
    peak=equity[0]
    dd=0.0
    for x in equity:
        peak=max(peak,x)
        dd=max(dd,peak-x)
    return dd

def win_rate(results):
    if not results:
        raise ValueError("results cannot be empty")
    return sum(1 for x in results if x>0)/len(results)
