package com.notquests.neoforge;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemLore;

import com.notquests.core.NotQuestsPlugin;
import com.notquests.core.npc.NQNPCID;
import com.notquests.core.npc.NpcAttachments.Indicator;
import com.notquests.core.npc.NpcAttachments;
import com.notquests.core.platform.NotQuestsAdapter;
import com.notquests.core.platform.PlatformPlayer;

import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;

/** Native NeoForge armor-stand lookup, item materialization, and interaction effects. */
final class NeoForgeNpcAttachments {
    private final NotQuestsPlugin plugin;
    private final NeoForgeText text;
    private final Supplier<MinecraftServer> server;

    NeoForgeNpcAttachments(
            final NotQuestsPlugin plugin,
            final NeoForgeText text,
            final Supplier<MinecraftServer> server) {
        this.plugin = plugin;
        this.text = text;
        this.server = server;
    }

    void renderArmorStandIndicator(
            final NQNPCID npcId,
            final Indicator indicator) {
        if (indicator == null || !indicator.showParticle()) {
            return;
        }
        final MinecraftServer minecraftServer = server.get();
        if (minecraftServer == null || npcId == null || npcId.getUUIDID() == null) {
            return;
        }
        final SimpleParticleType particle = indicatorParticle(indicator.particleType());
        if (particle == null) {
            return;
        }
        for (final ServerLevel level : minecraftServer.getAllLevels()) {
            if (!(level.getEntity(npcId.getUUIDID()) instanceof final ArmorStand armorStand)) {
                continue;
            }
            level.sendParticles(
                    particle,
                    armorStand.getX(),
                    armorStand.getY() + 1.75d,
                    armorStand.getZ(),
                    indicator.particleCount(),
                    0.25d,
                    0.25d,
                    0.25d,
                    0.0d);
            return;
        }
    }

    boolean giveArmorStandTool(
            final PlatformPlayer actor,
            final NotQuestsAdapter.ArmorStandToolItem tool) {
        final ServerPlayer player = player(actor);
        if (player == null) {
            return false;
        }
        final Item item = item(tool.materialId());
        if (item == null || item == Items.AIR) {
            return false;
        }
        give(player, toolStack(
                item,
                tool.itemId(),
                tool.questName(),
                tool.displayName(),
                tool.loreLines()));
        return true;
    }

    boolean giveSelectionTool(
            final PlatformPlayer actor,
            final int selectionId,
            final String displayName,
            final List<String> lore) {
        final ServerPlayer player = player(actor);
        if (player == null) {
            return false;
        }
        final ItemStack stack = selectionToolStack(selectionId, displayName, lore);
        give(player, stack);
        return true;
    }

    boolean handleEntityInteraction(
            final PlatformPlayer questPlayer,
            final Entity target,
            final ItemStack usedItem) {
        if (!(target instanceof final ArmorStand armorStand)) {
            return false;
        }
        final ServerPlayer player = player(questPlayer);
        if (player == null) {
            return false;
        }
        final String selector = NpcAttachments.selector(
                "armorstand",
                NQNPCID.fromUUID(armorStand.getUUID()));
        final String armorStandName = armorStand.getCustomName() == null
                ? ""
                : armorStand.getCustomName().getString();
        final ArmorStandTool tool = armorStandTool(usedItem);
        final var interaction = plugin.playerInteractedWithArmorStand(
                questPlayer,
                tool.selectionId(),
                tool.itemId(),
                tool.name(),
                -1,
                tool.name(),
                selector,
                armorStandName);
        for (final String message : interaction.messages()) {
            text.sendMessage(player, message);
        }
        return interaction.cancelPlatformInteraction();
    }

    private ItemStack selectionToolStack(
            final int selectionId,
            final String displayName,
            final List<String> lore) {
        final ItemStack stack = displayedItem(
                Items.PAPER,
                displayName == null || displayName.isBlank()
                        ? NpcAttachments.selectionToolDisplayName()
                        : displayName,
                lore);
        CustomData.update(
                DataComponents.CUSTOM_DATA,
                stack,
                tag -> tag.putInt("notquests_selection", selectionId));
        return stack;
    }

    private ItemStack toolStack(
            final Item item,
            final int toolId,
            final String name,
            final String displayName,
            final List<String> loreLines) {
        final ItemStack stack = displayedItem(item, displayName, loreLines);
        CustomData.update(
                DataComponents.CUSTOM_DATA,
                stack,
                tag -> {
                    tag.putInt("notquests_item", toolId);
                    if (name != null && !name.isBlank()) {
                        tag.putString("notquests_name", name);
                    }
                });
        return stack;
    }

    private ItemStack displayedItem(
            final Item item,
            final String displayName,
            final List<String> loreLines) {
        final ItemStack stack = new ItemStack(item);
        if (displayName != null && !displayName.isBlank()) {
            stack.set(DataComponents.CUSTOM_NAME, text.component(displayName));
        }
        final List<Component> lore = (loreLines == null
                        ? List.<String>of()
                        : loreLines)
                .stream()
                .map(text::component)
                .toList();
        if (!lore.isEmpty()) {
            stack.set(DataComponents.LORE, new ItemLore(lore));
        }
        return stack;
    }

    private static ArmorStandTool armorStandTool(final ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return ArmorStandTool.NONE;
        }
        final CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData == null || customData.isEmpty()) {
            return ArmorStandTool.NONE;
        }
        final var tag = customData.copyTag();
        return new ArmorStandTool(
                tag.getIntOr("notquests_item", -1),
                tag.getIntOr("notquests_selection", -1),
                tag.getStringOr("notquests_name", ""));
    }

    private static Item item(final String materialId) {
        if (materialId == null || materialId.isBlank()) {
            return null;
        }
        final Identifier identifier = Identifier.tryParse(
                materialId.contains(":") ? materialId : "minecraft:" + materialId);
        return identifier == null
                ? null
                : BuiltInRegistries.ITEM.get(identifier).map(reference -> reference.value()).orElse(null);
    }

    private SimpleParticleType indicatorParticle(final String configured) {
        if (configured == null || configured.isBlank()) {
            return null;
        }
        final String normalized = configured.toLowerCase(Locale.ROOT);
        final Identifier identifier = Identifier.tryParse(
                normalized.contains(":") ? normalized : "minecraft:" + normalized);
        if (identifier == null) {
            return null;
        }
        final ParticleType<?> particle = BuiltInRegistries.PARTICLE_TYPE
                .get(identifier)
                .map(reference -> reference.value())
                .orElse(null);
        return particle instanceof final SimpleParticleType simple ? simple : null;
    }

    private static void give(final ServerPlayer player, final ItemStack stack) {
        if (!player.addItem(stack)) {
            player.drop(stack, false);
        }
    }

    private static ServerPlayer player(final PlatformPlayer questPlayer) {
        return questPlayer instanceof final NeoForgePlayer neoForgePlayer
                ? neoForgePlayer.player()
                : null;
    }

    private record ArmorStandTool(int itemId, int selectionId, String name) {
        private static final ArmorStandTool NONE = new ArmorStandTool(-1, -1, "");
    }
}
