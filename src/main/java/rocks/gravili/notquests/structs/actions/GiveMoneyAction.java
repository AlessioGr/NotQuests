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

package rocks.gravili.notquests.structs.actions;

import cloud.commandframework.ArgumentDescription;
import cloud.commandframework.Command;
import cloud.commandframework.arguments.standard.IntegerArgument;
import cloud.commandframework.meta.CommandMeta;
import cloud.commandframework.paper.PaperCommandManager;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import rocks.gravili.notquests.NotQuests;


public class GiveMoneyAction extends Action {

    private long rewardedMoney = 0;


    public GiveMoneyAction(final NotQuests main) {
        super(main);
    }

    public static void handleCommands(NotQuests main, PaperCommandManager<CommandSender> manager, Command.Builder<CommandSender> builder, ActionFor rewardFor) {
        if (!main.getIntegrationsManager().isVaultEnabled()) {
            return;
        }

        manager.command(builder.literal("GiveMoney")
                .argument(IntegerArgument.<CommandSender>newBuilder("amount").withMin(1), ArgumentDescription.of("Amount of money the player will receive."))
                .meta(CommandMeta.DESCRIPTION, "Adds a new GiveMoney Reward to a quest")
                .handler((context) -> {
                    final int moneyAmount = context.get("amount");

                    GiveMoneyAction giveMoneyAction = new GiveMoneyAction(main);
                    giveMoneyAction.setRewardedMoney(moneyAmount);
                    main.getActionManager().addAction(giveMoneyAction, context);

                }));
    }

    public void setRewardedMoney(final long rewardedMoney) {
        this.rewardedMoney = rewardedMoney;
    }

    @Override
    public void execute(final Player player, Object... objects) {
        if (rewardedMoney == 0) {
            main.getLogManager().warn("Tried to give money reward, but the amount of money is 0.");
            return;
        }

        if (!main.getIntegrationsManager().isVaultEnabled() || main.getIntegrationsManager().getVaultManager().getEconomy() == null) {
            Audience audience = main.adventure().player(player);
            audience.sendMessage(MiniMessage.miniMessage().parse(
                    "<RED>Error: cannot give you the money reward because Vault (needed for money stuff to work) is not installed on the server."
            ));
            return;
        }
        if (rewardedMoney > 0) {
            main.getIntegrationsManager().getVaultManager().getEconomy().depositPlayer(player, rewardedMoney);
        } else if (rewardedMoney < 0) {
            main.getIntegrationsManager().getVaultManager().getEconomy().withdrawPlayer(player, Math.abs(rewardedMoney));
        }
    }

    @Override
    public String getActionDescription() {
        return "Money: " + getRewardedMoney();
    }

    @Override
    public void save(final FileConfiguration configuration, String initialPath) {
        configuration.set(initialPath + ".specifics.moneyAmount", getRewardedMoney());
    }


    public final long getRewardedMoney() {
        return rewardedMoney;
    }

    @Override
    public void load(final FileConfiguration configuration, String initialPath) {
        this.rewardedMoney = configuration.getLong(initialPath + ".specifics.moneyAmount");
    }
}