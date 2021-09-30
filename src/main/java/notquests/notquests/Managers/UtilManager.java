package notquests.notquests.Managers;

import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import notquests.notquests.NotQuests;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

public class UtilManager {
    private final NotQuests main;

    public UtilManager(NotQuests main) {
        this.main = main;
    }

    public final OfflinePlayer getOfflinePlayer(final String playerName) {
        return Bukkit.getOfflinePlayer(playerName);
    }


    public Component getFancyActionBarTabCompletion(final String[] args, final String hintCurrentArg, final String hintNextArgs) {
        final StringBuilder argsTogether = new StringBuilder();

        for (int i = 0; i < args.length - 1; i++) {
            argsTogether.append(args[i]).append(" ");
        }

        Component currentCompletion;
        if (args[args.length - 1].isBlank()) {
            currentCompletion = Component.text(" " + hintCurrentArg, NamedTextColor.GREEN);
        } else {
            currentCompletion = Component.text(" " + args[args.length - 1], NamedTextColor.YELLOW);
        }

        if (!hintNextArgs.isBlank()) {
            return Component.text("/qa " + argsTogether, NamedTextColor.GRAY)
                    .append(currentCompletion)
                    .append(Component.text(" " + hintNextArgs, NamedTextColor.DARK_GRAY));
        } else {
            return Component.text("/qa " + argsTogether, NamedTextColor.GRAY)
                    .append(currentCompletion);
        }

    }


    public void sendFancyActionBar(final Audience audience, final String[] args, final String hintCurrentArg, final String hintNextArgs) {
        if (main.getDataManager().getConfiguration().isActionBarCommandCompletionEnabled()) {
            audience.sendActionBar(main.getUtilManager().getFancyActionBarTabCompletion(args, hintCurrentArg, hintNextArgs));
        }

    }

}
