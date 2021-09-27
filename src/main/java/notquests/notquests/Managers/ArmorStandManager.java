package notquests.notquests.Managers;

import notquests.notquests.NotQuests;
import org.bukkit.entity.ArmorStand;

import java.util.ArrayList;

public class ArmorStandManager {
    private final NotQuests main;

    private final ArrayList<ArmorStand> armorStandsWithQuestsAttachedToThem;

    public ArmorStandManager(NotQuests main) {
        this.main = main;
        armorStandsWithQuestsAttachedToThem = new ArrayList<>();
    }
}
