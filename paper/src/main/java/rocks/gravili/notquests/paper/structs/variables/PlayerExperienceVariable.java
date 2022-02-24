package rocks.gravili.notquests.paper.structs.variables;

import org.bukkit.entity.Player;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.structs.QuestPlayer;

import java.util.List;

public class PlayerExperienceVariable extends Variable<Integer>{
    public PlayerExperienceVariable(NotQuests main) {
        super(main);
        setCanSetValue(true);
    }

    @Override
    public Integer getValue(QuestPlayer questPlayer, Object... objects) {
        if (questPlayer != null) {
            return getPlayerExp(questPlayer.getPlayer());
        } else {
            return null;
        }
    }

    @Override
    public boolean setValueInternally(Integer newValue, QuestPlayer questPlayer, Object... objects) {
        if (questPlayer != null) {
            questPlayer.getPlayer().setExp(0);
            questPlayer.getPlayer().setLevel(0);
            questPlayer.getPlayer().giveExp(newValue);
            return true;
        } else {
            return false;
        }
    }


    @Override
    public List<String> getPossibleValues(QuestPlayer questPlayer, Object... objects) {
        return null;
    }

    @Override
    public String getPlural() {
        return "Experience";
    }

    @Override
    public String getSingular() {
        return "Experience";
    }



    /*
     * Code taken from https://www.spigotmc.org/threads/how-to-get-players-exp-points.239171/
     * by DOGC_Kyle
     */
    public int getExpToLevelUp(int level){
        if(level <= 15){
            return 2*level+7;
        } else if(level <= 30){
            return 5*level-38;
        } else {
            return 9*level-158;
        }
    }
    // Calculate total experience up to a level
    public int getExpAtLevel(int level){
        if(level <= 16){
            return (int) (Math.pow(level,2) + 6*level);
        } else if(level <= 31){
            return (int) (2.5*Math.pow(level,2) - 40.5*level + 360.0);
        } else {
            return (int) (4.5*Math.pow(level,2) - 162.5*level + 2220.0);
        }
    }
    // Calculate player's current EXP amount
    public int getPlayerExp(Player player){
        int exp = 0;
        int level = player.getLevel();

        // Get the amount of XP in past levels
        exp += getExpAtLevel(level);

        // Get amount of XP towards next level
        exp += Math.round(getExpToLevelUp(level) * player.getExp());

        return exp;
    }

}
