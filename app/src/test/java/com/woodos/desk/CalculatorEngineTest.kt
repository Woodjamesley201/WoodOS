package com.woodos.desk

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CalculatorEngineTest {

    private fun eval(s: String) = CalculatorEngine.evaluate(s)

    @Test fun basicOperations() {
        assertEquals(7.0, eval("3+4")!!, 1e-9)
        assertEquals(-1.0, eval("3-4")!!, 1e-9)
        assertEquals(12.0, eval("3*4")!!, 1e-9)
        assertEquals(2.5, eval("5/2")!!, 1e-9)
        assertEquals(1.0, eval("5%2")!!, 1e-9)
    }

    @Test fun precedence() {
        assertEquals(14.0, eval("2+3*4")!!, 1e-9)
        assertEquals(20.0, eval("(2+3)*4")!!, 1e-9)
        assertEquals(13.0, eval("2+3*4-5/5")!!, 1e-9)
    }

    @Test fun parenthesesNesting() {
        assertEquals(23.0, eval("((1+2)*(3+4))+2")!!, 1e-9)
    }

    @Test fun unaryMinus() {
        assertEquals(-4.0, eval("-4")!!, 1e-9)
        assertEquals(1.0, eval("-3+4")!!, 1e-9)
        assertEquals(-12.0, eval("-(3*4)")!!, 1e-9)
    }

    @Test fun decimals() {
        assertEquals(0.3, eval("0.1+0.2")!!, 1e-9)
        assertEquals(1.5, eval("3*0.5")!!, 1e-9)
    }

    @Test fun whitespace() {
        assertEquals(10.0, eval(" 5 + 5 ")!!, 1e-9)
    }

    @Test fun divisionByZeroIsError() {
        assertNull(eval("1/0"))
        assertNull(eval("5/(3-3)"))
    }

    @Test fun invalidExpressionsAreError() {
        assertNull(eval(""))
        assertNull(eval("3++"))
        assertNull(eval("(1+2"))
        assertNull(eval("abc"))
        assertNull(eval("1+2)"))
    }

    @Test fun formatting() {
        assertEquals("7", CalculatorEngine.format(7.0))
        assertEquals("2.5", CalculatorEngine.format(2.5))
    }
}
