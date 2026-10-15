# 本地 TC4R 开发依赖（20740 开发包）

TC4R 0.1.0-20740 来自用户提供的 20740 dev kit（`D:\Downloads\1.20.1-forge-20740.zip` 与
`D:\Downloads\1.21.1-neoforge-20740[-dev].zip`）。JAR 和 POM 保留在本机，不提交 Git，也不打入成品。

- `thaumcraft-forge/0.1.0-20740/`：完整运行时 JAR（SRG 映射；MDG 的 mod* 配置会重映射到官方名）
  + `-api` / `-sources`（来自 `D:\Downloads\1.20.1-forge-20740-dev.zip`，IDE 与 API 查阅用）
- `thaumcraft-neoforge/0.1.0-20740/`：完整 JAR（官方映射，兼作编译类路径）+ `-api` / `-sources`（IDE 用）

> **20740 API 变更（重要）**：Vis 与 Essentia 改为 Forge capability。
> `dev.tc4port.thaumcraft.api.essentia.EssentiaCapabilities`（`thaumcraft:essentia_transport` /
> `thaumcraft:essentia_source`）、`api.node.VisCapabilities`（`thaumcraft:vis_source`）。
> BE 实现 `EssentiaTransport` / `EssentiaSource` / `VisSource` 即自动提供对应 capability；
> 查询走 `EssentiaApi.transport/source/findSource` 与 `VisNetworkApi.drain/registerSource`，
> 不要再按 1.12 的方式直接拿 BE 强转或读 TC4R 内部字段。

`DEPENDENCIES.json` 记录全部文件的 SHA-1，用于校验。以后换 TC4R 版本时：放入新目录并更新清单。

后续 compat 阶段需要开发包内的附属（FM / Nodal Mechanics / Tainted Magic / Thaumic Tinkerer /
Warp Theory 20740）时，从同样的 zips 补充进本仓库。
