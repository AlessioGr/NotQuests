package rocks.gravili.notquests.Managers;

import rocks.gravili.notquests.NotQuests;

public class UpdateManager {
    final UpdateChecker updateChecker;
    private final NotQuests main;

    public UpdateManager(final NotQuests main) {
        this.main = main;
        this.updateChecker = new UpdateChecker(main, 95872);
    }

    public void checkForPluginUpdates() {
        try {
            if (updateChecker.checkForUpdates()) {
                main.getLogManager().info("<GOLD>The version <Yellow>" + main.getDescription().getVersion()
                        + " <GOLD>is not the latest version (<Green>" + updateChecker.getLatestVersion() + "<GOLD>)! Please update the plugin here: <Aqua>https://www.spigotmc.org/resources/95872/ <DARK_GRAY>(If your version is newer, the spigot API might not be updated yet).");
            } else {
                main.getLogManager().info("NotQuests seems to be up to date! :)");
            }
        } catch (Exception e) {
            e.printStackTrace();
            main.getLogManager().info("Unable to check for updates ('" + e.getMessage() + "').");
        }

    }
}
