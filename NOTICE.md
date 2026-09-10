# 第三方代码署名

> **本库自身的许可**见根目录 [`LICENSE`](LICENSE)（MIT，`Copyright (c) 2025 Altnoir`）。
> **本文件**只负责记录 AbyssLib 内置/移植的**第三方代码**的署名与许可原文——这些代码各有其
> 原始版权人，MIT 要求保留其版权与许可声明。两者分工不同，请勿混淆：
> - `LICENSE` = 本库自己写的代码（注册框架扩展、模型加载器接线、创造栏、datagen 等）
> - `NOTICE.md` = 内置的第三方源码（Registrate → Reginth、Athena → 模型加载器）

---

# 第三方代码署名：Registrate（→ Reginth）

AbyssLib 自 **1.4.0** 起**内置（源码级合并）**了 Registrate 的 1.21 分支代码，并改名为 `Reginth`。

- 上游项目：https://github.com/tterrag1098/Registrate （源码分支 `1.21/dev`）
- 上游版本：`MC1.21-1.3.0+67`（取自 Gradle 缓存的 `Registrate-MC1.21-1.3.0+67-sources.jar`）
- 上游作者：tterrag1098 及 Registrate 贡献者
- 上游许可：**MIT**（MIT 要求保留版权与许可声明，原文见本文件末尾）

> ⚠️ **待核项（重要）**：搬运时所依据的 `-sources.jar` **不包含上游 LICENSE 文件**（jar 内只有
> `META-INF/MANIFEST.MF`），且本环境无外网，无法读取上游仓库的 `LICENSE`。因此下方
> "Registrate 原始许可（MIT）" 一节里的**版权行文字是按 MIT 模板填写的、尚未与上游原文逐字核对**。
> 请在有网络时打开 <https://github.com/tterrag1098/Registrate/blob/1.21/dev/LICENSE>
> 核对并按原文更正版权行（通常是 `Copyright (c) <年份> <作者/组织>`）。

## 内置范围

| 上游内容 | 处置 |
|---|---|
| `common/src/main/java/com/tterrag/registrate/**`（66 个 `.java`，6896 行） | 全部移植到 `com.altnoir.abysslib.reginth.**` |
| 上游 Gradle 构建脚本、`.github`、资源文件 | 未移植（本库用自己的 ModDevGradle 构建） |

## 相对上游的修改

1. **包名**：`com.tterrag.registrate.**` → `com.altnoir.abysslib.reginth.**`。
2. **类名**（**只有这两个**，其余约 60 个类型名保持不变）：
   - `AbstractRegistrate` → `AbstractReginth`
   - `Registrate` → `Reginth`
3. **`Reginth` 被重写**：上游 `Registrate` 只是一个 37 行的工厂类（`create(String)` + 构造器）。
   本库把**原 `ALRegistrate`** 的分区创造栏逻辑（`defaultCreativeSection` / `ignoreCreativeTab` /
   `isIgnoredCreativeTab`，以及覆写的 `block(...)` / `item(...)`）并入了 `Reginth`。
   **原 `ALRegistrate` 类已删除。**
4. **本库新增的两个类（上游没有）**：`reginth/builders/` 下的
   `ReginthBlockBuilder`（extends 上游 `BlockBuilder`）与 `ReginthItemBuilder`（extends 上游 `ItemBuilder`）。
   它们在纯上游 builder 之上加了"默认生成 blockstate/loot/lang"与"创造栏分区"两件事，
   由 `Reginth.block(...)` / `Reginth.item(...)` 返回。**与上游 diff 时请注意这两个文件是本库新增，
   以及同名包内其余文件才是搬运来的上游代码。**
5. **可见性调整**：因 `Reginth`（`…reginth`）与上述两个 builder（`…reginth.builders`）
   位于不同包，原先包内可见的 `ignoreCreativeTab` / `isIgnoredCreativeTab` /
   `ReginthBlockBuilder.create` / `ReginthItemBuilder.create` / `defaultCreativeSection` 放宽为 `public`。
6. **清理编译残留**：删除全部 `@javax.annotation.Generated(...)` 注解与 `import javax.annotation.Generated;`
   （JSR-250 自 Java 11 起已不在 JDK 中）；删除 3 个 loot 文件中 delombok 遗留的无用
   `import lombok.*`（`ReginthBlockLootTables` / `ReginthEntityLootTables` / `ReginthLootTableProvider`）。
7. **其余逻辑与上游一致**（含上游的行为特征与已知限制）。

---

# 第三方代码署名：Athena

AbyssLib 自 1.3.0 起**内置（源码级合并）**了 Athena 的 1.21.1 NeoForge 部分代码。

- 上游项目：https://github.com/terrarium-earth/Athena
- 上游分支/版本：`1.21.1` 分支，`gradle.properties` 版本 `4.0.6`（含 changelog 中 "Improve loading
  compatibility with other mods that try to inject into the top of the reload listeners" 一处修复）
- 上游作者：ThatGravyBoat / Terrarium Earth
- 上游许可：MIT（见下方原文）。Athena 为 MIT 许可，允许复制、修改、再分发，条件是保留版权与许可声明。

> 本库内的类名 / 包名 / 资源 id 已统一为 `abysslib`（类名 `AL*` 前缀、包 `com.altnoir.abysslib.model.**`）。本文件中的 `Athena` 与 `athena:*` 均指**上游**项目与其原始命名，属于 MIT 要求的署名与迁移说明，**不可替换**。

## 内置范围

| 上游模块 | 处置 |
|---|---|
| `common/src/main/java/earth/terrarium/athena/**` | 全部移植（25 文件），2 个 mixin 归入 `com.altnoir.abysslib.mixin` |
| `neoforge/src/main/java/earth/terrarium/athena/**` | 移植（10 文件中的 8 个），删去 `@Mod("athena")` 入口与 neoforge `ModelBakeryMixin` |
| `fabric/**` | 未移植（Indigo/FRAPI 渲染路径，本库只服务 NeoForge） |
| 上游 Gradle（Architectury + Loom + Shadow）、`.github`、`templates/` | 未移植 |

## 相对上游的修改（供日后与上游 diff 时参考）

1. **包名**：`earth.terrarium.athena.**` → `com.altnoir.abysslib.model.**`；
   mixin 类 `earth.terrarium.athena.mixins.**` → `com.altnoir.abysslib.mixin.**`。
2. **资源命名空间**：模型类型 id 由 `athena:*` 改为 `abysslib:*`；模型 JSON 的声明键由 `"athena:loader"`
   改为 `"abysslib:loader"`；几何加载器 id 由 `athena:athena` 改为 `abysslib:model`；
   定义文件扫描目录由 `assets/<ns>/athena/` 改为 `assets/<ns>/abysslib/`。
   （`DefaultModels.MODID` 现取 `AbyssLib.MOD_ID`。）
3. **平台分发**：`FactoryManager` 去掉 Architectury 的 `@ExpectPlatform`，直接转发 `FactoryManagerImpl`。
4. **入口**：上游 `AthenaNeoForge`（`@Mod("athena")`）删除，改为由 `AbyssLibClient`
   （`@Mod(value = "abysslib", dist = CLIENT)`）调用 `ALModelSetup.init(...)`。
5. **模型替换机制**：上游 neoforge `ModelBakeryMixin` 注入 `ModelBakery` 内 lambda（`method_61072`）；
   该合成方法名在 NeoForge 1.21.1 官方命名中不存在（实测 21.1.248 的 `ModelBakery.class` 为
   `lambda$bakeModels$N`，Mojang 官方映射与 NeoForm tsrg 中亦无 `method_61072`），且 ModDevGradle
   不生成 refmap。故改为公开事件 `ModelEvent.ModifyBakingResult`：对每个顶层模型 id 依次询问已注册的
   本库模型类型，命中则重新烘焙并替换。行为差异：作用域仅顶层模型（上游 lambda 同时覆盖模型内部引用）。
6. **本库扩展：发光（emissive）**：新增 JSON 键 `"abysslib:emissive"`（布尔，见 `ALModelAttributes.EMISSIVE_KEY`），
   上游 Athena 与上游 wiki 都没有该特性。开启后：面的 `ExtraFaceData` 写入 `(color, 15, 15, ambientOcclusion=false)`，
   并且 `BlockElement` 以 `shade=false` 构造。借 NeoForge 原生 baked lightmap
   （`FaceBakery` 写 quad 顶点光照；渲染端 `QuadLighter.computeLightingForQuad` 与
   `IVertexConsumerExtension.applyBakedLighting` 都取 `max(烘焙光照, 世界光照)`）得到"不受环境光照影响、
   贴图原色、暗处发光"的效果，且 AO 关闭时渲染器会改用 `FlatQuadLighter`，去掉方向性明暗。
   配套改动：上游只有 `athena:ctm` 解析属性，本库在 `DefaultModels.init()` 用 `withAttributes(...)` 包装
   全部内置类型的工厂，经 `AttributeOverrideModel` 装饰器把 `tint` / `render_type` / `abysslib:emissive`
   套到任意内置类型上（JSON 无属性键时不包装，与上游行为一致；消费方自定义类型不受影响）。
   **另一种用法（同样本库扩展）：不写 `abysslib:loader`、只写 `abysslib:emissive`** ——
   `ALModelSetup.wrapEmissive` 经 `ALModelDefinitions.getRawData(...)` 读到该键后，用
   `EmissiveWrappedModel`（extends NeoForge `BakedModelWrapper`）给**原版已烘焙模型**包一层：
   quad 复制后 `QuadTransformers.settingMaxEmissivity()`，并以 `shade=false` / `ambientOcclusion=false`
   重建，形状、贴图、`tintindex` 全部保留，**无需任何贴图字段**；物品栏变体（`inventory`）不处理。
   （`getRawData` 需注意：同一 blockstate 会被多资源包叠加成列表，必须优先取含 `abysslib:*` 键的那一份，
   否则会被原版那份盖掉——这是实现时踩过并已修正的坑。）
7. **新增 DEBUG 日志**：`ALModelSetup.onModifyBakingResult` 在接管模型时打印一行 DEBUG
   （`AbyssLib: replaced top-level model '<id>#<variant>' with model type <type>`），便于排查 loader 数据未被识别；
   生产日志级别为 INFO，默认不可见。
8. **核实过、未改动的上游行为**：上游静态 `tint` 会把颜色写进 `ExtraFaceData(color, 0, 0, true)`。
   由于 `applyBakedLighting`/`QuadLighter` 对烘焙光照取 `max`，写入 `0` 等价于"不干预光照"，
   所以这不会导致面变暗（本库实测确认），保持上游写法不变。
9. **本库扩展：OptiFine 式发光叠加层**：客户端配置 `config/abysslib-client.toml`（{@code ALClientConfig}）——
   `emissiveLayer`（默认 false）、`emissiveSuffix`（默认 `_e`）、`emissiveExclude`（前缀黑名单）。
   启用后 `ALModelSetup.wrapOverlay` 用 `EmissiveOverlayModel`（extends `BakedModelWrapper`）包装方块模型：
   对每个 quad 取 `sprite.contents().name()`，若图集中存在 `<name> + suffix`（`MissingTextureAtlasSprite` 判定不存在）
   就额外输出一层满亮 quad（`QuadTransformers.settingMaxEmissivity()` + `shade=false`/`ao=false`），
   基贴图 quad 原样不动（副本，避免污染共享实例）。渲染层对齐 OptiFine：跟随基贴图所在层，基贴图仅 `SOLID` 时
   叠加层走 `CUTOUT`（透明像素被 alpha 剔除），并用 `ChunkRenderTypeSet.union` 加入模型渲染层集合；
   "某状态有无叠加层"按状态缓存、"某贴图有无同族图"按贴图名缓存（`ConcurrentHashMap`，区块网格在工作线程构建）。
   上游 Athena 无此能力；仅覆盖方块 5 参 `getQuads` 路径（物品/生物/方块实体不处理）。
   **关键实现点**：烘焙 quad 的顶点 UV 已经是**图集绝对坐标**（`FaceBakery.bakeVertex` 写入
   `sprite.getU(uv / 16)`，渲染端 `VertexConsumer.putBulkData` 直接使用、不按 sprite 再映射），
   所以叠加层必须把 UV 从基 sprite 的图集区间**线性重映射**到 `<name>_e` sprite 的区间（`remapUvs`）；
   只替换 `BakedQuad#getSprite()` 会让叠加层继续采样**本体贴图**，表现为"整块方块都在发光"。
   叠加层保留基 quad 的 `tintIndex`（因此生物群系染色等对发光像素同样生效）。
10. **本库扩展：datagen 支持**：`com.altnoir.abysslib.datagen.ALModelDefinitionProvider` /
    `ALModelDefinition` 把「模型定义」（`assets/<modid>/abysslib/<方块>.json`，即定义目录形式）
    纳入 NeoForge {@code GatherDataEvent}，与 `RegistrateBlockstateProvider` 的模型/blockstate 生成共存
    （本 provider **不写 blockstate**，因此不会互相覆盖）。字段缺失在生成期即抛错；引用到的贴图会做存在性检查并汇总 WARN。
    上游 Athena 只有手写 JSON，无 datagen 支持。
11. 其余代码逻辑与上游保持一致（含上游的已知行为特征，如 `CtmUtils.getFromDir` 无守卫读取
    `BlockStateProperties.UP/DOWN/...`）。

---

## Athena 原始许可（MIT）

```
MIT License

Copyright (c) 2024 Terrarium Earth

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```

---

## Registrate 原始许可（MIT）

> ⚠️ 版权行**待与上游 LICENSE 原文核对**，见本文档开头的"待核项"。

```
MIT License

Copyright (c) tterrag1098 and Registrate contributors

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```
