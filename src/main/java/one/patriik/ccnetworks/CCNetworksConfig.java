package one.patriik.ccnetworks;

import folk.sisby.kaleido.api.WrappedConfig;
import folk.sisby.kaleido.lib.quiltconfig.api.annotations.Comment;
import folk.sisby.kaleido.lib.quiltconfig.api.annotations.IntegerRange;

public class CCNetworksConfig extends WrappedConfig {
    @Comment("Max amount of nodes that can be connected to another node (if this is lowered, existing connections don't get removed)")
    @IntegerRange(min=1, max=255)
    public int maxConnectionsPerNode = 4;

    @Comment("Max length cables can reach")
    @IntegerRange(min=0, max=Integer.MAX_VALUE)
    public int maxCableLength = 64;

    //@Comment("If enabled, this prevents players from connecting cables through solid blocks")
    //public boolean doCableCollisionChecking = true;

    //@Comment("If enabled, this lets creative mode players ignore the doCableCollisionChecking setting")
    //public boolean ignoreCableCollisionCheckingForCreative = false;
}