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

package rocks.gravili.notquests.paper.structs.variables.tags;

import cloud.commandframework.arguments.standard.StringArgument;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.command.CommandSender;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.managers.tags.Tag;
import rocks.gravili.notquests.paper.managers.tags.TagType;
import rocks.gravili.notquests.paper.structs.QuestPlayer;
import rocks.gravili.notquests.paper.structs.variables.Variable;

public class DoubleTagVariable extends Variable<Double> {

    public DoubleTagVariable(final NotQuests main) {
        super(main);

        addRequiredString(
                StringArgument.<CommandSender>newBuilder("TagName").withSuggestionsProvider(
                        (context, lastString) -> {
                            final List<String> allArgs = context.getRawInput();
                            main.getUtilManager().sendFancyCommandCompletion(context.getSender(), allArgs.toArray(new String[0]), "[Tag Name]", "[...]");

                            final ArrayList<String> suggestions = new ArrayList<>();
                            for (final Tag tag : main.getTagManager().getTags()) {
                                if (tag.getTagType() == TagType.DOUBLE) {
                                    suggestions.add("" + tag.getTagName());
                                }
                            }
                            return suggestions;

                        }
                ).single().build()
        );

        setCanSetValue(true);
    }

    @Override
    public Double getValueInternally(final QuestPlayer questPlayer, final Object... objects) {
        if (questPlayer == null) {
            return 0d;
        }

        final String tagName = getRequiredStringValue("TagName");
        final Tag tag = main.getTagManager().getTag(tagName);
        if (tag == null) {
            main.getLogManager().warn("Error reading tag " + tagName + ". Tag does not exist.");
            return 0d;
        }
        if(tag.getTagType() != TagType.DOUBLE){
            main.getLogManager().warn("Error reading tag " + tagName + ". Tag is no double tag.");
            return 0d;
        }

        final Object value = questPlayer.getTagValue(tagName);

        if (value instanceof final Double doubleValue) {
            return doubleValue;
        } else {
            return 0d;
        }

    }

    @Override
    public boolean setValueInternally(final Double newValue, final QuestPlayer questPlayer, final Object... objects) {
        if (questPlayer == null) {
            return false;
        }

        final String tagName = getRequiredStringValue("TagName");
        final Tag tag = main.getTagManager().getTag(tagName);
        if (tag == null) {
            main.getLogManager().warn("Error reading tag " + tagName + ". Tag does not exist.");
            return false;
        }
        if(tag.getTagType() != TagType.DOUBLE){
            main.getLogManager().warn("Error reading tag " + tagName + ". Tag is no double tag.");
            return false;
        }


        questPlayer.setTagValue(tagName, newValue);

        return true;
    }


    @Override
    public final List<String> getPossibleValues(final QuestPlayer questPlayer, final Object... objects) {
        return null;
    }

    @Override
    public final String getPlural() {
        return "Tags";
    }

    @Override
    public final String getSingular() {
        return "Tag";
    }
}
