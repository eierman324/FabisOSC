package de.fabisosc.fabric;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import de.fabisosc.core.Mode;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.PermissionProviderCheck;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;

/**
 * /osc give [spieler] [stab|nuke] [ringe]   (Cheats/OP noetig)
 * /osc mode <stab|nuke>
 * /osc rings <anzahl>
 * /osc reload                               (Cheats/OP noetig)
 */
final class FabricCommands {

    private FabricCommands() {
    }

    static void register(CommandDispatcher<CommandSourceStack> dispatcher, FabisOscMod mod) {
        PermissionProviderCheck<CommandSourceStack> admin = Commands.hasPermission(Commands.LEVEL_GAMEMASTERS);

        dispatcher.register(Commands.literal("osc")
                .executes(ctx -> help(ctx.getSource()))
                .then(Commands.literal("give").requires(admin)
                        .executes(ctx -> give(ctx, mod, ctx.getSource().getPlayerOrException(), Mode.STAB, -1))
                        .then(stabGive(mod, null))
                        .then(nukeGive(mod, null))
                        .then(Commands.argument("spieler", EntityArgument.player())
                                .executes(ctx -> give(ctx, mod, EntityArgument.getPlayer(ctx, "spieler"), Mode.STAB, -1))
                                .then(stabGive(mod, "spieler"))
                                .then(nukeGive(mod, "spieler"))))
                .then(Commands.literal("mode")
                        .then(Commands.literal("stab").executes(ctx -> setMode(ctx, mod, Mode.STAB)))
                        .then(Commands.literal("nuke").executes(ctx -> setMode(ctx, mod, Mode.NUKE))))
                .then(Commands.literal("rings")
                        .then(Commands.argument("anzahl", IntegerArgumentType.integer(1))
                                .executes(ctx -> setRings(ctx, mod, IntegerArgumentType.getInteger(ctx, "anzahl")))))
                .then(Commands.literal("reload").requires(admin)
                        .executes(ctx -> {
                            mod.reload();
                            ctx.getSource().sendSuccess(() -> msg("FabisOSC Config neu geladen.", ChatFormatting.GREEN), true);
                            return 1;
                        })));
    }

    /** "stab" hinter /osc give [spieler] */
    private static LiteralArgumentBuilder<CommandSourceStack> stabGive(FabisOscMod mod, String playerArg) {
        return Commands.literal("stab").executes(ctx -> give(ctx, mod, target(ctx, playerArg), Mode.STAB, -1));
    }

    /** "nuke [ringe]" hinter /osc give [spieler] */
    private static LiteralArgumentBuilder<CommandSourceStack> nukeGive(FabisOscMod mod, String playerArg) {
        return Commands.literal("nuke")
                .executes(ctx -> give(ctx, mod, target(ctx, playerArg), Mode.NUKE, -1))
                .then(Commands.argument("ringe", IntegerArgumentType.integer(1))
                        .executes(ctx -> give(ctx, mod, target(ctx, playerArg), Mode.NUKE,
                                IntegerArgumentType.getInteger(ctx, "ringe"))));
    }

    private static ServerPlayer target(CommandContext<CommandSourceStack> ctx, String playerArg) throws CommandSyntaxException {
        return playerArg == null ? ctx.getSource().getPlayerOrException() : EntityArgument.getPlayer(ctx, playerArg);
    }

    private static int give(CommandContext<CommandSourceStack> ctx, FabisOscMod mod, ServerPlayer target, Mode mode, int rings) {
        var settings = mod.settings();
        int max = settings.nuke.maxRings;
        if (rings > max) {
            ctx.getSource().sendFailure(msg("Ringe muessen zwischen 1 und " + max + " liegen.", ChatFormatting.RED));
            return 0;
        }
        ItemStack rod = FabricRod.create(mode, rings < 1 ? settings.nuke.defaultRings : rings);
        if (!target.getInventory().add(rod)) {
            target.level().addFreshEntity(new ItemEntity(target.level(), target.getX(), target.getY(), target.getZ(), rod));
        }
        ctx.getSource().sendSuccess(() -> msg("OSC-Angel an " + target.getName().getString() + " gegeben.", ChatFormatting.GREEN), true);
        return 1;
    }

    private static int setMode(CommandContext<CommandSourceStack> ctx, FabisOscMod mod, Mode mode) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        ItemStack rod = player.getMainHandItem();
        if (!FabricRod.isRod(rod)) {
            ctx.getSource().sendFailure(msg("Nimm die OSC-Angel in die Hand.", ChatFormatting.RED));
            return 0;
        }
        FabricRod.update(rod, mode, FabricRod.rings(rod, mod.settings().nuke.defaultRings));
        ctx.getSource().sendSuccess(() -> msg("Modus: " + mode.displayName(), ChatFormatting.AQUA), false);
        return 1;
    }

    private static int setRings(CommandContext<CommandSourceStack> ctx, FabisOscMod mod, int rings) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        ItemStack rod = player.getMainHandItem();
        if (!FabricRod.isRod(rod)) {
            ctx.getSource().sendFailure(msg("Nimm die OSC-Angel in die Hand.", ChatFormatting.RED));
            return 0;
        }
        int max = mod.settings().nuke.maxRings;
        if (rings > max) {
            ctx.getSource().sendFailure(msg("Ringe muessen zwischen 1 und " + max + " liegen.", ChatFormatting.RED));
            return 0;
        }
        FabricRod.update(rod, Mode.NUKE, rings);
        ctx.getSource().sendSuccess(() -> msg("Nuke mit " + rings + " Ringen.", ChatFormatting.AQUA), false);
        return 1;
    }

    private static int help(CommandSourceStack source) {
        source.sendSuccess(() -> msg("FabisOSC:", ChatFormatting.GOLD), false);
        source.sendSuccess(() -> msg("/osc give [spieler] [stab|nuke] [ringe]", ChatFormatting.GRAY), false);
        source.sendSuccess(() -> msg("/osc mode <stab|nuke>", ChatFormatting.GRAY), false);
        source.sendSuccess(() -> msg("/osc rings <anzahl>", ChatFormatting.GRAY), false);
        source.sendSuccess(() -> msg("/osc reload", ChatFormatting.GRAY), false);
        return 1;
    }

    private static Component msg(String text, ChatFormatting color) {
        return Component.literal(text).withStyle(color);
    }
}
