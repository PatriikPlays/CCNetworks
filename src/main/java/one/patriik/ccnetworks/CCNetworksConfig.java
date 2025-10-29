package one.patriik.ccnetworks;

import folk.sisby.kaleido.api.WrappedConfig;
import folk.sisby.kaleido.lib.quiltconfig.api.annotations.Comment;
import folk.sisby.kaleido.lib.quiltconfig.api.annotations.IntegerRange;

public class CCNetworksConfig extends WrappedConfig {
    @Comment("Max length cables can reach")
    @IntegerRange(min=0, max=Integer.MAX_VALUE)
    public int maxCableLength = 64;
}