package rocks.gravili.notquests.paper.structs.variables;

import cloud.commandframework.arguments.standard.StringArgument;
import org.bukkit.command.CommandSender;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.structs.QuestPlayer;

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
    public Boolean getValue(QuestPlayer questPlayer, Object... objects) {
        return questPlayer != null && questPlayer.getPlayer().hasPermission(getRequiredStringValue("Permission"));
    }

    @Override
    public boolean setValueInternally(Boolean newValue, QuestPlayer questPlayer, Object... objects) {
        if (!main.getIntegrationsManager().isLuckpermsEnabled()) {
            return false;
        }

        if (newValue) {
            main.getIntegrationsManager().getLuckPermsManager().givePermission(questPlayer.getUniqueId(), getRequiredStringValue("Permission"));
        } else {
            main.getIntegrationsManager().getLuckPermsManager().denyPermission(questPlayer.getUniqueId(), getRequiredStringValue("Permission"));
        }


        return true;
    }


    @Override
    public List<String> getPossibleValues(QuestPlayer questPlayer, Object... objects) {
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
