package com.woodos.desk

/**
 * Tiny recursive-descent expression evaluator for the WoodDesk
 * calculator. Pure Kotlin (no Android imports) so it is unit-testable
 * on the JVM. Supports + - * / % ( ) and unary minus.
 */
object CalculatorEngine {

    /** Returns the result, or null when the expression is invalid. */
    fun evaluate(expression: String): Double? {
        return runCatching {
            val tokens = tokenize(expression)
            if (tokens.isEmpty()) return null
            val parser = Parser(tokens)
            val value = parser.parseExpression()
            if (parser.pos != tokens.size) return null // trailing garbage
            if (value.isNaN() || value.isInfinite()) null else value
        }.getOrNull()
    }

    fun format(value: Double): String {
        if (value == value.toLong().toDouble() && kotlin.math.abs(value) < 1e15) {
            return value.toLong().toString()
        }
        val s = String.format(java.util.Locale.US, "%.10g", value)
        // Trim trailing zeros ("2.500000000" -> "2.5").
        return if (s.contains('.')) s.trimEnd('0').trimEnd('.') else s
    }

    private fun tokenize(s: String): List<String> {
        val tokens = mutableListOf<String>()
        var i = 0
        while (i < s.length) {
            val c = s[i]
            when {
                c.isWhitespace() -> i++
                c.isDigit() || c == '.' -> {
                    val start = i
                    while (i < s.length && (s[i].isDigit() || s[i] == '.')) i++
                    tokens.add(s.substring(start, i))
                }
                c in "+-*/%()" -> {
                    tokens.add(c.toString()); i++
                }
                else -> throw IllegalArgumentException("bad char $c")
            }
        }
        return tokens
    }

    private class Parser(private val tokens: List<String>) {
        var pos = 0

        fun parseExpression(): Double {
            var value = parseTerm()
            while (peek() == "+" || peek() == "-") {
                val op = next()
                val rhs = parseTerm()
                value = if (op == "+") value + rhs else value - rhs
            }
            return value
        }

        private fun parseTerm(): Double {
            var value = parseFactor()
            while (peek() == "*" || peek() == "/" || peek() == "%") {
                val op = next()
                val rhs = parseFactor()
                value = when (op) {
                    "*" -> value * rhs
                    "/" -> value / rhs
                    else -> value % rhs
                }
                if (value.isNaN() || value.isInfinite()) throw ArithmeticException("div by zero")
            }
            return value
        }

        private fun parseFactor(): Double {
            when (peek()) {
                "-" -> {
                    next()
                    return -parseFactor()
                }
                "(" -> {
                    next()
                    val v = parseExpression()
                    if (next() != ")") throw IllegalArgumentException("missing )")
                    return v
                }
            }
            val t = next()
            return t.toDoubleOrNull() ?: throw IllegalArgumentException("bad number $t")
        }

        private fun peek(): String? = tokens.getOrNull(pos)
        private fun next(): String = tokens.getOrNull(pos++) ?: throw IllegalStateException("eof")
    }
}
