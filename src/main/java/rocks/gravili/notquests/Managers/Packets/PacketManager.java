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

import io.github.retrooper.packetevents.PacketEvents;
import io.github.retrooper.packetevents.settings.PacketEventsSettings;
import io.github.retrooper.packetevents.utils.server.ServerVersion;
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
        PacketEvents.create(main);
        PacketEventsSettings settings = PacketEvents.get().getSettings();
        settings
                .fallbackServerVersion(ServerVersion.v_1_17_1)
                .compatInjector(false)
                .checkForUpdates(false)
                .bStats(false);
        PacketEvents.get().loadAsyncNewThread();
    }

    public void initialize() {
        if (main.getDataManager().getConfiguration().packetMagic) {
            PacketEvents.get().registerListener(new PacketListener(main));
            PacketEvents.get().init();
        }

    }

    public void terminate() {
        PacketEvents.get().terminate();
    }
}
