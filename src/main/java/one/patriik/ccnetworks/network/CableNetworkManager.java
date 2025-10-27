package one.patriik.ccnetworks.network;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongArrayTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import one.patriik.ccnetworks.CCNetworks;

import java.util.*;

public class CableNetworkManager {
    private final Map<UUID, CableNetwork> networks = new HashMap<>();
    private final Map<BlockPos, CableNetworkNode> networkNodeMap = new HashMap<>();

    private CableNetworkWorldSavedData savedData;

    public CableNetworkManager(MinecraftServer server, ServerLevel level) {
        this.savedData = CableNetworkWorldSavedData.getDimensionSavedData(server, level.dimension());

        CompoundTag tag = savedData.getData();
        if (!tag.contains("networks")) return;

        ListTag networkList = tag.getList("networks", Tag.TAG_COMPOUND);

        for (int i = 0; i < networkList.size(); i++) {
            CompoundTag netTag = networkList.getCompound(i);
            UUID networkUUID = netTag.getUUID("uuid");

            CableNetwork network = new CableNetwork(networkUUID);
            networks.put(networkUUID, network);

            if (!netTag.contains("nodes")) continue;

            ListTag nodesList = netTag.getList("nodes", Tag.TAG_COMPOUND);

            for (int j = 0; j < nodesList.size(); j++) {
                CompoundTag nodeTag = nodesList.getCompound(j);
                BlockPos pos = BlockPos.of(nodeTag.getLong("pos"));

                CableNetworkNode node = new CableNetworkNode(pos, network);
                network.nodes.put(pos, node);
                networkNodeMap.put(pos, node);
            }

            // restore connections
            for (int j = 0; j < nodesList.size(); j++) {
                CompoundTag nodeTag = nodesList.getCompound(j);
                BlockPos pos = BlockPos.of(nodeTag.getLong("pos"));
                CableNetworkNode node = network.nodes.get(pos);

                if (!nodeTag.contains("connections")) continue;

                long[] connectionPosArray = nodeTag.getLongArray("connections");
                for (long l : connectionPosArray) {
                    BlockPos connectionPos = BlockPos.of(l);
                    CableNetworkNode connectedNode = network.nodes.get(connectionPos);
                    if (connectedNode != null) {
                        node.connections.add(connectedNode);
                    }
                }
            }
        }
    }

    private void saveToSavedData() {
        CompoundTag tag = new CompoundTag();
        ListTag networkList = new ListTag();

        for (CableNetwork network : networks.values()) {
            CompoundTag netTag = new CompoundTag();
            netTag.putUUID("uuid", network.uuid);

            ListTag nodesList = new ListTag();
            for (CableNetworkNode node : network.nodes.values()) {
                CompoundTag nodeTag = new CompoundTag();
                nodeTag.putLong("pos", node.pos.asLong());

                List<Long> connections = new ArrayList<>();
                for (CableNetworkNode connection : node.connections) {
                    connections.add(connection.pos.asLong());
                }
                nodeTag.put("connections", new LongArrayTag(connections));

                nodesList.add(nodeTag);
            }
            netTag.put("nodes", nodesList);
            networkList.add(netTag);
        }

        tag.put("networks", networkList);
        savedData.setData(tag); // marks it dirty automatically
    }

    public CableNetworkNode getNodeAt(BlockPos pos) {
        return networkNodeMap.get(pos);
    }

    public CableNetwork newNetwork() {
        UUID uuid = UUID.randomUUID();
        CableNetwork network = new CableNetwork(uuid);

        networks.put(uuid, network);

        saveToSavedData();

        return network;
    }

    public CableNetworkNode createNode(CableNetwork network, BlockPos position) {
        CableNetworkNode node = new CableNetworkNode(position, network);
        network.nodes.put(position, node);
        networkNodeMap.put(position, node);

        saveToSavedData();

        return node;
    }

    public void connectNodes(CableNetworkNode nodeA, CableNetworkNode nodeB) {
        if (nodeA.pos.equals(nodeB.pos)) {
            CCNetworks.LOGGER.warn("Tried to connect node to itself");
        }

        if (nodeA.parentNetwork.uuid != nodeB.parentNetwork.uuid) {
            joinNetwork(nodeA.parentNetwork, nodeB.parentNetwork);
        }

        if (nodeA.connections.contains(nodeB) && nodeB.connections.contains(nodeA)) {
            CCNetworks.LOGGER.warn("Tried to connect nodes that were already connected");
            return;
        } else if (nodeA.connections.contains(nodeB) || nodeB.connections.contains(nodeA)) {
            CCNetworks.LOGGER.warn("Invalid state: one node thinks its connected while other one isnt");
        }

        nodeA.connections.add(nodeB);
        nodeB.connections.add(nodeA);

        saveToSavedData();
    }

    public void joinNetwork(CableNetwork networkThatWillKeepExisting, CableNetwork networkThatWillBeDestroyed) {
        if (networkThatWillBeDestroyed == networkThatWillKeepExisting) {
            return;
        }

        for (Map.Entry<BlockPos, CableNetworkNode> entry : networkThatWillBeDestroyed.nodes.entrySet()) {
            entry.getValue().parentNetwork = networkThatWillKeepExisting;
            networkThatWillKeepExisting.nodes.put(entry.getKey(), entry.getValue());
            networkNodeMap.remove(entry.getKey());
            networkNodeMap.put(entry.getKey(), entry.getValue());
        }

        networkThatWillBeDestroyed.nodes.clear();
        networks.remove(networkThatWillBeDestroyed.uuid);

        saveToSavedData();
    }

    public void removeNode(CableNetworkNode node) { // this can be optimized way better i think
        List<CableNetworkNode> neighbors = new ArrayList<>(node.connections);

        for (CableNetworkNode connection : neighbors) {
            connection.connections.remove(node);
        }
        node.connections.clear();

        node.parentNetwork.nodes.remove(node.pos);
        networkNodeMap.remove(node.pos);
        if (node.parentNetwork.nodes.isEmpty()) {
            networks.remove(node.parentNetwork.uuid);
            return;
        }

        List<Set<CableNetworkNode>> bfsList = new ArrayList<>();
        for (CableNetworkNode neighbor : neighbors) {
            bfsList.add(listNetworkBFS(neighbor));
        }

        List<Set<CableNetworkNode>> filtered = new ArrayList<>();
        for (Set<CableNetworkNode> bfs : bfsList) {
            boolean foundDuplicate = false;
            for (Set<CableNetworkNode> f : filtered) {
                if (bfs.contains(f.iterator().next())) {
                    foundDuplicate = true;
                    break;
                }
            }
            if (!foundDuplicate) filtered.add(bfs);
        }

        for (Set<CableNetworkNode> f : filtered) {
            CableNetwork net = newNetwork();
            for (CableNetworkNode n : f) {
                n.parentNetwork.nodes.remove(n.pos);
                if (n.parentNetwork.nodes.isEmpty()) networks.remove(n.parentNetwork.uuid);
                n.parentNetwork = net;
                net.nodes.put(n.pos, n);
            }
        }

        saveToSavedData();
    }

    public void unlinkNodes(CableNetworkNode nodeA, CableNetworkNode nodeB) {
        nodeA.connections.remove(nodeB);
        nodeB.connections.remove(nodeA);

        Set<CableNetworkNode> bfsA = listNetworkBFS(nodeA);
        if (!bfsA.contains(nodeB)) {
            CableNetwork net = newNetwork();

            for (CableNetworkNode n : bfsA) {
                n.parentNetwork.nodes.remove(n.pos);
                if (n.parentNetwork.nodes.isEmpty()) networks.remove(net.uuid);
                n.parentNetwork = net;
                net.nodes.put(n.pos, n);
            }
        }

        saveToSavedData();
    }

    public Set<CableNetworkNode> listNetworkBFS(CableNetworkNode start) {
        Set<CableNetworkNode> visited = new HashSet<>();
        Queue<CableNetworkNode> queue = new ArrayDeque<>();
        queue.add(start);
        visited.add(start);

        while (!queue.isEmpty()) {
            CableNetworkNode node = queue.poll();
            for (CableNetworkNode neighbor : node.connections) {
                if (visited.add(neighbor)) {
                    queue.add(neighbor);
                }
            }
        }

        return visited;
    }
}
