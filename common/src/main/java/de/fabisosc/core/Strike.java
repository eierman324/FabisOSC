package de.fabisosc.core;

public abstract class Strike {

    protected final OscWorld world;
    protected final OscSettings settings;
    protected final double cx;
    protected final double cz;
    protected final int targetY;
    private final Runnable releaseChunks;

    protected Strike(OscWorld world, OscSettings settings, int bx, int by, int bz, double radius) {
        this.world = world;
        this.settings = settings;
        this.cx = bx + 0.5;
        this.cz = bz + 0.5;
        this.targetY = by;
        this.releaseChunks = world.holdChunks(cx, cz, radius + 8);
    }

    public static Strike create(Mode mode, OscWorld world, OscSettings settings, int bx, int by, int bz, int rings) {
        return mode == Mode.STAB
                ? new StabStrike(world, settings, bx, by, bz)
                : new NukeStrike(world, settings, bx, by, bz, settings.clampRings(rings));
    }

    /** @return true, wenn der Strike fertig ist */
    public abstract boolean tick();

    protected void onCleanup() {
    }

    public final void cleanup() {
        onCleanup();
        releaseChunks.run();
    }

    protected int startY() {
        return Math.min(targetY + 1 + settings.dropHeight, world.maxY() - 2);
    }
}
