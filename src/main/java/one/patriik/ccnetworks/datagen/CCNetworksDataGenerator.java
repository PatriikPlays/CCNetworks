package one.patriik.ccnetworks.datagen;

import net.fabricmc.fabric.api.datagen.v1.DataGeneratorEntrypoint;
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator;

public class CCNetworksDataGenerator implements DataGeneratorEntrypoint {

    @Override
    public void onInitializeDataGenerator(FabricDataGenerator fabricDataGenerator) {
        FabricDataGenerator.Pack pack = fabricDataGenerator.createPack();
        pack.addProvider(ModelDataGen::new);
        pack.addProvider(LanguageDataGen::new);
        pack.addProvider(LootTableDataGen::new);
        pack.addProvider(RecipeDataGen::new); // no recipes yet
        pack.addProvider(BlockTagDataGen::new);
    }
}
