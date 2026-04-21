package one.patriik.ccnetworks.datagen;

import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricModelProvider;
import net.minecraft.data.models.BlockModelGenerators;
import net.minecraft.data.models.ItemModelGenerators;
import net.minecraft.data.models.model.ModelTemplates;
import one.patriik.ccnetworks.Registration;

import static net.minecraft.data.models.model.ModelLocationUtils.getModelLocation;

public class ModelDataGen extends FabricModelProvider {
    public ModelDataGen(FabricDataOutput output) {
        super(output);
    }

    @Override
    public void generateBlockStateModels(BlockModelGenerators blockModelGenerators) {
        blockModelGenerators.delegateItemModel(Registration.ModBlocks.FUSED_SILICA, getModelLocation(Registration.ModBlocks.FUSED_SILICA));
        blockModelGenerators.delegateItemModel(Registration.ModBlocks.NETWORK_NODE, getModelLocation(Registration.ModBlocks.NETWORK_NODE));
        blockModelGenerators.delegateItemModel(Registration.ModBlocks.NETWORK_INTERFACE_NODE, getModelLocation(Registration.ModBlocks.NETWORK_INTERFACE_NODE));
    }

    @Override
    public void generateItemModels(ItemModelGenerators itemModelGenerators) {
        itemModelGenerators.generateFlatItem(Registration.ModItems.FIBER_OPTIC_CABLE, ModelTemplates.FLAT_ITEM);
    }
}
