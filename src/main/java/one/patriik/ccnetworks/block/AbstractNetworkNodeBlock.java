package one.patriik.ccnetworks.block;

import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import one.patriik.ccnetworks.Registration;
import one.patriik.ccnetworks.blockentity.AbstractNetworkNodeBlockEntity;
import one.patriik.ccnetworks.blockentity.NetworkNodeBlockEntity;
import one.patriik.ccnetworks.network.CableNetworkManager;
import one.patriik.ccnetworks.network.CableNetworkNode;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractNetworkNodeBlock extends DirectionalBlock implements EntityBlock {
    public AbstractNetworkNodeBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState());
    }

    // 🔸 Let subclasses define what BlockEntity type to use
    @Override
    public abstract @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state);

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof AbstractNetworkNodeBlockEntity nodeBE) {
            if (!level.isClientSide()) {
                var connections = nodeBE.getNetworkNode().connections;
                List<BlockPos> connectionPosList = new ArrayList<>();

                for (var node : connections) {
                    connectionPosList.add(node.pos);
                }

                nodeBE.getCableNetworkManager().removeNode(nodeBE.getNetworkNode());

                for (BlockPos p : connectionPosList) {
                    BlockEntity connectionBE = level.getBlockEntity(p);
                    if (connectionBE instanceof AbstractNetworkNodeBlockEntity connectionNodeBE) {
                        connectionNodeBE.update();
                    }
                }
            }
        }

        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}