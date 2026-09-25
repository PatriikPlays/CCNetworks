package one.patriik.ccnetworks.datagen;

import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricBlockLootTableProvider;
import net.minecraft.core.HolderLookup;
import one.patriik.ccnetworks.Registration;

import java.util.concurrent.CompletableFuture;

public class LootTableDataGen extends FabricBlockLootTableProvider {
    protected LootTableDataGen(FabricDataOutput dataOutput, CompletableFuture<HolderLookup.Provider> registriesFuture) {
        super(dataOutput, registriesFuture);
    }

    @Override
    public void generate() {
        this.dropSelf(Registration.ModBlocks.SILICA_BLEND);
        this.dropSelf(Registration.ModBlocks.FUSED_SILICA);
        this.dropSelf(Registration.ModBlocks.NETWORK_NODE);
        this.dropSelf(Registration.ModBlocks.NETWORK_INTERFACE_NODE);
    }
}
