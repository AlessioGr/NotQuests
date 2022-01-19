package rocks.gravili.notquests.paper.structs.variables;

import cloud.commandframework.arguments.standard.StringArgument;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import rocks.gravili.notquests.paper.NotQuests;

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
    public String getValue(Player player, Object... objects) {
        if (player != null) {
            return PlaceholderAPI.setPlaceholders(player, getRequiredStringValue("Placeholder"));
        } else {
            return "";
        }
    }

    @Override
    public boolean setValueInternally(String newValue, Player player, Object... objects) {
        return false;
    }

    @Override
    public List<String> getPossibleValues(Player player, Object... objects) {
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