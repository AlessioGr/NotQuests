/*
 * NotQuests - A Questing plugin for Minecraft Servers
 * Copyright (C) 2021 Alessio Gravili
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

package rocks.gravili.notquests.paper.structs.conditions;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.managers.data.Category;
import rocks.gravili.notquests.paper.structs.Quest;
import rocks.gravili.notquests.paper.structs.QuestPlayer;
import rocks.gravili.notquests.paper.structs.objectives.Objective;

import java.util.ArrayList;

public abstract class Condition {
    protected final NotQuests main;
    private String conditionName = "";
    private long progressNeeded = 1;
    private Quest quest;
    private Objective objective;
    private boolean negated = false;
    private Category category;

    /**
     * Custom Condition description
     */
    private String description = "";
    private int conditionID = -1;


    public Condition(NotQuests main) {
        this.main = main;
        category = main.getDataManager().getDefaultCategory();
        main.allConditions.add(this); //For bStats
    }

    public final int getConditionID() {
        return conditionID;
    }

    public void setConditionID(int conditionID) {
        this.conditionID = conditionID;
    }

    public final String getDescription(){
        return description;
    }

    public void setDescription(final String description){
        this.description = description;
    }

    public void removeDescription(){
        this.description = "";
    }

    public final Category getCategory() {
        return category;
    }

    public void setCategory(final Category category) {
        this.category = category;
    }

    public void setProgressNeeded(final long progressNeeded) {
        this.progressNeeded = progressNeeded;
    }

    public void setQuest(final Quest quest) {
        this.quest = quest;
    }

    public void setObjective(final Objective objective) {
        this.objective = objective;
    }

    public final String getConditionType() {
        return main.getConditionsManager().getConditionType(this.getClass());
    }

    public long getProgressNeeded() {
        return progressNeeded;
    }

    public final Quest getQuest() {
        return quest;
    }

    public final Objective getObjective() {
        return objective;
    }

    public final String getConditionName() {
        return conditionName;
    }

    public final String getConditionIdentifier() {
        return "Name: " + conditionName + " Type: " + getConditionType() + " Description: " + getDescription() + " Negated: " + negated;
    }

    public void setConditionName(final String conditionName) {
        this.conditionName = conditionName;
    }


    /**
     * @return String if the condition is not fulfilled. Empty string if the condition is fulfilled. The String should say the still-required condition.
     */
    protected abstract String checkInternally(final QuestPlayer questPlayer);

    public String check(final QuestPlayer questPlayer){
        final String result = checkInternally(questPlayer);

        if(!isNegated()){
            if(result.isBlank()){
                return "";
            }else{
                if(description.isBlank()){
                    return result;
                }else {
                    return "<YELLOW>" + description;
                }
            }
        }else{
            if(result.isBlank()){
                return "<YELLOW>You cannot fulfill this condition: <unimportant>" + getConditionDescription(questPlayer.getPlayer());
            }else{
                return "";
            }
        }

    }

    public String getConditionDescription(Player player, Object... objects){
        if(description.isBlank()){
            return getConditionDescriptionInternally(player, objects);
        }else {
            return "<GRAY>" + description;
        }
    }

    protected abstract String getConditionDescriptionInternally(Player player, Object... objects);

    public abstract void save(final FileConfiguration configuration, final String initialPath);

    public abstract void load(final FileConfiguration configuration, final String initialPath);

    public void setNegated(boolean negated) {
        this.negated = negated;
    }

    public final boolean isNegated(){
        return negated;
    }

    public abstract void deserializeFromSingleLineString(final ArrayList<String> arguments);

    public void switchCategory(final Category category) {

        final ConfigurationSection conditionsConfigurationSection = getCategory().getConditionsConfig().getConfigurationSection("conditions." + getConditionName());

        getCategory().getConditionsConfig().set("conditions." + getConditionName(), null);
        getCategory().saveConditionsConfig();

        setCategory(category);

        category.getConditionsConfig().set("conditions." + getConditionName(), conditionsConfigurationSection);
        category.saveConditionsConfig();

    }

    @Override
    public String toString() {
        return "Condition{" +
                "conditionName='" + conditionName + '\'' +
                ", progressNeeded=" + progressNeeded +
                ", negated=" + negated +
                ", category=" + category +
                ", description='" + description + '\'' +
                ", conditionID=" + conditionID +
                '}';
    }
}
