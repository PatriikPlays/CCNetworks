package one.patriik.ccnetworks.network;

import net.minecraft.core.BlockPos;
import one.patriik.ccnetworks.CCNetworks;

import java.util.*;

public class CableNetworkManager {
    public static final Map<UUID, CableNetwork> networks = new HashMap<>();
    public static final Map<BlockPos, CableNetworkNode> networkNodeMap = new HashMap<>();

    public static CableNetwork newNetwork() {
        UUID uuid = UUID.randomUUID();
        CableNetwork network = new CableNetwork(uuid);

        networks.put(uuid, network);
        return network;
    }

    public static CableNetworkNode createNode(CableNetwork network, BlockPos position) {
        CableNetworkNode node = new CableNetworkNode(position, network);
        network.nodes.put(position, node);
        networkNodeMap.put(position, node);

        return node;
    }

    public static void connectNodes(CableNetworkNode nodeA, CableNetworkNode nodeB) {
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
    }

    public static void joinNetwork(CableNetwork networkThatWillKeepExisting, CableNetwork networkThatWillBeDestroyed) {
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
    }

    public static void removeNode(CableNetworkNode node) { // this can be optimized way better i think
        /*
        node.parentNetwork.nodes.remove(node.pos);
        networkNodeMap.remove(node.pos);

        for (CableNetworkNode connection : node.connections) {
            connection.connections.remove(node);
        }

        if (node.parentNetwork.nodes.isEmpty()) {
            networks.remove(node.parentNetwork.uuid);
        } else {
            List<Set<CableNetworkNode>> bfsList = new ArrayList<>();
            for (CableNetworkNode connection : node.connections) {
                bfsList.add(listNetworkBFS(connection));
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

                if (!foundDuplicate) {
                    filtered.add(bfs);
                }
            }

            for (Set<CableNetworkNode> f : filtered) {
                CableNetwork net = newNetwork();

                for (CableNetworkNode n : f) {
                    n.parentNetwork.nodes.remove(n.pos);
                    if (n.parentNetwork.nodes.isEmpty()) networks.remove(net.uuid);
                    n.parentNetwork = net;
                    net.nodes.put(n.pos, n);
                }
            }
        }
         */

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
    }

    public static void unlinkNodes(CableNetworkNode nodeA, CableNetworkNode nodeB) {
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
    }

    public static Set<CableNetworkNode> listNetworkBFS(CableNetworkNode start) {
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
