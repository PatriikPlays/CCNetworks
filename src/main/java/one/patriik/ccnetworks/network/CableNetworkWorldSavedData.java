package one.patriik.ccnetworks.network;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.NotNull;

public class CableNetworkWorldSavedData extends SavedData {
    private static final Factory<CableNetworkWorldSavedData> FACTORY =
            new Factory<>(CableNetworkWorldSavedData::new, CableNetworkWorldSavedData::new, null);

    private CompoundTag data = new CompoundTag();

    private CableNetworkWorldSavedData() {
        this.setDirty();
    }

    private CableNetworkWorldSavedData(CompoundTag nbt, HolderLookup.Provider registries) {
        if (nbt.contains("data")) {
            this.data = nbt.getCompound("data");
        }
    }

    public void setData(CompoundTag newData) {
        this.data = newData;
        this.setDirty();
    }

    public CompoundTag getData() {
        return data;
    }

    public static CableNetworkWorldSavedData getDimensionSavedData(MinecraftServer server, ResourceKey<Level> dimension) {
        ServerLevel level = server.getLevel(dimension);
        if (level == null) throw new IllegalStateException("World not loaded: " + dimension);

        CableNetworkWorldSavedData state = level.getDataStorage().computeIfAbsent(
                FACTORY,
                "cable_network_data"
        );

        state.setDirty();
        return state;
    }

    @Override
    public @NotNull CompoundTag save(CompoundTag compoundTag, HolderLookup.Provider registries) {
        compoundTag.put("data", data.copy());
        return compoundTag;
    }
}
