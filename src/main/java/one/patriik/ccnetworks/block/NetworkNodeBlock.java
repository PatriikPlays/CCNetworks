package one.patriik.ccnetworks.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import one.patriik.ccnetworks.Registration;
import one.patriik.ccnetworks.blockentity.NetworkNodeBlockEntity;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

public class NetworkNodeBlock extends AbstractNetworkNodeBlock {
    public static final MapCodec<NetworkNodeBlock> CODEC = simpleCodec(NetworkNodeBlock::new);

    public NetworkNodeBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected @NonNull MapCodec<? extends NetworkNodeBlock> codec() {
        return CODEC;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new NetworkNodeBlockEntity(Registration.ModBlockEntities.NETWORK_NODE, pos, state);
    }
}
