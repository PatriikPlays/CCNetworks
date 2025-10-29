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
import one.patriik.ccnetworks.network.CableNetwork;
import one.patriik.ccnetworks.network.CableNetworkManager;
import one.patriik.ccnetworks.network.CableNetworkNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractNetworkNodeBlockEntity extends BlockEntity {
    public AbstractNetworkNodeBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
    }

    final List<BlockPos> renderLinks = new ArrayList<>();

    @Override
    public void load(CompoundTag nbt) {
        super.load(nbt);

        // this should only be loaded on clients, probably
        renderLinks.clear();
        if (nbt.contains("renderLinks", CompoundTag.TAG_LIST)) {
            ListTag list = nbt.getList("renderLinks", CompoundTag.TAG_INT_ARRAY);
            for (int i = 0; i<list.size(); i++) {
                int[] pos = list.getIntArray(i);
                if (pos.length == 3) {
                    renderLinks.add(new BlockPos(pos[0], pos[1], pos[2]));
                }
            }
        }
    }

    @Override
    public void setLevel(Level level) {
        super.setLevel(level);

        if (!level.isClientSide()) {
            if (this.getNetworkNode() == null) {
                this.getCableNetworkManager().createNode(this.getCableNetworkManager().newNetwork(), this.getBlockPos(), this.isInterfaceNode());
            } else if (this.getNetworkNode().isInterfaceNode != this.isInterfaceNode()) {
                this.getCableNetworkManager().setIsInterfaceNode(this.getNetworkNode(), isInterfaceNode());
            }

            updateLinks();
        }
    }

    protected boolean isInterfaceNode() {
        return false;
    }

    public CableNetworkManager getCableNetworkManager() {
        if (!this.hasLevel()) {
            throw new IllegalStateException("Can't get CableNetworkManager before BlockEntity has level");
        }
        if (!(this.getLevel() instanceof ServerLevel serverLevel)) {
            throw new IllegalStateException("Can't get CableNetworkManager on client");
        }
        return CCNetworks.getCableNetworkManager(serverLevel.getServer(), serverLevel);
    }

    public CableNetworkNode getNetworkNode() {
        return this.getCableNetworkManager().getNodeAt(this.getBlockPos());
    }

    public CableNetwork getNetwork() {
        return getNetworkNode().parentNetwork;
    }

    private void updateLinks() {
        renderLinks.clear();
        for (CableNetworkNode node : this.getNetworkNode().connections) {
            renderLinks.add(node.pos);
        }
    }

    public void update() {
        updateLinks();

        Level level = this.getLevel();
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(this.getBlockPos(), this.getBlockState(), this.getBlockState(), Block.UPDATE_CLIENTS);
            level.blockEntityChanged(this.getBlockPos());
            ((ServerLevel)level).getChunkSource().blockChanged(this.getBlockPos());
        }
    }

    public List<BlockPos> getRenderLinks() {
        return renderLinks;
    }

    @Override
    public @NotNull CompoundTag getUpdateTag() {
        CompoundTag tag = super.getUpdateTag();

        ListTag list = new ListTag();
        for (BlockPos pos : renderLinks) {
            int[] arr = new int[]{pos.getX(), pos.getY(), pos.getZ()};
            list.add(new IntArrayTag(arr));
        }
        tag.put("renderLinks", list);

        return tag;
    }

    @Override
    public @Nullable Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
