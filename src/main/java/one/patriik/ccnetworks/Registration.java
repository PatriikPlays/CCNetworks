package one.patriik.ccnetworks;

import dan200.computercraft.api.peripheral.PeripheralLookup;
import net.fabricmc.fabric.api.lookup.v1.block.BlockApiLookup;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import one.patriik.ccnetworks.block.NetworkNodeBlock;
import one.patriik.ccnetworks.blockentity.NetworkNodeBlockEntity;
import one.patriik.ccnetworks.item.FiberOpticCable;
import one.patriik.ccnetworks.peripherals.NetworkNodePeripheral;
import org.jetbrains.annotations.Nullable;

import java.util.function.BiFunction;

public final class Registration {
    public static final class ModBlockEntities {
        public static final BlockEntityType<NetworkNodeBlockEntity> NETWORK_NODE = register("network_node", FabricBlockEntityTypeBuilder.create(NetworkNodeBlockEntity::new, ModBlocks.NETWORK_NODE).build());

        public static <T extends BlockEntityType<?>> T register(String path, T blockEntityType) {
            return Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, new ResourceLocation(CCNetworks.MOD_ID, path), blockEntityType);
        }

        public static void init() {}
    }


    public static final class ModBlocks {
        public static final NetworkNodeBlock NETWORK_NODE = register("network_node", new NetworkNodeBlock(BlockBehaviour.Properties.of()
                .strength(2.0F)
                .mapColor(MapColor.STONE)
                .noOcclusion()
                .isValidSpawn(Blocks::never)
                .isRedstoneConductor(Blocks::never)
                .isSuffocating(Blocks::never)
                .isViewBlocking(Blocks::never)
        ), true);

        public static <T extends Block> T register(String name, T block, boolean shouldRegisterItem) {
            ResourceLocation id = new ResourceLocation(CCNetworks.MOD_ID, name);

            if (shouldRegisterItem) {
                BlockItem blockItem = new BlockItem(block, new Item.Properties());
                Registry.register(BuiltInRegistries.ITEM, id, (Item)blockItem);
            }

            return Registry.register(BuiltInRegistries.BLOCK, id, block);
        }

        public static void init() {}
    }


    public static final class ModItems {
        public static final Item FIBER_OPTIC_CABLE = register("fiber_optic_cable", new FiberOpticCable(new Item.Properties()));

        public static <T extends Item> T register(String name, T item) {
            ResourceLocation id = new ResourceLocation(CCNetworks.MOD_ID, name);
            return Registry.register(BuiltInRegistries.ITEM, id, item);
        }

        public static void init() {}
    }

    public static final class ModPeripherals {
        public static void init() {
            var peripherals = PeripheralLookup.get();
            peripherals.registerForBlockEntity((b, s) -> b.getPeripheral(), ModBlockEntities.NETWORK_NODE);
        }
    }


    public static void init() {
        ModBlockEntities.init();
        ModBlocks.init();
        ModItems.init();
        ModPeripherals.init();
    }
}

