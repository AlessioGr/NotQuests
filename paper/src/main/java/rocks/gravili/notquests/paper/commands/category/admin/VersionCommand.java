package rocks.gravili.notquests.paper.commands.category.admin;

import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import rocks.gravili.notquests.paper.commands.framework.NQCommandBuilder;
import rocks.gravili.notquests.paper.commands.framework.NQCommandManager;
import rocks.gravili.notquests.paper.commands.framework.NQDescription;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.commands.BaseCommand;

public class VersionCommand extends BaseCommand {
    public VersionCommand(NotQuests notQuests, NQCommandBuilder builder) {
        super(notQuests, builder);
    }

    @Override
    public void apply(NQCommandManager commandManager) {
        commandManager.command(builder.commandDescription(NQDescription.of("Displays the version of the NotQuests plugin you're using."))
                .literal("version", "ver", "v", "info")
                .handler((context) -> context.sender().sendMessage(notQuests.parse("<main>NotQuests version: <highlight>" + notQuests.getMain().getDescription().getVersion() +
                                        "\n<main>NotQuests module: <highlight>Paper" +
                                        "\n<main>Server version: <highlight>" + Bukkit.getVersion() +
                                        "\n<main>Server Brand: <highlight>" + Bukkit.getServer().getName() +
                                        "\n<main>Java version: <highlight>" + (System.getProperty("java.version") != null ? System.getProperty("java.version") : "null") +
                                        "\n<main>Enabled integrations: <highlight>" + notQuests.getIntegrationsManager().getEnabledIntegrationString()
                                )
                                .hoverEvent(HoverEvent.showText(notQuests.parse("<main>Click to copy this information to your clipboard.")))
                                .clickEvent(ClickEvent.copyToClipboard("**NotQuests version:** " + notQuests.getMain().getDescription().getVersion() +
                                        "\n**NotQuests module:** Paper" +
                                        "\n**Server version:** " + Bukkit.getVersion() +
                                        "\n**Server Brand:** " + Bukkit.getServer().getName() +
                                        "\n**Java version:** " + (System.getProperty("java.version") != null ? System.getProperty("java.version") : "null") +
                                        "\n**Enabled integrations:**" + notQuests.getIntegrationsManager().getEnabledIntegrationDiscordString()
                                ))
                )));
    }
}
