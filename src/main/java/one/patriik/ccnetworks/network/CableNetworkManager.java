package one.patriik.ccnetworks.network;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongArrayTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import one.patriik.ccnetworks.CCNetworks;
import one.patriik.ccnetworks.blockentity.AbstractNetworkNodeBlockEntity;

import java.util.*;

public class CableNetworkManager {
    private final Map<UUID, CableNetwork> networks = new HashMap<>();
    private final Map<BlockPos, CableNetworkNode> networkNodeMap = new HashMap<>();
    private final Map<ChunkPos, Set<CableNetworkNode>> chunkNodeMap = new HashMap<>();

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
                boolean isInterfaceNode = nodeTag.getBoolean("isInterface");

                CableNetworkNode node = new CableNetworkNode(pos, network, isInterfaceNode);
                network.nodes.put(pos, node);
                networkNodeMap.put(pos, node);
                chunkNodeMap.computeIfAbsent(new ChunkPos(pos), k -> new HashSet<>()).add(node);

                if (isInterfaceNode) {
                    network.interfaceNodeCache.add(node);
                }
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
                nodeTag.putBoolean("isInterface", node.isInterfaceNode);

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

    public void setIsInterfaceNode(CableNetworkNode node, boolean isInterfaceNode) {
        if (node != null) {
            node.isInterfaceNode = isInterfaceNode;
            saveToSavedData();
        }
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

    public CableNetworkNode createNode(CableNetwork network, BlockPos position, boolean isInterface) {
        CableNetworkNode node = new CableNetworkNode(position, network, isInterface);
        network.nodes.put(position, node);
        network.interfaceNodeCache.add(node);
        networkNodeMap.put(position, node);
        chunkNodeMap.computeIfAbsent(new ChunkPos(position), k -> new HashSet<>()).add(node);

        saveToSavedData();

        return node;
    }

    public void connectNodes(CableNetworkNode nodeA, CableNetworkNode nodeB) {
        if (nodeA.pos.equals(nodeB.pos)) {
            CCNetworks.LOGGER.warn("Tried to connect node to itself");
            return;
        }

        if (nodeA.parentNetwork.uuid != nodeB.parentNetwork.uuid) {
            joinNetwork(nodeA.parentNetwork, nodeB.parentNetwork);
        }

        if (nodeA.connections.contains(nodeB) && nodeB.connections.contains(nodeA)) {
            CCNetworks.LOGGER.warn("Tried to connect nodes that were already connected");
            return;
        } else if (nodeA.connections.contains(nodeB) || nodeB.connections.contains(nodeA)) {
            CCNetworks.LOGGER.warn("Invalid state: one node thinks its connected while other one isn't");
            nodeA.connections.remove(nodeB);
            nodeB.connections.remove(nodeA);
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
            if (entry.getValue().isInterfaceNode) {
                networkThatWillKeepExisting.interfaceNodeCache.add(entry.getValue());
            }
        }

        networkThatWillBeDestroyed.nodes.clear();
        networks.remove(networkThatWillBeDestroyed.uuid);

        saveToSavedData();
    }

    public void removeNode(CableNetworkNode node) { // this can be optimized way better i think
        List<CableNetworkNode> neighbors = new ArrayList<>(node.connections);

        // disconnect all connections
        for (CableNetworkNode connection : neighbors) {
            connection.connections.remove(node);
        }
        node.connections.clear();

        // remove from nodes, remove network and return if empty
        node.parentNetwork.nodes.remove(node.pos);
        networkNodeMap.remove(node.pos);
        
        ChunkPos chunkPos = new ChunkPos(node.pos);
        Set<CableNetworkNode> chunkNodes = chunkNodeMap.get(chunkPos);
        if (chunkNodes != null) {
            chunkNodes.remove(node);
            if (chunkNodes.isEmpty()) {
                chunkNodeMap.remove(chunkPos);
            }
        }

        if (node.parentNetwork.nodes.isEmpty()) {
            networks.remove(node.parentNetwork.uuid);
            saveToSavedData();
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

            net.recomputeInterfaceNodeCache();
        }

        // the original network should always be deleted unless i fucked something up, so no need to recompute that

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
                if (n.parentNetwork.nodes.isEmpty()) networks.remove(n.parentNetwork.uuid);
                n.parentNetwork = net;
                net.nodes.put(n.pos, n);
            }
        }

        if (networks.containsKey(nodeA.parentNetwork.uuid)) {
            networks.get(nodeA.parentNetwork.uuid).recomputeInterfaceNodeCache();
        }

        if (networks.containsKey(nodeB.parentNetwork.uuid)) {
            networks.get(nodeB.parentNetwork.uuid).recomputeInterfaceNodeCache();
        }

        saveToSavedData();
    }

    // todo: somehow send block updates to all nodes connected to the removed node?
    public void removeOrphanedNodesInChunk(LevelChunk chunk) {
        ChunkPos chunkPos = chunk.getPos();
        List<CableNetworkNode> toRemove = new ArrayList<>();

        Set<CableNetworkNode> chunkNodes = chunkNodeMap.get(chunkPos);
        if (chunkNodes != null) {
            for (CableNetworkNode node : chunkNodes) {
                BlockEntity blockEntity = chunk.getBlockEntity(node.pos);
                if (!(blockEntity instanceof AbstractNetworkNodeBlockEntity)) {
                    toRemove.add(node);
                } else {
                    CCNetworks.LOGGER.trace("Found valid network node at {} in chunk {}, skipping", node.pos, chunkPos);
                }
            }
        }

        for (CableNetworkNode node : toRemove) {
            CCNetworks.LOGGER.warn("Removing orphaned network node at {} (no matching block entity found)", node.pos);
            removeNode(node);
        }
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
