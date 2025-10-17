package one.patriik.ccnetworks.item;

import net.fabricmc.fabric.api.util.NbtType;
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
import net.minecraft.world.phys.BlockHitResult;
import one.patriik.ccnetworks.Registration;
import one.patriik.ccnetworks.blockentity.NetworkNodeBlockEntity;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class FiberOpticCable extends Item {
    public FiberOpticCable(Properties properties) {
        super(properties);
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand usedHand) {
        ItemStack stack = player.getItemInHand(usedHand);

        if (!level.isClientSide) {
            if (player.isShiftKeyDown()) {
                CompoundTag tag = stack.getOrCreateTag();

                tag.remove("pos1");
                tag.remove("pos2");

                stack.save(tag);

                player.sendSystemMessage(Component.literal("Selection cleared"));
                return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
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

        BlockState block = level.getBlockState(pos);
        BlockEntity be = level.getBlockEntity(pos);

        if (block.is(Registration.ModBlocks.NETWORK_NODE) && be instanceof NetworkNodeBlockEntity nodeBE) {
            if (!level.isClientSide) {
                List<BlockPos> nodeLinks = nodeBE.getLinks();

                /*
                if (nodeLinks.size() >= 2) {
                    if (player != null) player.sendSystemMessage(Component.literal("This node already has 2 links"));
                    return InteractionResult.FAIL;
                }*/

                CompoundTag tag = stack.getOrCreateTag();
                boolean hasPos1 = tag.contains("pos1", CompoundTag.TAG_INT_ARRAY) && tag.getIntArray("pos1").length >= 3;
                boolean hasPos2 = tag.contains("pos2", CompoundTag.TAG_INT_ARRAY) && tag.getIntArray("pos2").length >= 3;

                if (hasPos1 && hasPos2) { // link
                    tag.remove("pos1");
                    tag.remove("pos2");
                } else if (hasPos1) {
                    tag.putIntArray("pos2", new int[]{pos.getX(), pos.getY(), pos.getZ()});
                } else if (hasPos2) {
                    tag.putIntArray("pos1", new int[]{pos.getX(), pos.getY(), pos.getZ()});
                } else { // nothing linked yet
                    tag.putIntArray("pos1", new int[]{pos.getX(), pos.getY(), pos.getZ()});
                }

                hasPos1 = tag.contains("pos1", CompoundTag.TAG_INT_ARRAY) && tag.getIntArray("pos1").length >= 3;
                hasPos2 = tag.contains("pos2", CompoundTag.TAG_INT_ARRAY) && tag.getIntArray("pos2").length >= 3;

                if (hasPos1 && hasPos2) {
                    int[] pos1Array = tag.getIntArray("pos1");
                    int[] pos2Array = tag.getIntArray("pos2");
                    BlockPos pos1 = new BlockPos(pos1Array[0], pos1Array[1], pos1Array[2]);
                    BlockPos pos2 = new BlockPos(pos2Array[0], pos2Array[1], pos2Array[2]);

                    if (pos1.equals(pos2)) {
                        if (player != null) player.sendSystemMessage(Component.literal("Failed to link: cannot link to itself"));
                        tag.remove("pos1");
                        tag.remove("pos2");
                        return InteractionResult.sidedSuccess(level.isClientSide);
                    }

                    if (!level.isLoaded(pos1) || !level.isLoaded(pos2)) {
                        if (player != null) player.sendSystemMessage(Component.literal("Failed to link: one node isn't loaded"));
                        tag.remove("pos1");
                        tag.remove("pos2");
                        return InteractionResult.sidedSuccess(level.isClientSide);
                    }

                    BlockEntity be1 = level.getBlockEntity(pos1);
                    BlockEntity be2 = level.getBlockEntity(pos2);

                    if (be1 instanceof NetworkNodeBlockEntity nodeBE1 && be2 instanceof NetworkNodeBlockEntity nodeBE2) {
                        List<BlockPos> links1 = nodeBE1.getLinks();
                        List<BlockPos> links2 = nodeBE2.getLinks();

                        //todo: check if already exists, if yes, unlink
                        if (links1.contains(pos2) && links2.contains(pos1)) {
                            if (player != null) player.sendSystemMessage(Component.literal("Unlinked successfully"));
                            nodeBE1.removeLink(pos2);
                            nodeBE2.removeLink(pos1);
                            tag.remove("pos1");
                            tag.remove("pos2");
                            return InteractionResult.sidedSuccess(level.isClientSide);
                        } else if (links1.contains(pos1) && links2.contains(pos2)) {
                            if (player != null) player.sendSystemMessage(Component.literal("Unlinked successfully"));
                            nodeBE1.removeLink(pos1);
                            nodeBE1.removeLink(pos2);
                            tag.remove("pos1");
                            tag.remove("pos2");
                            return InteractionResult.sidedSuccess(level.isClientSide);
                        }

                        if (links1.size() >= 2 || links2.size() >= 2) {
                            if (player != null) player.sendSystemMessage(Component.literal("Failed to link: one or more positions already has 2 links"));
                            tag.remove("pos1");
                            tag.remove("pos2");
                            return InteractionResult.sidedSuccess(level.isClientSide);
                        }

                        nodeBE1.addLink(pos2);
                        nodeBE2.addLink(pos1);
                        tag.remove("pos1");
                        tag.remove("pos2");
                        if (player != null) player.sendSystemMessage(Component.literal("Linked successfully"));
                        return InteractionResult.sidedSuccess(level.isClientSide);
                    } else {
                        if (player != null) player.sendSystemMessage(Component.literal("Failed to link: one position isn't a valid NetworkNodeBlockEntity or doesn't exist anymore"));
                        tag.remove("pos1");
                        tag.remove("pos2");
                        return InteractionResult.sidedSuccess(level.isClientSide);
                    }
                }

                stack.save(tag);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        return InteractionResult.PASS;
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        CompoundTag tag = stack.getOrCreateTag();
        boolean hasPos1 = tag.contains("pos1", CompoundTag.TAG_INT_ARRAY) && tag.getIntArray("pos1").length >= 3;
        boolean hasPos2 = tag.contains("pos1", CompoundTag.TAG_INT_ARRAY) && tag.getIntArray("pos1").length >= 3;

        if (hasPos1 || hasPos2) {
            return true;
        }

        return super.isFoil(stack);
    }
}
