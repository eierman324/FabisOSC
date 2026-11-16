package de.fabisosc.core;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Angel-Verhalten, das auf Paper und Fabric gleich ist. */
public final class RodLogic {

    /** So lange nach einem Rechtsklick wird ein Arm-Schwung nicht als Linksklick gewertet. */
    private static final int SWING_GRACE_TICKS = 5;

    private final Map<UUID, Long> lastShot = new HashMap<>();
    private final Map<UUID, Long> lastRightClick = new HashMap<>();

    /** @return verbleibende Wartezeit in Sekunden, 0 = darf schiessen */
    public long cooldownLeft(UUID player, OscSettings settings) {
        Long last = lastShot.get(player);
        if (last == null) return 0;
        long left = settings.cooldownSeconds * 1000L - (System.currentTimeMillis() - last);
        return left <= 0 ? 0 : (left + 999) / 1000;
    }

    public void markShot(UUID player) {
        lastShot.put(player, System.currentTimeMillis());
    }

    public void markRightClick(UUID player, long tick) {
        lastRightClick.put(player, tick);
    }

    /** Beim Auswerfen schwingt der Arm - dieser Schwung ist kein Linksklick. */
    public boolean isSwingFromRightClick(UUID player, long tick) {
        Long last = lastRightClick.get(player);
        return last != null && tick - last <= SWING_GRACE_TICKS;
    }

    public void forget(UUID player) {
        lastShot.remove(player);
        lastRightClick.remove(player);
    }

    /** Ringe beim Schleichen + Linksklick: 1, 2, ... max, dann wieder 1. */
    public static int nextRings(int rings, OscSettings settings) {
        int clamped = settings.clampRings(rings);
        return clamped >= settings.nuke.maxRings ? 1 : clamped + 1;
    }

    /**
     * Wie weit der Blick-Strahl laufen darf, bis er auf X oder Z die Reichweite erreicht.
     * Nach oben/unten gibt es keine Grenze (ausser 1024 als Sicherheit).
     */
    public static double maxRayDistance(double dirX, double dirZ, int range) {
        double horizontal = Math.max(Math.abs(dirX), Math.abs(dirZ));
        return horizontal < 1.0E-4 ? 1024 : Math.min(1024, range / horizontal);
    }

    public static boolean inRange(double eyeX, double eyeZ, int bx, int bz, int range) {
        return Math.abs(bx + 0.5 - eyeX) <= range + 0.5 && Math.abs(bz + 0.5 - eyeZ) <= range + 0.5;
    }

    public static String describe(Mode mode, int rings) {
        return mode == Mode.NUKE ? "Nuke (" + rings + " Ringe)" : "Stab";
    }
}
