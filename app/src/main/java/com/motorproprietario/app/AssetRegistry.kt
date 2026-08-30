package com.motorproprietario.app

data class MarketAsset(
    val id: String,
    val displayName: String,
    val category: AssetCategory,
    val twelveDataSymbol: String,
    val alphaVantageSymbol: String? = null
)

enum class AssetCategory { FOREX, CRYPTO, INDEX, METAL, ENERGY, STOCK, ETF }

object AssetRegistry {
    private val assets = listOf(
        MarketAsset("EUR/USD","Euro / Dólar",AssetCategory.FOREX,"EUR/USD"),
        MarketAsset("GBP/USD","Libra / Dólar",AssetCategory.FOREX,"GBP/USD"),
        MarketAsset("USD/JPY","Dólar / Iene",AssetCategory.FOREX,"USD/JPY"),
        MarketAsset("USD/CHF","Dólar / Franco",AssetCategory.FOREX,"USD/CHF"),
        MarketAsset("AUD/USD","Dólar Australiano / Dólar",AssetCategory.FOREX,"AUD/USD"),
        MarketAsset("USD/CAD","Dólar / Dólar Canadense",AssetCategory.FOREX,"USD/CAD"),
        MarketAsset("NZD/USD","Dólar Neozelandês / Dólar",AssetCategory.FOREX,"NZD/USD"),
        MarketAsset("EUR/GBP","Euro / Libra",AssetCategory.FOREX,"EUR/GBP"),
        MarketAsset("EUR/JPY","Euro / Iene",AssetCategory.FOREX,"EUR/JPY"),
        MarketAsset("GBP/JPY","Libra / Iene",AssetCategory.FOREX,"GBP/JPY"),
        MarketAsset("BTC/USD","Bitcoin / Dólar",AssetCategory.CRYPTO,"BTC/USD"),
        MarketAsset("ETH/USD","Ethereum / Dólar",AssetCategory.CRYPTO,"ETH/USD"),
        MarketAsset("XRP/USD","XRP / Dólar",AssetCategory.CRYPTO,"XRP/USD"),
        MarketAsset("XAU/USD","Ouro Spot",AssetCategory.METAL,"XAU/USD","XAU"),
        MarketAsset("XAG/USD","Prata Spot",AssetCategory.METAL,"XAG/USD","XAG"),
        MarketAsset("US100","Nasdaq 100",AssetCategory.INDEX,"US100"),
        MarketAsset("US500","S&P 500",AssetCategory.INDEX,"US500"),
        MarketAsset("US30","Dow Jones",AssetCategory.INDEX,"US30"),
        MarketAsset("DE40","DAX 40",AssetCategory.INDEX,"DE40"),
        MarketAsset("UK100","FTSE 100",AssetCategory.INDEX,"UK100"),
        MarketAsset("FR40","CAC 40",AssetCategory.INDEX,"FR40"),
        MarketAsset("JP225","Nikkei 225",AssetCategory.INDEX,"JP225"),
        MarketAsset("HK50","Hang Seng",AssetCategory.INDEX,"HK50"),
        MarketAsset("WTI","Petróleo WTI",AssetCategory.ENERGY,"WTI"),
        MarketAsset("BRENT","Petróleo Brent",AssetCategory.ENERGY,"BRENT"),
        MarketAsset("NATGAS","Gás Natural",AssetCategory.ENERGY,"NATURAL_GAS"),
        MarketAsset("AAPL","Apple",AssetCategory.STOCK,"AAPL","AAPL"),
        MarketAsset("MSFT","Microsoft",AssetCategory.STOCK,"MSFT","MSFT"),
        MarketAsset("NVDA","NVIDIA",AssetCategory.STOCK,"NVDA","NVDA"),
        MarketAsset("AMZN","Amazon",AssetCategory.STOCK,"AMZN","AMZN"),
        MarketAsset("TSLA","Tesla",AssetCategory.STOCK,"TSLA","TSLA"),
        MarketAsset("GOOGL","Alphabet",AssetCategory.STOCK,"GOOGL","GOOGL"),
        MarketAsset("META","Meta",AssetCategory.STOCK,"META","META")
    )
    fun get(id: String): MarketAsset? = assets.firstOrNull { it.id.equals(id.trim(), true) }
    fun all(): List<MarketAsset> = assets
    fun byCategory(category: AssetCategory): List<MarketAsset> = assets.filter { it.category == category }
    fun twelveDataSymbol(id: String): String = get(id)?.twelveDataSymbol ?: id.trim()
}
