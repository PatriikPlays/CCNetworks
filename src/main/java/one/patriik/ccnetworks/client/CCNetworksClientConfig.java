package one.patriik.ccnetworks.client;

import folk.sisby.kaleido.api.WrappedConfig;
import folk.sisby.kaleido.lib.quiltconfig.api.annotations.Comment;
import folk.sisby.kaleido.lib.quiltconfig.api.annotations.FloatRange;
import folk.sisby.kaleido.lib.quiltconfig.api.annotations.IntegerRange;

public class CCNetworksClientConfig extends WrappedConfig {
    @Comment("Maximum distance in blocks at which network nodes render their cables")
    @FloatRange(min = 0.0, max = 65536.0)
    public float maxNodeRenderDistance = 256.0f;

    @Comment("Maximum straight-line cable length in blocks rendered from each endpoint")
    @FloatRange(min = 0.0, max = 65536.0)
    public float maxCableHalfLength = 512.0f;

    @Comment("Target cable segment length in blocks, measured along the curved cable")
    @FloatRange(min = 0.1, max = 65536.0)
    public float targetCableSegmentLength = 4.0f;

    @Comment("Maximum number of rendered segments per cable half")
    @IntegerRange(min = 1, max = 65536)
    public int maxSegmentsPerHalf = 64;
}
