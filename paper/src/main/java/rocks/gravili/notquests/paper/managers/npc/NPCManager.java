package rocks.gravili.notquests.paper.managers.npc;

import java.util.ArrayList;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.api.trait.Trait;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.structs.Quest;

public class NPCManager {
  private final NotQuests main;

  private final ArrayList<NQNPC> npcs;
  public NPCManager(final NotQuests main){
    this.main = main;
    npcs = new ArrayList<>();
  }

  public final NQNPC getOrCreateNQNpc(final String type, final int npcID){
    for(final NQNPC nqnpc : npcs){
      if(nqnpc.getID() == npcID && nqnpc.getNPCType().equalsIgnoreCase(type)){
        return nqnpc;
      }
    }

    if(type.equalsIgnoreCase("Citizens")){
      return new CitizensNPC(main, npcID);
    }

    return null;
  }

  public void cleanupBuggedNPCs() { //TODO: NPC
    if (!main.getIntegrationsManager().isCitizensEnabled()) {
      main.getLogManager().warn("Checking for bugged NPCs has been cancelled, because Citizens is not installed on your server. The Citizens plugin is needed for NPC stuff to work.");

      return;
    }
    main.getLogManager().info("Checking for bugged NPCs...");

    int buggedNPCsFound = 0;
    int allNPCsFound = 0;
    //Clean up bugged NPCs with quests attached wrongly
    final ArrayList<Trait> traitsToRemove = new ArrayList<>();
    for (final NPC npc : CitizensAPI.getNPCRegistry().sorted()) {
      allNPCsFound += 1;

      //No quests attached to NPC => check if it has the trait
      final NQNPC
      if (getAllQuestsAttachedToNPC(npc).size() == 0 && (main.getConversationManager().getConversationForNPC(npc == null)) {
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
        for (final Quest attachedQuest : getAllQuestsAttachedToNPC(npc)) {
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


}
