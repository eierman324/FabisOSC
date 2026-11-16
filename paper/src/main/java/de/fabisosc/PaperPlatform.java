package de.fabisosc;

import de.fabisosc.core.OscWorld;
import de.fabisosc.core.StrikeRunner;
import org.bukkit.Chunk;
import org.bukkit.HeightMap;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

/** Paper-Umsetzung von OscWorld plus der Tick-Task fuer alle Strikes. */
public final class PaperPlatform {

    private final FabisOSC plugin;
    private final NamespacedKey tntKey;
    private final StrikeRunner runner;
    private final List<Pending> pending = new ArrayList<>();
    /** Chunk-Tickets mit Zaehler, damit sich ueberlappende Strikes nicht gegenseitig entladen. */
    private final Map<UUID, Map<Long, Integer>> tickets = new HashMap<>();
    private BukkitTask task;
    private long currentTick;

    private record Pending(TNTPrimed tnt, long deadline) {
    }

    public PaperPlatform(FabisOSC plugin) {
        this.plugin = plugin;
        this.tntKey = new NamespacedKey(plugin, "strike_tnt");
        this.runner = new StrikeRunner(e -> plugin.getLogger().log(Level.WARNING, "Strike abgebrochen", e));
    }

    public StrikeRunner runner() {
        return runner;
    }

    public void start() {
        task = plugin.getServer().getScheduler().runTaskTimer(plugin, this::tick, 1L, 1L);
    }

    public void stop() {
        if (task != null) task.cancel();
        runner.stopAll();
        pending.clear();
        for (Map.Entry<UUID, Map<Long, Integer>> entry : tickets.entrySet()) {
            World world = plugin.getServer().getWorld(entry.getKey());
            if (world == null) continue;
            for (long key : entry.getValue().keySet()) {
                world.removePluginChunkTicket((int) key, (int) (key >> 32), plugin);
            }
        }
        tickets.clear();
    }

    public boolean isStrikeTnt(Entity entity) {
        return entity.getPersistentDataContainer().has(tntKey, PersistentDataType.BYTE);
    }

    private void tick() {
        currentTick++;
        runner.tick();

        // Falls ein Chunk keine Entities tickt, wuerde gezuendetes TNT nie explodieren.
        // Dann zuenden wir es nach kurzer Zeit selbst.
        Iterator<Pending> it = pending.iterator();
        while (it.hasNext()) {
            Pending p = it.next();
            if (!p.tnt().isValid()) {
                it.remove();
            } else if (currentTick >= p.deadline()) {
                TNTPrimed tnt = p.tnt();
                Location loc = tnt.getLocation();
                tnt.remove();
                loc.getWorld().createExplosion(loc, tnt.getYield(), false, true, tnt.getSource());
                it.remove();
            }
        }
    }

    public OscWorld world(World world, Player shooter) {
        return new PaperWorld(world, shooter);
    }

    private final class PaperWorld implements OscWorld {

        private final World world;
        private final Player shooter;

        PaperWorld(World world, Player shooter) {
            this.world = world;
            this.shooter = shooter;
        }

        @Override
        public int minY() {
            return world.getMinHeight();
        }

        @Override
        public int maxY() {
            return world.getMaxHeight();
        }

        @Override
        public boolean isPassable(int x, int y, int z) {
            return world.getBlockAt(x, y, z).isPassable();
        }

        @Override
        public int highestBlockY(int x, int z) {
            return world.getHighestBlockYAt(x, z, HeightMap.MOTION_BLOCKING);
        }

        @Override
        public FallingTnt spawnFalling(double x, double y, double z) {
            TNTPrimed tnt = world.spawn(new Location(world, x, y, z), TNTPrimed.class, t -> {
                t.setFuseTicks(80);
                t.setGravity(false);
                t.setVelocity(new Vector());
                t.setSource(shooter);
                t.getPersistentDataContainer().set(tntKey, PersistentDataType.BYTE, (byte) 1);
            });
            return new FallingTnt() {
                @Override
                public boolean isValid() {
                    return tnt.isValid();
                }

                @Override
                public void moveTo(double nx, double ny, double nz, double vx, double vy, double vz) {
                    tnt.teleport(new Location(world, nx, ny, nz));
                    tnt.setVelocity(new Vector(vx, vy, vz));
                    tnt.setFuseTicks(80);
                }

                @Override
                public void remove() {
                    tnt.remove();
                }
            };
        }

        @Override
        public void explode(double x, double y, double z, float power) {
            TNTPrimed tnt = world.spawn(new Location(world, x, y, z), TNTPrimed.class, t -> {
                t.setFuseTicks(1);
                t.setYield(power);
                t.setVelocity(new Vector());
                t.setSource(shooter);
                t.getPersistentDataContainer().set(tntKey, PersistentDataType.BYTE, (byte) 1);
            });
            pending.add(new Pending(tnt, currentTick + 4));
        }

        @Override
        public Runnable holdChunks(double cx, double cz, double radius) {
            List<Long> keys = new ArrayList<>();
            int minX = ((int) Math.floor(cx - radius)) >> 4;
            int maxX = ((int) Math.floor(cx + radius)) >> 4;
            int minZ = ((int) Math.floor(cz - radius)) >> 4;
            int maxZ = ((int) Math.floor(cz + radius)) >> 4;
            Map<Long, Integer> worldTickets = tickets.computeIfAbsent(world.getUID(), k -> new HashMap<>());
            for (int x = minX; x <= maxX; x++) {
                for (int z = minZ; z <= maxZ; z++) {
                    long key = Chunk.getChunkKey(x, z);
                    if (worldTickets.merge(key, 1, Integer::sum) == 1) world.addPluginChunkTicket(x, z, plugin);
                    keys.add(key);
                }
            }
            return () -> release(keys);
        }

        private void release(List<Long> keys) {
            Map<Long, Integer> worldTickets = tickets.get(world.getUID());
            if (worldTickets == null) return;
            for (long key : keys) {
                Integer count = worldTickets.get(key);
                if (count == null) continue;
                if (count <= 1) {
                    worldTickets.remove(key);
                    world.removePluginChunkTicket((int) key, (int) (key >> 32), plugin);
                } else {
                    worldTickets.put(key, count - 1);
                }
            }
            keys.clear();
        }
    }
}
