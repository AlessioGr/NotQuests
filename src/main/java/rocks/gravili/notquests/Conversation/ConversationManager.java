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

package rocks.gravili.notquests.Conversation;

import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import rocks.gravili.notquests.Commands.NotQuestColors;
import rocks.gravili.notquests.NotQuests;
import rocks.gravili.notquests.Structs.QuestPlayer;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;
import java.util.logging.Level;

public class ConversationManager {
    private final NotQuests main;
    private final ArrayList<Conversation> conversations;
    private final Speaker playerSpeaker;

    private final HashMap<UUID, ConversationPlayer> openConversations;

    public ConversationManager(final NotQuests main) {
        this.main = main;
        conversations = new ArrayList<>();

        openConversations = new HashMap<>();

        playerSpeaker = new Speaker("You");
        playerSpeaker.setPlayer(true);


        //playConversation(Bukkit.getPlayer("NoeX"), createTestConversation());
        loadConversationsFromConfig();
    }

    public ConversationPlayer getOpenConversation(final UUID uuid) {
        return openConversations.get(uuid);
    }

    public final HashMap<UUID, ConversationPlayer> getOpenConversations() {
        return openConversations;
    }

    public Conversation createTestConversation() {
        final Conversation testConversation = new Conversation("test", 0);

        final Speaker gustav = new Speaker("Gustav");


        final ConversationLine gustav1 = new ConversationLine(gustav, "Hello, I'm Gustav! What's your name?");

        final ConversationLine player1 = new ConversationLine(playerSpeaker, "I'm player!");
        final ConversationLine player2 = new ConversationLine(playerSpeaker, "None of your business!");

        gustav1.addNext(player1);
        gustav1.addNext(player2);

        final ConversationLine gustav2 = new ConversationLine(gustav, "Nice to meet you!");
        final ConversationLine gustav3 = new ConversationLine(gustav, "Yeah, fuck you!");

        player1.addNext(gustav2);

        player2.addNext(gustav3);

        testConversation.addStarterConversationLine(gustav1);

        return testConversation;
    }


    public void playConversation(final Player player, final Conversation conversation) {
        final QuestPlayer questPlayer = main.getQuestPlayerManager().getOrCreateQuestPlayer(player.getUniqueId());


        if (getOpenConversation(questPlayer.getUUID()) == null) {
            ConversationPlayer conversationPlayer = new ConversationPlayer(main, questPlayer, player, conversation);
            openConversations.put(questPlayer.getUUID(), conversationPlayer);

            conversationPlayer.play();
        } else {
            main.adventure().player(player).sendMessage(
                    MiniMessage.miniMessage().parse(
                            NotQuestColors.errorGradient + "You are already in a conversation!"
                    )
            );
        }

    }


    public void loadConversationsFromConfig() {
        conversations.clear();
        openConversations.clear();

        File conversationsFolder = new File(main.getDataFolder().getPath() + "/conversations/");
        if (!conversationsFolder.exists()) {
            main.getLogManager().log(Level.INFO, "Conversations Folder not found. Creating a new one...");

            if (!conversationsFolder.mkdirs()) {
                main.getLogManager().log(Level.SEVERE, "There was an error creating the NotQuests conversations folder");
                main.getDataManager().disablePluginAndSaving("There was an error creating the NotQuests conversations folder.");
                return;
            }

        }

        for (File conversationFile : main.getUtilManager().listFilesRecursively(conversationsFolder)) {
            main.getLogManager().log(Level.INFO, "Reading conversation file <AQUA>" + conversationFile.getName() + "</AQUA>...");

            final YamlConfiguration conversationConfig = new YamlConfiguration();
            try {
                conversationConfig.load(conversationFile);
            } catch (IOException | InvalidConfigurationException e) {
                e.printStackTrace();
                main.getLogManager().log(Level.WARNING, "Failed reading conversation file <AQUA>" + conversationFile.getName() + "</AQUA>. It's being skipped.");
                continue;
            }


        }
    }
}
