package rocks.gravili.notquests.paper.structs.variables;

import org.bukkit.block.Biome;
import org.bukkit.entity.Player;
import rocks.gravili.notquests.paper.NotQuests;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class PlayerCurrentBiomeVariable extends Variable<String>{
    public PlayerCurrentBiomeVariable(NotQuests main) {
        super(main);
    }

    @Override
    public String getValue(Player player, Object... objects) {
        if (player != null) {
            return player.getLocation().getBlock().getBiome().name().toLowerCase(Locale.ROOT);
        } else {
            return null;
        }
    }

    @Override
    public boolean setValue(String newValue, Player player, Object... objects) {
        return false;
    }


    @Override
    public List<String> getPossibleValues(Player player, Object... objects) {
        List<String> possibleValues = new ArrayList<>();
        for(Biome biome : Biome.values()){
            possibleValues.add(biome.name().toLowerCase(Locale.ROOT));
        }
        return possibleValues;
    }

    @Override
    public String getPlural() {
        return "Biomes";
    }

    @Override
    public String getSingular() {
        return "Biome";
    }
}
