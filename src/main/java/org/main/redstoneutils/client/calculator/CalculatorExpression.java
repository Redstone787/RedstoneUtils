package org.main.redstoneutils.client.calculator;

import net.minecraft.network.chat.Component;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

final class CalculatorExpression {

    private CalculatorExpression() {
    }

    static double evaluate(String input, double lastAnswer) {
        Parser parser = new Parser(input, lastAnswer);
        double value = parser.parseExpression();
        parser.skipWhitespace();

        if (parser.hasRemaining()) throw parser.error(message("unexpected"));
        if (!Double.isFinite(value)) throw parser.error(message("not_finite"));

        return value;
    }

    static String formatNumber(double value) {
        if (!Double.isFinite(value)) throw new IllegalArgumentException(message("not_finite"));
        if (Math.abs(value) < 1.0E-12D) value = 0.0D;

        if (Math.abs(value) < 1.0E15D && value == Math.rint(value)) {
            return Long.toString((long) value);
        }

        String plain = BigDecimal.valueOf(value).stripTrailingZeros().toPlainString();
        if (plain.length() <= 18) return plain;

        DecimalFormat format = new DecimalFormat("0.############E0", DecimalFormatSymbols.getInstance(Locale.ROOT));
        return format.format(value);
    }

    private static final class Parser {

        private final String input;
        private final double lastAnswer;
        private int index;

        private Parser(String input, double lastAnswer) {
            this.input = input == null ? "" : input;
            this.lastAnswer = lastAnswer;
        }

        private double parseExpression() {
            double value = parseTerm();

            while (true) {
                skipWhitespace();
                if (match('+')) {
                    value += parseTerm();
                } else if (match('-')) {
                    value -= parseTerm();
                } else {
                    return value;
                }
            }
        }

        private double parseTerm() {
            double value = parseUnary();

            while (true) {
                skipWhitespace();
                if (match('*')) {
                    value *= parseUnary();
                } else if (match('/')) {
                    double divisor = parseUnary();
                    if (divisor == 0.0D) throw error(message("division_zero"));
                    value /= divisor;
                } else if (match('%')) {
                    double divisor = parseUnary();
                    if (divisor == 0.0D) throw error(message("division_zero"));
                    value %= divisor;
                } else {
                    return value;
                }
            }
        }

        private double parsePower() {
            double value = parsePrimary();
            skipWhitespace();

            if (match('^')) {
                value = Math.pow(value, parseUnary());
            }

            return value;
        }

        private double parseUnary() {
            skipWhitespace();

            if (match('+')) return parseUnary();
            if (match('-')) return -parseUnary();

            return parsePower();
        }

        private double parsePrimary() {
            skipWhitespace();

            if (match('(')) {
                double value = parseExpression();
                expect(')');
                return value;
            }

            if (peekDigitOrDot()) {
                return parseNumber();
            }

            if (peekLetter()) {
                return parseWord();
            }

            throw error(message("expected_number"));
        }

        private double parseNumber() {
            int start = index;
            boolean hasDot = false;

            while (hasRemaining()) {
                char current = input.charAt(index);
                if (Character.isDigit(current)) {
                    index++;
                } else if (current == '.' && !hasDot) {
                    hasDot = true;
                    index++;
                } else {
                    break;
                }
            }

            if (start == index || ".".equals(input.substring(start, index))) throw error(message("expected_number"));
            return Double.parseDouble(input.substring(start, index));
        }

        private double parseWord() {
            int start = index;
            while (hasRemaining() && Character.isLetter(input.charAt(index))) {
                index++;
            }

            String word = input.substring(start, index).toLowerCase(Locale.ROOT);
            return switch (word) {
                case "ans" -> lastAnswer;
                case "sqrt" -> parseSqrt();
                default -> throw error(message("unknown_function"));
            };
        }

        private double parseSqrt() {
            skipWhitespace();

            double value;
            if (match('(')) {
                value = parseExpression();
                expect(')');
            } else {
                value = parseUnary();
            }

            if (value < 0.0D) throw error(message("negative_sqrt"));
            return Math.sqrt(value);
        }

        private boolean peekDigitOrDot() {
            return hasRemaining() && (Character.isDigit(input.charAt(index)) || input.charAt(index) == '.');
        }

        private boolean peekLetter() {
            return hasRemaining() && Character.isLetter(input.charAt(index));
        }

        private void expect(char expected) {
            if (!match(expected)) throw error(Component.translatable("calculator.redstoneutils.error.expected", expected).getString());
        }

        private boolean match(char expected) {
            skipWhitespace();
            if (!hasRemaining() || input.charAt(index) != expected) return false;

            index++;
            return true;
        }

        private void skipWhitespace() {
            while (hasRemaining() && Character.isWhitespace(input.charAt(index))) {
                index++;
            }
        }

        private boolean hasRemaining() {
            return index < input.length();
        }

        private IllegalArgumentException error(String message) {
            return new IllegalArgumentException(Component.translatable("calculator.redstoneutils.error.position", message, index).getString());
        }
    }

    private static String message(String suffix) {
        return Component.translatable("calculator.redstoneutils.error." + suffix).getString();
    }
}
