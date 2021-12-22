/*
 * NotQuests - A Questing plugin for Minecraft Servers
 * Copyright (C) 2021 Alessio Gravili
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package rocks.gravili.notquests.Structs.Actions;

import cloud.commandframework.ArgumentDescription;
import cloud.commandframework.Command;
import cloud.commandframework.arguments.standard.IntegerArgument;
import cloud.commandframework.meta.CommandMeta;
import cloud.commandframework.paper.PaperCommandManager;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import rocks.gravili.notquests.Commands.newCMDs.arguments.ActionSelector;
import rocks.gravili.notquests.NotQuests;

public class ActionAction extends Action {

    private Action action = null;
    private int amount = 1;
    private boolean ignoreConditions = false;


    public ActionAction(final NotQuests main) {
        super(main);
    }

    public static void handleCommands(NotQuests main, PaperCommandManager<CommandSender> manager, Command.Builder<CommandSender> builder, ActionFor rewardFor) {
        manager.command(builder.literal("Action")
                .argument(ActionSelector.of("Action", main), ArgumentDescription.of("Name of the action which will be executed"))
                .argument(IntegerArgument.<CommandSender>newBuilder("amount").asOptionalWithDefault(1).withMin(1), ArgumentDescription.of("Amount of times the action will be executed."))
                .flag(
                        manager.flagBuilder("ignoreConditions")
                                .withDescription(ArgumentDescription.of("Ignores action conditions"))
                )
                .meta(CommandMeta.DESCRIPTION, "Creates a new (actions.yml) Action")
                .handler((context) -> {
                    Action foundAction = context.get("Action");
                    int amount = context.get("amount");

                    ActionAction actionAction = new ActionAction(main);
                    actionAction.setAction(foundAction);
                    actionAction.setAmount(amount);

                    main.getActionManager().addAction(actionAction, context);
                }));
    }

    public final Action getAction() {
        return action;
    }

    public void setAction(final Action action) {
        this.action = action;
    }

    public final int getAmount() {
        return amount;
    }

    public void setAmount(final int amount) {
        this.amount = amount;
    }

    public final boolean isIgnoreConditions() {
        return ignoreConditions;
    }

    public void setIgnoreConditions(final boolean ignoreConditions) {
        this.ignoreConditions = ignoreConditions;
    }

    @Override
    public void execute(final Player player, Object... objects) {
        if (action == null) {
            main.getLogManager().warn("Tried to execute Action of Action action with null action.");
            return;
        }

        main.getLogManager().debug("Executing Action action. IsIgnoreConditions: " + isIgnoreConditions());

        if (!isIgnoreConditions()) {
            if (amount == 1) {
                main.getActionManager().executeActionWithConditions(action, main.getQuestPlayerManager().getOrCreateQuestPlayer(player.getUniqueId()), null, true, objects);
            } else {
                for (int i = 0; i < amount; i++) {
                    main.getActionManager().executeActionWithConditions(action, main.getQuestPlayerManager().getOrCreateQuestPlayer(player.getUniqueId()), null, true, objects);
                }
            }
        } else {
            if (amount == 1) {
                action.execute(player, objects);
            } else {
                for (int i = 0; i < amount; i++) {
                    action.execute(player, objects);
                }
            }
        }

    }

    @Override
    public void save(FileConfiguration configuration, String initialPath) {
        if (getAction() != null) {
            configuration.set(initialPath + ".specifics.action", getAction().getActionName());
        } else {
            main.getLogManager().warn("Error: cannot save Action for action action, because it's null. Configuration path: " + initialPath);
        }
        configuration.set(initialPath + ".specifics.amount", getAmount());
        configuration.set(initialPath + ".specifics.ignoreConditions", isIgnoreConditions());
    }

    @Override
    public void load(final FileConfiguration configuration, String initialPath) {
        String actionName = configuration.getString(initialPath + ".specifics.action");
        this.action = main.getActionsYMLManager().getAction(actionName);
        if (action == null) {
            main.getLogManager().warn("Error: ActionAction cannot find the action with name " + actionName + ". Action Path: " + initialPath);
        }

        this.amount = configuration.getInt(initialPath + ".specifics.amount", 1);
        this.ignoreConditions = configuration.getBoolean(initialPath + ".specifics.ignoreConditions", false);

    }


    @Override
    public String getActionDescription() {
        return "Executes Action: " + getAction().getActionName();
    }
}
