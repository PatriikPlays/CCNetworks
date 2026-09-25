package one.patriik.ccnetworks.datagen;

import dan200.computercraft.api.ComputerCraftTags;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.recipes.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import one.patriik.ccnetworks.CCNetworks;
import one.patriik.ccnetworks.Registration;

import java.util.concurrent.CompletableFuture;

public class RecipeDataGen extends FabricRecipeProvider {
    public RecipeDataGen(FabricDataOutput output, CompletableFuture<HolderLookup.Provider> registriesFuture) {
        super(output, registriesFuture);
    }

    @Override
    public void buildRecipes(RecipeOutput consumer) {
        ShapedRecipeBuilder.shaped(RecipeCategory.REDSTONE, Registration.ModItems.FIBER_OPTIC_CABLE, 64)
                .pattern(" W ")
                .pattern("FFF")
                .pattern(" W ")
                .define('F', Registration.ModBlocks.FUSED_SILICA)
                .define('W', ItemTags.WOOL)
                .unlockedBy("has_fused_silica", has(Registration.ModBlocks.FUSED_SILICA))
                .save(consumer, ResourceLocation.fromNamespaceAndPath(CCNetworks.MOD_ID, "fiber_optic_cable_from_wool"));
        ShapedRecipeBuilder.shaped(RecipeCategory.REDSTONE, Registration.ModItems.FIBER_OPTIC_CABLE, 64)
                .pattern("SSS")
                .pattern("FFF")
                .pattern("SSS")
                .define('F', Registration.ModBlocks.FUSED_SILICA)
                .define('S', Items.STRING)
                .unlockedBy("has_fused_silica", has(Registration.ModBlocks.FUSED_SILICA))
                .save(consumer, ResourceLocation.fromNamespaceAndPath(CCNetworks.MOD_ID, "fiber_optic_cable_from_string"));

        ShapedRecipeBuilder.shaped(RecipeCategory.REDSTONE, Registration.ModBlocks.NETWORK_NODE)
                .pattern(" F ")
                .pattern("SDS")
                .pattern("SRS")
                .define('F', Registration.ModBlocks.FUSED_SILICA)
                .define('S', Items.STONE)
                .define('R', Items.REDSTONE)
                .define('D', Items.DIAMOND)
                .unlockedBy("has_redstone", has(Items.REDSTONE))
                .save(consumer);

        ShapelessRecipeBuilder.shapeless(RecipeCategory.REDSTONE, Registration.ModBlocks.NETWORK_INTERFACE_NODE)
                .requires(Registration.ModBlocks.NETWORK_NODE)
                .requires(ComputerCraftTags.Items.WIRED_MODEM)
                .unlockedBy("has_redstone", has(Items.REDSTONE))
                .save(consumer);


        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, Registration.ModBlocks.SILICA_BLEND, 2)
                .requires(Items.QUARTZ_BLOCK)
                .requires(net.minecraft.tags.ItemTags.SAND)
                .unlockedBy("has_quartz_block", has(Items.QUARTZ_BLOCK))
                .save(consumer);

        SimpleCookingRecipeBuilder.smelting(
                Ingredient.of(Registration.ModBlocks.SILICA_BLEND),
                RecipeCategory.MISC,
                Registration.ModBlocks.FUSED_SILICA,
                0.1F,
                200
        ).unlockedBy("has_silica_blend", has(Registration.ModBlocks.SILICA_BLEND))
        .save(consumer, ResourceLocation.fromNamespaceAndPath(CCNetworks.MOD_ID, "fused_silica_from_smelting_silica_blend"));
        SimpleCookingRecipeBuilder.blasting(
                Ingredient.of(Registration.ModBlocks.SILICA_BLEND),
                RecipeCategory.MISC,
                Registration.ModBlocks.FUSED_SILICA,
                0.1F,
                100
        ).unlockedBy("has_silica_blend", has(Registration.ModBlocks.SILICA_BLEND))
        .save(consumer, ResourceLocation.fromNamespaceAndPath(CCNetworks.MOD_ID, "fused_silica_from_blasting_silica_blend"));
    }
}
