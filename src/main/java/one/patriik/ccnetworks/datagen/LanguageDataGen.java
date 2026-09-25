package one.patriik.ccnetworks.datagen;

import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricLanguageProvider;
import net.minecraft.core.HolderLookup;
import one.patriik.ccnetworks.Registration;

import java.util.concurrent.CompletableFuture;

public class LanguageDataGen extends FabricLanguageProvider {
    protected LanguageDataGen(FabricDataOutput dataOutput, CompletableFuture<HolderLookup.Provider> registriesFuture) {
        super(dataOutput, registriesFuture);
    }

    @Override
    public void generateTranslations(HolderLookup.Provider registries, TranslationBuilder translationBuilder) {
        translationBuilder.add(Registration.ModItems.FIBER_OPTIC_CABLE, "Fiber Optic Cable");

        translationBuilder.add(Registration.ModBlocks.NETWORK_NODE, "Network Node");
        translationBuilder.add(Registration.ModBlocks.NETWORK_INTERFACE_NODE, "Network Interface Node");

        translationBuilder.add(Registration.ModBlocks.SILICA_BLEND, "Silica Blend");
        translationBuilder.add(Registration.ModBlocks.FUSED_SILICA, "Fused Silica");

        translationBuilder.add("itemGroup.ccnetworks", "CC:Networks");
    }
}
