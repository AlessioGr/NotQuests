package notquests.notquests.Managers;

import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
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
        final int maxPreviousArgs = main.getDataManager().getConfiguration().getActionBarCommandCompletionMaxPreviousArgumentsDisplayed();

        final StringBuilder argsTogether = new StringBuilder();


        final int initialCutoff = (args.length) - maxPreviousArgs;
        int cutoff = (args.length) - maxPreviousArgs;


        for (int i = -1; i < args.length - 1; i++) {
            if (cutoff == 0) {
                if (i == -1) {
                    argsTogether.append("/qa ");
                } else {
                    argsTogether.append(args[i]).append(" ");
                }

            } else {
                cutoff -= 1;
            }

        }

        if (initialCutoff > 0) {
            argsTogether.insert(0, "[...] ");
        }


        Component currentCompletion;
        if (args[args.length - 1].isBlank()) {
            currentCompletion = Component.text("" + hintCurrentArg, NamedTextColor.GREEN);
        } else {
            currentCompletion = Component.text("" + args[args.length - 1], NamedTextColor.YELLOW);
        }

        if (!hintNextArgs.isBlank()) {
            return Component.text(argsTogether.toString(), TextColor.fromHexString("#616872"))
                    .append(currentCompletion)
                    .append(Component.text(" " + hintNextArgs, NamedTextColor.GRAY));
        } else {
            return Component.text(argsTogether.toString(), TextColor.fromHexString("#616872"))
                    .append(currentCompletion);
        }

    }


    public void sendFancyActionBar(final Audience audience, final String[] args, final String hintCurrentArg, final String hintNextArgs) {
        if (main.getDataManager().getConfiguration().isActionBarCommandCompletionEnabled()) {
            audience.sendActionBar(main.getUtilManager().getFancyActionBarTabCompletion(args, hintCurrentArg, hintNextArgs));
        }

    }

}
