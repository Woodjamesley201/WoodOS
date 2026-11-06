package dev.woodos.launcher.core

/**
 * Small shunting-yard expression evaluator used by the WoodDesk calculator.
 * Supports + - * / % ^, parentheses, unary minus and decimals.
 */
object CalcEngine {

    fun evaluate(expr: String): Double {
        val tokens = tokenize(expr)
        if (tokens.isEmpty()) throw IllegalArgumentException("empty expression")
        val rpn = toRpn(tokens)
        return evalRpn(rpn)
    }

    fun evaluateOrNull(expr: String): Double? = try {
        evaluate(expr)
    } catch (_: Exception) {
        null
    }

    private sealed class Tok {
        data class Num(val v: Double) : Tok()
        data class Op(val o: Char) : Tok()
        object LParen : Tok()
        object RParen : Tok()
    }

    private fun tokenize(s: String): List<Tok> {
        val out = ArrayList<Tok>()
        var i = 0
        while (i < s.length) {
            val c = s[i]
            when {
                c.isWhitespace() -> i++
                c.isDigit() || c == '.' -> {
                    val sb = StringBuilder()
                    while (i < s.length && (s[i].isDigit() || s[i] == '.')) {
                        sb.append(s[i]); i++
                    }
                    out.add(Tok.Num(sb.toString().toDouble()))
                }
                c == '(' -> { out.add(Tok.LParen); i++ }
                c == ')' -> { out.add(Tok.RParen); i++ }
                c in "+-*/%^" -> {
                    // unary minus: start of expr, after op or after '('
                    if (c == '-' && (out.isEmpty() || out.last() is Tok.Op || out.last() == Tok.LParen)) {
                        out.add(Tok.Num(0.0))
                        out.add(Tok.Op('-'))
                    } else {
                        out.add(Tok.Op(c))
                    }
                    i++
                }
                else -> throw IllegalArgumentException("bad char '$c'")
            }
        }
        return out
    }

    private fun prec(op: Char): Int = when (op) {
        '+', '-' -> 1
        '*', '/', '%' -> 2
        '^' -> 3
        else -> 0
    }

    private fun toRpn(tokens: List<Tok>): List<Tok> {
        val out = ArrayList<Tok>()
        val ops = ArrayDeque<Tok>()
        for (t in tokens) {
            when (t) {
                is Tok.Num -> out.add(t)
                is Tok.LParen -> ops.addLast(t)
                is Tok.RParen -> {
                    while (ops.isNotEmpty() && ops.last() != Tok.LParen) out.add(ops.removeLast())
                    if (ops.isEmpty()) throw IllegalArgumentException("mismatched parens")
                    ops.removeLast()
                }
                is Tok.Op -> {
                    while (ops.isNotEmpty() && ops.last() is Tok.Op &&
                        (prec((ops.last() as Tok.Op).o) >= prec(t.o) && t.o != '^')
                    ) out.add(ops.removeLast())
                    ops.addLast(t)
                }
            }
        }
        while (ops.isNotEmpty()) {
            val o = ops.removeLast()
            if (o == Tok.LParen) throw IllegalArgumentException("mismatched parens")
            out.add(o)
        }
        return out
    }

    private fun evalRpn(rpn: List<Tok>): Double {
        val st = ArrayDeque<Double>()
        for (t in rpn) {
            when (t) {
                is Tok.Num -> st.addLast(t.v)
                is Tok.Op -> {
                    if (st.size < 2) throw IllegalArgumentException("bad expression")
                    val b = st.removeLast()
                    val a = st.removeLast()
                    st.addLast(
                        when (t.o) {
                            '+' -> a + b
                            '-' -> a - b
                            '*' -> a * b
                            '/' -> if (b == 0.0) throw ArithmeticException("divide by zero") else a / b
                            '%' -> a % b
                            '^' -> Math.pow(a, b)
                            else -> throw IllegalArgumentException("bad op")
                        }
                    )
                }
                else -> throw IllegalArgumentException("bad token")
            }
        }
        if (st.size != 1) throw IllegalArgumentException("bad expression")
        return st.last()
    }

    fun format(d: Double): String {
        if (d.isNaN() || d.isInfinite()) return "Error"
        return if (d == Math.floor(d) && Math.abs(d) < 1e15) {
            d.toLong().toString()
        } else {
            val s = String.format(java.util.Locale.US, "%.6g", d)
            if (s.contains('.')) s.trimEnd('0').trimEnd('.') else s
        }
    }
}
