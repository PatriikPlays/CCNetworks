package one.patriik.ccnetworks.datagen;

import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricBlockLootTableProvider;
import one.patriik.ccnetworks.Registration;

public class LootTableDataGen extends FabricBlockLootTableProvider {
    protected LootTableDataGen(FabricDataOutput dataOutput) {
        super(dataOutput);
    }

    @Override
    public void generate() {
        this.dropSelf(Registration.ModBlocks.NETWORK_NODE);
        this.dropSelf(Registration.ModBlocks.NETWORK_INTERFACE_NODE);
    }
}
