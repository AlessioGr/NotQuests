package notquests.notquests.Managers;

import notquests.notquests.NotQuests;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.ArmorStand;

import java.util.ArrayList;

public class ArmorStandManager {
    private final NotQuests main;
    private final NamespacedKey attachedQuestsShowingKey;
    private final NamespacedKey attachedQuestsNonShowingKey;


    private final ArrayList<ArmorStand> armorStandsWithQuestsAttachedToThem;

    public ArmorStandManager(NotQuests main) {
        this.main = main;
        armorStandsWithQuestsAttachedToThem = new ArrayList<>();
        attachedQuestsShowingKey = new NamespacedKey(main, "notquests-attachedQuests-showing");
        attachedQuestsNonShowingKey = new NamespacedKey(main, "notquests-attachedQuests-nonshowing");
    }

    public final NamespacedKey getAttachedQuestsShowingKey() {
        return attachedQuestsShowingKey;
    }

    public final NamespacedKey getAttachedQuestsNonShowingKey() {
        return attachedQuestsNonShowingKey;
    }

    public void addArmorStandWithQuestsAttachedToThem(final ArmorStand armorStand){
        this.armorStandsWithQuestsAttachedToThem.add(armorStand);
    }

    public void removeArmorStandWithQuestsAttachedToThem(final ArmorStand armorStand){
        this.armorStandsWithQuestsAttachedToThem.remove(armorStand);
    }
}
