package one.patriik.ccnetworks.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import one.patriik.ccnetworks.Registration;
import one.patriik.ccnetworks.render.AbstractNetworkNodeBlockEntityRenderer;

import java.nio.file.Paths;

public class CCNetworksClient implements ClientModInitializer {
    public static final CCNetworksClientConfig CONFIG = CCNetworksClientConfig.createToml(
            Paths.get("config"),
            "",
            "ccnetworks-client",
            CCNetworksClientConfig.class
    );

    @Override
    public void onInitializeClient() {
        BlockEntityRenderers.register(Registration.ModBlockEntities.NETWORK_NODE, AbstractNetworkNodeBlockEntityRenderer::new);
        BlockEntityRenderers.register(Registration.ModBlockEntities.NETWORK_INTERFACE_NODE, AbstractNetworkNodeBlockEntityRenderer::new);

        BlockRenderLayerMap.INSTANCE.putBlock(
                Registration.ModBlocks.FUSED_SILICA,
                net.minecraft.client.renderer.RenderType.cutout()
        );
    }
}
