/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package io.github.redstone787.redstonelabworks.server.clock;

import net.minecraft.network.chat.Component;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** The exact period and counter size of a classic Ethonian hopper clock. */
public record HopperClockInterval(int itemCount) {

    public static final int MIN_ITEMS = 1;
    public static final int MAX_ITEMS = 5 * 64;
    public static final int GAME_TICKS_PER_REDSTONE_TICK = 2;
    public static final int GAME_TICKS_PER_SECOND = 20;

    private static final int FIRST_ITEM_PERIOD_GAME_TICKS = 14;
    private static final int SECOND_ITEM_PERIOD_GAME_TICKS = 20;
    private static final int PERIOD_STEP_GAME_TICKS = 16;
    private static final Pattern INTERVAL_PATTERN = Pattern.compile("([0-9]+(?:\\.[0-9]+)?)([ts]?)");
    private static final BigDecimal GAME_TICKS_PER_REDSTONE_TICK_DECIMAL =
            BigDecimal.valueOf(GAME_TICKS_PER_REDSTONE_TICK);
    private static final BigDecimal GAME_TICKS_PER_SECOND_DECIMAL =
            BigDecimal.valueOf(GAME_TICKS_PER_SECOND);

    public HopperClockInterval {
        if (itemCount < MIN_ITEMS || itemCount > MAX_ITEMS) {
            throw new IllegalArgumentException("Hopper-clock item count must be between 1 and " + MAX_ITEMS);
        }
    }

    public int periodGameTicks() {
        // Vanilla update order makes the one-item bounce a special case. Once two or more
        // items are present, the original piston EHC repeats every 16 * items - 12 game ticks.
        if (itemCount == MIN_ITEMS) return FIRST_ITEM_PERIOD_GAME_TICKS;
        return SECOND_ITEM_PERIOD_GAME_TICKS + (itemCount - 2) * PERIOD_STEP_GAME_TICKS;
    }

    public BigDecimal periodRedstoneTicks() {
        return BigDecimal.valueOf(periodGameTicks()).divide(GAME_TICKS_PER_REDSTONE_TICK_DECIMAL);
    }

    public String periodRedstoneTicksText() {
        return periodRedstoneTicks().stripTrailingZeros().toPlainString();
    }

    public boolean exceedsRedstoneTickLimit(int maximumRedstoneTicks) {
        return periodGameTicks() > (long) maximumRedstoneTicks * GAME_TICKS_PER_REDSTONE_TICK;
    }

    public static ParseResult parse(String value) {
        String normalized = value == null ? "" : value.toLowerCase(Locale.ROOT);
        Matcher matcher = INTERVAL_PATTERN.matcher(normalized);
        if (!matcher.matches()) {
            return ParseResult.failure(Component.translatable("message.redstonelabworks.clock.invalid_interval"));
        }

        BigDecimal amount;
        try {
            amount = new BigDecimal(matcher.group(1));
        } catch (NumberFormatException ignored) {
            return ParseResult.failure(Component.translatable("message.redstonelabworks.clock.too_large"));
        }

        if (amount.signum() <= 0) {
            return ParseResult.failure(Component.translatable("message.redstonelabworks.clock.positive"));
        }

        boolean seconds = matcher.group(2).equals("s");
        BigDecimal gameTicksValue = amount.multiply(
                seconds ? GAME_TICKS_PER_SECOND_DECIMAL : GAME_TICKS_PER_REDSTONE_TICK_DECIMAL
        );
        if (gameTicksValue.compareTo(BigDecimal.valueOf(maxPeriodGameTicks())) > 0) {
            return unsupportedPeriod();
        }

        int gameTicks;
        try {
            gameTicks = gameTicksValue.intValueExact();
        } catch (ArithmeticException ignored) {
            return ParseResult.failure(Component.translatable("message.redstonelabworks.clock.whole_game_ticks"));
        }

        if (gameTicks == FIRST_ITEM_PERIOD_GAME_TICKS) {
            return ParseResult.success(new HopperClockInterval(MIN_ITEMS));
        }

        int ticksAfterSecondItem = gameTicks - SECOND_ITEM_PERIOD_GAME_TICKS;
        if (ticksAfterSecondItem < 0 || ticksAfterSecondItem % PERIOD_STEP_GAME_TICKS != 0) {
            return unsupportedPeriod();
        }

        return ParseResult.success(new HopperClockInterval(2 + ticksAfterSecondItem / PERIOD_STEP_GAME_TICKS));
    }

    private static int maxPeriodGameTicks() {
        return SECOND_ITEM_PERIOD_GAME_TICKS + (MAX_ITEMS - 2) * PERIOD_STEP_GAME_TICKS;
    }

    private static ParseResult unsupportedPeriod() {
        return ParseResult.failure(Component.translatable("message.redstonelabworks.clock.hopper_range"));
    }

    public record ParseResult(HopperClockInterval interval, Component error) {

        private static ParseResult success(HopperClockInterval interval) {
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
