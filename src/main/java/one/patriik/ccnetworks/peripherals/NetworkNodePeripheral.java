package one.patriik.ccnetworks.peripherals;

import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.api.peripheral.AttachedComputerSet;
import dan200.computercraft.api.peripheral.IComputerAccess;
import dan200.computercraft.api.peripheral.IPeripheral;
import net.minecraft.world.level.block.entity.BlockEntity;
import one.patriik.ccnetworks.blockentity.NetworkNodeBlockEntity;
import one.patriik.ccnetworks.network.CableNetworkNode;
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

    public void receiveMessage(String data) {
        computers.forEach(computer -> computer.queueEvent("optic_network_message", computer.getAttachmentName(), data));
    }

    @LuaFunction(mainThread = true)
    public final void send(String data) { // FIXME: this seems bad, it checks all nodes, i should implement some sort of end node thing
        for (CableNetworkNode node : networkNode.getNetwork().nodes.values()) {
            if (!node.pos.equals(networkNode.getBlockPos()) && networkNode.getLevel() != null) {
                BlockEntity be = networkNode.getLevel().getBlockEntity(node.pos);
                if (be instanceof NetworkNodeBlockEntity nodeBE) {
                    nodeBE.sendMessageToPeripheral(data);
                }
            }
        }
    }
}
