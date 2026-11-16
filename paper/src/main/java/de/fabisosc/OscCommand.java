package de.fabisosc;

import de.fabisosc.core.Mode;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Collection;
import java.util.List;
import java.util.Locale;

/**
 * /osc give [spieler] [stab|nuke] [ringe]
 * /osc mode <stab|nuke>
 * /osc rings <anzahl>
 * /osc reload
 */
final class OscCommand implements BasicCommand {

    private final FabisOSC plugin;

    OscCommand(FabisOSC plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(CommandSourceStack source, String[] args) {
        CommandSender sender = source.getSender();
        String sub = args.length == 0 ? "" : args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "give" -> give(sender, args);
            case "mode" -> mode(sender, args);
            case "rings" -> rings(sender, args);
            case "reload" -> {
                if (!admin(sender)) return;
                plugin.loadSettings();
                sender.sendMessage(Component.text("FabisOSC Config neu geladen.", NamedTextColor.GREEN));
            }
            default -> help(sender);
        }
    }

    private void give(CommandSender sender, String[] args) {
        if (!admin(sender)) return;
        Player target;
        int i = 1;
        if (args.length > 1 && Bukkit.getPlayerExact(args[1]) != null) {
            target = Bukkit.getPlayerExact(args[1]);
            i = 2;
        } else if (sender instanceof Player p) {
            target = p;
        } else {
            sender.sendMessage(Component.text("Spieler nicht gefunden.", NamedTextColor.RED));
            return;
        }

        Mode mode = Mode.STAB;
        if (args.length > i) {
            mode = Mode.parse(args[i]);
            if (mode == null) {
                sender.sendMessage(Component.text("Modus muss stab oder nuke sein.", NamedTextColor.RED));
                return;
            }
        }
        int rings = plugin.settings().nuke.defaultRings;
        if (args.length > i + 1) {
            Integer parsed = parseRings(sender, args[i + 1]);
            if (parsed == null) return;
            rings = parsed;
        }

        ItemStack rod = plugin.rod().create(mode, rings);
        target.getInventory().addItem(rod).values()
                .forEach(left -> target.getWorld().dropItem(target.getLocation(), left));
        sender.sendMessage(Component.text("OSC-Angel an " + target.getName() + " gegeben.", NamedTextColor.GREEN));
    }

    private void mode(CommandSender sender, String[] args) {
        ItemStack rod = heldRod(sender);
        if (rod == null) return;
        Mode mode = args.length > 1 ? Mode.parse(args[1]) : null;
        if (mode == null) {
            sender.sendMessage(Component.text("/osc mode <stab|nuke>", NamedTextColor.RED));
            return;
        }
        int rings = plugin.rod().rings(rod, plugin.settings().nuke.defaultRings);
        plugin.rod().update(rod, mode, rings);
        sender.sendMessage(Component.text("Modus: " + mode.displayName(), NamedTextColor.AQUA));
    }

    private void rings(CommandSender sender, String[] args) {
        ItemStack rod = heldRod(sender);
        if (rod == null) return;
        if (args.length < 2) {
            sender.sendMessage(Component.text("/osc rings <anzahl>", NamedTextColor.RED));
            return;
        }
        Integer rings = parseRings(sender, args[1]);
        if (rings == null) return;
        plugin.rod().update(rod, Mode.NUKE, rings);
        sender.sendMessage(Component.text("Nuke mit " + rings + " Ringen.", NamedTextColor.AQUA));
    }

    private ItemStack heldRod(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Nur fuer Spieler.", NamedTextColor.RED));
            return null;
        }
        if (!player.hasPermission("fabisosc.use")) {
            sender.sendMessage(Component.text("Keine Berechtigung.", NamedTextColor.RED));
            return null;
        }
        ItemStack item = player.getInventory().getItemInMainHand();
        if (!plugin.rod().isRod(item)) {
            sender.sendMessage(Component.text("Nimm die OSC-Angel in die Hand.", NamedTextColor.RED));
            return null;
        }
        return item;
    }

    private Integer parseRings(CommandSender sender, String raw) {
        int max = plugin.settings().nuke.maxRings;
        try {
            int rings = Integer.parseInt(raw);
            if (rings >= 1 && rings <= max) return rings;
        } catch (NumberFormatException ignored) {
        }
        sender.sendMessage(Component.text("Ringe muessen zwischen 1 und " + max + " liegen.", NamedTextColor.RED));
        return null;
    }

    private boolean admin(CommandSender sender) {
        if (sender.hasPermission("fabisosc.admin")) return true;
        sender.sendMessage(Component.text("Keine Berechtigung.", NamedTextColor.RED));
        return false;
    }

    private void help(CommandSender sender) {
        sender.sendMessage(Component.text("FabisOSC:", NamedTextColor.GOLD));
        sender.sendMessage(Component.text("/osc give [spieler] [stab|nuke] [ringe]", NamedTextColor.GRAY));
        sender.sendMessage(Component.text("/osc mode <stab|nuke>", NamedTextColor.GRAY));
        sender.sendMessage(Component.text("/osc rings <anzahl>", NamedTextColor.GRAY));
        sender.sendMessage(Component.text("/osc reload", NamedTextColor.GRAY));
    }

    @Override
    public Collection<String> suggest(CommandSourceStack source, String[] args) {
        if (args.length <= 1) {
            return filter(List.of("give", "mode", "rings", "reload"), args.length == 0 ? "" : args[0]);
        }
        String sub = args[0].toLowerCase(Locale.ROOT);
        String last = args[args.length - 1];
        if (sub.equals("mode") && args.length == 2) return filter(List.of("stab", "nuke"), last);
        if (sub.equals("give")) {
            if (args.length == 2) {
                List<String> options = new java.util.ArrayList<>(List.of("stab", "nuke"));
                Bukkit.getOnlinePlayers().forEach(p -> options.add(p.getName()));
                return filter(options, last);
            }
            if (args.length == 3 && Mode.parse(args[1]) == null) return filter(List.of("stab", "nuke"), last);
        }
        return List.of();
    }

    private static List<String> filter(List<String> options, String prefix) {
        String p = prefix.toLowerCase(Locale.ROOT);
        return options.stream().filter(o -> o.toLowerCase(Locale.ROOT).startsWith(p)).toList();
    }
}
