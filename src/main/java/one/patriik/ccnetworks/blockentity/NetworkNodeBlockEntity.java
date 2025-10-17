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
import net.minecraft.world.level.block.state.BlockState;
import one.patriik.ccnetworks.Registration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class NetworkNodeBlockEntity extends BlockEntity {
    public NetworkNodeBlockEntity(BlockPos pos, BlockState blockState) {
        super(Registration.ModBlockEntities.NETWORK_NODE, pos, blockState);
    }

    final List<BlockPos> links = new ArrayList<>();

    @Override
    public void load(CompoundTag nbt) {
        super.load(nbt);

        links.clear();
        if (nbt.contains("links", CompoundTag.TAG_LIST)) {
            ListTag list = nbt.getList("links", CompoundTag.TAG_INT_ARRAY);
            for (int i = 0; i<list.size(); i++) {
                int[] pos = list.getIntArray(i);
                if (pos.length == 3) {
                    links.add(new BlockPos(pos[0], pos[1], pos[2]));
                }
            }
        }
    }

    @Override
    protected void saveAdditional(CompoundTag nbt) {
        super.saveAdditional(nbt);

        ListTag list = new ListTag();
        for (BlockPos pos : links) {
            int[] arr = new int[]{pos.getX(), pos.getY(), pos.getZ()};
            list.add(new IntArrayTag(arr));
        }
        nbt.put("links", list);
    }

    public List<BlockPos> getLinks() {
        return Collections.unmodifiableList(links);
    }

    public void addLink(BlockPos link) {
        links.add(link);
        this.setChanged();

        Level level = this.getLevel();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(this.getBlockPos(), this.getBlockState(), this.getBlockState(), Block.UPDATE_CLIENTS);
            level.blockEntityChanged(this.getBlockPos());
            ((ServerLevel)level).getChunkSource().blockChanged(this.getBlockPos());
        }
    }

    public boolean removeLink(BlockPos link) {
        boolean result = links.remove(link);
        this.setChanged();

        Level level = this.getLevel();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(this.getBlockPos(), this.getBlockState(), this.getBlockState(), Block.UPDATE_CLIENTS);
            level.blockEntityChanged(this.getBlockPos());
            ((ServerLevel)level).getChunkSource().blockChanged(this.getBlockPos());
        }

        return result;
    }

    public void removeAllLinks() {
        for (BlockPos link : new ArrayList<>(links)) {
            removeLink(link);
        }
    }

    @Override
    public @NotNull CompoundTag getUpdateTag() {
        CompoundTag tag = super.getUpdateTag();
        saveAdditional(tag);
        return tag;
    }

    @Override
    public @Nullable Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    public void cleanupConnectedNodes() {
        if (level == null || level.isClientSide) return;

        for (BlockPos link : links) {
            BlockEntity be = level.getBlockEntity(link);
            if (be instanceof NetworkNodeBlockEntity nodeBE) {
                nodeBE.removeLink(this.getBlockPos());
            }
        }
    }
}
