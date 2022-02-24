package rocks.gravili.notquests.paper.structs.variables;

import cloud.commandframework.arguments.standard.StringArgument;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.command.CommandSender;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.structs.QuestPlayer;

import java.util.ArrayList;
import java.util.List;

public class PlaceholderAPIStringVariable extends Variable<String>{

    public PlaceholderAPIStringVariable(NotQuests main) {
        super(main);
        addRequiredString(
                StringArgument.<CommandSender>newBuilder("Placeholder").withSuggestionsProvider(
                        (context, lastString) -> {
                            final List<String> allArgs = context.getRawInput();
                            main.getUtilManager().sendFancyCommandCompletion(context.getSender(), allArgs.toArray(new String[0]), "[Placeholder Name]", "[...]");

                            final ArrayList<String> suggestions = new ArrayList<>();

                            for(String identifier : PlaceholderAPI.getRegisteredIdentifiers()){
                                suggestions.add("%" + identifier + "_");
                            }

                            return suggestions;

                        }
                ).single().build()
        );
    }

    @Override
    public String getValue(QuestPlayer questPlayer, Object... objects) {
        if (questPlayer != null) {
            return PlaceholderAPI.setPlaceholders(questPlayer.getPlayer(), getRequiredStringValue("Placeholder"));
        } else {
            return "";
        }
    }

    @Override
    public boolean setValueInternally(String newValue, QuestPlayer questPlayer, Object... objects) {
        return false;
    }

    @Override
    public List<String> getPossibleValues(QuestPlayer questPlayer, Object... objects) {
        return null;
    }

    @Override
    public String getPlural() {
        return getRequiredStringValue("Placeholder");
    }

    @Override
    public String getSingular() {
        return getRequiredStringValue("Placeholder");
    }
}