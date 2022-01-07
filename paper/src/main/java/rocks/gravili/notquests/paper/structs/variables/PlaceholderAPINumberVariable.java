package rocks.gravili.notquests.paper.structs.variables;

import cloud.commandframework.arguments.standard.StringArgument;
import me.clip.placeholderapi.PlaceholderAPI;
import me.clip.placeholderapi.PlaceholderAPIPlugin;
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

                            return new ArrayList<>(PlaceholderAPI.getRegisteredIdentifiers());

                        }
                ).single().build()
        );
    }

    @Override
    public Double getValue(Player player, Object... objects) {
        if (player != null) {
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
    public List<String> getPossibleValues(Player player, Object... objects) {
        return null;
    }

    @Override
    public String getPlural() {
        return "Money";
    }

    @Override
    public String getSingular() {
        return "Money";
    }
}
