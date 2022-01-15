package rocks.gravili.notquests.paper.managers.packets.ownpacketstuff.modern;

import io.netty.channel.Channel;
import it.unimi.dsi.fastutil.shorts.ShortSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundBlockEventPacket;
import net.minecraft.network.protocol.game.ClientboundSectionBlocksUpdatePacket;
import net.minecraft.network.protocol.game.ServerboundSetBeaconPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.craftbukkit.v1_18_R1.CraftChunk;
import org.bukkit.craftbukkit.v1_18_R1.CraftWorld;
import org.bukkit.craftbukkit.v1_18_R1.block.CraftBlock;
import org.bukkit.craftbukkit.v1_18_R1.block.CraftBlockState;
import org.bukkit.craftbukkit.v1_18_R1.entity.CraftPlayer;
import org.bukkit.entity.Player;
import rocks.gravili.notquests.paper.NotQuests;


public class PacketInjector {
    private final NotQuests main;

    //NMS Mappings
    //private final ServerPlayer serverPlayer;
    /*private final String entityPlayerClass = "level.EntityPlayer";
    private final String PlayerConnectionFieldInEntityPlayer = "b";
    private final String playerConnectionClass = "network.PlayerConnection";
    private final String NetworkManagerClass = "net.minecraft.network.NetworkManager";
    private Field EntityPlayer_playerConnection;
    private Class<?> PlayerConnection;
    private Field PlayerConnection_networkManager;
    private Class<?> NetworkManager;
    private Field channelField;
    private Field packetListenerField;*/
    private boolean packetStuffEnabled = true; //disabled if there is an error

    //Paper


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


    }

    public void addPlayer(Player player) {
        try {

            Channel ch = getChannel(getConnection(getServerPlayer(player).connection));
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

    public void removePlayer(Player player) {
        try {
            Channel ch = getChannel(getConnection(getServerPlayer(player).connection));
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

    public Connection getConnection(ServerGamePacketListenerImpl serverGamePacketListener) {
        return serverGamePacketListener.getConnection();
    }

    public ServerPlayer getServerPlayer(Player player) {
        return ((CraftPlayer)player).getHandle();
    }

    private Channel getChannel(Connection networkManager) {
        Channel ch = networkManager.channel;

        if(ch == null){
            main.getLogManager().warn("Disabling packet stuff because something went wrong...");
            setPacketStuffEnabled(false);
        }
        return ch;
    }


    public void spawnBeaconBeam(Player player, Location location){

        //Prepare Data
        Connection connection = getConnection(getServerPlayer(player).connection);
        location = location.clone();
        BlockPos blockPos = new BlockPos(location.getX(), location.getY(), location.getZ());

        Chunk chunk = location.getChunk();
        CraftChunk craftChunk = (CraftChunk)chunk;
        LevelChunk levelChunk = craftChunk.getHandle();

        World world = location.getWorld();
        CraftWorld craftWorld = (CraftWorld)world;
        ServerLevel serverLevel = craftWorld.getHandle();
        //

        BlockState blockState = location.getBlock().getState();
        blockState.setType(Material.BEACON);



        SectionPos sectionPos = SectionPos.of((int)location.getX(), (int)location.getY(), (int)location.getZ());

        ShortSet positions = ShortSet.of((short)0);

        //PalettedContainer<BlockState> pcB = new PalettedContainer<>();


        //net.minecraft.world.level.block.state.BlockState[] presetBlockStates = serverLevel.chunkPacketBlockController.getPresetBlockStates(world, chunkPos, b0 << 4);

        //PalettedContainer<BlockState> datapaletteblock = new PalettedContainer<>(net.minecraft.world.level.block.Block.BLOCK_STATE_REGISTRY, Blocks.AIR.defaultBlockState(), PalettedContainer.Strategy.SECTION_STATES, presetBlockStates);


        LevelChunkSection section = levelChunk.getHighestSection();





        ClientboundSectionBlocksUpdatePacket clientboundSectionBlocksUpdatePacket = new ClientboundSectionBlocksUpdatePacket(
                sectionPos,
                positions,
                section,
                true);

        main.sendMessage(player, "<main>Sending packet...");

        connection.send(clientboundSectionBlocksUpdatePacket);

        main.sendMessage(player, "<success>Packet sent!");


        //ClientboundBlockUpdatePacket clientboundBlockUpdatePacket = new ClientboundBlockUpdatePacket(blockPos, BlockState.);


    }

   /* public static void sendMultiBlockChange(Player p, Block block) {
        // Creating some blocks to set
        Location loc = p.getLocation().clone();

        // BlockData is just an object that stores a Location, int and byte
        List<BlockData> blocks = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            blocks.add(new BlockData(loc, block.getType().getId() + 1, (byte) 0));
        }

        // Sending packet
        PacketPlayOutMultiBlockChange packet = new PacketPlayOutMultiBlockChange();
        MultiBlockChangeInfo[] info = new MultiBlockChangeInfo[blocks.size()];
        for (int i = 0; i < blocks.size(); i++) {
            int blockId = blocks.get(i).getTypeId();

            byte horizPos = (byte) (blocks.get(i).getLocation().getBlockX() << 4
                    + blocks.get(i).getLocation().getBlockZ());
            byte heigth = (byte) blocks.get(i).getLocation().getY();
            byte id = (byte) (blockId << 4 | (0 & 15));
            short data = (short) (horizPos + heigth + id);

            info[i] = packet.new MultiBlockChangeInfo(data,
                    net.minecraft.server.v1_8_R3.Block.getByCombinedId(blockId));
        }
        setPrivateObject(PacketPlayOutMultiBlockChange.class, packet, "a",
                new ChunkCoordIntPair(p.getLocation().getChunk().getX(), p.getLocation().getChunk().getZ()));
        setPrivateObject(PacketPlayOutMultiBlockChange.class, packet, "b", info);

        getNMSPlayer(p).playerConnection.sendPacket(packet);

        p.sendMessage("Packet sent");
    }*/


    public void sendBeaconUpdatePacket(Player player, Location location, BlockState blockState){
        Connection connection = getConnection(getServerPlayer(player).connection);

        CraftBlockState craftBlockState = (CraftBlockState)blockState;
        net.minecraft.world.level.block.Block nmsBlock = craftBlockState.getHandle().getBlock();






        BlockPos blockPos = new BlockPos(location.getBlockX(), location.getBlockY(), location.getBlockZ());

        player.teleport(location.clone().add(0,1,0));

        //player.sendMessage("nmsBlock: " + nmsBlock.getName().getString());


        ClientboundBlockEventPacket clientboundBlockEventPacket = new ClientboundBlockEventPacket(blockPos, nmsBlock, 1, 1); //BlockPost, Block, Action ID (1=recalculate), Action Type (ignored for beacons)

        //player.sendMessage("sent!");
        //connection.send(clientboundBlockEventPacket);
        connection.send(clientboundBlockEventPacket, (future) -> {
          //  player.sendMessage("Arrived!");
        });


    }


}