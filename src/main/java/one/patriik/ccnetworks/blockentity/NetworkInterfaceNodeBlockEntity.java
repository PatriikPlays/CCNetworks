package one.patriik.ccnetworks.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import one.patriik.ccnetworks.CCNetworks;
import one.patriik.ccnetworks.peripherals.NetworkInterfaceNodePeripheral;

public class NetworkInterfaceNodeBlockEntity extends AbstractNetworkNodeBlockEntity {
    public NetworkInterfaceNodeBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    protected final NetworkInterfaceNodePeripheral peripheral = new NetworkInterfaceNodePeripheral(this);

    public NetworkInterfaceNodePeripheral getPeripheral() {
        return peripheral;
    }

    @Override
    public void setLevel(net.minecraft.world.level.Level level) {
        super.setLevel(level);
        if (!level.isClientSide()) {
            var node = getNetworkNode();
            if (node == null) {
                CCNetworks.LOGGER.error("Registering interface peripheral at {} without a saved network node", getBlockPos());
                return;
            }
            getCableNetworkManager().registerInterfacePeripheral(node, peripheral);
        }
    }

    public void sendMessageToPeripheral(String data) {
        peripheral.receiveMessage(data);
    }

    @Override
    protected boolean isInterfaceNode() {
        return true;
    }
}
