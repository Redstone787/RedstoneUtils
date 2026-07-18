package org.main.redstoneutils.server.clock;

import net.minecraft.network.chat.Component;
import java.math.BigDecimal;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public record ClockInterval(int ticks) {

    public static final int TICKS_PER_SECOND = 10;
    public static final int MIN_COMPARATOR_TICKS = 2;
    public static final int COMPARATOR_TICK_STEP = 2;
    public static final int MAX_COMPARATOR_TICKS = 60 * TICKS_PER_SECOND;

    private static final Pattern INTERVAL_PATTERN = Pattern.compile("([0-9]+(?:\\.[0-9]+)?)([ts]?)");
    private static final BigDecimal TICKS_PER_SECOND_DECIMAL = BigDecimal.valueOf(TICKS_PER_SECOND);

    public static ParseResult parse(String value) {
        ParseResult parsed = parseValue(value, MAX_COMPARATOR_TICKS);
        if (!parsed.successful()) return parsed;

        int ticks = parsed.interval().ticks();
        if (ticks < MIN_COMPARATOR_TICKS || ticks % COMPARATOR_TICK_STEP != 0) {
            return ParseResult.failure(Component.translatable("message.redstoneutils.clock.comparator_range"));
        }

        return parsed;
    }

    private static ParseResult parseValue(String value, int maximumTicks) {
        String normalized = value == null ? "" : value.toLowerCase(Locale.ROOT);
        Matcher matcher = INTERVAL_PATTERN.matcher(normalized);
        if (!matcher.matches()) {
            return ParseResult.failure(Component.translatable("message.redstoneutils.clock.invalid_interval"));
        }

        BigDecimal amount;
        try {
            amount = new BigDecimal(matcher.group(1));
        } catch (NumberFormatException ignored) {
            return ParseResult.failure(Component.translatable("message.redstoneutils.clock.too_large"));
        }

        if (amount.signum() <= 0) {
            return ParseResult.failure(Component.translatable("message.redstoneutils.clock.positive"));
        }

        boolean seconds = matcher.group(2).equals("s");
        BigDecimal ticksValue = seconds ? amount.multiply(TICKS_PER_SECOND_DECIMAL) : amount;
        if (ticksValue.compareTo(BigDecimal.valueOf(maximumTicks)) > 0) {
            return ParseResult.failure(Component.translatable(
                    "message.redstoneutils.clock.maximum",
                    maximumTicks,
                    BigDecimal.valueOf(maximumTicks)
                            .divide(TICKS_PER_SECOND_DECIMAL)
                            .stripTrailingZeros()
                            .toPlainString()
            ));
        }

        int ticks;
        try {
            ticks = ticksValue.intValueExact();
        } catch (ArithmeticException ignored) {
            return ParseResult.failure(Component.translatable(seconds
                    ? "message.redstoneutils.clock.whole_seconds"
                    : "message.redstoneutils.clock.whole_ticks"));
        }

        return ParseResult.success(new ClockInterval(ticks));
    }

    public record ParseResult(ClockInterval interval, Component error) {

        private static ParseResult success(ClockInterval interval) {
            return new ParseResult(interval, null);
        }

        private static ParseResult failure(Component error) {
            return new ParseResult(null, error);
        }

        public boolean successful() {
            return interval != null;
        }
    }
}
