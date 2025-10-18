package one.patriik.ccnetworks.network;

import net.minecraft.core.BlockPos;

import java.util.*;

public class CableNetwork {
    public Map<BlockPos, CableNetworkNode> nodes = new HashMap<>();
    public UUID uuid;

    public CableNetwork(UUID uuid) {
        this.uuid = uuid;
    }

    /*public final Map<BlockPos, CableNetworkNode> getNodes() {
        return Collections.unmodifiableMap(nodes);
    }

    public void joinNetwork(UUID networkUUID) {
        if (CableNetworks.networks.containsKey(networkUUID)) {
            for (Map.Entry<BlockPos, CableNetworkNode> entry : CableNetworks.networks.get(networkUUID).getNodes().entrySet()) {
                entry.getValue().switchNetwork(this);
            }
        }
    }*/
}
