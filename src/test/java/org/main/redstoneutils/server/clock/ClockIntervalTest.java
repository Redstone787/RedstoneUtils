package org.main.redstoneutils.server.clock;

import org.junit.jupiter.api.Test;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClockIntervalTest {

    @Test
    void acceptsFirstFourDocumentedComparatorPeriods() {
        Stream.of(2, 4, 6, 8).forEach(ticks -> {
            ClockInterval.ParseResult result = ClockInterval.parse(ticks + "t");

            assertTrue(result.successful(), () -> ticks + "t should be a valid comparator-clock period");
            assertEquals(ticks, result.interval().ticks());
        });
    }

    @Test
    void acceptsEquivalentDocumentedSeconds() {
        Stream.of("0.2s", "0.4s", "0.6s", "0.8s").forEach(value ->
                assertTrue(ClockInterval.parse(value).successful(), () -> value + " should be valid")
        );
    }

    @Test
    void rejectsPeriodsBetweenDocumentedSteps() {
        assertFalse(ClockInterval.parse("3t").successful());
    }
}
