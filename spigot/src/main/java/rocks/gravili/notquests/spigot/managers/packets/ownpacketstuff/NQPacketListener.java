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

package rocks.gravili.notquests.spigot.managers.packets.ownpacketstuff;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import net.kyori.adventure.platform.bukkit.MinecraftComponentSerializer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.bungeecord.BungeeComponentSerializer;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import org.bukkit.entity.Player;
import rocks.gravili.notquests.spigot.NotQuests;
import rocks.gravili.notquests.spigot.managers.packets.ownpacketstuff.wrappers.WrappedChatPacket;
import rocks.gravili.notquests.spigot.managers.packets.ownpacketstuff.wrappers.WrappedChatType;

import java.util.ArrayList;
import java.util.Locale;

public class NQPacketListener extends ChannelDuplexHandler {
    private final NotQuests main;
    private final Player player;

    public NQPacketListener(NotQuests main, final Player player) {
        this.main = main;
        this.player = player;
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        super.write(ctx, msg, promise);
        if (!main.getPacketManager().getPacketInjector().isPacketStuffEnabled()) {
            return;
        }
        if (msg.getClass().getSimpleName().toLowerCase(Locale.ROOT).contains("playoutchat")) {
            //main.getLogManager().debug("Sending " + msg.getClass().getSimpleName());

            try {
                final WrappedChatPacket wrappedChatPacket = new WrappedChatPacket(msg, ctx);


                if (wrappedChatPacket.getType() == WrappedChatType.GAME_INFO) { //Skip actionbar messages
                    return;
                }
                main.getLogManager().debug("Valid chat packet! Type: " + wrappedChatPacket.getType().toString());

                handleMainChatHistorySavingLogic(wrappedChatPacket, player);
            } catch (Throwable e) {
                if (main.getConfiguration().debug) {
                    e.printStackTrace();
                }
                //main.getLogManager().warn("Disabling packet stuff because there was an error reading chat messages...");
                //main.getPacketManager().getPacketInjector().setPacketStuffEnabled(false);
            }

        }
    }


    public void handleMainChatHistorySavingLogic(final WrappedChatPacket wrappedChatPacket, final Player player) {
        try {
            Component component = null;
            Object vanillaMessage = wrappedChatPacket.getMessage();
            Object adventureComponent = wrappedChatPacket.getAdventureComponent();
            if (vanillaMessage == null && adventureComponent == null) {
                main.getLogManager().debug("All null :o");
                return;
            }

            if (adventureComponent != null) {
                component = GsonComponentSerializer.gson().deserialize(wrappedChatPacket.getPaperJson());
            }


            if (component == null) { //Spigot shit

                if (vanillaMessage != null) {
                    if (!MinecraftComponentSerializer.isSupported()) {
                        return;
                    }
                    component = MinecraftComponentSerializer.get().deserialize(vanillaMessage);
                    main.getLogManager().debug("vanilla serializer");
                } else {//vanilla shit

                    // component = GsonComponentSerializer.builder().build().deserialize(wrappedChatPacket.getChatComponentJson());
                }
            }


            final ArrayList<Component> convHist = main.getConversationManager().getConversationChatHistory().get(player.getUniqueId());
            if (convHist != null && convHist.contains(component)) {
                return;
            }

            ArrayList<Component> hist = main.getConversationManager().getChatHistory().get(player.getUniqueId());
            if (hist != null) {
                hist.add(component);
            } else {
                hist = new ArrayList<>();
                hist.add(component);
            }

            //main.getLogManager().debug("Registering chat message with Message: " + MiniMessage.builder().build().serialize(component));
            int toRemove = hist.size() - main.getConversationManager().getMaxChatHistory();
            if (toRemove > 0) {
                //main.getLogManager().log(Level.WARNING, "ToRemove: " + i);
                hist.subList(0, toRemove).clear();
            }
            //main.getLogManager().log(Level.WARNING, "After: " + hist.size());


            main.getConversationManager().getChatHistory().put(player.getUniqueId(), hist);


        } catch (Exception e) {
            if (main.getConfiguration().debug) {
                main.getLogManager().warn("Exception reading chat packet: ");
                e.printStackTrace();
            }

        }
        //if (component != null) {
        //main.getLogManager().log(Level.INFO, "E " + LegacyComponentSerializer.legacyAmpersand().serialize(component));
        //}


    }



    /*@Override
    public void channelRead(ChannelHandlerContext c, Object m) throws Exception {
        main.getLogManager().debug("Reading " + m.getClass().getSimpleName());
        if (m.getClass().getSimpleName().equalsIgnoreCase("PacketPlayInResourcePackStatus")) {
            String s = Reflection.getFieldValueOfObject(m, "b").toString();
            if (s.equals("DECLINED")) {
            }
            if (s.equals("FAILED_DOWNLOAD")) {
            }
            if (s.equals("ACCEPTED")) {
            }
            if (s.equals("SUCCESSFULLY_LOADED")) {
                this.player.sendMessage("You have our texture pack installed");
                return;
            }
        } else {
            super.channelRead(c, m);
        }
    }*/
}