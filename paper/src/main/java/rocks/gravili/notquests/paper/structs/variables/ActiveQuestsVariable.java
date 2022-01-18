package rocks.gravili.notquests.paper.structs.variables;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.structs.ActiveQuest;
import rocks.gravili.notquests.paper.structs.Quest;
import rocks.gravili.notquests.paper.structs.QuestPlayer;

import java.util.Arrays;
import java.util.List;

public class ActiveQuestsVariable extends Variable<String[]>{
    public ActiveQuestsVariable(NotQuests main) {
        super(main);
        setCanSetValue(true);
    }

    @Override
    public String[] getValue(Player player, Object... objects) {
        String[] activeQuests;
        final QuestPlayer questPlayer = main.getQuestPlayerManager().getQuestPlayer(player.getUniqueId());
        if(questPlayer == null){
            return null;
        }

        activeQuests = questPlayer.getActiveQuests().stream().map(ActiveQuest::getQuestName).
                toArray(String[]::new);

        return activeQuests;
    }

    @Override
    public boolean setValue(String[] newValue, Player player, Object... objects) {
        final QuestPlayer questPlayer = main.getQuestPlayerManager().getOrCreateQuestPlayer(player.getUniqueId());
        if(questPlayer == null){
            return false;
        }

        for(ActiveQuest acceptedQuest : questPlayer.getActiveQuests()){
            boolean foundQuest = false;
            for (int i = 0; i < newValue.length; i++)
            {
                if (newValue[i].equalsIgnoreCase(acceptedQuest.getQuestName())) {
                    foundQuest = true;
                    break;
                }
            }
            if(!foundQuest){
                questPlayer.failQuest(acceptedQuest);
            }
        }

        for (int i = 0; i < newValue.length; i++)
        {
            Quest quest = main.getQuestManager().getQuest(newValue[i]);
            if(quest != null && !questPlayer.hasAcceptedQuest(quest)){
                main.getQuestPlayerManager().forceAcceptQuest(questPlayer.getUUID(), quest);
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
        return "Active Quests";
    }

    @Override
    public String getSingular() {
        return "Active Quest";
    }
}
