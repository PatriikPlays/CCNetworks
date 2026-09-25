package one.patriik.ccnetworks.peripherals;

import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.api.peripheral.AttachedComputerSet;
import dan200.computercraft.api.peripheral.IComputerAccess;
import dan200.computercraft.api.peripheral.IPeripheral;
import one.patriik.ccnetworks.blockentity.NetworkInterfaceNodeBlockEntity;
import one.patriik.ccnetworks.network.CableNetworkNode;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class NetworkInterfaceNodePeripheral implements IPeripheral {
    private final NetworkInterfaceNodeBlockEntity networkNode;
    private final AttachedComputerSet computers = new AttachedComputerSet();
    private volatile CableNetworkNode networkNodeRef;

    private final ConcurrentHashMap<Integer, Object> openChannels = new ConcurrentHashMap<>();

    public NetworkInterfaceNodePeripheral(NetworkInterfaceNodeBlockEntity networkNode) {
        this.networkNode = networkNode;
    }

    @Override
    public String getType() {
        return "modem";
    }

    @Override
    public Set<String> getAdditionalTypes() {
        return Set.of("network_node");
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

    public void receiveMessage(int channel, int replyChannel, Object payload) {
        if (openChannels.containsKey(channel)) {
            computers.forEach(computer -> {
                computer.queueEvent("modem_message", computer.getAttachmentName(), channel, replyChannel, payload);
            });
        }
    }

    public void setNetworkNode(CableNetworkNode node) {
        networkNodeRef = node;
    }

    public void clearNetworkNode() {
        networkNodeRef = null;
    }

    private static void validateChannel(int channel) throws LuaException {
        if (channel < 0 || channel > 65535) throw new LuaException("Expected number in range 0-65535");
    }

    @LuaFunction
    public final boolean isWireless() {
        return false;
    }

    @LuaFunction
    public final void open(int channel) throws LuaException {
        validateChannel(channel);

        synchronized (openChannels) {
            if (openChannels.containsKey(channel)) {
                return;
            }

            if (openChannels.size() >= 128) {
                throw new LuaException("Too many open channels");
            }

            openChannels.put(channel, new Object());
        }
    }

    @LuaFunction
    public final void close(int channel) throws LuaException {
        validateChannel(channel);
        synchronized (openChannels) {
            openChannels.remove(channel);
        }
    }

    @LuaFunction
    public final boolean isOpen(int channel) throws LuaException {
        validateChannel(channel);
        return openChannels.containsKey(channel);
    }

    @LuaFunction
    public final void closeAll() {
        synchronized (openChannels) {
            openChannels.clear();
        }
    }

    @LuaFunction
    public final void transmit(int channel, int replyChannel, Object payload) throws LuaException {
        validateChannel(channel);
        validateChannel(replyChannel);

        CableNetworkNode source = networkNodeRef;
        if (source == null) {
            throw new IllegalStateException("Network interface peripheral is not attached to a network node");
        }

        List<CableNetworkNode> destinations = source.interfaceDestinations;
        for (CableNetworkNode node : destinations) {
            if (node == source) continue;

            NetworkInterfaceNodePeripheral destination = node.peripheral;
            if (destination != null) {
                destination.receiveMessage(channel, replyChannel, payload);
            }
        }
    }
}
