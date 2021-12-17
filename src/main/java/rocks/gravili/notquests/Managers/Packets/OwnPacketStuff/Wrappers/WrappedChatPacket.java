package rocks.gravili.notquests.Managers.Packets.OwnPacketStuff.Wrappers;

import net.kyori.adventure.text.Component;
import net.md_5.bungee.api.chat.BaseComponent;
import rocks.gravili.notquests.Managers.Packets.OwnPacketStuff.Reflection;

import java.util.UUID;

public class WrappedChatPacket {
    private final Object packetObject; // https://nms.screamingsandals.org/1.18/net/minecraft/network/protocol/game/ClientboundChatPacket.html
    private final WrappedChatType chatType; //Type: ChatType
    private final UUID sender; //Type: UUID
    private final String json; //Type: UUID
    private Object message; //Type: Component
    private Component adventureComponent;
    private BaseComponent[] spigotComponent;

    public WrappedChatPacket(Object packetObject) {
        this.packetObject = packetObject;

        try {
            //message = Reflection.getFieldValueOfObject(packetObject, "a");
            message = Reflection.getFieldValueOfObject(packetObject, "a");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        try {
            chatType = WrappedChatType.valueOf(((Enum<?>) ((Enum<?>) Reflection.getFieldValueOfObject(packetObject, "b"))).toString());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        try {
            sender = (UUID) Reflection.getFieldValueOfObject(packetObject, "c");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }


        try {//spigot only
            spigotComponent = (BaseComponent[]) Reflection.getFieldValueOfObject(packetObject, "components");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        try {//paper only
            adventureComponent = (Component) Reflection.getFieldValueOfObject(packetObject, "adventure$message");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        if (message != null) {
            try {
                json = (String) Reflection.getMethodValueOfObject(message, "getString");
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        } else {
            json = null;
            //NotQuests.getInstance().getLogManager().debug("Message is null. Fields: " + Arrays.toString(packetObject.getClass().getDeclaredFields()));
        }


    }

    public Object getMessage() { //Type: Component (Vanilla)
        return message;
    }

    public Component getAdventureComponent() { //Type: Component //paper+ only
        return adventureComponent;
    }

    public BaseComponent[] getSpigotComponent() { //Type: BaseComponent //spigot+ only
        return spigotComponent;
    }

    public WrappedChatType getType() { //Type: ChatType
        return chatType;
    }


    public UUID getSender() { //Type: UUID
        return sender;
    }

    public String getChatComponentJson() {
        return json;
    }
}
