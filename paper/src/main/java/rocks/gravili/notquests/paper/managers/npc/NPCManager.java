package rocks.gravili.notquests.paper.managers.npc;

import java.util.ArrayList;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.managers.data.Category;
import rocks.gravili.notquests.paper.structs.Quest;

public class NPCManager {
  private final NotQuests main;

  private final ArrayList<NQNPC> npcs;
  public NPCManager(final NotQuests main){
    this.main = main;
    npcs = new ArrayList<>();
  }

  public final NQNPC getOrCreateNQNpc(final String type, final int npcID){
    main.getLogManager().debug("Called getOrCreateNQNpc with type " + type + " and npcID " + npcID);
    for(final NQNPC nqnpc : npcs){
      if(nqnpc.getID() == npcID && nqnpc.getNPCType().equalsIgnoreCase(type)){
        return nqnpc;
      }
    }

    if(type.equalsIgnoreCase("Citizens")){
      final CitizensNPC newCitizensNPC = new CitizensNPC(main, npcID);
      npcs.add(newCitizensNPC);
      return newCitizensNPC;
    }

    return null;
  }

  public void cleanupBuggedNPCs() { //TODO: Currently only works with Citizens :(
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
          for (final String npcNumber : npcsConfigurationSection.getKeys(false)) {

            if (category.getQuestsConfig() != null) {
              final NQNPC nqNPC = NQNPC.fromConfig(main, category.getQuestsConfig(), "quests." + questName + ".npcs." + npcNumber + ".npcData");
              if (nqNPC != null) {
                final boolean questShowing = category.getQuestsConfig().getBoolean("quests." + questName + ".npcs." + npcNumber + ".questShowing", true);
                // call the callback with the result
                main.getLogManager().info("Attaching Quest with the name <highlight>" + quest.getQuestName() + "</highlight> to NPC with the ID <highlight>" + nqNPC.getID() + " </highlight>and name <highlight>" + nqNPC.getName());
                quest.removeNPC(nqNPC);
                quest.bindToNPC(nqNPC, questShowing);
              } else {
                main.getLogManager().warn("Error attaching npc with ID <highlight>" + category.getQuestsConfig().getInt("quests." + questName + ".npcs." + npcNumber + ".npcID")
                    + "</highlight> to quest <highlight>" + quest.getQuestName() + "</highlight> - NPC not found.");
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
    main.getLogManager().info("Requesting cleaning of bugged NPCs in loadNPCData()...");
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
}
