/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package io.github.redstone787.redstone_utils.client.ui;

public enum CircleSegment {
    NONE(-1),
    SEGMENT_1(0),
    SEGMENT_2(1),
    SEGMENT_3(2),
    SEGMENT_4(3),
    SEGMENT_5(4),
    SEGMENT_6(5),
    SEGMENT_7(6),
    SEGMENT_8(7),
    SEGMENT_9(8),
    SEGMENT_10(9),
    SEGMENT_11(10),
    SEGMENT_12(11),
    SEGMENT_13(12),
    SEGMENT_14(13),
    SEGMENT_15(14),
    SEGMENT_16(15),
    SEGMENT_17(16),
    SEGMENT_18(17),
    SEGMENT_19(18),
    SEGMENT_20(19),
    SEGMENT_21(20),
    SEGMENT_22(21),
    SEGMENT_23(22),
    SEGMENT_24(23),
    SEGMENT_25(24),
    SEGMENT_26(25),
    SEGMENT_27(26),
    SEGMENT_28(27),
    SEGMENT_29(28),
    SEGMENT_30(29),
    SEGMENT_31(30),
    SEGMENT_32(31);

    private final int index;

    CircleSegment(int index) {
        this.index = index;
    }

    public int index() {
        return index;
    }

    public static CircleSegment fromIndex(int index) {
        for (CircleSegment segment : values()) {
            if (segment.index == index) return segment;
        }

        return NONE;
    }

    public static int maxSegments() {
        return segmentCount();
    }

    public static int segmentCount() {
        return values().length - 1;
    }
}
