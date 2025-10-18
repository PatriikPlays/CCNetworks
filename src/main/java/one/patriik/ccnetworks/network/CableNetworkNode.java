package one.patriik.ccnetworks.network;

import net.minecraft.core.BlockPos;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.*;

public class CableNetworkNode {
    @NonNull public final BlockPos pos;
    public List<CableNetworkNode> connections = new ArrayList<>(4);
    @NonNull public CableNetwork parentNetwork;

    public CableNetworkNode(@NonNull BlockPos pos, @NonNull CableNetwork parentNetwork) {
        this.pos = pos;
        this.parentNetwork = parentNetwork;
    }

    /*
    public void connect(CableNetworkNode other) {
        if (!connections.contains(other)) connections.add(other);
        if (!other.connections.contains(this)) other.connections.add(this);
    }

    public void disconnect(CableNetworkNode other) {
        connections.remove(other);
        other.connections.remove(this);

        Set<CableNetworkNode> reachableFromOther = CableNetworks.listNetworkBFS(other);

        if (!reachableFromOther.contains(this)) {
            CableNetwork newNetwork = new CableNetwork(UUID.randomUUID());

            for (CableNetworkNode newNetworkNode : reachableFromOther) {
                newNetworkNode.switchNetwork(newNetwork);
            }
        }
    }

    public void switchNetwork(@NonNull CableNetwork newNetwork) {
        newNetwork.nodes.put(this.pos, this);
        this.parentNetwork.nodes.remove(this.pos);

        if (this.parentNetwork.getNodes().isEmpty()) {
            CableNetworks.networks.remove(this.parentNetwork.uuid);
        }
        this.parentNetwork = newNetwork;
    }
     */
}
