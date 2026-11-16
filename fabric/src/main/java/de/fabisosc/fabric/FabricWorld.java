package de.fabisosc.fabric;

import de.fabisosc.core.OscWorld;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Fabric-Umsetzung von OscWorld fuer eine Welt und den Spieler, der geschossen hat. */
final class FabricWorld implements OscWorld {

    /** Chunk-Zaehler pro Welt, damit sich ueberlappende Strikes nicht gegenseitig entladen. */
    private static final Map<ResourceKey<Level>, Map<Long, Integer>> FORCED = new HashMap<>();

    /** Waehrend einer OSC-Explosion gespawnte Items werden hier gesammelt und danach geloescht. */
    private static boolean capturingDrops;
    private static final List<Entity> CAPTURED = new ArrayList<>();

    private final ServerLevel level;
    private final ServerPlayer shooter;
    private final boolean dropItems;

    FabricWorld(ServerLevel level, ServerPlayer shooter, boolean dropItems) {
        this.level = level;
        this.shooter = shooter;
        this.dropItems = dropItems;
    }

    static void onEntityLoad(Entity entity) {
        if (capturingDrops && entity instanceof ItemEntity) CAPTURED.add(entity);
    }

    @Override
    public int minY() {
        return level.getMinY();
    }

    @Override
    public int maxY() {
        return level.getMaxY() + 1;
    }

    @Override
    public boolean isPassable(int x, int y, int z) {
        BlockPos pos = new BlockPos(x, y, z);
        return level.getBlockState(pos).getCollisionShape(level, pos).isEmpty();
    }

    @Override
    public int highestBlockY(int x, int z) {
        return level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z) - 1;
    }

    @Override
    public FallingTnt spawnFalling(double x, double y, double z) {
        PrimedTnt tnt = new PrimedTnt(level, x, y, z, shooter);
        tnt.setDeltaMovement(0, 0, 0);
        tnt.setNoGravity(true);
        tnt.setFuse(80);
        level.addFreshEntity(tnt);
        return new FallingTnt() {
            @Override
            public boolean isValid() {
                return !tnt.isRemoved();
            }

            @Override
            public void moveTo(double nx, double ny, double nz, double vx, double vy, double vz) {
                tnt.teleportTo(nx, ny, nz);
                tnt.setDeltaMovement(vx, vy, vz);
                tnt.needsSync = true;
                tnt.setFuse(80);
            }

            @Override
            public void remove() {
                tnt.discard();
            }
        };
    }

    @Override
    public void explode(double x, double y, double z, float power) {
        // Wie Vanilla-TNT: Explosion 0.0625 ueber der Position
        if (dropItems) {
            level.explode(null, x, y + 0.0625, z, power, Level.ExplosionInteraction.TNT);
            return;
        }
        capturingDrops = true;
        try {
            level.explode(null, x, y + 0.0625, z, power, Level.ExplosionInteraction.TNT);
        } finally {
            capturingDrops = false;
            for (Entity item : CAPTURED) item.discard();
            CAPTURED.clear();
        }
    }

    @Override
    public Runnable holdChunks(double cx, double cz, double radius) {
        Map<Long, Integer> counts = FORCED.computeIfAbsent(level.dimension(), k -> new HashMap<>());
        List<Long> ours = new ArrayList<>();
        int minX = ((int) Math.floor(cx - radius)) >> 4;
        int maxX = ((int) Math.floor(cx + radius)) >> 4;
        int minZ = ((int) Math.floor(cz - radius)) >> 4;
        int maxZ = ((int) Math.floor(cz + radius)) >> 4;
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                long key = ChunkPos.pack(x, z);
                Integer count = counts.get(key);
                if (count == null) {
                    // Chunks, die schon per /forceload geladen sind, fassen wir nicht an
                    if (level.getForceLoadedChunks().contains(key)) continue;
                    level.setChunkForced(x, z, true);
                }
                counts.put(key, count == null ? 1 : count + 1);
                ours.add(key);
            }
        }
        return () -> {
            for (long key : ours) {
                Integer count = counts.get(key);
                if (count == null) continue;
                if (count <= 1) {
                    counts.remove(key);
                    level.setChunkForced((int) key, (int) (key >> 32), false);
                } else {
                    counts.put(key, count - 1);
                }
            }
            ours.clear();
        };
    }
}
