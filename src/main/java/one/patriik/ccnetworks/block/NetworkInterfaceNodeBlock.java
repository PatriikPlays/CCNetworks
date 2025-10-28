package one.patriik.ccnetworks.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import one.patriik.ccnetworks.Registration;
import one.patriik.ccnetworks.blockentity.NetworkInterfaceNodeBlockEntity;
import one.patriik.ccnetworks.blockentity.NetworkNodeBlockEntity;
import org.jetbrains.annotations.Nullable;

public class NetworkInterfaceNodeBlock extends AbstractNetworkNodeBlock {
    public NetworkInterfaceNodeBlock(Properties properties) {
        super(properties);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new NetworkInterfaceNodeBlockEntity(Registration.ModBlockEntities.NETWORK_INTERFACE_NODE, pos, state);
    }
}
