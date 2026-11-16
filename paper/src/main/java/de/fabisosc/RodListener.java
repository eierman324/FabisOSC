package de.fabisosc;

import de.fabisosc.core.Mode;
import de.fabisosc.core.OscSettings;
import de.fabisosc.core.RodLogic;
import de.fabisosc.core.Strike;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

public final class RodListener implements Listener {

    private final FabisOSC plugin;
    private final RodLogic logic = new RodLogic();

    public RodListener(FabisOSC plugin) {
        this.plugin = plugin;
    }

    /** Angel auswerfen = feuern */
    @EventHandler(ignoreCancelled = true)
    public void onFish(PlayerFishEvent event) {
        if (event.getState() != PlayerFishEvent.State.FISHING) return;
        Player player = event.getPlayer();
        EquipmentSlot hand = event.getHand() == null ? EquipmentSlot.HAND : event.getHand();
        ItemStack item = player.getInventory().getItem(hand);
        if (!plugin.rod().isRod(item)) return;

        event.setCancelled(true);
        logic.markRightClick(player.getUniqueId(), Bukkit.getCurrentTick());
        if (!player.hasPermission("fabisosc.use")) {
            player.sendActionBar(Component.text("Keine Berechtigung.", NamedTextColor.RED));
            return;
        }

        OscSettings settings = plugin.settings();
        long left = logic.cooldownLeft(player.getUniqueId(), settings);
        if (left > 0) {
            player.sendActionBar(Component.text("OSC laedt nach... " + left + "s", NamedTextColor.YELLOW));
            return;
        }

        Block target = findTarget(player, settings.range);
        if (target == null) {
            player.sendActionBar(Component.text("Kein Ziel in Reichweite.", NamedTextColor.RED));
            return;
        }

        Mode mode = plugin.rod().mode(item);
        int rings = settings.clampRings(plugin.rod().rings(item, settings.nuke.defaultRings));
        plugin.platform().runner().launch(Strike.create(mode,
                plugin.platform().world(target.getWorld(), player), settings,
                target.getX(), target.getY(), target.getZ(), rings));
        logic.markShot(player.getUniqueId());

        player.playSound(player.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 1f, 0.6f);
        player.sendActionBar(Component.text(RodLogic.describe(mode, rings) + " auf "
                + target.getX() + " " + target.getY() + " " + target.getZ(), NamedTextColor.GOLD));
    }

    /** Linksklick wechselt Modus, Schleichen + Linksklick aendert die Ringe. */
    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        Action action = event.getAction();
        ItemStack item = event.getItem();
        if (!plugin.rod().isRod(item)) return;
        Player player = event.getPlayer();

        if (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK) {
            logic.markRightClick(player.getUniqueId(), Bukkit.getCurrentTick());
            return;
        }
        if (action != Action.LEFT_CLICK_AIR && action != Action.LEFT_CLICK_BLOCK) return;
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (!player.hasPermission("fabisosc.use")) return;
        event.setCancelled(true);

        // Beim Auswerfen schwingt der Arm. Paper meldet diesen Schwung als
        // "Linksklick in die Luft" - den ignorieren wir kurz nach einem Rechtsklick.
        if (action == Action.LEFT_CLICK_AIR
                && logic.isSwingFromRightClick(player.getUniqueId(), Bukkit.getCurrentTick())) {
            return;
        }

        OscSettings settings = plugin.settings();
        Mode mode = plugin.rod().mode(item);
        int rings = settings.clampRings(plugin.rod().rings(item, settings.nuke.defaultRings));
        if (player.isSneaking()) {
            mode = Mode.NUKE;
            rings = RodLogic.nextRings(rings, settings);
        } else {
            mode = mode.next();
        }
        plugin.rod().update(item, mode, rings);
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.6f, 1.4f);
        player.sendActionBar(Component.text("Modus: " + RodLogic.describe(mode, rings), NamedTextColor.AQUA));
    }

    /** Explosionen der OSC droppen standardmaessig keine Items (sonst massiver Lag). */
    @EventHandler(ignoreCancelled = true)
    public void onExplode(EntityExplodeEvent event) {
        if (!plugin.platform().isStrikeTnt(event.getEntity())) return;
        if (!plugin.settings().dropItems) {
            event.setYield(0f);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        logic.forget(event.getPlayer().getUniqueId());
    }

    /** Sucht den anvisierten Block. Reichweite gilt nur auf X und Z, Y ist egal. */
    private Block findTarget(Player player, int range) {
        Location eye = player.getEyeLocation();
        Vector dir = eye.getDirection();
        double maxDistance = RodLogic.maxRayDistance(dir.getX(), dir.getZ(), range);
        RayTraceResult hit = player.getWorld().rayTraceBlocks(eye, dir, maxDistance, FluidCollisionMode.SOURCE_ONLY, true);
        if (hit == null || hit.getHitBlock() == null) return null;
        Block block = hit.getHitBlock();
        return RodLogic.inRange(eye.getX(), eye.getZ(), block.getX(), block.getZ(), range) ? block : null;
    }
}
