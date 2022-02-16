package rocks.gravili.notquests.paper.managers.packets.ownpacketstuff.modern;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.papermc.paper.adventure.PaperAdventure;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.bungeecord.BungeeComponentSerializer;
import net.md_5.bungee.api.chat.BaseComponent;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.protocol.game.ClientboundChatPacket;
import net.minecraft.network.protocol.game.ClientboundSectionBlocksUpdatePacket;
import org.bukkit.entity.Player;
import rocks.gravili.notquests.paper.NotQuests;

import java.util.ArrayList;

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
        if (!main.getPacketManager().getModernPacketInjector().isPacketStuffEnabled()) {
            return;
        }
        if (msg instanceof ClientboundChatPacket clientboundChatPacket) {
            //main.getLogManager().debug("Sending " + msg.getClass().getSimpleName());

            try {

                if (clientboundChatPacket.getType() == ChatType.GAME_INFO) { //Skip actionbar messages
                    return;
                }
                main.getLogManager().debug("Valid chat packet! Type: " + clientboundChatPacket.getType().toString());

                handleMainChatHistorySavingLogic(clientboundChatPacket, player);
            } catch (Exception e) {
                if (main.getConfiguration().debug) {
                    e.printStackTrace();
                }
                //main.getLogManager().warn("Disabling packet stuff because there was an error reading chat messages...");
                //main.getPacketManager().getPacketInjector().setPacketStuffEnabled(false);
            }

        }else if (msg instanceof ClientboundSectionBlocksUpdatePacket) {
            //player.sendMessage("ClientboundSectionBlocksUpdatePacket");

        }
    }


    public void handleMainChatHistorySavingLogic(final ClientboundChatPacket clientboundChatPacket, final Player player) {
        try {
            net.minecraft.network.chat.Component vanillaMessage = clientboundChatPacket.getMessage();
            BaseComponent[] spigotComponent = clientboundChatPacket.components;
            Component adventureComponent = clientboundChatPacket.adventure$message;


            if (vanillaMessage == null && spigotComponent == null && adventureComponent == null) {
                main.getLogManager().debug("All null :o");
                return;
            }


            if (adventureComponent == null) { //Spigot shit

                if (spigotComponent != null) {
                    adventureComponent = BungeeComponentSerializer.get().deserialize(spigotComponent);

                } else {//vanilla shit
                    try {//paper only
                        adventureComponent = PaperAdventure.asAdventure(vanillaMessage);

                        main.getLogManager().debug("vanilla serializer: " + adventureComponent.getClass().toString());
                    } catch (Exception e) {
                        if (main.getConfiguration().debug) {
                            e.printStackTrace();
                        }
                    }

                }
            }


            final ArrayList<Component> convHist = main.getConversationManager().getConversationChatHistory().get(player.getUniqueId());
            if (convHist != null && convHist.contains(adventureComponent)) {
                return;
            }

            ArrayList<Component> hist = main.getConversationManager().getChatHistory().get(player.getUniqueId());
            if (hist != null) {
                hist.add(adventureComponent);
            } else {
                hist = new ArrayList<>();
                hist.add(adventureComponent);
            }

            main.getLogManager().debug("Registering chat message with Message: " + MiniMessage.builder().build().serialize(adventureComponent));
            final int toRemove = hist.size() - main.getConversationManager().getMaxChatHistory();
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