I'll explore the target directory structure and analyze each group.
$ ls -R "H:/MinecraftMods/_ta-analysis/src/main/java/org/zeith/thaumicadditions/client" 2>&1 | head -100
H:/MinecraftMods/_ta-analysis/src/main/java/org/zeith/thaumicadditions/client:
HudHandlerHookTAR.java
ParticleHooksTAR.java
fx
isr
models
render
runnable
seal
texture
util

H:/MinecraftMods/_ta-analysis/src/main/java/org/zeith/thaumicadditions/client/fx:
FXColoredDrop.java
FXShadowBeam.java
FXTextureAtlasSpriteDigging.java
ParticleFX.java

H:/MinecraftMods/_ta-analysis/src/main/java/org/zeith/thaumicadditions/client/isr:
ItemRenderJar.java

H:/MinecraftMods/_ta-analysis/src/main/java/org/zeith/thaumicadditions/client/models:
ModelAspectCombiner.java
ModelAuraCharger.java
ModelBlueWolf.java
ModelBlueWolfHead.java
ModelChester.java
ModelCrystalBore.java
ModelCrystalCrusher.java
ModelFluxConcentrator.java
baked

H:/MinecraftMods/_ta-analysis/src/main/java/org/zeith/thaumicadditions/client/models/baked:
BakedCropModel.java

H:/MinecraftMods/_ta-analysis/src/main/java/org/zeith/thaumicadditions/client/render:
block
entity
tile

H:/MinecraftMods/_ta-analysis/src/main/java/org/zeith/thaumicadditions/client/render/block:
statemap

H:/MinecraftMods/_ta-analysis/src/main/java/org/zeith/thaumicadditions/client/render/block/statemap:
LambdaStateMapper.java

H:/MinecraftMods/_ta-analysis/src/main/java/org/zeith/thaumicadditions/client/render/entity:
RenderBlueWolf.java
RenderChester.java
RenderEssentiaShot.java
RenderMithminiteScythe.java

H:/MinecraftMods/_ta-analysis/src/main/java/org/zeith/thaumicadditions/client/render/tile:
TESRAbstractJar.java
TESRArcaneCake.java
TESRAspectCombiner.java
TESRAuraCharger.java
TESRAuraDisperser.java
TESRCrystalBore.java
TESRCrystalCrusher.java
TESREssentiaSink.java
TESRFluxConcentrator.java
TESRMobSummoner.java
TESRShadowEnchanter.java

H:/MinecraftMods/_ta-analysis/src/main/java/org/zeith/thaumicadditions/client/runnable:
ThunderRunnable.java
VisSparkleRunnable.java

H:/MinecraftMods/_ta-analysis/src/main/java/org/zeith/thaumicadditions/client/seal:
PortalRenderer.java
TARSealRenders.java
TESRSeal.java

H:/MinecraftMods/_ta-analysis/src/main/java/org/zeith/thaumicadditions/client/texture:
TextureThaumonomiconBG.java

H:/MinecraftMods/_ta-analysis/src/main/java/org/zeith/thaumicadditions/client/util:
GLDownloader.java
planemath
tesseract

H:/MinecraftMods/_ta-analysis/src/main/java/org/zeith/thaumicadditions/client/util/planemath:
Matrix4f.java
TrigMath.java

H:/MinecraftMods/_ta-analysis/src/main/java/org/zeith/thaumicadditions/client/util/tesseract:
Plane.java
Tesseract.java

Now let me read the TESR files (Group A) and models (Group B).
Now Group B — models.
Error: The user has specified a rule which prevents you from using this specific tool call. Here are some of the relevant rules [{"permission":"*","action":"allow","pattern":"*"},{"permission":"*","action":"deny","pattern":"*"},{"permission":"bash","action":"allow","pattern":"*"},{"permission":"bash","pattern":"*","action":"deny"},{"permission":"bash","pattern":"ls*","action":"allow"},{"permission":"bash","pattern":"wc *","action":"allow"},{"permission":"bash","pattern":"find *","action":"allow"},{"permission":"bash","pattern":"grep *","action":"allow"},{"permission":"bash","pattern":"rg *","action":"allow"},{"permission":"bash","pattern":"head *","action":"allow"},{"permission":"bash","pattern":"tail *","action":"allow"},{"permission":"bash","pattern":"sed -n *","action":"allow"},{"permission":"bash","pattern":"unzip -l *","action":"allow"},{"permission":"bash","pattern":"unzip -p *","action":"allow"},{"permission":"bash","pattern":"git log*","action":"allow"},{"permission":"bash","pattern":"git show*","action":"allow"},{"permission":"bash","pattern":"git diff*","action":"allow"},{"permission":"bash","pattern":"git status*","action":"allow"}]

$ wc -l "H:/MinecraftMods/_ta-analysis/src/main/java/org/zeith/thaumicadditions/client/models/ModelBlueWolf.java" "H:/MinecraftMods/_ta-analysis/src/main/java/org/zeith/thaumicadditions/client/models/ModelBlueWolfHead.java" "H:/MinecraftMods/_ta-analysis/src/main/java/org/zeith/thaumicadditions/client/models/baked/BakedCropModel.java"
117 H:/MinecraftMods/_ta-analysis/src/main/java/org/zeith/thaumicadditions/client/models/ModelBlueWolf.java
  222 H:/MinecraftMods/_ta-analysis/src/main/java/org/zeith/thaumicadditions/client/models/ModelBlueWolfHead.java
  150 H:/MinecraftMods/_ta-analysis/src/main/java/org/zeith/thaumicadditions/client/models/baked/BakedCropModel.java
  489 total

$ grep -c "addBox" "H:/MinecraftMods/_ta-analysis/src/main/java/org/zeith/thaumicadditions/client/models/ModelBlueWolf.java" "H:/MinecraftMods/_ta-analysis/src/main/java/org/zeith/thaumicadditions/client/models/ModelBlueWolfHead.java" "H:/MinecraftMods/_ta-analysis/src/main/java/org/zeith/thaumicadditions/client/models/ModelAspectCombiner.java" "H:/MinecraftMods/_ta-analysis/src/main/java/org/zeith/thaumicadditions/client/models/ModelAuraCharger.java" "H:/MinecraftMods/_ta-analysis/src/main/java/org/zeith/thaumicadditions/client/models/ModelCrystalBore.java" "H:/MinecraftMods/_ta-analysis/src/main/java/org/zeith/thaumicadditions/client/models/ModelCrystalCrusher.java" "H:/MinecraftMods/_ta-analysis/src/main/java/org/zeith/thaumicadditions/client/models/ModelFluxConcentrator.java" "H:/MinecraftMods/_ta-analysis/src/main/java/org/zeith/thaumicadditions/client/models/ModelChester.java" "H:/MinecraftMods/_ta-analysis/src/main/java/org/zeith/thaumicadditions/client/models/baked/BakedCropModel.java"
H:/MinecraftMods/_ta-analysis/src/main/java/org/zeith/thaumicadditions/client/models/ModelBlueWolf.java:11
H:/MinecraftMods/_ta-analysis/src/main/java/org/zeith/thaumicadditions/client/models/ModelBlueWolfHead.java:0
H:/MinecraftMods/_ta-analysis/src/main/java/org/zeith/thaumicadditions/client/models/ModelAspectCombiner.java:5
H:/MinecraftMods/_ta-analysis/src/main/java/org/zeith/thaumicadditions/client/models/ModelAuraCharger.java:8
H:/MinecraftMods/_ta-analysis/src/main/java/org/zeith/thaumicadditions/client/models/ModelCrystalBore.java:6
H:/MinecraftMods/_ta-analysis/src/main/java/org/zeith/thaumicadditions/client/models/ModelCrystalCrusher.java:3
H:/MinecraftMods/_ta-analysis/src/main/java/org/zeith/thaumicadditions/client/models/ModelFluxConcentrator.java:23
H:/MinecraftMods/_ta-analysis/src/main/java/org/zeith/thaumicadditions/client/models/ModelChester.java:3
H:/MinecraftMods/_ta-analysis/src/main/java/org/zeith/thaumicadditions/client/models/baked/BakedCropModel.java:0


$ grep -rn "addChild\|super(\|setTextureSize\|textureWidth\s*=\|textureHeight\s*=" "H:/MinecraftMods/_ta-analysis/src/main/java/org/zeith/thaumicadditions/client/models/" 
H:/MinecraftMods/_ta-analysis/src/main/java/org/zeith/thaumicadditions/client/models/ModelAspectCombiner.java:18:		this.textureWidth = 64;
H:/MinecraftMods/_ta-analysis/src/main/java/org/zeith/thaumicadditions/client/models/ModelAspectCombiner.java:19:		this.textureHeight = 64;
H:/MinecraftMods/_ta-analysis/src/main/java/org/zeith/thaumicadditions/client/models/ModelAuraCharger.java:28:		this.textureWidth = 32;
H:/MinecraftMods/_ta-analysis/src/main/java/org/zeith/thaumicadditions/client/models/ModelAuraCharger.java:29:		this.textureHeight = 32;
H:/MinecraftMods/_ta-analysis/src/main/java/org/zeith/thaumicadditions/client/models/ModelAuraCharger.java:54:		this.shape2.addChild(this.shape5);
H:/MinecraftMods/_ta-analysis/src/main/java/org/zeith/thaumicadditions/client/models/ModelAuraCharger.java:55:		this.shape2.addChild(this.shape5_1);
H:/MinecraftMods/_ta-analysis/src/main/java/org/zeith/thaumicadditions/client/models/ModelAuraCharger.java:56:		this.shape2.addChild(this.shape3);
H:/MinecraftMods/_ta-analysis/src/main/java/org/zeith/thaumicadditions/client/models/ModelAuraCharger.java:57:		this.shape2.addChild(this.shape4);
H:/MinecraftMods/_ta-analysis/src/main/java/org/zeith/thaumicadditions/client/models/ModelAuraCharger.java:58:		this.shape2.addChild(this.shape3_1);
H:/MinecraftMods/_ta-analysis/src/main/java/org/zeith/thaumicadditions/client/models/ModelAuraCharger.java:59:		this.shape2.addChild(this.shape4_1);
H:/MinecraftMods/_ta-analysis/src/main/java/org/zeith/thaumicadditions/client/models/ModelBlueWolf.java:27:		this.textureWidth = 64;
H:/MinecraftMods/_ta-analysis/src/main/java/org/zeith/thaumicadditions/client/models/ModelBlueWolf.java:28:		this.textureHeight = 64;
H:/MinecraftMods/_ta-analysis/src/main/java/org/zeith/thaumicadditions/client/models/ModelBlueWolfHead.java:25:		super(modelSize);
H:/MinecraftMods/_ta-analysis/src/main/java/org/zeith/thaumicadditions/client/models/ModelBlueWolfHead.java:94:			this.bipedHead.addChild(headBase);
H:/MinecraftMods/_ta-analysis/src/main/java/org/zeith/thaumicadditions/client/models/ModelBlueWolfHead.java:95:			this.bipedHead.addChild(headLeftEar);
H:/MinecraftMods/_ta-analysis/src/main/java/org/zeith/thaumicadditions/client/models/ModelBlueWolfHead.java:96:			this.bipedHead.addChild(headRightEar);
H:/MinecraftMods/_ta-analysis/src/main/java/org/zeith/thaumicadditions/client/models/ModelBlueWolfHead.java:101:			this.bipedBody.addChild(bodyBase);
H:/MinecraftMods/_ta-analysis/src/main/java/org/zeith/thaumicadditions/client/models/ModelBlueWolfHead.java:102:			this.bipedLeftArm.addChild(bodyLeftPaw);
H:/MinecraftMods/_ta-analysis/src/main/java/org/zeith/thaumicadditions/client/models/ModelBlueWolfHead.java:103:			this.bipedRightArm.addChild(bodyRightPaw);
H:/MinecraftMods/_ta-analysis/src/main/java/org/zeith/thaumicadditions/client/models/ModelBlueWolfHead.java:108:			this.bipedBody.addChild(legsTail);
H:/MinecraftMods/_ta-analysis/src/main/java/org/zeith/thaumicadditions/client/models/ModelBlueWolfHead.java:109:			this.bipedLeftLeg.addChild(legsLeft);
H:/MinecraftMods/_ta-analysis/src/main/java/org/zeith/thaumicadditions/client/models/ModelBlueWolfHead.java:110:			this.bipedRightLeg.addChild(legsRight);
H:/MinecraftMods/_ta-analysis/src/main/java/org/zeith/thaumicadditions/client/models/ModelBlueWolfHead.java:115:			this.bipedLeftLeg.addChild(feetpawLeft);
H:/MinecraftMods/_ta-analysis/src/main/java/org/zeith/thaumicadditions/client/models/ModelBlueWolfHead.java:116:			this.bipedRightLeg.addChild(feetpawRight);
H:/MinecraftMods/_ta-analysis/src/main/java/org/zeith/thaumicadditions/client/models/ModelChester.java:16:		chestLid = new ModelRenderer(this, 0, 0).setTextureSize(64, 64);
H:/MinecraftMods/_ta-analysis/src/main/java/org/zeith/thaumicadditions/client/models/ModelChester.java:19:		chestKnob = new ModelRenderer(this, 0, 0).setTextureSize(64, 64);
H:/MinecraftMods/_ta-analysis/src/main/java/org/zeith/thaumicadditions/client/models/ModelChester.java:22:		chestBelow = new ModelRenderer(this, 0, 19).setTextureSize(64, 64);
H:/MinecraftMods/_ta-analysis/src/main/java/org/zeith/thaumicadditions/client/models/ModelCrystalBore.java:20:		this.textureWidth = 128;
H:/MinecraftMods/_ta-analysis/src/main/java/org/zeith/thaumicadditions/client/models/ModelCrystalBore.java:21:		this.textureHeight = 64;
H:/MinecraftMods/_ta-analysis/src/main/java/org/zeith/thaumicadditions/client/models/ModelCrystalBore.java:40:		this.rotational.addChild(this.shape4);
H:/MinecraftMods/_ta-analysis/src/main/java/org/zeith/thaumicadditions/client/models/ModelCrystalBore.java:41:		this.shape5.addChild(this.shape6);
H:/MinecraftMods/_ta-analysis/src/main/java/org/zeith/thaumicadditions/client/models/ModelCrystalBore.java:42:		this.shape4.addChild(this.shape5);
H:/MinecraftMods/_ta-analysis/src/main/java/org/zeith/thaumicadditions/client/models/ModelCrystalCrusher.java:17:		this.textureWidth = 64;
H:/MinecraftMods/_ta-analysis/src/main/java/org/zeith/thaumicadditions/client/models/ModelCrystalCrusher.java:18:		this.textureHeight = 64;
H:/MinecraftMods/_ta-analysis/src/main/java/org/zeith/thaumicadditions/client/models/ModelCrystalCrusher.java:28:		this.shape3.addChild(this.shape2);
H:/MinecraftMods/_ta-analysis/src/main/java/org/zeith/thaumicadditions/client/models/ModelFluxConcentrator.java:42:		super(128, 64, new ResourceLocation(InfoTAR.MOD_ID, "textures/models/flux_concentrator.png"));
H:/MinecraftMods/_ta-analysis/src/main/java/org/zeith/thaumicadditions/client/models/ModelFluxConcentrator.java:43:		this.textureWidth = 128;
H:/MinecraftMods/_ta-analysis/src/main/java/org/zeith/thaumicadditions/client/models/ModelFluxConcentrator.java:44:		this.textureHeight = 64;


$ grep -n "^public class\|^public interface\|^ *public [A-Za-z<>]*\(class\|interface\)\|extends\|implements" "H:/MinecraftMods/_ta-analysis/src/main/java/org/zeith/thaumicadditions/client/isr/ItemRenderJar.java" "H:/MinecraftMods/_ta-analysis/src/main/java/org/zeith/thaumicadditions/client/seal/PortalRenderer.java" "H:/MinecraftMods/_ta-analysis/src/main/java/org/zeith/thaumicadditions/client/seal/TARSealRenders.java" "H:/MinecraftMods/_ta-analysis/src/main/java/org/zeith/thaumicadditions/client/texture/TextureThaumonomiconBG.java" "H:/MinecraftMods/_ta-analysis/src/main/java/org/zeith/thaumicadditions/client/util/GLDownloader.java" "H:/MinecraftMods/_ta-analysis/src/main/java/org/zeith/thaumicadditions/client/util/planemath/Matrix4f.java" "H:/MinecraftMods/_ta-analysis/src/main/java/org/zeith/thaumicadditions/client/util/planemath/TrigMath.java" "H:/MinecraftMods/_ta-analysis/src/main/java/org/zeith/thaumicadditions/client/util/tesseract/Plane.java" "H:/MinecraftMods/_ta-analysis/src/main/java/org/zeith/thaumicadditions/client/util/tesseract/Tesseract.java" "H:/MinecraftMods/_ta-analysis/src/main/java/org/zeith/thaumicadditions/client/runnable/ThunderRunnable.java" "H:/MinecraftMods/_ta-analysis/src/main/java/org/zeith/thaumicadditions/client/runnable/VisSparkleRunnable.java" "H:/MinecraftMods/_ta-analysis/src/main/java/org/zeith/thaumicadditions/client/HudHandlerHookTAR.java" "H:/MinecraftMods/_ta-analysis/src/main/java/org/zeith/thaumicadditions/client/ParticleHooksTAR.java" "H:/MinecraftMods/_ta-analysis/src/main/java/org/zeith/thaumicadditions/client/render/block/statemap/LambdaStateMapper.java" "H:/MinecraftMods/_ta-analysis/src/main/java/org/zeith/thaumicadditions/client/render/entity/RenderBlueWolf.java" "H:/MinecraftMods/_ta-analysis/src/main/java/org/zeith/thaumicadditions/client/render/entity/RenderChester.java" "H:/MinecraftMods/_ta-analysis/src/main/java/org/zeith/thaumicadditions/client/render/entity/RenderEssentiaShot.java" "H:/MinecraftMods/_ta-analysis/src/main/java/org/zeith/thaumicadditions/client/render/entity/RenderMithminiteScythe.java"
H:/MinecraftMods/_ta-analysis/src/main/java/org/zeith/thaumicadditions/client/isr/ItemRenderJar.java:22:public class ItemRenderJar
H:/MinecraftMods/_ta-analysis/src/main/java/org/zeith/thaumicadditions/client/isr/ItemRenderJar.java:23:		implements IItemRender
H:/MinecraftMods/_ta-analysis/src/main/java/org/zeith/thaumicadditions/client/seal/PortalRenderer.java:20:public class PortalRenderer
H:/MinecraftMods/_ta-analysis/src/main/java/org/zeith/thaumicadditions/client/seal/TARSealRenders.java:38:public class TARSealRenders
H:/MinecraftMods/_ta-analysis/src/main/java/org/zeith/thaumicadditions/client/texture/TextureThaumonomiconBG.java:15:public class TextureThaumonomiconBG
H:/MinecraftMods/_ta-analysis/src/main/java/org/zeith/thaumicadditions/client/texture/TextureThaumonomiconBG.java:16:		extends AbstractTexture
H:/MinecraftMods/_ta-analysis/src/main/java/org/zeith/thaumicadditions/client/texture/TextureThaumonomiconBG.java:17:		implements ITickableTextureObject
H:/MinecraftMods/_ta-analysis/src/main/java/org/zeith/thaumicadditions/client/util/GLDownloader.java:10:public class GLDownloader
H:/MinecraftMods/_ta-analysis/src/main/java/org/zeith/thaumicadditions/client/util/planemath/Matrix4f.java:5:public class Matrix4f
H:/MinecraftMods/_ta-analysis/src/main/java/org/zeith/thaumicadditions/client/util/planemath/TrigMath.java:5:public class TrigMath
H:/MinecraftMods/_ta-analysis/src/main/java/org/zeith/thaumicadditions/client/util/tesseract/Plane.java:16:public class Plane
H:/MinecraftMods/_ta-analysis/src/main/java/org/zeith/thaumicadditions/client/util/tesseract/Tesseract.java:8:public class Tesseract
H:/MinecraftMods/_ta-analysis/src/main/java/org/zeith/thaumicadditions/client/runnable/ThunderRunnable.java:9:public class ThunderRunnable
H:/MinecraftMods/_ta-analysis/src/main/java/org/zeith/thaumicadditions/client/runnable/ThunderRunnable.java:10:		implements Runnable
H:/MinecraftMods/_ta-analysis/src/main/java/org/zeith/thaumicadditions/client/runnable/VisSparkleRunnable.java:8:public class VisSparkleRunnable
H:/MinecraftMods/_ta-analysis/src/main/java/org/zeith/thaumicadditions/client/runnable/VisSparkleRunnable.java:9:		implements Runnable
H:/MinecraftMods/_ta-analysis/src/main/java/org/zeith/thaumicadditions/client/HudHandlerHookTAR.java:13:public class HudHandlerHookTAR
H:/MinecraftMods/_ta-analysis/src/main/java/org/zeith/thaumicadditions/client/ParticleHooksTAR.java:9:public class ParticleHooksTAR
H:/MinecraftMods/_ta-analysis/src/main/java/org/zeith/thaumicadditions/client/render/block/statemap/LambdaStateMapper.java:7:public class LambdaStateMapper
H:/MinecraftMods/_ta-analysis/src/main/java/org/zeith/thaumicadditions/client/render/block/statemap/LambdaStateMapper.java:8:		extends StateMapperBase
H:/MinecraftMods/_ta-analysis/src/main/java/org/zeith/thaumicadditions/client/render/entity/RenderBlueWolf.java:14:public class RenderBlueWolf
H:/MinecraftMods/_ta-analysis/src/main/java/org/zeith/thaumicadditions/client/render/entity/RenderBlueWolf.java:15:		extends RenderLiving<EntityBlueWolf>
H:/MinecraftMods/_ta-analysis/src/main/java/org/zeith/thaumicadditions/client/render/entity/RenderChester.java:19:public class RenderChester
H:/MinecraftMods/_ta-analysis/src/main/java/org/zeith/thaumicadditions/client/render/entity/RenderChester.java:20:		extends RenderLiving<EntityChester>
H:/MinecraftMods/_ta-analysis/src/main/java/org/zeith/thaumicadditions/client/render/entity/RenderEssentiaShot.java:12:public class RenderEssentiaShot
H:/MinecraftMods/_ta-analysis/src/main/java/org/zeith/thaumicadditions/client/render/entity/RenderEssentiaShot.java:13:		extends Render<EntityEssentiaShot>
H:/MinecraftMods/_ta-analysis/src/main/java/org/zeith/thaumicadditions/client/render/entity/RenderMithminiteScythe.java:28:public class RenderMithminiteScythe
H:/MinecraftMods/_ta-analysis/src/main/java/org/zeith/thaumicadditions/client/render/entity/RenderMithminiteScythe.java:29:		extends RenderSnowball<EntityMithminiteScythe>

Error: The user has specified a rule which prevents you from using this specific tool call. Here are some of the relevant rules [{"permission":"*","action":"allow","pattern":"*"},{"permission":"*","action":"deny","pattern":"*"},{"permission":"bash","action":"allow","pattern":"*"},{"permission":"bash","pattern":"*","action":"deny"},{"permission":"bash","pattern":"ls*","action":"allow"},{"permission":"bash","pattern":"wc *","action":"allow"},{"permission":"bash","pattern":"find *","action":"allow"},{"permission":"bash","pattern":"grep *","action":"allow"},{"permission":"bash","pattern":"rg *","action":"allow"},{"permission":"bash","pattern":"head *","action":"allow"},{"permission":"bash","pattern":"tail *","action":"allow"},{"permission":"bash","pattern":"sed -n *","action":"allow"},{"permission":"bash","pattern":"unzip -l *","action":"allow"},{"permission":"bash","pattern":"unzip -p *","action":"allow"},{"permission":"bash","pattern":"git log*","action":"allow"},{"permission":"bash","pattern":"git show*","action":"allow"},{"permission":"bash","pattern":"git diff*","action":"allow"},{"permission":"bash","pattern":"git status*","action":"allow"}]
## A) client/render/tile — 11 个 TESR

| 文件 | 类名 | 渲染的 Tile | 模型类 | 贴图路径 | 动态动画（角度来源） |
|---|---|---|---|---|---|
| `render/tile/TESRAbstractJar.java` | `TESRAbstractJar extends TESR<TileAbstractJarFillable>` (`@AtTESR`) | TileAbstractJarFillable | 上游 `thaumcraft.client.renderers.models.block.ModelJar`（+ `ItemRenderJar` 画液面） | `thaumcraft:textures/models/jarbrine.png`、`thaumcraft:textures/models/label.png` | 有（非时间轴）：标签歪斜角 `rot = (aspectFilter.getTag().hashCode() + pos.getX() + facing) % 4 - 2`（:71），另按 `tile.facing` 定朝向 |
| `render/tile/TESRArcaneCake.java` | `TESRArcaneCake extends TESR<TileArcaneCake>` (`@AtTESR`) | TileArcaneCake | `SimpleBlockRendering`（`RenderBlocks.forMod(InfoTAR.MOD_ID).simpleRenderer`） | `thaumadditions:textures/blocks/cake_top_cherries.png` | 无；仅按 `te.getRGB()` 染顶面 + 按 AABB 抬 0.01/16 |
| `render/tile/TESRAspectCombiner.java` | `TESRAspectCombiner extends TESR<TileAspectCombiner>` | TileAspectCombiner | `client.models.ModelAspectCombiner` | `thaumadditions:textures/models/aspect_combiner.png` | 有：绕 Y 旋转 `te.prevRotation + (te.rotation - te.prevRotation)*partialTicks`（:39，字段 rotation/prevRotation）；另有 `te.vis/prevVis` 驱动 5.8/16 高光柱（:92,97）与 `te.craftingTime` 混色 |
| `render/tile/TESRAuraCharger.java` | `TESRAuraCharger extends TESR<TileAuraCharger>` | TileAuraCharger | `client.models.ModelAuraCharger` | `thaumadditions:textures/models/aura_charger.png` | 有：`te.rotator`(FrictionRotator).getActualRotation(partialTicks) 绕 Y（:31），作用于 shape2 子树 |
| `render/tile/TESRAuraDisperser.java` | `TESRAuraDisperser extends TESR<TileAuraDisperser>` | TileAuraDisperser | 无 | 无 | 无（方法体为空，空实现桩） |
| `render/tile/TESRCrystalBore.java` | `TESRCrystalBore extends TESR<TileCrystalBore>` | TileCrystalBore | `client.models.ModelCrystalBore` | `thaumadditions:textures/models/crystal_bore.png` | 有：`te.rotator.getActualRotation(partialTicks)` 传给 `model.render(angle)`（:28），内部绕 Y 转 `rotational` 节点 |
| `render/tile/TESRCrystalCrusher.java` | `TESRCrystalCrusher extends TESR<TileCrystalCrusher>` | TileCrystalCrusher | `client.models.ModelCrystalCrusher` | `thaumadditions:textures/models/crystal_crusher.png` | 有（双重）：升降 `lift = sin(toRadians(te.craftTime/100F*180F))`（:21-22,34）+ `te.rotator.getActualRotation(partialTicks)` 绕 Y（:35） |
| `render/tile/TESREssentiaSink.java` | `TESREssentiaSink extends TESR<TileEssentiaSink>` (`@AtTESR`) | TileEssentiaSink | 无 | 无 | 无（空实现桩，仅读了 `inventory.getStackInSlot(0)`） |
| `render/tile/TESRFluxConcentrator.java` | `TESRFluxConcentrator extends TESR<TileFluxConcentrator>` | TileFluxConcentrator | `client.models.ModelFluxConcentrator`（`static MODEL`） | `thaumadditions:textures/models/flux_concentrator.png`（ModelSimple 构造里绑定） | 有：`bindTexture(te)` 取 `te.valve`(FrictionRotator).getActualRotation(partialTicks)，在 model 内转 shape18（ModelFluxConcentrator.java:151-170） |
| `render/tile/TESRMobSummoner.java` | `TESRMobSummoner extends TESR<TileEntitySummoner>` (`@AtTESR`) | TileEntitySummoner | 无（`Tesseract.INSTANCE.draw` + `proxy.getFX().renderMob`） | `thaumcraft:textures/blocks/metal_thaumium.png` | 有：`scale = toRadians(te.rotator.getActualRotation(partialTicks)/2)` 作为四维超立方体缩放/相位（:23） |
| `render/tile/TESRShadowEnchanter.java` | `TESRShadowEnchanter extends TESR<TileShadowEnchanter>` (`@AtTESR`) | TileShadowEnchanter | 无（`RenderItem.renderItem(GROUND)`） | 由物品自身贴图 | 有：绕 Y `(te.ticksExisted + partialTicks) % 360F`（:28）+ 正弦悬浮 `sin(PI*((ticksExisted+partialTicks)/60F % 60F))*0.05F`（:27） |

> 同为 TESR 但在 `client/seal`：`seal/TESRSeal.java:19` `TESR<TileSeal>` — 无自带模型，渲染 `te.stack` 物品 + 3 个 aspect 字符（`TARSealRenders.renderStandart`）+ `ISealRenderer` 组合特效，动画参数为 partialTicks。

## B) client/models — 9 个模型类

| 文件 | 类名 | 贴图尺寸(w,h) | addBox 次数 | 子节点层级 |
|---|---|---|---|---|
| `models/ModelAspectCombiner.java` | `ModelAspectCombiner extends ModelBase` | 64,64 | 5 | 无（扁平；shape2 被 render() 注释掉，由 TESR 单独旋转绘制） |
| `models/ModelAuraCharger.java` | `ModelAuraCharger extends ModelBase` | 32,32 | 8 | 有：shape2 → 6 子节点 (shape5, shape5_1, shape3, shape4, shape3_1, shape4_1)，:54-59；shape1 独立 |
| `models/ModelBlueWolf.java` | `ModelBlueWolf extends ModelBase` (`@SideOnly CLIENT`) | 64,64 | 11 | 无（8 个 ModelRenderer；wolfHeadMain 另经 setTextureOffset 追加 3 个 box，:54-56） |
| `models/ModelBlueWolfHead.java` | `ModelBlueWolfHead extends ModelBiped` | 未显式设置（沿用 ModelBiped 默认 64,32）；部件用自带贴图 `textures/armor/blue_wolf_{head,body,legs,feetpaws}.png` | 0（改用 `ModelRendererWavefront` + `.obj`，:27-40） | 有：bipedHead/Body/LeftArm/RightArm/LeftLeg/RightLeg 挂 OBJ 部件，按 isHelmet/isChestPiece/isLeggings/isdBoots 条件挂载（:92-117） |
| `models/ModelChester.java` | `ModelChester extends ModelBase` | 64,64（逐个 `setTextureSize(64,64)`） | 3 | 无 |
| `models/ModelCrystalBore.java` | `ModelCrystalBore extends ModelBase` | 128,64 | 6 | 有：3 层链 `rotational → shape4 → shape5 → shape6`（:40-42） |
| `models/ModelCrystalCrusher.java` | `ModelCrystalCrusher extends ModelBase` | 64,64 | 3 | 有：`shape3.addChild(shape2)`（:28） |
| `models/ModelFluxConcentrator.java` | `ModelFluxConcentrator extends ModelSimple<TileFluxConcentrator>` | 128,64（`super(128,64, textures/models/flux_concentrator.png)`，:42） | 23 | 无 addChild（扁平，render() 逐个列出；shape18 阀门通过 GL 矩阵临时平移旋转点后旋转） |
| `models/baked/BakedCropModel.java` | `BakedCropModel implements IBakedModel` | N/A（方块图集 sprite `thaumadditions:blocks/vis_crop/10..51`，:21-35） | 0 | 无（按 side/axis 用 FACE_BAKERY 生成 2 个交叉 quad ×1~2 张 sprite） |

## C) 其他 client 子包

**fx/**（4）
- `fx/ParticleFX.java` — `SimpleParticle implements IGlowingEntity`：加载 `textures/particles/fx/<idx>.png` 帧动画粒子并按 `lightStrength/lightColor` 发射 HammerCore 彩色光源。
- `fx/FXColoredDrop.java` — 带 RGB 染色、受重力下落的有色液滴粒子，接触方块时与液体/效应实体交互（引用 RenderEssentiaShot）。
- `fx/FXShadowBeam.java` — 用 `GL_LINES` 绘制首尾淡入淡出的粗线光束，颜色 `0x7712DFFF`，寿命 20 tick。
- `fx/FXTextureAtlasSpriteDigging.java` — `ParticleDigging` 子类，允许指定自定义 `TextureAtlasSprite` 贴图（用于挖掘粒子）。

**isr/**（1）
- `isr/ItemRenderJar.java` — `implements IItemRender`：罐内液面/要素液体渲染（按 amount/capacity 算高度、aspect 上色），被 TESRAbstractJar 与物品渲染共用，含 JarData 元数据处理。

**seal/**（3）
- `seal/TESRSeal.java` — `TESR<TileSeal>`：渲染 seal 方块的符号物品 + 3 个 aspect 字符 + 自定义组合 `ISealRenderer`，并覆写 `translateFromOrientation` 处理 6 向。
- `seal/TARSealRenders.java` — seal 渲染静态工具/注册表（`renderStandart`、renderNone、portal 列表 saveRender/rendered），静态块向 Forge 事件总线注册 RenderTick 监听。
- `seal/PortalRenderer.java` — FBO 传送门离屏渲染器（512² RGB 纹理 + GLDownloader 回读），受 `ConfigsTAR.portalGfx` 开关控制。

**texture/**（1）
- `texture/TextureThaumonomiconBG.java` — `AbstractTexture implements ITickableTextureObject`：加载 `textures/gui/thaumonomicon_back.jpg`，每 tick 平移滚动并按 blur/clamp 重新上传。

**util/**（5）
- `util/GLDownloader.java` — 静态工具：ByteBuffer ↔ BufferedImage 互转、缓冲区上传到指定 glTex（RGBA）。
- `util/planemath/Matrix4f.java` — 不可变 4×4 浮点矩阵（16 个 final float），`from(...)`/`transform(Vector4f)`，服务于四维数学。
- `util/planemath/TrigMath.java` — 预计算 4194304 项正弦表的快速 sin/cos（避免超立方体逐帧开销）。
- `util/tesseract/Tesseract.java` — `INSTANCE` 单例，持有 24 个 Plane 组成的四维超立方体；`draw(color, scale)` 用相位参数旋转。
- `util/tesseract/Plane.java` — 超立方体的一个四边形面（4 个 Vector4f），含 `rotXW/rotZW` 四维旋转并用 Tessellator 绘制。

**runnable/**（2）
- `runnable/ThunderRunnable.java` — `Runnable`：在 start/end 两点间按 seed/age 生成 HammerCore 雷击粒子（core+aura 两层）。
- `runnable/VisSparkleRunnable.java` — `Runnable`：在两点间生成指定 RGB 的 `FXVisSparkle` 并加入 ParticleEngine。

**client 根目录（2）**
- `HudHandlerHookTAR.java` — 替换/扩展 Thaumcraft `HudHandler` 的 thaumometer 绘制：叠加 base/vis/flux 三条进度条与倒计时文本。
- `ParticleHooksTAR.java` — 用反射读取 `FXGeneric` 私有字段 `gridSize`/`scaleFrames`，识别「会发声的 FX」粒子（用于静音）。

**render/block/**（1）
- `render/block/statemap/LambdaStateMapper.java` — `StateMapperBase` 适配器，把 `getModelResourceLocation` 委托给内部 `IStateMapper` 函数式接口（lambda 注册方块模型）。

**render/entity/**（4）
- `render/entity/RenderBlueWolf.java` — `RenderLiving<EntityBlueWolf>` 用 `ModelBlueWolf`，贴图 `textures/entity/blue_wolf.png` / `blue_wolf_angry.png`（按愤怒切换），尾巴旋转取 `getTailRotation()`。
- `render/entity/RenderChester.java` — `RenderLiving<EntityChester>` 用 `ModelChester`，贴图 `textures/models/chester.png`，`adjustTrunk` 做开合/弹跳缩放，并预留 `visualsRenderer` 外部特效钩子。
- `render/entity/RenderEssentiaShot.java` — `Render<EntityEssentiaShot>`，贴图 `textures/misc/essentia_drop.png`。
- `render/entity/RenderMithminiteScythe.java` — `RenderSnowball`：投掷出的 Mithminite Scythe 物品渲染，叠加 shootYaw/shootPitch 与 `(ticksExisted+partialTicks)*40F` 自转。

合计覆盖 43 个 java 文件（11 TESR + 9 模型 + 23 其他）。