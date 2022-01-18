package rocks.gravili.notquests.paper.structs.variables;

import org.bukkit.entity.Player;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.structs.ActiveQuest;
import rocks.gravili.notquests.paper.structs.CompletedQuest;
import rocks.gravili.notquests.paper.structs.Quest;
import rocks.gravili.notquests.paper.structs.QuestPlayer;

import java.util.Arrays;
import java.util.List;

public class CompletedQuestsVariable extends Variable<String[]>{
    public CompletedQuestsVariable(NotQuests main) {
        super(main);
        setCanSetValue(true);
    }

    @Override
    public String[] getValue(Player player, Object... objects) {
        String[] completedQuests;
        final QuestPlayer questPlayer = main.getQuestPlayerManager().getQuestPlayer(player.getUniqueId());
        if(questPlayer == null){
            return null;
        }

        completedQuests = questPlayer.getCompletedQuests().stream().map(CompletedQuest::getQuestName).
                toArray(String[]::new);

        return completedQuests;
    }

    @Override
    public boolean setValue(String[] newValue, Player player, Object... objects) {
        final QuestPlayer questPlayer = main.getQuestPlayerManager().getOrCreateQuestPlayer(player.getUniqueId());
        if(questPlayer == null){
            return false;
        }

        for(CompletedQuest completedQuest : questPlayer.getCompletedQuests()){
            boolean foundQuest = false;
            for (int i = 0; i < newValue.length; i++)
            {
                if (newValue[i].equalsIgnoreCase(completedQuest.getQuestName())) {
                    foundQuest = true;
                    break;
                }
            }
            if(!foundQuest){
                questPlayer.getCompletedQuests().remove(completedQuest);
            }
        }

        for (int i = 0; i < newValue.length; i++)
        {
            Quest quest = main.getQuestManager().getQuest(newValue[i]);
            if(quest != null && !questPlayer.hasCompletedQuest(quest)){
                questPlayer.getCompletedQuests().add(new CompletedQuest(quest, questPlayer));
            }
        }

        return true;
    }


    @Override
    public List<String> getPossibleValues(Player player, Object... objects) {
        return main.getQuestManager().getAllQuests().stream().map(quest -> quest.getQuestName()).toList();
    }

    @Override
    public String getPlural() {
        return "Completed Quests";
    }

    @Override
    public String getSingular() {
        return "Completed Quest";
    }
}
