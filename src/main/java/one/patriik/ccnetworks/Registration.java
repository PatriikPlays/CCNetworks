package one.patriik.ccnetworks;

import dan200.computercraft.api.peripheral.PeripheralLookup;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import one.patriik.ccnetworks.block.NetworkInterfaceNodeBlock;
import one.patriik.ccnetworks.block.NetworkNodeBlock;
import one.patriik.ccnetworks.blockentity.NetworkInterfaceNodeBlockEntity;
import one.patriik.ccnetworks.blockentity.NetworkNodeBlockEntity;
import one.patriik.ccnetworks.item.FiberOpticCable;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.signature.qual.Identifier;

public final class Registration {
    public static final class ModBlockEntities {
        public static final BlockEntityType<NetworkNodeBlockEntity> NETWORK_NODE = register("network_node", FabricBlockEntityTypeBuilder.create((blockPos, blockState) -> new NetworkNodeBlockEntity(Registration.ModBlockEntities.NETWORK_NODE, blockPos, blockState), ModBlocks.NETWORK_NODE).build());
        public static final BlockEntityType<NetworkInterfaceNodeBlockEntity> NETWORK_INTERFACE_NODE = register("network_interface_node", FabricBlockEntityTypeBuilder.create((blockPos, blockState) -> new NetworkInterfaceNodeBlockEntity(Registration.ModBlockEntities.NETWORK_INTERFACE_NODE, blockPos, blockState), ModBlocks.NETWORK_INTERFACE_NODE).build());

        public static <T extends BlockEntityType<?>> T register(String path, T blockEntityType) {
            return Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, new ResourceLocation(CCNetworks.MOD_ID, path), blockEntityType);
        }

        public static void init() {}
    }


    public static final class ModBlocks {
        public static final NetworkNodeBlock NETWORK_NODE = register("network_node", new NetworkNodeBlock(BlockBehaviour.Properties.of()
                .strength(0.5F)
                .mapColor(MapColor.STONE)
                .noOcclusion()
                .isValidSpawn(Blocks::never)
                .isRedstoneConductor(Blocks::never)
                .isSuffocating(Blocks::never)
                .isViewBlocking(Blocks::never)
        ), true);

        public static final NetworkInterfaceNodeBlock NETWORK_INTERFACE_NODE = register("network_interface_node", new NetworkInterfaceNodeBlock(BlockBehaviour.Properties.of()
                .strength(0.5F)
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
            peripherals.registerForBlockEntity((b, s) -> b.getPeripheral(), ModBlockEntities.NETWORK_INTERFACE_NODE);
        }
    }

    public static final class ModCreativeTabs {
        public static final CreativeModeTab CCNETWORKS_TAB = Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB,
                new ResourceLocation(CCNetworks.MOD_ID, "ccnetworks"),
                FabricItemGroup.builder()
                    .icon(() -> new ItemStack(ModBlocks.NETWORK_INTERFACE_NODE))
                    .title(Component.translatable("itemGroup.ccnetworks"))
                    .build()
        );

        public static void registerItems() {
            // this seems like a way youre not supposed to do this, but i cant find anything else
            ItemGroupEvents.MODIFY_ENTRIES_ALL.register((tab, entries) -> {
                if (tab == CCNETWORKS_TAB) {
                    entries.accept(ModBlocks.NETWORK_NODE);
                    entries.accept(ModBlocks.NETWORK_INTERFACE_NODE);
                    entries.accept(ModItems.FIBER_OPTIC_CABLE);
                }
            });
        }

        public static void init() {}
    }


    public static void init() {
        ModBlockEntities.init();
        ModBlocks.init();
        ModItems.init();
        ModPeripherals.init();

        ModCreativeTabs.init();
        ModCreativeTabs.registerItems();
    }
}

