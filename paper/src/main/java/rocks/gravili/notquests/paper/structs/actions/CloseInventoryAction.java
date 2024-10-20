package rocks.gravili.notquests.paper.structs.actions;

import cloud.commandframework.Command;
import cloud.commandframework.paper.PaperCommandManager;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.structs.QuestPlayer;

import java.util.ArrayList;

public class CloseInventoryAction extends Action {
    public CloseInventoryAction(NotQuests main) {
        super(main);
    }


    public static void handleCommands(NotQuests main, PaperCommandManager<CommandSender> manager, Command.Builder<CommandSender> builder, ActionFor actionFor) {
        manager.command(builder.handler(commandContext -> {
            main.getActionManager().addAction(new CloseInventoryAction(main), commandContext, actionFor);
        }));
    }

    @Override
    public String getActionDescription(QuestPlayer questPlayer, Object... objects) {
        return "Closes the inventory of a given player";
    }

    @Override
    protected void executeInternally(QuestPlayer questPlayer, Object... objects) {
        questPlayer.getPlayer().closeInventory();
    }

    @Override
    public void save(FileConfiguration configuration, String initialPath) {

    }

    @Override
    public void load(FileConfiguration configuration, String initialPath) {

    }

    @Override
    public void deserializeFromSingleLineString(ArrayList<String> arguments) {

    }
}
