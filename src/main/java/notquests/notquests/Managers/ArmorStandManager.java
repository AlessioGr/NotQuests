package notquests.notquests.Managers;

import notquests.notquests.NotQuests;
import org.bukkit.Bukkit;
import org.bukkit.Location;
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

        if (main.getDataManager().getConfiguration().isQuestGiverIndicatorParticleEnabled()) {
            startQuestGiverIndicatorParticleRunnable();
        }
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


    public void startQuestGiverIndicatorParticleRunnable(){
        Bukkit.getServer().getScheduler().runTaskTimerAsynchronously(main, new Runnable() {
            @Override
            public void run() {
                for(final ArmorStand armorStand : armorStandsWithQuestsAttachedToThem){
                    final Location location = armorStand.getLocation();

                    armorStand.getWorld().spawnParticle(main.getDataManager().getConfiguration().getQuestGiverIndicatorParticleType(), location.getX() - 0.25 + (Math.random() / 2), location.getY() + 1.75 + (Math.random() / 2), location.getZ() - 0.25 + (Math.random() / 2), main.getDataManager().getConfiguration().getQuestGiverIndicatorParticleCount());

                }
            }
        }, main.getDataManager().getConfiguration().getQuestGiverIndicatorParticleSpawnInterval(), main.getDataManager().getConfiguration().getQuestGiverIndicatorParticleSpawnInterval());
    }

}
