package io.github.sanchitsinghal630_afk.simplecalc

import android.app.Activity
import android.content.res.Configuration
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import java.math.BigDecimal
import java.math.MathContext

class MainActivity : Activity() {
    private var cur = "0"
    private var prev = 0.0
    private var op: String? = null
    private var fresh = false
    private var err: String? = null
    private var expr = ""

    private lateinit var valView: TextView
    private lateinit var exprView: TextView
    private lateinit var acButton: Button
    private val opButtons = mutableMapOf<String, Button>()

    private val fnKeys = setOf("AC", "⌫", "%", "±")
    private val opKeys = setOf("÷", "×", "−", "+")

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val night = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
            Configuration.UI_MODE_NIGHT_YES
        val bg = if (night) 0xFF17141F.toInt() else 0xFFECE8F3.toInt()
        val ink = if (night) 0xFFF3F0FA.toInt() else 0xFF1F1B2E.toInt()
        val muted = if (night) 0xFF9A92B3.toInt() else 0xFF7A7390.toInt()
        val keyBg = if (night) 0xFF2A2538.toInt() else 0xFFFFFFFF.toInt()
        val fnBg = if (night) 0xFF3A3350.toInt() else 0xFFDCD5EA.toInt()
        val opBg = if (night) 0xFF8E7BFF.toInt() else 0xFF5B3FD4.toInt()
        val opInk = if (night) 0xFF17141F.toInt() else 0xFFFFFFFF.toInt()
        val eqBg = 0xFFFFC857.toInt()
        val eqInk = 0xFF1F1B2E.toInt()

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(bg)
            fitsSystemWindows = true
        }
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(10), dp(10), dp(10), dp(10))
        }
        root.addView(content, LinearLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT))

        val screen = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.BOTTOM or Gravity.END
            setPadding(dp(14), 0, dp(14), dp(16))
        }
        exprView = TextView(this).apply {
            textSize = 18f; setTextColor(muted); gravity = Gravity.END
        }
        valView = TextView(this).apply {
            textSize = 64f; setTextColor(ink); gravity = Gravity.END
            typeface = Typeface.create("sans-serif-light", Typeface.NORMAL)
        }
        screen.addView(exprView, LinearLayout.LayoutParams(MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))
        screen.addView(valView, LinearLayout.LayoutParams(MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))
        content.addView(screen, LinearLayout.LayoutParams(MATCH_PARENT, 0, 1f))

        val rows = listOf(
            listOf("AC", "⌫", "%", "÷"),
            listOf("7", "8", "9", "×"),
            listOf("4", "5", "6", "−"),
            listOf("1", "2", "3", "+"),
            listOf("±", "0", ".", "="),
        )
        for (row in rows) {
            val line = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            for (k in row) {
                val (fill, text) = when {
                    k == "=" -> eqBg to eqInk
                    k in opKeys -> opBg to opInk
                    k in fnKeys -> fnBg to ink
                    else -> keyBg to ink
                }
                val b = Button(this).apply {
                    this.text = k
                    textSize = 26f
                    isAllCaps = false
                    stateListAnimator = null
                    setTextColor(text)
                    background = GradientDrawable().apply {
                        cornerRadius = dp(24).toFloat()
                        setColor(fill)
                    }
                    contentDescription = when (k) {
                        "AC" -> "Clear"; "⌫" -> "Delete"; "÷" -> "Divide"; "×" -> "Multiply"
                        "−" -> "Subtract"; "+" -> "Add"; "=" -> "Equals"; "±" -> "Toggle sign"
                        "%" -> "Percent"; "." -> "Decimal point"; else -> k
                    }
                    setOnClickListener { press(k) }
                }
                if (k == "AC") acButton = b
                if (k in opKeys) opButtons[k] = b
                val lp = LinearLayout.LayoutParams(0, dp(72), 1f)
                lp.setMargins(dp(5), dp(5), dp(5), dp(5))
                line.addView(b, lp)
            }
            content.addView(line, LinearLayout.LayoutParams(MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))
        }
        setContentView(root)
        render()
    }

    // ---- formatting ----
    private fun fmt(n: Double): String {
        if (!n.isFinite()) return "0"
        val bd = BigDecimal(n).round(MathContext(12)).stripTrailingZeros()
        return when {
            bd.signum() == 0 -> "0"
            bd.abs() >= BigDecimal("1E12") || bd.abs() < BigDecimal("1E-9") -> bd.toString()
            else -> bd.toPlainString()
        }
    }

    private fun group(s: String): String {
        if (s.contains('E')) return s
        val neg = s.startsWith("-")
        val body = if (neg) s.substring(1) else s
        val parts = body.split(".")
        val whole = parts[0].replace(Regex("\\B(?=(\\d{3})+(?!\\d))"), ",")
        val out = if (parts.size > 1) "$whole.${parts[1]}" else whole
        return if (neg) "-$out" else out
    }

    private fun render() {
        val e = err
        val show = e ?: group(cur)
        valView.text = show
        valView.textSize = when {
            e != null -> 24f
            show.length > 14 -> 32f
            show.length > 11 -> 42f
            show.length > 8 -> 52f
            else -> 64f
        }
        exprView.text = expr
        acButton.text = if (cur == "0" && op == null && err == null) "AC" else "C"
        for ((k, b) in opButtons) b.alpha = if (op == k && fresh) 0.7f else 1f
    }

    // ---- calculator logic ----
    private fun reset() { cur = "0"; prev = 0.0; op = null; fresh = false; err = null; expr = "" }

    private fun calc(a: Double, o: String, b: Double): Double? = when (o) {
        "+" -> a + b
        "−" -> a - b
        "×" -> a * b
        "÷" -> if (b == 0.0) null else a / b
        else -> null
    }

    private fun fail() {
        err = "Can’t divide by zero"; expr = ""; op = null; fresh = true
    }

    private fun press(k: String) {
        when {
            k.length == 1 && k[0].isDigit() -> {
                if (err != null) reset()
                if (fresh) { cur = k; fresh = false }
                else if (cur == "0") cur = k
                else if (cur.count { it.isDigit() } < 12) cur += k
            }
            k == "." -> {
                if (err != null) reset()
                if (fresh) { cur = "0."; fresh = false }
                else if (!cur.contains('.')) cur += "."
            }
            k in opKeys -> {
                if (err == null) {
                    val o = op
                    if (o != null && fresh) {
                        op = k; expr = fmt(prev) + " " + k
                    } else {
                        if (o != null) {
                            val r = calc(prev, o, cur.toDouble())
                            if (r == null) { fail(); render(); return }
                            cur = fmt(r)
                        }
                        prev = cur.toDouble(); op = k; fresh = true
                        expr = fmt(prev) + " " + k
                    }
                }
            }
            k == "=" -> {
                val o = op
                if (err == null && o != null) {
                    val b = cur.toDouble()
                    val r = calc(prev, o, b)
                    if (r == null) { fail() }
                    else {
                        expr = fmt(prev) + " " + o + " " + fmt(b) + " ="
                        cur = fmt(r); op = null; fresh = true
                    }
                }
            }
            k == "AC" -> reset()
            k == "⌫" -> {
                if (err != null) reset()
                else if (!fresh) {
                    cur = cur.dropLast(1)
                    if (cur.isEmpty() || cur == "-") cur = "0"
                }
            }
            k == "%" -> if (err == null) { cur = fmt(cur.toDouble() / 100); fresh = true }
            k == "±" -> if (err == null && cur != "0") {
                cur = if (cur.startsWith("-")) cur.substring(1) else "-$cur"
            }
        }
        render()
    }
}
