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

package rocks.gravili.notquests.paper.managers.packets.ownpacketstuff.modern;

import io.netty.channel.Channel;
import it.unimi.dsi.fastutil.shorts.ShortSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.BlockState;

//import org.bukkit.craftbukkit.v1_20_R1.CraftChunk;
//import org.bukkit.craftbukkit.v1_20_R1.CraftWorld;
import org.bukkit.craftbukkit.v1_20_R1.block.CraftBlockState;
import org.bukkit.craftbukkit.v1_20_R1.entity.CraftArmorStand;
import org.bukkit.craftbukkit.v1_20_R1.entity.CraftPlayer;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import rocks.gravili.notquests.paper.NotQuests;

public class PacketInjector {
  private final NotQuests main;

  // NMS Mappings
  // private final ServerPlayer serverPlayer;
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
  private boolean packetStuffEnabled = true; // disabled if there is an error

  // Paper

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

  public void initializeNMSStuff() {}

  public void addPlayer(Player player) {
    try {

      Channel ch = getChannel(getConnection(getServerPlayer(player).connection));
      if (ch != null && ch.pipeline().get("notquests-packetinjector") == null) {
        NQPacketListener h = new NQPacketListener(main, player);
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

  public void removePlayer(Player player) {
    try {
      Channel ch = getChannel(getConnection(getServerPlayer(player).connection));
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

  public Connection getConnection(ServerGamePacketListenerImpl serverGamePacketListener) {
    return serverGamePacketListener.connection;
  }

  public ServerPlayer getServerPlayer(Player player) {
    return ((CraftPlayer) player).getHandle();
  }

  private Channel getChannel(Connection networkManager) {
    Channel ch = networkManager.channel;

    if (ch == null) {
      main.getLogManager().warn("Disabling packet stuff because something went wrong...");
      setPacketStuffEnabled(false);
    }
    return ch;
  }

  public void spawnBeaconBeam(Player player, Location location) {
/*
    // Prepare Data
    Connection connection = getConnection(getServerPlayer(player).connection);
    location = location.clone();
    BlockPos blockPos = new BlockPos(location.getBlockX(), location.getBlockY(), location.getBlockZ());

    Chunk chunk = location.getChunk();
    CraftChunk craftChunk = (CraftChunk) chunk;

    World world = location.getWorld();
    CraftWorld craftWorld = (CraftWorld) world;
    ServerLevel serverLevel = craftWorld.getHandle();
    //

    BlockState blockState = location.getBlock().getState();
    blockState.setType(Material.BEACON);

    SectionPos sectionPos =
        SectionPos.of((int) location.getX(), (int) location.getY(), (int) location.getZ());

    ShortSet positions = ShortSet.of((short) 0);

    // PalettedContainer<BlockState> pcB = new PalettedContainer<>();

    // net.minecraft.world.level.block.state.BlockState[] presetBlockStates =
    // serverLevel.chunkPacketBlockController.getPresetBlockStates(world, chunkPos, b0 << 4);

    // PalettedContainer<BlockState> datapaletteblock = new
    // PalettedContainer<>(net.minecraft.world.level.block.Block.BLOCK_STATE_REGISTRY,
    // Blocks.AIR.defaultBlockState(), PalettedContainer.Strategy.SECTION_STATES,
    // presetBlockStates);

    LevelChunkSection section = craftChunk.getHandle(ChunkStatus.FULL).getHighestSection();

    ClientboundSectionBlocksUpdatePacket clientboundSectionBlocksUpdatePacket =
        new ClientboundSectionBlocksUpdatePacket(sectionPos, positions, section, true);

    main.sendMessage(player, "<main>Sending packet...");

    connection.send(clientboundSectionBlocksUpdatePacket);

    main.sendMessage(player, "<success>Packet sent!");

    // ClientboundBlockUpdatePacket clientboundBlockUpdatePacket = new
    // ClientboundBlockUpdatePacket(blockPos, BlockState.);*/

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

  public void sendBeaconUpdatePacket(Player player, Location location, BlockState blockState) {
    Connection connection = getConnection(getServerPlayer(player).connection);

    CraftBlockState craftBlockState = (CraftBlockState) blockState;
    net.minecraft.world.level.block.Block nmsBlock = craftBlockState.getHandle().getBlock();

    BlockPos blockPos =
        new BlockPos(location.getBlockX(), location.getBlockY(), location.getBlockZ());

    player.teleport(location.clone().add(0, 1, 0));

    // player.sendMessage("nmsBlock: " + nmsBlock.getName().getString());

    ClientboundBlockEventPacket clientboundBlockEventPacket =
        new ClientboundBlockEventPacket(
            blockPos, nmsBlock, 1,
            1); // BlockPost, Block, Action ID (1=recalculate), Action Type (ignored for beacons)

    // player.sendMessage("sent!");
    // connection.send(clientboundBlockEventPacket);
    connection.send(
        clientboundBlockEventPacket
        );

  }
  public void sendHolo(Player player,ArmorStand armorStand,boolean show) {
    Connection connection = getConnection(getServerPlayer(player).connection);
    net.minecraft.world.entity.decoration.ArmorStand a=((CraftArmorStand)armorStand).getHandle();
    a.setCustomNameVisible(show);

    // ClientboundSetEntityDataPacket dataPck=new ClientboundSetEntityDataPacket(a.getId(),a.getEntityData(),true); //TODO: Fix. What the hell is a FriendlyByteBuf

    // connection.send(dataPck);
  }

}
