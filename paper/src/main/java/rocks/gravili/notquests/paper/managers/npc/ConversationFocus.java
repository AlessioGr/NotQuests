package rocks.gravili.notquests.paper.managers.npc;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.conversation.Conversation;
import rocks.gravili.notquests.paper.conversation.ConversationPlayer;

public class ConversationFocus extends BukkitRunnable {

    private final NotQuests main;
    private final Player player;
    private final Location baseLocation;
    private Location previousLocation;
    private final Entity entity;
    private FocusState state;
    private float[] rotations;
    private int tick;
    private final float tickToRotate;

    private final PotionEffect potionEffect;

    private final Conversation conversation;

    public ConversationFocus(final NotQuests main, final Player player, final Entity entity, final Conversation conversation) {
        this.main = main;
        this.player = player;
        this.baseLocation = player.getLocation().clone();
        this.baseLocation.setY(0d);
        this.previousLocation = player.getLocation().clone();
        this.entity = entity;
        this.state = FocusState.FOCUSING;
        this.tickToRotate = main.getDataManager().getConfiguration().getCitizensFocusingRotateTime() / 2f;
        this.rotations = this.getRotation();
        this.conversation = conversation;

        this.potionEffect = new PotionEffect(PotionEffectType.SLOW, 4, 2, false, false);
    }

    @Override
    public void run() {
        if (!this.player.isOnline()) {
            this.cancel();
            return;
        }

        // Cancel if conversation not active anymore
        assert main.getConversationManager() != null;
        final ConversationPlayer currentOpenConversationPlayer = main.getConversationManager().getOpenConversation(player.getUniqueId());
        if (currentOpenConversationPlayer == null || currentOpenConversationPlayer.getConversation() == null || !currentOpenConversationPlayer.getConversation().getIdentifier().equals(conversation.getIdentifier())) {
            this.cancel();
            return;
        }

        this.player.addPotionEffect(potionEffect);

        // Cancel if player moves away too far from the original location

        if (!this.player.getLocation().getWorld().getUID().equals(this.baseLocation.getWorld().getUID())
                || this.player.getLocation().subtract(0, this.player.getLocation().getY(), 0).distanceSquared(this.baseLocation) > 0.04) {
            this.cancel();
            if (main.getConfiguration().isCitizensFocusingCancelConversationWhenTooFar()){
                main.sendMessage(
                        player,
                        main.getLanguageManager()
                                .getString("chat.conversations.ended-previous-conversation", player, conversation));
                main.getConversationManager().stopConversation(currentOpenConversationPlayer);
            }
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
            this.rotations = this.getRotation();
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

    private float[] getRotation() {
        final float[] newRotations = new float[4];
        newRotations[0] = this.player.getEyeLocation().getYaw();
        newRotations[1] = this.player.getEyeLocation().getPitch();
        // - 0.2 : a magic value (get the eye's height of a fake player)
        Vector entityPosition = this.entity.getLocation().clone().add(0, this.entity.getHeight() - 0.2, 0).toVector();
        Vector playerPosition = this.player.getEyeLocation().toVector();

        final Vector vector = playerPosition.subtract(entityPosition);
        // 180f : a magic value (mojang hate geometry)
        newRotations[2] = Location.normalizeYaw((float) (180f - Math.toDegrees(Math.atan2(vector.getX(), vector.getZ()))));
        newRotations[3] = Location.normalizePitch((float) Math.toDegrees(Math.atan2(vector.getY(), Math.sqrt(vector.getX() * vector.getX() + vector.getZ() * vector.getZ()))));
        return newRotations;
    }

    enum FocusState {
        WAITING,
        FOCUSING,
        DONE
    }

}
