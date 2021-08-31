package notquests.notquests.Structs.Triggers;


import notquests.notquests.NotQuests;
import notquests.notquests.Structs.ActiveQuest;
import org.bukkit.Bukkit;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

public class Action {

    private final NotQuests main;
    private final String actionName;
    private String consoleCommand;


    public Action(NotQuests main, String name, String consoleCommand) {
        this.main = main;
        this.actionName = name;
        this.consoleCommand = consoleCommand;
    }

    public void execute(final Player player, final ActiveQuest activeQuest) {
        String executeConsoleCommand = consoleCommand.replace("{PLAYER}", player.getName()).replace("{PLAYERUUID}", player.getUniqueId().toString());
        executeConsoleCommand = executeConsoleCommand.replace("{PLAYERX}", "" + player.getLocation().getX());
        executeConsoleCommand = executeConsoleCommand.replace("{PLAYERY}", "" + player.getLocation().getY());
        executeConsoleCommand = executeConsoleCommand.replace("{PLAYERZ}", "" + player.getLocation().getZ());
        executeConsoleCommand = executeConsoleCommand.replace("{WORLD}", "" + player.getWorld().getName());
        executeConsoleCommand = executeConsoleCommand.replace("{QUEST}", "" + activeQuest.getQuest().getQuestName());
        ConsoleCommandSender console = Bukkit.getServer().getConsoleSender();
        if (Bukkit.isPrimaryThread()) {
            Bukkit.dispatchCommand(console, executeConsoleCommand);
        } else {
            final String finalExecuteConsoleCommand = executeConsoleCommand;
            Bukkit.getScheduler().runTask(main, () -> {
                Bukkit.dispatchCommand(console, finalExecuteConsoleCommand);
            });
        }
    }


    public final String getActionName() {
        return actionName;
    }

    public final String getConsoleCommand() {
        return consoleCommand;
    }

    public void setConsoleCommand(String newConsoleCommand) {
        this.consoleCommand = newConsoleCommand;
        main.getDataManager().getQuestsData().set("actions." + actionName + ".consoleCommand", newConsoleCommand);
    }
}
