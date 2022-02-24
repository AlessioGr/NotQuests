/*
 * NotQuests - A Questing plugin for Minecraft Servers
 * Copyright (C) 2021-2022 Alessio Gravili
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package rocks.gravili.notquests.spigot.structs.objectives;

import cloud.commandframework.ArgumentDescription;
import cloud.commandframework.Command;
import cloud.commandframework.arguments.standard.StringArgument;
import cloud.commandframework.meta.CommandMeta;
import cloud.commandframework.paper.PaperCommandManager;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import rocks.gravili.notquests.spigot.NotQuests;
import rocks.gravili.notquests.spigot.commands.NotQuestColors;
import rocks.gravili.notquests.spigot.structs.ActiveObjective;
import rocks.gravili.notquests.spigot.structs.Quest;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class TalkToNPCObjective extends Objective {

    private int npcToTalkID = -1;

    private UUID armorStandUUID = null;

    public TalkToNPCObjective(NotQuests main) {
        super(main);
    }

    public static void handleCommands(NotQuests main, PaperCommandManager<CommandSender> manager, Command.Builder<CommandSender> addObjectiveBuilder) {
        manager.command(addObjectiveBuilder.literal("TalkToNPC")
                .argument(StringArgument.<CommandSender>newBuilder("NPC or Armorstand").withSuggestionsProvider((context, lastString) -> {
                    ArrayList<String> completions = new ArrayList<>();
                    for (final NPC npc : CitizensAPI.getNPCRegistry().sorted()) {
                        completions.add("" + npc.getId());
                    }
                    completions.add("armorstand");
                    final List<String> allArgs = context.getRawInput();
                    final Audience audience = main.adventure().sender(context.getSender());
                    main.getUtilManager().sendFancyCommandCompletion(audience, allArgs.toArray(new String[0]), "[NPC ID / 'armorstand']", "");

                    return completions;
                }).build(), ArgumentDescription.of("ID of the Citizens NPC or 'armorstand' to whom you should talk."))
                .meta(CommandMeta.DESCRIPTION, "Adds a new TalkToNPC Objective to a quest.")
                .handler((context) -> {
                    final Audience audience = main.adventure().sender(context.getSender());
                    final Quest quest = context.get("quest");

                    final String npcIDOrArmorstand = context.get("NPC or Armorstand");


                    if (!npcIDOrArmorstand.equalsIgnoreCase("armorstand")) {
                        if (!main.getIntegrationsManager().isCitizensEnabled()) {
                            audience.sendMessage(MiniMessage.miniMessage().deserialize(
                                    NotQuestColors.errorGradient + "Error: Any kind of NPC stuff has been disabled, because you don't have the Citizens plugin installed on your server. You need to install the Citizens plugin in order to use Citizen NPCs. You can, however, use armor stands as an alternative. To do that, just enter 'armorstand' instead of the NPC ID."
                            ));
                            return;
                        }
                        int npcID;
                        try {
                            npcID = Integer.parseInt(npcIDOrArmorstand);
                        } catch (NumberFormatException e) {
                            audience.sendMessage(
                                    MiniMessage.miniMessage().deserialize(
                                            NotQuestColors.errorGradient + "Invalid NPC ID."
                                    )
                            );
                            return;
                        }

                        TalkToNPCObjective talkToNPCObjective = new TalkToNPCObjective(main);
                        talkToNPCObjective.setNPCtoTalkID(npcID);

                        main.getObjectiveManager().addObjective(talkToNPCObjective, context);
                    } else {//Armorstands
                        if (context.getSender() instanceof Player player) {

                            ItemStack itemStack = new ItemStack(Material.PAPER, 1);
                            //give a specialitem. clicking an armorstand with that special item will remove the pdb.

                            NamespacedKey key = new NamespacedKey(main.getMain(), "notquests-item");
                            NamespacedKey QuestNameKey = new NamespacedKey(main.getMain(), "notquests-questname");

                            ItemMeta itemMeta = itemStack.getItemMeta();
                            //Only paper List<Component> lore = new ArrayList<>();
                            List<String> lore = new ArrayList<>();

                            assert itemMeta != null;

                            itemMeta.getPersistentDataContainer().set(QuestNameKey, PersistentDataType.STRING, quest.getQuestName());
                            itemMeta.getPersistentDataContainer().set(key, PersistentDataType.INTEGER, 5);

                            //Only paper itemMeta.displayName(Component.text("§dCheck Armor Stand", NamedTextColor.LIGHT_PURPLE));
                            itemMeta.setDisplayName("§dAdd TalkToNPC Objective to Armor Stand");
                            //Only paper lore.add(Component.text("§fRight-click an Armor Stand to see which Quests are attached to it."));
                            lore.add("§fRight-click an Armor Stand to add the following objective to it:");
                            lore.add("§eTalkToNPC §fObjective of Quest §b" + quest.getQuestName() + "§f.");

                            itemMeta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
                            //Only paper itemMeta.lore(lore);

                            itemMeta.setLore(lore);
                            itemStack.setItemMeta(itemMeta);

                            player.getInventory().addItem(itemStack);

                            audience.sendMessage(
                                    MiniMessage.miniMessage().deserialize(
                                            NotQuestColors.successGradient + "You have been given an item with which you can add the TalkToNPC Objective to an armor stand. Check your inventory!"
                                    )
                            );


                        } else {
                            audience.sendMessage(
                                    MiniMessage.miniMessage().deserialize(
                                            NotQuestColors.errorGradient + "Must be a player!"
                                    )
                            );
                        }
                    }


                }));

    }

    public void setArmorStandUUID(final UUID armorStandUUID) {
        this.armorStandUUID = armorStandUUID;
    }

    @Override
    public String getObjectiveTaskDescription(final String eventualColor, final Player player) {
        String toReturn = "";
        if (main.getIntegrationsManager().isCitizensEnabled() && getNPCtoTalkID() != -1) {
            final NPC npc = CitizensAPI.getNPCRegistry().getById(getNPCtoTalkID());
            if (npc != null) {
                toReturn = main.getLanguageManager().getString("chat.objectives.taskDescription.talkToNPC.base", player)
                        .replace("%EVENTUALCOLOR%", eventualColor)
                        .replace("%NAME%", npc.getName());
            } else {
                toReturn = "    <GRAY>" + eventualColor + "The target NPC is currently not available!";
            }
        } else {
            if (getNPCtoTalkID() != -1) {
                toReturn += "    <RED>Error: Citizens plugin not installed. Contact an admin.";
            } else { //Armor Stands
                final UUID armorStandUUID = getArmorStandUUID();
                if (armorStandUUID != null) {
                    toReturn = main.getLanguageManager().getString("chat.objectives.taskDescription.talkToNPC.base", player)
                            .replace("%EVENTUALCOLOR%", eventualColor)
                            .replace("%NAME%", main.getArmorStandManager().getArmorStandName(armorStandUUID));
                } else {
                    toReturn += "    <GRAY>" + eventualColor + "The target Armor Stand is currently not available!";
                }
            }
        }
        return toReturn;
    }

    @Override
    public void save(FileConfiguration configuration, String initialPath) {
        configuration.set(initialPath + ".specifics.NPCtoTalkID", getNPCtoTalkID());
        if (getArmorStandUUID() != null) {
            configuration.set(initialPath + ".specifics.ArmorStandToTalkUUID", getArmorStandUUID().toString());
        } else {
            configuration.set(initialPath + ".specifics.ArmorStandToTalkUUID", null);
        }
    }

    @Override
    public void load(FileConfiguration configuration, String initialPath) {
        npcToTalkID = configuration.getInt(initialPath + ".specifics.NPCtoTalkID", -1);
        if (npcToTalkID != -1) {
            armorStandUUID = null;
        } else {
            final String armorStandUUIDString = configuration.getString(initialPath + ".specifics.ArmorStandToTalkUUID");
            if (armorStandUUIDString != null) {
                armorStandUUID = UUID.fromString(armorStandUUIDString);
            } else {
                armorStandUUID = null;
            }

        }
    }

    @Override
    public void onObjectiveUnlock(ActiveObjective activeObjective) {

    }


    public final int getNPCtoTalkID() {
        return npcToTalkID;
    }

    public final UUID getArmorStandUUID() {
        return armorStandUUID;
    }

    public void setNPCtoTalkID(final int npcToTalkID) {
        this.npcToTalkID = npcToTalkID;
    }
}