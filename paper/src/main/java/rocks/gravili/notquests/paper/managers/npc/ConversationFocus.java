package rocks.gravili.notquests.paper.managers.npc;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import rocks.gravili.notquests.paper.NotQuests;

import java.util.Arrays;

public class ConversationFocus extends BukkitRunnable {


    private final Player player;
    private final Location baseLocation;
    private Location previousLocation;
    private final Entity entity;
    private FocusState state;
    private float[] rotations;
    private int tick;
    private final float tickToRotate;

    public ConversationFocus(NotQuests main, Player player, Entity entity) {
        this.player = player;
        this.baseLocation = player.getLocation().clone();
        this.baseLocation.setY(0d);
        this.previousLocation = player.getLocation().clone();
        this.entity = entity;
        this.state = FocusState.FOCUSING;
        this.tickToRotate = main.getDataManager().getConfiguration().getCitizensFocusingRotateTime() / 2f;
        this.getRotation();
    }

    @Override
    public void run() {
        if (!this.player.isOnline()) {
            this.cancel();
            return;
        }
        this.player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 4, 2, false, false));
        if (this.player.getLocation().subtract(0, this.player.getLocation().getY(), 0).distanceSquared(this.baseLocation) > 0.04) {
            this.cancel();
            this.player.removePotionEffect(PotionEffectType.SLOW);
            return;
        }
        if (this.player.getLocation().getYaw() != this.previousLocation.getYaw() || this.player.getLocation().getPitch() != this.previousLocation.getPitch()) {
            this.tick = 0;
            this.state = FocusState.WAITING;
            this.previousLocation = this.player.getLocation();
            return;
        }
        if (this.state == FocusState.WAITING && this.tick == 5) {
            this.state = FocusState.FOCUSING;
            this.tick = -1;
            this.getRotation();
        } else if (this.state == FocusState.FOCUSING && this.tick <= this.tickToRotate) {
            Location target = this.player.getLocation().clone();
            target.setYaw((1 - this.tick / this.tickToRotate) * this.rotations[0] + (this.tick / this.tickToRotate) * this.rotations[2]);
            target.setPitch((1 - this.tick / this.tickToRotate) * this.rotations[1] + (this.tick / this.tickToRotate) * this.rotations[3]);
            this.player.teleport(target);
        } else if (this.state == FocusState.FOCUSING) {
            this.state = FocusState.DONE;
        }
        this.previousLocation = this.player.getLocation();

        this.tick++;
    }

    private void getRotation() {
        this.rotations = new float[4];
        this.rotations[0] = this.player.getEyeLocation().getYaw();
        this.rotations[1] = this.player.getEyeLocation().getPitch();
        Location vector = this.entity.getLocation().clone().add(0, this.entity.getHeight() - 0.2, 0).subtract(this.player.getEyeLocation().clone());
        this.rotations[2] = Location.normalizeYaw(vector.getYaw() + 180f);
        this.rotations[3] = vector.getPitch();
    }

    enum FocusState {
        WAITING,
        FOCUSING,
        DONE
    }

}
