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

import io.netty.channel.Channel;
import org.bukkit.entity.Player;
import rocks.gravili.notquests.spigot.NotQuests;

import java.lang.reflect.Field;
import java.lang.reflect.Method;


public class PacketInjector {
    private final NotQuests main;
    //NMS Mappings
    private final String entityPlayerClass = "level.EntityPlayer";
    private final String PlayerConnectionFieldInEntityPlayer = "b";
    private final String playerConnectionClass = "network.PlayerConnection";
    private final String NetworkManagerClass = "net.minecraft.network.NetworkManager";
    private Field EntityPlayer_playerConnection;
    private Class<?> PlayerConnection;
    private Field PlayerConnection_networkManager;
    private Class<?> NetworkManager;
    private Field channelField;
    private Field packetListenerField;
    private boolean packetStuffEnabled = true; //disabled if there is an error


    //Paper
    private Object paperGsonComponentSerializer;
    private Class<?> paperComponentClass;

    public final Class<?> getPaperComponentClass() {
        return paperComponentClass;
    }

    public final Object getPaperGsonComponentSerializer() {
        return paperGsonComponentSerializer;
    }

    public PacketInjector(final NotQuests main) {
        this.main = main;
        initializeNMSStuff();
    }

    public final boolean isPacketStuffEnabled() {
        return packetStuffEnabled;
    }

    public void setPacketStuffEnabled(final boolean packetStuffEnabled) {
        this.packetStuffEnabled = packetStuffEnabled;
        main.getConfiguration().packetMagic = false;
        main.getConfiguration().deletePreviousConversations = false;
    }

    public void initializeNMSStuff() {
        try {
            EntityPlayer_playerConnection = Reflection.getField(Reflection.getNMSClass(entityPlayerClass), PlayerConnectionFieldInEntityPlayer); //adj => EntityPlayer //b => playerConnection https://nms.screamingsandals.org/1.18/net/minecraft/server/network/ServerGamePacketListenerImpl.html

            PlayerConnection = Reflection.getNMSClass(playerConnectionClass);
            PlayerConnection_networkManager = Reflection.getField(PlayerConnection, "a"); //a => Connection (NetworkManager)

            NetworkManager = Reflection.getClass(NetworkManagerClass);
            channelField = Reflection.getField(NetworkManager, "k");
            packetListenerField = Reflection.getField(NetworkManager, "m");

        } catch (Throwable t) {
            if (main.getConfiguration().debug) {
                t.printStackTrace();
            }
            main.getLogManager().warn("Disabling packet stuff because something went wrong...");
            setPacketStuffEnabled(false);
        }

        //Paper
        try {
            String nkat = new String(new char[]{'n', 'e', 't', '.', 'k', 'y', 'o', 'r', 'i', '.', 'a', 'd', 'v', 'e', 'n', 't', 'u', 'r', 'e', '.', 't', 'e', 'x', 't'});
            Method gsonMethod = Class.forName(nkat + ".serializer.gson.GsonComponentSerializer").getDeclaredMethod("gson");
            paperGsonComponentSerializer = gsonMethod.invoke(null); //null since static
            paperComponentClass = Class.forName(nkat + ".Component");
            //paperGsonComponentSerializer = gson.getClass().getDeclaredMethod(); Reflection.getMethodValueOfObject(gson, "serialize");

            //paperGsonComponentSerializer = Class.forName("net.kyori.adventure.text.serializer.gson" Reflection.getField("net.kyori.adventure.text.serializer.gson")
        } catch (Exception e) {
            paperGsonComponentSerializer = null;
            if (main.getConfiguration().debug) {
                e.printStackTrace();
            }
        }
    }

    public void addPlayer(Player player) {
        try {
            Channel ch = getChannel(getNetworkManager(Reflection.getNmsPlayer(player)));
            if (ch != null && ch.pipeline().get("PacketInjector") == null) {
                NQPacketListener h = new NQPacketListener(main, player);
                ch.pipeline().addBefore("packet_handler", "PacketInjector", h);
            }
        } catch (Throwable t) {
            if (main.getConfiguration().debug) {
                t.printStackTrace();
            }
            main.getLogManager().warn("Disabling packet stuff because something went wrong...");
            setPacketStuffEnabled(false);
        }
    }

    public void removePlayer(Player p) {
        try {
            Channel ch = getChannel(getNetworkManager(Reflection.getNmsPlayer(p)));
            if (ch != null && ch.pipeline().get("PacketInjector") != null) {
                ch.pipeline().remove("PacketInjector");
            }
        } catch (Throwable t) {
            if (main.getConfiguration().debug) {
                t.printStackTrace();
            }
            main.getLogManager().warn("Disabling packet stuff because something went wrong...");
            setPacketStuffEnabled(false);
        }
    }

    private Object getNetworkManager(Object ep) {
        return Reflection.getFieldValue(PlayerConnection_networkManager, Reflection.getFieldValue(EntityPlayer_playerConnection, ep));
    }

    private Channel getChannel(Object networkManager) {
        Channel ch = null;
        try {
            ch = Reflection.getFieldValue(channelField, networkManager);
        } catch (Exception e) {
            try {
                ch = Reflection.getFieldValue(packetListenerField, networkManager);
            } catch (Exception e2) {
                if (main.getConfiguration().debug) {
                    e2.printStackTrace();
                }
                main.getLogManager().warn("Disabling packet stuff because something went wrong...");
                setPacketStuffEnabled(false);
            }
        }
        return ch;
    }
}