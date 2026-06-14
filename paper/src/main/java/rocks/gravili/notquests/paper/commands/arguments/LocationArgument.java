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

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import rocks.gravili.notquests.paper.commands.framework.NQArgumentType;

import java.util.List;

/**
 * Native-framework port of Cloud's {@code LocationParser}. Because native argument types read a
 * single token, this expects a comma-separated {@code x,y,z} (optionally world-prefixed:
 * {@code world,x,y,z}). Backed by a greedy string so the whole coordinate token is captured.
 */
public final class LocationArgument extends NQArgumentType<Location> {

    public static LocationArgument locationArgument() {
        return new LocationArgument();
    }

    @Override
    public Location convert(final String input) throws CommandSyntaxException {
        final String[] parts = input.trim().split(",");
        try {
            if (parts.length == 3) {
                final double x = Double.parseDouble(parts[0].trim());
                final double y = Double.parseDouble(parts[1].trim());
                final double z = Double.parseDouble(parts[2].trim());
                return new Location(null, x, y, z);
            } else if (parts.length == 4) {
                final World world = Bukkit.getWorld(parts[0].trim());
                final double x = Double.parseDouble(parts[1].trim());
                final double y = Double.parseDouble(parts[2].trim());
                final double z = Double.parseDouble(parts[3].trim());
                return new Location(world, x, y, z);
            }
        } catch (final NumberFormatException e) {
            throw fail("'" + input + "' is not a valid location (expected x,y,z or world,x,y,z)");
        }
        throw fail("'" + input + "' is not a valid location (expected x,y,z or world,x,y,z)");
    }

    @Override
    public ArgumentType<String> getNativeType() {
        return StringArgumentType.greedyString();
    }

    @Override
    protected List<String> suggest(final CommandContext<?> context, final String remaining) {
        return List.of();
    }
}
