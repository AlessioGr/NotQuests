package notquests.notquests.Managers;

import notquests.notquests.NotQuests;
import org.bukkit.entity.ArmorStand;

import java.util.ArrayList;

public class ArmorstandManager {
    private final NotQuests main;

    private final ArrayList<ArmorStand> armorStandsWithQuestsAttachedToThem;

    public ArmorstandManager(NotQuests main) {
        this.main = main;
        armorStandsWithQuestsAttachedToThem = new ArrayList<>();
    }
}
