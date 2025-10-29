package one.patriik.ccnetworks.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import one.patriik.ccnetworks.blockentity.AbstractNetworkNodeBlockEntity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractNetworkNodeBlock extends BaseEntityBlock {
    public static final DirectionProperty FACING = DirectionalBlock.FACING;

    public AbstractNetworkNodeBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(FACING, Direction.UP));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getClickedFace());
    }

    @Override
    public abstract @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state);

    @Override
    public @NotNull RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return switch(state.getValue(FACING)) {
            case UP -> Shapes.box(6/16d, 0/16d, 6/16d, 10/16d, 5/16d, 10/16d);
            case DOWN -> Shapes.box(6/16d, 11/16d, 6/16d, 10/16d, 16/16d, 10/16d);
            case NORTH -> Shapes.box(6/16d, 6/16d, 11/16d, 10/16d, 10/16d, 16/16d);
            case SOUTH -> Shapes.box(6/16d, 6/16d, 0/16d, 10/16d, 10/16d, 5/16d);
            case WEST -> Shapes.box(11/16d, 6/16d, 6/16d, 16/16d, 10/16d, 10/16d);
            case EAST -> Shapes.box(0/16d, 6/16d, 6/16d, 5/16d, 10/16d, 10/16d);
        };
    }

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