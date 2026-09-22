package one.patriik.ccnetworks.network;

import net.minecraft.core.BlockPos;

import java.util.*;

public class CableNetwork {
    public Map<BlockPos, CableNetworkNode> nodes = new HashMap<>();
    public volatile List<CableNetworkNode> interfaceNodeCache = List.of();
    public UUID uuid;

    public CableNetwork(UUID uuid) {
        this.uuid = uuid;
    }

    public void recomputeInterfaceNodeCache() {
        List<CableNetworkNode> interfaces = new ArrayList<>();
        for (CableNetworkNode node : nodes.values()) {
            if (node.isInterfaceNode) {
                interfaces.add(node);
            }
        }

        List<CableNetworkNode> snapshot = List.copyOf(interfaces);
        interfaceNodeCache = snapshot;
        for (CableNetworkNode node : nodes.values()) {
            node.interfaceDestinations = snapshot;
        }
    }
}
