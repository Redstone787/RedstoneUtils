package org.main.redstoneutils.server.signal;

public final class ComparatorSignal {

    public static final int MIN = 0;
    public static final int MAX = 15;

    private ComparatorSignal() {
    }

    public static boolean isValid(int strength) {
        return strength >= MIN && strength <= MAX;
    }

    public static int filledSlotsForSignal(int strength, int slotCount) {
        if (strength <= MIN || slotCount <= 0) return 0;

        int numerator = (strength - 1) * slotCount;
        int denominator = MAX - 1;
        return Math.clamp(ceilDiv(numerator, denominator), 1, slotCount);
    }

    public static int amountForSignal(int strength, int containerSize, int slotMaxStackSize) {
        if (strength <= MIN || containerSize <= 0 || slotMaxStackSize <= 0) return 0;

        long capacity = (long) containerSize * slotMaxStackSize;
        if (strength >= MAX) return saturatedInt(capacity);

        long numerator = (long) (strength - 1) * capacity;
        long denominator = MAX - 1L;
        return Math.max(1, saturatedInt(ceilDiv(numerator, denominator)));
    }

    private static int ceilDiv(int numerator, int denominator) {
        return (numerator + denominator - 1) / denominator;
    }

    private static long ceilDiv(long numerator, long denominator) {
        return (numerator + denominator - 1L) / denominator;
    }

    private static int saturatedInt(long value) {
        return value > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) value;
    }
}
