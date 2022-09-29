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

package rocks.gravili.notquests.paper.managers.packets.packetevents;

import com.github.retrooper.packetevents.event.PacketListener;
import com.github.retrooper.packetevents.event.impl.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerChatMessage;
import java.util.ArrayList;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextReplacementConfig;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import org.bukkit.entity.Player;
import rocks.gravili.notquests.paper.NotQuests;

public class PacketEventsPacketListener implements PacketListener {
  private final NotQuests main;

  public PacketEventsPacketListener(final NotQuests main) {
    this.main = main;
  }

  /*  private Class<?> getNMSClass(final String className) {
      try {
          return Class.forName("net.minecraft.server." + className);

      } catch (ClassNotFoundException e) {
          throw new RuntimeException(e);
      }
  }*/

  public void handleMainChatHistorySavingLogic(
      final WrapperPlayServerChatMessage wrapperPlayServerChatMessage, final Player player) {
    Component component;
    try {
      component =
          GsonComponentSerializer.builder()
              .build()
              .deserialize(wrapperPlayServerChatMessage.getChatComponentJson());

      final ArrayList<Component> convHist =
          main.getConversationManager().getConversationChatHistory().get(player.getUniqueId());
      if (convHist != null && convHist.contains(component)) {
        return;
      }

      ArrayList<Component> hist =
          main.getConversationManager().getChatHistory().get(player.getUniqueId());
      if (hist != null) {
        hist.add(component);
      } else {
        hist = new ArrayList<>();
        hist.add(component);
      }

      /*main.getLogManager()
          .debug(
              "Registering chat message with position: "
                  + wrapperPlayServerChatMessage.getPosition()
                  + " and packet ID: "
                  + wrapperPlayServerChatMessage.getPacketId()
                  + ". Message: "
                  + MiniMessage.builder().build().serialize(component));*/
      int toRemove = hist.size() - main.getConversationManager().getMaxChatHistory();
      if (toRemove > 0) {
        // main.getLogManager().log(Level.WARNING, "ToRemove: " + i);
        hist.subList(0, toRemove).clear();
      }
      // main.getLogManager().log(Level.WARNING, "After: " + hist.size());

      main.getConversationManager().getChatHistory().put(player.getUniqueId(), hist);

    } catch (Exception ignored) {

    }
    // if (component != null) {
    // main.getLogManager().log(Level.INFO, "E " +
    // LegacyComponentSerializer.legacyAmpersand().serialize(component));
    // }

  }

  @Override
  public void onPacketSend(PacketSendEvent event) {
    if (event.getPacketType() == PacketType.Play.Server.CHAT_MESSAGE) {

      WrapperPlayServerChatMessage wrapperPlayServerChatMessage =
          new WrapperPlayServerChatMessage(event);

      if (wrapperPlayServerChatMessage.getPosition()
          == WrapperPlayServerChatMessage.ChatPosition.GAME_INFO) { // Skip actionbar messages
        return;
      }

      if (wrapperPlayServerChatMessage.getChatComponentJson() != null
          && !wrapperPlayServerChatMessage.getChatComponentJson().contains("fg9023zf729ofz")) {
        Player player = (Player) event.getPlayer();

        handleMainChatHistorySavingLogic(wrapperPlayServerChatMessage, player);

      } else if (wrapperPlayServerChatMessage.getChatComponentJson() != null
          && wrapperPlayServerChatMessage.getChatComponentJson().contains("fg9023zf729ofz")) {
        // main.getLogManager().log(Level.INFO, "replay");
        Component component;
        try {
          component =
              GsonComponentSerializer.builder()
                  .build()
                  .deserialize(wrapperPlayServerChatMessage.getChatComponentJson());

          component =
              component.replaceText(
                  TextReplacementConfig.builder()
                      .match("fg9023zf729ofz")
                      .replacement(Component.text(""))
                      .build());

        } catch (Exception ignored) {
        }
      }
    }
  }
}
