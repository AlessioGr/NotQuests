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

package rocks.gravili.notquests.paper.structs.variables.hooks;

import me.clip.placeholderapi.PlaceholderAPI;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.commands.arguments.variables.StringVariableValueParser;
import rocks.gravili.notquests.paper.structs.QuestPlayer;
import rocks.gravili.notquests.paper.structs.variables.Variable;

import java.util.ArrayList;
import java.util.List;

public class PlaceholderAPINumberVariable extends Variable<Double> {
    public PlaceholderAPINumberVariable(NotQuests main) {
        super(main);
        addRequiredString(StringVariableValueParser.of("Placeholder", null, (context, input) -> {
            ArrayList<String> suggestions = new ArrayList<>();
            for (String identifier : PlaceholderAPI.getRegisteredIdentifiers()) {
                suggestions.add("%" + identifier + "_");
            }

            return suggestions;
        }));

        addRequiredBooleanFlag(rocks.gravili.notquests.paper.commands.framework.NQFlag.presence("removeTextFromPlaceholderValue", rocks.gravili.notquests.paper.commands.framework.NQDescription.of("Tries to remove all text from the placeholder before parsing")));
    }

    @Override
    public Double getValueInternally(QuestPlayer questPlayer, Object... objects) {
        if (questPlayer != null) {
            if (getRequiredStringValue("Placeholder") == null) {
                return 0D;
            }

            String placeholder =
                    PlaceholderAPI.setPlaceholders(
                            questPlayer.getPlayer(), getRequiredStringValue("Placeholder"));

            if (getRequiredBooleanValue("removeTextFromPlaceholderValue", questPlayer)) {
                placeholder = placeholder.replaceAll("[^\\d.]", "");
            }

            double parsedPlaceholder;
            try {
                if (placeholder.contains(".")) {
                    parsedPlaceholder = Double.parseDouble(placeholder);
                } else {
                    parsedPlaceholder = (double) Integer.parseInt(placeholder);
                }
            } catch (NumberFormatException e) {
                main.getLogManager()
                        .warn(
                                "Error: Placeholder Variable can not be parsed. Placeholder: <highlight>"
                                        + placeholder);
                return 0d;
            }

            return parsedPlaceholder;
        } else {
            return 0D;
        }
    }

    @Override
    public boolean setValueInternally(Double newValue, QuestPlayer questPlayer, Object... objects) {
        return false;
    }

    @Override
    public List<String> getPossibleValues(QuestPlayer questPlayer, Object... objects) {
        return null;
    }

    @Override
    public String getPlural() {
        return getRequiredStringValue("Placeholder");
    }

    @Override
    public String getSingular() {
        return getRequiredStringValue("Placeholder");
    }
}
