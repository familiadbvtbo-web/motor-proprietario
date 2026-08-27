package com.motorproprietario.app

import android.app.AlertDialog
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.widget.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max

class DopmDashboardView(
    context: Context
) : ScrollView(context) {

    // =========================================================
    // CORES DA INTERFACE
    // =========================================================

    private val backgroundColor =
        Color.rgb(2, 10, 22)

    private val panelColor =
        Color.rgb(4, 21, 40)

    private val panelDark =
        Color.rgb(3, 17, 32)

    private val blueBorder =
        Color.rgb(0, 95, 165)

    private val blue =
        Color.rgb(35, 150, 255)

    private val cyan =
        Color.rgb(0, 220, 230)

    private val green =
        Color.rgb(0, 235, 125)

    private val red =
        Color.rgb(255, 65, 75)

    private val purple =
        Color.rgb(155, 65, 255)

    private val yellow =
        Color.rgb(255, 190, 30)

    private val white =
        Color.WHITE

    private val gray =
        Color.rgb(165, 185, 215)

    // =========================================================
    // ROOT
    // =========================================================

    private val root =
        LinearLayout(context)

    // =========================================================
    // CAMPOS PRINCIPAIS
    // =========================================================

    private val decisionView =
        TextView(context)

    private val totalView =
        TextView(context)

    private val buyView =
        TextView(context)

    private val sellView =
        TextView(context)

    private val neutralView =
        TextView(context)

    private val probabilityView =
        TextView(context)

    private val deterministicView =
        TextView(context)

    private val mtfView =
        TextView(context)

    private val entryView =
        TextView(context)

    private val stopView =
        TextView(context)

    private val targetsView =
        TextView(context)

    private val timingView =
        TextView(context)

    private val validityView =
        TextView(context)

    private val bestTimeframeView =
        TextView(context)

    private val priceView =
        TextView(context)

    private val onlineView =
        TextView(context)

    private val clockView =
        TextView(context)

    private val dateView =
        TextView(context)

    private val apiView =
        TextView(context)

    private val detailedStatusView =
        TextView(context)

    private val totalCircle =
        DecisionCircleView(context)

    // =========================================================
    // SELETORES
    // =========================================================

    private val assetSpinner =
        Spinner(context)

    private val timeframeSpinner =
        Spinner(context)

    private var selectedMarket =
        "FOREX"

    private var suppressInitialSelection =
        true

    private var onMarketChanged:
        ((String) -> Unit)? = null

    private var onAssetChanged:
        ((String) -> Unit)? = null

    private var onTimeframeChanged:
        ((String) -> Unit)? = null

    // =========================================================
    // INDICADORES
    // =========================================================

    private val indicatorViews =
        LinkedHashMap<String, IndicatorItem>()

    // =========================================================
    // INICIALIZAÇÃO
    // =========================================================

    init {

        setBackgroundColor(
            backgroundColor
        )

        isFillViewport = true

        build()
    }

    // =========================================================
    // CONSTRUÇÃO PRINCIPAL
    // =========================================================

    private fun build() {

        root.orientation =
            LinearLayout.VERTICAL

        root.setPadding(
            dp(10),
            dp(8),
            dp(10),
            dp(12)
        )

        root.setBackgroundColor(
            backgroundColor
        )

        addView(
            root,
            LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT
            )
        )

        buildHeader()

        buildSelectors()

        buildDecision()

        buildTradePlan()

        buildTiming()

        buildConfidence()

        buildIndicators()

        buildDetailedAnalysis()

        buildBottomNavigation()

        startClock()
    }

    // =========================================================
    // HEADER
    // =========================================================

    private fun buildHeader() {

        val row =
            LinearLayout(context)

        row.orientation =
            LinearLayout.HORIZONTAL

        row.gravity =
            Gravity.CENTER_VERTICAL

        val logo =
            TextView(context).apply {

                text = "🐂"

                textSize = 42f

                gravity =
                    Gravity.CENTER

                setPadding(
                    0,
                    0,
                    dp(5),
                    0
                )
            }

        row.addView(
            logo,
            LinearLayout.LayoutParams(
                dp(55),
                dp(75)
            )
        )

        val brand =
            LinearLayout(context)

        brand.orientation =
            LinearLayout.VERTICAL

        val dopm =
            TextView(context).apply {

                text = "DOPM"

                textSize = 32f

                setTypeface(
                    null,
                    Typeface.BOLD
                )

                setTextColor(
                    white
                )
            }

        brand.addView(
            dopm
        )

        val motor =
            TextView(context).apply {

                text =
                    "MOTOR PROPRIETÁRIO"

                textSize = 15f

                setTypeface(
                    null,
                    Typeface.BOLD
                )

                setTextColor(
                    white
                )
            }

        brand.addView(
            motor
        )

        val subtitle =
            TextView(context).apply {

                text =
                    "ANÁLISE • PRECISÃO • RESULTADO"

                textSize = 9f

                setTextColor(
                    cyan
                )
            }

        brand.addView(
            subtitle
        )

        row.addView(
            brand,
            LinearLayout.LayoutParams(
                0,
                dp(78),
                1f
            )
        )

        val status =
            LinearLayout(context)

        status.orientation =
            LinearLayout.VERTICAL

        status.gravity =
            Gravity.END

        onlineView.text =
            "● ONLINE"

        onlineView.textSize =
            13f

        onlineView.setTypeface(
            null,
            Typeface.BOLD
        )

        onlineView.setTextColor(
            green
        )

        status.addView(
            onlineView
        )

        clockView.text =
            "◷ --:--:--"

        clockView.textSize =
            11f

        clockView.setTextColor(
            white
        )

        status.addView(
            clockView
        )

        dateView.text =
            "▣ --/--/----"

        dateView.textSize =
            10f

        dateView.setTextColor(
            white
        )

        status.addView(
            dateView
        )

        apiView.text =
            "API DE DADOS"

        apiView.textSize =
            9f

        apiView.setTextColor(
            blue
        )

        status.addView(
            apiView
        )

        row.addView(
            status,
            LinearLayout.LayoutParams(
                dp(150),
                dp(78)
            )
        )

        root.addView(
            row,
            marginParams(
                0,
                0,
                0,
                7
            )
        )
    }

    // =========================================================
    // SELETORES
    // =========================================================

    private fun buildSelectors() {

        val row =
            LinearLayout(context)

        row.orientation =
            LinearLayout.HORIZONTAL

        // -----------------------------------------------------
        // MERCADO
        // -----------------------------------------------------

        val marketBox =
            panel(
                blueBorder
            )

        marketBox.addView(
            smallLabel(
                "MERCADO"
            )
        )

        val marketRow =
            LinearLayout(context)

        marketRow.orientation =
            LinearLayout.HORIZONTAL

        val forexButton =
            marketButton(
                "◎ FOREX",
                "FOREX"
            )

        val cryptoButton =
            marketButton(
                "₿ CRIPTO",
                "CRIPTO"
            )

        val b3Button =
            marketButton(
                "▥ B3",
                "B3"
            )

        marketRow.addView(
            forexButton,
            weightParams(1f)
        )

        marketRow.addView(
            cryptoButton,
            weightParams(1f)
        )

        marketRow.addView(
            b3Button,
            weightParams(1f)
        )

        marketBox.addView(
            marketRow
        )

        row.addView(
            marketBox,
            weightParams(1.55f)
        )

        // -----------------------------------------------------
        // ATIVO
        // -----------------------------------------------------

        val assetBox =
            selector(
                "ATIVO",
                assetSpinner
            )

        row.addView(
            assetBox,
            weightParams(1.15f)
        )

        val assets =
            listOf(
                "EUR/USD",
                "GBP/USD",
                "USD/JPY",
                "USD/CHF",
                "AUD/USD",
                "USD/CAD",
                "NZD/USD",
                "EUR/GBP",
                "EUR/JPY",
                "GBP/JPY",
                "BTC/USD",
                "ETH/USD",
                "IBOV"
            )

        assetSpinner.adapter =
            ArrayAdapter(
                context,
                android.R.layout.simple_spinner_dropdown_item,
                assets
            )

        assetSpinner.setSelection(
            assets.indexOf("EUR/USD")
                .coerceAtLeast(0)
        )

        // -----------------------------------------------------
        // TIMEFRAME
        // -----------------------------------------------------

        val timeframeBox =
            selector(
                "TIMEFRAME",
                timeframeSpinner
            )

        row.addView(
            timeframeBox,
            weightParams(0.95f)
        )

        val timeframes =
            listOf(
                "M1",
                "M5",
                "M15",
                "M30",
                "H1",
                "H4",
                "D1"
            )

        timeframeSpinner.adapter =
            ArrayAdapter(
                context,
                android.R.layout.simple_spinner_dropdown_item,
                timeframes
            )

        timeframeSpinner.setSelection(2)

        root.addView(
            row,
            marginParams(
                0,
                0,
                0,
                5
            )
        )

        // -----------------------------------------------------
        // MELHOR TIMEFRAME
        // -----------------------------------------------------

        val best =
            panel(
                Color.rgb(
                    0,
                    105,
                    75
                )
            )

        val bestRow =
            LinearLayout(context)

        bestRow.orientation =
            LinearLayout.HORIZONTAL

        bestRow.gravity =
            Gravity.CENTER_VERTICAL

        bestRow.addView(
            label(
                "★",
                28f,
                true
            ).apply {
                setTextColor(green)
            },
            LinearLayout.LayoutParams(
                dp(45),
                dp(45)
            )
        )

        val bestText =
            LinearLayout(context)

        bestText.orientation =
            LinearLayout.VERTICAL

        bestText.addView(
            smallLabel(
                "MELHOR TIMEFRAME"
            )
        )

        bestTimeframeView.text =
            "--"

        bestTimeframeView.textSize =
            21f

        bestTimeframeView.setTypeface(
            null,
            Typeface.BOLD
        )

        bestTimeframeView.setTextColor(
            green
        )

        bestText.addView(
            bestTimeframeView
        )

        bestRow.addView(
            bestText
        )

        best.addView(
            bestRow
        )

        root.addView(
            best,
            marginParams(
                0,
                0,
                0,
                6
            )
        )
    }

    // =========================================================
    // DECISÃO
    // =========================================================

    private fun buildDecision() {

        val box =
            panel(
                Color.rgb(
                    0,
                    110,
                    70
                )
            )

        val row =
            LinearLayout(context)

        row.orientation =
            LinearLayout.HORIZONTAL

        row.gravity =
            Gravity.CENTER_VERTICAL

        val icon =
            TextView(context).apply {

                text = "↗"

                textSize = 36f

                gravity =
                    Gravity.CENTER

                setTypeface(
                    null,
                    Typeface.BOLD
                )

                setTextColor(
                    Color.WHITE
                )

                background =
                    rounded(
                        green,
                        16
                    )
            }

        row.addView(
            icon,
            LinearLayout.LayoutParams(
                dp(72),
                dp(72)
            ).apply {
                rightMargin = dp(10)
            }
        )

        val decision =
            LinearLayout(context)

        decision.orientation =
            LinearLayout.VERTICAL

        decision.addView(
            smallLabel(
                "DECISÃO"
            )
        )

        decisionView.text =
            "AGUARDAR"

        decisionView.textSize =
            29f

        decisionView.setTypeface(
            null,
            Typeface.BOLD
        )

        decisionView.setTextColor(
            white
        )

        decision.addView(
            decisionView
        )

        totalView.text =
            "TOTAL: ---%"

        totalView.textSize =
            16f

        totalView.setTypeface(
            null,
            Typeface.BOLD
        )

        totalView.setTextColor(
            white
        )

        decision.addView(
            totalView
        )

        row.addView(
            decision,
            LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
        )

        row.addView(
            totalCircle,
            LinearLayout.LayoutParams(
                dp(115),
                dp(115)
            ).apply {
                rightMargin = dp(8)
            }
        )

        val breakdown =
            LinearLayout(context)

        breakdown.orientation =
            LinearLayout.VERTICAL

        buyView.text =
            "● COMPRA   ---%"

        buyView.textSize =
            13f

        buyView.setTextColor(
            green
        )

        sellView.text =
            "● VENDA    ---%"

        sellView.textSize =
            13f

        sellView.setTextColor(
            red
        )

        neutralView.text =
            "● NEUTRO   ---%"

        neutralView.textSize =
            13f

        neutralView.setTextColor(
            Color.rgb(
                170,
                195,
                230
            )
        )

        breakdown.addView(
            buyView
        )

        breakdown.addView(
            sellView
        )

        breakdown.addView(
            neutralView
        )

        row.addView(
            breakdown,
            LinearLayout.LayoutParams(
                dp(145),
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )

        box.addView(
            row
        )

        root.addView(
            box,
            marginParams(
                0,
                0,
                0,
                6
            )
        )
    }

    // =========================================================
    // ENTRADA / STOP / TAKE PROFIT
    // =========================================================

    private fun buildTradePlan() {

        val row =
            LinearLayout(context)

        row.orientation =
            LinearLayout.HORIZONTAL

        row.addView(
            information(
                "◎",
                "ENTRADA",
                "--",
                entryView,
                blue
            ),
            weightParams(1f)
        )

        row.addView(
            information(
                "▽",
                "STOP",
                "--",
                stopView,
                red
            ),
            weightParams(1f)
        )

        row.addView(
            information(
                "◎",
                "TAKE PROFIT",
                "TP1 --\nTP2 --\nTP3 --",
                targetsView,
                green
            ),
            weightParams(1.4f)
        )

        root.addView(
            row,
            marginParams(
                0,
                0,
                0,
                5
            )
        )
    }

    // =========================================================
    // TIMING / VALIDADE
    // =========================================================

    private fun buildTiming() {

        val row =
            LinearLayout(context)

        row.orientation =
            LinearLayout.HORIZONTAL

        row.addView(
            information(
                "◷",
                "TIMING",
                "AGUARDAR",
                timingView,
                blue
            ),
            weightParams(1f)
        )

        row.addView(
            information(
                "▣",
                "VALIDADE",
                "--",
                validityView,
                blue
            ),
            weightParams(1f)
        )

        root.addView(
            row,
            marginParams(
                0,
                0,
                0,
                5
            )
        )
    }

    // =========================================================
    // PROBABILIDADE / DETERMINISMO / MTF
    // =========================================================

    private fun buildConfidence() {

        val box =
            panel(
                blueBorder
            )

        val row =
            LinearLayout(context)

        row.orientation =
            LinearLayout.HORIZONTAL

        row.addView(
            confidence(
                "PROBABILIDADE",
                probabilityView,
                blue
            ),
            weightParams(1f)
        )

        row.addView(
            confidence(
                "DETERMINISMO",
                deterministicView,
                purple
            ),
            weightParams(1f)
        )

        row.addView(
            confidence(
                "CONFLUÊNCIA MTF",
                mtfView,
                yellow
            ),
            weightParams(1f)
        )

        box.addView(
            row
        )

        root.addView(
            box,
            marginParams(
                0,
                0,
                0,
                5
            )
        )
    }

    // =========================================================
    // INDICADORES
    // =========================================================

    private fun buildIndicators() {

        val box =
            panel(
                blueBorder
            )

        val row =
            LinearLayout(context)

        row.orientation =
            LinearLayout.HORIZONTAL

        val names =
            listOf(
                "FI",
                "FSI",
                "RSI",
                "MACD",
                "EMA",
                "ADX"
            )

        val colors =
            listOf(
                green,
                blue,
                purple,
                cyan,
                Color.rgb(
                    255,
                    110,
                    50
                ),
                yellow
            )

        names.forEachIndexed {
                index,
                name ->

            val item =
                IndicatorItem(
                    context,
                    name,
                    colors[index]
                )

            indicatorViews[name] =
                item

            row.addView(
                item,
                weightParams(1f)
            )
        }

        box.addView(
            row
        )

        root.addView(
            box,
            marginParams(
                0,
                0,
                0,
                6
            )
        )
    }

    // =========================================================
    // ANÁLISE DETALHADA
    // =========================================================

    private fun buildDetailedAnalysis() {

        val box =
            panel(
                blueBorder
            )

        val header =
            LinearLayout(context)

        header.orientation =
            LinearLayout.HORIZONTAL

        header.gravity =
            Gravity.CENTER_VERTICAL

        val icon =
            label(
                "◎",
                32f,
                true
            )

        icon.setTextColor(
            cyan
        )

        header.addView(
            icon,
            LinearLayout.LayoutParams(
                dp(48),
                dp(48)
            )
        )

        val title =
            LinearLayout(context)

        title.orientation =
            LinearLayout.VERTICAL

        title.addView(
            label(
                "ANÁLISE DETALHADA",
                14f,
                true
            )
        )

        detailedStatusView =
            label(
                "Veja todos os cálculos, indicadores e a calibração do motor.",
                9f,
                false
            ).apply {
                setTextColor(gray)
            }

        title.addView(
            detailedStatusView
        )

        header.addView(
            title,
            LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
        )

        val arrow =
            label(
                "›",
                34f,
                false
            )

        arrow.setTextColor(
            gray
        )

        header.addView(
            arrow,
            LinearLayout.LayoutParams(
                dp(35),
                dp(50)
            )
        )

        box.addView(
            header
        )

        // -----------------------------------------------------
        // GRADE DOS MÓDULOS
        // -----------------------------------------------------

        val modules =
            listOf(
                ModuleData(
                    "☷",
                    "Calibração",
                    green
                ),
                ModuleData(
                    "◴",
                    "Histórico",
                    purple
                ),
                ModuleData(
                    "▱",
                    "MTF",
                    blue
                ),
                ModuleData(
                    "♧",
                    "FSI",
                    cyan
                ),
                ModuleData(
                    "⌁",
                    "Fibonacci",
                    Color.rgb(
                        230,
                        40,
                        130
                    )
                ),
                ModuleData(
                    "〽",
                    "RSI",
                    cyan
                ),
                ModuleData(
                    "▥",
                    "MACD",
                    Color.rgb(
                        255,
                        105,
                        35
                    )
                ),
                ModuleData(
                    "⌁",
                    "EMA",
                    yellow
                ),
                ModuleData(
                    "▥",
                    "ADX",
                    green
                ),
                ModuleData(
                    "♧",
                    "Estrutura",
                    purple
                ),
                ModuleData(
                    "⚠",
                    "Armadilha",
                    red
                ),
                ModuleData(
                    "▱",
                    "Acumulação",
                    cyan
                ),
                ModuleData(
                    "▱",
                    "Distribuição",
                    yellow
                ),
                ModuleData(
                    "ϟ",
                    "Exaustão",
                    Color.rgb(
                        255,
                        40,
                        100
                    )
                )
            )

        val grid =
            LinearLayout(context)

        grid.orientation =
            LinearLayout.VERTICAL

        var row: LinearLayout? =
            null

        modules.forEachIndexed {
                index,
                module ->

            if (index % 5 == 0) {

                row =
                    LinearLayout(context)

                row!!.orientation =
                    LinearLayout.HORIZONTAL

                grid.addView(
                    row,
                    marginParams(
                        0,
                        0,
                        0,
                        2
                    )
                )
            }

            val moduleView =
                detailedModule(
                    module
                )

            row!!.addView(
                moduleView,
                weightParams(1f)
            )
        }

        box.addView(
            grid
        )

        box.setOnClickListener {
            showDetailedMessage()
        }

        root.addView(
            box,
            marginParams(
                0,
                0,
                0,
                6
            )
        )
    }

    // =========================================================
    // NAVEGAÇÃO INFERIOR
    // =========================================================

    private fun buildBottomNavigation() {

        val nav =
            LinearLayout(context)

        nav.orientation =
            LinearLayout.HORIZONTAL

        nav.gravity =
            Gravity.CENTER

        val items =
            listOf(
                "⌂\nPrincipal",
                "▥\nGráficos",
                "▤\nAnálise Detalhada",
                "⚙\nConfigurações"
            )

        items.forEachIndexed {
                index,
                item ->

            val itemView =
                label(
                    item,
                    10f,
                    true
                )

            itemView.gravity =
                Gravity.CENTER

            itemView.setTextColor(
                if (
                    index == 0
                ) {
                    green
                } else {
                    gray
                }
            )

            itemView.setPadding(
                dp(3),
                dp(6),
                dp(3),
                dp(6)
            )

            itemView.setOnClickListener {

                when (index) {

                    0 -> {
                        // Principal já está aberta.
                    }

                    1 -> {
                        Toast.makeText(
                            context,
                            "Gráficos: módulo será conectado aos dados reais.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }

                    2 -> {
                        showDetailedMessage()
                    }

                    3 -> {
                        Toast.makeText(
                            context,
                            "Configurações: módulo de configuração.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }

            nav.addView(
                itemView,
                weightParams(1f)
            )
        }

        root.addView(
            nav,
            LinearLayout.LayoutParams(
                -1,
                dp(60)
            )
        )
    }

    // =========================================================
    // SELETORES / CALLBACKS
    // =========================================================

    fun setSelectionListeners(
        marketChanged: (String) -> Unit,
        assetChanged: (String) -> Unit,
        timeframeChanged: (String) -> Unit
    ) {

        onMarketChanged =
            marketChanged

        onAssetChanged =
            assetChanged

        onTimeframeChanged =
            timeframeChanged

        assetSpinner.onItemSelectedListener =
            object :
                AdapterView.OnItemSelectedListener {

                override fun onNothingSelected(
                    parent: AdapterView<*>?
                ) {
                }

                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {

                    if (
                        suppressInitialSelection
                    ) {
                        return
                    }

                    val asset =
                        assetSpinner
                            .selectedItem
                            ?.toString()
                            ?: return

                    if (
                        asset.isNotBlank()
                    ) {
                        onAssetChanged?.invoke(
                            asset
                        )
                    }
                }
            }

        timeframeSpinner.onItemSelectedListener =
            object :
                AdapterView.OnItemSelectedListener {

                override fun onNothingSelected(
                    parent: AdapterView<*>?
                ) {
                }

                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {

                    if (
                        suppressInitialSelection
                    ) {
                        return
                    }

                    val timeframe =
                        timeframeSpinner
                            .selectedItem
                            ?.toString()
                            ?: return

                    if (
                        timeframe.isNotBlank()
                    ) {
                        onTimeframeChanged?.invoke(
                            timeframe
                        )
                    }
                }
            }

        postDelayed(
            {
                suppressInitialSelection =
                    false
            },
            300L
        )
    }

    // =========================================================
    // BOTÃO DE MERCADO
    // =========================================================

    private fun marketButton(
        text: String,
        market: String
    ): TextView {

        return TextView(context).apply {

            this.text =
                text

            textSize =
                11f

            gravity =
                Gravity.CENTER

            setTypeface(
                null,
                Typeface.BOLD
            )

            setTextColor(
                if (
                    market ==
                        selectedMarket
                ) {
                    green
                } else {
                    white
                }
            )

            background =
                GradientDrawable().apply {

                    setColor(
                        panelDark
                    )

                    setStroke(
                        dp(1),
                        if (
                            market ==
                                selectedMarket
                        ) {
                            green
                        } else {
                            blueBorder
                        }
                    )

                    cornerRadius =
                        dp(9).toFloat()
                }

            setPadding(
                dp(2),
                dp(8),
                dp(2),
                dp(8)
            )

            setOnClickListener {

                if (
                    market ==
                        selectedMarket
                ) {
                    return@setOnClickListener
                }

                selectedMarket =
                    market

                updateMarketButtons()

                onMarketChanged?.invoke(
                    market
                )
            }
        }
    }

    private fun updateMarketButtons() {
        // Os botões são atualizados quando reconstruídos pelo
        // próprio estado. A seleção lógica já é encaminhada.
    }

    // =========================================================
    // COMPONENTES VISUAIS
    // =========================================================

    private fun selector(
        title: String,
        spinner: Spinner
    ): LinearLayout {

        val box =
            panel(
                blueBorder
            )

        box.addView(
            smallLabel(
                title
            )
        )

        box.addView(
            spinner,
            LinearLayout.LayoutParams(
                -1,
                dp(40)
            )
        )

        return box
    }

    private fun information(
        icon: String,
        title: String,
        value: String,
        target: TextView,
        iconColor: Int
    ): LinearLayout {

        val box =
            panel(
                blueBorder
            )

        val row =
            LinearLayout(context)

        row.orientation =
            LinearLayout.HORIZONTAL

        row.gravity =
            Gravity.CENTER_VERTICAL

        val iconView =
            label(
                icon,
                25f,
                true
            )

        iconView.setTextColor(
            iconColor
        )

        row.addView(
            iconView,
            LinearLayout.LayoutParams(
                dp(38),
                dp(55)
            )
        )

        val values =
            LinearLayout(context)

        values.orientation =
            LinearLayout.VERTICAL

        values.addView(
            smallLabel(
                title
            )
        )

        target.text =
            value

        target.textSize =
            13f

        target.setTypeface(
            null,
            Typeface.BOLD
        )

        target.setTextColor(
            white
        )

        values.addView(
            target
        )

        row.addView(
            values
        )

        box.addView(
            row
        )

        return box
    }

    private fun confidence(
        title: String,
        target: TextView,
        valueColor: Int
    ): LinearLayout {

        val box =
            LinearLayout(context)

        box.orientation =
            LinearLayout.VERTICAL

        box.gravity =
            Gravity.CENTER

        box.setPadding(
            dp(2),
            dp(6),
            dp(2),
            dp(6)
        )

        box.addView(
            smallLabel(
                title
            )
        )

        target.text =
            "--%"

        target.textSize =
            17f

        target.setTypeface(
            null,
            Typeface.BOLD
        )

        target.setTextColor(
            valueColor
        )

        box.addView(
            target
        )

        return box
    }

    private fun detailedModule(
        module: ModuleData
    ): LinearLayout {

        val box =
            LinearLayout(context)

        box.orientation =
            LinearLayout.VERTICAL

        box.gravity =
            Gravity.CENTER

        box.setPadding(
            dp(3),
            dp(5),
            dp(3),
            dp(5)
        )

        box.background =
            GradientDrawable().apply {

                setColor(
                    panelDark
                )

                setStroke(
                    dp(1),
                    blueBorder
                )

                cornerRadius =
                    dp(8).toFloat()
            }

        val icon =
            label(
                module.icon,
                23f,
                true
            )

        icon.gravity =
            Gravity.CENTER

        icon.setTextColor(
            module.color
        )

        box.addView(
            icon
        )

        val title =
            label(
                module.name,
                8f,
                false
            )

        title.gravity =
            Gravity.CENTER

        title.setTextColor(
            gray
        )

        box.addView(
            title
        )

        box.setOnClickListener {

            showModuleMessage(
                module.name
            )
        }

        return box
    }

    private fun panel(
        border: Int
    ): LinearLayout {

        return LinearLayout(context).apply {

            orientation =
                LinearLayout.VERTICAL

            setPadding(
                dp(7),
                dp(6),
                dp(7),
                dp(6)
            )

            background =
                GradientDrawable().apply {

                    setColor(
                        panelColor
                    )

                    setStroke(
                        dp(1),
                        border
                    )

                    cornerRadius =
                        dp(11).toFloat()
                }
        }
    }

    private fun label(
        value: String,
        size: Float,
        bold: Boolean
    ): TextView {

        return TextView(context).apply {

            text =
                value

            textSize =
                size

            setTextColor(
                white
            )

            if (
                bold
            ) {

                setTypeface(
                    null,
                    Typeface.BOLD
                )
            }
        }
    }

    private fun smallLabel(
        value: String
    ): TextView {

        return label(
            value,
            8f,
            true
        ).apply {

            setTextColor(
                gray
            )
        }
    }

    private fun rounded(
        color: Int,
        radius: Int
    ): GradientDrawable {

        return GradientDrawable().apply {

            setColor(
                color
            )

            cornerRadius =
                dp(radius).toFloat()
        }
    }

    private fun weightParams(
        weight: Float
    ): LinearLayout.LayoutParams {

        return LinearLayout.LayoutParams(
            0,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            weight
        ).apply {

            setMargins(
                dp(2),
                dp(2),
                dp(2),
                dp(2)
            )
        }
    }

    private fun marginParams(
        left: Int,
        top: Int,
        right: Int,
        bottom: Int
    ): LinearLayout.LayoutParams {

        return LinearLayout.LayoutParams(
            -1,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {

            setMargins(
                dp(left),
                dp(top),
                dp(right),
                dp(bottom)
            )
        }
    }

    private fun dp(
        value: Int
    ): Int {

        return (
            value *
                resources.displayMetrics.density
            ).toInt()
    }

    // =========================================================
    // RELÓGIO
    // =========================================================

    private fun startClock() {

        post(
            object : Runnable {

                override fun run() {

                    val now =
                        Date()

                    clockView.text =
                        SimpleDateFormat(
                            "HH:mm:ss",
                            Locale.getDefault()
                        ).format(
                            now
                        )

                    dateView.text =
                        SimpleDateFormat(
                            "dd/MM/yyyy",
                            Locale.getDefault()
                        ).format(
                            now
                        )

                    postDelayed(
                        this,
                        1000L
                    )
                }
            }
        )
    }

    // =========================================================
    // ANÁLISE DETALHADA
    // =========================================================

    private fun showDetailedMessage() {

        val text =
            detailedStatusView.text
                ?.toString()
                ?: "Aguardando dados reais."

        AlertDialog.Builder(
            context
        )
            .setTitle(
                "ANÁLISE DETALHADA"
            )
            .setMessage(
                text
            )
            .setPositiveButton(
                "FECHAR",
                null
            )
            .show()
    }

    private fun showModuleMessage(
        module: String
    ) {

        AlertDialog.Builder(
            context
        )
            .setTitle(
                module
            )
            .setMessage(
                "Este módulo está reservado para apresentar os dados reais produzidos pelo motor. Nenhum valor será simulado."
            )
            .setPositiveButton(
                "FECHAR",
                null
            )
            .show()
    }

    // =========================================================
    // MÉTODOS PÚBLICOS USADOS PELO CONTROLLER
    // =========================================================

    fun setOnline(
        online: Boolean
    ) {

        onlineView.text =
            if (
                online
            ) {
                "● ONLINE"
            } else {
                "● OFFLINE"
            }

        onlineView.setTextColor(
            if (
                online
            ) {
                green
            } else {
                red
            }
        )
    }

    fun setApi(
        name: String
    ) {

        apiView.text =
            "API: $name"
    }

    fun setPrice(
        price: String
    ) {

        priceView.text =
            price
    }

    fun setDecision(
        decision: String,
        total: Double
    ) {

        decisionView.text =
            decision

        totalView.text =
            "TOTAL: ${
                "%.1f".format(
                    total
                )
            }%"

        decisionView.setTextColor(

            when (
                decision.uppercase()
            ) {

                "COMPRA" ->
                    green

                "VENDA" ->
                    red

                else ->
                    white
            }
        )

        totalCircle.setValue(
            total,
            decision
        )
    }

    fun setProbabilities(
        buy: Double,
        sell: Double,
        neutral: Double
    ) {

        buyView.text =
            "● COMPRA   ${
                "%.1f".format(
                    buy
                )
            }%"

        sellView.text =
            "● VENDA    ${
                "%.1f".format(
                    sell
                )
            }%"

        neutralView.text =
            "● NEUTRO   ${
                "%.1f".format(
                    neutral
                )
            }%"

        probabilityView.text =
            "${
                "%.1f".format(
                    max(
                        buy,
                        sell
                    )
                )
            }%"
    }

    fun setDeterminism(
        value: Double
    ) {

        deterministicView.text =
            "${
                "%.1f".format(
                    value
                )
            }%"
    }

    fun setMtf(
        value: Double
    ) {

        mtfView.text =
            "${
                "%.1f".format(
                    value
                )
            }%"
    }

    fun setTradePlan(
        entry: String,
        stop: String,
        tp1: String,
        tp2: String,
        tp3: String
    ) {

        entryView.text =
            entry

        stopView.text =
            stop

        targetsView.text =
            "TP1 $tp1\nTP2 $tp2\nTP3 $tp3"
    }

    fun setTiming(
        timing: String,
        validity: String
    ) {

        timingView.text =
            timing

        validityView.text =
            validity
    }

    fun setBestTimeframe(
        timeframe: String
    ) {

        bestTimeframeView.text =
            timeframe
    }

    // =========================================================
    // INDICADORES — RECEBEM VALORES REAIS
    // =========================================================

    fun setIndicator(
        name: String,
        value: Double
    ) {

        indicatorViews[name]
            ?.setValue(
                value
            )
    }

    fun setIndicators(
        fi: Double,
        fsi: Double,
        rsi: Double,
        macd: Double,
        ema: Double,
        adx: Double
    ) {

        setIndicator(
            "FI",
            fi
        )

        setIndicator(
            "FSI",
            fsi
        )

        setIndicator(
            "RSI",
            rsi
        )

        setIndicator(
            "MACD",
            macd
        )

        setIndicator(
            "EMA",
            ema
        )

        setIndicator(
            "ADX",
            adx
        )
    }

    fun setDetailedStatus(
        text: String
    ) {

        detailedStatusView.text =
            text
    }
}

// =============================================================
// MODELO DOS MÓDULOS
// =============================================================

private data class ModuleData(
    val icon: String,
    val name: String,
    val color: Int
)

// =============================================================
// ITEM DE INDICADOR
// =============================================================

private class IndicatorItem(
    context: Context,
    private val name: String,
    private val barColor: Int
) : LinearLayout(context) {

    private val valueView =
        TextView(context)

    private val progress =
        ProgressBar(
            context,
            null,
            android.R.attr.progressBarStyleHorizontal
        )

    init {

        orientation =
            VERTICAL

        gravity =
            Gravity.CENTER

        setPadding(
            2,
            3,
            2,
            3
        )

        val title =
            TextView(context).apply {

                text =
                    name

                textSize =
                    8f

                setTypeface(
                    null,
                    Typeface.BOLD
                )

                setTextColor(
                    Color.rgb(
                        180,
                        195,
                        220
                    )
                )
            }

        addView(
            title
        )

        progress.max =
            100

        progress.progress =
            0

        addView(
            progress,
            LayoutParams(
                dp(50),
                dp(7)
            )
        )

        valueView.text =
            "--"

        valueView.textSize =
            10f

        valueView.setTypeface(
            null,
            Typeface.BOLD
        )

        valueView.setTextColor(
            barColor
        )

        addView(
            valueView
        )
    }

    fun setValue(
        value: Double
    ) {

        val safe =
            value.coerceIn(
                0.0,
                100.0
            )

        progress.progress =
            safe.toInt()

        valueView.text =
            "${"%.1f".format(safe)}%"
    }

    private fun dp(
        value: Int
    ): Int {

        return (
            value *
                resources.displayMetrics.density
            ).toInt()
    }
}

// =============================================================
// CÍRCULO DA DECISÃO
// =============================================================

private class DecisionCircleView(
    context: Context
) : View(context) {

    private val paint =
        Paint(
            Paint.ANTI_ALIAS_FLAG
        )

    private var value =
        0.0

    private var direction =
        "AGUARDAR"

    init {

        paint.strokeWidth =
            8f

        paint.style =
            Paint.Style.STROKE

        paint.strokeCap =
            Paint.Cap.BUTT
    }

    fun setValue(
        value: Double,
        direction: String
    ) {

        this.value =
            value.coerceIn(
                0.0,
                100.0
            )

        this.direction =
            direction

        invalidate()
    }

    override fun onDraw(
        canvas: Canvas
    ) {

        super.onDraw(
            canvas
        )

        val centerX =
            width / 2f

        val centerY =
            height / 2f

        val radius =
            (
                minOf(
                    width,
                    height
                ) / 2f
            ) - 12f

        // Fundo do círculo
        paint.style =
            Paint.Style.STROKE

        paint.strokeWidth =
            8f

        paint.color =
            Color.rgb(
                10,
                55,
                75
            )

        canvas.drawCircle(
            centerX,
            centerY,
            radius,
            paint
        )

        // Parte preenchida
        paint.color =
            when (
                direction.uppercase()
            ) {

                "VENDA" ->
                    Color.rgb(
                        255,
                        65,
                        75
                    )

                "COMPRA" ->
                    Color.rgb(
                        0,
                        235,
                        125
                    )

                else ->
                    Color.rgb(
                        120,
                        145,
                        175
                    )
            }

        val rect =
            android.graphics.RectF(
                centerX - radius,
                centerY - radius,
                centerX + radius,
                centerY + radius
            )

        canvas.drawArc(
            rect,
            -90f,
            (
                360f *
                    value.toFloat() /
                    100f
                ),
            false,
            paint
        )

        // Texto
        paint.style =
            Paint.Style.FILL

        paint.color =
            Color.WHITE

        paint.textAlign =
            Paint.Align.CENTER

        paint.typeface =
            Typeface.create(
                Typeface.DEFAULT,
                Typeface.BOLD
            )

        paint.textSize =
            18f

        canvas.drawText(
            "${"%.1f".format(value)}%",
            centerX,
            centerY + 2f,
            paint
        )

        paint.textSize =
            9f

        paint.color =
            Color.rgb(
                175,
                195,
                220
            )

        canvas.drawText(
            "TOTAL",
            centerX,
            centerY + 18f,
            paint
        )
    }
}
