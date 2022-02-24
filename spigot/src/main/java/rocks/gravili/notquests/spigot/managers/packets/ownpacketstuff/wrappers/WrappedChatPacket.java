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

package rocks.gravili.notquests.spigot.managers.packets.ownpacketstuff.wrappers;

import io.netty.channel.ChannelHandlerContext;
import net.md_5.bungee.api.chat.BaseComponent;
import rocks.gravili.notquests.spigot.managers.packets.ownpacketstuff.Reflection;

import java.util.UUID;

;

public class WrappedChatPacket {
    private final Object packetObject; // https://nms.screamingsandals.org/1.18/net/minecraft/network/protocol/game/ClientboundChatPacket.html
    private final WrappedChatType chatType; //Type: ChatType
    private final UUID sender; //Type: UUID
    private final String json; //Type: UUID
    private Object message; //Type: Component
    private Object adventureComponent;
    private BaseComponent[] spigotComponent;
    private String paperJson;

    //private final ByteBuf byteBuf;

    public WrappedChatPacket(Object packetObject, ChannelHandlerContext ctx) {
        this.packetObject = packetObject;
        //byteBuf = ctx.alloc().buffer();
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

        //System.out.println("READ: " + readString());

        /*try {//paper only
            if (NotQuests.getInstance().getPacketManager().getPacketInjector().getPaperGsonComponentSerializer() != null) {
                adventureComponent = Reflection.getFieldValueOfObject(packetObject, "adventure$message");

                Method method = NotQuests.getInstance().getPacketManager().getPacketInjector().getPaperGsonComponentSerializer().getClass().getMethod("serialize", NotQuests.getInstance().getPacketManager().getPacketInjector().getPaperComponentClass());
                method.setAccessible(true);
                paperJson = (String) method.invoke(NotQuests.getInstance().getPacketManager().getPacketInjector().getPaperGsonComponentSerializer(), adventureComponent);
                //NotQuests.getInstance().getLogManager().debug("Paper json: " + paperJson);


            } else {
                //NotQuests.getInstance().getLogManager().debug("Null gson serializer :(");
            }
        } catch (Exception e) {
            if (NotQuests.getInstance().getConfiguration().debug) {
                throw new RuntimeException(e);

            }
        }**/ //Commented out cuz useless on spigot servers?

    }

    public String getPaperJson() {
        return paperJson;
    }

    /*public String readString() {
        int j = readVarInt();
        if (j > 262144 * 4) {
            throw new RuntimeException("The received encoded string buffer length is longer than maximum allowed (" + j + " > " + 262144 * 4 + ")");
        } else if (j < 0) {
            throw new RuntimeException("The received encoded string buffer length is less than zero! Weird string!");
        } else {
            System.out.println("buffer: " + Arrays.toString(Arrays.copyOf(byteBuf.array(), byteBuf.readableBytes())));

            String s = byteBuf.toString(byteBuf.readerIndex(), j, StandardCharsets.UTF_8);
            byteBuf.readerIndex(byteBuf.readerIndex() + j);
            if (s.length() > 262144) {
                throw new RuntimeException("The received string length is longer than maximum allowed (" + j + " > " + 262144 + ")");
            } else {
                return s;
            }
        }
    }
    public int readVarInt() {
        byte b0;
        int i = 0;
        int j = 0;
        do {
            b0 = byteBuf.readByte();
            i |= (b0 & Byte.MAX_VALUE) << j++ * 7;
            if (j > 5)
                throw new RuntimeException("VarInt too big");
        } while ((b0 & 0x80) == 128);
        return i;
    }*/

    public Object getMessage() { //Type: Component (Vanilla)
        return message;
    }

    public Object getAdventureComponent() { //Type: Component //paper+ only
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
