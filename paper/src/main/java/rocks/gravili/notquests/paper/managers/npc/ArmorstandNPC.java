package rocks.gravili.notquests.paper.managers.npc;

import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.Nullable;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.conversation.Conversation;
import rocks.gravili.notquests.paper.structs.Quest;

public class ArmorstandNPC extends NQNPC {
  private ArmorStand cachedArmorstand;
  private final NQNPCID npcID;

  public ArmorstandNPC(final NotQuests main, final NQNPCID npcID) {
    super(main, "armorstand");
    this.npcID = npcID;
    this.cachedArmorstand = (ArmorStand) main.getMain().getServer().getEntity(npcID.getUUIDID());
    }

  private boolean updateCachedNPC() {
    if (cachedArmorstand == null) {
      cachedArmorstand = (ArmorStand) main.getMain().getServer().getEntity(npcID.getUUIDID());
    }
    return cachedArmorstand != null;
  }

  @Nullable
  @Override
  public String getName() {
    if (!updateCachedNPC()) {
      return null;
    }
    return main.getMiniMessage()
        .serialize(
            LegacyComponentSerializer.legacyAmpersand()
                .deserialize(cachedArmorstand.getName().replace("§", "&")));
  }

  @Override
  public NQNPCID getID() {
    if (!updateCachedNPC()) {
      return npcID;
    }
    return NQNPCID.fromUUID(cachedArmorstand.getUniqueId());
  }

  @Override
  public void bindToConversation(Conversation conversation) { //TODO: needed?

  }

  @Override
  public String removeQuestGiverNPCTrait(final @Nullable Boolean showQuestInNPC, final Quest quest) { //TODO: needed?
    if (!updateCachedNPC()) {
      return "Armorstand not found!";
    }
    PersistentDataContainer armorstandPDB = cachedArmorstand.getPersistentDataContainer();

    final NamespacedKey attachedQuestsKey;

    if(showQuestInNPC == null || showQuestInNPC){ //showing
      attachedQuestsKey  = main.getArmorStandManager().getAttachedQuestsShowingKey();
    }else{
      attachedQuestsKey  = main.getArmorStandManager().getAttachedQuestsNonShowingKey();
    }

    if(armorstandPDB.has(attachedQuestsKey, PersistentDataType.STRING)){
      String existingAttachedQuests = armorstandPDB.get(attachedQuestsKey, PersistentDataType.STRING);
      if(existingAttachedQuests != null && existingAttachedQuests.contains("°"+quest.getIdentifier() +"°")){


        existingAttachedQuests = existingAttachedQuests.replace("°" + quest.getIdentifier()  + "°", "°");

        //So it can go fully empty again
        boolean foundNonSeparator = false;
        for (int i = 0; i < existingAttachedQuests.length(); i++){
          char c = existingAttachedQuests.charAt(i);
          if (c != '°') {
            foundNonSeparator = true;
            break;
          }
        }

        if (!foundNonSeparator) {
          //It consists only of separators - no quests. Thus, we set this to "" and remove the PDB
          existingAttachedQuests = "";
          armorstandPDB.remove(attachedQuestsKey);
          main.getArmorStandManager().removeArmorStandWithQuestsOrConversationAttachedToThem(cachedArmorstand);
        } else {
          armorstandPDB.set(attachedQuestsKey, PersistentDataType.STRING, existingAttachedQuests);
        }


      } else {
        if(showQuestInNPC == null){
          return removeQuestGiverNPCTrait(false, quest);
        }
        return "<RED>Error: That armor stand does not have the Quest <highlight>" + quest.getIdentifier()  + "</highlight> attached to it!\n" +
            "<DARK_GREEN>Attached Quests: <highlight>" + existingAttachedQuests;
      }
    } else {
      if(showQuestInNPC == null){
        return removeQuestGiverNPCTrait(false, quest);
      }
      return "<RED>This armor stand has no quests attached to it!";
    }
    if(showQuestInNPC == null){
      return removeQuestGiverNPCTrait(false, quest);
    }
    return "";
  }

  @Override
  public String addQuestGiverNPCTrait(final @Nullable Boolean showQuestInNPC, final Quest quest) { //TODO: needed?
    if (!updateCachedNPC()) {
      return "Armorstand not found!";
    }

    PersistentDataContainer armorStandPDB = cachedArmorstand.getPersistentDataContainer();
    final NamespacedKey attachedQuestsKey;
    if (showQuestInNPC == null || showQuestInNPC) { //showing
      attachedQuestsKey = main.getArmorStandManager().getAttachedQuestsShowingKey();
    } else {
      attachedQuestsKey = main.getArmorStandManager().getAttachedQuestsNonShowingKey();
    }
    if (armorStandPDB.has(attachedQuestsKey, PersistentDataType.STRING)) {
      String existingAttachedQuests = armorStandPDB.get(attachedQuestsKey, PersistentDataType.STRING);


      if (existingAttachedQuests != null) {

        if( existingAttachedQuests.equals(quest.getIdentifier() ) || existingAttachedQuests.contains("°" + quest.getIdentifier() +"°") ) {
          if(showQuestInNPC == null){
            return addQuestGiverNPCTrait(false, quest);
          }
          return "<RED>Error: That armor stand already has the Quest <highlight>" + quest.getIdentifier()  + "</highlight> attached to it!\n"
              + "<RED>Attached Quests: <highlight>" + existingAttachedQuests;
        }

      }

      if(existingAttachedQuests != null && existingAttachedQuests.length() >= 1){
        if(   existingAttachedQuests.charAt(existingAttachedQuests.length()-1) == '°' ){
          existingAttachedQuests += (quest.getIdentifier() +"°") ;
        } else {
          existingAttachedQuests += "°" + quest.getIdentifier()  + "°";
        }

      } else {
        existingAttachedQuests += "°" + quest.getIdentifier()  + "°";
      }

      armorStandPDB.set(attachedQuestsKey, PersistentDataType.STRING, existingAttachedQuests);

    }else {
      armorStandPDB.set(attachedQuestsKey, PersistentDataType.STRING, "°" + quest.getIdentifier()  + "°");


      //Since this is the first Quest added to it:
      main.getArmorStandManager().addArmorStandWithQuestsOrConversationAttachedToThem(cachedArmorstand);

    }
    if(showQuestInNPC == null){
      return addQuestGiverNPCTrait(false, quest);
    }
    return "";
  }

  @Override
  public final Entity getEntity() {
    if (!updateCachedNPC()) {
      return null;
    }
    return cachedArmorstand;
  }
}
