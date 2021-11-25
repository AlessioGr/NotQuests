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

package rocks.gravili.notquests.Managers.Registering;

import cloud.commandframework.Command;
import cloud.commandframework.paper.PaperCommandManager;
import rocks.gravili.notquests.NotQuests;
import rocks.gravili.notquests.Structs.Requirements.*;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashMap;

public class RequirementManager {
    private final NotQuests main;

    private final HashMap<String, Class<? extends Requirement>> requirements;


    public RequirementManager(final NotQuests main) {
        this.main = main;
        requirements = new HashMap<>();

        registerDefaultRequirements();

    }

    public void registerDefaultRequirements() {
        requirements.clear();
        registerRequirement("OtherQuest", OtherQuestRequirement.class);
        registerRequirement("QuestPoints", QuestPointsRequirement.class);
        registerRequirement("Permission", PermissionRequirement.class);
        registerRequirement("Money", MoneyRequirement.class);


    }


    public void registerRequirement(final String identifier, final Class<? extends Requirement> requirement) {
        main.getLogManager().info("Registering requirement <AQUA>" + identifier);
        requirements.put(identifier, requirement);

        try {
            Method commandHandler = requirement.getMethod("handleCommands", main.getClass(), PaperCommandManager.class, Command.Builder.class);
            commandHandler.invoke(requirement, main, main.getCommandManager().getPaperCommandManager(), main.getCommandManager().getAdminEditAddRequirementCommandBuilder());
        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
            e.printStackTrace();
        }
    }


    public final Class<? extends Requirement> getRequirementClass(final String type) {
        return requirements.get(type);
    }

    public final String getRequirementType(final Class<? extends Requirement> requirement) {
        for (final String requirementType : requirements.keySet()) {
            if (requirements.get(requirementType).equals(requirement)) {
                return requirementType;
            }
        }
        return null;
    }

    public final HashMap<String, Class<? extends Requirement>> getRequirementsAndIdentfiers() {
        return requirements;
    }

    public final Collection<Class<? extends Requirement>> getRequirements() {
        return requirements.values();
    }

    public final Collection<String> getRequirementIdentifiers() {
        return requirements.keySet();
    }
}
