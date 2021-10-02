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

package notquests.notquests.Hooks.BetonQuest.Events;

import notquests.notquests.NotQuests;
import notquests.notquests.Structs.ActiveQuest;
import notquests.notquests.Structs.Quest;
import notquests.notquests.Structs.QuestPlayer;
import org.betonquest.betonquest.Instruction;
import org.betonquest.betonquest.api.Condition;
import org.betonquest.betonquest.exceptions.InstructionParseException;
import org.betonquest.betonquest.exceptions.QuestRuntimeException;
import org.betonquest.betonquest.id.ID;
import org.betonquest.betonquest.utils.PlayerConverter;
import org.bukkit.entity.Player;

public class BQQuestActiveCondition extends Condition {

    private final NotQuests main;
    private Quest quest;

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
    public BQQuestActiveCondition(Instruction instruction) throws InstructionParseException {
        super(instruction, false);
        this.main = NotQuests.getInstance();

        final String questName = instruction.getPart(1);

        boolean foundQuest = false;
        for (Quest quest : main.getQuestManager().getAllQuests()) {
            if (quest.getQuestName().equalsIgnoreCase(questName)) {
                foundQuest = true;
                this.quest = quest;
                break;
            }
        }

        if (!foundQuest) {
            throw new InstructionParseException("NotQuests Quest with the name '" + questName + "' does not exist.");
        }


    }

    @Override
    protected Boolean execute(String playerID) throws QuestRuntimeException {
        if (quest != null) {

            final Player player = PlayerConverter.getPlayer(playerID);

            if (player != null) {
                final QuestPlayer questPlayer = main.getQuestPlayerManager().getQuestPlayer(player.getUniqueId());
                if (questPlayer != null) {

                    for (final ActiveQuest activeQuest : questPlayer.getActiveQuests()) {
                        if (activeQuest.getQuest().getQuestName().equalsIgnoreCase(quest.getQuestName())) {
                            return true;
                        }
                    }
                    return false;

                }
                return false;

            }

        } else {
            throw new QuestRuntimeException("NotQuests Quest of this BetonQuest event does not exist.");
        }
        return null;
    }
}
