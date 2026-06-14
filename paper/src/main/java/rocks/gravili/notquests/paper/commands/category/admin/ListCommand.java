package rocks.gravili.notquests.paper.commands.category.admin;

import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;
import rocks.gravili.notquests.paper.commands.framework.NQCommandBuilder;
import rocks.gravili.notquests.paper.commands.framework.NQCommandManager;
import rocks.gravili.notquests.paper.commands.framework.NQDescription;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.commands.BaseCommand;
import rocks.gravili.notquests.paper.structs.Quest;

public class ListCommand extends BaseCommand {

    public ListCommand(NotQuests notQuests, NQCommandBuilder builder) {
        super(notQuests, builder);
    }

    @Override
    public void apply(NQCommandManager commandManager) {
        builder = builder.literal("list");

        commandManager.command(builder.commandDescription(NQDescription.of("Shows you a list of all available Objective Types."))
                .literal("ObjectiveTypes")
                .handler((context) -> {
                    context.sender().sendMessage(Component.empty());
                    context.sender().sendMessage(notQuests.parse("<highlight>All objective types:"));
                    for (final String objectiveType : notQuests.getObjectiveManager().getObjectiveIdentifiers()) {
                        context.sender().sendMessage(notQuests.parse("<main>" + objectiveType));
                    }
                }));

        commandManager.command(builder.commandDescription(NQDescription.of("Shows you a list of all available Requirement Types."))
                .literal("RequirementTypes")
                .handler((context) -> {
                    context.sender().sendMessage(Component.empty());
                    context.sender().sendMessage(notQuests.parse("<highlight>All requirement types:"));
                    for (final String requirementType : notQuests.getConditionsManager().getConditionIdentifiers()) {
                        context.sender().sendMessage(notQuests.parse("<main>" + requirementType));
                    }
                }));

        commandManager.command(builder.commandDescription(NQDescription.of("Shows you a list of all available Action (Reward) Types."))
                .literal("ActionTypes")
                .handler((context) -> {
                    context.sender().sendMessage(Component.empty());
                    context.sender().sendMessage(notQuests.parse("<highlight>" + "All reward types:"));
                    for (final String rewardType : notQuests.getActionManager().getActionIdentifiers()) {
                        context.sender().sendMessage(notQuests.parse("<main>" + rewardType));
                    }
                }));

        commandManager.command(builder.commandDescription(NQDescription.of("Shows you a list of all available Trigger Types."))
                .literal("TriggerTypes")
                .handler((context) -> {
                    context.sender().sendMessage(Component.empty());
                    context.sender().sendMessage(notQuests.parse("<highlight>" + "All trigger types:"));
                    for (final String triggerType : notQuests.getTriggerManager().getTriggerIdentifiers()) {
                        context.sender().sendMessage(notQuests.parse("<main>" + triggerType));
                    }
                }));

        commandManager.command(builder.commandDescription(NQDescription.of("Shows you a list of all created Quests."))
                .literal("AllQuests")
                .handler((context) -> {
                    context.sender().sendMessage(Component.empty());
                    int counter = 1;
                    context.sender().sendMessage(notQuests.parse("<highlight>" + "All Quests:"));
                    for (final Quest quest : notQuests.getQuestManager().getAllQuests()) {
                        context.sender().sendMessage(notQuests.parse("<highlight>" + counter + ".</highlight> " + "<main>" + quest.getIdentifier()));
                        counter += 1;
                    }

                }));

        commandManager.command(builder.commandDescription(NQDescription.of("Shows you a list of all available Placeholders which can be used in Trigger or Action commands."))
                .literal("Placeholders")
                .handler((context) -> {
                    context.sender().sendMessage(Component.empty());

                    context.sender().sendMessage(notQuests.parse("<highlight>All Placeholders (Case-sensitive):"));
                    context.sender().sendMessage(notQuests.parse("<highlight>1.</highlight> <highlight2>{PLAYER}</highlight2> <main>- Name of the player"));
                    context.sender().sendMessage(notQuests.parse("<highlight>2.</highlight> <highlight2>{PLAYERUUID}</highlight2> <main>- UUID of the player"));
                    context.sender().sendMessage(notQuests.parse("<highlight>3.</highlight> <highlight2>{PLAYERX}</highlight2> <main>- X coordinates of the player"));
                    context.sender().sendMessage(notQuests.parse("<highlight>4.</highlight> <highlight2>{PLAYERY}</highlight2> <main>- Y coordinates of the player"));
                    context.sender().sendMessage(notQuests.parse("<highlight>5.</highlight> <highlight2>{PLAYERZ}</highlight2> <main>- Z coordinates of the player"));
                    context.sender().sendMessage(notQuests.parse("<highlight>6.</highlight> <highlight2>{WORLD}</highlight2> <main>- World name of the player"));
                    context.sender().sendMessage(notQuests.parse("<highlight>6.</highlight> <highlight2>{QUEST}</highlight2> <main>- Quest name (if relevant)"));

                }));
    }
}
