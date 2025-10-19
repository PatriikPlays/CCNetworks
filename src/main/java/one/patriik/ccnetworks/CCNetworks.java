package one.patriik.ccnetworks;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import one.patriik.ccnetworks.network.CableNetworkManager;
import one.patriik.ccnetworks.network.CableNetworkWorldSavedData;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

public class CCNetworks implements ModInitializer {
    public static String MOD_ID = "ccnetworks";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        Registration.init();

        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            CableNetworkManager.networks.clear();
            CableNetworkManager.networkNodeMap.clear();
        });

        ServerWorldEvents.LOAD.register((server, level) -> {
            CableNetworkWorldSavedData networkSavedData = CableNetworkWorldSavedData.getDimensionSavedData(server, level.dimension());
            LOGGER.info("Loaded CableNetworkWorldSavedData for dimension {}", level.dimension().toString());
        });
    }
}
