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

package rocks.gravili.notquests.paper.structs.actions;

import cloud.commandframework.ArgumentDescription;
import cloud.commandframework.Command;
import cloud.commandframework.arguments.standard.IntegerArgument;
import cloud.commandframework.paper.PaperCommandManager;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.commands.arguments.MultipleActionsSelector;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ActionAction extends Action {

    private ArrayList<Action> actions = null;
    private int amount = 1;
    private boolean ignoreConditions = false;


    public ActionAction(final NotQuests main) {
        super(main);
    }

    public static void handleCommands(NotQuests main, PaperCommandManager<CommandSender> manager, Command.Builder<CommandSender> builder, ActionFor rewardFor) {
        manager.command(builder
                .argument(MultipleActionsSelector.of("Actions", main), ArgumentDescription.of("Name of the actions which will be executed"))
                .argument(IntegerArgument.<CommandSender>newBuilder("amount").asOptionalWithDefault(1).withMin(1), ArgumentDescription.of("Amount of times the action will be executed."))
                .flag(
                        manager.flagBuilder("ignoreConditions")
                                .withDescription(ArgumentDescription.of("Ignores action conditions"))
                )
                .handler((context) -> {
                    ArrayList<Action> foundActions = context.get("Actions");
                    int amount = context.get("amount");
                    final boolean ignoreConditions = context.flags().isPresent("ignoreConditions");

                    ActionAction actionAction = new ActionAction(main);
                    actionAction.setActions(foundActions);
                    actionAction.setAmount(amount);

                    actionAction.setIgnoreConditions(ignoreConditions);

                    main.getActionManager().addAction(actionAction, context);
                }));
    }

    public final ArrayList<Action> getActions() {
        return actions;
    }

    public void setActions(final ArrayList<Action> actions) {
        this.actions = actions;
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
    public void executeInternally(final Player player, Object... objects) {
        if (actions == null || actions.isEmpty()) {
            main.getLogManager().warn("Tried to execute Action of Action action with no valid actions.");
            return;
        }

        main.getLogManager().debug("Executing Action action. IsIgnoreConditions: " + isIgnoreConditions());

        for(final Action action : getActions()){
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

    }

    @Override
    public void save(FileConfiguration configuration, String initialPath) {
        if (getActions() != null && !getActions().isEmpty()) {
            ArrayList<String> actionsStringList = new ArrayList<>();
            for(final Action action : getActions()){
                actionsStringList.add(action.getActionName());
            }
            configuration.set(initialPath + ".specifics.actions", actionsStringList);
        } else {
            main.getLogManager().warn("Error: cannot save Action for action action, because it's null. Configuration path: " + initialPath);
        }
        configuration.set(initialPath + ".specifics.amount", getAmount());
        configuration.set(initialPath + ".specifics.ignoreConditions", isIgnoreConditions());
    }

    @Override
    public void load(final FileConfiguration configuration, String initialPath) {
        this.actions = new ArrayList<>();

        if(configuration.contains(initialPath + ".specifics.actions")){
            List<String> actionNames = configuration.getStringList(initialPath + ".specifics.actions");
            for(String actionName : actionNames){
                final Action action = main.getActionsYMLManager().getAction(actionName);
                if (action == null) {
                    main.getLogManager().warn("Error: ActionAction cannot find the action with name " + actionName + ". Action Path: " + initialPath);
                }else{
                    actions.add(action);
                }
            }
        }else{
            String actionName = configuration.getString(initialPath + ".specifics.action");
            final Action action = main.getActionsYMLManager().getAction(actionName);
            if (action == null) {
                main.getLogManager().warn("Error: ActionAction cannot find the action with name " + actionName + ". Action Path: " + initialPath);
            }else{
                actions.add(action);
            }
        }


        this.amount = configuration.getInt(initialPath + ".specifics.amount", 1);
        this.ignoreConditions = configuration.getBoolean(initialPath + ".specifics.ignoreConditions", false);

    }

    @Override
    public void deserializeFromSingleLineString(ArrayList<String> arguments) {
        String actionNames = arguments.get(0);
        this.actions = new ArrayList<>();

        for(String actionName : actionNames.split(",")){
            final Action action = main.getActionsYMLManager().getAction(actionName);
            if (action == null) {
                main.getLogManager().warn("Error: ActionAction cannot find the action with name " + actionName + ". Action Name: " + arguments.get(0));
            }else{
                actions.add(action);
            }
        }


        if(arguments.size() >= 2){
            this.amount = Integer.parseInt(arguments.get(1));
        }else{
            this.amount = 1;
        }

        this.ignoreConditions = String.join(" ", arguments).toLowerCase(Locale.ROOT).contains("--ignoreconditions");

    }


    @Override
    public String getActionDescription(final Player player, final Object... objects) {
        return "Executes Actions: " + getActions().toString();
    }
}
