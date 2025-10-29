package one.patriik.ccnetworks.network;

import net.minecraft.core.BlockPos;

import java.util.*;

public class CableNetwork {
    public Map<BlockPos, CableNetworkNode> nodes = new HashMap<>();
    public List<CableNetworkNode> interfaceNodeCache = new ArrayList<>();
    public UUID uuid;

    public CableNetwork(UUID uuid) {
        this.uuid = uuid;
    }

    public void recomputeInterfaceNodeCache() {
        interfaceNodeCache.clear();
        for (CableNetworkNode node : nodes.values()) {
            if (node.isInterfaceNode) {
                interfaceNodeCache.add(node);
            }
        }
    }
}
