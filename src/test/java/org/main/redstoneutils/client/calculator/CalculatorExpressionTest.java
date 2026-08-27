package org.main.redstoneutils.client.calculator;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CalculatorExpressionTest {

    @Test
    void formattedScientificResultCanBeUsedAsInput() {
        double value = CalculatorExpression.evaluate("10^20", 0.0D);
        String formatted = CalculatorExpression.formatNumber(value);

        assertEquals(value, CalculatorExpression.evaluate(formatted + "+1", 0.0D), Math.ulp(value));
    }

    @Test
    void respectsPowerAndUnaryPrecedence() {
        assertEquals(-4.0D, CalculatorExpression.evaluate("-2^2", 0.0D));
        assertEquals(512.0D, CalculatorExpression.evaluate("2^3^2", 0.0D));
    }

    @Test
    void rejectsIncompleteExponent() {
        assertThrows(IllegalArgumentException.class, () -> CalculatorExpression.evaluate("1E+", 0.0D));
    }
}
