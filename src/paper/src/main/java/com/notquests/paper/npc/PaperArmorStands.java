package com.notquests.paper.npc;

import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;

import com.notquests.core.npc.NQNPCID;
import com.notquests.core.npc.NpcAttachments.Indicator;
import com.notquests.paper.NotQuests;

/** Paper armor-stand lookup and indicator rendering. All attachment decisions come from core. */
public class PaperArmorStands {
    private final NotQuests main;

    public PaperArmorStands(final NotQuests main) {
        this.main = main;
    }

    public void renderIndicator(
            final NQNPCID npcId,
            final Indicator indicator) {
        if (npcId == null || indicator == null || !indicator.showParticle()) {
            return;
        }
        final var entity = main.getMain().getServer().getEntity(npcId.getUUIDID());
        if (!(entity instanceof final ArmorStand armorStand) || !armorStand.isValid()) {
            return;
        }
        final Location location = armorStand.getLocation();
        armorStand.getWorld().spawnParticle(
                main.particle(indicator.particleType()),
                location.getX() - 0.25 + Math.random() / 2,
                location.getY() + 1.75 + Math.random() / 2,
                location.getZ() - 0.25 + Math.random() / 2,
                indicator.particleCount());
    }

    public String getArmorStandName(final ArmorStand armorStand) {
        return armorStand.getName();
    }
}
