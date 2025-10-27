package one.patriik.ccnetworks.block;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import one.patriik.ccnetworks.blockentity.NetworkNodeBlockEntity;
import one.patriik.ccnetworks.network.CableNetworkManager;
import one.patriik.ccnetworks.network.CableNetworkNode;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class NetworkNodeBlock extends DirectionalBlock implements EntityBlock {
    public NetworkNodeBlock(Properties properties) {
        super(properties);

        registerDefaultState(defaultBlockState()
                //.setValue(DirectionalBlock.FACING, Direction.UP) //this doesnt work for some reason
        );
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide) {
            if (level.getBlockEntity(pos) instanceof NetworkNodeBlockEntity be) {
                be.load(new CompoundTag());
            }
        }
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new NetworkNodeBlockEntity(pos, state);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof NetworkNodeBlockEntity nodeBE) {
            if (!level.isClientSide) {
                List<CableNetworkNode> connections = nodeBE.getNetworkNode().connections;
                List<BlockPos> connectionPosList = new ArrayList<>();
                for (CableNetworkNode node : connections) {
                    connectionPosList.add(node.pos);
                }

                nodeBE.getCableNetworkManager().removeNode(nodeBE.getNetworkNode());

                for (BlockPos p : connectionPosList) {
                    BlockEntity connectionBE = level.getBlockEntity(p);
                    if (connectionBE instanceof NetworkNodeBlockEntity connectionNodeBE) {
                        connectionNodeBE.update();
                    }
                }

            }
        }

        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
