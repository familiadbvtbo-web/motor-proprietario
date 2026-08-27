package com.motorproprietario.app

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.widget.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DopmDashboardView(
    context: Context
) : ScrollView(context) {

    private val backgroundColor =
        Color.rgb(2, 10, 22)

    private val panelColor =
        Color.rgb(4, 21, 40)

    private val panelBorder =
        Color.rgb(0, 110, 180)

    private val green =
        Color.rgb(0, 235, 125)

    private val red =
        Color.rgb(255, 70, 80)

    private val blue =
        Color.rgb(40, 165, 255)

    private val white =
        Color.WHITE

    private val gray =
        Color.rgb(165, 185, 210)

    private val yellow =
        Color.rgb(255, 195, 40)

    private val root =
        LinearLayout(context)

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

    private val apiView =
        TextView(context)

    private val marketSpinner =
        Spinner(context)

    private val assetSpinner =
        Spinner(context)

    private val timeframeSpinner =
        Spinner(context)

    init {

        setBackgroundColor(
            backgroundColor
        )

        isFillViewport =
            true

        build()
    }

    private fun build() {

        root.orientation =
            LinearLayout.VERTICAL

        root.setPadding(
            dp(10),
            dp(8),
            dp(10),
            dp(8)
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

    private fun buildHeader() {

        val row =
            LinearLayout(context)

        row.orientation =
            LinearLayout.HORIZONTAL

        row.gravity =
            Gravity.CENTER_VERTICAL

        val brand =
            LinearLayout(context)

        brand.orientation =
            LinearLayout.VERTICAL

        brand.addView(
            label(
                "🐂  DOPM",
                24f,
                true
            )
        )

        brand.addView(
            label(
                "MOTOR PROPRIETÁRIO",
                12f,
                true
            )
        )

        brand.addView(
            label(
                "DIREÇÃO • PROBABILIDADE • DETERMINISMO",
                8f,
                false
            ).apply {
                setTextColor(blue)
            }
        )

        row.addView(
            brand,
            weightParams(
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
            11f

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

        apiView.text =
            "API: TWELVE DATA"

        apiView.textSize =
            9f

        apiView.setTextColor(
            blue
        )

        status.addView(
            apiView
        )

        clockView.text =
            "◷ --:--:--"

        clockView.textSize =
            10f

        clockView.setTextColor(
            white
        )

        status.addView(
            clockView
        )

        row.addView(
            status,
            LinearLayout.LayoutParams(
                dp(125),
                dp(65)
            )
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

    private fun buildSelectors() {

        val row =
            LinearLayout(context)

        row.orientation =
            LinearLayout.HORIZONTAL

        val markets =
            listOf(
                "FOREX",
                "CRIPTO",
                "B3"
            )

        marketSpinner.adapter =
            ArrayAdapter(
                context,
                android.R.layout.simple_spinner_dropdown_item,
                markets
            )

        row.addView(
            selector(
                "MERCADO",
                marketSpinner
            ),
            weightParams(
                1f
            )
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
                "GBP/JPY"
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

        row.addView(
            selector(
                "ATIVO",
                assetSpinner
            ),
            weightParams(
                1.3f
            )
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

        timeframeSpinner.setSelection(
            2
        )

        row.addView(
            selector(
                "TIMEFRAME",
                timeframeSpinner
            ),
            weightParams(
                1f
            )
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

        val best =
            panel(
                Color.rgb(
                    0,
                    100,
                    75
                )
            )

        best.addView(
            smallLabel(
                "MELHOR TIMEFRAME"
            )
        )

        bestTimeframeView.text =
            "M15"

        bestTimeframeView.textSize =
            17f

        bestTimeframeView.setTypeface(
            null,
            Typeface.BOLD
        )

        bestTimeframeView.setTextColor(
            green
        )

        best.addView(
            bestTimeframeView
        )

        root.addView(
            best,
            marginParams(
                0,
                0,
                0,
                5
            )
        )
    }

    private fun buildDecision() {

        val box =
            panel(
                Color.rgb(
                    0,
                    100,
                    65
                )
            )

        val row =
            LinearLayout(context)

        row.orientation =
            LinearLayout.HORIZONTAL

        row.gravity =
            Gravity.CENTER_VERTICAL

        val icon =
            label(
                "↗",
                34f,
                true
            )

        icon.gravity =
            Gravity.CENTER

        icon.setTextColor(
            Color.BLACK
        )

        icon.background =
            rounded(
                green,
                16
            )

        row.addView(
            icon,
            LinearLayout.LayoutParams(
                dp(58),
                dp(58)
            )
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
            27f

        decisionView.setTypeface(
            null,
            Typeface.BOLD
        )

        decisionView.setTextColor(
            green
        )

        decision.addView(
            decisionView
        )

        totalView.text =
            "TOTAL: ---%"

        totalView.textSize =
            15f

        totalView.setTypeface(
            null,
            Typeface.BOLD
        )

        decision.addView(
            totalView
        )

        row.addView(
            decision,
            weightParams(
                1f
            )
        )

        val circle =
            label(
                "--%\nTOTAL",
                14f,
                true
            )

        circle.gravity =
            Gravity.CENTER

        circle.setTextColor(
            green
        )

        circle.background =
            circleBackground(
                green
            )

        row.addView(
            circle,
            LinearLayout.LayoutParams(
                dp(78),
                dp(78)
            )
        )

        val breakdown =
            LinearLayout(context)

        breakdown.orientation =
            LinearLayout.VERTICAL

        buyView.text =
            "● COMPRA   ---%"

        buyView.setTextColor(
            green
        )

        sellView.text =
            "● VENDA    ---%"

        sellView.setTextColor(
            red
        )

        neutralView.text =
            "● NEUTRO   ---%"

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
            weightParams(
                1f
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
                5
            )
        )
    }

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
                entryView
            ),
            weightParams(
                1f
            )
        )

        row.addView(
            information(
                "▽",
                "STOP",
                "--",
                stopView
            ),
            weightParams(
                1f
            )
        )

        row.addView(
            information(
                "◎",
                "TAKE PROFIT",
                "TP1 --  TP2 --  TP3 --",
                targetsView
            ),
            weightParams(
                1.4f
            )
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
                timingView
            ),
            weightParams(
                1f
            )
        )

        row.addView(
            information(
                "▣",
                "VALIDADE",
                "--",
                validityView
            ),
            weightParams(
                1f
            )
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

    private fun buildConfidence() {

        val box =
            panel(
                panelBorder
            )

        val row =
            LinearLayout(context)

        row.orientation =
            LinearLayout.HORIZONTAL

        row.addView(
            confidence(
                "PROBABILIDADE",
                probabilityView
            ),
            weightParams(
                1f
            )
        )

        row.addView(
            confidence(
                "DETERMINISMO",
                deterministicView
            ),
            weightParams(
                1f
            )
        )

        row.addView(
            confidence(
                "CONFLUÊNCIA MTF",
                mtfView
            ),
            weightParams(
                1f
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
                5
            )
        )
    }

    private fun buildIndicators() {

        val box =
            panel(
                panelBorder
            )

        val row =
            LinearLayout(context)

        row.orientation =
            LinearLayout.HORIZONTAL

        listOf(
            "FI",
            "FSI",
            "RSI",
            "MACD",
            "EMA",
            "ADX"
        ).forEach {

            val item =
                LinearLayout(context)

            item.orientation =
                LinearLayout.VERTICAL

            item.gravity =
                Gravity.CENTER

            item.addView(
                smallLabel(
                    it
                )
            )

            val bar =
                ProgressBar(
                    context,
                    null,
                    android.R.attr.progressBarStyleHorizontal
                )

            bar.max =
                100

            bar.progress =
                0

            item.addView(
                bar,
                LinearLayout.LayoutParams(
                    dp(45),
                    dp(7)
                )
            )

            item.addView(
                smallLabel(
                    "--"
                )
            )

            row.addView(
                item,
                weightParams(
                    1f
                )
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
                5
            )
        )
    }

    private fun buildDetailedAnalysis() {

        val box =
            panel(
                Color.rgb(
                    5,
                    30,
                    50
                )
            )

        val row =
            LinearLayout(context)

        row.orientation =
            LinearLayout.HORIZONTAL

        val title =
            LinearLayout(context)

        title.orientation =
            LinearLayout.VERTICAL

        title.addView(
            smallLabel(
                "ANÁLISE DETALHADA"
            )
        )

        title.addView(
            label(
                "Cálculos, indicadores e calibração do motor",
                9f,
                false
            ).apply {
                setTextColor(gray)
            }
        )

        row.addView(
            title,
            weightParams(
                1f
            )
        )

        val button =
            Button(context)

        button.text =
            "ABRIR"

        button.textSize =
            9f

        button.setTextColor(
            green
        )

        button.setOnClickListener {
            showDetailedMessage()
        }

        row.addView(
            button,
            LinearLayout.LayoutParams(
                dp(75),
                dp(42)
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
                5
            )
        )
    }

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
                "▤\nAnálise",
                "⚙\nConfig."
            )

        items.forEachIndexed {
                index,
                item ->

            val view =
                label(
                    item,
                    9f,
                    true
                )

            view.gravity =
                Gravity.CENTER

            view.setTextColor(
                if (
                    index == 0
                ) {
                    green
                } else {
                    gray
                }
            )

            nav.addView(
                view,
                weightParams(
                    1f
                )
            )
        }

        root.addView(
            nav,
            LinearLayout.LayoutParams(
                -1,
                dp(52)
            )
        )
    }

    private fun selector(
        title: String,
        spinner: Spinner
    ): LinearLayout {

        val box =
            panel(
                panelBorder
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
        target: TextView
    ): LinearLayout {

        val box =
            panel(
                panelBorder
            )

        val row =
            LinearLayout(context)

        row.orientation =
            LinearLayout.HORIZONTAL

        row.gravity =
            Gravity.CENTER_VERTICAL

        row.addView(
            label(
                icon,
                20f,
                true
            ).apply {
                setTextColor(blue)
            },
            LinearLayout.LayoutParams(
                dp(28),
                dp(35)
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
            12f

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
        target: TextView
    ): LinearLayout {

        val box =
            LinearLayout(context)

        box.orientation =
            LinearLayout.VERTICAL

        box.gravity =
            Gravity.CENTER

        box.addView(
            smallLabel(
                title
            )
        )

        target.text =
            "--%"

        target.textSize =
            16f

        target.setTypeface(
            null,
            Typeface.BOLD
        )

        target.setTextColor(
            white
        )

        box.addView(
            target
        )

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

    private fun circleBackground(
        color: Int
    ): GradientDrawable {

        return GradientDrawable().apply {

            shape =
                GradientDrawable.OVAL

            setColor(
                Color.TRANSPARENT
            )

            setStroke(
                dp(7),
                color
            )
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

    private fun startClock() {

        post(
            object : Runnable {

                override fun run() {

                    clockView.text =
                        "◷ " +
                            SimpleDateFormat(
                                "HH:mm:ss",
                                Locale.getDefault()
                            ).format(
                                Date()
                            )

                    postDelayed(
                        this,
                        1000L
                    )
                }
            }
        )
    }

    private fun showDetailedMessage() {

        Toast.makeText(
            context,
            "A análise detalhada será ligada aos cálculos reais no próximo arquivo.",
            Toast.LENGTH_LONG
        ).show()
    }

    /*
     * Estes métodos serão usados no próximo passo para
     * alimentar a interface com os resultados REAIS do motor.
     */

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
                    maxOf(
                        buy,
                        sell,
                        neutral
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
}
