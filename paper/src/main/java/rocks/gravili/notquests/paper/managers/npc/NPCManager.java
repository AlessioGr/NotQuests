package rocks.gravili.notquests.paper.managers.npc;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.managers.data.Category;
import rocks.gravili.notquests.paper.structs.Quest;

public class NPCManager {
  private final NotQuests main;

  private final ArrayList<Consumer<NQNPC>> npcSelectionActions;

  private final ArrayList<NQNPC> npcs;
  public NPCManager(final NotQuests main){
    this.main = main;
    npcs = new ArrayList<>();
    npcSelectionActions = new ArrayList<>();
  }

  public final ArrayList<String> getAllNPCsString(){ //TODO: Armor stands?
    final ArrayList<String> npcs = new ArrayList<>();
    if(main.getIntegrationsManager().isCitizensEnabled()){
      for(final int npcID : main.getIntegrationsManager().getCitizensManager().getAllNPCIDs()){
        npcs.add("citizens:"+npcID);
      }
    }
    if(main.getIntegrationsManager().isZNPCsEnabled()){
      for(final int npcID : main.getIntegrationsManager().getZNPCsManager().getAllNPCIDs()){
        npcs.add("znpcs:"+npcID);
      }
    }
    return npcs;
  }

  public final @Nullable NQNPC getOrCreateNQNpc(final @NotNull String type, final @NotNull NQNPCID npcID){
    main.getLogManager().debug("Called getOrCreateNQNpc with type %s and npcID %s", type, npcID);
    for(final NQNPC nqnpc : npcs){
      if(nqnpc.getID().equals(npcID) && nqnpc.getNPCType().equalsIgnoreCase(type)){
        return nqnpc;
      }
    }

    if(type.equalsIgnoreCase("citizens")){
      if(main.getIntegrationsManager().isCitizensEnabled()){
        final CitizensNPC newCitizensNPC = new CitizensNPC(main, npcID);
        npcs.add(newCitizensNPC);
        return newCitizensNPC;
      }else{
        main.getLogManager().warn("Tried to create a Citizens NQNPC with ID <highlight>%s</highlight>, but Citizens is not active/loaded.", npcID);
        return null;
      }
    }else if(type.equalsIgnoreCase("armorstand")){
      final ArmorstandNPC newArmorStandNPC = new ArmorstandNPC(main, npcID);
      npcs.add(newArmorStandNPC);
      return newArmorStandNPC;
    }else if(type.equalsIgnoreCase("znpcs")){
      if(main.getIntegrationsManager().isZNPCsEnabled()){
        final ZNPCNPC newZNPCNPC = new ZNPCNPC(main, npcID);
        npcs.add(newZNPCNPC);
        return newZNPCNPC;
      }else{
        main.getLogManager().warn("Tried to create a zNPCs NQNPC with ID <highlight>%s</highlight>, but zNPCs is not active/loaded.", npcID);
        return null;
      }
    }

    return null;
  }

  public void cleanupBuggedNPCs() { //TODO: Currently only works with Citizens
    if(main.getIntegrationsManager().isCitizensEnabled()){
      main.getIntegrationsManager().getCitizensManager().cleanupBuggedNPCs();
    }
  }

  public void loadNPCData() {
    if(main.getDataManager().isDisabled()){
      main.getLogManager().info("Skipped loading NPC data, because NotQuests has been disabled due to a previous error");
      return;
    }
    if (main.getDataManager().isAlreadyLoadedQuests()) {
      for (final Category category : main.getDataManager().getCategories()) {
        loadNPCData(category);
      }
    } else {
      if(main.getDataManager().isDisabled()){
        main.getLogManager().info("Tried to load NPC data before quest data was loaded. NotQuests has skipped scheduling another load though, because NotQuests has been disabled due to a previous error");
        return;
      }

      main.getLogManager().info("Tried to load NPC data before quest data was loaded. NotQuests is scheduling another load...");

      Bukkit.getScheduler().runTaskLaterAsynchronously(main.getMain(), () -> {
        if (!main.getDataManager().isAlreadyLoadedNPCs()) {
          main.getLogManager().info("Trying to load NPC quest data again...");
          main.getDataManager().loadNPCData();
        }
      }, 60);
    }

  }

  public void loadNPCData(final Category category) {
    main.getLogManager().info("Loading NPC data...");


    if(category.getQuestsConfig() == null){
      main.getLogManager().warn("Skipped loading NPC data because the entire quests configuration of the category <highlight>" + category.getCategoryFullName() + "</highlight> was null. This should never happen.");
      return;
    }

    try {
      final ConfigurationSection questsConfigurationSetting = category.getQuestsConfig().getConfigurationSection("quests");
      if (questsConfigurationSetting != null) {
        if (!Bukkit.isPrimaryThread()) {
          Bukkit.getScheduler().runTask(main.getMain(), () -> {
            loadNPCDataInternal(category, questsConfigurationSetting);
          });
        } else {
          loadNPCDataInternal(category, questsConfigurationSetting);
        }
      } else {
        main.getLogManager().info("Skipped loading NPC data because the 'quests' configuration section of the quests configuration for the category <highlight>" + category.getCategoryFullName() + "</highlight> was null.");
      }
      main.getLogManager().info("NPC data loaded!");
      main.getDataManager().setAlreadyLoadedNPCs(true);
    } catch (Exception ex) {
      main.getDataManager().disablePluginAndSaving("Plugin disabled, because there was an exception while loading quests NPC data.", ex);
      return;
    }
  }

  private void loadNPCDataInternal(final Category category, final ConfigurationSection questsConfigurationSetting) {
    for (final String questName : questsConfigurationSetting.getKeys(false)) {
      final Quest quest = main.getQuestManager().getQuest(questName);
      if (quest != null) {
        //NPC
        final ConfigurationSection npcsConfigurationSection = category.getQuestsConfig().getConfigurationSection("quests." + questName + ".npcs");
        if (npcsConfigurationSection != null) {
          for (final String npcIdentifyingString : npcsConfigurationSection.getKeys(false)) {

            if (category.getQuestsConfig() != null) {
              final NQNPC nqNPC = NQNPC.fromConfig(main, category.getQuestsConfig(), "quests." + questName + ".npcs." + npcIdentifyingString + ".npcData");
              if (nqNPC != null) {
                final boolean questShowing = category.getQuestsConfig().getBoolean("quests." + questName + ".npcs." + npcIdentifyingString + ".questShowing", true);
                // call the callback with the result
                main.getLogManager().info("Attaching Quest with the name <highlight>" + quest.getIdentifier() + "</highlight> to NPC with the ID <highlight>" + nqNPC.getID() + " </highlight>and name <highlight>" + nqNPC.getName());
                quest.removeNPC(nqNPC);
                quest.bindToNPC(nqNPC, questShowing);
              } else {
                main.getLogManager().warn("Error attaching npc with ID <highlight>" + category.getQuestsConfig().getInt("quests." + questName + ".npcs." + npcIdentifyingString + ".npcID")
                    + "</highlight> to quest <highlight>" + quest.getIdentifier() + "</highlight> - NPC not found.");
              }
            } else {
              main.getLogManager().warn("Error: quests data is null");
            }
          }
        }
      } else {
        main.getLogManager().warn("Error: Quest not found while trying to load NPC");
      }
    }
    main.getLogManager().debug("Requesting cleaning of bugged NPCs in loadNPCData()...");
    main.getNPCManager().cleanupBuggedNPCs();
  }

  public final boolean foundAnyNPCs() {
    /*boolean foundNPC = false;
    try{
      for (final NPC ignored : CitizensAPI.getNPCRegistry().sorted()) {
        foundNPC = true;
        break;
      }
      if (foundNPC && !isAlreadyLoadedNPCs()) {
        loadNPCData();
      }
    }catch (Exception e){
      if(main.getConfiguration().isDebug()){
        e.printStackTrace();
      }
    }*/
    return true;
  }



  public void handleRightClickNQNPCSelectionWithAction(final @NotNull Consumer<NQNPC> actionWhenSelected, final @NotNull Player player, final @Nullable String successMessage, final @Nullable String displayName, final @Nullable String... lore){
    final ItemStack itemStack = new ItemStack(Material.PAPER, 1);
    //give a specialitem. clicking an armorstand with that special item will remove the pdb.

    final NamespacedKey key = new NamespacedKey(main.getMain(), "notquests-nqnpc-selector-with-action");

    final ItemMeta itemMeta = itemStack.getItemMeta();
    final List<Component> loreComponentList = new ArrayList<>();

    itemMeta.displayName(main.parse(Objects.requireNonNullElse(displayName,
        "<LIGHT_PURPLE>Right click any NQNPC to execute action")));

    if(lore != null){
      for (final String loreLine : lore) {
        loreComponentList.add(main.parse(loreLine));
      }

    }

    npcSelectionActions.add(actionWhenSelected);


    itemMeta.getPersistentDataContainer().set(key, PersistentDataType.INTEGER, npcSelectionActions.size()-1);


    itemMeta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);

    itemMeta.lore(loreComponentList);

    itemStack.setItemMeta(itemMeta);

    player.getInventory().addItem(itemStack);

    player.sendMessage(main.parse(
        Objects.requireNonNullElse(successMessage, "<success>You have been given an item with which you can execute a certain action when right-clicking any NQNPC. Check your inventory!")
    ));
  }

  public final ArrayList<Consumer<NQNPC>> getNPCSelectionActions() {
    return npcSelectionActions;
  }

  public void executeNPCSelectionAction(final NQNPC nqnpc, final int npcSelectionActionID){
    if(npcSelectionActionID < npcSelectionActions.size()){
      npcSelectionActions.get(npcSelectionActionID).accept(nqnpc);
      //npcSelectionActions.remove(npcSelectionActionID); //This can shift the IDs badly. lets just leave it, else we'd need to use a hashmap
    }
  }
}
