package one.patriik.ccnetworks.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;

import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import one.patriik.ccnetworks.CCNetworks;
import one.patriik.ccnetworks.Registration;
import one.patriik.ccnetworks.block.AbstractNetworkNodeBlock;
import one.patriik.ccnetworks.network.CableNetwork;
import one.patriik.ccnetworks.network.CableNetworkManager;
import one.patriik.ccnetworks.network.CableNetworkNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class NetworkNodeBlockEntity extends AbstractNetworkNodeBlockEntity {
    public NetworkNodeBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }
}
