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
import rocks.gravili.notquests.Structs.Rewards.*;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashMap;

public class RewardManager {
    private final NotQuests main;

    private final HashMap<String, Class<? extends Reward>> rewards;


    public RewardManager(final NotQuests main) {
        this.main = main;
        rewards = new HashMap<>();

        registerDefaultRewards();

    }

    public void registerDefaultRewards() {
        rewards.clear();
        registerReward("ConsoleCommand", CommandReward.class);
        registerReward("QuestPoints", QuestPointsReward.class);
        registerReward("Item", ItemReward.class);
        registerReward("Money", MoneyReward.class);


    }


    public void registerReward(final String identifier, final Class<? extends Reward> reward) {
        main.getLogManager().info("Registering reward <AQUA>" + identifier);
        rewards.put(identifier, reward);

        try {
            Method commandHandler = reward.getMethod("handleCommands", main.getClass(), PaperCommandManager.class, Command.Builder.class);
            commandHandler.invoke(reward, main, main.getCommandManager().getPaperCommandManager(), main.getCommandManager().getAdminEditAddRewardCommandBuilder());
        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
            e.printStackTrace();
        }
    }


    public final Class<? extends Reward> getRewardClass(final String type) {
        return rewards.get(type);
    }

    public final String getRewardType(final Class<? extends Reward> reward) {
        for (final String rewardType : rewards.keySet()) {
            if (rewards.get(rewardType).equals(reward)) {
                return rewardType;
            }
        }
        return null;
    }

    public final HashMap<String, Class<? extends Reward>> getRewardsAndIdentfiers() {
        return rewards;
    }

    public final Collection<Class<? extends Reward>> getRewards() {
        return rewards.values();
    }

    public final Collection<String> getRewardIdentifiers() {
        return rewards.keySet();
    }
}
