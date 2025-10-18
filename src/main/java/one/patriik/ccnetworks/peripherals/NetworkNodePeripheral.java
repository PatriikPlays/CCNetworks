package one.patriik.ccnetworks.peripherals;

import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.api.peripheral.AttachedComputerSet;
import dan200.computercraft.api.peripheral.IComputerAccess;
import dan200.computercraft.api.peripheral.IPeripheral;
import one.patriik.ccnetworks.blockentity.NetworkNodeBlockEntity;
import org.jspecify.annotations.Nullable;

public class NetworkNodePeripheral implements IPeripheral {
    private final NetworkNodeBlockEntity networkNode;
    private final AttachedComputerSet computers = new AttachedComputerSet();

    public NetworkNodePeripheral(NetworkNodeBlockEntity networkNode) {
        this.networkNode = networkNode;
    }

    @Override
    public String getType() {
        return "network_node";
    }

    @Override
    public boolean equals(@Nullable IPeripheral other) {
        return other instanceof NetworkNodePeripheral node && networkNode == node.networkNode;
    }

    @Override
    public void attach(IComputerAccess computer) {
        computers.add(computer);
    }

    @Override
    public void detach(IComputerAccess computer) {
        computers.remove(computer);
    }

    @LuaFunction(mainThread = true)
    public final void send(int channel, String data) {

    }
}
