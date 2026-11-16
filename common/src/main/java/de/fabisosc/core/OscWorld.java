package de.fabisosc.core;

/**
 * Was ein Strike von der Welt braucht. Paper und Fabric bauen das jeweils
 * fuer eine Welt und den Spieler, der geschossen hat.
 */
public interface OscWorld {

    /** Unterste Blockhoehe (inklusive). */
    int minY();

    /** Oberste Blockhoehe (exklusive). */
    int maxY();

    /** true, wenn der Block keine Kollision hat (Luft, Gras, Wasser ...). */
    boolean isPassable(int x, int y, int z);

    /** Y des hoechsten festen Blocks in dieser Spalte. */
    int highestBlockY(int x, int z);

    /** Spawnt ein TNT, das der Strike selbst bewegt. */
    FallingTnt spawnFalling(double x, double y, double z);

    /** Eine normale TNT-Explosion (Staerke 4) an dieser Stelle (sofort oder im naechsten Tick). */
    default void detonate(double x, double y, double z) {
        explode(x, y, z, 4.0f);
    }

    /** Eine TNT-Explosion mit beliebiger Staerke (sofort oder im naechsten Tick). */
    void explode(double x, double y, double z, float power);

    /** Haelt die Chunks im Umkreis geladen. Der Rueckgabewert gibt sie wieder frei. */
    Runnable holdChunks(double cx, double cz, double radius);

    interface FallingTnt {
        boolean isValid();

        /** Setzt Position und Geschwindigkeit (fuer fluessige Anzeige) und haelt die Zuendschnur am Brennen. */
        void moveTo(double x, double y, double z, double vx, double vy, double vz);

        void remove();
    }
}
