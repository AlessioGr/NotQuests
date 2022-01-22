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

package rocks.gravili.notquests.paper.structs.actions.hooks.betonquest;

import cloud.commandframework.ArgumentDescription;
import cloud.commandframework.Command;
import cloud.commandframework.arguments.standard.StringArrayArgument;
import cloud.commandframework.paper.PaperCommandManager;
import org.apache.commons.lang.StringUtils;
import org.betonquest.betonquest.BetonQuest;
import org.betonquest.betonquest.Instruction;
import org.betonquest.betonquest.api.QuestEvent;
import org.betonquest.betonquest.config.Config;
import org.betonquest.betonquest.exceptions.QuestRuntimeException;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.structs.actions.Action;
import rocks.gravili.notquests.paper.structs.actions.ActionFor;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class BetonQuestFireInlineEventAction extends Action {

    private String event = "";
    private QuestEvent cachedEvent = null;

    public BetonQuestFireInlineEventAction(final NotQuests main) {
        super(main);
    }

    public static void handleCommands(NotQuests main, PaperCommandManager<CommandSender> manager, Command.Builder<CommandSender> builder, ActionFor rewardFor) {
        manager.command(builder
                .argument(StringArrayArgument.of("event",
                        (context, lastString) -> {
                            final List<String> allArgs = context.getRawInput();
                            main.getUtilManager().sendFancyCommandCompletion(context.getSender(), allArgs.toArray(new String[0]), "<Enter new BetonQuest inline event>", "");
                            ArrayList<String> completions = new ArrayList<>();
                            String rawInput = context.getRawInputJoined();
                            String curInput =  context.getRawInputJoined().substring(
                                    context.getRawInputJoined().indexOf("BetonQuestFireInlineEvent ") + 26
                            );
                            String[] curInputSplit =  curInput.split(" ");

                            //context.getSender().sendMessage(curInput);

                            if(curInputSplit.length <= 0){
                                Map<String, Class<? extends QuestEvent>> eventTypes = null;
                                try{
                                    Class<?> betonQuestClass = BetonQuest.class;
                                    Field eventTypesField =  betonQuestClass.getDeclaredField("EVENT_TYPES");
                                    eventTypesField.setAccessible(true);
                                    eventTypes = (Map<String, Class<? extends QuestEvent>>) eventTypesField.get(null);
                                }catch (Exception ignored){
                                    //ignored.printStackTrace();
                                }
                                if(eventTypes != null){
                                    completions.addAll(eventTypes.keySet());
                                }
                            }else{
                                String eventClassName = curInputSplit[0];
                                Class<? extends QuestEvent> eventClass = BetonQuest.getInstance().getEventClass(eventClassName);
                                if (eventClass == null) {
                                    return null;
                                }
                                int lenght = curInputSplit.length;

                                switch (eventClassName){
                                    case "cancel":
                                        if(lenght == 1){
                                            completions.add("<name of a quest canceler, as defined in main.yml>");
                                        }
                                        break;
                                    case "chat":
                                        completions.add("<enter chat message>");
                                        break;
                                    default:
                                        completions.add("<arguments>");
                                }

                            }



                            return completions;
                        }
                ), ArgumentDescription.of("BetonQuest Event"))
                .handler((context) -> {
                    final String event = String.join(" ", (String[]) context.get("event"));

                    //QuestEvent questEvent;

                    BetonQuestFireEventAction betonQuestFireEventAction = new BetonQuestFireEventAction(main);
                    betonQuestFireEventAction.setEventName(event);


                    main.getActionManager().addAction(betonQuestFireEventAction, context);
                }));
    }

    public final String getEvent() {
        return event;
    }

    public void setEvent(final String event) {
        this.event = event;
    }

    public final QuestEvent getQuestEvent(){
        if(cachedEvent == null){
            final Class<? extends QuestEvent> eventClass = BetonQuest.getInstance().getEventClass(getEvent().split(" ")[0]);
            if (eventClass == null) {
                // if it's null then there is no such type registered, log
                // an error
                main.getLogManager().warn("Error: Event type " + getEvent().split(" ")[0] + " was not found!");
                return null;
            }

            String instruction = "";
            int counter = 0;
            for(String part : getEvent().split(" ")){
                if(++counter == 2){
                    instruction += part;
                }else if(counter > 2){
                    instruction += " " + part;
                }
            }

            Instruction instructionObject = new Instruction(Config.getDefaultPackage(), null, instruction);

            try{
                cachedEvent = eventClass.getConstructor(Instruction.class)
                        .newInstance(instructionObject);
            }catch (Exception e){
                main.getLogManager().warn("Something went wrong creating BetonQuest Event from: " + getEvent());
            }


        }
        return cachedEvent;
    }


    @Override
    public void executeInternally(final Player player, Object... objects) {
        if(getQuestEvent() != null){
            try {
                getQuestEvent().fire(player.getUniqueId().toString());
            } catch (final QuestRuntimeException e) {
                main.getLogManager().warn("Error while firing BetonQuest '" + getEvent().split(" ")[0] + "' event: " + e.getMessage());
            }
        }

    }

    @Override
    public void save(FileConfiguration configuration, String initialPath) {
        configuration.set(initialPath + ".specifics.event", getEvent());
    }

    @Override
    public void load(final FileConfiguration configuration, String initialPath) {
        this.event = configuration.getString(initialPath + ".specifics.event");
    }

    @Override
    public void deserializeFromSingleLineString(ArrayList<String> arguments) {
        this.event = String.join(" ", arguments);
    }


    @Override
    public String getActionDescription(final Player player, final Object... objects) {
        return "Executes Event: " + getEvent();

    }
}
