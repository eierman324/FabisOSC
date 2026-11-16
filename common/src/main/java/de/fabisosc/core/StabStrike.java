package de.fabisosc.core;

/**
 * Stab: Ein TNT-Paket faellt aus 100 Bloecken Hoehe auf das Ziel und bohrt dann
 * Schicht fuer Schicht eine Saeule bis runter zum Bedrock.
 */
final class StabStrike extends Strike {

    private final FallingCharge charge;
    private double drillY = Double.NaN;
    private int cooldownTicks = -1;

    StabStrike(OscWorld world, OscSettings settings, int bx, int by, int bz) {
        super(world, settings, bx, by, bz, 0);
        this.charge = new FallingCharge(world, cx, startY(), cz);
    }

    @Override
    public boolean tick() {
        if (cooldownTicks >= 0) {
            // kurz warten, bis die letzten Explosionen durch sind
            return --cooldownTicks <= 0;
        }

        if (Double.isNaN(drillY)) {
            if (!charge.step(world)) return false;
            drillY = charge.y();
            charge.remove();
        }

        int minY = world.minY();
        for (int i = 0; i < settings.stab.layersPerTick; i++) {
            if (drillY <= minY) {
                cooldownTicks = 10;
                return false;
            }
            for (int t = 0; t < settings.stab.tntPerLayer; t++) {
                world.detonate(cx, drillY, cz);
            }
            drillY -= settings.stab.layerStep;
        }
        return false;
    }

    @Override
    protected void onCleanup() {
        charge.remove();
    }
}
