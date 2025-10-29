package one.patriik.ccnetworks;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import one.patriik.ccnetworks.network.CableNetworkManager;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Map;

public class CCNetworks implements ModInitializer {
    public static String MOD_ID = "ccnetworks";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static Map<ResourceKey<Level>, CableNetworkManager> cableNetworkManagers = new HashMap<>();

    public static CableNetworkManager getCableNetworkManager(MinecraftServer server, ServerLevel level) {
        if (!cableNetworkManagers.containsKey(level.dimension())) { // i dont think this is necessary, but it probably doesnt hurt anything
            CableNetworkManager manager = new CableNetworkManager(server, level);
            cableNetworkManagers.put(level.dimension(), manager);
        }

        return cableNetworkManagers.get(level.dimension());
    }

    @Override
    public void onInitialize() {
        Registration.init();

        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            cableNetworkManagers.clear();
        });

        ServerWorldEvents.LOAD.register((server, level) -> {
            cableNetworkManagers.remove(level.dimension());
            CableNetworkManager manager = new CableNetworkManager(server, level);
            cableNetworkManagers.put(level.dimension(), manager);

            LOGGER.info("Loaded CableNetworkWorldSavedData for dimension {}", level.dimension().toString());
        });
    }
}
