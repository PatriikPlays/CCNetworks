package one.patriik.ccnetworks.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import one.patriik.ccnetworks.blockentity.NetworkNodeBlockEntity;
import org.jetbrains.annotations.Nullable;

public class NetworkNodeBlock extends DirectionalBlock implements EntityBlock {
    public NetworkNodeBlock(Properties properties) {
        super(properties);

        registerDefaultState(defaultBlockState()
                //.setValue(DirectionalBlock.FACING, Direction.UP) //this doesnt work for some reason
        );
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new NetworkNodeBlockEntity(pos, state);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof NetworkNodeBlockEntity nodeBE) {
            for (BlockPos link : nodeBE.getLinks()) {
                BlockEntity linkBE = level.getBlockEntity(link);
                if (linkBE instanceof NetworkNodeBlockEntity linkNodeBE) {
                    linkNodeBE.removeLink(pos);
                }
            }

            nodeBE.removeAllLinks(); // i think this is needed in case it gets moved by piston
        }

        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
