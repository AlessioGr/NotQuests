package rocks.gravili.notquestsspigot.managers.packets.ownpacketstuff;

import io.lumine.xikage.mythicmobs.utils.scoreboard.Scoreboard;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

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
            connection.getClass().getMethod("sendPacket", Reflection.getNMSClass("Packet")).invoke(connection, packet);
        }
    }

    public static void sendListPacket(List<String> players, Object packet) {
        try {
            for (String name : players) {
                Object nmsPlayer = getNmsPlayer(Bukkit.getPlayer(name));
                Object connection = nmsPlayer.getClass().getField("playerConnection").get(nmsPlayer);
                connection.getClass().getMethod("sendPacket", Reflection.getNMSClass("Packet")).invoke(connection, packet);
            }
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    public static void sendPlayerPacket(Player p, Object packet) throws Exception {
        Object nmsPlayer = getNmsPlayer(p);
        Object connection = nmsPlayer.getClass().getField("playerConnection").get(nmsPlayer);
        connection.getClass().getMethod("sendPacket", Reflection.getNMSClass("Packet")).invoke(connection, packet);
    }
}