package notquests.notquests.Placeholders;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import notquests.notquests.NotQuests;
import notquests.notquests.Structs.*;
import org.bukkit.entity.Player;

/**
 * This class will be registered through the register-method in the
 * plugins onEnable-method.
 */
public class QuestPlaceholders extends PlaceholderExpansion {

    private NotQuests main;

    /**
     * Since we register the expansion inside our own plugin, we
     * can simply use this method here to get an instance of our
     * plugin.
     *
     * @param main The instance of our plugin.
     */
    public QuestPlaceholders(NotQuests main) {
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
    @Override
    public String getAuthor() {
        return main.getDescription().getAuthors().toString();
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
    @Override
    public String getVersion() {
        return main.getDescription().getVersion();
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
    public String onPlaceholderRequest(Player player, String identifier) {

        if (player == null) {
            return "";
        }

        if (identifier.startsWith("player_questpoints")) {
            final QuestPlayer questPlayer = main.getQuestPlayerManager().getQuestPlayer(player.getUniqueId());
            if (questPlayer != null) {
                return "" + questPlayer.getQuestPoints();
            }
            return "0";

        }

        if (identifier.startsWith("player_has_completed_quest_")) {
            final String questName = identifier.replace("player_has_completed_quest_", "");
            final Quest quest = main.getQuestManager().getQuest(questName);
            if (quest != null) {
                final QuestPlayer questPlayer = main.getQuestPlayerManager().getQuestPlayer(player.getUniqueId());
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
                final QuestPlayer questPlayer = main.getQuestPlayerManager().getQuestPlayer(player.getUniqueId());
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
                final QuestPlayer questPlayer = main.getQuestPlayerManager().getQuestPlayer(player.getUniqueId());
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
                final QuestPlayer questPlayer = main.getQuestPlayerManager().getQuestPlayer(player.getUniqueId());
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
                final QuestPlayer questPlayer = main.getQuestPlayerManager().getQuestPlayer(player.getUniqueId());
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
        }


        // We return null if an invalid placeholder (f.e. %someplugin_placeholder3%)
        // was provided
        return null;
    }
}
//%notquests_player_has_completed_quest_bob_the_king%
