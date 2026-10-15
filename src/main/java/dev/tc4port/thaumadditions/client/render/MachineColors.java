package dev.tc4port.thaumadditions.client.render;

import dev.tc4port.thaumadditions.aspect.TaAspect;
import dev.tc4port.thaumadditions.tiles.TileCrystal;
import dev.tc4port.thaumcraft.api.aspect.AspectId;
import net.minecraft.client.Minecraft;
import net.minecraft.client.color.block.BlockColor;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import java.util.List;

/**
 * The one thing 1.12 drew with a colour instead of a model: the arcane cake's top.
 *
 * <p>Upstream's {@code TESRArcaneCake} drew a single lit quad over the cake in the colour of the
 * aspects that had been eaten. 1.20.1 has a cheaper way to say the same thing — the block's top
 * face carries a tint index, and this provider answers for it — which also means the colour is lit
 * and culled like any other block face instead of being a separate translucent pass.</p>
 */
public final class MachineColors {
    private MachineColors() {
    }

    /** Tint index 0 is the cake's top face; nothing else in the mod uses a tint index. */
    public static final int CAKE_TOP = 0;

    /** The colour of the most recently eaten aspect, or white when nobody has taken a bite. */
    public static final BlockColor CAKE = new BlockColor() {
        @Override
        public int getColor(BlockState state, @Nullable BlockAndTintGetter level, @Nullable BlockPos pos,
                            int tintIndex) {
            if (tintIndex != CAKE_TOP || pos == null) {
                return 0xFFFFFF;
            }
            // The block entity is only there once the client has loaded the chunk it sits in;
            // before that the cake is drawn plain rather than guessed at.
            if (Minecraft.getInstance().level == null
                    || !(Minecraft.getInstance().level.getBlockEntity(pos) instanceof TileCrystal.Cake cake)) {
                return 0xFFFFFF;
            }
            List<AspectId> eaten = cake.aspects();
            if (eaten.isEmpty()) {
                return 0xFFFFFF;
            }
            return TaAspect.color(eaten.get(eaten.size() - 1));
        }
    };
}
