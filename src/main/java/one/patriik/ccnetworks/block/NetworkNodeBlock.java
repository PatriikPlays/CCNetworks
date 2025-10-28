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
import one.patriik.ccnetworks.Registration;
import one.patriik.ccnetworks.blockentity.NetworkNodeBlockEntity;
import one.patriik.ccnetworks.network.CableNetworkManager;
import one.patriik.ccnetworks.network.CableNetworkNode;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class NetworkNodeBlock extends AbstractNetworkNodeBlock {
    public NetworkNodeBlock(Properties properties) {
        super(properties);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new NetworkNodeBlockEntity(Registration.ModBlockEntities.NETWORK_NODE, pos, state);
    }
}
