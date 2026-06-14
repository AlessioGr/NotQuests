package rocks.gravili.notquests.paper.commands.category.admin.structs;

import org.bukkit.entity.Player;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.commands.BaseCommand;
import rocks.gravili.notquests.paper.commands.framework.NQArguments;
import rocks.gravili.notquests.paper.commands.framework.NQCommandBuilder;
import rocks.gravili.notquests.paper.commands.framework.NQCommandManager;
import rocks.gravili.notquests.paper.commands.framework.NQDescription;
import rocks.gravili.notquests.paper.managers.npc.NQNPC;
import rocks.gravili.notquests.paper.structs.ActiveObjective;
import rocks.gravili.notquests.paper.structs.ActiveQuest;
import rocks.gravili.notquests.paper.structs.Quest;
import rocks.gravili.notquests.paper.structs.QuestPlayer;
import rocks.gravili.notquests.paper.structs.objectives.Objective;
import rocks.gravili.notquests.paper.structs.objectives.TriggerCommandObjective;

import java.util.ArrayList;
import java.util.List;

public class ObjectiveTriggerCommand extends BaseCommand {

    public ObjectiveTriggerCommand(NotQuests notQuests, NQCommandBuilder builder) {
        super(notQuests, builder);
    }

    @Override
    public void apply(NQCommandManager commandManager) {
        commandManager.command(builder.commandDescription(NQDescription.of("This triggers the Trigger Command which is needed to complete a TriggerObjective (don't mistake it with Triggers & actions)."))
                .literal("triggerObjective")
                .required("trigger-name", NQArguments.stringArgument(), NQDescription.of("Name of the trigger which should be triggered."), (context, input) -> {
                            final List<String> completions = new ArrayList<>();
                            for (final Quest quest : notQuests.getQuestManager().getAllQuests()) {
                                for (final Objective objective : quest.getObjectives()) {
                                    if (objective instanceof final TriggerCommandObjective triggerCommandObjective) {
                                        completions.add(triggerCommandObjective.getTriggerName());
                                    }
                                }
                            }
                            return completions;
                        }
                )
                .required("player", NQArguments.playerArgument(), NQDescription.of("Player whose trigger should e triggered."))
                .handler((context) -> {
                    final String triggerName = context.get("trigger-name");
                    final Player player = context.get("player");
                    final QuestPlayer questPlayer = notQuests.getQuestPlayerManager().getActiveQuestPlayer(player.getUniqueId());
                    if (questPlayer != null) {
                        if (!questPlayer.getActiveQuests().isEmpty()) {
                            for (ActiveQuest activeQuest : questPlayer.getActiveQuests()) {
                                for (ActiveObjective activeObjective : activeQuest.getActiveObjectives()) {
                                    if (activeObjective.isUnlocked()) {
                                        if (activeObjective.getObjective() instanceof final TriggerCommandObjective triggerCommandObjective) {
                                            if (triggerCommandObjective.getTriggerName().equalsIgnoreCase(triggerName)) {
                                                activeObjective.addProgress(1, (NQNPC) null);

                                            }
                                        }
                                    }
                                }
                                activeQuest.removeCompletedObjectives(true);
                            }
                            questPlayer.removeCompletedQuests();
                        }
                    }
                }));
    }
}
