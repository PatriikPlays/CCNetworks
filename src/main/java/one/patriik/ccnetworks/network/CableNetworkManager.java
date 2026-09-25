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
import one.patriik.ccnetworks.peripherals.NetworkInterfaceNodePeripheral;

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

            network.recomputeInterfaceNodeCache();
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
            node.parentNetwork.recomputeInterfaceNodeCache();
            saveToSavedData();
        }
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
        UUID uuid = UUID.randomUUID();
        CableNetwork network = new CableNetwork(uuid);

        networks.put(uuid, network);

        saveToSavedData();

        return network;
    }

    public CableNetworkNode createNode(CableNetwork network, BlockPos position, boolean isInterface) {
        CableNetworkNode node = new CableNetworkNode(position, network, isInterface);
        network.nodes.put(position, node);
        network.recomputeInterfaceNodeCache();
        networkNodeMap.put(position, node);
        chunkNodeMap.computeIfAbsent(new ChunkPos(position), k -> new HashSet<>()).add(node);

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
        if (peripheral != null) peripheral.clearNetworkNode();
        node.interfaceDestinations = List.of();
        List<CableNetworkNode> neighbors = new ArrayList<>(node.connections);

        // disconnect all connections
        for (CableNetworkNode connection : neighbors) {
            connection.connections.remove(node);
        }
        node.connections.clear();

        // Remove the node from every index before rebuilding the remaining components.
        originalNetwork.nodes.remove(node.pos);
        networkNodeMap.remove(node.pos);
        
        ChunkPos chunkPos = new ChunkPos(node.pos);
        Set<CableNetworkNode> chunkNodes = chunkNodeMap.get(chunkPos);
        if (chunkNodes != null) {
            chunkNodes.remove(node);
            if (chunkNodes.isEmpty()) {
                chunkNodeMap.remove(chunkPos);
            }
        }

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
