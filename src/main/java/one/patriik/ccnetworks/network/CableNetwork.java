package one.patriik.ccnetworks.network;

import net.minecraft.core.BlockPos;

import java.util.*;

public class CableNetwork {
    public Map<BlockPos, CableNetworkNode> nodes = new HashMap<>();
    public UUID uuid;

    public CableNetwork(UUID uuid) {
        this.uuid = uuid;
    }
}
