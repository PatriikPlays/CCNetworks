package one.patriik.ccnetworks;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import one.patriik.ccnetworks.blockentity.AbstractNetworkNodeBlockEntity;
import one.patriik.ccnetworks.network.CableNetworkManager;
import one.patriik.ccnetworks.network.CableNetworkNode;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import net.minecraft.commands.Commands;

import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CCNetworks implements ModInitializer {
    public static String MOD_ID = "ccnetworks";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static final CCNetworksConfig CONFIG = CCNetworksConfig.createToml(Paths.get("config"), "", "ccnetworks", CCNetworksConfig.class);

    public static Map<ResourceKey<Level>, CableNetworkManager> cableNetworkManagers = new HashMap<>();

    public static CableNetworkManager getCableNetworkManager(MinecraftServer server, ServerLevel level) {
        if (!cableNetworkManagers.containsKey(level.dimension())) { // i dont think this is necessary, but it probably doesnt hurt anything
            CableNetworkManager manager = new CableNetworkManager(server, level);
            cableNetworkManagers.put(level.dimension(), manager);
        }

        return cableNetworkManagers.get(level.dimension());
    }

    @Override
    public void onInitialize() {
        Registration.init();

        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            cableNetworkManagers.clear();
        });

        ServerWorldEvents.LOAD.register((server, level) -> {
            cableNetworkManagers.remove(level.dimension());
            CableNetworkManager manager = new CableNetworkManager(server, level);
            cableNetworkManagers.put(level.dimension(), manager);

            LOGGER.info("Loaded CableNetworkWorldSavedData for dimension {}", level.dimension().toString());
        });

        ServerChunkEvents.CHUNK_LOAD.register((level, chunk) -> {
            CableNetworkManager manager = cableNetworkManagers.get(level.dimension());
            if (manager != null) {
                manager.removeOrphanedNodesInChunk(chunk);
            }
        });


        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(Commands.literal("ccnetworks")
                .then(Commands.literal("netdebug")
                    .requires(source -> source.hasPermission(2))
                    .then(Commands.argument("pos", BlockPosArgument.blockPos())
                        .executes(context -> {
                            BlockPos pos = BlockPosArgument.getLoadedBlockPos(context, "pos");
                            ServerLevel level = context.getSource().getLevel();

                            BlockEntity be = level.getBlockEntity(pos);
                            if (be instanceof AbstractNetworkNodeBlockEntity node) {
                                CableNetworkNode nodeObj = node.getNetworkNode();
                                UUID networkUuid = nodeObj.parentNetwork.uuid;

                                // yes i hate this, no im not gonna fix it
                                StringBuilder serialized = new StringBuilder();
                                serialized.append("[");
                                var it = nodeObj.parentNetwork.nodes.values().iterator();
                                while (it.hasNext()) {
                                    CableNetworkNode v = it.next();
                                    serialized.append(String.format("{\"pos\":[%d,%d,%d],\"isInterface\":%s}", v.pos.getX(), v.pos.getY(), v.pos.getZ(), v.isInterfaceNode));
                                    if (it.hasNext()) serialized.append(",");
                                }
                                serialized.append("]");

                                String debugMessage = "Network " + networkUuid + ":\n"
                                    + "size: " + nodeObj.parentNetwork.nodes.size() + "\n"
                                    + "ifnodes: " + nodeObj.parentNetwork.interfaceNodeCache.size() + "\n"
                                    + "net: " + serialized;

                                if (debugMessage.length() < 32768-4096) {
                                    context.getSource().sendSuccess(() -> Component.literal(debugMessage), false);
                                } else {
                                    context.getSource().sendFailure(Component.literal("Network info is too large to display in chat"));
                                }
                            } else {
                                context.getSource().sendFailure(Component.literal("No network node found at specified position"));
                            }

                            return 1;
                        })
                    )
                )
                .then(Commands.literal("netinfo")
                    .then(Commands.argument("pos", BlockPosArgument.blockPos())
                        .executes(context -> {
                            BlockPos pos = BlockPosArgument.getLoadedBlockPos(context, "pos");
                            ServerLevel level = context.getSource().getLevel();

                            BlockEntity be = level.getBlockEntity(pos);
                            if (be instanceof AbstractNetworkNodeBlockEntity node) {
                                CableNetworkNode nodeObj = node.getNetworkNode();
                                UUID networkUuid = nodeObj.parentNetwork.uuid;

                                String debugMessage = "Network " + networkUuid + ":\n"
                                    + "size: " + nodeObj.parentNetwork.nodes.size() + "\n"
                                    + "ifnodes: " + nodeObj.parentNetwork.interfaceNodeCache.size();

                                context.getSource().sendSuccess(() -> Component.literal(debugMessage), false);
                            } else {
                                context.getSource().sendFailure(Component.literal("No network node found at specified position"));
                            }

                            return 1;
                        })
                    )
                )
            );
        });
    }
}
