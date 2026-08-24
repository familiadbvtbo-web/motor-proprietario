def portfolio_exposure(positions):
    return sum(abs(float(p.get("notional",0))) for p in positions)

def simulated_pnl(side, entry, current, quantity):
    if quantity < 0: raise ValueError("quantity")
    if side == "LONG": return (current-entry)*quantity
    if side == "SHORT": return (entry-current)*quantity
    raise ValueError("side")
