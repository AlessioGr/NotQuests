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

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;

public class Reflection {

  public static Class<?> getNMSClass(final String className) {
    try {
      return Class.forName("net.minecraft.server." + className);

    } catch (ClassNotFoundException e) {
      throw new RuntimeException(e);
    }
  }

  public static Class<?> getClass(final String className) {
    try {
      return Class.forName(className);

    } catch (ClassNotFoundException e) {
      throw new RuntimeException(e);
    }
  }

  public static Object getNmsPlayer(Player p) throws Exception {
    Method getHandle = p.getClass().getMethod("getHandle");
    return getHandle.invoke(p);
  }

  public static Object getNmsScoreboard(Scoreboard s) throws Exception {
    Method getHandle = s.getClass().getMethod("getHandle");
    return getHandle.invoke(s);
  }

  public static Object getFieldValueOfObject(Object instance, String fieldName) throws Exception {
    Field field = instance.getClass().getDeclaredField(fieldName);
    field.setAccessible(true);
    return field.get(instance);
  }

  public static Object getMethodValueOfObject(Object instance, String methodName) throws Exception {
    Method method = instance.getClass().getMethod(methodName);
    method.setAccessible(true);
    return method.invoke(instance);
  }

  @SuppressWarnings("unchecked")
  public static <T> T getFieldValue(Field field, Object obj) {
    try {
      return (T) field.get(obj);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public static Field getField(Class<?> clazz, String fieldName) throws Exception {
    Field field = clazz.getDeclaredField(fieldName);
    field.setAccessible(true);
    return field;
  }

  public static void setValue(Object instance, String field, Object value) {
    try {
      Field f = instance.getClass().getDeclaredField(field);
      f.setAccessible(true);
      f.set(instance, value);
    } catch (Throwable t) {
      throw new RuntimeException(t);
    }
  }

  public static void sendAllPacket(Object packet) throws Exception {
    for (Player p : Bukkit.getOnlinePlayers()) {
      Object nmsPlayer = getNmsPlayer(p);
      Object connection = nmsPlayer.getClass().getField("playerConnection").get(nmsPlayer);
      connection
          .getClass()
          .getMethod("sendPacket", Reflection.getNMSClass("Packet"))
          .invoke(connection, packet);
    }
  }

  public static void sendListPacket(List<String> players, Object packet) {
    try {
      for (String name : players) {
        Object nmsPlayer = getNmsPlayer(Bukkit.getPlayer(name));
        Object connection = nmsPlayer.getClass().getField("playerConnection").get(nmsPlayer);
        connection
            .getClass()
            .getMethod("sendPacket", Reflection.getNMSClass("Packet"))
            .invoke(connection, packet);
      }
    } catch (Throwable t) {
      throw new RuntimeException(t);
    }
  }

  public static void sendPlayerPacket(Player p, Object packet) throws Exception {
    Object nmsPlayer = getNmsPlayer(p);
    Object connection = nmsPlayer.getClass().getField("playerConnection").get(nmsPlayer);
    connection
        .getClass()
        .getMethod("sendPacket", Reflection.getNMSClass("Packet"))
        .invoke(connection, packet);
  }
}
