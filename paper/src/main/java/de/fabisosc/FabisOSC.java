package de.fabisosc;

import de.fabisosc.core.OscSettings;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

public final class FabisOSC extends JavaPlugin {

    private OscRod rod;
    private PaperPlatform platform;
    private OscSettings settings;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadSettings();
        rod = new OscRod(this);
        platform = new PaperPlatform(this);
        platform.start();

        getServer().getPluginManager().registerEvents(new RodListener(this), this);
        getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event ->
                event.registrar().register("osc", "Orbital Strike Cannon", List.of("orbitalstrike"), new OscCommand(this)));
    }

    @Override
    public void onDisable() {
        if (platform != null) platform.stop();
    }

    /** Liest config.yml in die gemeinsamen Einstellungen. */
    public void loadSettings() {
        reloadConfig();
        FileConfiguration c = getConfig();
        OscSettings s = new OscSettings();
        s.range = c.getInt("range", s.range);
        s.cooldownSeconds = c.getInt("cooldown-seconds", s.cooldownSeconds);
        s.dropHeight = c.getInt("drop-height", s.dropHeight);
        s.dropItems = c.getBoolean("drop-items", s.dropItems);
        s.stab.tntPerLayer = c.getInt("stab.tnt-per-layer", s.stab.tntPerLayer);
        s.stab.layerStep = c.getDouble("stab.layer-step", s.stab.layerStep);
        s.stab.layersPerTick = c.getInt("stab.layers-per-tick", s.stab.layersPerTick);
        s.nuke.defaultRings = c.getInt("nuke.default-rings", s.nuke.defaultRings);
        s.nuke.maxRings = c.getInt("nuke.max-rings", s.nuke.maxRings);
        s.nuke.ringSpacing = c.getDouble("nuke.ring-spacing", s.nuke.ringSpacing);
        s.nuke.pointSpacing = c.getDouble("nuke.point-spacing", s.nuke.pointSpacing);
        s.nuke.tntPerPoint = c.getInt("nuke.tnt-per-point", s.nuke.tntPerPoint);
        s.nuke.splitAt = c.getDouble("nuke.split-at", s.nuke.splitAt);
        s.nuke.groundTicks = c.getInt("nuke.ground-ticks", s.nuke.groundTicks);
        s.nuke.splitPower = c.getDouble("nuke.split-power", s.nuke.splitPower);
        s.nuke.primeOffset = c.getDouble("nuke.prime-offset", s.nuke.primeOffset);
        s.nuke.centerHit = c.getBoolean("nuke.center-hit", s.nuke.centerHit);
        settings = s.sanitize();
    }

    public OscSettings settings() {
        return settings;
    }

    public OscRod rod() {
        return rod;
    }

    public PaperPlatform platform() {
        return platform;
    }
}
