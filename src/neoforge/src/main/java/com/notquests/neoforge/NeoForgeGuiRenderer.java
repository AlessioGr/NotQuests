package com.notquests.neoforge;

import com.google.common.collect.ImmutableMultimap;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.item.component.TooltipDisplay;
import org.slf4j.Logger;

import com.notquests.core.NotQuestsPlugin;
import com.notquests.core.gui.GuiService.GuiSlot;
import com.notquests.core.gui.GuiService.ResolvedGui;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.OptionalInt;
import java.util.UUID;
import java.util.function.Consumer;

final class NeoForgeGuiRenderer {
    private final NotQuestsPlugin plugin;
    private final NeoForgeText text;
    private final Logger logger;

    NeoForgeGuiRenderer(
            final NotQuestsPlugin plugin,
            final NeoForgeText text,
            final Logger logger) {
        this.plugin = plugin;
        this.text = text;
        this.logger = logger;
    }

    boolean open(
            final ServerPlayer player,
            final ResolvedGui gui,
            final NeoForgePlayer platformPlayer) {
        if (player == null || gui == null || platformPlayer == null) {
            return false;
        }
        try {
            final SimpleContainer container = new SimpleContainer(gui.size());
            for (final GuiSlot slot : gui.slots()) {
                if (slot.index() >= 0 && slot.index() < gui.size()) {
                    container.setItem(slot.index(), item(slot));
                }
            }
            final OptionalInt opened = player.openMenu(new NotQuestsMenuFactory(
                    container,
                    gui,
                    slot -> click(platformPlayer, slot),
                    text.component(gui.title())));
            return opened.isPresent();
        } catch (final RuntimeException exception) {
            logger.warn(
                    "Failed to render NotQuests GUI for player '{}'.",
                    player.getGameProfile().name(),
                    exception);
            return false;
        }
    }

    private void click(final NeoForgePlayer player, final GuiSlot slot) {
        if (slot == null || !slot.clickable()) {
            return;
        }
        plugin.runGuiActions(player, slot.actions(), warning -> {
            if (warning != null && !warning.isBlank()) {
                logger.warn("NotQuests GUI action warning: {}", warning);
            }
        });
    }

    private ItemStack item(final GuiSlot slot) {
        final ItemStack stack = new ItemStack(item(slot.material()));
        stack.set(
                DataComponents.TOOLTIP_DISPLAY,
                TooltipDisplay.DEFAULT.withHidden(DataComponents.ATTRIBUTE_MODIFIERS, true));
        try {
            applySkullTexture(stack, slot);
        } catch (final RuntimeException exception) {
            logger.warn("Skipping invalid NotQuests GUI skull texture for slot {}.", slot.index(), exception);
        }
        if (!slot.displayName().isBlank()) {
            stack.set(DataComponents.CUSTOM_NAME, text.component(slot.displayName()));
        }
        if (!slot.lore().isEmpty()) {
            final List<Component> lore = new ArrayList<>();
            for (final String line : slot.lore()) {
                lore.add(text.component(line));
            }
            stack.set(DataComponents.LORE, new ItemLore(lore));
        }
        return stack;
    }

    private static void applySkullTexture(final ItemStack stack, final GuiSlot slot) {
        if (!stack.is(Items.PLAYER_HEAD) || slot.skullTexture().isBlank()) {
            return;
        }
        final Property texture = new Property("textures", slot.skullTexture());
        final PropertyMap properties = new PropertyMap(ImmutableMultimap.of("textures", texture));
        final GameProfile profile = new GameProfile(
                UUID.nameUUIDFromBytes(slot.skullTexture().getBytes(StandardCharsets.UTF_8)),
                "NotQuests",
                properties);
        stack.set(DataComponents.PROFILE, ResolvableProfile.createResolved(profile));
    }

    private static Item item(final String material) {
        final String cleaned = material.toLowerCase(Locale.ROOT);
        try {
            final Identifier id = Identifier.parse(cleaned.contains(":") ? cleaned : "minecraft:" + cleaned);
            return BuiltInRegistries.ITEM.get(id)
                    .map(reference -> reference.value())
                    .orElseThrow(() -> new IllegalArgumentException("Unknown GUI material: " + material));
        } catch (final RuntimeException exception) {
            throw new IllegalArgumentException("Invalid GUI material: " + material, exception);
        }
    }

    private final class NotQuestsMenuFactory implements MenuProvider {
        private final SimpleContainer container;
        private final ResolvedGui gui;
        private final Consumer<GuiSlot> clickHandler;
        private final Component title;

        private NotQuestsMenuFactory(
                final SimpleContainer container,
                final ResolvedGui gui,
                final Consumer<GuiSlot> clickHandler,
                final Component title) {
            this.container = container;
            this.gui = gui;
            this.clickHandler = clickHandler;
            this.title = title;
        }

        @Override
        public Component getDisplayName() {
            return title;
        }

        @Override
        public AbstractContainerMenu createMenu(
                final int containerId,
                final Inventory inventory,
                final Player player) {
            return new NeoForgeNotQuestsMenu(containerId, inventory, container, gui, clickHandler);
        }

        @Override
        public void writeClientSideData(
                final AbstractContainerMenu menu,
                final RegistryFriendlyByteBuf buffer) {
            // NotQuests uses vanilla chest menu types, so the client needs no extra NeoForge payload.
        }
    }

    private final class NeoForgeNotQuestsMenu extends ChestMenu {
        private final ResolvedGui gui;
        private final Consumer<GuiSlot> clickHandler;

        private NeoForgeNotQuestsMenu(
                final int containerId,
                final Inventory inventory,
                final SimpleContainer container,
                final ResolvedGui gui,
                final Consumer<GuiSlot> clickHandler) {
            super(menuType(gui.rows()), containerId, inventory, container, gui.rows());
            this.gui = gui;
            this.clickHandler = clickHandler;
        }

        @Override
        public void clicked(
                final int slotIndex,
                final int buttonNum,
                final ContainerInput input,
                final Player player) {
            if (slotIndex >= 0 && slotIndex < gui.size()) {
                clickHandler.accept(gui.slot(slotIndex));
                return;
            }
            super.clicked(slotIndex, buttonNum, input, player);
        }

        @Override
        public ItemStack quickMoveStack(final Player player, final int index) {
            return ItemStack.EMPTY;
        }
    }

    private static MenuType<ChestMenu> menuType(final int rows) {
        return switch (rows) {
            case 1 -> MenuType.GENERIC_9x1;
            case 2 -> MenuType.GENERIC_9x2;
            case 3 -> MenuType.GENERIC_9x3;
            case 4 -> MenuType.GENERIC_9x4;
            case 5 -> MenuType.GENERIC_9x5;
            default -> MenuType.GENERIC_9x6;
        };
    }
}
