package one.patriik.ccnetworks.network;

import net.minecraft.core.BlockPos;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.*;

public class CableNetworkNode {
    @NonNull public final BlockPos pos;
    public List<CableNetworkNode> connections = new ArrayList<>(4);
    @NonNull public CableNetwork parentNetwork;
    public
    boolean isInterfaceNode;

    public CableNetworkNode(@NonNull BlockPos pos, @NonNull CableNetwork parentNetwork, boolean isInterfaceNode) {
        this.pos = pos;
        this.parentNetwork = parentNetwork;
        this.isInterfaceNode = isInterfaceNode;
    }
}
