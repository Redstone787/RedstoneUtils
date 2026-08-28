/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package io.github.redstone787.redstonelabworks.server.autowire;

import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public enum WireType {
    NONE("none", "None", "off", "disabled", "disable"),
    NORMAL("normal", "Normal"),
    AUTO("auto", "Auto"),
    FAST_AUTO("fast_auto", "Fast Auto", "fast-auto"),
    ONLY_REPEATERS("only_repeaters", "Only Repeaters", "repeaters", "only-repeaters"),
    ONLY_COMPARATORS("only_comparators", "Only Comparators", "comparators", "only-comparators"),
    FAST_COMPARATORS("fast_comparators", "Fast Comparators", "fast-comparators");

    private static final Map<String, WireType> BY_NAME = Arrays.stream(values())
            .flatMap(type -> type.names().map(name -> Map.entry(name, type)))
            .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, Map.Entry::getValue, (first, ignored) -> first));

    private final String key;
    private final String displayName;
    private final String[] aliases;

    WireType(String key, String displayName, String... aliases) {
        this.key = key;
        this.displayName = displayName;
        this.aliases = aliases;
    }

    public static Optional<WireType> find(String name) {
        return Optional.ofNullable(BY_NAME.get(normalize(name)));
    }

    public static String suggestions() {
        return Arrays.stream(values())
                .map(WireType::key)
                .collect(Collectors.joining(", "));
    }

    public String key() {
        return key;
    }

    public String displayName() {
        return displayName;
    }

    private java.util.stream.Stream<String> names() {
        return java.util.stream.Stream.concat(
                java.util.stream.Stream.of(key),
                Arrays.stream(aliases)
        ).map(WireType::normalize);
    }

    private static String normalize(String name) {
        return name.toLowerCase(Locale.ROOT).replace('-', '_');
    }
}
