package dev.woodos.launcher

import dev.woodos.launcher.core.CalcEngine
import org.junit.Assert.assertEquals
import org.junit.Test

class CalcEngineTest {

    @Test
    fun basicOperations() {
        assertEquals(7.0, CalcEngine.evaluate("3+4"), 1e-9)
        assertEquals(2.0, CalcEngine.evaluate("6-4"), 1e-9)
        assertEquals(12.0, CalcEngine.evaluate("3*4"), 1e-9)
        assertEquals(2.5, CalcEngine.evaluate("5/2"), 1e-9)
    }

    @Test
    fun precedence() {
        assertEquals(14.0, CalcEngine.evaluate("2+3*4"), 1e-9)
        assertEquals(20.0, CalcEngine.evaluate("(2+3)*4"), 1e-9)
    }

    @Test
    fun unaryMinusAndParens() {
        assertEquals(-3.0, CalcEngine.evaluate("-3"), 1e-9)
        assertEquals(1.0, CalcEngine.evaluate("-(2-3)"), 1e-9)
    }

    @Test
    fun powerAndModulo() {
        assertEquals(8.0, CalcEngine.evaluate("2^3"), 1e-9)
        assertEquals(2.0, CalcEngine.evaluate("7%5"), 1e-9)
    }

    @Test
    fun invalidExpressions() {
        assertEquals(null, CalcEngine.evaluateOrNull("3+"))
        assertEquals(null, CalcEngine.evaluateOrNull("((1+2)"))
        assertEquals(null, CalcEngine.evaluateOrNull(""))
    }

    @Test
    fun formatting() {
        assertEquals("42", CalcEngine.format(42.0))
        assertEquals("3.14", CalcEngine.format(3.14))
        assertEquals("Error", CalcEngine.format(Double.NaN))
    }
}
