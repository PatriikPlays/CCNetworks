package one.patriik.ccnetworks.client;

import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import one.patriik.ccnetworks.Registration;
import one.patriik.ccnetworks.render.NetworkNodeBlockEntityRenderer;

public class CCNetworksClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        BlockEntityRenderers.register(Registration.ModBlockEntities.NETWORK_NODE, NetworkNodeBlockEntityRenderer::new);
    }
}
