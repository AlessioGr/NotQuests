package notquests.notquests.Commands.AdminCommands;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import notquests.notquests.NotQuests;
import notquests.notquests.Structs.Quest;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Handles admin commands related to armor stands. This could be part of the CommandNotQuestsAdmin class, but I split it up
 * for better readability and maintainability.
 *
 * @author Alessio Gravili
 */
public class ArmorstandsAdminCommand {

    private final NotQuests main;

    public ArmorstandsAdminCommand(final NotQuests main) {
        this.main = main;
    }

    public void handleArmorstandsAdminCommand(final CommandSender sender, final String[] args, final Quest quest) {
        if (args.length == 3) {
            showUsage(quest, sender, args);
        } else if (args.length == 4) {
            if (args[3].equalsIgnoreCase("add")) {
                if(sender instanceof Player player){
                    ItemStack itemStack = new ItemStack(Material.GHAST_TEAR, 1);
                    //give a specialitem. clicking an armorstand with that special item will give it the pdb.

                    NamespacedKey key = new NamespacedKey(main, "notquests-item");
                    NamespacedKey QuestNameKey = new NamespacedKey(main, "notquests-questname");
                    ItemMeta itemMeta = itemStack.getItemMeta();

                    itemMeta.getPersistentDataContainer().set(key, PersistentDataType.INTEGER, 0);
                    itemMeta.getPersistentDataContainer().set(QuestNameKey, PersistentDataType.STRING, quest.getQuestName());

                    itemMeta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);

                    itemMeta.displayName(Component.text("§6Add Quest §b" + quest.getQuestName() + " §6to Armor Stand", NamedTextColor.GOLD));
                    List<Component> lore = new ArrayList<>();
                    lore.add(Component.text("§fHit an armor stand to add the Quest §b" + quest.getQuestName() + " §fto it."));
                    itemMeta.lore(lore);

                    itemStack.setItemMeta(itemMeta);

                    player.getInventory().addItem(itemStack);

                    player.sendMessage("§aYou have been given an item with which you can add this quest to armor stands!");


                }else{
                    sender.sendMessage("§cMust be a player!");
                    showUsage(quest, sender, args);
                }


            } else if (args[3].equalsIgnoreCase("remove")) {
                if(sender instanceof Player player){
                    ItemStack itemStack = new ItemStack(Material.NETHER_STAR, 1);
                    //give a specialitem. clicking an armorstand with that special item will remove the pdb.

                    NamespacedKey key = new NamespacedKey(main, "notquests-item");
                    NamespacedKey QuestNameKey = new NamespacedKey(main, "notquests-questname");

                    ItemMeta itemMeta = itemStack.getItemMeta();

                    itemMeta.getPersistentDataContainer().set(key, PersistentDataType.INTEGER, 1);
                    itemMeta.getPersistentDataContainer().set(QuestNameKey, PersistentDataType.STRING, quest.getQuestName());

                    itemMeta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);

                    itemMeta.displayName(Component.text("§cRemove Quest §b" + quest.getQuestName() + " §cfrom Armor Stand", NamedTextColor.RED));
                    List<Component> lore = new ArrayList<>();
                    lore.add(Component.text("§fHit an armor stand to remove the Quest §b" + quest.getQuestName() + " §ffrom it."));
                    itemMeta.lore(lore);

                    itemStack.setItemMeta(itemMeta);

                    player.getInventory().addItem(itemStack);

                    player.sendMessage("§aYou have been given an item with which you can remove this quest from armor stands!");


                }else{
                    sender.sendMessage("§cMust be a player!");
                    showUsage(quest, sender, args);
                }
            } else if (args[3].equalsIgnoreCase("list")) {
                showUsage(quest, sender, args);
            } else if (args[3].equalsIgnoreCase("clear")) {
                showUsage(quest, sender, args);
            } else {
                showUsage(quest, sender, args);
            }
        }else {

            sender.sendMessage("§cWrong command usage!");
            showUsage(quest, sender, args);
        }

    }


    public @Nullable List<String> handleCompletions(final CommandSender sender, final String[] args) {
        main.getDataManager().completions.clear();

        final Quest quest = main.getQuestManager().getQuest(args[1]);
        if (quest != null) {

        }
        return main.getDataManager().completions;
    }

    private void showUsage(final Quest quest, final CommandSender sender, final String[] args) {
        if (args.length == 3) {
            sender.sendMessage("§e/nquestsadmin §6edit §2" + args[1] + " §6armorstands add §3...");
            sender.sendMessage("§e/nquestsadmin §6edit §2" + args[1] + " §6armorstands remove §3...");
            sender.sendMessage("§e/nquestsadmin §6edit §2" + args[1] + " §6armorstands list");
            sender.sendMessage("§e/nquestsadmin §6edit §2" + args[1] + " §6armorstands clear");
        }else{
            sender.sendMessage("§e/nquestsadmin §6edit §2" + args[1] + " §6armorstands add §3...");
            sender.sendMessage("§e/nquestsadmin §6edit §2" + args[1] + " §6armorstands remove §3...");
            sender.sendMessage("§e/nquestsadmin §6edit §2" + args[1] + " §6armorstands list");
            sender.sendMessage("§e/nquestsadmin §6edit §2" + args[1] + " §6armorstands clear");
        }
    }

}
