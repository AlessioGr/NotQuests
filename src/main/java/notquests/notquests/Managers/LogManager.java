package notquests.notquests.Managers;

import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import notquests.notquests.NotQuests;
import org.bukkit.Bukkit;

import java.util.logging.Level;

public class LogManager {
    private final NotQuests main;
    private final Audience consoleSender;
    private final Component prefix;
    private final String prefixText;


    public LogManager(final NotQuests main) {
        this.main = main;
        consoleSender = main.adventure().sender(Bukkit.getConsoleSender());

        prefixText = "<#393e46>[<gradient:#E0EAFC:#CFDEF3>NotQuests<#393e46>]<#636c73>: ";
        prefix = MiniMessage.miniMessage().parse(prefixText);


        boolean isPaper = false;
        try {
            // Any other works, just the shortest I could find.
            Class.forName("com.destroystokyo.paper.ParticleBuilder");
            isPaper = true;
        } catch (ClassNotFoundException ignored) {
        }
    }


    public void log(final Level level, final String message) {
        /*Component afterPrefixSuffix = Component.text("<WHITE>");
        if(level == Level.INFO) {
            afterPrefixSuffix = MiniMessage.miniMessage().parse("<GREEN>");
        }
        consoleSender.sendMessage(Component.text("Log level: " + level.getName()));
        consoleSender.sendMessage(prefix.append(afterPrefixSuffix).append(Component.text(message)));*/

        String afterPrefixSuffix = "<WHITE>";
        if (level == Level.INFO) {
            afterPrefixSuffix = "<gradient:#37a659:#56B4D3>";
        } else if (level == Level.WARNING) {
            afterPrefixSuffix = "<YELLOW>";
        } else if (level == Level.SEVERE) {
            afterPrefixSuffix = "<RED>";
        }

        consoleSender.sendMessage(MiniMessage.miniMessage().parse(prefixText + afterPrefixSuffix + message));

    }


    public void log(final Level level, final LogCategory logCategory, final String message) {
        String afterPrefixSuffix = "<WHITE>";
        if (level == Level.INFO) {
            if (logCategory == LogCategory.DEFAULT) {
                afterPrefixSuffix = "<gradient:#37a659:#56B4D3>";
            } else if (logCategory == LogCategory.DATA) {
                afterPrefixSuffix = "<gradient:#1FA2FF:#12D8FA:#A6FFCB>";
            } else if (logCategory == LogCategory.LANGUAGE) {
                afterPrefixSuffix = "<gradient:#AA076B:#61045F>";
            }

        } else if (level == Level.WARNING) {
            afterPrefixSuffix = "<YELLOW>";
        } else if (level == Level.SEVERE) {
            afterPrefixSuffix = "<RED>";
        }

        consoleSender.sendMessage(MiniMessage.miniMessage().parse(prefixText + afterPrefixSuffix + message));

    }


    public void info(final String message) {
        log(Level.INFO, message);
    }

    public void warn(final String message) {
        log(Level.WARNING, message);
    }

    public void severe(final String message) {
        log(Level.SEVERE, message);
    }

}

