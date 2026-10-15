package dev.tc4port.thaumadditions.client.model;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;

/**
 * The five machine models, rebuilt from the 1.12 {@code ModelBase} classes.
 *
 * <p>The 1.12 shape maps onto 1.20.1 one-to-one: {@code ModelRenderer(parent, u, v)} is
 * {@code CubeListBuilder.texOffs(u, v)}, {@code setRotationPoint(x, y, z)} is
 * {@code PartPose.offset(x, y, z)}, {@code addBox(...)} keeps its arguments, and
 * {@code addChild} is {@code PartDefinition.addOrReplaceChild}. Both spaces are in 1/16-block
 * model units with the box origin at the part's rotation point, so the numbers carry over
 * unchanged.</p>
 *
 * <p>Transforms the 1.12 renderers applied with {@code GL11.glTranslatef} around the whole model
 * (the bore's {@code -0.5} drop, the crusher's 1/16 lift on its head) are applied to the pose
 * stack by the renderers instead, so the shapes here stay faithful to the originals.</p>
 */
public final class MachineModels {
    private MachineModels() {
    }

    /** 1.12 {@code ModelCrystalBore}: body, skirt and a drill head that spins. */
    public static final class CrystalBore {
        public static final ResourceLocation TEXTURE = texture("crystal_bore");
        public static final int TEXTURE_WIDTH = 128;
        public static final int TEXTURE_HEIGHT = 64;

        private final ModelPart root;
        private final ModelPart rotational;

        public CrystalBore(ModelPart root) {
            this.root = root;
            this.rotational = root.getChild("rotational");
        }

        public static LayerDefinition layer() {
            MeshDefinition mesh = new MeshDefinition();
            PartDefinition root = mesh.getRoot();
            PartDefinition staticParts = root.addOrReplaceChild("static",
                    CubeListBuilder.create(), PartPose.ZERO);
            PartDefinition rotational = root.addOrReplaceChild("rotational",
                    box(84, 0, 0, 0, 0, 8, 3, 8), PartPose.offset(-4.0F, 15.0F, -4.0F));
            PartDefinition shape4 = rotational.addOrReplaceChild("shape4",
                    box(91, 11, 0, 0, 0, 5, 4, 5), PartPose.offset(1.5F, -4.0F, 1.5F));
            PartDefinition shape5 = shape4.addOrReplaceChild("shape5",
                    box(0, 0, 0, 0, 0, 3, 2, 3), PartPose.offset(1.0F, -2.0F, 1.0F));
            shape5.addOrReplaceChild("shape6",
                    box(12, 0, 0, 0, 0, 1, 3, 1), PartPose.offset(1.0F, -3.0F, 1.0F));
            staticParts.addOrReplaceChild("shape1",
                    box(0, 0, 0, 0, 0, 16, 2, 16), PartPose.offset(-8.0F, 22.0F, -8.0F));
            staticParts.addOrReplaceChild("shape2",
                    box(48, 0, 0, 0, 0, 12, 4, 12), PartPose.offset(-6.0F, 18.0F, -6.0F));
            return LayerDefinition.create(mesh, TEXTURE_WIDTH, TEXTURE_HEIGHT);
        }

        public ModelPart root() {
            return root;
        }

        /** The drill head; the 1.12 renderer rotates it about Y. */
        /** The parts that never move; the renderer draws these once, outside the spin. */
        public ModelPart staticParts() {
            return root.getChild("static");
        }

        public ModelPart rotator() {
            return rotational;
        }
    }

    /** 1.12 {@code ModelAspectCombiner}: two side tanks feeding a central pot. */
    public static final class AspectCombiner {
        public static final ResourceLocation TEXTURE = texture("aspect_combiner");
        // The 1.12 texture is 128 square; declaring it 64 scaled every UV to the wrong texel.
        public static final int TEXTURE_WIDTH = 128;
        public static final int TEXTURE_HEIGHT = 128;

        private final ModelPart root;
        private final ModelPart rotor;

        public AspectCombiner(ModelPart root) {
            this.root = root;
            this.rotor = root.getChild("shape2");
        }

        public static LayerDefinition layer() {
            MeshDefinition mesh = new MeshDefinition();
            PartDefinition root = mesh.getRoot();
            PartDefinition staticParts = root.addOrReplaceChild("static",
                    CubeListBuilder.create(), PartPose.ZERO);
            root.addOrReplaceChild("shape2",
                    box(24, 0, 0, 0, 0, 5, 3, 5), PartPose.offset(-2.5F, 10.0F, -2.5F));
            staticParts.addOrReplaceChild("shape1",
                    box(0, 0, 0, 0, 0, 6, 2, 6), PartPose.offset(-3.0F, 8.0F, -3.0F));
            staticParts.addOrReplaceChild("shape4",
                    box(44, 0, 0, 0, 0, 5, 4, 4), PartPose.offset(-8.0F, 14.0F, -2.0F));
            staticParts.addOrReplaceChild("shape4_1",
                    box(0, 8, 0, 0, 0, 5, 4, 4), PartPose.offset(3.0F, 14.0F, -2.0F));
            staticParts.addOrReplaceChild("shape7",
                    box(18, 8, 0, 0, 0, 6, 6, 6), PartPose.offset(-3.0F, 13.0F, -3.0F));
            return LayerDefinition.create(mesh, TEXTURE_WIDTH, TEXTURE_HEIGHT);
        }

        public ModelPart root() {
            return root;
        }

        /** The parts that never move; the renderer draws these once, outside the spin. */
        public ModelPart staticParts() {
            return root.getChild("static");
        }

        /** The knob; the 1.12 renderer draws it twice, the second time 45° further round. */
        public ModelPart rotor() {
            return rotor;
        }
    }

    /** 1.12 {@code ModelAuraCharger}: a spout whose four sprinkler arms hang off the base. */
    public static final class AuraCharger {
        public static final ResourceLocation TEXTURE = texture("aura_charger");
        // The 1.12 texture is 64 square; declaring it 32 scaled every UV to the wrong texel.
        public static final int TEXTURE_WIDTH = 64;
        public static final int TEXTURE_HEIGHT = 64;

        private final ModelPart root;
        private final ModelPart rotor;

        public AuraCharger(ModelPart root) {
            this.root = root;
            this.rotor = root.getChild("shape2");
        }

        public static LayerDefinition layer() {
            MeshDefinition mesh = new MeshDefinition();
            PartDefinition root = mesh.getRoot();
            PartDefinition staticParts = root.addOrReplaceChild("static",
                    CubeListBuilder.create(), PartPose.ZERO);
            PartDefinition base = root.addOrReplaceChild("shape2",
                    box(12, 0, 0, 0, 0, 4, 2, 4), PartPose.offset(-2.0F, 18.0F, -2.0F));
            base.addOrReplaceChild("shape5",
                    box(9, 0, 0, 0, 0, 2, 1, 1), PartPose.offset(-4.0F, -2.0F, 1.0F));
            base.addOrReplaceChild("shape5_1",
                    box(24, 0, 0, 0, 0, 2, 1, 1), PartPose.offset(6.0F, -2.0F, 2.0F));
            base.addOrReplaceChild("shape3",
                    box(10, 6, 0, 0, 0, 2, 1, 2), PartPose.offset(-2.0F, 0.0F, 1.0F));
            base.addOrReplaceChild("shape4",
                    box(18, 6, 0, 0, 0, 3, 1, 2), PartPose.offset(-4.0F, -1.0F, 1.0F));
            base.addOrReplaceChild("shape3_1",
                    box(0, 7, 0, 0, 0, 2, 1, 2), PartPose.offset(4.0F, 0.0F, 1.0F));
            base.addOrReplaceChild("shape4_1",
                    box(6, 9, 0, 0, 0, 3, 1, 2), PartPose.offset(5.0F, -1.0F, 1.0F));
            staticParts.addOrReplaceChild("shape1",
                    box(0, 0, 0, 0, 0, 3, 4, 3), PartPose.offset(-1.5F, 20.0F, -1.5F));
            return LayerDefinition.create(mesh, TEXTURE_WIDTH, TEXTURE_HEIGHT);
        }

        public ModelPart root() {
            return root;
        }

        /** The parts that never move; the renderer draws these once, outside the spin. */
        public ModelPart staticParts() {
            return root.getChild("static");
        }

        /** The sprinkler arms; the 1.12 renderer rotates this subtree about Y. */
        public ModelPart rotor() {
            return rotor;
        }
    }

    /** 1.12 {@code ModelCrystalCrusher}: a plate, a lifted head carrying the crushing plate. */
    public static final class CrystalCrusher {
        public static final ResourceLocation TEXTURE = texture("crystal_crusher");
        public static final int TEXTURE_WIDTH = 64;
        public static final int TEXTURE_HEIGHT = 64;

        private final ModelPart root;
        private final ModelPart head;

        public CrystalCrusher(ModelPart root) {
            this.root = root;
            this.head = root.getChild("shape3");
        }

        public static LayerDefinition layer() {
            MeshDefinition mesh = new MeshDefinition();
            PartDefinition root = mesh.getRoot();
            PartDefinition staticParts = root.addOrReplaceChild("static",
                    CubeListBuilder.create(), PartPose.ZERO);
            staticParts.addOrReplaceChild("shape1",
                    box(0, 0, 0, 0, 0, 14, 3, 14), PartPose.offset(-7.0F, 21.0F, -7.0F));
            // The 1.12 render lifts this subtree by 1/16 before drawing it, so the head's pose is
            // one model unit higher than the constructor's rotation point.
            PartDefinition head = root.addOrReplaceChild("shape3",
                    box(0, 0, 0, 0, 0, 2, 2, 2), PartPose.offset(-1.0F, 20.0F, -1.0F));
            head.addOrReplaceChild("shape2",
                    box(0, 17, 0, 0, 0, 14, 3, 14), PartPose.offset(-6.0F, -3.0F, -6.0F));
            return LayerDefinition.create(mesh, TEXTURE_WIDTH, TEXTURE_HEIGHT);
        }

        public ModelPart root() {
            return root;
        }

        /** The crushing head; the 1.12 renderer rotates this subtree about Y. */
        /** The parts that never move; the renderer draws these once, outside the spin. */
        public ModelPart staticParts() {
            return root.getChild("static");
        }

        public ModelPart head() {
            return head;
        }
    }

    /** 1.12 {@code ModelFluxConcentrator}: a caged column with a rotating valve wheel. */
    public static final class FluxConcentrator {
        public static final ResourceLocation TEXTURE = texture("flux_concentrator");
        public static final int TEXTURE_WIDTH = 128;
        public static final int TEXTURE_HEIGHT = 64;

        /** The valve turns about this point, which the 1.12 renderer offsets by (1/16, 1/16, 0). */
        private static final float VALVE_PIVOT_X = 0.0F;
        private static final float VALVE_PIVOT_Y = 17.5F;
        private static final float VALVE_PIVOT_Z = -2.2F;

        private final ModelPart root;
        private final ModelPart valve;

        public FluxConcentrator(ModelPart root) {
            this.root = root;
            this.valve = root.getChild("shape18");
        }

        public static LayerDefinition layer() {
            MeshDefinition mesh = new MeshDefinition();
            PartDefinition root = mesh.getRoot();
            PartDefinition staticParts = root.addOrReplaceChild("static",
                    CubeListBuilder.create(), PartPose.ZERO);
            staticParts.addOrReplaceChild("shape6_3", box(100, 0, 0, 0, 0, 1, 14, 1), PartPose.offset(6.5F, 10.0F, 6.5F));
            staticParts.addOrReplaceChild("shape14_1", box(48, 0, 0, 0, 0, 1, 2, 1), PartPose.offset(1.0F, 21.0F, 3.0F));
            staticParts.addOrReplaceChild("shape24", box(74, 8, 0, 0, 0, 1, 1, 14), PartPose.offset(7.0F, 23.0F, -7.0F));
            staticParts.addOrReplaceChild("shape1", box(0, 0, 0, 0, 0, 2, 10, 2), PartPose.offset(-1.0F, 13.0F, -1.0F));
            staticParts.addOrReplaceChild("shape6_1", box(12, 0, 0, 0, 0, 1, 14, 1), PartPose.offset(-7.5F, 10.0F, -7.5F));
            staticParts.addOrReplaceChild("shape2", box(0, 0, 0, 0, 0, 16, 2, 16), PartPose.offset(-8.0F, 8.0F, -8.0F));
            staticParts.addOrReplaceChild("shape6", box(8, 0, 0, 0, 0, 1, 14, 1), PartPose.offset(-7.5F, 10.0F, 6.5F));
            staticParts.addOrReplaceChild("shape6_2", box(96, 0, 0, 0, 0, 1, 14, 1), PartPose.offset(6.5F, 10.0F, -7.5F));
            staticParts.addOrReplaceChild("shape10", box(104, 0, 0, 0, 0, 3, 6, 3), PartPose.offset(0.0F, 15.0F, 2.0F));
            staticParts.addOrReplaceChild("shape1_2", box(80, 0, 0, 0, 0, 4, 1, 4), PartPose.offset(-2.0F, 23.0F, -2.0F));
            staticParts.addOrReplaceChild("shape22", box(48, 9, 0, 0, 0, 16, 1, 1), PartPose.offset(-8.0F, 23.0F, -8.0F));
            staticParts.addOrReplaceChild("shape14", box(116, 3, 0, 0, 0, 1, 4, 1), PartPose.offset(1.0F, 11.0F, 3.0F));
            staticParts.addOrReplaceChild("shape16", box(120, 4, 0, 0, 0, 1, 1, 2), PartPose.offset(1.0F, 23.0F, 2.0F));
            staticParts.addOrReplaceChild("shape17", box(80, 5, 0, 0, 0, 1, 1, 2), PartPose.offset(-0.5F, 17.0F, -2.7F));
            staticParts.addOrReplaceChild("shape11", box(113, 0, 0, 0, 0, 2, 1, 2), PartPose.offset(0.5F, 14.3F, 2.5F));
            staticParts.addOrReplaceChild("shape11_1", box(119, 1, 0, 0, 0, 2, 1, 2), PartPose.offset(0.5F, 20.7F, 2.5F));
            staticParts.addOrReplaceChild("shape22_1", box(48, 11, 0, 0, 0, 16, 1, 1), PartPose.offset(-8.0F, 23.0F, 7.0F));
            staticParts.addOrReplaceChild("shape24_1", box(90, 9, 0, 0, 0, 1, 1, 14), PartPose.offset(-8.0F, 23.0F, -7.0F));
            root.addOrReplaceChild("shape18",
                    box(-1, -1, 0, 0, 0, 2, 2, 1),
                    PartPose.offset(VALVE_PIVOT_X, VALVE_PIVOT_Y, VALVE_PIVOT_Z));
            staticParts.addOrReplaceChild("shape19", box(60, 0, 0, 0, 0, 1, 1, 1), PartPose.offset(-2.0F, 20.0F, 0.0F));
            staticParts.addOrReplaceChild("shape21", box(92, 5, 0, 0, 0, 1, 9, 1), PartPose.offset(-3.0F, 12.0F, 0.0F));
            staticParts.addOrReplaceChild("shape1_1", box(48, 0, 0, 0, 0, 4, 2, 4), PartPose.offset(-2.0F, 11.0F, -2.0F));
            staticParts.addOrReplaceChild("shape4", box(56, 0, 0, 0, 0, 8, 1, 8), PartPose.offset(-4.0F, 10.0F, -4.0F));
            return LayerDefinition.create(mesh, TEXTURE_WIDTH, TEXTURE_HEIGHT);
        }

        public ModelPart root() {
            return root;
        }

        /** The valve wheel; the 1.12 renderer spins it about Z by the rotator's angle. */
        /** The parts that never move; the renderer draws these once, outside the spin. */
        public ModelPart staticParts() {
            return root.getChild("static");
        }

        public ModelPart valve() {
            return valve;
        }
    }

    private static CubeListBuilder box(int u, int v, int x, int y, int z, int width, int height, int depth) {
        return CubeListBuilder.create().texOffs(u, v)
                .addBox(x, y, z, width, height, depth, CubeDeformation.NONE);
    }

    private static ResourceLocation texture(String name) {
        return ResourceLocation.tryParse("thaumadds:textures/models/" + name + ".png");
    }
}
