/*
 * NotQuests - A Questing plugin for Minecraft Servers
 * Copyright (C) 2021-2022 Alessio Gravili
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package rocks.gravili.notquests.paper.managers;

import java.util.ArrayList;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.LivingEntity;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import rocks.gravili.notquests.paper.NotQuests;

public class ArmorStandManager {
    final NamespacedKey attachedConversationKey;
    private final NotQuests main;
    private final NamespacedKey attachedQuestsShowingKey;
    private final NamespacedKey attachedQuestsNonShowingKey;
    private final ArrayList<ArmorStand> armorStandsWithQuestsOrConversationAttachedToThem;

    public ArmorStandManager(NotQuests main) {
        this.main = main;
        armorStandsWithQuestsOrConversationAttachedToThem = new ArrayList<>();
        attachedQuestsShowingKey = new NamespacedKey(main.getMain(), "notquests-attachedQuests-showing");
        attachedQuestsNonShowingKey = new NamespacedKey(main.getMain(), "notquests-attachedQuests-nonshowing");
        attachedConversationKey = new NamespacedKey(main.getMain(), "notquests-attachedConversation");

        if (main.getConfiguration().isArmorStandQuestGiverIndicatorParticleEnabled()) {
            startQuestGiverIndicatorParticleRunnable();
        }
    }

    public final NamespacedKey getAttachedConversationKey() {
        return attachedConversationKey;
    }

    public final NamespacedKey getAttachedQuestsShowingKey() {
        return attachedQuestsShowingKey;
    }

    public final NamespacedKey getAttachedQuestsNonShowingKey() {
        return attachedQuestsNonShowingKey;
    }

    public void addArmorStandWithQuestsOrConversationAttachedToThem(final ArmorStand armorStand) {
        this.armorStandsWithQuestsOrConversationAttachedToThem.add(armorStand);
    }

    public void removeArmorStandWithQuestsOrConversationAttachedToThem(final ArmorStand armorStand) {
        this.armorStandsWithQuestsOrConversationAttachedToThem.remove(armorStand);
    }

    public void loadAllArmorStandsFromLoadedChunks() {
        for (final World world : Bukkit.getWorlds()) {
            for (LivingEntity entity : world.getLivingEntities()) {
                if (entity instanceof final ArmorStand armorStand) {
                    final PersistentDataContainer armorStandPDB = armorStand.getPersistentDataContainer();

                    boolean hasShowingQuestsPDBKey = false;
                    boolean hasNonShowingQuestsPDBKey = false;
                    boolean hasConversationPDBKey = false;

                    if (armorStandPDB.has(main.getArmorStandManager().getAttachedQuestsShowingKey(), PersistentDataType.STRING)) {
                        hasShowingQuestsPDBKey = true;
                    }
                    if (armorStandPDB.has(main.getArmorStandManager().getAttachedQuestsNonShowingKey(), PersistentDataType.STRING)) {
                        hasNonShowingQuestsPDBKey = true;
                    }
                    if (armorStandPDB.has(main.getArmorStandManager().getAttachedConversationKey(), PersistentDataType.STRING)) {
                        hasConversationPDBKey = true;
                    }

                    if (hasShowingQuestsPDBKey || hasNonShowingQuestsPDBKey || hasConversationPDBKey) {
                        main.getArmorStandManager().addArmorStandWithQuestsOrConversationAttachedToThem(armorStand);
                    }
                }
            }
        }
    }


    public void startQuestGiverIndicatorParticleRunnable() {
        Bukkit.getServer().getScheduler().runTaskTimerAsynchronously(main.getMain(), () -> {

            //Disable if Server TPS is too low
            double minimumTPS = main.getConfiguration().getArmorStandQuestGiverIndicatorParticleDisableIfTPSBelow();
            if (minimumTPS >= 0) {
                if (main.getPerformanceManager().getTPS() < minimumTPS) {
                    return;
                }
            }

            for (final ArmorStand armorStand : armorStandsWithQuestsOrConversationAttachedToThem) {
                final Location location = armorStand.getLocation();
                armorStand.getWorld().spawnParticle(main.getConfiguration().getArmorStandQuestGiverIndicatorParticleType(), location.getX() - 0.25 + (Math.random() / 2), location.getY() + 1.75 + (Math.random() / 2), location.getZ() - 0.25 + (Math.random() / 2), main.getConfiguration().getArmorStandQuestGiverIndicatorParticleCount());

            }
        }, main.getConfiguration().getArmorStandQuestGiverIndicatorParticleSpawnInterval(), main.getConfiguration().getArmorStandQuestGiverIndicatorParticleSpawnInterval());
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
