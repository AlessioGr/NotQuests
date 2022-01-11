package rocks.gravili.notquests.paper.structs.variables;

import cloud.commandframework.arguments.standard.StringArgument;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import rocks.gravili.notquests.paper.NotQuests;

import java.util.ArrayList;
import java.util.List;

public class PermissionVariable extends Variable<Boolean>{
    public PermissionVariable(NotQuests main) {
        super(main);
        if (main.getIntegrationsManager().isLuckpermsEnabled()) {
            setCanSetValue(true);
        }

        addRequiredString(
                StringArgument.<CommandSender>newBuilder("Permission").withSuggestionsProvider(
                        (context, lastString) -> {
                            final List<String> allArgs = context.getRawInput();
                            main.getUtilManager().sendFancyCommandCompletion(context.getSender(), allArgs.toArray(new String[0]), "[Permission Node]", "[...]");

                            ArrayList<String> suggestions = new ArrayList<>();
                            suggestions.add("<Enter Permission node>");
                            return suggestions;

                        }
                ).single().build()
        );
    }

    @Override
    public Boolean getValue(Player player, Object... objects) {
        return player != null && player.hasPermission(getRequiredStringValue("Permission"));
    }

    @Override
    public boolean setValue(Boolean newValue, Player player, Object... objects) {
        if(!main.getIntegrationsManager().isLuckpermsEnabled()){
            return false;
        }

        main.getIntegrationsManager().getLuckPermsManager().givePermission(player.getUniqueId(), getRequiredStringValue("Permission"));

        return true;
    }


    @Override
    public List<String> getPossibleValues(Player player, Object... objects) {
        return null;
    }

    @Override
    public String getPlural() {
        return "Permissions";
    }

    @Override
    public String getSingular() {
        return "Permission";
    }
}
