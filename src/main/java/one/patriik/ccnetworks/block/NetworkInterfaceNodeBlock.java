package one.patriik.ccnetworks.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import one.patriik.ccnetworks.Registration;
import one.patriik.ccnetworks.blockentity.NetworkInterfaceNodeBlockEntity;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

public class NetworkInterfaceNodeBlock extends AbstractNetworkNodeBlock {
    public static final MapCodec<NetworkInterfaceNodeBlock> CODEC = simpleCodec(NetworkInterfaceNodeBlock::new);

    public NetworkInterfaceNodeBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected @NonNull MapCodec<? extends NetworkInterfaceNodeBlock> codec() {
        return CODEC;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new NetworkInterfaceNodeBlockEntity(Registration.ModBlockEntities.NETWORK_INTERFACE_NODE, pos, state);
    }
}
