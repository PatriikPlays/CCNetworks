package one.patriik.ccnetworks;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import one.patriik.ccnetworks.network.CableNetworkManager;

import java.util.logging.LogManager;
import java.util.logging.Logger;

public class CCNetworks implements ModInitializer {
    public static String MOD_ID = "ccnetworks";
    public static final Logger LOGGER = LogManager.getLogManager().getLogger("CCNetworks");

    @Override
    public void onInitialize() {
        Registration.init();

        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            CableNetworkManager.networks.clear();
            CableNetworkManager.networkNodeMap.clear();
        });
    }
}
