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

package rocks.gravili.notquests.paper.placeholders;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.managers.expressions.NumberExpression;
import rocks.gravili.notquests.paper.managers.tags.Tag;
import rocks.gravili.notquests.paper.structs.*;
import rocks.gravili.notquests.paper.structs.variables.Variable;

/**
 * This class will be registered through the register-method in the
 * plugins onEnable-method.
 */
public class QuestPlaceholders extends PlaceholderExpansion {

    private final NotQuests main;

    /**
     * Since we register the expansion inside our own plugin, we
     * can simply use this method here to get an instance of our
     * plugin.
     *
     * @param main The instance of our plugin.
     */
    public QuestPlaceholders(final NotQuests main) {
        this.main = main;
    }

    /**
     * Because this is an internal class,
     * you must override this method to let PlaceholderAPI know to not unregister your expansion class when
     * PlaceholderAPI is reloaded
     *
     * @return true to persist through reloads
     */
    @Override
    public boolean persist() {
        return true;
    }

    /**
     * Because this is a internal class, this check is not needed
     * and we can simply return {@code true}
     *
     * @return Always true since it's an internal class.
     */
    @Override
    public boolean canRegister() {
        return true;
    }

    /**
     * The name of the person who created this expansion should go here.
     * <br>For convienience do we return the author from the plugin.yml
     *
     * @return The name of the author as a String.
     */
    @NotNull
    @Override
    public String getAuthor() {
        return main.getMain().getDescription().getAuthors().toString();
    }

    /**
     * The placeholder identifier should go here.
     * <br>This is what tells PlaceholderAPI to call our onRequest
     * method to obtain a value if a placeholder starts with our
     * identifier.
     * <br>The identifier has to be lowercase and can't contain _ or %
     *
     * @return The identifier in {@code %<identifier>_<value>%} as String.
     */
    @NotNull
    @Override
    public String getIdentifier() {
        return "notquests";
    }

    /**
     * This is the version of the expansion.
     * <br>You don't have to use numbers, since it is set as a String.
     * <p>
     * For convienience do we return the version from the plugin.yml
     *
     * @return The version as a String.
     */
    @NotNull
    @Override
    public String getVersion() {
        return main.getMain().getDescription().getVersion();
    }

    /**
     * This is the method called when a placeholder with our identifier
     * is found and needs a value.
     * <br>We specify the value identifier in this method.
     * <br>Since version 2.9.1 can you use OfflinePlayers in your requests.
     *
     * @param identifier A String containing the identifier/value.
     * @return possibly-null String of the requested identifier.
     */
    @Override
    public String onPlaceholderRequest(Player player, @NotNull String identifier) {

        if (player == null) {
            return "";
        }


        if (identifier.startsWith("player_questpoints")) {
            final QuestPlayer questPlayer = main.getQuestPlayerManager().getActiveQuestPlayer(player.getUniqueId());
            if (questPlayer != null) {
                return "" + questPlayer.getQuestPoints();
            }
            return "0";
        }

        if (identifier.startsWith("player_completed_quests_amount")) {
            final QuestPlayer questPlayer = main.getQuestPlayerManager().getActiveQuestPlayer(player.getUniqueId());
            if (questPlayer != null) {
                return "" + questPlayer.getCompletedQuests().size();
            }
            return "0";
        }

        if (identifier.startsWith("player_active_quests_amount")) {
            final QuestPlayer questPlayer = main.getQuestPlayerManager().getActiveQuestPlayer(player.getUniqueId());
            if (questPlayer != null) {
                return "" + questPlayer.getActiveQuests().size();
            }
            return "0";
        }

        if (identifier.startsWith("player_active_quests_list_horizontal")) {
            final QuestPlayer questPlayer = main.getQuestPlayerManager().getActiveQuestPlayer(player.getUniqueId());
            if (questPlayer != null) {
                StringBuilder list = new StringBuilder();
                int amount = 0;
                for (ActiveQuest activeQuest : questPlayer.getActiveQuests()) {
                    amount++;
                    //return if it's bigger than limit
                    if (main.getConfiguration().placeholder_player_active_quests_list_horizontal_limit >= 0 && amount > main.getConfiguration().placeholder_player_active_quests_list_horizontal_limit) {
                        return list.toString();
                    }

                    String nameToAdd = activeQuest.getQuest().getIdentifier();
                    if (main.getConfiguration().placeholder_player_active_quests_list_horizontal_use_displayname_if_available) {
                        if (!activeQuest.getQuest().getQuestDisplayName().isBlank()) {
                            nameToAdd = activeQuest.getQuest().getQuestDisplayName();
                        }
                    }

                    if(amount == 1){
                        list = new StringBuilder(nameToAdd);
                    }else{
                        list.append(main.getConfiguration().placeholder_player_active_quests_list_horizontal_separator).append(nameToAdd);
                    }

                }
                return list.toString();
            }
            return "-";

        }
        if (identifier.startsWith("player_active_quests_list_vertical")) {
            final QuestPlayer questPlayer = main.getQuestPlayerManager().getActiveQuestPlayer(player.getUniqueId());
            if (questPlayer != null) {
                StringBuilder list = new StringBuilder();
                int amount = 0;
                for(ActiveQuest activeQuest : questPlayer.getActiveQuests()){
                    amount++;
                    //return if it's bigger than limit
                    if (main.getConfiguration().placeholder_player_active_quests_list_vertical_limit >= 0 && amount > main.getConfiguration().placeholder_player_active_quests_list_vertical_limit) {
                        return list.toString();
                    }

                    String nameToAdd = activeQuest.getQuest().getIdentifier();
                    if (main.getConfiguration().placeholder_player_active_quests_list_vertical_use_displayname_if_available) {
                        if (!activeQuest.getQuest().getQuestDisplayName().isBlank()) {
                            nameToAdd = activeQuest.getQuest().getQuestDisplayName();
                        }
                    }

                    if(amount == 1){
                        list = new StringBuilder(nameToAdd);
                    }else{
                        list.append("\n").append(nameToAdd);
                    }

                }
                return list.toString();
            }
            return "-";

        }

        if (identifier.startsWith("player_has_completed_quest_")) {
            final String questName = identifier.replace("player_has_completed_quest_", "");
            final Quest quest = main.getQuestManager().getQuest(questName);
            if (quest != null) {
                final QuestPlayer questPlayer = main.getQuestPlayerManager().getActiveQuestPlayer(player.getUniqueId());
                if (questPlayer != null) {
                    for (final CompletedQuest completedQuest : questPlayer.getCompletedQuests()) {
                        if (completedQuest.getQuest().equals(quest)) {
                            return "Yes";
                        }
                    }
                }
            }
            return "No";

        }
        if (identifier.startsWith("player_has_current_active_quest_")) {
            final String questName = identifier.replace("player_has_current_active_quest_", "");
            final Quest quest = main.getQuestManager().getQuest(questName);
            if (quest != null) {
                final QuestPlayer questPlayer = main.getQuestPlayerManager().getActiveQuestPlayer(player.getUniqueId());
                if (questPlayer != null) {
                    for (final ActiveQuest activeQuest : questPlayer.getActiveQuests()) {
                        if (activeQuest.getQuest().equals(quest)) {
                            return "Yes";
                        }
                    }
                }
            }
            return "No";
        }

        if (identifier.startsWith("player_is_objective_unlocked_and_active") && identifier.contains("_from_active_quest_")) {
            String objectiveIDName = identifier.replace("player_is_objective_unlocked_and_active_", "");
            objectiveIDName = objectiveIDName.substring(0, objectiveIDName.indexOf("_from_active_quest_"));
            final int objectiveID = Integer.parseInt(objectiveIDName);

            final String questName = identifier.replace("player_is_objective_unlocked_and_active_", "").replace(objectiveIDName, "").replace("_from_active_quest_", "");


            final Quest quest = main.getQuestManager().getQuest(questName);
            if (quest != null) {
                final QuestPlayer questPlayer = main.getQuestPlayerManager().getActiveQuestPlayer(player.getUniqueId());
                if (questPlayer != null) {
                    for (final ActiveQuest activeQuest : questPlayer.getActiveQuests()) {
                        if (activeQuest.getQuest().equals(quest)) {
                            for (final ActiveObjective objective : activeQuest.getActiveObjectives()) {
                                if (objective.getObjectiveID() == objectiveID) {
                                    if (objective.isUnlocked()) {
                                        return "Yes";
                                    }
                                }
                            }

                            return "No";
                        }
                    }
                    return "No";
                }
                return "No";
            }
            return "No";

        } else if (identifier.startsWith("player_is_objective_unlocked_") && identifier.contains("_from_active_quest_")) {
            String objectiveIDName = identifier.replace("player_is_objective_unlocked_", "");
            objectiveIDName = objectiveIDName.substring(0, objectiveIDName.indexOf("_from_active_quest_"));
            final int objectiveID = Integer.parseInt(objectiveIDName);

            final String questName = identifier.replace("player_is_objective_unlocked_", "").replace(objectiveIDName, "").replace("_from_active_quest_", "");


            final Quest quest = main.getQuestManager().getQuest(questName);
            if (quest != null) {
                final QuestPlayer questPlayer = main.getQuestPlayerManager().getActiveQuestPlayer(player.getUniqueId());
                if (questPlayer != null) {
                    for (final ActiveQuest activeQuest : questPlayer.getActiveQuests()) {
                        if (activeQuest.getQuest().equals(quest)) {
                            for (final ActiveObjective objective : activeQuest.getActiveObjectives()) {
                                if (objective.getObjectiveID() == objectiveID) {
                                    if (objective.isUnlocked()) {
                                        return "Yes";
                                    }
                                }
                            }
                            for (final ActiveObjective objective : activeQuest.getCompletedObjectives()) {
                                if (objective.getObjectiveID() == objectiveID) {
                                    if (objective.isUnlocked()) {
                                        return "Yes";
                                    }
                                }
                            }
                            return "No";
                        }
                    }
                    return "No";
                }
                return "No";
            }
            return "No";

        } else if (identifier.startsWith("player_is_objective_completed_") && identifier.contains("_from_active_quest_")) {
            String objectiveIDName = identifier.replace("player_is_objective_completed_", "");
            objectiveIDName = objectiveIDName.substring(0, objectiveIDName.indexOf("_from_active_quest_"));
            final int objectiveID = Integer.parseInt(objectiveIDName);

            final String questName = identifier.replace("player_is_objective_completed_", "").replace(objectiveIDName, "").replace("_from_active_quest_", "");


            final Quest quest = main.getQuestManager().getQuest(questName);
            if (quest != null) {
                final QuestPlayer questPlayer = main.getQuestPlayerManager().getActiveQuestPlayer(player.getUniqueId());
                if (questPlayer != null) {
                    for (final ActiveQuest activeQuest : questPlayer.getActiveQuests()) {
                        if (activeQuest.getQuest().equals(quest)) {
                            for (final ActiveObjective objective : activeQuest.getCompletedObjectives()) {
                                if (objective.getObjectiveID() == objectiveID) {
                                    return "Yes";
                                }
                            }
                            return "No";
                        }


                    }
                    return "No";
                }
                return "No";
            }
            return "No";
        } else if (identifier.startsWith("player_expression_")) {
            final String expression = identifier.replace("player_expression_", "");

            final NumberExpression numberExpression = new NumberExpression(main, expression);
            return "" + numberExpression.calculateValue(main.getQuestPlayerManager().getOrCreateQuestPlayer(player.getUniqueId()));
        } else if (identifier.startsWith("player_rounded_expression_")) {
            final String expression = identifier.replace("player_rounded_expression_", "");

            final NumberExpression numberExpression = new NumberExpression(main, expression);
            return "" + (int) Math.round(numberExpression.calculateValue(main.getQuestPlayerManager().getOrCreateQuestPlayer(player.getUniqueId())));
        } else if (identifier.startsWith("player_variable_")) { //Variables
            final String variableName = identifier.replace("player_variable_", "");
            Variable<?> variable = main.getVariablesManager().getVariableFromString(variableName);
            if (variable != null) {
                Object value = variable.getValue(main.getQuestPlayerManager().getOrCreateQuestPlayer(player.getUniqueId()));
                return value != null ? "" + value : "";
            }
            return "";
        } else if (identifier.startsWith("player_tag_")) { //Tags
            final String tagName = identifier.replace("player_tag_", "");
            Tag tag = main.getTagManager().getTag(tagName);
            if (tag != null) {
                final QuestPlayer questPlayer = main.getQuestPlayerManager().getActiveQuestPlayer(player.getUniqueId());
                if (questPlayer != null) {
                    Object tagValue = questPlayer.getTagValue(tagName);
                    if (tagValue != null) {
                        return "" + tagValue;

                    }
                }
            }
            return "";

        } else if (identifier.startsWith("player_quest_cooldown_left_formatted_")) {
            final String questName = identifier.replace("player_quest_cooldown_left_formatted_", "");
            final Quest quest = main.getQuestManager().getQuest(questName);

            if (quest != null) {
                final QuestPlayer questPlayer = main.getQuestPlayerManager().getActiveQuestPlayer(player.getUniqueId());
                if (questPlayer != null) {
                    return questPlayer.getCooldownFormatted(quest);
                }
            }
            final String prefix = main.getLanguageManager().getString("placeholders.questcooldownleftformatted.prefix", player);
            return prefix + main.getLanguageManager().getString("placeholders.questcooldownleftformatted.no-cooldown", player);

        } else if (identifier.startsWith("player_objective_progress_percentage_") && identifier.contains("_from_active_quest_")) {
            String objectiveIDName = identifier.replace("player_objective_progress_percentage_", "");
            objectiveIDName = objectiveIDName.substring(0, objectiveIDName.indexOf("_from_active_quest_"));
            final int objectiveID = Integer.parseInt(objectiveIDName);

            final String questName = identifier.replace("player_objective_progress_percentage_", "").replace(objectiveIDName, "").replace("_from_active_quest_", "");


            final Quest quest = main.getQuestManager().getQuest(questName);
            if (quest != null) {
                final QuestPlayer questPlayer = main.getQuestPlayerManager().getActiveQuestPlayer(player.getUniqueId());
                if (questPlayer != null) {
                    for (final ActiveQuest activeQuest : questPlayer.getActiveQuests()) {
                        if (activeQuest.getQuest().equals(quest)) {
                            for (final ActiveObjective activeObjective : activeQuest.getActiveObjectives()) {
                                if (activeObjective.getObjectiveID() == objectiveID) {
                                    if (activeObjective.isUnlocked()) {
                                        return "" + ((int) (((float) activeObjective.getCurrentProgress() / (float) activeObjective.getProgressNeeded()) * 100));
                                    }
                                }
                            }
                            for (final ActiveObjective objective : activeQuest.getCompletedObjectives()) {
                                if (objective.getObjectiveID() == objectiveID) {
                                    if (objective.isUnlocked()) {
                                        return "100";
                                    }
                                }
                            }
                            return "0";
                        }
                    }
                    return "0";
                }
                return "0";
            }
            return "0";

        } else if (identifier.startsWith("player_objective_progress_") && identifier.contains("_from_active_quest_")) {
            String objectiveIDName = identifier.replace("player_objective_progress_", "");
            objectiveIDName = objectiveIDName.substring(0, objectiveIDName.indexOf("_from_active_quest_"));
            final int objectiveID = Integer.parseInt(objectiveIDName);

            final String questName = identifier.replace("player_objective_progress_", "").replace(objectiveIDName, "").replace("_from_active_quest_", "");


            final Quest quest = main.getQuestManager().getQuest(questName);
            if (quest != null) {
                final QuestPlayer questPlayer = main.getQuestPlayerManager().getActiveQuestPlayer(player.getUniqueId());
                if (questPlayer != null) {
                    for (final ActiveQuest activeQuest : questPlayer.getActiveQuests()) {
                        if (activeQuest.getQuest().equals(quest)) {
                            for (final ActiveObjective activeObjective : activeQuest.getActiveObjectives()) {
                                if (activeObjective.getObjectiveID() == objectiveID) {
                                    if (activeObjective.isUnlocked()) {
                                        return "" + activeObjective.getCurrentProgress();
                                    }
                                }
                            }
                            for (final ActiveObjective completedObjective : activeQuest.getCompletedObjectives()) {
                                if (completedObjective.getObjectiveID() == objectiveID) {
                                    if (completedObjective.isUnlocked()) {
                                        return "" + completedObjective.getProgressNeeded();
                                    }
                                }
                            }
                            return "0";
                        }
                    }
                    return "0";
                }
                return "0";
            }
            return "0";

        }


        // We return null if an invalid placeholder (f.e. %someplugin_placeholder3%)
        // was provided
        return null;
    }
}
//%notquests_player_has_completed_quest_bob_the_king%
