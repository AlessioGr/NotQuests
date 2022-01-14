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

package rocks.gravili.notquests.paper.managers.integrations.betonquest.conditions;

import org.betonquest.betonquest.Instruction;
import org.betonquest.betonquest.exceptions.InstructionParseException;
import org.betonquest.betonquest.exceptions.QuestRuntimeException;
import org.betonquest.betonquest.id.ID;
import org.betonquest.betonquest.utils.PlayerConverter;
import org.betonquest.betonquest.utils.Utils;
import org.bukkit.entity.Player;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.structs.CompletedQuest;
import rocks.gravili.notquests.paper.structs.Quest;
import rocks.gravili.notquests.paper.structs.QuestPlayer;
import rocks.gravili.notquests.paper.structs.conditions.Condition;

import java.util.List;

public class BQRequirementsCondition extends org.betonquest.betonquest.api.Condition { //TODO: Make it dynamic for future or API requirements

    private final NotQuests main;
    private Condition condition = null;

    /**
     * Creates new instance of the condition. The condition should parse
     * instruction string at this point and extract all the data from it. If
     * anything goes wrong, throw {@link InstructionParseException} with an
     * error message describing the problem.
     *
     * @param instruction the Instruction object; you can get one from ID instance with
     *                    {@link ID#generateInstruction()
     *                    ID.generateInstruction()} or create it from an instruction
     *                    string
     */
    public BQRequirementsCondition(Instruction instruction) throws InstructionParseException {
        super(instruction, false);
        this.main = NotQuests.getInstance();

        final String[] conditionLine = Utils.split(instruction.toString());

        try {
            condition = main.getConversationManager().parseConditionsString(List.of(conditionLine)).get(0);
        } catch (Exception e) {
            throw new RuntimeException("Invalid Condition line: " + e.getLocalizedMessage());
        }
    }

    @Override
    protected Boolean execute(String playerID) throws QuestRuntimeException {
        if (condition != null) {
            final Player player = PlayerConverter.getPlayer(playerID);

            return condition.check(main.getQuestPlayerManager().getOrCreateQuestPlayer(player.getUniqueId())).isBlank();
        } else {
            throw new QuestRuntimeException("Condition was not found.");
        }
    }
}
