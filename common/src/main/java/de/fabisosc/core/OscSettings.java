package de.fabisosc.core;

/**
 * Alle Einstellungen. Paper liest sie aus config.yml, Fabric aus config/fabisosc.json.
 * Die Feldnamen sind gleichzeitig die JSON-Schluessel auf Fabric.
 */
public final class OscSettings {

    /** Maximale Entfernung zum Zielblock auf X und Z (Y ist egal). */
    public int range = 256;
    /** Wartezeit zwischen zwei Schuessen pro Spieler. */
    public int cooldownSeconds = 3;
    /** Wie hoch ueber dem Ziel das TNT startet (wie im Buch: +100). */
    public int dropHeight = 100;
    /** Sollen zerstoerte Bloecke droppen? false = deutlich weniger Lag. */
    public boolean dropItems = false;

    public Stab stab = new Stab();
    public Nuke nuke = new Nuke();

    public static final class Stab {
        /** TNT pro Bohr-Schicht (mehr = breiteres Loch). */
        public int tntPerLayer = 12;
        /** Abstand zwischen zwei Schichten in Bloecken. */
        public double layerStep = 3;
        /** Schichten pro Tick (mehr = schneller). */
        public int layersPerTick = 1;
    }

    public static final class Nuke {
        /** Ringe, wenn nichts angegeben ist (linker Dropper im Buch). */
        public int defaultRings = 8;
        public int maxRings = 20;
        /** Abstand zwischen zwei Ringen in Bloecken. */
        public double ringSpacing = 4;
        /** Abstand zwischen zwei Einschlaegen auf einem Ring. */
        public double pointSpacing = 3;
        /** TNT pro Einschlagpunkt. */
        public int tntPerPoint = 2;
        /** Nach welchem Anteil der Fallhoehe sich das TNT in die Ringe teilt (0.6 = 60 %). */
        public double splitAt = 0.6;
        /** Staerke der Explosion beim Teilen (normales TNT = 4, 0 = keine Explosion). */
        public double splitPower = 16;
        /** Jedes weggesprengte TNT landet um so viele Bloecke in zufaelliger Richtung versetzt (Prime Momentum). */
        public double primeOffset = 0.2;
        /** Wie viele Ticks das TNT am Boden liegt, bevor es explodiert. */
        public int groundTicks = 5;
        /** Zusaetzlich ein Einschlag genau in der Mitte. */
        public boolean centerHit = true;
    }

    /** Setzt unsinnige Werte auf gueltige Grenzen. */
    public OscSettings sanitize() {
        if (stab == null) stab = new Stab();
        if (nuke == null) nuke = new Nuke();
        range = Math.max(1, range);
        cooldownSeconds = Math.max(0, cooldownSeconds);
        dropHeight = Math.max(0, dropHeight);
        stab.tntPerLayer = Math.max(1, stab.tntPerLayer);
        stab.layerStep = Math.max(1, stab.layerStep);
        stab.layersPerTick = Math.max(1, stab.layersPerTick);
        nuke.maxRings = Math.max(1, nuke.maxRings);
        nuke.defaultRings = clampRings(nuke.defaultRings);
        nuke.ringSpacing = Math.max(1, nuke.ringSpacing);
        nuke.pointSpacing = Math.max(1, nuke.pointSpacing);
        nuke.tntPerPoint = Math.max(1, nuke.tntPerPoint);
        nuke.splitAt = Math.max(0, Math.min(1, nuke.splitAt));
        nuke.groundTicks = Math.max(0, nuke.groundTicks);
        nuke.splitPower = Math.max(0, nuke.splitPower);
        nuke.primeOffset = Math.max(0, nuke.primeOffset);
        return this;
    }

    public int clampRings(int rings) {
        return Math.max(1, Math.min(nuke.maxRings, rings));
    }
}
