package notquests.notquests;

import net.citizensnpcs.api.persistence.Persist;
import net.citizensnpcs.api.trait.Trait;
import net.citizensnpcs.api.util.DataKey;
import notquests.notquests.Structs.Quest;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.plugin.java.JavaPlugin;

public class QuestGiverNPCTrait extends Trait {

    NotQuests plugin = null;
    boolean SomeSetting = false;
    // see the 'Persistence API' section
    @Persist("mysettingname")
    boolean automaticallyPersistedSetting = false;
    private int particleTimer = 0;

    public QuestGiverNPCTrait() {
        super("nquestgiver");
        plugin = JavaPlugin.getPlugin(NotQuests.class);
    }

    // Here you should load up any values you have previously saved (optional).
    // This does NOT get called when applying the trait for the first time, only loading onto an existing npc at server start.
    // This is called AFTER onAttach so you can load defaults in onAttach and they will be overridden here.
    // This is called BEFORE onSpawn, npc.getEntity() will return null.
    public void load(DataKey key) {
        //SomeSetting = key.getBoolean("SomeSetting", false);
    }

    // Save settings for this NPC (optional). These values will be persisted to the Citizens saves file
    public void save(DataKey key) {
        // key.setInt("SomeSetting",SomeSetting);
    }

    // An example event handler. All traits will be registered automatically as Bukkit Listeners.
    @EventHandler
    public void click(net.citizensnpcs.api.event.NPCRightClickEvent event) {
        //Handle a click on a NPC. The event has a getNPC() method.
        //Be sure to check event.getNPC() == this.getNPC() so you only handle clicks on this NPC!
        //npc.getTrait(FollowTrait.class) new FollowTrait();
        if (event.getNPC() == this.getNPC()) {
            final Player player = event.getClicker();
            plugin.getQuestManager().sendQuestsPreviewOfQuestShownNPCs(getNPC(), player);
        }

    }

    // Called every tick
    @Override
    public void run() {
        if (npc.isSpawned()) {
            if (particleTimer >= 10) {
                particleTimer = 0;
                Location location = getNPC().getEntity().getLocation();

                getNPC().getEntity().getWorld().spawnParticle(Particle.VILLAGER_ANGRY, location.getX() - 0.25 + (Math.random() / 2), location.getY() + 1.75 + (Math.random() / 2), location.getZ() - 0.25 + (Math.random() / 2), 1);

                //System.out.println("§eSpawned particle!");
            }

            particleTimer += 1;

        }

    }

    //Run code when your trait is attached to a NPC.
    //This is called BEFORE onSpawn, so npc.getEntity() will return null
    //This would be a good place to load configurable defaults for new NPCs.
    @Override
    public void onAttach() {
        plugin.getServer().getLogger().info("§aNotQuests > NPC with the ID §b" + npc.getId() + " §aand name §b" + npc.getName() + " §ahas been assigned the Quest Giver trait!");
    }

    // Run code when the NPC is despawned. This is called before the entity actually despawns so npc.getEntity() is still valid.
    @Override
    public void onDespawn() {
    }

    //Run code when the NPC is spawned. Note that npc.getEntity() will be null until this method is called.
    //This is called AFTER onAttach and AFTER Load when the server is started.
    @Override
    public void onSpawn() {

    }

    //run code when the NPC is removed. Use this to tear down any repeating tasks.
    @Override
    public void onRemove() {
        //REMOVEEEE FROM QUEST
        plugin.getServer().getLogger().info("§aNotQuests > NPC with the ID §b" + npc.getId() + " §aand name §b" + npc.getName() + " §ahas been removed!");
        for (Quest quest : plugin.getQuestManager().getQuestsAttachedToNPC(getNPC())) {
            quest.removeNPC(getNPC());
        }
    }

}