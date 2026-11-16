package de.fabisosc.core;

import java.util.Locale;

public enum Mode {
    STAB("Stab"),
    NUKE("Nuke");

    private final String displayName;

    Mode(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }

    public Mode next() {
        return this == STAB ? NUKE : STAB;
    }

    public static Mode parse(String s) {
        if (s == null) return null;
        try {
            return valueOf(s.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
