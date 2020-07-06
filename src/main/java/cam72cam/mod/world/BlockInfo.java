package cam72cam.mod.world;

import net.minecraft.block.BlockState;
import net.minecraft.nbt.NbtHelper;
import cam72cam.mod.serialization.SerializationException;
import cam72cam.mod.serialization.TagCompound;
import cam72cam.mod.serialization.TagField;
import cam72cam.mod.serialization.TagMapped;

@TagMapped(BlockInfo.TagMapper.class)
public class BlockInfo {
    final BlockState internal;

    BlockInfo(BlockState state) {
        this.internal = state;
    }

    public static class TagMapper implements cam72cam.mod.serialization.TagMapper<BlockInfo> {
        @Override
        public TagAccessor<BlockInfo> apply(Class<BlockInfo> type, String fieldName, TagField tag) throws SerializationException {
            return new TagAccessor<>(
                    (d, o) -> {
                        if (o == null) {
                            d.remove(fieldName);
                            return;
                        }
                        d.set(fieldName, new TagCompound(NbtHelper.fromBlockState(o.internal)));
                    },
                    info -> new BlockInfo(NbtHelper.toBlockState(info.internal))
            );
        }
    }
}
