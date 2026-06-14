/*
 * NotQuests - A Questing plugin for Minecraft Servers
 * Copyright (C) 2021-2022 Alessio Gravili
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

package rocks.gravili.notquests.paper.commands.arguments;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.enchantments.Enchantment;
import rocks.gravili.notquests.paper.commands.framework.NQArgumentType;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Native-framework port of Cloud's {@code EnchantmentParser}: resolves a Bukkit
 * {@link Enchantment} by its (lower-cased) namespaced key.
 */
public final class EnchantmentArgument extends NQArgumentType<Enchantment> {

    public static EnchantmentArgument enchantmentArgument() {
        return new EnchantmentArgument();
    }

    @Override
    public Enchantment convert(final String input) throws CommandSyntaxException {
        final Enchantment enchantment =
                Registry.ENCHANTMENT.get(NamespacedKey.minecraft(input.toLowerCase(Locale.ROOT)));
        if (enchantment == null) {
            throw fail("Enchantment '" + input + "' not found");
        }
        return enchantment;
    }

    @Override
    protected List<String> suggest(final CommandContext<?> context, final String remaining) {
        final List<String> names = new ArrayList<>();
        for (final Enchantment enchantment : Registry.ENCHANTMENT) {
            names.add(enchantment.getKey().getKey());
        }
        return names;
    }
}
