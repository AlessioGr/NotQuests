// 
// Decompiled by Procyon v0.5.36
// 

package rocks.gravili.notquests.paper.structs.objectives;

import cloud.commandframework.ArgumentDescription;
import cloud.commandframework.Command;
import cloud.commandframework.arguments.standard.StringArgument;
import cloud.commandframework.paper.PaperCommandManager;
import com.neostorm.neostorm.Api;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.checkerframework.checker.nullness.qual.Nullable;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.commands.arguments.EntityTypeSelector;
import rocks.gravili.notquests.paper.commands.arguments.variables.NumberVariableValueArgument;
import rocks.gravili.notquests.paper.structs.ActiveObjective;
import rocks.gravili.notquests.paper.structs.QuestPlayer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class SkillLevelObjective extends Objective {
    public SkillLevelObjective(NotQuests main) {
        super(main);
    }

    private String mobToKillType;
    private String nameTagContainsAny;
    private String nameTagEquals;


    public static void handleCommands(
            NotQuests main,
            PaperCommandManager<CommandSender> manager,
            Command.Builder<CommandSender> addObjectiveBuilder) {
        addObjectiveBuilder =
                addObjectiveBuilder
                        .argument(
                                EntityTypeSelector.of("skillName", main),
                                ArgumentDescription.of("The name of the skill"))
                        .argument(
                                NumberVariableValueArgument.newBuilder("amount", main, null),
                                ArgumentDescription.of("Mastery to get"))
                        .argument(StringArgument.<CommandSender>newBuilder("Skill to level up").withSuggestionsProvider((context, lastString) -> {
                            ArrayList<String> completions = new ArrayList<>(Arrays.asList(Api.getStatTable()));
                            final List<String> allArgs = context.getRawInput();
                            main.getUtilManager().sendFancyCommandCompletion(context.getSender(), allArgs.toArray(new String[0]), "[Skill name]", "");
                            return completions;
                        }).build(), ArgumentDescription.of("Skill to level up"))
                        .flag(main.getCommandManager().nametag_equals)
                        .flag(main.getCommandManager().nametag_containsany);

        addObjectiveBuilder =
                addObjectiveBuilder.handler(
                        (context) -> {
                            final String skillName = context.get("skillName");
                            final String masterNeededExpression = context.get("amount");

                            final String[] a =
                                    context
                                            .flags()
                                            .getValue(main.getCommandManager().nametag_equals, new String[] {""});
                            final String[] b =
                                    context
                                            .flags()
                                            .getValue(main.getCommandManager().nametag_containsany, new String[] {""});
                            final String nametag_equals = String.join(" ", a);
                            final String nametag_containsany = String.join(" ", b);

                            SkillMasteryObjective skillMasteryObjective = new SkillMasteryObjective(main);

                            skillMasteryObjective.setSkillToMaster(skillName);
                            skillMasteryObjective.setProgressNeededExpression(masterNeededExpression);

                            // Add flags
                            skillMasteryObjective.setNameTagEquals(nametag_equals);
                            skillMasteryObjective.setNameTagContainsAny(nametag_containsany);

                            main.getObjectiveManager().addObjective(skillMasteryObjective, context);

                            if (!nametag_equals.isBlank()) {
                                context
                                        .getSender()
                                        .sendMessage(
                                                main.parse(
                                                        "<main>With nametag_equals flag: <highlight>"
                                                                + nametag_equals
                                                                + "</highlight>!"));
                            }
                            if (!nametag_containsany.isBlank()) {
                                context
                                        .getSender()
                                        .sendMessage(
                                                main.parse(
                                                        "main>With nametag_containsany flag: <highlight>"
                                                                + nametag_containsany
                                                                + "</highlight>!"));
                            }
                        });

        manager.command(addObjectiveBuilder);
    }

    @Override
    public String getTaskDescriptionInternal(
            final QuestPlayer questPlayer, final @Nullable ActiveObjective activeObjective) {
        return main.getLanguageManager()
                .getString(
                        "chat.objectives.taskDescription.levelUpSkill.base",
                        questPlayer,
                        activeObjective,
                        Map.of("%SKILLTOLEVELUP%", getSkillToLevelUp()));
    }

    public void setSkillToMaster(final String skillToMaster) {
        this.mobToKillType = skillToMaster;
    }

    @Override
    public void save(FileConfiguration configuration, String initialPath) {
        configuration.set(initialPath + ".specifics.skillToLevelUp", getSkillToLevelUp());

        // Extra args
        if (!getNameTagContainsAny().isBlank()) {
            configuration.set(initialPath + ".extras.nameTagContainsAny", getNameTagContainsAny());
        }
        if (!getNameTagEquals().isBlank()) {
            configuration.set(initialPath + ".extras.nameTagEquals", getNameTagEquals());
        }
    }

    @Override
    public void onObjectiveUnlock(
            final ActiveObjective activeObjective,
            final boolean unlockedDuringPluginStartupQuestLoadingProcess) {}

    @Override
    public void onObjectiveCompleteOrLock(
            final ActiveObjective activeObjective,
            final boolean lockedOrCompletedDuringPluginStartupQuestLoadingProcess,
            final boolean completed) {}

    public final String getSkillToLevelUp() {
        return mobToKillType;
    }

    // Extra args
    public final String getNameTagContainsAny() {
        return nameTagContainsAny;
    }

    public void setNameTagContainsAny(final String nameTagContainsAny) {
        this.nameTagContainsAny = nameTagContainsAny;
    }

    public final String getNameTagEquals() {
        return nameTagEquals;
    }

    public void setNameTagEquals(final String nameTagEquals) {
        this.nameTagEquals = nameTagEquals;
    }

    @Override
    public void load(FileConfiguration configuration, String initialPath) {
        mobToKillType = configuration.getString(initialPath + ".specifics.skillToLevelUp");

        // Extras
        final String nameTagContains =
                configuration.getString(initialPath + ".extras.nameTagContainsAny", "");
        if (!nameTagContains.isBlank()) {
            setNameTagContainsAny(nameTagContains);
        }

        final String nameTagEquals = configuration.getString(initialPath + ".extras.nameTagEquals", "");
        if (!nameTagEquals.isBlank()) {
            setNameTagEquals(nameTagEquals);
        }
    }
}
