package rocks.gravili.notquests.paper.managers.npc;

import java.util.ArrayList;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.api.trait.Trait;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
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

    main.getLogManager().info("Checking for bugged NPCs...");

    int buggedNPCsFound = 0;
    int allNPCsFound = 0;
    //Clean up bugged NPCs with quests attached wrongly
    final ArrayList<Trait> traitsToRemove = new ArrayList<>();
    for (final NPC npc : CitizensAPI.getNPCRegistry().sorted()) {
      allNPCsFound += 1;

      final NQNPC nqnpc = getOrCreateNQNpc("Citizens", npc.getId());
      //No quests attached to NPC => check if it has the trait
      if (main.getQuestManager().getAllQuestsAttachedToNPC(nqnpc).isEmpty() && (main.getConversationManager().getConversationForNPC(nqnpc) == null)) {
        for (final Trait trait : npc.getTraits()) {
          if (trait.getName().contains("questgiver")) {
            traitsToRemove.add(trait);
          }
        }

        if (!Bukkit.isPrimaryThread()) {
          Bukkit.getScheduler().runTask(main.getMain(), () -> {
            for (Trait trait : traitsToRemove) {
              npc.removeTrait(trait.getClass());
            }
          });
        } else {
          for (Trait trait : traitsToRemove) {
            npc.removeTrait(trait.getClass());
          }
        }

        if (!traitsToRemove.isEmpty()) {
          buggedNPCsFound += 1;
          final String mmNpcName = main.getMiniMessage().serialize(
              LegacyComponentSerializer.legacyAmpersand().deserialize(npc.getName().replace("§","&")));

          main.getLogManager().info("  Bugged trait removed from npc with ID <highlight>" + npc.getId() + "</highlight> and name <highlight>" + mmNpcName + "</highlight>!");
        }


      } else {
        //TODO: Remove debug shit or improve performance
        final ArrayList<String> attachedQuestNames = new ArrayList<>();
        for (final Quest attachedQuest : main.getQuestManager().getAllQuestsAttachedToNPC(nqnpc)) {
          attachedQuestNames.add(attachedQuest.getQuestName());
        }
        main.getLogManager().info("  NPC with the ID: <highlight>" + npc.getId() + "</highlight> is not bugged, because it has the following quests attached: <highlight>" + attachedQuestNames + "</highlight>");

      }
      traitsToRemove.clear();

    }
    if (buggedNPCsFound == 0) {
      main.getLogManager().info("No bugged NPCs found! Amount of checked NPCs: <highlight>" + allNPCsFound + "</highlight>");

    } else {
      main.getLogManager().info("<YELLOW><highlight>" + buggedNPCsFound + "</highlight> bugged NPCs have been found and removed! Amount of checked NPCs: <highlight>" + allNPCsFound + "</highlight>");

    }
  }

  public void loadNPCData() {
    if (main.getDataManager().isAlreadyLoadedQuests()) {
      for (final Category category : main.getDataManager().getCategories()) {
        loadNPCData(category);
      }
    } else {
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
