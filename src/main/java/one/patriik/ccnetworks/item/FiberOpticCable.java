package one.patriik.ccnetworks.item;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
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

        if (be instanceof AbstractNetworkNodeBlockEntity nodeBE) {
            if (!level.isClientSide()) {
                CompoundTag tag = stack.getOrCreateTag();
                boolean hasPos1 = tag.contains("pos1", CompoundTag.TAG_INT_ARRAY) && tag.getIntArray("pos1").length >= 3;
                boolean hasPos1Dim = tag.contains("pos1dim", CompoundTag.TAG_STRING);

                if ((hasPos1 || hasPos1Dim) && !(hasPos1 && hasPos1Dim)) {
                    if (player != null) player.sendSystemMessage(Component.literal("Failed to link"));
                    tag.remove("pos1");
                    tag.remove("pos1dim");
                } else if (hasPos1 && hasPos1Dim) { // link now
                    int[] pos1Array = tag.getIntArray("pos1");
                    BlockPos pos1 = new BlockPos(pos1Array[0], pos1Array[1], pos1Array[2]);
                    BlockPos pos2 = nodeBE.getBlockPos();

                    if (pos1.equals(pos2)) {
                        if (player != null) player.sendSystemMessage(Component.literal("Failed to link: cannot link to itself"));
                        tag.remove("pos1");
                        tag.remove("pos1dim");
                        return InteractionResult.sidedSuccess(level.isClientSide());
                    }

                    if (!tag.getString("pos1dim").equals(level.dimension().location().toString())) {
                        if (player != null) player.sendSystemMessage(Component.literal("Failed to link: cannot link across dimensions"));
                        tag.remove("pos1");
                        tag.remove("pos1dim");
                        return InteractionResult.sidedSuccess(level.isClientSide());
                    }

                    if (!level.isLoaded(pos1) || !level.isLoaded(be.getBlockPos())) {
                        if (player != null) player.sendSystemMessage(Component.literal("Failed to link: one node isn't loaded"));
                        tag.remove("pos1");
                        tag.remove("pos1dim");
                        return InteractionResult.sidedSuccess(level.isClientSide());
                    }

                    BlockEntity be1 = level.getBlockEntity(pos1);

                    if (be1 instanceof AbstractNetworkNodeBlockEntity nodeBE1) {
                        CableNetworkManager nm = nodeBE1.getCableNetworkManager();
                        if (nm != nodeBE.getCableNetworkManager()) {
                            throw new IllegalStateException("Two nodes in the same dimension don't have the same network manager");
                        }
                        CableNetworkNode node1 = nodeBE1.getNetworkNode();
                        CableNetworkNode node2 = nodeBE.getNetworkNode();
                        List<CableNetworkNode> links1 = node1.connections;
                        List<CableNetworkNode> links2 = node2.connections;

                        if (links1.contains(node2) && links2.contains(node1)) {
                            if (player != null) player.sendSystemMessage(Component.literal("Unlinked successfully"));

                            nm.unlinkNodes(node1, node2);
                            nodeBE.update();
                            nodeBE1.update();
                            tag.remove("pos1");
                            tag.remove("pos1dim");
                            return InteractionResult.sidedSuccess(level.isClientSide());
                        }
                        // this seems like nonsense, why did i do this
                        /* else if (links1.contains(pos1) && links2.contains(pos2)) {
                            if (player != null) player.sendSystemMessage(Component.literal("Unlinked successfully"));
                            nodeBE1.removeLink(pos1);
                            nodeBE.removeLink(pos2);
                            CableNetworkManager.unlinkNodes(nodeBE.getNetworkNode(), nodeBE1.getNetworkNode());
                            tag.remove("pos1");
                            tag.remove("pos1dim");
                            return InteractionResult.sidedSuccess(level.isClientSide());
                        }*/

                        if (links1.size() >= 4 || links2.size() >= 4) {
                            if (player != null) player.sendSystemMessage(Component.literal("Failed to link: one or more nodes already have 4 links"));
                            tag.remove("pos1");
                            tag.remove("pos1dim");
                            return InteractionResult.sidedSuccess(level.isClientSide());
                        }

                        nm.connectNodes(node1, node2);
                        nodeBE.update();
                        nodeBE1.update();
                        tag.remove("pos1");
                        tag.remove("pos1dim");
                        if (player != null) player.sendSystemMessage(Component.literal("Linked successfully"));
                        return InteractionResult.sidedSuccess(level.isClientSide());
                    } else {
                        if (player != null) player.sendSystemMessage(Component.literal("Failed to link: one position isn't a valid AbstractNetworkNodeBlockEntity or doesn't exist anymore"));
                        tag.remove("pos1");
                        tag.remove("pos1dim");
                        return InteractionResult.sidedSuccess(level.isClientSide());
                    }
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
}
