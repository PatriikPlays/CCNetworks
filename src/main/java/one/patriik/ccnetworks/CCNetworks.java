package one.patriik.ccnetworks;

import dan200.computercraft.api.ComputerCraftAPI;
import net.fabricmc.api.ModInitializer;

public class CCNetworks implements ModInitializer {
    public static String MOD_ID = "ccnetworks";

    @Override
    public void onInitialize() {
        Registration.init();
    }
}
