/*
 * NotQuests - A Questing plugin for Minecraft Servers
 * Copyright (C) $originalComment.match("Copyright \(c\) (\d+)", 1, "-", "$today.year")2021 Alessio Gravili
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

package rocks.gravili.notquests.Managers.Packets;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.factory.bukkit.BukkitPacketEventsBuilder;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerChatMessage;
import net.kyori.adventure.text.Component;
import rocks.gravili.notquests.NotQuests;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;

public class PacketManager {
    private final NotQuests main;
    HashMap<UUID, ArrayList<Component>> chatHistory;

    HashMap<UUID, ArrayList<Component>> conversationChatHistory;

    public PacketManager(final NotQuests main) {
        this.main = main;
        chatHistory = new HashMap<>();
        conversationChatHistory = new HashMap<>();
    }


    public final HashMap<UUID, ArrayList<Component>> getChatHistory() {
        return chatHistory;
    }

    public final HashMap<UUID, ArrayList<Component>> getConversationChatHistory() {
        return conversationChatHistory;
    }

    public void onLoad() {

        PacketEvents.setAPI(BukkitPacketEventsBuilder.build(main));
        PacketEvents.getAPI().load();


    }

    public void initialize() {
        if (main.getDataManager().getConfiguration().packetMagic) {
            WrapperPlayServerChatMessage.HANDLE_JSON = false;
            PacketEvents.getAPI().getEventManager().registerListener(new NQPacketListener(main), PacketListenerPriority.LOW);
            PacketEvents.getAPI().init();


        }

    }

    public void terminate() {
        PacketEvents.getAPI().terminate();
    }
}
