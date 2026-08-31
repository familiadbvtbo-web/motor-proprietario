package com.motorproprietario.app

/*
 * Extensão da camada de seleção.
 *
 * IMPORTANTE:
 * O Controller existente continua responsável pela UI.
 * Este arquivo fornece apenas a lista completa de ativos
 * para ser conectada ao seletor que já existe.
 */
object DopmAssetCatalog {

    fun all(): List<MarketAsset> =
        AssetRegistry.all()

    fun forex(): List<MarketAsset> =
        AssetRegistry.byCategory(
            AssetCategory.FOREX
        )

    fun crypto(): List<MarketAsset> =
        AssetRegistry.byCategory(
            AssetCategory.CRYPTO
        )

    fun indices(): List<MarketAsset> =
        AssetRegistry.byCategory(
            AssetCategory.INDEX
        )

    fun metals(): List<MarketAsset> =
        AssetRegistry.byCategory(
            AssetCategory.METAL
        )

    fun energy(): List<MarketAsset> =
        AssetRegistry.byCategory(
            AssetCategory.ENERGY
        )

    fun stocks(): List<MarketAsset> =
        AssetRegistry.byCategory(
            AssetCategory.STOCK
        )

    fun etfs(): List<MarketAsset> =
        AssetRegistry.byCategory(
            AssetCategory.ETF
        )

    fun ids(): List<String> =
        AssetRegistry.all().map {
            it.id
        }
}
