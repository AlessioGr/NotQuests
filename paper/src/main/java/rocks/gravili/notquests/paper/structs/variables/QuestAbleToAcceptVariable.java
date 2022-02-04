package rocks.gravili.notquests.paper.structs.variables;

import cloud.commandframework.arguments.standard.StringArgument;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.structs.CompletedQuest;
import rocks.gravili.notquests.paper.structs.Quest;
import rocks.gravili.notquests.paper.structs.QuestPlayer;
import rocks.gravili.notquests.paper.structs.conditions.Condition;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * This variable is true if the player is able to take the Quest. That means, they fulfill all Quest conditions, as well as other factors like the Quest cooldown or maxAccepts.
 */
public class QuestAbleToAcceptVariable extends Variable<Boolean>{
    public QuestAbleToAcceptVariable(NotQuests main) {
        super(main);
        addRequiredString(
                StringArgument.<CommandSender>newBuilder("Quest to check").withSuggestionsProvider(
                        (context, lastString) -> {
                            final List<String> allArgs = context.getRawInput();
                            main.getUtilManager().sendFancyCommandCompletion(context.getSender(), allArgs.toArray(new String[0]), "[Quest Name]", "[...]");

                            ArrayList<String> suggestions = new ArrayList<>();
                            for(Quest quest : main.getQuestManager().getAllQuests()){
                                suggestions.add(quest.getQuestName());
                            }
                            return suggestions;

                        }
                ).single().build()
        );
    }

    @Override
    public Boolean getValue(Player player, Object... objects) {
        final Quest quest = main.getQuestManager().getQuest(getRequiredStringValue("Quest to check"));
        final QuestPlayer questPlayer = main.getQuestPlayerManager().getQuestPlayer(player.getUniqueId());

        if(quest == null){
            return true;
        }

        if(questPlayer != null){
            int completedAmount = 0; //only needed for maxAccepts

            long mostRecentAcceptTime = 0;
            for (CompletedQuest completedQuest : questPlayer.getCompletedQuests()) {
                if (completedQuest.getQuest().equals(quest)) {
                    completedAmount += 1;
                    if (completedQuest.getTimeCompleted() > mostRecentAcceptTime) {
                        mostRecentAcceptTime = completedQuest.getTimeCompleted();
                    }
                }
            }
            final long acceptTimeDifference = System.currentTimeMillis() - mostRecentAcceptTime;
            final long acceptTimeDifferenceMinutes = TimeUnit.MILLISECONDS.toMinutes(acceptTimeDifference);

            if(acceptTimeDifferenceMinutes < quest.getAcceptCooldown() || quest.getMaxAccepts() == 0 || (quest.getMaxAccepts() > -1 && completedAmount >= quest.getMaxAccepts())){
                return false;
            }
        }else{
            if(quest.getMaxAccepts() == 0){
                return false;
            }
        }





        for (final Condition condition : quest.getRequirements()) {
            final String check = condition.check(questPlayer);
            if (!check.isBlank()) {
                return false;
            }
        }





        return true; //Able to accept the Quest
    }

    @Override
    public boolean setValueInternally(Boolean newValue, Player player, Object... objects) {
        return false;
    }


    @Override
    public List<String> getPossibleValues(Player player, Object... objects) {
        return null;
    }

    @Override
    public String getPlural() {
        return "Able to accept Quest";
    }

    @Override
    public String getSingular() {
        return "Able to accept Quest";
    }
}
