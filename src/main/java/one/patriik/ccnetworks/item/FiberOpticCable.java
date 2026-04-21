package one.patriik.ccnetworks.item;

import net.fabricmc.fabric.api.entity.FakePlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import one.patriik.ccnetworks.CCNetworks;
import one.patriik.ccnetworks.Registration;
import one.patriik.ccnetworks.blockentity.AbstractNetworkNodeBlockEntity;
import one.patriik.ccnetworks.network.CableNetworkManager;
import one.patriik.ccnetworks.network.CableNetworkNode;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class FiberOpticCable extends Item {
    public FiberOpticCable(Properties properties) {
        super(properties);
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand usedHand) {
        ItemStack stack = player.getItemInHand(usedHand);

        if (!level.isClientSide()) {
            if (player.isShiftKeyDown()) {
                CompoundTag tag = stack.getOrCreateTag();

                tag.remove("pos1");
                tag.remove("pos1dim");

                player.sendSystemMessage(Component.literal("Selection cleared"));
                return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
            }
        }

        return InteractionResultHolder.pass(stack);
    }

    @Override
    public @NotNull InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Player player = context.getPlayer();
        ItemStack stack = context.getItemInHand();

        BlockEntity be = level.getBlockEntity(pos);

        if (be instanceof AbstractNetworkNodeBlockEntity nodeBE && player instanceof ServerPlayer && !(player instanceof FakePlayer)) { // todo: support turtles later?
            if (!level.isClientSide()) {
                CompoundTag tag = stack.getOrCreateTag();
                boolean hasPos1 = tag.contains("pos1", CompoundTag.TAG_INT_ARRAY) && tag.getIntArray("pos1").length >= 3;
                boolean hasPos1Dim = tag.contains("pos1dim", CompoundTag.TAG_STRING);

                if ((hasPos1 || hasPos1Dim) && !(hasPos1 && hasPos1Dim)) {
                    player.sendSystemMessage(Component.literal("Failed to link"));
                    tag.remove("pos1");
                    tag.remove("pos1dim");
                    stack.setTag(null);
                } else if (hasPos1 && hasPos1Dim) { // link now
                    int[] pos1Array = tag.getIntArray("pos1");
                    BlockPos pos1 = new BlockPos(pos1Array[0], pos1Array[1], pos1Array[2]);
                    BlockPos pos2 = nodeBE.getBlockPos();

                    if (pos1.equals(pos2)) {
                        player.sendSystemMessage(Component.literal("Failed to link: cannot link to itself"));
                        tag.remove("pos1");
                        tag.remove("pos1dim");
                        stack.setTag(null);
                        return InteractionResult.sidedSuccess(level.isClientSide());
                    }

                    if (!tag.getString("pos1dim").equals(level.dimension().location().toString())) {
                        player.sendSystemMessage(Component.literal("Failed to link: cannot link across dimensions"));
                        tag.remove("pos1");
                        tag.remove("pos1dim");
                        stack.setTag(null);
                        return InteractionResult.sidedSuccess(level.isClientSide());
                    }

                    BlockEntity be1 = level.getBlockEntity(pos1);

                    if (!(be1 instanceof AbstractNetworkNodeBlockEntity nodeBE1)) {
                        player.sendSystemMessage(Component.literal("Failed to link: first node no longer exists or is not loaded"));
                        tag.remove("pos1");
                        tag.remove("pos1dim");
                        stack.setTag(null);
                        return InteractionResult.sidedSuccess(level.isClientSide());
                    }

                    CableNetworkManager nm = nodeBE1.getCableNetworkManager();
                    if (nm != nodeBE.getCableNetworkManager()) {
                        throw new IllegalStateException("Two nodes in the same dimension don't have the same network manager");
                    }
                    CableNetworkNode node1 = nodeBE1.getNetworkNode();
                    CableNetworkNode node2 = nodeBE.getNetworkNode();

                    if (node1 == null || node2 == null) {
                        player.sendSystemMessage(Component.literal("Failed to link: one or both nodes are not properly initialized"));
                        tag.remove("pos1");
                        tag.remove("pos1dim");
                        stack.setTag(null);
                        return InteractionResult.sidedSuccess(level.isClientSide());
                    }

                    List<CableNetworkNode> links1 = node1.connections;
                        List<CableNetworkNode> links2 = node2.connections;

                        if (links1.contains(node2) && links2.contains(node1)) {
                            player.sendSystemMessage(Component.literal("Unlinked successfully"));

                            nm.unlinkNodes(node1, node2);
                            nodeBE.update();
                            nodeBE1.update();
                            tag.remove("pos1");
                            tag.remove("pos1dim");
                            stack.setTag(null);

                            if (!player.isCreative()) { // todo: directly give into inventory if possible
                                double distRound = Math.round(Math.sqrt(nodeBE1.getBlockPos().distSqr(nodeBE.getBlockPos())));
                                while (distRound > 0) {
                                    int amt = (int) Math.min(64, distRound);

                                    ItemEntity item = new ItemEntity(level, player.getX(), player.getY(), player.getZ(), new ItemStack(Registration.ModItems.FIBER_OPTIC_CABLE, amt));
                                    level.addFreshEntity(item);
                                    distRound -= amt;
                                }
                            }

                            return InteractionResult.sidedSuccess(level.isClientSide());
                        }

                        if (links1.size() >= CCNetworks.CONFIG.maxConnectionsPerNode || links2.size() >= CCNetworks.CONFIG.maxConnectionsPerNode) {
                            player.sendSystemMessage(Component.literal("Failed to link: one or more nodes already has max amount of links ("+CCNetworks.CONFIG.maxConnectionsPerNode+")"));
                            tag.remove("pos1");
                            tag.remove("pos1dim");
                            stack.setTag(null);
                            return InteractionResult.sidedSuccess(level.isClientSide());
                        }

                        double distRound = Math.round(Math.sqrt(nodeBE1.getBlockPos().distSqr(nodeBE.getBlockPos())));

                        if (distRound > CCNetworks.CONFIG.maxCableLength) {
                            player.sendSystemMessage(Component.literal("Failed to link: cable too long, limit is "+CCNetworks.CONFIG.maxCableLength));
                            tag.remove("pos1");
                            tag.remove("pos1dim");
                            stack.setTag(null);
                            return InteractionResult.sidedSuccess(level.isClientSide());
                        }

                        if (!player.isCreative()) {
                            int cost = (int) distRound;
                            int available = 0;
                            for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                                ItemStack invStack = player.getInventory().getItem(i);
                                if (invStack.getItem() instanceof FiberOpticCable) {
                                    available += invStack.getCount();
                                }
                            }
                            if (available < cost) {
                                player.sendSystemMessage(Component.literal("Failed to link: not enough cables (have " + available + ", costs " + cost + ")"));
                                tag.remove("pos1");
                                tag.remove("pos1dim");
                                stack.setTag(null);
                                return InteractionResult.sidedSuccess(level.isClientSide());
                            }
                        }

                        tag.remove("pos1");
                        tag.remove("pos1dim");
                        stack.setTag(null);

                        if (!player.isCreative()) {
                            consumeCablesFromInventory(player, (int) distRound, stack);
                        }
                        nm.connectNodes(node1, node2);
                        nodeBE.update();
                        nodeBE1.update();
                        player.sendSystemMessage(Component.literal("Linked successfully"));

                        return InteractionResult.sidedSuccess(level.isClientSide());
                } else { // nothing linked yet
                    tag.putIntArray("pos1", new int[]{pos.getX(), pos.getY(), pos.getZ()});
                    tag.putString("pos1dim", level.dimension().location().toString());
                }

            }
            return InteractionResult.sidedSuccess(level.isClientSide());
        }

        return InteractionResult.PASS;
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        CompoundTag tag = stack.getOrCreateTag();
        boolean hasPos1 = tag.contains("pos1", CompoundTag.TAG_INT_ARRAY) && tag.getIntArray("pos1").length >= 3;

        if (hasPos1) {
            return true;
        }

        return super.isFoil(stack);
    }

    /**
     * Consume cable items from the player's inventory.
     * If heldStack is provided, it will be consumed last.
     */
    public static void consumeCablesFromInventory(Player player, int cost, ItemStack heldStack) {
        int remaining = cost;
        for (int i = 0; i < player.getInventory().getContainerSize() && remaining > 0; i++) {
            ItemStack invStack = player.getInventory().getItem(i);
            if (heldStack != null && invStack == heldStack) continue; // consume held stack last
            if (invStack.getItem() instanceof FiberOpticCable) {
                int take = Math.min(remaining, invStack.getCount());
                invStack.shrink(take);
                remaining -= take;
            }
        }
        if (remaining > 0 && heldStack != null) {
            heldStack.shrink(remaining);
        }
    }
}
