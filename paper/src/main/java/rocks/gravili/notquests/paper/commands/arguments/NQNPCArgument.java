/*
 * NotQuests - A Questing plugin for Minecraft Servers
 * Copyright (C) 2022 Alessio Gravili
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
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.commands.arguments.wrappers.NQNPCResult;
import rocks.gravili.notquests.paper.commands.framework.NQArgumentType;
import rocks.gravili.notquests.paper.managers.npc.NQNPC;
import rocks.gravili.notquests.paper.managers.npc.NQNPCID;

import java.util.ArrayList;
import java.util.List;

/**
 * Native-framework port of {@code NQNPCParser}: resolves an {@link NQNPCResult} from a
 * {@code [NPC Plugin Name]:[NPC ID]} token, or the literals {@code none} / {@code rightClickSelect}
 * when allowed.
 */
public final class NQNPCArgument extends NQArgumentType<NQNPCResult> {
    private final NotQuests main;

    private final boolean allowNone;
    private final boolean allowRightClickSelect;

    public NQNPCArgument(final NotQuests main, final boolean allowNone, final boolean allowRightClickSelect) {
        this.main = main;
        this.allowNone = allowNone;
        this.allowRightClickSelect = allowRightClickSelect;
    }

    public static NQNPCArgument nqNPCArgument(final NotQuests main, final boolean allowNone, final boolean allowRightClickSelect) {
        return new NQNPCArgument(main, allowNone, allowRightClickSelect);
    }

    public static NQNPCArgument nqNPCArgument(final NotQuests main) {
        return new NQNPCArgument(main, false, false);
    }

    @Override
    public ArgumentType<String> getNativeType() {
        return StringArgumentType.greedyString();
    }

    @Override
    public NQNPCResult convert(final String input) throws CommandSyntaxException {
        if (input == null || input.isEmpty()) {
            throw fail("Invalid NPC: empty input");
        }

        if (allowNone && input.equalsIgnoreCase("none")) {
            return new NQNPCResult(null, true, false);
        }

        if (allowRightClickSelect && input.equalsIgnoreCase("rightClickSelect")) {
            return new NQNPCResult(null, false, true);
        }

        if (!input.contains(":") || input.split(":").length != 2) {
            throw fail("Wrong input. Format needs to be [NPC Plugin Name]:[NPC ID]. Please follow the command suggestions.");
        }

        for (String npcIdentifier : NotQuests.getInstance().getNPCManager().getAllNPCsString()) {
            if (npcIdentifier.equalsIgnoreCase(input)) {
                String type = npcIdentifier.split(":")[0];
                String id = npcIdentifier.split(":")[1];
                // FancyNPCs uses String (UUID) ids; Citizens uses int ids. Parsing a FancyNPCs UUID as
                // an int threw NumberFormatException, so branch on the NPC backend.
                final NQNPCID nqnpcid;
                if (type.equalsIgnoreCase("fancynpcs")) {
                    nqnpcid = NQNPCID.fromString(id);
                } else {
                    try {
                        nqnpcid = NQNPCID.fromInteger(Integer.parseInt(id));
                    } catch (final NumberFormatException e) {
                        throw fail("Invalid NPC id '" + id + "' for type '" + type + "'");
                    }
                }
                NQNPC npc = main.getNPCManager().getOrCreateNQNpc(type, nqnpcid);
                if (npc == null) {
                    throw fail("No NPC found: " + input);
                }
                // A concrete NPC was resolved, so this is neither a "none" nor a "rightClickSelect"
                // request — passing the parser's allow* config here made every specified NPC behave
                // like rightClickSelect.
                return new NQNPCResult(npc, false, false);
            }
        }
        throw fail("No NPC found: " + input);
    }

    @Override
    protected List<String> suggest(final CommandContext<?> context, final String remaining) {
        final List<String> entries = new ArrayList<>();
        if (allowRightClickSelect) {
            entries.add("rightClickSelect");
        }
        if (allowNone) {
            entries.add("none");
        }
        for (final String npcIdentifier : main.getNPCManager().getAllNPCsString()) {
            entries.add(npcIdentifier);
        }
        return entries;
    }
}
