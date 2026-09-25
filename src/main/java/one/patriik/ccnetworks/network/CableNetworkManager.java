package one.patriik.ccnetworks.network;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongArrayTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import one.patriik.ccnetworks.CCNetworks;
import one.patriik.ccnetworks.blockentity.AbstractNetworkNodeBlockEntity;
import one.patriik.ccnetworks.peripherals.NetworkInterfaceNodePeripheral;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;

public class CableNetworkManager {
    private static final String NETWORKS_TAG = "networks";
    private static final String UUID_TAG = "uuid";
    private static final String NODES_TAG = "nodes";
    private static final String POSITION_TAG = "pos";
    private static final String INTERFACE_TAG = "isInterface";
    private static final String CONNECTIONS_TAG = "connections";

    private final Map<UUID, CableNetwork> networks = new HashMap<>();
    private final Map<BlockPos, CableNetworkNode> networkNodeMap = new HashMap<>();
    private final Map<ChunkPos, Set<CableNetworkNode>> chunkNodeMap = new HashMap<>();

    private final CableNetworkWorldSavedData savedData;

    public CableNetworkManager(MinecraftServer server, ServerLevel level) {
        this.savedData = CableNetworkWorldSavedData.getDimensionSavedData(server, level.dimension());
        loadNetworks(savedData.getData());
    }

    private void loadNetworks(CompoundTag data) {
        if (!data.contains(NETWORKS_TAG)) {
            return;
        }

        ListTag networkTags = data.getList(NETWORKS_TAG, Tag.TAG_COMPOUND);
        for (int i = 0; i < networkTags.size(); i++) {
            loadNetwork(networkTags.getCompound(i));
        }
    }

    private void loadNetwork(CompoundTag networkTag) {
        UUID networkUUID = networkTag.getUUID(UUID_TAG);
        CableNetwork network = new CableNetwork(networkUUID);
        networks.put(networkUUID, network);

        if (!networkTag.contains(NODES_TAG)) {
            return;
        }

        ListTag nodeTags = networkTag.getList(NODES_TAG, Tag.TAG_COMPOUND);
        for (int i = 0; i < nodeTags.size(); i++) {
            loadNode(network, nodeTags.getCompound(i));
        }

        for (int i = 0; i < nodeTags.size(); i++) {
            loadConnections(network, nodeTags.getCompound(i));
        }

        network.recomputeInterfaceNodeCache();
    }

    private void loadNode(CableNetwork network, CompoundTag nodeTag) {
        BlockPos pos = BlockPos.of(nodeTag.getLong(POSITION_TAG));
        boolean isInterfaceNode = nodeTag.getBoolean(INTERFACE_TAG);
        CableNetworkNode node = new CableNetworkNode(pos, network, isInterfaceNode);
        network.nodes.put(pos, node);
        addNodeToIndexes(node);
    }

    private void loadConnections(CableNetwork network, CompoundTag nodeTag) {
        if (!nodeTag.contains(CONNECTIONS_TAG)) {
            return;
        }

        BlockPos pos = BlockPos.of(nodeTag.getLong(POSITION_TAG));
        CableNetworkNode node = network.nodes.get(pos);
        for (long connectionPos : nodeTag.getLongArray(CONNECTIONS_TAG)) {
            CableNetworkNode connectedNode = network.nodes.get(BlockPos.of(connectionPos));
            if (connectedNode != null) {
                node.connections.add(connectedNode);
            }
        }
    }

    private void addNodeToIndexes(CableNetworkNode node) {
        networkNodeMap.put(node.pos, node);
        chunkNodeMap.computeIfAbsent(new ChunkPos(node.pos), key -> new HashSet<>()).add(node);
    }

    private void removeNodeFromIndexes(CableNetworkNode node) {
        networkNodeMap.remove(node.pos);

        ChunkPos chunkPos = new ChunkPos(node.pos);
        Set<CableNetworkNode> chunkNodes = chunkNodeMap.get(chunkPos);
        if (chunkNodes != null) {
            chunkNodes.remove(node);
            if (chunkNodes.isEmpty()) {
                chunkNodeMap.remove(chunkPos);
            }
        }
    }

    private void saveToSavedData() {
        CompoundTag data = new CompoundTag();
        ListTag networkTags = new ListTag();

        for (CableNetwork network : networks.values()) {
            networkTags.add(saveNetwork(network));
        }

        data.put(NETWORKS_TAG, networkTags);
        savedData.setData(data);
    }

    private CompoundTag saveNetwork(CableNetwork network) {
        CompoundTag networkTag = new CompoundTag();
        networkTag.putUUID(UUID_TAG, network.uuid);

        ListTag nodeTags = new ListTag();
        for (CableNetworkNode node : network.nodes.values()) {
            nodeTags.add(saveNode(node));
        }
        networkTag.put(NODES_TAG, nodeTags);
        return networkTag;
    }

    private CompoundTag saveNode(CableNetworkNode node) {
        CompoundTag nodeTag = new CompoundTag();
        nodeTag.putLong(POSITION_TAG, node.pos.asLong());
        nodeTag.putBoolean(INTERFACE_TAG, node.isInterfaceNode);

        long[] connections = new long[node.connections.size()];
        for (int i = 0; i < node.connections.size(); i++) {
            connections[i] = node.connections.get(i).pos.asLong();
        }
        nodeTag.put(CONNECTIONS_TAG, new LongArrayTag(connections));
        return nodeTag;
    }

    public void setIsInterfaceNode(CableNetworkNode node, boolean isInterfaceNode) {
        if (node == null) {
            return;
        }

        node.isInterfaceNode = isInterfaceNode;
        node.parentNetwork.recomputeInterfaceNodeCache();
        saveToSavedData();
    }

    public void registerInterfacePeripheral(CableNetworkNode node, NetworkInterfaceNodePeripheral peripheral) {
        node.peripheral = peripheral;
        peripheral.setNetworkNode(node);
        node.parentNetwork.recomputeInterfaceNodeCache();
    }

    public void unregisterInterfacePeripheral(CableNetworkNode node, NetworkInterfaceNodePeripheral peripheral) {
        if (node.peripheral == peripheral) {
            node.peripheral = null;
            peripheral.clearNetworkNode();
        }
    }

    public CableNetworkNode getNodeAt(BlockPos pos) {
        return networkNodeMap.get(pos);
    }

    public CableNetwork newNetwork() {
        CableNetwork network = new CableNetwork(UUID.randomUUID());
        networks.put(network.uuid, network);
        saveToSavedData();
        return network;
    }

    public CableNetworkNode createNode(CableNetwork network, BlockPos position, boolean isInterface) {
        CableNetworkNode node = new CableNetworkNode(position, network, isInterface);
        network.nodes.put(position, node);
        addNodeToIndexes(node);
        network.recomputeInterfaceNodeCache();

        saveToSavedData();

        return node;
    }

    public void connectNodes(CableNetworkNode nodeA, CableNetworkNode nodeB) {
        if (!isManagedNode(nodeA) || !isManagedNode(nodeB)) {
            throw new IllegalArgumentException("Cannot connect nodes that are not managed by this network manager");
        }

        if (nodeA.pos.equals(nodeB.pos)) {
            CCNetworks.LOGGER.warn("Tried to connect node to itself");
            return;
        }

        if (!nodeA.parentNetwork.uuid.equals(nodeB.parentNetwork.uuid)) {
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
            networkNodeMap.put(entry.getKey(), entry.getValue());
        }

        networkThatWillBeDestroyed.nodes.clear();
        networks.remove(networkThatWillBeDestroyed.uuid);
        networkThatWillKeepExisting.recomputeInterfaceNodeCache();

        saveToSavedData();
    }

    public void removeNode(CableNetworkNode node) {
        if (!isManagedNode(node)) {
            return;
        }

        CableNetwork originalNetwork = node.parentNetwork;
        NetworkInterfaceNodePeripheral peripheral = node.peripheral;
        node.peripheral = null;
        if (peripheral != null) {
            peripheral.clearNetworkNode();
        }
        node.interfaceDestinations = List.of();
        List<CableNetworkNode> neighbors = new ArrayList<>(node.connections);

        for (CableNetworkNode connection : neighbors) {
            connection.connections.remove(node);
        }
        node.connections.clear();

        originalNetwork.nodes.remove(node.pos);
        removeNodeFromIndexes(node);

        if (originalNetwork.nodes.isEmpty()) {
            networks.remove(originalNetwork.uuid);
            saveToSavedData();
            return;
        }

        splitNetwork(originalNetwork);
        saveToSavedData();
    }

    public void unlinkNodes(CableNetworkNode nodeA, CableNetworkNode nodeB) {
        if (!isManagedNode(nodeA) || !isManagedNode(nodeB)) {
            throw new IllegalArgumentException("Cannot unlink nodes that are not managed by this network manager");
        }

        nodeA.connections.remove(nodeB);
        nodeB.connections.remove(nodeA);

        if (nodeA.parentNetwork == nodeB.parentNetwork) {
            splitNetwork(nodeA.parentNetwork);
        } else {
            nodeA.parentNetwork.recomputeInterfaceNodeCache();
            nodeB.parentNetwork.recomputeInterfaceNodeCache();
        }

        saveToSavedData();
    }

    private boolean isManagedNode(CableNetworkNode node) {
        return node != null
            && networkNodeMap.get(node.pos) == node
            && networks.get(node.parentNetwork.uuid) == node.parentNetwork
            && node.parentNetwork.nodes.get(node.pos) == node;
    }

    private void splitNetwork(CableNetwork network) {
        Set<CableNetworkNode> remaining = new HashSet<>(network.nodes.values());
        List<Set<CableNetworkNode>> components = new ArrayList<>();

        while (!remaining.isEmpty()) {
            CableNetworkNode start = remaining.iterator().next();
            Set<CableNetworkNode> component = listNetworkBFS(start);
            component.retainAll(remaining);
            remaining.removeAll(component);
            components.add(component);
        }

        if (components.size() == 1) {
            network.recomputeInterfaceNodeCache();
            return;
        }

        network.nodes.clear();
        for (CableNetworkNode node : components.get(0)) {
            node.parentNetwork = network;
            network.nodes.put(node.pos, node);
        }
        network.recomputeInterfaceNodeCache();

        for (int i = 1; i < components.size(); i++) {
            CableNetwork split = new CableNetwork(UUID.randomUUID());
            networks.put(split.uuid, split);
            for (CableNetworkNode node : components.get(i)) {
                node.parentNetwork = split;
                split.nodes.put(node.pos, node);
            }
            split.recomputeInterfaceNodeCache();
        }
    }

    // todo: somehow send block updates to all nodes connected to the removed node?
    public void removeOrphanedNodesInChunk(LevelChunk chunk) {
        ChunkPos chunkPos = chunk.getPos();
        Set<CableNetworkNode> chunkNodes = chunkNodeMap.get(chunkPos);
        if (chunkNodes == null) {
            return;
        }

        List<CableNetworkNode> toRemove = new ArrayList<>();

        for (CableNetworkNode node : chunkNodes) {
            BlockEntity blockEntity = chunk.getBlockEntity(node.pos);
            if (!(blockEntity instanceof AbstractNetworkNodeBlockEntity nodeBlockEntity)) {
                toRemove.add(node);
            } else if (node.isInterfaceNode != nodeBlockEntity.isInterfaceNode()) {
                setIsInterfaceNode(node, nodeBlockEntity.isInterfaceNode());
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
