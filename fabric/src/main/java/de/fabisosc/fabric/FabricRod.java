package de.fabisosc.fabric;

import de.fabisosc.core.Mode;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.Unit;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemLore;

import java.util.ArrayList;
import java.util.List;

/** Erstellt und liest die OSC-Angel. Modus und Ringe stehen in den Custom-Daten des Items. */
final class FabricRod {

    private static final String ROD = "fabisosc_rod";
    private static final String MODE = "fabisosc_mode";
    private static final String RINGS = "fabisosc_rings";

    private FabricRod() {
    }

    static ItemStack create(Mode mode, int rings) {
        ItemStack stack = new ItemStack(Items.FISHING_ROD);
        stack.set(DataComponents.UNBREAKABLE, Unit.INSTANCE);
        stack.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putBoolean(ROD, true));
        update(stack, mode, rings);
        return stack;
    }

    static boolean isRod(ItemStack stack) {
        return stack != null && stack.getItem() == Items.FISHING_ROD && tag(stack).getBooleanOr(ROD, false);
    }

    static Mode mode(ItemStack stack) {
        Mode mode = Mode.parse(tag(stack).getStringOr(MODE, "STAB"));
        return mode == null ? Mode.STAB : mode;
    }

    static int rings(ItemStack stack, int fallback) {
        return tag(stack).getIntOr(RINGS, fallback);
    }

    static void update(ItemStack stack, Mode mode, int rings) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
            tag.putString(MODE, mode.name());
            tag.putInt(RINGS, rings);
        });
        stack.set(DataComponents.CUSTOM_NAME, Component.literal("Orbital Strike Cannon")
                .withStyle(ChatFormatting.RED, ChatFormatting.BOLD)
                .withStyle(s -> s.withItalic(false)));

        List<Component> lore = new ArrayList<>();
        lore.add(line("Modus: ", ChatFormatting.GRAY).append(line(mode.displayName(), ChatFormatting.GOLD)));
        if (mode == Mode.NUKE) {
            lore.add(line("Ringe: ", ChatFormatting.GRAY).append(line(String.valueOf(rings), ChatFormatting.GOLD)));
        }
        lore.add(Component.empty());
        lore.add(line("Rechtsklick: Feuer auf den anvisierten Block", ChatFormatting.DARK_GRAY));
        lore.add(line("Linksklick: Modus wechseln", ChatFormatting.DARK_GRAY));
        lore.add(line("Schleichen + Linksklick: Ringe aendern", ChatFormatting.DARK_GRAY));
        stack.set(DataComponents.LORE, new ItemLore(lore));
    }

    private static CompoundTag tag(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        return data == null ? new CompoundTag() : data.copyTag();
    }

    private static MutableComponent line(String text, ChatFormatting color) {
        return Component.literal(text).withStyle(color).withStyle(s -> s.withItalic(false));
    }
}
