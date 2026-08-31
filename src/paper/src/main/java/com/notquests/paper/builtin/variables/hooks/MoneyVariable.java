package com.notquests.paper.builtin.variables.hooks;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import com.notquests.core.platform.NotQuestsAdapter;
import com.notquests.core.platform.PlatformPlayer;
import com.notquests.paper.NotQuests;
import com.notquests.paper.PaperPlayer;

import java.util.UUID;

public final class MoneyVariable {
    private MoneyVariable() {}

    public static void register(final NotQuests main, final NotQuestsAdapter adapter) {
        adapter.variables()
                .numberVariable("Money")
                .displayName("Money")
                .description("Reads or changes the target player's Vault economy balance.")
                .singular("Money")
                .plural("Money")
                .get(context -> balance(main, context.questPlayer()))
                .set((newValue, context) -> setBalance(main, context.questPlayer(), newValue.doubleValue()))
                .register();
    }

    private static double balance(final NotQuests main, final PlatformPlayer questPlayer) {
        final Player player = player(questPlayer);
        if (player == null || !vaultReady(main)) {
            return 0;
        }
        return main.integrations()
                .vault()
                .getEconomy()
                .getBalance(player, player.getWorld().getName());
    }

    private static boolean setBalance(
            final NotQuests main, final PlatformPlayer questPlayer, final double balance) {
        final Player player = player(questPlayer);
        if (player == null || !vaultReady(main)) {
            return false;
        }
        final var economy = main.integrations().vault().getEconomy();
        final String world = player.getWorld().getName();
        final double currentBalance = economy.getBalance(player, world);
        if (balance > currentBalance) {
            return economy.depositPlayer(player, world, balance - currentBalance).transactionSuccess();
        }
        return economy.withdrawPlayer(player, world, currentBalance - balance).transactionSuccess();
    }

    private static boolean vaultReady(final NotQuests main) {
        return main.getCorePlugin().integrationEnabled("Vault")
                && main.integrations().vault() != null
                && main.integrations().vault().getEconomy() != null;
    }

    private static Player player(final PlatformPlayer questPlayer) {
        if (questPlayer == null) {
            return null;
        }
        final String identifier = questPlayer.playerIdentifier();
        if (identifier != null && !identifier.isBlank()) {
            try {
                final Player player = Bukkit.getPlayer(UUID.fromString(identifier));
                if (player != null) {
                    return player;
                }
            } catch (final IllegalArgumentException ignored) {
                // Non-UUID platform identifiers can still be resolved by player name.
            }
        }
        final String name = questPlayer.playerName();
        return name == null || name.isBlank() ? null : Bukkit.getPlayerExact(name);
    }
}
