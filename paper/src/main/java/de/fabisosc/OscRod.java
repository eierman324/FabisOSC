package de.fabisosc;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import de.fabisosc.core.Mode;
import java.util.ArrayList;
import java.util.List;

/** Erstellt und liest die OSC-Angel. Modus und Ringe stehen im Item selbst. */
public final class OscRod {

    private final NamespacedKey rodKey;
    private final NamespacedKey modeKey;
    private final NamespacedKey ringsKey;

    public OscRod(FabisOSC plugin) {
        this.rodKey = new NamespacedKey(plugin, "osc_rod");
        this.modeKey = new NamespacedKey(plugin, "mode");
        this.ringsKey = new NamespacedKey(plugin, "rings");
    }

    public ItemStack create(Mode mode, int rings) {
        ItemStack item = new ItemStack(Material.FISHING_ROD);
        item.editMeta(meta -> {
            meta.getPersistentDataContainer().set(rodKey, PersistentDataType.BYTE, (byte) 1);
            meta.setUnbreakable(true);
            meta.setEnchantmentGlintOverride(true);
            write(meta, mode, rings);
        });
        return item;
    }

    public boolean isRod(ItemStack item) {
        if (item == null || item.getType() != Material.FISHING_ROD || !item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer().has(rodKey, PersistentDataType.BYTE);
    }

    public Mode mode(ItemStack item) {
        String raw = item.getItemMeta().getPersistentDataContainer().get(modeKey, PersistentDataType.STRING);
        Mode mode = Mode.parse(raw);
        return mode == null ? Mode.STAB : mode;
    }

    public int rings(ItemStack item, int fallback) {
        Integer rings = item.getItemMeta().getPersistentDataContainer().get(ringsKey, PersistentDataType.INTEGER);
        return rings == null ? fallback : rings;
    }

    public void update(ItemStack item, Mode mode, int rings) {
        item.editMeta(meta -> write(meta, mode, rings));
    }

    private void write(ItemMeta meta, Mode mode, int rings) {
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(modeKey, PersistentDataType.STRING, mode.name());
        pdc.set(ringsKey, PersistentDataType.INTEGER, rings);

        meta.displayName(Component.text("Orbital Strike Cannon", NamedTextColor.RED)
                .decoration(TextDecoration.BOLD, true)
                .decoration(TextDecoration.ITALIC, false));

        List<Component> lore = new ArrayList<>();
        lore.add(line("Modus: ", NamedTextColor.GRAY).append(Component.text(mode.displayName(), NamedTextColor.GOLD)));
        if (mode == Mode.NUKE) {
            lore.add(line("Ringe: ", NamedTextColor.GRAY).append(Component.text(rings, NamedTextColor.GOLD)));
        }
        lore.add(Component.empty());
        lore.add(line("Rechtsklick: Feuer auf den anvisierten Block", NamedTextColor.DARK_GRAY));
        lore.add(line("Linksklick: Modus wechseln", NamedTextColor.DARK_GRAY));
        lore.add(line("Schleichen + Linksklick: Ringe aendern", NamedTextColor.DARK_GRAY));
        meta.lore(lore);
    }

    private static Component line(String text, NamedTextColor color) {
        return Component.text(text, color).decoration(TextDecoration.ITALIC, false);
    }
}
