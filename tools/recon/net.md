$ ls -R "H:\MinecraftMods\_ta-analysis\src\main\java\org\zeith\thaumicadditions\net" 2>/dev/null || find "H:/MinecraftMods/_ta-analysis/src/main/java/org/zeith/thaumicadditions/net" -type f
H:\MinecraftMods\_ta-analysis\src\main\java\org\zeith\thaumicadditions\net:
PacketBlockEvent.java
PacketCrystalBagAspectClick.java
PacketCrystalBagAspects.java
PacketHandleShadowLvl.java
PacketLeftClick.java
PacketRemovePotionEffect.java
PacketReorientPlayer.java
PacketSetVoidItemName.java
PacketShadowFX.java
PacketSyncEntity.java
PacketSyncRotationAndShootShadowBeamStaff.java
PacketSyncTARTag.java
PacketSyncTrunk.java
PacketTP.java
PacketTransfurmWolf.java
fxh

H:\MinecraftMods\_ta-analysis\src\main\java\org\zeith\thaumicadditions\net/fxh:
BaseParticlePacket.java
FXHPacket.java
FXShadowBeamParticle.java
PacketSpawnColorCloud.java
PacketSpawnColorDrop.java
PacketSpawnItemCrack.java


$ cd "H:/MinecraftMods/_ta-analysis/src/main/java/org/zeith/thaumicadditions/net" && wc -l *.java fxh/*.java
69 PacketBlockEvent.java
   54 PacketCrystalBagAspectClick.java
   47 PacketCrystalBagAspects.java
   60 PacketHandleShadowLvl.java
   53 PacketLeftClick.java
   46 PacketRemovePotionEffect.java
   40 PacketReorientPlayer.java
   57 PacketSetVoidItemName.java
   55 PacketShadowFX.java
   69 PacketSyncEntity.java
   58 PacketSyncRotationAndShootShadowBeamStaff.java
   63 PacketSyncTARTag.java
   83 PacketSyncTrunk.java
   44 PacketTP.java
   79 PacketTransfurmWolf.java
   57 fxh/BaseParticlePacket.java
   61 fxh/FXHPacket.java
   50 fxh/FXShadowBeamParticle.java
   50 fxh/PacketSpawnColorCloud.java
   45 fxh/PacketSpawnColorDrop.java
   53 fxh/PacketSpawnItemCrack.java
 1193 total


$ cd "H:/MinecraftMods/_ta-analysis/src/main/java/org/zeith/thaumicadditions" && grep -rn "PacketSyncEntity\|PacketSyncTrunk\|PacketTransfurmWolf\|PacketTP\b\|PacketReorientPlayer\|PacketBlockEvent\|PacketLeftClick\|PacketRemovePotionEffect\|PacketSpawnColorCloud\|PacketSpawnColorDrop\|PacketSpawnItemCrack\|FXShadowBeamParticle\|FXHPacket\|performBlockEvent" --include=*.java | grep -v "^net/Packet\|^net/fxh/Packet\|^net/fxh/FXH\|^net/fxh/Base"
entity/EntityBlueWolf.java:37:import org.zeith.thaumicadditions.net.PacketTransfurmWolf;
entity/EntityBlueWolf.java:75:			HCNet.INSTANCE.sendToAllAround(new PacketTransfurmWolf(wolf), new TargetPoint(wolf.world.provider.getDimension(), wolf.posX, wolf.posY, wolf.posZ, 128));
entity/EntityChester.java:35:import org.zeith.thaumicadditions.net.PacketSyncEntity;
entity/EntityChester.java:36:import org.zeith.thaumicadditions.net.PacketSyncTrunk;
entity/EntityChester.java:319:				HCNet.INSTANCE.sendToAllAround(new PacketSyncEntity(this), new TargetPoint(world.provider.getDimension(), posX, posY, posZ, 32));
entity/EntityChester.java:345:			HCNet.INSTANCE.sendTo(new PacketSyncTrunk(this), (EntityPlayerMP) player);
entity/EntityChester.java:542:			HCNet.INSTANCE.sendToAllAround(new PacketSyncEntity(this), new TargetPoint(world.provider.getDimension(), posX, posY, posZ, 32));
events/ClientEventReactor.java:64:import org.zeith.thaumicadditions.net.PacketLeftClick;
events/ClientEventReactor.java:174:				HCNet.INSTANCE.sendToServer(new PacketLeftClick(keyBindAttack));
items/armor/ItemMithminiteDress.java:31:import org.zeith.thaumicadditions.net.PacketRemovePotionEffect;
items/armor/ItemMithminiteDress.java:116:							HCNet.INSTANCE.sendTo(new PacketRemovePotionEffect(MobEffects.NIGHT_VISION), (EntityPlayerMP) mp);
items/weapons/ItemShadowBeamStaff.java:27:import org.zeith.thaumicadditions.net.fxh.FXShadowBeamParticle;
items/weapons/ItemShadowBeamStaff.java:162:			HCNet.INSTANCE.sendToAllAround(FXShadowBeamParticle.create(positions), new TargetPoint(playerIn.world.provider.getDimension(), cx, cy, cz, 256));
net/fxh/FXShadowBeamParticle.java:15:public class FXShadowBeamParticle
net/fxh/FXShadowBeamParticle.java:20:	public static FXShadowBeamParticle create(List<Vec3d> list)
net/fxh/FXShadowBeamParticle.java:22:		FXShadowBeamParticle p = new FXShadowBeamParticle();
proxy/fx/FXHandler.java:20:		HCNet.INSTANCE.sendToAllAroundTracking(new FXHPacket(tile.getPos(), 0), HCNet.point(tile.getWorld(), new Vec3d(tile.getPos()), 32));
proxy/fx/FXHandler.java:25:		HCNet.INSTANCE.sendToAllAroundTracking(new PacketSpawnItemCrack(pos, motion, stack), HCNet.point(world, pos, 32));
proxy/fx/FXHandler.java:30:		HCNet.INSTANCE.sendToAllAroundTracking(new PacketSpawnColorCloud(pos, targetPos, color.getRGB(), noclip), HCNet.point(world, pos, 32));
proxy/fx/FXHandler.java:35:		HCNet.INSTANCE.sendToAllAroundTracking(new PacketSpawnColorDrop(pos, targetPos, color.getRGB()), HCNet.point(world, pos, 32));
seals/magic/SealPortal.java:38:import org.zeith.thaumicadditions.net.PacketReorientPlayer;
seals/magic/SealPortal.java:39:import org.zeith.thaumicadditions.net.PacketTP;
seals/magic/SealPortal.java:168:							PacketTP tpp = new PacketTP();
seals/magic/SealPortal.java:172:							PacketReorientPlayer prp = new PacketReorientPlayer();
tiles/TileAuraCharger.java:16:import org.zeith.thaumicadditions.net.PacketBlockEvent;
tiles/TileAuraCharger.java:67:			PacketBlockEvent.performBlockEvent(world, pos, 1, 0);
tiles/TileGrowthChamber.java:21:import org.zeith.thaumicadditions.net.PacketBlockEvent;
tiles/TileGrowthChamber.java:112:				PacketBlockEvent.performBlockEvent(world, pos, 1, 0);

21 个文件全部读取完毕（含 `net/` 15 个 + `net/fxh/` 6 个）。`BaseParticlePacket` 为抽象基类，也计一行。

## 逐文件清单

```
PacketBlockEvent.java — PacketBlockEvent | 服务端->客户端 | int id, int type, long pos | 把 AuraCharger/GrowthChamber 的方块动画事件广播到周围 128 格客户端
PacketCrystalBagAspectClick.java — PacketCrystalBagAspectClick | 客户端->服务端 | Aspect a(按 tag), boolean shift, int btn | GUI 点要素 → ContainerCrystalBag.onAspectClick 增减/转移要素
PacketCrystalBagAspects.java — PacketCrystalBagAspects | 服务端->客户端 | AspectList list | 把玩家水晶袋当前要素列表同步给打开的 GuiCrystalBag
PacketHandleShadowLvl.java — PacketHandleShadowLvl | 客户端->服务端 | int action(1=升/2=降), Enchantment ench(注册名字符串) | 暗影附魔台请求升降某条附魔（先校验 openContainer）
PacketLeftClick.java — PacketLeftClick | 客户端->服务端 | boolean val | 同步按键攻击状态进玩家 NBT，并在 true 时调用主手 ILeftClickItem.onLeftClick
PacketRemovePotionEffect.java — PacketRemovePotionEffect | 服务端->客户端 | Potion potion(注册名) | 让客户端本地玩家移除指定药水效果（mithminite 套装夜视）
PacketReorientPlayer.java — PacketReorientPlayer | 服务端->客户端 | float yaw | 传送后延迟 5 tick 把客户端 yaw 对齐服务端，防止视角瞬移甩头
PacketSetVoidItemName.java — PacketSetVoidItemName | 客户端->服务端 | String name | 虚空修复台改名（服务端 filterAllowedCharacters + 长度 ≤35校验）
PacketShadowFX.java — PacketShadowFX | 服务端->客户端 | long pos, int color | 暗影附魔台播放 wispy motes 粒子（100 格广播）
PacketSyncEntity.java — PacketSyncEntity | 服务端->客户端 | UUID uuid, NBTTagCompound nbt, int id | 用服务端 NBT 覆盖客户端同 ID 实体状态（Pos保留客户端本地值）
PacketSyncRotationAndShootShadowBeamStaff.java — 同名 | 客户端->服务端 | float pitch, float yaw, EnumHand hand(存为 boolean) | 同步视角后由服务端执行影束法杖 handleRightClick
PacketSyncTARTag.java — PacketSyncTARTag | 服务端->客户端 | NBTTagCompound tag, UUID id | 广播玩家 TA 持久化 NBT（64 格，镰刀/镰刀形态触发 sync）
PacketSyncTrunk.java — PacketSyncTrunk | 服务端->客户端 | UUID uuid, NBTTagCompound nbt, int id | 同步 Chester 实体 NBT 并在客户端打开 GuiChester 背包界面
PacketTP.java — PacketTP | 服务端->客户端 | Vec3d target | 传送本地玩家到目标点，大距离时重载渲染器，并回包 PacketTeleportPlayer
PacketTransfurmWolf.java — PacketTransfurmWolf | 服务端->客户端 | int id(实体ID) | 把狼加入静态列表，客户端每 tick 在其身上放 spark 直到死亡/消失
fxh/BaseParticlePacket.java — BaseParticlePacket(abstract) | 服务端->客户端 | Vec3d pos, Vec3d motion | 粒子包抽象基类（PacketBuffer 序列化），子类实现 spawnParticle
fxh/FXHPacket.java — FXHPacket | 服务端->客户端 | BlockPos pos, int sub | 客户端在 TileAuraDisperser 位置生成光环分散器特效（仅 sub==0 生效）
fxh/FXShadowBeamParticle.java — FXShadowBeamParticle | 服务端->客户端 | List<Vec3d> positions(NBT: Count + P0..Pn) | 播放影束光柱 FXShadowBeam（256 格广播）
fxh/PacketSpawnColorCloud.java — PacketSpawnColorCloud | 服务端->客户端 | pos, motion, int color, boolean noclip | 彩色云粒子
fxh/PacketSpawnColorDrop.java — PacketSpawnColorDrop | 服务端->客户端 | pos, motion, int color | 彩色液滴粒子
fxh/PacketSpawnItemCrack.java — PacketSpawnItemCrack | 服务端->客户端 | pos, motion, ItemStack stack | 物品破损（裂纹）粒子
```

## 按用途分组

| 分组 | 数量 | 成员 |
|---|---|---|
| **GUI 同步** | 6 | PacketCrystalBagAspects(S→C)、PacketCrystalBagAspectClick(C→S)、PacketHandleShadowLvl(C→S)、PacketSetVoidItemName(C→S)、PacketSyncTrunk(S→C，开 Chester背包) |
| **密封（Seal）** | 2 | PacketTP、PacketReorientPlayer — 唯一调用点均为 `seals/magic/SealPortal.java:168,172` |
| **配置** | **0** | `net/` 下无任何包传输 `ConfigsTAR` 数据 |
| **其他** | 13 | 实体同步 1（PacketSyncEntity）、玩家 NBT 同步 1（PacketSyncTARTag）、输入/状态 2（PacketLeftClick、PacketRemovePotionEffect）、方块事件 1（PacketBlockEvent）、特效触发 2（PacketShadowFX、fxh/FXHPacket）、粒子 7（BaseParticlePacket、fxh/PacketSpawnColorCloud、fxh/PacketSpawnColorDrop、fxh/PacketSpawnItemCrack、fxh/FXShadowBeamParticle、PacketTransfurmWolf） |

补充事实（已核对调用点）：
- 方向分布：客户端→服务端5 个（S→C 侧无一个 `executeOnServer2` 与之对称），服务端→客户端 16 个（含抽象基类）。**无双向包**。
- 封印（Seal）系统本身没有专属网络包：全部经 `TileSeal`/`BlockSeal` 常规方块交互 + `SealInstance` 回调完成，门户效果借 `PacketTP`/`PacketReorientPlayer`。
- 若需“配置”分组，最接近的候选是 `PacketSyncTARTag`（玩家持久化数据广播），但它同步的是玩家 NBT 而非 config，我按事实归入「其他」。
- 所有包均基于 HammerCore `IPacket`，走 NBT（`writeToNBT`）或 `PacketBuffer`（仅 `fxh` 三个粒子子类用后者）。