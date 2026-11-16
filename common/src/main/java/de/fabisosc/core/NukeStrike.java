package de.fabisosc.core;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Nuke: Ein TNT faellt aus 100 Bloecken Hoehe Richtung Ziel. Nach einem Teil der
 * Fallhoehe (Standard 60 %) knallt in der Luft eine grosse Explosion (Standard
 * Staerke 16), die das TNT in Ringen nach aussen sprengt. Am Boden bleibt jedes
 * TNT kurz liegen (Standard 5 Ticks) und explodiert dann.
 * Die Ringanzahl entspricht dem linken Dropper der gebauten OSC.
 */
final class NukeStrike extends Strike {

    private record Landed(FallingCharge charge, int detonateAt) {
    }

    private final int rings;
    private final FallingCharge drop;
    private final double splitY;
    private final List<FallingCharge> falling = new ArrayList<>();
    private final List<Landed> landed = new ArrayList<>();
    private boolean split;
    private int age;
    private int cooldownTicks = -1;

    NukeStrike(OscWorld world, OscSettings settings, int bx, int by, int bz, int rings) {
        super(world, settings, bx, by, bz, rings * settings.nuke.ringSpacing);
        this.rings = rings;
        double startY = startY();
        double groundY = targetY + 1;
        this.splitY = startY - settings.nuke.splitAt * (startY - groundY);
        this.drop = new FallingCharge(world, cx, startY, cz);
    }

    @Override
    public boolean tick() {
        if (cooldownTicks >= 0) {
            return --cooldownTicks <= 0;
        }
        age++;

        // Fallende TNT weiterbewegen, gelandete in die Warteliste
        Iterator<FallingCharge> fit = falling.iterator();
        while (fit.hasNext()) {
            FallingCharge charge = fit.next();
            if (charge.step(world)) {
                landed.add(new Landed(charge, age + settings.nuke.groundTicks));
                fit.remove();
            }
        }

        // Gelandete TNT liegen noch kurz und explodieren dann
        Iterator<Landed> lit = landed.iterator();
        while (lit.hasNext()) {
            Landed l = lit.next();
            l.charge().step(world); // am Boden festhalten
            if (age >= l.detonateAt()) {
                l.charge().remove();
                for (int i = 0; i < settings.nuke.tntPerPoint; i++) {
                    world.detonate(l.charge().x(), l.charge().y(), l.charge().z());
                }
                lit.remove();
            }
        }

        // Das eine TNT faellt, bis es sich teilt
        if (!split) {
            boolean hitGround = drop.step(world);
            if (hitGround || drop.y() <= splitY) {
                doSplit(hitGround);
            }
        }

        if (split && falling.isEmpty() && landed.isEmpty()) {
            cooldownTicks = 10;
        }
        return false;
    }

    /** Das fallende TNT teilt sich in alle Ring-TNT, die von hier aus auseinanderfaechern. */
    private void doSplit(boolean dropLanded) {
        split = true;
        OscSettings.Nuke nuke = settings.nuke;
        double x = drop.x();
        double y = drop.y();
        double z = drop.z();
        double speed = drop.speed();

        // Die grosse Explosion in der Luft, die das TNT in die Ringe sprengt
        if (nuke.splitPower > 0) {
            world.explode(x, y, z, (float) nuke.splitPower);
        }

        if (nuke.centerHit) {
            // Das Ausgangs-TNT faellt weiter in die Mitte
            if (dropLanded) {
                landed.add(new Landed(drop, age + nuke.groundTicks));
            } else {
                falling.add(drop);
            }
        } else {
            drop.remove();
        }

        for (int ring = 1; ring <= rings; ring++) {
            double radius = ring * nuke.ringSpacing;
            int count = Math.max(6, (int) Math.round(2 * Math.PI * radius / nuke.pointSpacing));
            double offset = ring * 0.5; // Ringe leicht versetzt, sieht gleichmaessiger aus
            for (int i = 0; i < count; i++) {
                double angle = offset + 2 * Math.PI * i / count;
                // "Prime Momentum": jedes TNT wird ein Stueck in eine zufaellige Richtung verschoben
                double jitter = ThreadLocalRandom.current().nextDouble(2 * Math.PI);
                double px = cx + Math.cos(angle) * radius + Math.cos(jitter) * nuke.primeOffset;
                double pz = cz + Math.sin(angle) * radius + Math.sin(jitter) * nuke.primeOffset;
                double groundY = world.highestBlockY((int) Math.floor(px), (int) Math.floor(pz)) + 1;
                falling.add(new FallingCharge(world, x, y, z, speed, px, pz, groundY));
            }
        }
    }

    @Override
    protected void onCleanup() {
        drop.remove();
        for (FallingCharge charge : falling) charge.remove();
        for (Landed l : landed) l.charge().remove();
        falling.clear();
        landed.clear();
    }
}
