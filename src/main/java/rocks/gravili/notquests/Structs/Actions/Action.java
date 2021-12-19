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


import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import rocks.gravili.notquests.NotQuests;
import rocks.gravili.notquests.Structs.Objectives.Objective;
import rocks.gravili.notquests.Structs.Quest;

public abstract class Action {


    protected final NotQuests main;
    private String actionName = "";
    private Quest quest;
    private Objective objective;


    public Action(NotQuests main) {
        this.main = main;
    }

    public final String getActionType() {
        return main.getActionManager().getActionType(this.getClass());
    }

    public final String getActionName() {
        return ChatColor.translateAlternateColorCodes('&', LegacyComponentSerializer.builder().hexColors().build().serialize(MiniMessage.miniMessage().parse(actionName)));
    }

    public void setActionName(final String actionName) {
        this.actionName = actionName;

    }

    public void removeActionName() {
        this.actionName = "";
    }

    public final Quest getQuest() {
        return quest;
    }

    public void setQuest(final Quest quest) {
        this.quest = quest;
    }

    public final Objective getObjective() {
        return objective;
    }

    public void setObjective(final Objective objective) {
        this.objective = objective;
    }

    public abstract String getActionDescription();

    public abstract void execute(final Player player, Object... objects);

    public abstract void save(final FileConfiguration configuration, final String initialPath);

    public abstract void load(final FileConfiguration configuration, final String initialPath);

    /* public void setConsoleCommand(String newConsoleCommand) {
        this.consoleCommand = newConsoleCommand;
        main.getActionsManager().getActionsConfig().set("actions." + actionName + ".consoleCommand", newConsoleCommand);
    }*/
}
