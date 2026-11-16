package de.fabisosc.core;

/**
 * Ein TNT, das nach unten faellt (gleiche Schwerkraft wie Vanilla-TNT). Optional
 * wandert es dabei seitlich zu einem Zielpunkt, so dass viele TNT zusammen
 * im Fall auseinanderfaechern. Die Bewegung rechnet der Strike selbst, damit
 * Explosionen in der Naehe es nicht wegschleudern - dafuer brauchte die gebaute
 * OSC "Disable Entity Ticking".
 */
final class FallingCharge {

    static final double GRAVITY = 0.04;
    static final double DRAG = 0.98;

    private final OscWorld.FallingTnt tnt;
    private final double startX;
    private final double startZ;
    private final double targetX;
    private final double targetZ;
    /** Nach so vielen Ticks soll das TNT seitlich am Ziel sein (ungefaehr beim Aufschlag). */
    private final int spreadTicks;
    private double x;
    private double y;
    private double z;
    private double speed;
    private int age;
    private boolean landed;

    /** Faellt senkrecht nach unten. */
    FallingCharge(OscWorld world, double x, double y, double z) {
        this(world, x, y, z, 0, x, z, y);
    }

    /**
     * Startet bei (x, y, z) mit Fallgeschwindigkeit speed und faechert beim Fallen
     * zu (targetX, targetZ) auf. groundY ist die geschaetzte Bodenhoehe am Ziel.
     */
    FallingCharge(OscWorld world, double x, double y, double z, double speed,
                  double targetX, double targetZ, double groundY) {
        this.tnt = world.spawnFalling(x, y, z);
        this.startX = x;
        this.startZ = z;
        this.targetX = targetX;
        this.targetZ = targetZ;
        this.spreadTicks = ticksToFall(y - groundY, speed);
        this.x = x;
        this.y = y;
        this.z = z;
        this.speed = speed;
    }

    /** Wie viele Ticks ein TNT mit Startgeschwindigkeit v braucht, um diese Strecke zu fallen. */
    private static int ticksToFall(double distance, double v) {
        double fallen = 0;
        int ticks = 0;
        while (fallen < distance && ticks < 2000) {
            v = (v + GRAVITY) * DRAG;
            fallen += v;
            ticks++;
        }
        return Math.max(1, ticks);
    }

    double x() {
        return x;
    }

    double y() {
        return y;
    }

    double z() {
        return z;
    }

    double speed() {
        return speed;
    }

    /** Bewegt das TNT einen Tick weiter. @return true, sobald es aufgeschlagen ist. */
    boolean step(OscWorld world) {
        if (landed) {
            // Liegt am Boden: an Ort und Stelle halten, Explosionen in der Naehe verschieben es nicht
            if (tnt.isValid()) tnt.moveTo(x, y, z, 0, 0, 0);
            return true;
        }
        if (!tnt.isValid()) {
            landed = true;
            return true;
        }
        age++;
        speed = (speed + GRAVITY) * DRAG;
        double nextY = y - speed;
        // Nach der Split-Explosion erst schnell nach aussen, dann immer langsamer (wie weggesprengt)
        double t = Math.min(1.0, (double) age / spreadTicks);
        double s = 1 - (1 - t) * (1 - t) * (1 - t);
        double nextX = startX + (targetX - startX) * s;
        double nextZ = startZ + (targetZ - startZ) * s;

        int bx = (int) Math.floor(nextX);
        int bz = (int) Math.floor(nextZ);
        int from = (int) Math.floor(y);
        int to = (int) Math.floor(nextY);
        int minY = world.minY();
        for (int by = from; by >= to; by--) {
            if (by < minY) {
                land(nextX, minY, nextZ);
                return true;
            }
            // Wasser/Lava/Gras sind passierbar -> TNT sinkt durch bis zum festen Boden
            if (!world.isPassable(bx, by, bz)) {
                land(nextX, by + 1, nextZ);
                return true;
            }
        }

        tnt.moveTo(nextX, nextY, nextZ, nextX - x, -speed, nextZ - z);
        x = nextX;
        y = nextY;
        z = nextZ;
        return false;
    }

    /** Setzt das TNT auf den Boden. Es bleibt sichtbar liegen, bis remove() aufgerufen wird. */
    private void land(double atX, double atY, double atZ) {
        x = atX;
        y = atY;
        z = atZ;
        speed = 0;
        landed = true;
        if (tnt.isValid()) tnt.moveTo(x, y, z, 0, 0, 0);
    }

    void remove() {
        if (tnt.isValid()) tnt.remove();
    }
}
