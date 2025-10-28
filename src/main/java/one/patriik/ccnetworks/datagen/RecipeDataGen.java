package one.patriik.ccnetworks.datagen;

import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.world.item.Items;
import one.patriik.ccnetworks.Registration;

import java.util.function.Consumer;

public class RecipeDataGen extends FabricRecipeProvider {
    public RecipeDataGen(FabricDataOutput output) {
        super(output);
    }

    @Override
    public void buildRecipes(Consumer<FinishedRecipe> consumer) {
        ShapedRecipeBuilder.shaped(RecipeCategory.REDSTONE, Registration.ModBlocks.NETWORK_NODE)
                .pattern("xxx")
                .pattern("xxx")
                .pattern("xxx")
                .define('x', Items.DIRT)
                .unlockedBy("has_iron", has(Items.IRON_INGOT))
                .save(consumer);

        ShapedRecipeBuilder.shaped(RecipeCategory.REDSTONE, Registration.ModBlocks.NETWORK_INTERFACE_NODE)
                .pattern("xxx")
                .pattern("xNx")
                .pattern("xxx")
                .define('N', Registration.ModBlocks.NETWORK_NODE)
                .define('x', Items.DIRT)
                .unlockedBy("has_iron", has(Items.IRON_INGOT))
                .save(consumer);
    }
}
