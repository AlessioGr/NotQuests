package notquests.notquests.Commands.AdminCommands;


import notquests.notquests.NotQuests;
import notquests.notquests.Structs.Quest;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

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
        }else if (args.length == 4) {
            if (args[3].equalsIgnoreCase("check")) {
                if(sender instanceof Player player){
                    ItemStack itemStack = new ItemStack(Material.LEATHER, 1);
                    //give a specialitem. clicking an armorstand with that special item will remove the pdb.

                    NamespacedKey key = new NamespacedKey(main, "notquests-item");

                    ItemMeta itemMeta = itemStack.getItemMeta();
                    //Only paper List<Component> lore = new ArrayList<>();
                    List<String> lore = new ArrayList<>();


                    itemMeta.getPersistentDataContainer().set(key, PersistentDataType.INTEGER, 4);

                    //Only paper itemMeta.displayName(Component.text("§dCheck Armor Stand", NamedTextColor.LIGHT_PURPLE));
                    itemMeta.setDisplayName("§dCheck Armor Stand");
                    //Only paper lore.add(Component.text("§fRight-click an Armor Stand to see which Quests are attached to it."));
                    lore.add("§fRight-click an Armor Stand to see which Quests are attached to it.");

                    itemMeta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);


                    //Only paper itemMeta.lore(lore);

                    itemMeta.setLore(lore);

                    itemStack.setItemMeta(itemMeta);

                    player.getInventory().addItem(itemStack);

                    player.sendMessage("§aYou have been given an item with which you can check armor stands!");


                }else{
                    sender.sendMessage("§cMust be a player!");
                    showUsage(quest, sender, args);
                }
            }else{
                showUsage(quest, sender, args);
            }

        } else if (args.length == 5) {
            if (args[3].equalsIgnoreCase("add")) {
                boolean showing = true;
                boolean chosenIfShowing = false;
                if(args[4].equalsIgnoreCase("no") || args[4].equalsIgnoreCase("false")){
                    showing = false;
                    chosenIfShowing = true;
                }else if(args[4].equalsIgnoreCase("yes") || args[4].equalsIgnoreCase("true")){
                    showing = true;
                    chosenIfShowing = true;
                }
                if(!chosenIfShowing){
                    sender.sendMessage("§cWrong last argument!");
                    return;
                }

                if(sender instanceof Player player) {
                    ItemStack itemStack = new ItemStack(Material.GHAST_TEAR, 1);
                    //give a specialitem. clicking an armorstand with that special item will give it the pdb.

                    NamespacedKey key = new NamespacedKey(main, "notquests-item");
                    NamespacedKey QuestNameKey = new NamespacedKey(main, "notquests-questname");
                    ItemMeta itemMeta = itemStack.getItemMeta();

                    //only paper List<Component> lore = new ArrayList<>();
                    List<String> lore = new ArrayList<>();

                    if (showing) {
                        itemMeta.getPersistentDataContainer().set(key, PersistentDataType.INTEGER, 0);
                        //only paper itemMeta.displayName(Component.text("§6Add showing Quest §b" + quest.getQuestName() + " §6to Armor Stand", NamedTextColor.GOLD));
                        itemMeta.setDisplayName("§6Add showing Quest §b" + quest.getQuestName() + " §6to Armor Stand");
                        //only paper lore.add(Component.text("§fHit an armor stand to add the showing Quest §b" + quest.getQuestName() + " §fto it."));
                        lore.add("§fHit an armor stand to add the showing Quest §b" + quest.getQuestName() + " §fto it.");

                    } else {
                        itemMeta.getPersistentDataContainer().set(key, PersistentDataType.INTEGER, 1);
                        //only paper itemMeta.displayName(Component.text("§6Add non-showing Quest §b" + quest.getQuestName() + " §6to Armor Stand", NamedTextColor.GOLD));
                        itemMeta.setDisplayName("§6Add non-showing Quest §b" + quest.getQuestName() + " §6to Armor Stand");

                        //only paper lore.add(Component.text("§fHit an armor stand to add the non-showing Quest §b" + quest.getQuestName() + " §fto it."));

                        lore.add("§fHit an armor stand to add the non-showing Quest §b" + quest.getQuestName() + " §fto it.");


                    }
                    itemMeta.getPersistentDataContainer().set(QuestNameKey, PersistentDataType.STRING, quest.getQuestName());

                    itemMeta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);


                    //only paper itemMeta.lore(lore);
                    itemMeta.setLore(lore);

                    itemStack.setItemMeta(itemMeta);

                    player.getInventory().addItem(itemStack);

                    player.sendMessage("§aYou have been given an item with which you can add this quest to armor stands!");


                }else{
                    sender.sendMessage("§cMust be a player!");
                    showUsage(quest, sender, args);
                }


            } else if (args[3].equalsIgnoreCase("remove")) {
                boolean showing = true;
                boolean chosenIfShowing = false;
                if(args[4].equalsIgnoreCase("no") || args[4].equalsIgnoreCase("false")){
                    showing = false;
                    chosenIfShowing = true;
                }else if(args[4].equalsIgnoreCase("yes") || args[4].equalsIgnoreCase("true")){
                    showing = true;
                    chosenIfShowing = true;
                }
                if(!chosenIfShowing){
                    sender.sendMessage("§cWrong last argument!");
                    return;
                }
                if(sender instanceof Player player) {
                    ItemStack itemStack = new ItemStack(Material.NETHER_STAR, 1);
                    //give a specialitem. clicking an armorstand with that special item will remove the pdb.

                    NamespacedKey key = new NamespacedKey(main, "notquests-item");
                    NamespacedKey QuestNameKey = new NamespacedKey(main, "notquests-questname");

                    ItemMeta itemMeta = itemStack.getItemMeta();
                    //only paper List<Component> lore = new ArrayList<>();
                    List<String> lore = new ArrayList<>();

                    if (showing) {
                        itemMeta.getPersistentDataContainer().set(key, PersistentDataType.INTEGER, 2);
                        //itemMeta.displayName(Component.text("§cRemove showing Quest §b" + quest.getQuestName() + " §cfrom Armor Stand", NamedTextColor.RED));
                        itemMeta.setDisplayName("§cRemove showing Quest §b" + quest.getQuestName() + " §cfrom Armor Stand");

                        //only paper lore.add(Component.text("§fHit an armor stand to remove the showing Quest §b" + quest.getQuestName() + " §ffrom it."));
                        lore.add("§fHit an armor stand to remove the showing Quest §b" + quest.getQuestName() + " §ffrom it.");
                    } else {
                        itemMeta.getPersistentDataContainer().set(key, PersistentDataType.INTEGER, 3);
                        //only paper itemMeta.displayName(Component.text("§cRemove non-showing Quest §b" + quest.getQuestName() + " §cfrom Armor Stand", NamedTextColor.RED));
                        itemMeta.setDisplayName("§cRemove non-showing Quest §b" + quest.getQuestName() + " §cfrom Armor Stand");
                        //only paper lore.add(Component.text("§fHit an armor stand to remove the non-showing Quest §b" + quest.getQuestName() + " §ffrom it."));
                        lore.add("§fHit an armor stand to remove the non-showing Quest §b" + quest.getQuestName() + " §ffrom it.");
                    }
                    itemMeta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);

                    itemMeta.getPersistentDataContainer().set(QuestNameKey, PersistentDataType.STRING, quest.getQuestName());


                    //only paper itemMeta.lore(lore);
                    itemMeta.setLore(lore);

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


    public List<String> handleCompletions(final CommandSender sender, final String[] args) {
        main.getDataManager().completions.clear();

        final Quest quest = main.getQuestManager().getQuest(args[1]);
        if (quest != null) {
            if (args.length == 4) {
                main.getDataManager().completions.add("add");
                main.getDataManager().completions.add("remove");
                main.getDataManager().completions.add("check");

                main.getDataManager().completions.add("list");
                main.getDataManager().completions.add("clear");
                return main.getDataManager().completions;
            }else if(args.length == 5){
                if (args[3].equalsIgnoreCase("add")) {
                    main.getDataManager().completions.add("yes");
                    main.getDataManager().completions.add("no");
                    return main.getDataManager().completions;
                }else if (args[3].equalsIgnoreCase("remove")) {
                    main.getDataManager().completions.add("yes");
                    main.getDataManager().completions.add("no");
                    return main.getDataManager().completions;
                }
            }
        }
        return main.getDataManager().completions;
    }

    private void showUsage(final Quest quest, final CommandSender sender, final String[] args) {
        if (args.length == 3) {
            sender.sendMessage("§e/nquestsadmin §6edit §2" + args[1] + " §6armorstands add §3 §3[ShowInArmorstand (yes/no)]");
            sender.sendMessage("§e/nquestsadmin §6edit §2" + args[1] + " §6armorstands remove §3[ShowInArmorstand (yes/no)]");
            sender.sendMessage("§e/nquestsadmin §6edit §2" + args[1] + " §6armorstands check");

            sender.sendMessage("§e/nquestsadmin §6edit §2" + args[1] + " §6armorstands list [WIP]");
            sender.sendMessage("§e/nquestsadmin §6edit §2" + args[1] + " §6armorstands clear [WIP]");
        }else{
            sender.sendMessage("§e/nquestsadmin §6edit §2" + args[1] + " §6armorstands add §3 §3[ShowInArmorstand (yes/no)]");
            sender.sendMessage("§e/nquestsadmin §6edit §2" + args[1] + " §6armorstands remove §3[ShowInArmorstand (yes/no)]");
            sender.sendMessage("§e/nquestsadmin §6edit §2" + args[1] + " §6armorstands check");

            sender.sendMessage("§e/nquestsadmin §6edit §2" + args[1] + " §6armorstands list [WIP]");
            sender.sendMessage("§e/nquestsadmin §6edit §2" + args[1] + " §6armorstands clear [WIP]");
        }
    }

}
