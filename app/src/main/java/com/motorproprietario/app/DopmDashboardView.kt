package com.motorproprietario.app

import android.app.AlertDialog
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.TextView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * DOPM - Dashboard
 *
 * SOMENTE INTERFACE.
 *
 * Nenhum cálculo matemático é realizado aqui.
 * Os resultados são recebidos do Controller.
 */
class DopmDashboardView(
    context: Context
) : FrameLayout(context) {

    // =========================================================
    // CORES
    // =========================================================

    private val bg = Color.rgb(2, 10, 22)
    private val panel = Color.rgb(4, 21, 40)
    private val dark = Color.rgb(3, 17, 32)
    private val border = Color.rgb(0, 82, 145)
    private val green = Color.rgb(0, 235, 125)
    private val red = Color.rgb(255, 65, 75)
    private val blue = Color.rgb(35, 150, 255)
    private val cyan = Color.rgb(0, 220, 230)
    private val purple = Color.rgb(155, 65, 255)
    private val yellow = Color.rgb(255, 190, 30)
    private val white = Color.WHITE
    private val gray = Color.rgb(165, 185, 215)
    private val neutral = Color.rgb(170, 195, 230)

    // =========================================================
    // ROOT
    // =========================================================

    private val scroll = ScrollView(context)
    private val content = LinearLayout(context)
    private val bottomNavigation = LinearLayout(context)

    // =========================================================
    // HEADER
    // =========================================================

    private val onlineView = TextView(context)
    private val clockView = TextView(context)
    private val dateView = TextView(context)
    private val apiView = TextView(context)
    private val priceView = TextView(context)

    // =========================================================
    // SELETORES
    // =========================================================

    private val assetSpinner = Spinner(context)
    private val timeframeSpinner = Spinner(context)
    private val bestTimeframeView = TextView(context)
    private val marketButtons = LinkedHashMap<String, TextView>()

    private var onMarketChanged: ((String) -> Unit)? = null
    private var onAssetChanged: ((String) -> Unit)? = null
    private var onTimeframeChanged: ((String) -> Unit)? = null
    private var suppressSelection = true

    // =========================================================
    // DECISÃO
    // =========================================================

    private val decisionView = TextView(context)
    private val totalView = TextView(context)
    private val buyView = TextView(context)
    private val sellView = TextView(context)
    private val neutralView = TextView(context)
    private val decisionIcon = TextView(context)
    private val decisionCircle = DecisionCircle(context)

    // =========================================================
    // PLANO
    // =========================================================

    private val entryView = TextView(context)
    private val stopView = TextView(context)
    private val targetsView = TextView(context)
    private val timingView = TextView(context)
    private val validityView = TextView(context)

    // =========================================================
    // CONFIANÇA
    // =========================================================

    private val probabilityView = TextView(context)
    private val deterministicView = TextView(context)
    private val mtfView = TextView(context)

    // =========================================================
    // INDICADORES
    // =========================================================

    private val indicatorViews =
        LinkedHashMap<String, IndicatorBar>()

    // =========================================================
    // RELÓGIO
    // =========================================================

    private val clockHandler =
        Handler(Looper.getMainLooper())

    private val clockRunnable =
        object : Runnable {

            override fun run() {

                val now = Date()

                clockView.text =
                    "◷ " +
                        SimpleDateFormat(
                            "HH:mm:ss",
                            Locale.getDefault()
                        ).format(now)

                dateView.text =
                    "▣ " +
                        SimpleDateFormat(
                            "dd/MM/yyyy",
                            Locale.getDefault()
                        ).format(now)

                clockHandler.postDelayed(
                    this,
                    1000L
                )
            }
        }

    // =========================================================
    // INIT
    // =========================================================

    init {

        setBackgroundColor(bg)

        build()

        postDelayed(
            {
                suppressSelection = false
            },
            500L
        )
    }

    // =========================================================
    // UTILITÁRIOS
    // =========================================================

    private fun dp(
        value: Int
    ): Int {

        return (
            value *
                resources.displayMetrics.density
            ).toInt()
    }

    private fun txt(
        value: String,
        size: Float,
        color: Int = white,
        bold: Boolean = false
    ): TextView {

        return TextView(context).apply {

            text = value
            textSize = size
            setTextColor(color)
            includeFontPadding = true

            if (bold) {
                setTypeface(
                    null,
                    Typeface.BOLD
                )
            }
        }
    }

    private fun rounded(
        fill: Int,
        stroke: Int = border,
        radius: Int = 14
    ): GradientDrawable {

        return GradientDrawable().apply {

            setColor(fill)

            setStroke(
                dp(1),
                stroke
            )

            cornerRadius =
                dp(radius).toFloat()
        }
    }

    private fun card(
        stroke: Int = border
    ): LinearLayout {

        return LinearLayout(context).apply {

            orientation =
                LinearLayout.VERTICAL

            setPadding(
                dp(10),
                dp(8),
                dp(10),
                dp(8)
            )

            background =
                rounded(
                    dark,
                    stroke,
                    14
                )
        }
    }

    private fun gap(
        height: Int = 5
    ) {

        content.addView(
            View(context),
            LinearLayout.LayoutParams(
                1,
                dp(height)
            )
        )
    }

    // =========================================================
    // BUILD
    // =========================================================

    private fun build() {

        content.orientation =
            LinearLayout.VERTICAL

        content.setPadding(
            dp(10),
            dp(8),
            dp(10),
            dp(82)
        )

        content.setBackgroundColor(bg)

        scroll.isFillViewport = true

        scroll.addView(
    content,
    ScrollView.LayoutParams(
        FrameLayout.LayoutParams.MATCH_PARENT,
        FrameLayout.LayoutParams.WRAP_CONTENT
    )
)

addView(
    scroll,
    FrameLayout.LayoutParams(
        FrameLayout.LayoutParams.MATCH_PARENT,
        FrameLayout.LayoutParams.MATCH_PARENT
    )
)

        buildHeader()
        buildSelectors()
        buildDecision()
        buildTradePlan()
        buildTiming()
        buildConfidence()
        buildIndicators()
        buildDetailed()
        buildBottomNavigation()
        startClock()
    }

    // =========================================================
    // HEADER
    // =========================================================

    private fun buildHeader() {

        val row =
            LinearLayout(context).apply {

                orientation =
                    LinearLayout.HORIZONTAL

                gravity =
                    Gravity.CENTER_VERTICAL
            }

        val brand =
            LinearLayout(context).apply {

                orientation =
                    LinearLayout.VERTICAL
            }

        brand.addView(
            txt(
                "🐂  DOPM",
                26f,
                white,
                true
            )
        )

        brand.addView(
            txt(
                "MOTOR PROPRIETÁRIO",
                13f,
                white,
                true
            )
        )

        brand.addView(
            txt(
                "ANÁLISE • PRECISÃO • RESULTADO",
                9f,
                cyan
            )
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
            LinearLayout(context).apply {

                orientation =
                    LinearLayout.VERTICAL

                gravity =
                    Gravity.END
            }

        onlineView.apply {

            text = "● ONLINE"
            textSize = 12f
            setTextColor(green)

            setTypeface(
                null,
                Typeface.BOLD
            )

            gravity =
                Gravity.END
        }

        clockView.apply {

            text = "◷ --:--:--"
            textSize = 10f
            setTextColor(white)
            gravity =
                Gravity.END
        }

        dateView.apply {

            text = "▣ --/--/----"
            textSize = 10f
            setTextColor(white)
            gravity =
                Gravity.END
        }

        apiView.apply {

            text =
                "API DE DADOS • TWELVE DATA"

            textSize = 9f
            setTextColor(blue)
            gravity =
                Gravity.END

            maxLines = 1

            ellipsize =
                TextUtils.TruncateAt.END
        }

        priceView.apply {

            text = "Preço: --"
            textSize = 9f
            setTextColor(gray)
            gravity =
                Gravity.END
        }

        status.addView(onlineView)
        status.addView(clockView)
        status.addView(dateView)
        status.addView(apiView)
        status.addView(priceView)

        row.addView(
            status,
            LinearLayout.LayoutParams(
                dp(155),
                dp(78)
            )
        )

        content.addView(row)

        gap(6)
    }

    // =========================================================
    // SELETORES
    // =========================================================

    private fun buildSelectors() {

        val row =
            LinearLayout(context).apply {

                orientation =
                    LinearLayout.HORIZONTAL
            }

        val market = card()

        market.addView(
            txt(
                "MERCADO",
                10f,
                gray,
                true
            )
        )

        val marketRow =
            LinearLayout(context).apply {

                orientation =
                    LinearLayout.HORIZONTAL
            }

        listOf(
            "FOREX" to "◎ FOREX",
            "CRIPTO" to "₿ CRIPTO",
            "B3" to "▥ B3"
        ).forEach { pair ->

            val button =
                txt(
                    pair.second,
                    10f,
                    white,
                    true
                ).apply {

                    gravity =
                        Gravity.CENTER

                    setPadding(
                        dp(1),
                        dp(6),
                        dp(1),
                        dp(6)
                    )

                    background =
                        rounded(
                            panel,
                            border,
                            10
                        )

                    setOnClickListener {

                        selectMarket(
                            pair.first
                        )

                        onMarketChanged?.invoke(
                            pair.first
                        )
                    }
                }

            marketButtons[pair.first] =
                button

            marketRow.addView(
                button,
                LinearLayout.LayoutParams(
                    0,
                    dp(38),
                    1f
                ).apply {
                    marginEnd =
                        dp(3)
                }
            )
        }

        market.addView(marketRow)

        row.addView(
            market,
            LinearLayout.LayoutParams(
                0,
                dp(78),
                1.5f
            ).apply {
                marginEnd =
                    dp(5)
            }
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

        assetSpinner.setSelection(0)

        assetSpinner.onItemSelectedListener =
            object :
                AdapterView.OnItemSelectedListener {

                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {

                    if (!suppressSelection) {

                        onAssetChanged?.invoke(
                            assets[position]
                        )
                    }
                }

                override fun onNothingSelected(
                    parent: AdapterView<*>?
                ) {}
            }

        row.addView(
            selector(
                "ATIVO",
                assetSpinner
            ),
            LinearLayout.LayoutParams(
                0,
                dp(78),
                1.05f
            ).apply {
                marginEnd =
                    dp(5)
            }
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

        timeframeSpinner.onItemSelectedListener =
            object :
                AdapterView.OnItemSelectedListener {

                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {

                    if (!suppressSelection) {

                        onTimeframeChanged?.invoke(
                            timeframes[position]
                        )
                    }
                }

                override fun onNothingSelected(
                    parent: AdapterView<*>?
                ) {}
            }

        row.addView(
            selector(
                "TIMEFRAME",
                timeframeSpinner
            ),
            LinearLayout.LayoutParams(
                0,
                dp(78),
                0.8f
            )
        )

        content.addView(row)

        gap()

        val best =
            card(
                Color.rgb(
                    0,
                    105,
                    75
                )
            )

        val bestRow =
            LinearLayout(context).apply {

                orientation =
                    LinearLayout.HORIZONTAL

                gravity =
                    Gravity.CENTER_VERTICAL
            }

        bestRow.addView(
            txt(
                "★",
                30f,
                green,
                true
            ),
            LinearLayout.LayoutParams(
                dp(48),
                dp(52)
            )
        )

        val bestText =
            LinearLayout(context).apply {

                orientation =
                    LinearLayout.VERTICAL
            }

        bestText.addView(
            txt(
                "MELHOR TIMEFRAME",
                10f,
                gray,
                true
            )
        )

        bestTimeframeView.apply {

            text = "--"
            textSize = 22f
            setTextColor(green)

            setTypeface(
                null,
                Typeface.BOLD
            )
        }

        bestText.addView(
            bestTimeframeView
        )

        bestRow.addView(bestText)
        best.addView(bestRow)

        content.addView(
            best,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(68)
            )
        )

        gap()

        selectMarket("FOREX")
    }

    private fun selector(
        title: String,
        spinner: Spinner
    ): LinearLayout {

        return LinearLayout(context).apply {

            orientation =
                LinearLayout.VERTICAL

            gravity =
                Gravity.CENTER_VERTICAL

            setPadding(
                dp(7),
                dp(5),
                dp(2),
                dp(3)
            )

            background =
                rounded(
                    dark,
                    border,
                    14
                )

            addView(
                txt(
                    title,
                    9f,
                    gray,
                    true
                )
            )

            addView(
                spinner,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    dp(42)
                )
            )
        }
    }

    private fun selectMarket(
        market: String
    ) {

        marketButtons.forEach { entry ->

            val active =
                entry.key == market

            entry.value.background =
                rounded(
                    if (active) {
                        Color.rgb(
                            3,
                            48,
                            38
                        )
                    } else {
                        panel
                    },
                    if (active) {
                        green
                    } else {
                        border
                    },
                    10
                )

            entry.value.setTextColor(
                if (active) {
                    green
                } else {
                    white
                }
            )
        }
    }

    // =========================================================
    // DECISÃO
    // =========================================================

    private fun buildDecision() {

        val box =
            card(
                Color.rgb(
                    0,
                    110,
                    70
                )
            )

        val row =
            LinearLayout(context).apply {

                orientation =
                    LinearLayout.HORIZONTAL

                gravity =
                    Gravity.CENTER_VERTICAL
            }

        decisionIcon.apply {

            text = "→"
            textSize = 30f
            gravity =
                Gravity.CENTER

            setTextColor(white)

            setTypeface(
                null,
                Typeface.BOLD
            )

            background =
                rounded(
                    Color.rgb(
                        70,
                        90,
                        115
                    ),
                    Color.rgb(
                        70,
                        90,
                        115
                    ),
                    16
                )
        }

        row.addView(
            decisionIcon,
            LinearLayout.LayoutParams(
                dp(60),
                dp(76)
            ).apply {
                marginEnd =
                    dp(6)
            }
        )

        val decision =
            LinearLayout(context).apply {

                orientation =
                    LinearLayout.VERTICAL

                gravity =
                    Gravity.CENTER_VERTICAL
            }

        decision.addView(
            txt(
                "DECISÃO",
                10f,
                gray,
                true
            )
        )

        decisionView.apply {

            text = "AGUARDAR"
            textSize = 23f
            setTextColor(white)

            setTypeface(
                null,
                Typeface.BOLD
            )

            maxLines = 1

            ellipsize =
                TextUtils.TruncateAt.END

            includeFontPadding = false
        }

        decision.addView(decisionView)

        totalView.apply {

            text = "TOTAL: ---%"
            textSize = 14f
            setTextColor(white)

            setTypeface(
                null,
                Typeface.BOLD
            )

            maxLines = 1
        }

        decision.addView(totalView)

        row.addView(
            decision,
            LinearLayout.LayoutParams(
                0,
                dp(82),
                1f
            ).apply {
                marginEnd =
                    dp(4)
            }
        )

        row.addView(
            decisionCircle,
            LinearLayout.LayoutParams(
                dp(92),
                dp(92)
            ).apply {
                marginEnd =
                    dp(4)
            }
        )

        val breakdown =
            LinearLayout(context).apply {

                orientation =
                    LinearLayout.VERTICAL

                gravity =
                    Gravity.CENTER_VERTICAL
            }

        buyView.apply {

            text = "● COMPRA   ---%"
            textSize = 11f
            setTextColor(green)
            maxLines = 1
            includeFontPadding = false
        }

        sellView.apply {

            text = "● VENDA    ---%"
            textSize = 11f
            setTextColor(red)
            maxLines = 1
            includeFontPadding = false
        }

        neutralView.apply {

            text = "● NEUTRO   ---%"
            textSize = 11f
            setTextColor(neutral)
            maxLines = 1
            includeFontPadding = false
        }

        breakdown.addView(buyView)
        breakdown.addView(sellView)
        breakdown.addView(neutralView)

        row.addView(
            breakdown,
            LinearLayout.LayoutParams(
                0,
                dp(84),
                1.15f
            )
        )

        box.addView(row)

        content.addView(
            box,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(122)
            )
        )

        gap()
    }

    // =========================================================
    // INFORMAÇÕES
    // =========================================================

    private fun information(
        icon: String,
        title: String,
        initial: String,
        valueView: TextView,
        color: Int
    ): LinearLayout {

        val box = card()

        val row =
            LinearLayout(context).apply {

                orientation =
                    LinearLayout.HORIZONTAL

                gravity =
                    Gravity.CENTER_VERTICAL
            }

        row.addView(
            txt(
                icon,
                24f,
                color,
                true
            ),
            LinearLayout.LayoutParams(
                dp(38),
                LinearLayout.LayoutParams.MATCH_PARENT
            )
        )

        val column =
            LinearLayout(context).apply {

                orientation =
                    LinearLayout.VERTICAL

                gravity =
                    Gravity.CENTER_VERTICAL
            }

        column.addView(
            txt(
                title,
                9f,
                gray,
                true
            )
        )

        valueView.apply {

            text = initial

            textSize =
                if (title == "TAKE PROFIT") {
                    14f
                } else {
                    17f
                }

            setTextColor(white)

            setTypeface(
                null,
                Typeface.BOLD
            )

            maxLines = 3
        }

        column.addView(valueView)

        row.addView(
            column,
            LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.MATCH_PARENT,
                1f
            )
        )

        box.addView(row)

        return box
    }

    // =========================================================
    // PLANO
    // =========================================================

    private fun buildTradePlan() {

        val row =
            LinearLayout(context).apply {

                orientation =
                    LinearLayout.HORIZONTAL
            }

        row.addView(
            information(
                "◎",
                "ENTRADA",
                "--",
                entryView,
                blue
            ),
            LinearLayout.LayoutParams(
                0,
                dp(98),
                1f
            ).apply {
                marginEnd =
                    dp(5)
            }
        )

        row.addView(
            information(
                "▽",
                "STOP",
                "--",
                stopView,
                red
            ),
            LinearLayout.LayoutParams(
                0,
                dp(98),
                1f
            ).apply {
                marginEnd =
                    dp(5)
            }
        )

        row.addView(
            information(
                "◎",
                "TAKE PROFIT",
                "TP1 --\nTP2 --\nTP3 --",
                targetsView,
                green
            ),
            LinearLayout.LayoutParams(
                0,
                dp(98),
                1.35f
            )
        )

        content.addView(row)

        gap()
    }

    // =========================================================
    // TIMING
    // =========================================================

    private fun buildTiming() {

        val row =
            LinearLayout(context).apply {

                orientation =
                    LinearLayout.HORIZONTAL
            }

        row.addView(
            information(
                "◷",
                "TIMING",
                "AGUARDAR",
                timingView,
                blue
            ),
            LinearLayout.LayoutParams(
                0,
                dp(82),
                1f
            ).apply {
                marginEnd =
                    dp(5)
            }
        )

        row.addView(
            information(
                "▣",
                "VALIDADE",
                "--",
                validityView,
                blue
            ),
            LinearLayout.LayoutParams(
                0,
                dp(82),
                1f
            )
        )

        content.addView(row)

        gap()
    }

    // =========================================================
    // CONFIANÇA
    // =========================================================

    private fun buildConfidence() {

        val box = card()

        val row =
            LinearLayout(context).apply {

                orientation =
                    LinearLayout.HORIZONTAL
            }

        row.addView(
            confidence(
                "PROBABILIDADE",
                probabilityView,
                blue
            ),
            LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.MATCH_PARENT,
                1f
            ).apply {
                marginEnd =
                    dp(5)
            }
        )

        row.addView(
            confidence(
                "DETERMINISMO",
                deterministicView,
                purple
            ),
            LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.MATCH_PARENT,
                1f
            ).apply {
                marginEnd =
                    dp(5)
            }
        )

        row.addView(
            confidence(
                "CONFLUÊNCIA MTF",
                mtfView,
                yellow
            ),
            LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.MATCH_PARENT,
                1f
            )
        )

        box.addView(row)

        content.addView(
            box,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(76)
            )
        )

        gap()
    }

    private fun confidence(
        title: String,
        value: TextView,
        color: Int
    ): LinearLayout {

        return LinearLayout(context).apply {

            orientation =
                LinearLayout.VERTICAL

            gravity =
                Gravity.CENTER_VERTICAL

            addView(
                txt(
                    title,
                    9f,
                    gray,
                    true
                )
            )

            value.apply {

                text = "--"
                textSize = 19f
                setTextColor(color)

                setTypeface(
                    null,
                    Typeface.BOLD
                )
            }

            addView(value)
        }
    }

    // =========================================================
    // INDICADORES
    // =========================================================

    private fun buildIndicators() {

        val box = card()

        val row =
            LinearLayout(context).apply {

                orientation =
                    LinearLayout.HORIZONTAL
            }

        listOf(
            "FI" to green,
            "FSI" to blue,
            "RSI" to purple,
            "MACD" to cyan,
            "EMA" to Color.rgb(
                255,
                110,
                50
            ),
            "ADX" to yellow
        ).forEachIndexed { index, pair ->

            val item =
                IndicatorBar(
                    context,
                    pair.first,
                    pair.second
                )

            indicatorViews[pair.first] =
                item

            row.addView(
                item,
                LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    1f
                ).apply {

                    if (index < 5) {
                        marginEnd =
                            dp(3)
                    }
                }
            )
        }

        box.addView(row)

        content.addView(
            box,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(76)
            )
        )

        gap()
    }

    // =========================================================
    // ANÁLISE DETALHADA
    // =========================================================

    private fun buildDetailed() {

        val box = card()

        val header =
            LinearLayout(context).apply {

                orientation =
                    LinearLayout.HORIZONTAL

                gravity =
                    Gravity.CENTER_VERTICAL
            }

        header.addView(
            txt(
                "◎",
                30f,
                cyan,
                true
            ),
            LinearLayout.LayoutParams(
                dp(48),
                dp(48)
            )
        )

        val title =
            LinearLayout(context).apply {

                orientation =
                    LinearLayout.VERTICAL
            }

        title.addView(
            txt(
                "ANÁLISE DETALHADA",
                15f,
                white,
                true
            )
        )

        title.addView(
            txt(
                "Veja todos os cálculos, indicadores e a calibração do motor.",
                9f,
                gray
            )
        )

        header.addView(
            title,
            LinearLayout.LayoutParams(
                0,
                dp(50),
                1f
            )
        )

        header.addView(
            txt(
                "›",
                34f,
                gray
            ).apply {

                gravity =
                    Gravity.CENTER

                setOnClickListener {
                    showDetailed()
                }
            },
            LinearLayout.LayoutParams(
                dp(42),
                dp(50)
            )
        )

        box.addView(header)

        val names =
            listOf(
                "Calibração",
                "Histórico",
                "MTF",
                "FSI",
                "Fibonacci",
                "RSI",
                "MACD",
                "EMA",
                "ADX",
                "Estrutura",
                "Armadilha",
                "Acumulação",
                "Distribuição",
                "Exaustão"
            )

        val grid =
            LinearLayout(context).apply {

                orientation =
                    LinearLayout.VERTICAL
            }

        names.chunked(5).forEachIndexed {
            rowIndex,
            chunk ->

            val line =
                LinearLayout(context).apply {

                    orientation =
                        LinearLayout.HORIZONTAL
                }

            chunk.forEachIndexed {
                columnIndex,
                name ->

                val index =
                    rowIndex * 5 +
                        columnIndex

                val item =
                    txt(
                        "${iconFor(index)}\n$name",
                        8f,
                        white
                    ).apply {

                        gravity =
                            Gravity.CENTER

                        setPadding(
                            dp(2),
                            dp(5),
                            dp(2),
                            dp(5)
                        )

                        background =
                            rounded(
                                panel,
                                border,
                                10
                            )
                    }

                line.addView(
                    item,
                    LinearLayout.LayoutParams(
                        0,
                        dp(62),
                        1f
                    ).apply {

                        if (columnIndex < 4) {
                            marginEnd =
                                dp(4)
                        }
                    }
                )
            }

            while (line.childCount < 5) {

                line.addView(
                    View(context),
                    LinearLayout.LayoutParams(
                        0,
                        dp(62),
                        1f
                    )
                )
            }

            grid.addView(line)

            if (rowIndex < 2) {

                grid.addView(
                    View(context),
                    LinearLayout.LayoutParams(
                        1,
                        dp(4)
                    )
                )
            }
        }

        box.addView(grid)

        content.addView(box)

        gap(8)
    }

    private fun iconFor(
        index: Int
    ): String {

        return when (index) {

            0 -> "☷"
            1 -> "◴"
            2 -> "▱"
            3 -> "♣"
            4 -> "↻"
            5 -> "∿"
            6 -> "▥"
            7 -> "↗"
            8 -> "▥"
            9 -> "♧"
            10 -> "⚠"
            11 -> "▱"
            12 -> "▱"
            else -> "ϟ"
        }
    }

    private fun showDetailed() {

        val message =
            buildString {

                append(
                    "MOTOR PROPRIETÁRIO\n\n"
                )

                append("DECISÃO: ")
                append(decisionView.text)
                append("\n")

                append(totalView.text)
                append("\n")

                append(buyView.text)
                append("\n")

                append(sellView.text)
                append("\n")

                append(neutralView.text)

                append(
                    "\n\nPROBABILIDADE: "
                )

                append(
                    probabilityView.text
                )

                append(
                    "\nDETERMINISMO: "
                )

                append(
                    deterministicView.text
                )

                append(
                    "\nMTF: "
                )

                append(
                    mtfView.text
                )

                append(
                    "\n\nENTRADA: "
                )

                append(
                    entryView.text
                )

                append(
                    "\nSTOP: "
                )

                append(
                    stopView.text
                )

                append("\n")

                append(
                    targetsView.text
                )

                append(
                    "\nTIMING: "
                )

                append(
                    timingView.text
                )

                append(
                    "\nVALIDADE: "
                )

                append(
                    validityView.text
                )
            }

        AlertDialog.Builder(context)
            .setTitle(
                "ANÁLISE DETALHADA"
            )
            .setMessage(message)
            .setPositiveButton(
                "FECHAR",
                null
            )
            .show()
    }

    // =========================================================
    // NAVEGAÇÃO
    // =========================================================

    private fun buildBottomNavigation() {

        bottomNavigation.orientation =
            LinearLayout.HORIZONTAL

        bottomNavigation.gravity =
            Gravity.CENTER

        bottomNavigation.setPadding(
            0,
            dp(8),
            0,
            dp(4)
        )

        listOf(
            "⌂\nPrincipal",
            "▥\nGráficos",
            "▤\nAnálise Detalhada",
            "⚙\nConfigurações"
        ).forEachIndexed {
            index,
            label ->

            val item =
                txt(
                    label,
                    9f,
                    if (index == 0) {
                        green
                    } else {
                        gray
                    },
                    index == 0
                ).apply {

                    gravity =
                        Gravity.CENTER

                    setPadding(
                        dp(2),
                        dp(6),
                        dp(2),
                        dp(6)
                    )
                }

            bottomNavigation.addView(
                item,
                LinearLayout.LayoutParams(
                    0,
                    dp(58),
                    1f
                )
            )
        }

        addView(
            bottomNavigation,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                dp(72),
                Gravity.BOTTOM
            ).apply {

                leftMargin = dp(4)
                rightMargin = dp(4)
                bottomMargin = dp(2)
            }
        )
    }

    // =========================================================
    // RELÓGIO
    // =========================================================

    private fun startClock() {

        clockHandler.removeCallbacks(
            clockRunnable
        )

        clockHandler.post(
            clockRunnable
        )
    }

    override fun onDetachedFromWindow() {

        clockHandler.removeCallbacks(
            clockRunnable
        )

        super.onDetachedFromWindow()
    }

    // =========================================================
    // CALLBACKS
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
    }

    // =========================================================
    // ATUALIZAÇÕES
    // =========================================================

    fun setOnline(
        online: Boolean
    ) {

        onlineView.text =
            if (online) {
                "● ONLINE"
            } else {
                "● OFFLINE"
            }

        onlineView.setTextColor(
            if (online) {
                green
            } else {
                red
            }
        )
    }

    fun setApi(
        api: String
    ) {

        apiView.text =
            "API DE DADOS • $api"
    }

    fun setPrice(
        price: String
    ) {

        priceView.text =
            "Preço: $price"
    }

    fun setDecision(
        direction: String,
        total: Double
    ) {

        val shown =
            when (
                direction.uppercase(
                    Locale.US
                )
            ) {

                "COMPRA",
                "BUY" ->
                    "COMPRA"

                "VENDA",
                "SELL" ->
                    "VENDA"

                else ->
                    "AGUARDAR"
            }

        decisionView.text =
            shown

        totalView.text =
            "TOTAL: ${format(total)}%"

        val color =
            when (shown) {

                "COMPRA" ->
                    green

                "VENDA" ->
                    red

                else ->
                    white
            }

        decisionView.setTextColor(
            color
        )

        decisionIcon.text =
            when (shown) {

                "COMPRA" ->
                    "↗"

                "VENDA" ->
                    "↘"

                else ->
                    "→"
            }

        decisionCircle.value =
            total

        decisionCircle.accent =
            color
    }

    fun setProbabilities(
        buy: Double,
        sell: Double,
        neutral: Double
    ) {

        buyView.text =
            "● COMPRA   ${format(buy)}%"

        sellView.text =
            "● VENDA    ${format(sell)}%"

        neutralView.text =
            "● NEUTRO   ${format(neutral)}%"
    }

    fun setProbability(
        value: Double
    ) {

        probabilityView.text =
            "${format(value)}%"
    }

    fun setDeterminism(
        value: Double
    ) {

        deterministicView.text =
            "${format(value)}%"
    }

    fun setMtf(
        value: Double
    ) {

        mtfView.text =
            "${format(value)}%"
    }

    fun setBestTimeframe(
        value: String
    ) {

        bestTimeframeView.text =
            value
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

    fun setIndicator(
        name: String,
        value: Double
    ) {

        indicatorViews[name]?.setValue(
            value
        )
    }

    private fun format(
        value: Double
    ): String {

        return String.format(
            Locale.US,
            "%.1f",
            value
        )
    }

    // =========================================================
    // INDICADOR
    // =========================================================

    private class IndicatorBar(
        context: Context,
        private val name: String,
        private val color: Int
    ) : LinearLayout(context) {

        private val valueView =
            TextView(context)

        private val barView =
            View(context)

        init {

            orientation =
                VERTICAL

            gravity =
                Gravity.CENTER_VERTICAL

            setPadding(
                dp(2),
                dp(2),
                dp(2),
                dp(2)
            )

            addView(
                TextView(context).apply {

                    text =
                        name

                    textSize = 9f

                    setTextColor(
                        Color.rgb(
                            165,
                            185,
                            215
                        )
                    )

                    setTypeface(
                        null,
                        Typeface.BOLD
                    )
                }
            )

            addView(
                barView,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    dp(7)
                ).apply {

                    topMargin =
                        dp(4)

                    bottomMargin =
                        dp(3)
                }
            )

            addView(
                valueView.apply {

                    text = "--"
                    textSize = 11f
                    setTextColor(color)

                    setTypeface(
                        null,
                        Typeface.BOLD
                    )
                }
            )
        }

        fun setValue(
            value: Double
        ) {

            valueView.text =
                String.format(
                    Locale.US,
                    "%.1f",
                    value
                )

            val clamped =
                value.coerceIn(
                    0.0,
                    100.0
                )

            barView.background =
                GradientDrawable().apply {

                    setColor(color)

                    cornerRadius =
                        dp(4).toFloat()
                }

            barView.scaleX =
                clamped.toFloat() / 100f

            barView.pivotX =
                0f
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

    // =========================================================
    // CÍRCULO DA DECISÃO
    // =========================================================

    private class DecisionCircle(
        context: Context
    ) : View(context) {

        var value: Double = 0.0
            set(newValue) {

                field =
                    newValue.coerceIn(
                        0.0,
                        100.0
                    )

                invalidate()
            }

        var accent: Int =
            Color.WHITE

            set(newValue) {

                field =
                    newValue

                invalidate()
            }

        private val paint =
            Paint(
                Paint.ANTI_ALIAS_FLAG
            )

        override fun onDraw(
            canvas: Canvas
        ) {

            super.onDraw(canvas)

            val centerX =
                width / 2f

            val centerY =
                height / 2f

            val radius =
                minOf(
                    width,
                    height
                ) / 2f -
                    dp(8)

            paint.style =
                Paint.Style.STROKE

            paint.strokeWidth =
                dp(8).toFloat()

            paint.strokeCap =
                Paint.Cap.ROUND

            paint.color =
                Color.rgb(
                    12,
                    58,
                    80
                )

            canvas.drawCircle(
                centerX,
                centerY,
                radius,
                paint
            )

            paint.color =
                accent

            canvas.drawArc(
                centerX - radius,
                centerY - radius,
                centerX + radius,
                centerY + radius,
                -90f,
                (
                    360f *
                        (
                            value /
                                100f
                        )
                    ).toFloat(),
                false,
                paint
            )

            paint.style =
                Paint.Style.FILL

            paint.textAlign =
                Paint.Align.CENTER

            paint.color =
                Color.WHITE

            paint.textSize =
                dp(16).toFloat()

            canvas.drawText(
                String.format(
                    Locale.US,
                    "%.1f%%",
                    value
                ),
                centerX,
                centerY + dp(5),
                paint
            )

            paint.textSize =
                dp(8).toFloat()

            paint.color =
                Color.rgb(
                    165,
                    185,
                    215
                )

            canvas.drawText(
                "TOTAL",
                centerX,
                centerY + dp(20),
                paint
            )
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
}
