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

import io.github.retrooper.packetevents.event.PacketListenerAbstract;
import io.github.retrooper.packetevents.event.PacketListenerPriority;
import io.github.retrooper.packetevents.event.impl.PacketPlaySendEvent;
import io.github.retrooper.packetevents.packettype.PacketType;
import io.github.retrooper.packetevents.packetwrappers.play.out.chat.WrappedPacketOutChat;
import net.kyori.adventure.platform.bukkit.MinecraftComponentSerializer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextReplacementConfig;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.entity.Player;
import rocks.gravili.notquests.NotQuests;

import java.util.ArrayList;
import java.util.logging.Level;

public class PacketListener extends PacketListenerAbstract {
    private final NotQuests main;

    private final int maxChathistory = 15;


    public PacketListener(final NotQuests main) {
        super(PacketListenerPriority.LOW);
        this.main = main;

    }


  /*  private Class<?> getNMSClass(final String className) {
        try {
            return Class.forName("net.minecraft.server." + className);

        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }*/


    public void handleMainChatHistorySavingLogic(final WrappedPacketOutChat wrappedPacketOutChat, final Player player) {

        Component component = null;
        try {
            component = MinecraftComponentSerializer.get().deserialize(wrappedPacketOutChat.readAnyObject(1));

            final ArrayList<Component> convHist = main.getPacketManager().getConversationChatHistory().get(player.getUniqueId());
            if (convHist != null && convHist.contains(component)) {
                return;
            }

            ArrayList<Component> hist = main.getPacketManager().getChatHistory().get(player.getUniqueId());
            if (hist != null) {
                hist.add(component);
            } else {
                hist = new ArrayList<>();
                hist.add(component);
            }

            for (final Component comp : hist) {
                main.getLogManager().log(Level.WARNING, "Prev: " + comp);
            }


            int toRemove = maxChathistory - hist.size();
            if (toRemove > 0) {
                for (int i = 0; i < toRemove; i++) {
                    main.getLogManager().log(Level.WARNING, "ToRemove: " + i);

                    hist.remove(0);
                }

            }

            main.getPacketManager().getChatHistory().put(player.getUniqueId(), hist);

            for (final Component comp : hist) {
                main.getLogManager().log(Level.WARNING, "Aft: " + comp);
            }


        } catch (Exception ignored) {
        }
        if (component != null) {
            main.getLogManager().log(Level.INFO, "E " + LegacyComponentSerializer.legacyAmpersand().serialize(component));

        }


        main.getLogManager().log(Level.INFO, wrappedPacketOutChat.getMessage());
    }


    @Override
    public void onPacketPlaySend(PacketPlaySendEvent event) {
        Player player = event.getPlayer();
        if (event.getPacketId() == PacketType.Play.Server.CHAT) {
            WrappedPacketOutChat wrappedPacketOutChat = new WrappedPacketOutChat(event.getNMSPacket());

            if (wrappedPacketOutChat.getMessage() != null && !wrappedPacketOutChat.getMessage().contains("fg9023zf729ofz")) {

                handleMainChatHistorySavingLogic(wrappedPacketOutChat, player);

            } else if (wrappedPacketOutChat.getMessage() != null && wrappedPacketOutChat.getMessage().contains("fg9023zf729ofz")) {
                main.getLogManager().log(Level.INFO, "replay");
                Component component = null;
                try {
                    component = MinecraftComponentSerializer.get().deserialize(wrappedPacketOutChat.readAnyObject(1));
                    component.replaceText(TextReplacementConfig.builder()
                            .match("fg9023zf729ofz").replacement(Component.text("")).build());
                    //wrappedPacketOutChat.writeAnyObject(1, MinecraftComponentSerializer.get().serialize(component));

                    //event.setNMSPacket(wrappedPacketOutChat);
                } catch (Exception ignored) {
                    // e.printStackTrace();
                }
            }


            //1: int
            //2: ChatComponentText

            try {
                //main.getLogManager().log(Level.INFO, "Class: " + wrappedPacketOutChat.readAnyObject(1).getClass().getName());
            } catch (Exception ignored) {
                // e.printStackTrace();

            }
        }
    }
}