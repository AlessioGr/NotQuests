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

package rocks.gravili.notquests.paper.managers.packets.ownpacketstuff.reflection;

import io.netty.channel.Channel;
import java.lang.reflect.Field;
import org.bukkit.entity.Player;
import rocks.gravili.notquests.paper.NotQuests;

public class ReflectionPacketInjector {
  private final NotQuests main;
  // NMS Mappings
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
  private boolean packetStuffEnabled = true; // disabled if there is an error

  // Paper
  private Object paperMinecraftSerializer;
  private Class<?> paperAdventureClass;

  public ReflectionPacketInjector(final NotQuests main) {
    this.main = main;
    initializeNMSStuff();
  }

  public final Class<?> getPaperAdventureClass() {
    return paperAdventureClass;
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
      EntityPlayer_playerConnection =
          Reflection.getField(
              Reflection.getNMSClass(entityPlayerClass),
              PlayerConnectionFieldInEntityPlayer); // adj => EntityPlayer //b => playerConnection
                                                    // https://nms.screamingsandals.org/1.18/net/minecraft/server/network/ServerGamePacketListenerImpl.html

      PlayerConnection = Reflection.getNMSClass(playerConnectionClass);
      PlayerConnection_networkManager =
          Reflection.getField(PlayerConnection, "a"); // a => Connection (NetworkManager)

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

    // Paper
    try {
      // Method gsonMethod = Class.forName(nkat +
      // ".serializer.gson.GsonComponentSerializer").getDeclaredMethod("gson");
      paperAdventureClass = Class.forName("io.papermc.paper.adventure.PaperAdventure");
      // paperGsonComponentSerializer = gson.getClass().getDeclaredMethod();
      // Reflection.getMethodValueOfObject(gson, "serialize");

      // paperGsonComponentSerializer = Class.forName("net.kyori.adventure.text.serializer.gson"
      // Reflection.getField("net.kyori.adventure.text.serializer.gson")
    } catch (Exception e) {
      paperAdventureClass = null;
      if (main.getConfiguration().debug) {
        e.printStackTrace();
      }
    }
  }

  public void addPlayer(Player player) {
    try {
      Channel ch = getChannel(getNetworkManager(Reflection.getNmsPlayer(player)));
      if (ch != null && ch.pipeline().get("notquests-packetinjector") == null) {
        ReflectionNQPacketListener h = new ReflectionNQPacketListener(main, player);
        ch.pipeline().addBefore("packet_handler", "notquests-packetinjector", h);
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
      if (ch != null && ch.pipeline().get("notquests-packetinjector") != null) {
        ch.pipeline().remove("notquests-packetinjector");
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
    return Reflection.getFieldValue(
        PlayerConnection_networkManager,
        Reflection.getFieldValue(EntityPlayer_playerConnection, ep));
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
