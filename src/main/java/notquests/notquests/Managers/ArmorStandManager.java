package notquests.notquests.Managers;

import notquests.notquests.NotQuests;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.LivingEntity;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.UUID;

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

        if (main.getDataManager().getConfiguration().isArmorStandQuestGiverIndicatorParticleEnabled()) {
            startQuestGiverIndicatorParticleRunnable();
        }
    }

    public final NamespacedKey getAttachedQuestsShowingKey() {
        return attachedQuestsShowingKey;
    }

    public final NamespacedKey getAttachedQuestsNonShowingKey() {
        return attachedQuestsNonShowingKey;
    }

    public void addArmorStandWithQuestsAttachedToThem(final ArmorStand armorStand) {
        this.armorStandsWithQuestsAttachedToThem.add(armorStand);
    }

    public void removeArmorStandWithQuestsAttachedToThem(final ArmorStand armorStand) {
        this.armorStandsWithQuestsAttachedToThem.remove(armorStand);
    }

    public void loadAllArmorStandsFromLoadedChunks() {
        for (final World world : Bukkit.getWorlds()) {
            for (LivingEntity entity : world.getLivingEntities()) {
                if (entity instanceof ArmorStand armorStand) {
                    final PersistentDataContainer armorStandPDB = armorStand.getPersistentDataContainer();

                    boolean hasShowingQuestsPDBKey = false;
                    boolean hasNonShowingQuestsPDBKey = false;


                    if (armorStandPDB.has(main.getArmorStandManager().getAttachedQuestsShowingKey(), PersistentDataType.STRING)) {
                        hasShowingQuestsPDBKey = true;
                    }
                    if (armorStandPDB.has(main.getArmorStandManager().getAttachedQuestsNonShowingKey(), PersistentDataType.STRING)) {
                        hasNonShowingQuestsPDBKey = true;
                    }

                    if (hasShowingQuestsPDBKey || hasNonShowingQuestsPDBKey) {
                        main.getArmorStandManager().addArmorStandWithQuestsAttachedToThem(armorStand);
                    }
                }
            }
        }
    }


    public void startQuestGiverIndicatorParticleRunnable() {
        Bukkit.getServer().getScheduler().runTaskTimerAsynchronously(main, () -> {

            //Disable if Server TPS is too low
            double minimumTPS = main.getDataManager().getConfiguration().getArmorStandQuestGiverIndicatorParticleDisableIfTPSBelow();
            if (minimumTPS >= 0) {
                if (main.getPerformanceManager().getTPS() < minimumTPS) {
                    return;
                }
            }

            for (final ArmorStand armorStand : armorStandsWithQuestsAttachedToThem) {
                final Location location = armorStand.getLocation();
                armorStand.getWorld().spawnParticle(main.getDataManager().getConfiguration().getArmorStandQuestGiverIndicatorParticleType(), location.getX() - 0.25 + (Math.random() / 2), location.getY() + 1.75 + (Math.random() / 2), location.getZ() - 0.25 + (Math.random() / 2), main.getDataManager().getConfiguration().getArmorStandQuestGiverIndicatorParticleCount());

            }
        }, main.getDataManager().getConfiguration().getArmorStandQuestGiverIndicatorParticleSpawnInterval(), main.getDataManager().getConfiguration().getArmorStandQuestGiverIndicatorParticleSpawnInterval());
    }

    public final String getArmorStandName(final UUID armorStandUUID) {
        if (Bukkit.getEntity(armorStandUUID) instanceof ArmorStand armorStand) {
            return armorStand.getName();
        } else {
            return "unknown";
        }
    }

    public final String getArmorStandName(final ArmorStand armorStand) {
        return armorStand.getName();
    }
}
