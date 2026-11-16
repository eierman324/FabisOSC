package de.fabisosc.fabric;

import de.fabisosc.core.Mode;
import de.fabisosc.core.OscSettings;
import de.fabisosc.core.RodLogic;
import de.fabisosc.core.Strike;
import de.fabisosc.core.StrikeRunner;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class FabisOscMod implements ModInitializer {

    public static final Logger LOG = LoggerFactory.getLogger("FabisOSC");
    private static FabisOscMod instance;

    private final RodLogic logic = new RodLogic();
    private final StrikeRunner runner = new StrikeRunner(e -> LOG.warn("Strike abgebrochen", e));
    /** Linksklick auf einen Block kann zusaetzlich ein Punch-Paket schicken - nur einmal umschalten. */
    private final Map<UUID, Long> lastToggle = new HashMap<>();
    private OscSettings settings;
    private long tick;

    public static FabisOscMod get() {
        return instance;
    }

    @Override
    public void onInitialize() {
        instance = this;
        settings = FabricConfig.load(LOG);

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            tick++;
            runner.tick();
        });
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> runner.stopAll());
        ServerEntityEvents.ENTITY_LOAD.register((entity, level) -> FabricWorld.onEntityLoad(entity));
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            logic.forget(handler.player.getUUID());
            lastToggle.remove(handler.player.getUUID());
        });
        CommandRegistrationCallback.EVENT.register((dispatcher, context, selection) ->
                FabricCommands.register(dispatcher, this));

        // Rechtsklick (Angel auswerfen) = feuern
        UseItemCallback.EVENT.register((player, level, hand) -> {
            ItemStack stack = player.getItemInHand(hand);
            if (!FabricRod.isRod(stack)) return InteractionResult.PASS;
            if (player instanceof ServerPlayer serverPlayer && level instanceof ServerLevel serverLevel) {
                fire(serverPlayer, serverLevel, stack);
            }
            // Auch auf dem Client SUCCESS: kein Schwimmer, aber das Paket geht trotzdem an den Server
            return InteractionResult.SUCCESS;
        });

        // Linksklick auf einen Block = Modus wechseln (und Block nicht abbauen)
        AttackBlockCallback.EVENT.register((player, level, hand, pos, direction) -> {
            if (hand != InteractionHand.MAIN_HAND || !FabricRod.isRod(player.getMainHandItem())) {
                return InteractionResult.PASS;
            }
            if (player instanceof ServerPlayer serverPlayer) onLeftClick(serverPlayer, true);
            return InteractionResult.SUCCESS;
        });
    }

    OscSettings settings() {
        return settings;
    }

    void reload() {
        settings = FabricConfig.load(LOG);
    }

    private void fire(ServerPlayer player, ServerLevel level, ItemStack stack) {
        logic.markRightClick(player.getUUID(), tick);
        long left = logic.cooldownLeft(player.getUUID(), settings);
        if (left > 0) {
            actionBar(player, "OSC laedt nach... " + left + "s", ChatFormatting.YELLOW);
            return;
        }

        BlockPos target = findTarget(player, level, settings.range);
        if (target == null) {
            actionBar(player, "Kein Ziel in Reichweite.", ChatFormatting.RED);
            return;
        }

        Mode mode = FabricRod.mode(stack);
        int rings = settings.clampRings(FabricRod.rings(stack, settings.nuke.defaultRings));
        runner.launch(Strike.create(mode, new FabricWorld(level, player, settings.dropItems), settings,
                target.getX(), target.getY(), target.getZ(), rings));
        logic.markShot(player.getUUID());

        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1f, 0.6f);
        actionBar(player, RodLogic.describe(mode, rings) + " auf "
                + target.getX() + " " + target.getY() + " " + target.getZ(), ChatFormatting.GOLD);
    }

    /** Linksklick wechselt Modus, Schleichen + Linksklick aendert die Ringe. */
    public void onLeftClick(ServerPlayer player, boolean onBlock) {
        ItemStack stack = player.getMainHandItem();
        if (!FabricRod.isRod(stack)) return;
        UUID id = player.getUUID();
        if (!onBlock && logic.isSwingFromRightClick(id, tick)) return;
        Long last = lastToggle.get(id);
        if (last != null && tick - last <= 2) return;
        lastToggle.put(id, tick);

        Mode mode = FabricRod.mode(stack);
        int rings = settings.clampRings(FabricRod.rings(stack, settings.nuke.defaultRings));
        if (player.isShiftKeyDown()) {
            mode = Mode.NUKE;
            rings = RodLogic.nextRings(rings, settings);
        } else {
            mode = mode.next();
        }
        FabricRod.update(stack, mode, rings);
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.UI_BUTTON_CLICK, SoundSource.PLAYERS, 0.6f, 1.4f);
        actionBar(player, "Modus: " + RodLogic.describe(mode, rings), ChatFormatting.AQUA);
    }

    /** Sucht den anvisierten Block. Reichweite gilt nur auf X und Z, Y ist egal. */
    private static BlockPos findTarget(ServerPlayer player, ServerLevel level, int range) {
        Vec3 eye = player.getEyePosition();
        Vec3 dir = player.getViewVector(1f);
        double maxDistance = RodLogic.maxRayDistance(dir.x, dir.z, range);
        BlockHitResult hit = level.clip(new ClipContext(eye, eye.add(dir.scale(maxDistance)),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.SOURCE_ONLY, player));
        if (hit.getType() == HitResult.Type.MISS) return null;
        BlockPos pos = hit.getBlockPos();
        return RodLogic.inRange(eye.x, eye.z, pos.getX(), pos.getZ(), range) ? pos : null;
    }

    static void actionBar(ServerPlayer player, String text, ChatFormatting color) {
        player.sendSystemMessage(Component.literal(text).withStyle(color), true);
    }
}
