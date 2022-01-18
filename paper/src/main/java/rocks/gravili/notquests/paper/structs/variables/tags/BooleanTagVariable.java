package rocks.gravili.notquests.paper.structs.variables.tags;

import cloud.commandframework.arguments.standard.StringArgument;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.commands.arguments.variables.NumberVariableValueArgument;
import rocks.gravili.notquests.paper.managers.tags.Tag;
import rocks.gravili.notquests.paper.managers.tags.TagType;
import rocks.gravili.notquests.paper.structs.QuestPlayer;
import rocks.gravili.notquests.paper.structs.variables.Variable;

import java.util.ArrayList;
import java.util.List;

public class BooleanTagVariable extends Variable<Boolean> {

    public BooleanTagVariable(NotQuests main) {
        super(main);

        addRequiredString(
                StringArgument.<CommandSender>newBuilder("Tag Name").withSuggestionsProvider(
                        (context, lastString) -> {
                            final List<String> allArgs = context.getRawInput();
                            main.getUtilManager().sendFancyCommandCompletion(context.getSender(), allArgs.toArray(new String[0]), "[Tag Name]", "[...]");

                            ArrayList<String> suggestions = new ArrayList<>();
                            for(Tag tag : main.getTagManager().getTags()){
                                if(tag.getTagType() == TagType.BOOLEAN){
                                    suggestions.add("" + tag.getTagName());
                                }
                            }
                            return suggestions;

                        }
                ).single().build()
        );

        setCanSetValue(true);
    }

    @Override
    public Boolean getValue(Player player, Object... objects) {
        final QuestPlayer questPlayer = main.getQuestPlayerManager().getQuestPlayer(player.getUniqueId());
        if(questPlayer == null){
            return false;
        }

        final String tagName = getRequiredStringValue("Tag Name");
        final Tag tag = main.getTagManager().getTag(tagName);
        if(tag == null){
            main.getLogManager().warn("Error reading tag " + tagName + ". Tag does not exist.");
            return false;
        }
        if(tag.getTagType() != TagType.BOOLEAN){
            main.getLogManager().warn("Error reading tag " + tagName + ". Tag is no boolean tag.");
            return false;
        }

        Object value = questPlayer.getTag(tagName);

        if(value instanceof Boolean boolValue){
            return boolValue;
        }else{
            return false;
        }

    }

    @Override
    public boolean setValue(Boolean newValue, Player player, Object... objects) {
        final QuestPlayer questPlayer = main.getQuestPlayerManager().getQuestPlayer(player.getUniqueId());
        if(questPlayer == null){
            return false;
        }

        final String tagName = getRequiredStringValue("Tag Name");
        final Tag tag = main.getTagManager().getTag(tagName);
        if(tag == null){
            main.getLogManager().warn("Error reading tag " + tagName + ". Tag does not exist.");
            return false;
        }
        if(tag.getTagType() != TagType.BOOLEAN){
            main.getLogManager().warn("Error reading tag " + tagName + ". Tag is no boolean tag.");
            return false;
        }



        questPlayer.setTag(tagName, newValue);

        return true;
    }


    @Override
    public List<String> getPossibleValues(Player player, Object... objects) {
        return null;
    }

    @Override
    public String getPlural() {
        return "Tags";
    }

    @Override
    public String getSingular() {
        return "Tag";
    }
}
