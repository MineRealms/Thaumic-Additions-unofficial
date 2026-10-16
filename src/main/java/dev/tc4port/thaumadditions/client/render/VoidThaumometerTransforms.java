package dev.tc4port.thaumadditions.client.render;

import net.minecraft.world.item.ItemDisplayContext;

/**
 * How the void thaumometer sits in each place it can be seen, copied from the transforms its 1.12
 * model carried.
 *
 * <p>1.12 could say this in the model's own file, and did: the blockstate listed a transform per
 * context, including separate ones for the left hand. A 1.20.1 renderer gets no such help — an item
 * model of {@code builtin/entity} carries no display transforms, so whatever draws the model has to
 * place it too. These are upstream's numbers, in the units upstream wrote them: translations in
 * sixteenths of a block, rotations in degrees, applied translate, then rotate, then scale.</p>
 *
 * <p>Note that the numbers are asymmetric between the hands, which is upstream's doing and the
 * reason a left-handed player saw it the other way round in 1.12 as well.</p>
 */
public final class VoidThaumometerTransforms {
    private VoidThaumometerTransforms() {
    }

    /** Where and how the model goes for one context. */
    public record Place(float tx, float ty, float tz, float sx, float sy, float sz,
                        float rotX, float rotY, float rotZ) {
    }

    private static final Place IDENTITY = new Place(0.0F, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F,
            0.0F, 0.0F, 0.0F);

    /** The 1.12 blockstate's numbers, per context. */
    public static Place forContext(ItemDisplayContext context) {
        boolean left = context == ItemDisplayContext.FIRST_PERSON_LEFT_HAND
                || context == ItemDisplayContext.THIRD_PERSON_LEFT_HAND;
        return switch (context) {
            case FIRST_PERSON_LEFT_HAND -> new Place(-1.06F, 0.006F, -0.4F, 1, 1, 1, 90, 0, 0);
            case FIRST_PERSON_RIGHT_HAND -> new Place(-0.06F, 0.006F, -0.4F, 1, 1, 1, 90, 0, 0);
            case THIRD_PERSON_LEFT_HAND -> new Place(0.0F, -0.1F, 0.0F, 0.2F, 0.2F, 0.2F, 90, 90, 0);
            case THIRD_PERSON_RIGHT_HAND -> new Place(0.0F, 0.0F, -0.1F, 0.2F, 0.2F, 0.2F, 90, 90, 0);
            case GUI -> new Place(0.175F, -0.175F, 0.0F, 0.35F, 0.35F, 0.35F, 90, 0, 0);
            case GROUND -> new Place(0.1F, 0.1F, 0.1F, 0.2F, 0.2F, 0.2F, 90, 0, 0);
            case FIXED -> new Place(0.2F, 0.2F, -0.17F, 0.4F, 0.4F, 0.4F, -90, 0, 0);
            default -> IDENTITY;
        };
    }

    /** Whether this context is one of the held ones, which are the two the player sees most. */
    public static boolean isHeld(ItemDisplayContext context) {
        return context.firstPerson() || context == ItemDisplayContext.THIRD_PERSON_LEFT_HAND
                || context == ItemDisplayContext.THIRD_PERSON_RIGHT_HAND;
    }
}
