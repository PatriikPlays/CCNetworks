package one.patriik.ccnetworks.peripherals;

import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.api.peripheral.AttachedComputerSet;
import dan200.computercraft.api.peripheral.IComputerAccess;
import dan200.computercraft.api.peripheral.IPeripheral;
import one.patriik.ccnetworks.CCNetworks;
import one.patriik.ccnetworks.blockentity.NetworkInterfaceNodeBlockEntity;
import one.patriik.ccnetworks.network.CableNetworkNode;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class NetworkInterfaceNodePeripheral implements IPeripheral {
    private final NetworkInterfaceNodeBlockEntity networkNode;
    private final AttachedComputerSet computers = new AttachedComputerSet();
    private volatile CableNetworkNode networkNodeRef;

    public NetworkInterfaceNodePeripheral(NetworkInterfaceNodeBlockEntity networkNode) {
        this.networkNode = networkNode;
    }

    @Override
    public String getType() {
        return "network_node";
    }

    @Override
    public boolean equals(@Nullable IPeripheral other) {
        return other instanceof NetworkInterfaceNodePeripheral node && networkNode == node.networkNode;
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
        computers.forEach(computer -> {
            computer.queueEvent("optic_network_message", computer.getAttachmentName(), data);
        });
    }

    public void setNetworkNode(CableNetworkNode node) {
        networkNodeRef = node;
    }

    @LuaFunction
    public final void send(String data) {
        CableNetworkNode source = networkNodeRef;
        if (source == null) {
            throw new IllegalStateException("Network interface peripheral is not attached to a network node");
        }

        List<CableNetworkNode> destinations = source.interfaceDestinations;
        CompletableFuture.runAsync(() -> {
            for (CableNetworkNode node : destinations) {
                if (node != source && node.peripheral != null) {
                    node.peripheral.receiveMessage(data);
                }
            }
        }).exceptionally(error -> {
            CCNetworks.LOGGER.error("Failed to send optic network message", error);
            return null;
        });
    }
}
