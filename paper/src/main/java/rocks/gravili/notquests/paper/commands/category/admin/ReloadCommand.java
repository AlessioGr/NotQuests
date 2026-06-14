package rocks.gravili.notquests.paper.commands.category.admin;

import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;
import rocks.gravili.notquests.paper.commands.framework.NQCommandBuilder;
import rocks.gravili.notquests.paper.commands.framework.NQCommandManager;
import rocks.gravili.notquests.paper.commands.framework.NQDescription;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.commands.BaseCommand;

public class ReloadCommand extends BaseCommand {
    public ReloadCommand(NotQuests notQuests, NQCommandBuilder builder) {
        super(notQuests, builder);
    }

    @Override
    public void apply(NQCommandManager commandManager) {
        commandManager.command(builder.commandDescription(NQDescription.of("Loads from the NotQuests configuration file."))
                .literal("reload", "load")
                .handler((context) -> {

                    notQuests.getDataManager().loadGeneralConfig();
                    notQuests.getLanguageManager().loadLanguageConfig(false);
                    if (notQuests.getConversationManager() != null) {
                        notQuests.getConversationManager().loadConversationsFromConfig();
                    } else {
                        context.sender().sendMessage("<error> Loading conversations has been skipped: ConversationManager is null");
                    }
                    context.sender().sendMessage(Component.empty());
                    context.sender().sendMessage(notQuests.parse("<success>NotQuests general.yml, language configuration and conversations have been re-loaded. <unimportant>If you want to reload more, please use the ServerUtils plugin (available on spigot) or restart the server. This reload command does not reload the quests file or the database."));
                }));

        commandManager.command(builder.commandDescription(NQDescription.of("Reload the general.yml."))
                .literal("reload", "load")
                .literal("general.yml")
                .handler((context) -> {
                    notQuests.getDataManager().loadGeneralConfig();
                    context.sender().sendMessage(Component.empty());
                    context.sender().sendMessage(notQuests.parse("<success>General.yml has been reloaded."));
                }));

        commandManager.command(builder.commandDescription(NQDescription.of("Reload the languages from conversations files."))
                .literal("reload", "load")
                .literal("languages")
                .handler((context) -> {
                    notQuests.getLanguageManager().loadLanguageConfig(false);
                    context.sender().sendMessage(Component.empty());
                    context.sender().sendMessage(notQuests.parse("<success>Languages have been reloaded."));
                }));

        commandManager.command(builder.commandDescription(NQDescription.of("Reload the conversations from conversations files."))
                .literal("reload", "load")
                .literal("conversations")
                .handler((context) -> {
                    if (notQuests.getConversationManager() != null) {
                        notQuests.getConversationManager().loadConversationsFromConfig();
                        context.sender().sendMessage(notQuests.parse("<success>Conversations have been reloaded."));
                    } else {
                        context.sender().sendMessage("<error> Loading conversations has been skipped: ConversationManager is null");
                    }
                    context.sender().sendMessage(Component.empty());
                }));
    }
}
