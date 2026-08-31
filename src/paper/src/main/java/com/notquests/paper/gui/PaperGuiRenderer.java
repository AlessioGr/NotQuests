package com.notquests.paper.gui;

import com.destroystokyo.paper.profile.ProfileProperty;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import xyz.xenondevs.invui.Click;
import xyz.xenondevs.invui.gui.Gui;
import xyz.xenondevs.invui.item.AbstractItem;
import xyz.xenondevs.invui.item.Item;
import xyz.xenondevs.invui.item.ItemProvider;
import xyz.xenondevs.invui.item.ItemWrapper;
import xyz.xenondevs.invui.window.Window;

import com.notquests.core.gui.GuiService.GuiSlot;
import com.notquests.core.gui.GuiService.ResolvedGui;
import com.notquests.paper.NotQuests;
import com.notquests.paper.PaperPlayer;

import java.nio.charset.StandardCharsets;
import java.util.*;

public class PaperGuiRenderer {
    private static final String SLOT_KEYS =
            "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";

    private final NotQuests notQuests;

    public PaperGuiRenderer(NotQuests notQuests) {
        this.notQuests = notQuests;
    }

    public boolean open(
            final Player player,
            final ResolvedGui gui,
            final PaperPlayer questPlayer) {
        if (player == null || !player.isOnline() || gui == null || questPlayer == null) {
            return false;
        }
        Window.builder()
                .setViewer(player)
                .setTitle(notQuests.parse(gui.title()))
                .setUpperGui(buildGui(gui, questPlayer))
                .build()
                .open();
        return true;
    }

    Gui buildGui(final ResolvedGui gui, final PaperPlayer questPlayer) {
        final String[] structure = structure(gui.rows());
        final var builder = Gui.builder().setStructure(structure);
        for (int index = 0; index < gui.size(); index++) {
            builder.addIngredient(SLOT_KEYS.charAt(index), item(gui.slot(index), questPlayer));
        }
        return builder.build();
    }

    private static String[] structure(final int rows) {
        final String[] structure = new String[rows];
        for (int row = 0; row < rows; row++) {
            structure[row] = SLOT_KEYS.substring(row * 9, row * 9 + 9);
        }
        return structure;
    }

    private Item item(final GuiSlot slot, final PaperPlayer questPlayer) {
        if (slot == null || slot.empty()) {
            return Item.EMPTY;
        }
        final ItemStack stack = itemStack(slot);
        if (!slot.clickable()) {
            return Item.simple(new ItemWrapper(stack));
        }
        return new CoreGuiSlotItem(new ItemWrapper(stack), slot, questPlayer);
    }

    private ItemStack itemStack(final GuiSlot slot) {
        final Material material = Objects.requireNonNull(
                Material.matchMaterial(slot.material()),
                () -> "Core supplied an unknown GUI item material: " + slot.material());
        final ItemStack stack = new ItemStack(material);
        final ItemMeta meta = stack.getItemMeta();
        if (meta == null) {
            return stack;
        }
        if (!slot.displayName().isBlank()) {
            meta.displayName(notQuests.parse(slot.displayName()));
        }
        if (!slot.lore().isEmpty()) {
            meta.lore(slot.lore().stream().map(notQuests::parse).toList());
        }
        stack.setItemMeta(meta);
        applySkullTexture(stack, slot.skullTexture());
        return stack;
    }

    private void applySkullTexture(final ItemStack stack, final String skullTexture) {
        if (stack.getType() != Material.PLAYER_HEAD || skullTexture == null || skullTexture.isBlank()) {
            return;
        }
        if (!(stack.getItemMeta() instanceof final SkullMeta skullMeta)) {
            return;
        }
        final var profile = Bukkit.createProfile(
                UUID.nameUUIDFromBytes(skullTexture.getBytes(StandardCharsets.UTF_8)),
                "NotQuests");
        profile.getProperties().add(new ProfileProperty("textures", skullTexture));
        skullMeta.setPlayerProfile(profile);
        stack.setItemMeta(skullMeta);
    }

    private final class CoreGuiSlotItem extends AbstractItem {
        private final ItemWrapper itemWrapper;
        private final GuiSlot slot;
        private final PaperPlayer questPlayer;

        private CoreGuiSlotItem(
                final ItemWrapper itemWrapper,
                final GuiSlot slot,
                final PaperPlayer questPlayer) {
            this.itemWrapper = itemWrapper;
            this.slot = slot;
            this.questPlayer = questPlayer;
        }

        @Override
        public ItemProvider getItemProvider(final Player viewer) {
            return itemWrapper;
        }

        @Override
        public void handleClick(final ClickType clickType, final Player player, final Click click) {
            notQuests.getCorePlugin().runGuiActions(
                    questPlayer,
                    slot.actions(),
                    notQuests.getCorePlugin()::warn);
        }
    }
}
