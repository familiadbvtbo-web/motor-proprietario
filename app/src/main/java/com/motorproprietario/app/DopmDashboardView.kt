package com.motorproprietario.app

import android.app.AlertDialog
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.widget.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * DOPM - Dashboard
 *
 * SOMENTE INTERFACE.
 *
 * Nenhum cálculo matemático é realizado aqui.
 * Os resultados são recebidos do Controller,
 * que por sua vez recebe os resultados dos motores.
 */
class DopmDashboardView(
    context: Context
) : FrameLayout(context) {

    // =========================================================
    // CORES
    // =========================================================

    private val bg =
        Color.rgb(2, 10, 22)

    private val panel =
        Color.rgb(4, 21, 40)

    private val dark =
        Color.rgb(3, 17, 32)

    private val border =
        Color.rgb(0, 82, 145)

    private val green =
        Color.rgb(0, 235, 125)

    private val red =
        Color.rgb(255, 65, 75)

    private val blue =
        Color.rgb(35, 150, 255)

    private val cyan =
        Color.rgb(0, 220, 230)

    private val purple =
        Color.rgb(155, 65, 255)

    private val yellow =
        Color.rgb(255, 190, 30)

    private val white =
        Color.WHITE

    private val gray =
        Color.rgb(165, 185, 215)

    private val neutral =
        Color.rgb(170, 195, 230)

    // =========================================================
    // ROOT
    // =========================================================

    private val scroll =
        ScrollView(context)

    private val content =
        LinearLayout(context)

    private val bottomNavigation =
        LinearLayout(context)

    // =========================================================
    // HEADER
    // =========================================================

    private val onlineView =
        TextView(context)

    private val clockView =
        TextView(context)

    private val dateView =
        TextView(context)

    private val apiView =
        TextView(context)

    private val priceView =
        TextView(context)

    // =========================================================
    // SELETORES
    // =========================================================

    private val assetSpinner =
        Spinner(context)

    private val timeframeSpinner =
        Spinner(context)

    private val bestTimeframeView =
        TextView(context)

    private val marketButtons =
        LinkedHashMap<String, TextView>()

    private var onMarketChanged:
        ((String) -> Unit)? = null

    private var onAssetChanged:
        ((String) -> Unit)? = null

    private var onTimeframeChanged:
        ((String) -> Unit)? = null

    private var suppressSelection =
        true

    // =========================================================
    // DECISÃO
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

    private val decisionIcon =
        TextView(context)

    private val decisionCircle =
        DecisionCircle(context)

    // =========================================================
    // PLANO
    // =========================================================

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

    // =========================================================
    // CONFIANÇA
    // =========================================================

    private val probabilityView =
        TextView(context)

    private val deterministicView =
        TextView(context)

    private val mtfView =
        TextView(context)

    // =========================================================
    // INIT
    // =========================================================

    init {

        setBackgroundColor(
            bg
        )

        build()

        postDelayed({

            suppressSelection =
                false

        }, 500L)
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

            text =
                value

            textSize =
                size

            setTextColor(
                color
            )

            includeFontPadding =
                true

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

            setColor(
                fill
            )

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

        content.setBackgroundColor(
            bg
        )

        scroll.isFillViewport =
            true

        scroll.addView(

            content,

            ScrollView.LayoutParams(
                MATCH_PARENT,
                WRAP_CONTENT
            )
        )

        addView(

            scroll,

            LayoutParams(
                MATCH_PARENT,
                MATCH_PARENT
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

            text =
                "● ONLINE"

            textSize =
                12f

            setTextColor(
                green
            )

            setTypeface(
                null,
                Typeface.BOLD
            )

            gravity =
                Gravity.END
        }

        clockView.apply {

            text =
                "◷ --:--:--"

            textSize =
                10f

            setTextColor(
                white
            )

            gravity =
                Gravity.END
        }

        dateView.apply {

            text =
                "▣ --/--/----"

            textSize =
                10f

            setTextColor(
                white
            )

            gravity =
                Gravity.END
        }

        apiView.apply {

            text =
                "API DE DADOS • TWELVE DATA"

            textSize =
                9f

            setTextColor(
                blue
            )

            gravity =
                Gravity.END

            maxLines =
                1

            ellipsize =
                TextUtils.TruncateAt.END
        }

        priceView.apply {

            text =
                "Preço: --"

            textSize =
                9f

            setTextColor(
                gray
            )

            gravity =
                Gravity.END
        }

        status.addView(
            onlineView
        )

        status.addView(
            clockView
        )

        status.addView(
            dateView
        )

        status.addView(
            apiView
        )

        status.addView(
            priceView
        )

        row.addView(

            status,

            LinearLayout.LayoutParams(
                dp(155),
                dp(78)
            )
        )

        content.addView(
            row
        )

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

        val market =
            card()

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

            marketButtons[
                pair.first
            ] =
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

        market.addView(
            marketRow
        )

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

        // -----------------------------------------------------
        // ATIVO
        // -----------------------------------------------------

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
            0
        )

        assetSpinner.onItemSelectedListener =
            object :
                AdapterView.OnItemSelectedListener {

                override fun onItemSelected(

                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long

                ) {

                    if (
                        !suppressSelection
                    ) {

                        onAssetChanged?.invoke(
                            assets[position]
                        )
                    }
                }

                override fun onNothingSelected(
                    parent: AdapterView<*>?
                ) {
                }
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

        // -----------------------------------------------------
        // TIMEFRAME
        // -----------------------------------------------------

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

        timeframeSpinner.setSelection(
            2
        )

        timeframeSpinner.onItemSelectedListener =
            object :
                AdapterView.OnItemSelectedListener {

                override fun onItemSelected(

                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long

                ) {

                    if (
                        !suppressSelection
                    ) {

                        onTimeframeChanged?.invoke(
                            timeframes[position]
                        )
                    }
                }

                override fun onNothingSelected(
                    parent: AdapterView<*>?
                ) {
                }
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

        content.addView(
            row
        )

        gap()

        // -----------------------------------------------------
        // MELHOR TIMEFRAME
        // -----------------------------------------------------

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

            text =
                "--"

            textSize =
                22f

            setTextColor(
                green
            )

            setTypeface(
                null,
                Typeface.BOLD
            )
        }

        bestText.addView(
            bestTimeframeView
        )

        bestRow.addView(
            bestText
        )

        best.addView(
            bestRow
        )

        content.addView(

            best,

            LinearLayout.LayoutParams(
                MATCH_PARENT,
                dp(68)
            )
        )

        gap()

        selectMarket(
            "FOREX"
        )
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
                    MATCH_PARENT,
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
                entry.key ==
                    market

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

            text =
                "→"

            textSize =
                30f

            gravity =
                Gravity.CENTER

            setTextColor(
                white
            )

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

            text =
                "AGUARDAR"

            textSize =
                23f

            setTextColor(
                white
            )

            setTypeface(
                null,
                Typeface.BOLD
            )

            maxLines =
                1

            ellipsize =
                TextUtils.TruncateAt.END

            includeFontPadding =
                false
        }

        decision.addView(
            decisionView
        )

        totalView.apply {

            text =
                "TOTAL: ---%"

            textSize =
                14f

            setTextColor(
                white
            )

            setTypeface(
                null,
                Typeface.BOLD
            )

            maxLines =
                1
        }

        decision.addView(
            totalView
        )

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

            text =
                "● COMPRA ---%"

            textSize =
                11f

            setTextColor(
                green
            )

            maxLines =
                1

            includeFontPadding =
                false
        }

        sellView.apply {

            text =
                "● VENDA ---%"

            textSize =
                11f

            setTextColor(
                red
            )

            maxLines =
                1

            includeFontPadding =
                false
        }

        neutralView.apply {

            text =
                "● NEUTRO ---%"

            textSize =
                11f

            setTextColor(
                neutral
            )

            maxLines =
                1

            includeFontPadding =
                false
        }

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
                0,
                dp(84),
                1.15f
            )
        )

        box.addView(
            row
        )

        content.addView(

            box,

            LinearLayout.LayoutParams(
                MATCH_PARENT,
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
        value: TextView,
        color: Int
    ): LinearLayout {

        val box =
            card()

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
                22f,
                color,
                true
            ),

            LinearLayout.LayoutParams(
                dp(30),
                MATCH_PARENT
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

        value.apply {

            text =
                initial

            textSize =

                if (
                    title ==
                        "TAKE PROFIT"
                ) {

                    13f

                } else {

                    17f
                }

            setTextColor(
                white
            )

            setTypeface(
                null,
                Typeface.BOLD
            )

            maxLines =
                3

            ellipsize =
                TextUtils.TruncateAt.END
        }

        column.addView(
            value
        )

        row.addView(

            column,

            LinearLayout.LayoutParams(
                0,
                MATCH_PARENT,
                1f
            )
        )

        box.addView(
            row
        )

        return box
    }

    // =========================================================
    // TRADE PLAN
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
                dp(94),
                1f
            ).apply {

                marginEnd =
                    dp(4)
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
                dp(94),
                1f
            ).apply {

                marginEnd =
                    dp(4)
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
                dp(94),
                1.25f
            )
        )

        content.addView(
            row
        )

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
                dp(80),
                1f
            ).apply {

                marginEnd =
                    dp(4)
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
                dp(80),
                1f
            )
        )

        content.addView(
            row
        )

        gap()
    }

    // =========================================================
    // CONFIANÇA
    // =========================================================

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
                    8f,
                    gray,
                    true
                )
            )

            value.apply {

                text =
                    "--"

                textSize =
                    18f

                setTextColor(
                    color
                )

                setTypeface(
                    null,
                    Typeface.BOLD
                )
            }

            addView(
                value
            )
        }
    }

    private fun buildConfidence() {

        val box =
            card()

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
                MATCH_PARENT,
                1f
            ).apply {

                marginEnd =
                    dp(4)
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
                MATCH_PARENT,
                1f
            ).apply {

                marginEnd =
                    dp(4)
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
                MATCH_PARENT,
                1f
            )
        )

        box.addView(
            row
        )

        content.addView(

            box,

            LinearLayout.LayoutParams(
                MATCH_PARENT,
                dp(72)
            )
        )

        gap()
    }

    // =========================================================
    // INDICADORES
    // =========================================================

    private fun buildIndicators() {

        val box =
            card()

        val row =
            LinearLayout(context).apply {

                orientation =
                    LinearLayout.HORIZONTAL
            }

        val indicators =
            listOf(

                "FI" to green,

                "FSI" to blue,

                "RSI" to purple,

                "MACD" to cyan,

                "EMA" to
                    Color.rgb(
                        255,
                        110,
                        50
                    ),

                "ADX" to yellow
            )

        indicators.forEachIndexed {

            index,
            pair ->

            val item =
                LinearLayout(context).apply {

                    orientation =
                        LinearLayout.VERTICAL

                    gravity =
                        Gravity.CENTER_VERTICAL

                    setPadding(
                        dp(1),
                        0,
                        dp(1),
                        0
                    )
                }

            item.addView(

                txt(
                    pair.first,
                    8f,
                    gray,
                    true
                )
            )

            val bar =
                ProgressBar(
                    context,
                    null,
                    android.R.attr.progressBarStyleHorizontal
                ).apply {

                    max =
                        100

                    progress =
                        0

                    isIndeterminate =
                        false
                }

            item.addView(

                bar,

                LinearLayout.LayoutParams(
                    MATCH_PARENT,
                    dp(6)
                ).apply {

                    topMargin =
                        dp(3)

                    bottomMargin =
                        dp(2)
                }
            )

            item.addView(

                txt(
                    "--",
                    9f,
                    pair.second,
                    true
                )
            )

            row.addView(

                item,

                LinearLayout.LayoutParams(
                    0,
                    MATCH_PARENT,
                    1f
                ).apply {

                    if (
                        index < 5
                    ) {

                        marginEnd =
                            dp(3)
                    }
                }
            )
        }

        box.addView(
            row
        )

        content.addView(

            box,

            LinearLayout.LayoutParams(
                MATCH_PARENT,
                dp(68)
            )
        )

        gap()
    }

    // =========================================================
    // ANÁLISE DETALHADA
    // =========================================================

    private fun buildDetailed() {

        val box =
            card()

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
                28f,
                cyan,
                true
            ),

            LinearLayout.LayoutParams(
                dp(42),
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
                14f,
                white,
                true
            )
        )

        title.addView(

            txt(
                "Veja todos os cálculos, indicadores e a calibração do motor.",
                8f,
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
                32f,
                gray
            ).apply {

                gravity =
                    Gravity.CENTER

                setOnClickListener {

                    showDetailed()
                }
            },

            LinearLayout.LayoutParams(
                dp(36),
                dp(50)
            )
        )

        box.addView(
            header
        )

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

        val icons =
            listOf(

                "☷",
                "◴",
                "▱",
                "♣",
                "↻",

                "∿",
                "▥",
                "↗",
                "▥",
                "♧",

                "⚠",
                "▱",
                "▱",
                "ϟ"
            )

        val grid =
            LinearLayout(context).apply {

                orientation =
                    LinearLayout.VERTICAL
            }

        names.chunked(
            5
        ).forEachIndexed {

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
                    rowIndex *
                        5 +
                        columnIndex

                val item =
                    txt(

                        "${icons[index]}\n$name",

                        7.5f,

                        white

                    ).apply {

                        gravity =
                            Gravity.CENTER

                        background =
                            rounded(
                                panel,
                                border,
                                10
                            )

                        setPadding(
                            dp(1),
                            dp(3),
                            dp(1),
                            dp(3)
                        )
                    }

                line.addView(

                    item,

                    LinearLayout.LayoutParams(
                        0,
                        dp(60),
                        1f
                    ).apply {

                        if (
                            columnIndex < 4
                        ) {

                            marginEnd =
                                dp(3)
                        }
                    }
                )
            }

            grid.addView(
                line
            )

            if (
                rowIndex < 2
            ) {

                grid.addView(

                    View(context),

                    LinearLayout.LayoutParams(
                        1,
                        dp(3)
                    )
                )
            }
        }

        box.addView(
            grid
        )

        content.addView(
            box
        )

        gap(8)
    }

    // =========================================================
    // DIALOG
    // =========================================================

    private fun showDetailed() {

        val message =
            buildString {

                append(
                    "MOTOR PROPRIETÁRIO\n\n"
                )

                append(
                    "DECISÃO: "
                )

                append(
                    decisionView.text
                )

                append(
                    "\n"
                )

                append(
                    totalView.text
                )

                append(
                    "\n"
                )

                append(
                    buyView.text
                )

                append(
                    "\n"
                )

                append(
                    sellView.text
                )

                append(
                    "\n"
                )

                append(
                    neutralView.text
                )

                append(
                    "\n\n"
                )

                append(
                    "PROBABILIDADE: "
                )

                append(
                    probabilityView.text
                )

                append(
                    "\n"
                )

                append(
                    "DETERMINISMO: "
                )

                append(
                    deterministicView.text
                )

                append(
                    "\n"
                )

                append(
                    "CONFLUÊNCIA MTF: "
                )

                append(
                    mtfView.text
                )

                append(
                    "\n\n"
                )

                append(
                    "ENTRADA: "
                )

                append(
                    entryView.text
                )

                append(
                    "\n"
                )

                append(
                    "STOP: "
                )

                append(
                    stopView.text
                )

                append(
                    "\n"
                )

                append(
                    targetsView.text
                )

                append(
                    "\n"
                )

                append(
                    "TIMING: "
                )

                append(
                    timingView.text
                )

                append(
                    "\n"
                )

                append(
                    "VALIDADE: "
                )

                append(
                    validityView.text
                )
            }

        AlertDialog.Builder(
            context
        )
            .setTitle(
                "ANÁLISE DETALHADA"
            )
            .setMessage(
                message
            )
            .setPositiveButton(
                "FECHAR",
                null
            )
            .show()
    }

    // =========================================================
    // NAVEGAÇÃO INFERIOR
    // =========================================================

    private fun buildBottomNavigation() {

        bottomNavigation.orientation =
            LinearLayout.HORIZONTAL

        bottomNavigation.gravity =
            Gravity.CENTER

        bottomNavigation.setPadding(
            0,
            dp(4),
            0,
            dp(4)
        )

        bottomNavigation.setBackgroundColor(
            bg
        )

        val items =
            listOf(

                "⌂\nPrincipal",

                "▥\nGráficos",

                "▤\nAnálise Detalhada",

                "⚙\nConfigurações"
            )

        items.forEachIndexed {

            index,
            label ->

            val item =
                txt(

                    label,

                    9f,

                    if (
                        index == 0
                    ) {
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
                        dp(4),
                        dp(2),
                        dp(4)
                    )
                }

            bottomNavigation.addView(

                item,

                LinearLayout.LayoutParams(
                    0,
                    dp(62),
                    1f
                )
            )
        }

        bottomNavigation.elevation =
            dp(8).toFloat()

        addView(

            bottomNavigation,

            LayoutParams(
                MATCH_PARENT,
                dp(70),
                Gravity.BOTTOM
            )
        )
    }

    // =========================================================
    // RELÓGIO
    // =========================================================

    private fun startClock() {

        val handler =
            android.os.Handler(
                android.os.Looper.getMainLooper()
            )

        val task =
            object : Runnable {

                override fun run() {

                    val now =
                        Date()

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

                    handler.postDelayed(
                        this,
                        1_000L
                    )
                }
            }

        handler.post(
            task
        )
    }

    // =========================================================
    // CALLBACKS
    // =========================================================

    fun setSelectionListeners(

        marketChanged:
            (String) -> Unit,

        assetChanged:
            (String) -> Unit,

        timeframeChanged:
            (String) -> Unit

    ) {

        onMarketChanged =
            marketChanged

        onAssetChanged =
            assetChanged

        onTimeframeChanged =
            timeframeChanged
    }

    // =========================================================
    // STATUS
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

    // =========================================================
    // DECISÃO
    // =========================================================

    fun setDecision(

        direction: String,

        total: Double

    ) {

        val shown =
            when (direction) {

                "COMPRA" ->
                    "COMPRA"

                "VENDA" ->
                    "VENDA"

                else ->
                    "NEUTRO"
            }

        val color =
            when (shown) {

                "COMPRA" ->
                    green

                "VENDA" ->
                    red

                else ->
                    white
            }

        decisionView.text =
            shown

        totalView.text =
            "TOTAL: ${format(total)}%"

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
            "● COMPRA  ${format(buy)}%"

        sellView.text =
            "● VENDA   ${format(sell)}%"

        neutralView.text =
            "● NEUTRO  ${format(neutral)}%"
    }

    // =========================================================
    // MATEMÁTICA RECEBIDA
    // =========================================================

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

    // =========================================================
    // MELHOR TIMEFRAME
    // =========================================================

    fun setBestTimeframe(
        value: String
    ) {

        bestTimeframeView.text =
            value
    }

    // =========================================================
    // TRADE PLAN
    // =========================================================

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
            "TP1 $tp1\n" +
                "TP2 $tp2\n" +
                "TP3 $tp3"
    }

    // =========================================================
    // TIMING
    // =========================================================

    fun setTiming(

        timing: String,

        validity: String

    ) {

        timingView.text =
            timing

        validityView.text =
            validity
    }

    // =========================================================
    // FORMATAÇÃO
    // =========================================================

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
    // CÍRCULO DA DECISÃO
    // =========================================================

    private class DecisionCircle(
        context: Context
    ) : View(context) {

        var value =
            0.0

            set(newValue) {

                field =
                    newValue.coerceIn(
                        0.0,
                        100.0
                    )

                invalidate()
            }

        var accent =
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

        private fun dp(
            value: Int
        ): Int {

            return (
                value *
                    resources.displayMetrics.density
                ).toInt()
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
                minOf(
                    width,
                    height
                ) / 2f -
                    dp(7)

            paint.style =
                Paint.Style.STROKE

            paint.strokeWidth =
                dp(7).toFloat()

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
                        value /
                        100f
                    ).toFloat(),

                false,

                paint
            )

            paint.style =
                Paint.Style.FILL

            paint.color =
                white

            paint.textAlign =
                Paint.Align.CENTER

            paint.textSize =
                dp(15).toFloat()

            canvas.drawText(

                String.format(
                    Locale.US,
                    "%.1f%%",
                    value
                ),

                centerX,

                centerY +
                    dp(4),

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

                centerY +
                    dp(20),

                paint
            )
        }
    }
}
