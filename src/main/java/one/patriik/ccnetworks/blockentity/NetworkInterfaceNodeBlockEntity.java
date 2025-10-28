package one.patriik.ccnetworks.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import one.patriik.ccnetworks.Registration;
import one.patriik.ccnetworks.peripherals.NetworkInterfaceNodePeripheral;

public class NetworkInterfaceNodeBlockEntity extends AbstractNetworkNodeBlockEntity {
    public NetworkInterfaceNodeBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    protected final NetworkInterfaceNodePeripheral peripheral = new NetworkInterfaceNodePeripheral(this);

    public NetworkInterfaceNodePeripheral getPeripheral() {
        return peripheral;
    }

    public void sendMessageToPeripheral(String data) {
        peripheral.receiveMessage(data);
    }
}
