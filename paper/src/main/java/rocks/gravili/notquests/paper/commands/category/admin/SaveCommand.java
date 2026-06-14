package rocks.gravili.notquests.paper.commands.category.admin;

import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;
import rocks.gravili.notquests.paper.commands.framework.NQCommandBuilder;
import rocks.gravili.notquests.paper.commands.framework.NQCommandManager;
import rocks.gravili.notquests.paper.commands.framework.NQDescription;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.commands.BaseCommand;

public class SaveCommand extends BaseCommand {
    public SaveCommand(NotQuests notQuests, NQCommandBuilder builder) {
        super(notQuests, builder);
    }

    @Override
    public void apply(NQCommandManager commandManager) {
        commandManager.command(builder.commandDescription(NQDescription.of("Saves the NotQuests configuration file."))
                .literal("save")
                .handler((context) -> {
                    notQuests.getDataManager().saveData();
                    context.sender().sendMessage(Component.empty());
                    context.sender().sendMessage(notQuests.parse("<success>NotQuests configuration and player data has been saved"));
                }));
    }
}
