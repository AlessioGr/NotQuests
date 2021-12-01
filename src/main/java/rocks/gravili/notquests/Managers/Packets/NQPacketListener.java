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

import com.github.retrooper.packetevents.event.PacketListener;
import com.github.retrooper.packetevents.event.impl.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerChatMessage;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextReplacementConfig;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.entity.Player;
import rocks.gravili.notquests.NotQuests;

import java.util.ArrayList;
import java.util.logging.Level;

public class NQPacketListener implements PacketListener {
    private final NotQuests main;

    private final int maxChathistory = 16;


    public NQPacketListener(final NotQuests main) {
        this.main = main;

    }


  /*  private Class<?> getNMSClass(final String className) {
        try {
            return Class.forName("net.minecraft.server." + className);

        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }*/


    public void handleMainChatHistorySavingLogic(final WrapperPlayServerChatMessage wrapperPlayServerChatMessage, final Player player) {

        Component component = null;
        try {
            component = GsonComponentSerializer.builder().build().deserialize(wrapperPlayServerChatMessage.getJSONMessageRaw());

            //component = MinecraftComponentSerializer.get().deserialize(wrapperPlayServerChatMessage.readAnyObject(1));

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

            /*for (final Component comp : hist) {
                main.getLogManager().log(Level.WARNING, "Prev: " + comp);
            }*/

            main.getLogManager().log(Level.WARNING, "Prev: " + hist.size());
            int toRemove = hist.size() - maxChathistory;
            if (toRemove > 0) {
                for (int i = 0; i < toRemove; i++) {
                    main.getLogManager().log(Level.WARNING, "ToRemove: " + i);

                    hist.remove(0);
                }
            }
            main.getLogManager().log(Level.WARNING, "After: " + hist.size());


            main.getPacketManager().getChatHistory().put(player.getUniqueId(), hist);

            /*for (final Component comp : hist) {
                main.getLogManager().log(Level.WARNING, "Aft: " + comp);
            }*/

        } catch (Exception ignored) {
        }
        if (component != null) {
            main.getLogManager().log(Level.INFO, "E " + LegacyComponentSerializer.legacyAmpersand().serialize(component));
        }


        //main.getLogManager().log(Level.INFO, wrapperPlayServerChatMessage.getJSONMessageRaw());
    }


    @Override
    public void onPacketSend(PacketSendEvent event) {
        Player player = (Player) event.getPlayer();
        if (event.getPacketType() == PacketType.Play.Server.CHAT_MESSAGE) {


            WrapperPlayServerChatMessage wrapperPlayServerChatMessage = new WrapperPlayServerChatMessage(event);


            if (wrapperPlayServerChatMessage.getJSONMessageRaw() != null && !wrapperPlayServerChatMessage.getJSONMessageRaw().contains("fg9023zf729ofz")) {

                handleMainChatHistorySavingLogic(wrapperPlayServerChatMessage, player);

            } else if (wrapperPlayServerChatMessage.getJSONMessageRaw() != null && wrapperPlayServerChatMessage.getJSONMessageRaw().contains("fg9023zf729ofz")) {
                main.getLogManager().log(Level.INFO, "replay");
                Component component = null;
                try {
                    component = GsonComponentSerializer.builder().build().deserialize(wrapperPlayServerChatMessage.getJSONMessageRaw());

                    //component = MinecraftComponentSerializer.get().deserialize(wrapperPlayServerChatMessage.readAnyObject(1));
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