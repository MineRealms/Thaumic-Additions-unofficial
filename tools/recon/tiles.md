Error: The user has specified a rule which prevents you from using this specific tool call. Here are some of the relevant rules [{"permission":"*","action":"allow","pattern":"*"},{"permission":"*","action":"deny","pattern":"*"},{"permission":"bash","action":"allow","pattern":"*"},{"permission":"bash","pattern":"*","action":"deny"},{"permission":"bash","pattern":"ls*","action":"allow"},{"permission":"bash","pattern":"wc *","action":"allow"},{"permission":"bash","pattern":"find *","action":"allow"},{"permission":"bash","pattern":"grep *","action":"allow"},{"permission":"bash","pattern":"rg *","action":"allow"},{"permission":"bash","pattern":"head *","action":"allow"},{"permission":"bash","pattern":"tail *","action":"allow"},{"permission":"bash","pattern":"sed -n *","action":"allow"},{"permission":"bash","pattern":"unzip -l *","action":"allow"},{"permission":"bash","pattern":"unzip -p *","action":"allow"},{"permission":"bash","pattern":"git log*","action":"allow"},{"permission":"bash","pattern":"git show*","action":"allow"},{"permission":"bash","pattern":"git diff*","action":"allow"},{"permission":"bash","pattern":"git status*","action":"allow"}]
I'll explore the target directory first.
Found 26 files (20 top-level + 6 in `jars/`). Reading them now.
Now the 6 jar subclasses:
## 26 Tile 类摘要（19 个顶层 + 6 个 jars 子类；题面"25"应为不含 `TileSmelterImpl` 的上游计数）

格式：`文件 — 类名 | update/tick 要点 | 持久化/同步关键字段 | 依赖`

```
TileAbstractJarFillable.java — TileAbstractJarFillable | 每 5 tick 向上方 IEssentiaTransport 吸 1 essentia（过滤/已有种类优先） | 无自有 NBT（继承 TC TileJarFillable: aspect/amount/aspectFilter）；absCount 仅运行时 | TC TileJarFillable, IEssentiaTransport, ThaumcraftApiHelper
TileAbstractSmelter.java — TileAbstractSmelter | 烧燃料+按 smeltTime 熔炼，间歇把 essence 推给上方/侧面 alembic，风箱加速 & flux 溢出污染 aura | furnaceBurnTime, currentItemBurnTime, furnaceCookTime, smeltTime(运行时), speedBoost, aspects, vis, inv | TC TileSmelter/TileAlembic/TileBellows, BlocksTC.smelterAux/smelterVent, AuraHelper, FXDispatcher
TileSmelterImpl.java — TileSmelterImpl | 无自有 tick，仅实现抽象参数 | efficiency, speed, maxVis（Speed/Efficiency/MaxVis） | TileAbstractSmelter
TileShadowEnchanter.java — TileShadowEnchanter | 剔除失效附魔；每 6 tick 抽 1 essentia 扣入 pending，pending 空则注入成品并清空 | infusing, craftTimer, pending(Aspects), enchants, brainCD, items | TC EssentiaHandler, SoundsTC, ShadowEnchantment, PacketShadowFX
TileSeal.java — TileSeal | 读朝向；dirty 时重建 SealCombination/SealInstance 并 tick 实例；红石时冻结 ticksExisted | orientation, stack, slots[3], placer（NetProperty*）, combination, instance, optInstNBT | SealManager, BlocksTAR.SEAL, EnumRotation, IGlowingEntity
TileGrowthChamber.java — TileGrowthChamber | 蓄同种 essence，耗 aura 累积到 toDrainAura 上限即 craft 出 1 个 crystalEssence | amount, slotAspect(由槽0推导), toDrainAura, growthInventory | TC AuraHelper, ThaumcraftApiHelper, FXVisSparkle, AspectUtil
TileFluxConcentrator.java — TileFluxConcentrator | 更新 valve 旋转；消耗 vitium→fuel；每 20 tick 从背面扩散 fluxed 网格、腐蚀方块、伤害生物 | vitium, fuel, cooldown, fluxed(List<BlockPos>), facing, cache | TC AuraHelper/PotionVisExhaust/DamageSourceThaumcraft.dissolve, RecipesFluxConcentrator, FrictionRotator
TileEssentiaSink.java — TileEssentiaSink | 读 FACING；从 essentiaFace 相邻罐抽 ELDRITCH 喂给槽0 里的 IAspectChargableItem | essentiaFace(由方块state推导), aci, inventory | IAspectChargableItem, BlocksTAR.ESSENTIA_SINK, IBlockHorizontal.FACING
TileEntitySummoner.java — TileEntitySummoner | 更新 rotator；每 3 tick 抽 essence 累积，每 20 tick 满足配方则 performSpawn 刷实体 | sample, accumulated, missing, cooldown, entities(IntList), rotator(存入NBT) | TC EntityTags/CommonInternals.scanEntities, EssentiaHandler, AnvilChunkLoader, ItemsTAR.ENTITY_CELL
TileCrystalLamp.java — TileCrystalLamp | 无 tick（TileSyncable 非 Tickable）；仅 produceColoredLight 用正弦相位做呼吸光 | crystal(NetPropertyString), inventory(1槽,存 crystalEssence) | BlocksTAR.CRYSTAL_LAMP, BlockAmberLamp.BASE_SIMPLEX, IGlowingEntity
TileCrystalCrusher.java — TileCrystalCrusher | 客户端更新 rotator + 加速；服务端抽 MECHANISM→crushes+3；craftTime≥100 压 crystalEssence→SALT_ESSENCE | crushes, craftTime, inv(0=essence,1=salt) | TC EssentiaHandler, ItemsTC.crystalEssence, ItemsTAR.SALT_ESSENCE, FrictionRotator
TileCrystalBore.java — TileCrystalBore | 更新 rotator；面向 BlockCrystal 时抽 vis，boring≥100 钻小一档并产出 essence | minCrystals(NBT), face/oldFace(由方块), boring(运行时) | TC BlockCrystal, AuraHandler.drainVis, FXDispatcher.drawCurlyWisp, SoundsTC.crystal
TileCrystalBlock.java — TileCrystalBlock | 无 tick；getAspect() 懒分配随机 aspect | aspect(String "Aspect") | Aspect, AspectUtil.crystalEssence
TileCraftingFurnace.java — TileCraftingFurnace | 原版熔炉 tick 逻辑（FurnaceRecipes, cookTime, 燃烧递减, 海绵+水桶） | furnaceBurnTime, currentItemBurnTime, cookTime, totalCookTime, furnaceCustomName | FurnaceRecipes, TileEntityFurnace, BlockCraftingFurnace
TileAuraDisperser.java — TileAuraDisperser | 选一格 SALT_ESSENCE 换成 aspects；每秒向前方 5×4×5 AABB 内生物施加 EdibleAspect，命中则 timer-- | timer, tickTimer, aspects, auraApplied, secondsPerSalt | EdibleAspect, ItemsTAR.SALT_ESSENCE, TAReconstructed.proxy.getFX()
TileAuraCharger.java — TileAuraCharger | 客户端更新 rotator；amount>0 且 vis<base*2.5 时每 100 tick 补 10 vis；同时从下方吸入 AURA | amount, capacity=10, isRotating(NetPropertyBool), rotator | TC AuraHandler.getAuraBase/getVis, PacketBlockEvent, FXVisSparkle
TileAspectFactory.java — TileAspectFactory | 懒建 8×3×8 的 TileJarFillable 搜索器并每 4 tick 更新；每秒按 AspectRule 把 contained 灌入 jar、超量抽回→toDissolve；每 tick 推进 AspectOperator 溶解 | contained, toDissolve, rules(IndexedMap<Aspect,AspectRule>), currentOperation, speed | TC TileJarFillable, AspectOperator, AspectRule, PositionedSearching
TileAspectCombiner.java — TileAspectCombiner | 更新 front/prev*；inA+inB 可合成则 100 tick 推进 craftingTime 并驱动 rotation；每秒从左/右吸入，向 UP 输出 | inA, inB, output, craftingTime/prevCraftingTime, rotation/prevRotation, toggle, vis/prevVis, prevPowered, front | TC TileJarFillable/TileThaumcraft, SoundsTC.pump, Aspect.getComponents
TileArcaneCake.java — TileArcaneCake | 无 tick（TileSyncable） | aspects | AspectUtil.getColor

jars/TileThaumiumJar.java   — TileThaumiumJar   | 继承 fillAbstractJar（无自有逻辑） | capacity=350 | TileAbstractJarFillable
jars/TileMithrilliumJar.java — TileMithrilliumJar | 继承 fillAbstractJar | capacity=1000 | TileAbstractJarFillable
jars/TileBrassJar.java      — TileBrassJar      | 继承 fillAbstractJar | capacity=275 | TileAbstractJarFillable
jars/TileEldritchJar.java   — TileEldritchJar   | 继承 fillAbstractJar | capacity=500 | TileAbstractJarFillable
jars/TileMithminiteJar.java — TileMithminiteJar | 继承 fillAbstractJar；覆写 addToContainer：>4000 部分按 1/(250-overfill) 概率污染 aura 并截断；suction 48/32 | amount(aspect 由首注入决定) | TC AuraHelper.polluteAura
jars/TileAdaminiteJar.java  — TileAdaminiteJar  | super.update() 后 time%1000 且随机 50% 时，若 FLUX 且 ≥90% 满则爆炸：掉方块、destroyBlock、污染全部 aura | time(仅运行时, 不写NBT), capacity=2000 | HCNet.spawnParticle, SoundUtil, WorldUtil.spawnItemStack
```

## 有可见动画 / 旋转角度状态（TESR 可用）

| Tile | 字段 | 类型 / 语义 | 同步情况 |
|---|---|---|---|
| **TileAuraCharger** | `rotator` (L36) + `isRotating` (L35) | `FrictionRotator`，`degree/prevDegree/speed/currentSpeed`，需 `getActualRotation(partialTicks)` | rotator **不写 NBT**，仅客户端 update；`isRotating` 是 NetPropertyBool（同步） |
| **TileCrystalBore** | `rotator` (L26)、`face`/`oldFace` (L28-29)、`boring` (L31) | FrictionRotator + 钻头朝向（插值用 oldFace）+ 进度 | rotator 不写 NBT；双端 `rotator.update()` |
| **TileCrystalCrusher** | `rotator` (L27)、`craftTime` (L33) | FrictionRotator + 进度条 | rotator 不写 NBT；仅客户端 update |
| **TileEntitySummoner** | `rotator` (L45) | FrictionRotator | **唯一把 rotator 5 个字段写入 NBT**（`CurrentSpeed/Degree/Friction/PrevDegree/Speed`，见 L87-106），双端 update |
| **TileFluxConcentrator** | `valve` (L35)、`facing` (L39)、`fluxed` (L38)、`fuel`/`vitium` | FrictionRotator(friction=.1) + 朝向 + 被腐化方块列表（渲染范围） | valve/facing 不写 NBT；`fluxed` 存 NBTTagList "FPS" |
| **TileAspectCombiner** | `rotation`/`prevRotation` (L35)、`toggle` (L38)、`craftingTime`/`prevCraftingTime`、`vis`/`prevVis` (L33)、`front` (L31) | 纯 float 手工插值，**非 FrictionRotator** | 全部写 NBT（Rotation/PrevRotation/Toggle/CraftingTime/Vis/PrevVis） |
| **TileSeal** | `orientation` (L29)、`instance`/`combination` | 朝向 + 印章实例（模型由实例决定） | `orientation` 不写 NBT（由 BlocksTAR.SEAL 的 EFACING 推导）；instance 存 "SealInstance" |
| **TileAbstractSmelter** | `furnaceCookTime`/`smeltTime`、`furnaceBurnTime`/`currentItemBurnTime`、`vis`/`capacity` | 箭头进度 + 燃烧条 + 流体条 | 存 NBT 但 **`vis`/`smeltTime` 不存**（加载时重算） |
| **TileGrowthChamber** | `toDrainAura` (L43)、`slotAspect`/`amount` | 无角度字段；`receiveClientEvent` 用 `ticksExisted*12` 算 sparkle 角 | `toDrainAura`/`amount` 写 NBT；`slotAspect` 不存（由槽0推导） |
| **TileAuraDisperser** | `tickTimer`/`timer`、`aspects` | 无角度字段，FX 由 proxy 方法驱动 | 全部写 NBT |
| **TileCrystalLamp** | 无角度字段 | `produceColoredLight` 用 `System.currentTimeMillis()` 正弦相位做呼吸 | 仅 `crystal` 同步 |

## 复刻时需注意的坑

1. **`FrictionRotator` 未持久化**：只有 `TileEntitySummoner` 写 NBT；`AuraCharger/CrystalBore/CrystalCrusher/FluxConcentrator` 的旋转角纯客户端推导，掉线重连/区块卸载后会跳变。
2. **`TileAspectCombiner.java:177`** 引用 `TileAbstractJarFillable.CAPACITY` —— 该类**没有**定义它，实际解析到继承的 TC `TileJarFillable.CAPACITY`（TC 原版 250）。复刻时要么显式定义常量，要么补 `instanceof` 之外的默认容量。
3. `TileAbstractSmelter` 与 `TileCraftingFurnace` 的 `tick()` 里 `isBurning()` / 燃烧递减在 `!world.isRemote` 之外执行（双端跑），移植时不要"顺手修正"，否则动画进度会变。
4. `TileCrystalBore.tick` 无条件 `rotator.update()`（双端），而 `TileCrystalCrusher`/`TileAuraCharger` 只在 `world.isRemote` 更新——刻意不同，照抄。
5. `TileAuraCharger.receiveClientEvent` 里有大段被注释掉的 ChainReaction 雷霆特效代码（L269-287）。
6. `TileAdaminiteJar.time` 每 tick 自增但**不写 NBT**，爆炸倒计时不随存档保留。
7. `TileGrowthChamber.tick` 开头就有槽1满则 return 的早退（L59-60），会让槽0的 `slotAspect` 保持上一 tick 值。