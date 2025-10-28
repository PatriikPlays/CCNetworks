package one.patriik.ccnetworks.datagen;

import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricLanguageProvider;
import one.patriik.ccnetworks.Registration;

public class LanguageDataGen extends FabricLanguageProvider {
    protected LanguageDataGen(FabricDataOutput dataOutput) {
        super(dataOutput);
    }

    @Override
    public void generateTranslations(TranslationBuilder translationBuilder) {
        translationBuilder.add(Registration.ModItems.FIBER_OPTIC_CABLE, "Fiber Optic Cable");

        translationBuilder.add(Registration.ModBlocks.NETWORK_NODE, "Network Node");
        translationBuilder.add(Registration.ModBlocks.NETWORK_INTERFACE_NODE, "Network Interface Node");

        translationBuilder.add("itemGroup.ccnetworks", "CC:Networks");
    }
}
