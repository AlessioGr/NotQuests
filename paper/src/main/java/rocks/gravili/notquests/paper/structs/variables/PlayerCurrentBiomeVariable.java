package rocks.gravili.notquests.paper.structs.variables;

import org.bukkit.block.Biome;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.structs.QuestPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class PlayerCurrentBiomeVariable extends Variable<String>{
    public PlayerCurrentBiomeVariable(NotQuests main) {
        super(main);
    }

    @Override
    public String getValue(QuestPlayer questPlayer, Object... objects) {
        if (questPlayer != null) {
            return questPlayer.getPlayer().getLocation().getBlock().getBiome().name().toLowerCase(Locale.ROOT);
        } else {
            return null;
        }
    }

    @Override
    public boolean setValueInternally(String newValue, QuestPlayer questPlayer, Object... objects) {
        return false;
    }


    @Override
    public List<String> getPossibleValues(QuestPlayer questPlayer, Object... objects) {
        List<String> possibleValues = new ArrayList<>();
        for (Biome biome : Biome.values()) {
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
