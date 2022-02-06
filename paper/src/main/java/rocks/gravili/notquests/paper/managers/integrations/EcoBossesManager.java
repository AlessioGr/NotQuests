package rocks.gravili.notquests.paper.managers.integrations;


import com.willfp.ecobosses.EcoBossesPlugin;
import com.willfp.ecobosses.bosses.Bosses;
import com.willfp.ecobosses.bosses.EcoBoss;
import org.bukkit.Location;
import rocks.gravili.notquests.paper.NotQuests;

import java.util.ArrayList;
import java.util.Collection;

public class EcoBossesManager {
    private final NotQuests main;
    private final ArrayList<String> bossNames;
    private final EcoBossesPlugin ecoBossesPlugin;

    public EcoBossesManager(final NotQuests main) {
        this.main = main;
        ecoBossesPlugin = EcoBossesPlugin.getInstance();

        bossNames = new ArrayList<>();


        try{
            for(EcoBoss ecoBoss : Bosses.values()){
                try{
                    //bossNames.add(ecoBoss.getId());
                    //main.getLogManager().info("Registered EcoBoss: <highlight>" + ecoBoss.getId());
                    final String id = ecoBoss.getId();

                    bossNames.add(id);
                    main.getLogManager().info("Registered EcoBoss: <highlight>" + id);
                }catch (Exception e){
                    final String id = (String) ecoBoss.getClass().getMethod("getName").invoke(ecoBoss);
                    bossNames.add(id);
                    main.getLogManager().info("Registered EcoBoss: <highlight>" + id);
                }

            }
            main.getLogManager().info("Registered <highlight>" + Bosses.values().size() + "</highlight> EcoBosses.");

        }catch (Exception ignored){
            main.getLogManager().warn("Failed to add EcoBosses mobs. Are you on the latest version?");
        }

    }

    public final Collection<String> getBossNames() {
       return bossNames;
    }

    public final boolean isEcoBoss(final String bossToSpawnType) {
        return Bosses.getByID(bossToSpawnType) != null;
    }

    public void spawnMob(String mobToSpawnType, Location location, int amount) {
        EcoBoss foundEcoBoss = Bosses.getByID(mobToSpawnType);
        if (foundEcoBoss == null) {
            main.getLogManager().warn("Tried to spawn EcoBoss, but the spawn " + mobToSpawnType + " was not found.");
            return;
        }
        if (location == null) {
            main.getLogManager().warn("Tried to spawn EcoBoss, but the spawn location is invalid.");
            return;
        }
        if (location.getWorld() == null) {
            main.getLogManager().warn("Tried to spawn EcoBoss, but the spawn location world is invalid.");
            return;
        }

        try{
            foundEcoBoss.spawn(location);
        }catch (NoSuchMethodError e){
            try{
                foundEcoBoss.getClass().getMethod("spawn", Location.class).invoke(foundEcoBoss, location);
            }catch (Exception ignored) {
                main.getLogManager().warn("Failed to add EcoBosses mobs. Are you on the latest version?");
            }

        }


    }
}
