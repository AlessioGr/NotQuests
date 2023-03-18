package rocks.gravili.notquests.paper.gui.icon;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.gui.GuiContext;


import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class ItemHelper {

    private static final String EMPTY_STRING_EXPRESSION = "EMPTY";

    public static ItemStack assembleItemStack(Icon icon, NotQuests notQuests, GuiContext guiContext) {
        var material = fetchMaterial(icon, guiContext);

        var itemStack = new ItemStack(material);
        var itemMeta = itemStack.getItemMeta();

        var questPlayer = guiContext.getPlayer() != null ? notQuests.getQuestPlayerManager().getActiveQuestPlayer(guiContext.getPlayer().getUniqueId()) : null;

        // Replace placeholders of displayname
        if (icon.pathToDisplayName() != null) {
            if (icon.pathToDisplayName().equals(EMPTY_STRING_EXPRESSION)) {
                itemMeta.displayName(Component.empty());
            } else {
                var displayName = notQuests.getLanguageManager().getString(
                        icon.pathToDisplayName(), guiContext.getPlayer(), guiContext.getAsObjectArray()
                );

                itemMeta.displayName(notQuests.parse(displayName));
            }
        }

        // Replace placeholders of lore
        if (icon.pathToLore() != null) {
            if (icon.pathToLore().equals(EMPTY_STRING_EXPRESSION)) {
                itemMeta.lore(List.of(Component.empty()));
            } else {
                var lore = notQuests.getLanguageManager().getStringList(
                        icon.pathToLore(), guiContext.getPlayer(), guiContext.getAsObjectArray()
                );

                var newLore = new ArrayList<String>();
                    for (String loreString : lore) {
                        if (loreString.contains("%QUESTREWARDS%")) {
                            if (questPlayer != null && (guiContext.getQuest() != null || guiContext.getActiveQuest() != null )) {
                                newLore.add(loreString.replace("%QUESTREWARDS%", ""));
                                newLore.addAll(notQuests.getQuestManager().getQuestRewardsList(
                                        guiContext.getQuest() == null ? guiContext.getActiveQuest().getQuest() : guiContext.getQuest(), questPlayer));
                            }
                            continue;
                        }
                        if (loreString.contains("%QUESTREQUIREMENTS%")) {
                            if (questPlayer != null && (guiContext.getQuest() != null || guiContext.getActiveQuest() != null )) {
                                newLore.add(loreString.replace("%QUESTREQUIREMENTS%", ""));
                                newLore.addAll(notQuests.getQuestManager().getQuestRequirementsList(
                                        guiContext.getQuest() == null ? guiContext.getActiveQuest().getQuest() : guiContext.getQuest(), questPlayer));
                            }
                            continue;
                        }
                        if (loreString.contains("%WRAPPEDQUESTDESCRIPTION%")) {
                            if (guiContext.getQuest() != null || guiContext.getActiveQuest() != null) {
                                var quest = guiContext.getQuest() == null ? guiContext.getActiveQuest().getQuest() : guiContext.getQuest();
                                newLore.add(loreString.replace("%WRAPPEDQUESTDESCRIPTION%", ""));
                                newLore.addAll(quest.getQuestDescriptionList(notQuests.getConfiguration().guiQuestDescriptionMaxLineLength));
                                continue;
                            }
                        }
                        newLore.add(loreString);
                    }


                itemMeta.lore(newLore.stream().map(notQuests::parse).collect(Collectors.toList()));
            }
        }

        // Handle skull textures
        if (material == Material.PLAYER_HEAD) {
            if (icon.skullTexture() != null) {
                var texture = icon.skullTexture();
                SkullMeta meta = (SkullMeta) itemMeta;
                PlayerProfile prof = Bukkit.createProfile(UUID.randomUUID(), null);
                prof.getProperties().add(
                                new ProfileProperty(
                                        "textures",
                                        texture
                                ));
                meta.setPlayerProfile(prof);
                itemStack.setItemMeta(meta);
            } else {
                itemStack.setItemMeta(itemMeta);
            }
        } else {
            itemStack.setItemMeta(itemMeta);
        }
        return itemStack;
    }

    public static Material fetchMaterial(Icon item, GuiContext guiContext) {
        if (!item.material().equals("%QUEST_ITEM_MATERIAL%")) {
            return Material.valueOf(item.material());
        }
        if (guiContext.getActiveQuest() != null) {
            return guiContext.getActiveQuest().getQuest().getTakeItem().getType();
        }
        if (guiContext.getQuest() != null) {
            return guiContext.getQuest().getTakeItem().getType();
        }
        return Material.STONE;
    }
}
