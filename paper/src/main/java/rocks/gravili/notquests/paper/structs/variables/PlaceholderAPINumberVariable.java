package rocks.gravili.notquests.paper.structs.variables;

import cloud.commandframework.arguments.standard.StringArgument;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import rocks.gravili.notquests.paper.NotQuests;

import java.util.ArrayList;
import java.util.List;

public class PlaceholderAPINumberVariable extends Variable<Double>{
    public PlaceholderAPINumberVariable(NotQuests main) {
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
    public Double getValue(Player player, Object... objects) {
        if (player != null) {
            if(getRequiredStringValue("Placeholder") == null){
                return 0D;
            }

            final String placeholder = PlaceholderAPI.setPlaceholders(player, getRequiredStringValue("Placeholder"));

            double parsedPlaceholder;
            try{
                if(placeholder.contains(".")){
                    parsedPlaceholder = Double.parseDouble(placeholder);
                }else{
                    parsedPlaceholder = (double) Integer.parseInt(placeholder);

                }
            }catch (NumberFormatException e){
                main.getLogManager().warn("Error: Placeholder Variable can not be parsed. Placeholder: <highlight>" + placeholder);
                return 0d;
            }

            return parsedPlaceholder;
        } else {
            return 0D;
        }
    }

    @Override
    public boolean setValueInternally(Double newValue, Player player, Object... objects) {
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
