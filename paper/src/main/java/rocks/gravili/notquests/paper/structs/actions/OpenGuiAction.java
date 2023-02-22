package rocks.gravili.notquests.paper.structs.actions;

import cloud.commandframework.ArgumentDescription;
import cloud.commandframework.Command;
import cloud.commandframework.arguments.standard.StringArgument;
import cloud.commandframework.paper.PaperCommandManager;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.commands.arguments.CommandSelector;
import rocks.gravili.notquests.paper.structs.QuestPlayer;

import java.util.ArrayList;

public class OpenGuiAction extends Action {
    private String guiName = "";
    public OpenGuiAction(NotQuests main) {
        super(main);
    }

    public static void handleCommands(
            NotQuests main,
            PaperCommandManager<CommandSender> manager,
            Command.Builder<CommandSender> builder,
            ActionFor actionFor) {
        manager.command(
                builder.argument(
                        StringArgument.<CommandSender>builder("guiName").withSuggestionsProvider((objectCommandContext, s) -> {

                            var completions = main.getGuiService().getGuis().keySet();

                            return completions.stream().toList();
                        }).build(),
                        ArgumentDescription.of("Opens a gui for the player")
                ).handler(commandContext -> {
                    final var guiName = (String) commandContext.get("guiName");

                    var openGuiAction = new OpenGuiAction(main);
                    openGuiAction.setGuiName(guiName);

                    main.getActionManager().addAction(openGuiAction, commandContext, actionFor);
                }));

    }


    @Override
    public String getActionDescription(QuestPlayer questPlayer, Object... objects) {
        return "Opens a gui with given name for the player";
    }

    @Override
    protected void executeInternally(QuestPlayer questPlayer, Object... objects) {
        final var player = questPlayer.getPlayer();
        if(player == null) {
            main.getLogManager()
                    .warn("Tried to execute PlayerCommand action with invalid player.");
            return;
        }

        if (Bukkit.isPrimaryThread()) {
            main.getGuiService().showGui(guiName, player);
        } else {
            Bukkit.getScheduler()
                    .runTask(main.getMain(), () -> main.getGuiService().showGui(guiName, player));
        }
    }

    @Override
    public void save(FileConfiguration configuration, String initialPath) {
        configuration.set(initialPath + ".specifics.guiName", getGuiName());
    }

    @Override
    public void load(FileConfiguration configuration, String initialPath) {
        this.guiName = configuration.getString(initialPath + ".specifics.guiName");
    }

    @Override
    public void deserializeFromSingleLineString(ArrayList<String> arguments) {
        this.guiName = arguments.get(0);
    }

    public final String getGuiName() {
        return guiName;
    }

    public void setGuiName(String guiName) {
        this.guiName = guiName;
    }
}
