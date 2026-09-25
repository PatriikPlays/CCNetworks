package one.patriik.ccnetworks.command;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import one.patriik.ccnetworks.blockentity.AbstractNetworkNodeBlockEntity;
import one.patriik.ccnetworks.network.CableNetwork;
import one.patriik.ccnetworks.network.CableNetworkNode;

import java.util.UUID;

public final class CCNetworksCommands {
    private CCNetworksCommands() {}

    public static void register() {
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
                                CableNetwork network = node.getNetwork();
                                if (nodeObj == null || network == null) {
                                    context.getSource().sendFailure(Component.literal("No network found for node at specified position"));
                                    return 0;
                                }
                                UUID networkUuid = network.uuid;

                                // yes i hate this, no im not gonna fix it
                                StringBuilder serialized = new StringBuilder();
                                serialized.append("[");
                                var it = network.nodes.values().iterator();
                                while (it.hasNext()) {
                                    CableNetworkNode v = it.next();
                                    serialized.append(String.format("{\"pos\":[%d,%d,%d],\"isInterface\":%s}", v.pos.getX(), v.pos.getY(), v.pos.getZ(), v.isInterfaceNode));
                                    if (it.hasNext()) serialized.append(",");
                                }
                                serialized.append("]");

                                String debugMessage = "Network " + networkUuid + ":\n"
                                    + "size: " + network.nodes.size() + "\n"
                                    + "ifnodes: " + network.interfaceNodeCache.size() + "\n"
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
                                CableNetwork network = node.getNetwork();
                                if (network == null) {
                                    context.getSource().sendFailure(Component.literal("No network found for node at specified position"));
                                    return 0;
                                }

                                String debugMessage = "Network " + network.uuid + ":\n"
                                    + "size: " + network.nodes.size() + "\n"
                                    + "ifnodes: " + network.interfaceNodeCache.size();

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
