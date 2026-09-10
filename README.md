# AbyssLib

Altnoir 系列模组的公共前置库。**NeoForge 1.21.1 / Java 21** · mod id `abysslib` · 包名 `com.altnoir.abysslib` · 许可 MIT

本库把两套常用基础设施**源码级内置**，消费方装上 `abysslib` 一个模组即可，**不需要**再单独装 Registrate / Athena：

| 内置内容 | 是什么 | 详见 |
|---|---|---|
| **`Reginth`**（注册框架） | fork 自 Registrate `MC1.21-1.3.0+67`，改命名空间；**额外提供分区式创造栏** | [§2](#2-注册框架-reginth) |
| **内置模型加载器** | 移植自 Athena（CTM / 拼接 / 柱状等 8 种动态模型）+ 两套发光方案 + datagen | [§3](#3-内置模型加载器) |

> 需要 **Simple Bedrock Model / mae** 的模组请自行声明（jitpack 坐标 + 各自 jarJar / compileOnly），本库不提供。

---

## 目录

- [0. 快速开始](#0-快速开始)
- [1. 分层与构建](#1-分层与构建)
- [2. 注册框架 Reginth](#2-注册框架-reginth)
  - [2.1 建立实例](#21-建立实例)
  - [2.2 注册方块、物品、实体等](#22-注册方块物品实体等)
  - [2.3 通用 ResourceLocation 工具](#23-通用-resourcelocation-工具)
- [3. 内置模型加载器](#3-内置模型加载器)
  - [3.1 三种摆放方式](#31-三种摆放方式)
  - [3.2 内置类型一览](#32-内置类型一览)
  - [3.3 发光方案 A：整模型满亮](#33-发光方案-a整模型满亮)
  - [3.4 发光方案 B：OptiFine 式叠加层](#34-发光方案-boptifine-式叠加层)
  - [3.5 用 datagen 生成定义](#35-用-datagen-生成定义推荐做法)
- [4. 分区式创造栏](#4-分区式创造栏)
  - [4.1 建标签页与分区](#41-建标签页与分区)
  - [4.2 横幅样式](#42-横幅样式albannerstyle)
- [5. 消费方接入](#5-消费方接入)
- [6. 迁移指南](#6-迁移指南)
- [7. 排错](#7-排错)
- [8. 分支与许可](#8-分支与许可)

---

## 0. 快速开始

**消费方 `build.gradle`**：

```gradle
repositories {
    maven { url = file("../AbyssLib/repo") }   // 本地发布仓库（先在 AbyssLib 下 ./gradlew publish）
}

dependencies {
    // Reginth 与模型加载器都在这一份 jar 里，无需声明任何其它前置依赖
    implementation("com.altnoir.abysslib:AbyssLib:1.4.0")
}
```

**`neoforge.mods.toml`**：

```toml
[[dependencies.你的modid]]
modId = "abysslib"
type = "required"
versionRange = "[1.0,)"
ordering = "AFTER"
side = "BOTH"
```

**模组入口**：

```java
public class MyMod {
    public static final String MOD_ID = "mymod";
    private static final Reginth REGINTH = Reginth.create(MOD_ID);

    public static Reginth reginth() {
        return REGINTH;
    }

    public static ResourceLocation loc(String path) {
        return AbyssLib.modloc(MOD_ID, path);
    }
}
```

**注册一个方块**（自动生成 blockstate / 战利品表 / 语言键）：

```java
public final class MyBlocks {
    public static final BlockEntry<Block> RUBY_BLOCK = MyMod.reginth()
            .block("ruby_block", Block::new)
            .simpleItem()          // 同时生成方块物品
            .register();

    public static void register() {}   // 空方法：供入口调用以触发类加载
}
```

> 注册类都要在入口构造函数里调一次 `MyBlocks.register();`，靠类初始化完成注册——这是 Registrate 体系的既有约定。

---

## 1. 分层与构建

**代码结构**（`src/main/java/com/altnoir/abysslib/`）：

| 位置 | 内容 |
|---|---|
| `reginth/` | 内置注册框架（搬运自 Registrate）；`Reginth` / `AbstractReginth` / `builders/` / `providers/` / `util/` |
| `reginth/builders/ReginthBlockBuilder`<br>`reginth/builders/ReginthItemBuilder` | **本库新增**（上游没有）：在纯上游 builder 之上加了"默认生成 blockstate/loot/lang"与"创造栏分区"，由 `Reginth.block()/item()` 返回 |
| `creative/` | 分区式创造栏：`ALCreativeTabSection` / `ALSectionedCreativeModeTab` / `ALBannerStyle` |
| `model/` | 内置模型加载器（全部 `AL*` 前缀） |
| `datagen/` | `ALModelDefinitionProvider` / `ALModelDefinition` |
| `client/` | `ALClientConfig`（发光叠加层配置）、`AbyssLibClient`（客户端接线入口） |
| `mixin/` | `ModelManagerMixin`、`ModelBakeryMixin`（仅 client 段） |

**构建与发布**：

```bash
./gradlew build      # 产物在 build/libs/
./gradlew publish    # 发布到 repo/（本地仓库），坐标为 com.altnoir.abysslib:AbyssLib:<版本>
```

**客户端边界**：分区横幅渲染与模型加载器都在 `AbyssLibClient`（`@Mod(dist = CLIENT)`）里初始化，
**专用服务端不会加载这些类**；mixin 配置 `abysslib.mixins.json` 只有 `client` 段。

---

## 2. 注册框架 Reginth

### 2.1 建立实例

```java
public class MyMod {
    private static final Reginth REGINTH = Reginth.create(MyMod.MOD_ID);

    public static Reginth reginth() {
        return REGINTH;
    }
}
```

`Reginth` 继承 `AbstractReginth<Reginth>`，上游 Registrate 的 API **全部保留**——
`entry(...)` / `generic(...)` / `block(...)` / `item(...)` / `entity(...)` / `addDataGenerator(...)` /
`defaultCreativeTab(...)` 等，只是包名与类名换成了本库命名空间。用法与上游文档一致。

> **铁律**：`Reginth` 的类就在 `abysslib.jar` 里，**不存在外部 Maven 坐标**。
> 所以消费方**不要**再声明或 `jarJar` Registrate / Reginth——运行时天然单副本，
> 跨模组传 `ItemEntry` / `BlockEntry` 也不会类型分裂。

### 2.2 注册方块、物品、实体等

`Reginth.block(...)` / `Reginth.item(...)` 返回的是本库的 `ReginthBlockBuilder` / `ReginthItemBuilder`，
比上游多两件默认行为：**自动生成 blockstate / 战利品表 / 语言键**（物品是模型 / 语言键），以及**创造栏分区支持**。

```java
// 方块（自动 blockstate + loot + lang，并自动创建方块物品）
public static final BlockEntry<Block> RUBY_BLOCK = REGINTH
        .block("ruby_block", Block::new)
        .simpleItem()
        .register();

// 物品（自动模型 + lang）
public static final ItemEntry<Item> RUBY = REGINTH
        .item("ruby", Item::new)
        .register();

// 实体
public static final EntityEntry<MyEntity> MY_ENTITY = REGINTH
        .entity("my_entity", MyEntity::new, MobCategory.CREATURE)
        .properties(p -> p.sized(0.5F, 0.6F))
        .renderer(() -> MyRenderer::new)
        .register();
```

需要更细的控制（自定义方块物品、自定义战利品表、不要物品等）时，直接用上游 builder 的既有 API，
例如 `block(...).loot(...)` / `.item(...)` / `.properties(...)` / `.blockstate(...)` / `.model(...)`。

> **`Supplier` 用哪个**：Reginth API 里需要 `Supplier` 的位置要用
> `com.altnoir.abysslib.reginth.util.nullness.NonNullSupplier`，不是 `java.util.function.Supplier`。

### 2.3 通用 ResourceLocation 工具

`AbyssLib` 入口类自带一组静态工具，消费方无需各自复制：

| 方法 | 说明 |
|---|---|
| `AbyssLib.modloc(namespace, path)` | 任意 `namespace:path` |
| `AbyssLib.mcloc(path)` | 原版 `minecraft:path` |
| `AbyssLib.parse(str)` / `tryParse(str)` | 解析 `"ns:path"`（严格抛错 / 宽松返回 null） |
| `AbyssLib.getItemPath(item)` / `getBlockPath(block)` / `getBlockKey(block)` | 物品/方块的注册名 path 或 ResourceLocation |

典型用法就是入口里的 `loc` 委托（见 [§0](#0-快速开始)）。

---

## 3. 内置模型加载器

[Athena](https://github.com/terrarium-earth/Athena)（MIT，Terrarium Earth）的 1.21.1 NeoForge 部分
**已源码级并入本库**：消费方可以直接写连接纹理（CTM）/ 拼接 / 柱状等动态模型，
**无需安装 Athena 模组，也无需自行打包**。上游来源与改动清单见 [`NOTICE.md`](NOTICE.md)。

**命名统一**：类名（`AL*`）、包名（`com.altnoir.abysslib.model.**`）、资源 id 都已改为 `abysslib`。
下文出现的 `athena:*` / `earth.terrarium.athena` 一律指**上游**写法，仅用于署名与迁移对照。

> **铁律**：加载器由本库唯一提供。**不要**再安装上游 Athena、也不要自行 jarJar 它。
> 两套并存不会崩，但各自只认自己的定义（本库 id 是 `abysslib:model`、目录是 `assets/<ns>/abysslib/`），
> 会出现"模型莫名不生效"的隐性故障。

### 3.1 三种摆放方式

定义要声明**模型类型** `"abysslib:loader"`（如 `"abysslib:ctm"`），有三种等效摆放位置：

**① blockstate 根**（上游 wiki 的规范写法，推荐照抄）

`assets/<你的modid>/blockstates/<方块名>.json`：

```json
{
  "variants": { "": { "model": "minecraft:block/air" } },

  "abysslib:loader": "abysslib:ctm",
  "ctm_textures": {
    "center":     "chipped:block/amethyst_block/ctm/cut_amethyst_block_column_ctm/3",
    "empty":      "chipped:block/amethyst_block/ctm/cut_amethyst_block_column_ctm/0",
    "horizontal": "chipped:block/amethyst_block/ctm/cut_amethyst_block_column_ctm/2",
    "vertical":   "chipped:block/amethyst_block/ctm/cut_amethyst_block_column_ctm/1",
    "particle":   "chipped:block/amethyst_block/cut_amethyst_block_column"
  }
}
```

`variants` 里的 `model` 只是占位（会被本库模型替换），wiki 统一写 `minecraft:block/air`。

**② 模型文件**（wiki 未收录）：把上面那个对象放进 `assets/<ns>/models/**.json`，
并补一个 `"loader": "abysslib:model"`，blockstate 里以**字符串**引用该模型。

```json
{
  "loader": "abysslib:model",
  "abysslib:loader": "abysslib:ctm",
  "ctm_textures": { "center": "…", "empty": "…", "horizontal": "…", "vertical": "…", "particle": "…" }
}
```

**③ 定义目录**（datagen 默认产出）：不带 `loader`，把该对象放到
`assets/<ns>/abysslib/<方块注册名>.json`。运行时优先读它。

> ⚠️ **1.21.1 原版限制**：blockstate 的 `variants.*.model` **只能写字符串**。写"内联模型对象"会在加载时报
> `Expected model to be a string, was an object`（实测确认）。需要 `"loader"` 的写法必须放进**模型文件**。

> **匹配语义**：本库用**方块 id**（`blockstates/<名字>.json` 的名字）查找定义，**不是**方块所用模型的 id。
> 物品模型变体（`inventory`）自动跳过，物品栏图标仍走普通模型。

**wiki 未记录、但代码支持**的字段：`connect_to`（连接条件树：`not` / `and` / `or` / `xor` /
`state`（可带 `properties`）/ `tag` / `sameBlock` / `sameState`）、
`render_type`（`solid` / `cutout` / `cutout_mipped` / `translucent`）、
`tint`（数字索引，或 `{r,g,b,a}` / `[r,g,b,a]` 固定色）。

**自定义 Java 类型**：`FactoryManager.register(ResourceLocation, ALModelFactory)`，
实现 `ALBlockModel`（`getQuads` / `getTextures` / 可选 `getDefaultQuads` / `getAttributes`）。

### 3.2 内置类型一览

| 类型 | 本库标识符 | 用途 | `ctm_textures` 键 |
|---|---|---|---|
| Full Cube CTM | `abysslib:ctm` | 整面连接纹理 | `center` / `empty` / `horizontal` / `vertical` / `particle` |
| Carpet CTM | `abysslib:carpet_ctm` | 地毯 / 薄板连接 | 同上五项 |
| Pane CTM | `abysslib:pane_ctm` | 玻璃板连接（含竖向剔除） | 同上五项 |
| Giant / Mural | `abysslib:giant`（别名 `abysslib:mural`） | 多格拼接大图 | `"1"`…`"width*height"` + `particle`，另需 `width` / `height` |
| Pillar | `abysslib:pillar` | 带 `AXIS` 属性的柱 | `self` / `top` / `center` / `bottom` / `particle` |
| Limited Pillar | `abysslib:limited_pillar` | 仅竖向的柱 | 同上五项 |
| Pane Pillar | `abysslib:pane_pillar` | 玻璃板柱 | 同上五项（另读 `edge` / `side_edge`） |

> 上游 wiki "Mural" 页给的标识符其实是 `athena:giant`（不是 `mural`）；本库两者都注册，与上游一致。
> `abysslib:ctm` 还额外支持"按方向分别给贴图 + `default` 回退"的写法（wiki 未记录）。

> **属性键对所有内置类型都生效**：上游只有 `athena:ctm` 解析 `tint` / `render_type`，
> 本库统一套了属性装饰器，因此 `carpet_ctm` / `pane_ctm` / `giant` / `pillar` / `limited_pillar` / `pane_pillar`
> 也能写 `render_type` / `tint` / `abysslib:emissive`（例如玻璃板 CTM 需要 `"render_type": "translucent"`）。

### 3.3 发光方案 A：整模型满亮

两种写法，效果相同——所有面**强制 15/15 光照并关闭 AO 与方向性明暗**，
于是不受环境光照影响、贴图什么颜色就显示什么颜色，暗处看起来就是发光。

**① 让原版/已有模型直接发亮**（最省事，不需要任何贴图字段）：blockstate 里**只写** `abysslib:emissive`

```json
{
  "variants": { "": { "model": "minecraft:block/stone" } },
  "abysslib:emissive": true
}
```

不写 `abysslib:loader` 时本库**不替换模型**：保留 `variants` 指向的原版模型（石头、楼梯、台阶，或你自己的模型），
只把每个面**复制一份**并写满光照——形状、贴图、`tintindex` / 生物群系染色全部照旧。
该键也可写进 `assets/<ns>/abysslib/<方块>.json` 定义文件（同样不需要 `abysslib:loader`）。

**② 本库模型 + 发光**：与 `abysslib:loader` 同级写（三种摆放方式都适用）

```json
{
  "variants": { "": { "model": "minecraft:block/stone" } },
  "abysslib:loader": "abysslib:ctm",
  "abysslib:emissive": true,
  "ctm_textures": { "center": "…", "empty": "…", "horizontal": "…", "vertical": "…", "particle": "…" }
}
```

实现走 NeoForge 原生机制（不需要光影）：`FaceBakery` 把 15/15 写进 quad 顶点光照，
渲染时 `QuadLighter` / `applyBakedLighting` 取 `max(烘焙光照, 世界光照)` → 永远满亮。

注意事项：

- **只影响外观**，不会照亮周围。要真正发光请另在 Java 侧用
  `BlockBehaviour.Properties.lightLevel(state -> 15)`（`block(...).properties(...)` 直接可写）。
- **写法①是"包裹"、写法②是"重建"**：① 保留模型原本的 `tintindex` / 生物群系染色（不会出现 §7 的灰度问题）；
  ② 由本库生成面，颜色需自己用 `tint` 指定。① 改写的是 quad 的**副本**，原版模型实例（可能被多个方块/状态共享）不受污染。
- 可与 `tint` / `render_type` 叠加（例如 `"render_type": "translucent"` + 发光玻璃）。
- 发光面是"平"的（无 AO、无方向明暗层次），这是刻意的。
- 光影（Iris/Oculus）下是否被当作 emissive 由光影包决定；原版渲染路径下必定满亮。

### 3.4 发光方案 B：OptiFine 式叠加层

想复刻 OptiFine "画一张 `iron_ore_e.png` 就发光"的体验时用这个（**默认关闭**）。

**1) 配置** `config/abysslib-client.toml`（游戏内 **模组列表 → AbyssLib → Config** 也能改）：

```toml
emissiveLayer = false        # 是否启用发光叠加层
emissiveSuffix = "_e"        # 叠加层贴图后缀；留空 = 关闭
emissiveExclude = []         # 不应用叠加层的贴图 / 命名空间前缀（防止第三方 _e 贴图被误用）
```

**2) 画叠加贴图**：基贴图 `iron_ore.png` → 同名加后缀 `iron_ore_e.png`，**只画发光像素**，其余留透明。
放在与基贴图同级，例如原版铁矿 `assets/minecraft/textures/block/iron_ore_e.png`，
自己的方块 `assets/<你的modid>/textures/block/xxx_e.png`。

**3) 生效**：打开 `emissiveLayer` 后自动应用，不需要 F3+T，也不需要写任何模型/blockstate JSON。
改动配置后的行为分级（日志会打印一行说明）：

- **关闭开关 / 改后缀 / 改排除表** → 自动重建区块网格（等价 F3+A，代价很小）；
- **从关闭切到开启** → 自动重载一次客户端资源（模型需要重新包一层，无法只靠重建网格完成）。

渲染语义（对齐 OptiFine）：

- 叠加层**满亮、不受环境光照影响**；
- **跟随基贴图的渲染层**；基贴图只有 `SOLID` 层时改用 `CUTOUT`（这样叠加图里的透明像素会被 alpha 剔除，而不是画成黑块）；
- 叠加贴图的 alpha 参与混合（半透明像素 = 半亮），可做柔光边缘；
- 基贴图 quad 完全不动（叠加层是**副本**），不影响共用同一模型/贴图的其它方块；
- **只对方块生效**（物品 / 生物 / 方块实体暂不支持）；
- 与 §3.3 的 `abysslib:emissive` 互不冲突，可叠加。

### 3.5 用 datagen 生成定义（推荐做法）

`ALModelDefinitionProvider` 生成的是**定义目录**形式（`assets/<modid>/abysslib/<方块>.json`），
**不改写 blockstate**，因此可以和你现有的 `RegistrateBlockstateProvider`（如 PoopSky 的 `BlockStateGen`）
**共存、互不覆盖**。

```java
// 1) 常规模型 / blockstate / 物品模型：照旧用现有 helper
//    （这份模型同时充当"模型加载器未生效时的回退外观"，也让物品栏图标正常）
simpleBlockWithItem(MyBlocks.GLOWING_ORE.get(),
        models().cubeAll("glowing_ore", modLoc("block/glowing_ore")));

// 2) CTM 定义（新增一个 provider）
public class CtmModelGen extends ALModelDefinitionProvider {
    public CtmModelGen(PackOutput output, ExistingFileHelper helper) {
        super(output, MyMod.MOD_ID, helper);
    }

    @Override
    protected void registerDefinitions() {
        // CTM 五连贴图自动推导：particle = 本体贴图，其余 = <dir>/ctm/<name>_ctm/0..3
        //（0=empty, 1=vertical, 2=horizontal, 3=center，与常见 CTM 材质包一致）
        ctm(MyBlocks.GLOWING_ORE).baseTexture("block/glowing_ore").emissive().save();

        // 柱类：显式给五张
        pillar(MyBlocks.POOP_PILLAR).pillarTextures(
                "block/pillar/self", "block/pillar/top", "block/pillar/center",
                "block/pillar/bottom", "block/poop_block").save();

        // 多格拼接：尺寸 + 编号贴图
        giant(MyBlocks.MURAL).size(2, 3).numberedTextures("block/mural/poop")
                .texture("particle", "block/poop_block").save();
    }
}

// 3) 挂到 GatherDataEvent（客户端资源）
generators.addProvider(event.includeClient(), new CtmModelGen(packOutput, existingFileHelper));
```

**入口方法**（都接受 `Block` 或 `BlockEntry`）：

| 入口 | 生成类型 | 必填字段（缺了 datagen 直接抛错） |
|---|---|---|
| `ctm(block)` | `abysslib:ctm` | particle / center / empty / vertical / horizontal |
| `carpetCtm(block)` | `abysslib:carpet_ctm` | 同上五张 |
| `paneCtm(block)` | `abysslib:pane_ctm` | 同上五张（可选 `paneEdges(edge, sideEdge)`） |
| `giant(block)` / `mural(block)` | 多格拼接 | `size(w,h)` + `"1".."w*h"` + particle |
| `pillar(block)` / `limitedPillar(block)` / `panePillar(block)` | 柱类 | particle / self / top / center / bottom |

**链式方法**（`ALModelDefinition`）：`baseTexture(...)`（可带 `ctmDir`）、`ctmDir(...)`、
`texture(key, rl)`、`pillarTextures(...)`、`paneEdges(...)`、`size(w,h)`、`numberedTextures(dir)`、
`emissive()`、`renderType("cutout"|"translucent"|…)`、`tint(0)` / `tint(r,g,b,a)`、
`property(key, json)`（逃生口，例如 `connect_to` 条件树）、`save()`。

- 生成路径 `assets/<你的modid>/abysslib/<方块注册名>.json`，与手写文件**完全等价**（运行时优先读它）；
- 生成时检查引用到的贴图是否存在，缺图汇总成一条 WARN（**不阻断**，因为贴图也可能来自资源包/其它模组）；
- 同一个方块重复声明会 WARN，并以最后一次为准。

---

## 4. 分区式创造栏

给创造栏标签页加"分区"（带标题横幅的分组）。**横幅渲染开箱即用**：随 `AbyssLibClient` 自动注册，
消费方无需任何客户端代码。

### 4.1 建标签页与分区

```java
public final class MyItemGroups {
    private static final Reginth REGINTH = MyMod.reginth();

    public static final ALCreativeTabSection TS_ITEMS = new ALCreativeTabSection("itemGroup.mymod.section.items");
    public static final ALCreativeTabSection TS_BLOCKS = new ALCreativeTabSection("itemGroup.mymod.section.blocks");

    public static final RegistryEntry<CreativeModeTab, CreativeModeTab> TAB = REGINTH.generic("main",
            Registries.CREATIVE_MODE_TAB, () ->
                    ALSectionedCreativeModeTab.configure(
                            CreativeModeTab.builder()
                                    .title(Component.translatable("itemGroup.mymod"))
                                    .icon(MyItems.SOME_ITEM::asStack),
                            MyItemGroups::populate,
                            TS_ITEMS, TS_BLOCKS
                    ).build()
    ).register();

    private static void populate(CreativeModeTab.ItemDisplayParameters parameters) {
        for (Item item : MyItems.getAllItems()) {
            TS_ITEMS.add(item);
        }
    }

    public static void register() {}
}
```

`ALSectionedCreativeModeTab.configure(...)` 有两个重载：

```java
configure(builder, populator, sections...)                    // 用默认横幅样式
configure(builder, bannerStyle, populator, sections...)        // 指定横幅样式
```

**分区内容的两种填充方式**（等价，可混用）：

```java
// 方式一：populate 回调里手动 add（上面的 MyItemGroups::populate）
TS_ITEMS.add(item);          // ItemLike
TS_ITEMS.add(itemStack);     // ItemStack
TS_ITEMS.add(() -> stack);   // 惰性 Supplier<ItemStack>
TS_ITEMS.clear();            // 清空

// 方式二：注册期自动归类（设置默认分区后，之后注册的方块/物品自动归入）
REGINTH.defaultCreativeSection(TS_ITEMS);

MyMod.reginth().item("some_item", Item::new).register();                 // → 自动进 TS_ITEMS
MyMod.reginth().item("no_tab_item", Item::new).ignore().register();      // → 排除，不进任何分区
MyMod.reginth().item("extra_item", Item::new)
        .addTabSection(MyItemGroups.TS_BLOCKS).register();               // → 额外进 TS_BLOCKS
```

> **注意**：分区会在每次 `buildContents` 清空后由标签页的 populate 重新填充。
> 因此依赖"注册期自动归类"的条目必须能被 populate 覆盖到（如遍历 `getAllItems()` 重新 add），
> 或直接用链式 API 手动归类——这与纯 populate 驱动的写法等价。

### 4.2 横幅样式（ALBannerStyle）

分区横幅是每个分区标题上方的那条色带/贴图。**样式按标签页各自独立**，建标签页时作为
`configure(...)` 的第二个参数传入；不传则用默认样式 `ALBannerStyle.DEFAULT`（绿色系纯色，整行）。

样式统一用**格数**（1~9）描述长度：每格 = 18px（创造栏一格宽），**9 = 整行 162px**。

| API | 说明 |
|---|---|
| `ALBannerStyle.colors(背景, 暗边框, 亮边框, 文字)` | 纯色，9 格整行（颜色为 ARGB，如 `0xFF123456`） |
| `ALBannerStyle.colors(格数, 背景, 暗边框, 亮边框, 文字)` | 纯色 + 指定格数 |
| `ALBannerStyle.texture(格数)` | 内置预设贴图（见下表） |
| `ALBannerStyle.texture(格数, "路径")` | 自定义贴图（支持 `"ns:path"` 或 ResourceLocation），拉伸到指定格数 |
| `样式.withUnits(格数)` | 在已有样式上改格数（纯色 / 贴图都有） |

**格数与像素宽**：`1→18`、`2→36`、`3→54`、`4→72`、`5→90`、`6→108`、`7→126`、`8→144`、`9→162`；
越界抛 `IllegalArgumentException`。

**内置预设贴图**位于本库 jar 的 `assets/abysslib/textures/gui/section/banner_1~9.png`，
N 号贴图宽 `N×18`、高 18，与格数精确对应，`texture(N)` 自动引入、像素级 1:1：

| `texture(n)` | 1 | 2 | 3 | 4 | 5 | 6 | 7 | 8 | 9 |
|---|---|---|---|---|---|---|---|---|---|
| 对应像素宽 | 18 | 36 | 54 | 72 | 90 | 108 | 126 | 144 | 162 |

```java
// 方式一：内置预设贴图，只写格数（texture(4) → banner_4.png，72px）
ALSectionedCreativeModeTab.configure(
        CreativeModeTab.builder().title(…).icon(…),
        ALBannerStyle.texture(4),
        MyItemGroups::populate, TS_ITEMS)

// 方式二：纯色 + 自定义格数
ALSectionedCreativeModeTab.configure(
        CreativeModeTab.builder().title(…).icon(…),
        ALBannerStyle.colors(6, 0xFF123456, 0xFF789ABC, 0xFFABCDEF, 0xFFFFFFFF),
        MyItemGroups::populate, TS_BLOCKS)

// 方式三：自定义贴图 + 格数
ALBannerStyle.texture(3, "mymod:textures/gui/creative/banner")
```

要点：

- **横幅与物品同行接续**：横幅 N 格时，该分区标题行行首 N 格被横幅占据，物品从右侧第 N+1 格开始同行排布
  （满 9 格换行）；N = 9 即横幅独占一整行、物品从下一行开始，与默认外观一致。
- 同一标签页内的所有分区**共用**该标签页的样式与格数（暂不支持一个标签页里分区各异）。
- 贴图模式标题文字固定白色带阴影（保证任何贴图上可读）；纯色模式用样式里的文字色。
- 自定义贴图建议为 18 的倍数宽、18 高；非匹配尺寸会整张拉伸到横幅宽度。

---

## 5. 消费方接入

`build.gradle`：

```gradle
repositories {
    maven { url = file("../AbyssLib/repo") }   // 本地发布仓库（先 ./gradlew publish）
    // 1.4.0 起注册框架已源码内置，不再需要 mvn.devos.one（Registrate）仓库。
    // 需要 SBM 的模组请自行再加 jitpack（SBM 不再由 AbyssLib 提供）
    // maven { url = "https://jitpack.io" }
}

dependencies {
    // Reginth 与模型加载器在同一份 jar 里；无需声明 Registrate / Reginth 等任何额外依赖，
    // 也不要再 jarJar 它们。
    implementation("com.altnoir.abysslib:AbyssLib:1.4.0")
}
```

`neoforge.mods.toml`：见 [§0](#0-快速开始)（required 依赖 `abysslib`）。

> **不再需要 `mvn.devos.one` 仓库**。旧版本（≤1.3.0）需要它解析 AbyssLib `api` 传递出的 Registrate 构件；
> 1.4.0 起 Reginth 的类直接随 jar 发布，那条仓库可以删掉。
> 同理，把 AbyssLib 设成 `{ transitive = false }` 也不再需要补 Registrate 的 `runtimeOnly`。

> **间接用到上游 Registrate 的情况**：若你依赖的第三方 mod（如 Create）把上游
> `com.tterrag.registrate.*` 类型写进了公开 API，你的代码**只要触碰那些字段**，javac 就需要该类型。
> 这不是 AbyssLib 的问题，解决办法是在你的工程里加
> `compileOnly("com.tterrag.registrate:Registrate:<上游版本>")`；dev 运行时若同样报缺类再加一条 `runtimeOnly`。
> 判断方法：`javap -cp <mod.jar> <类名>` 看其公开签名里有没有 `com.tterrag.registrate.*`。

---

## 6. 迁移指南

### 6.1 从上游 Athena 资源迁移

只需两步替换，**顺序不能反**：

```powershell
Get-ChildItem -Recurse -Filter *.json | ForEach-Object {
  $t = Get-Content $_.FullName -Raw
  $n = $t -replace 'athena:athena', 'abysslib:model'   # 先处理几何加载器 id（避免被下一步拆坏）
  $n = $n -replace 'athena:', 'abysslib:'               # 键名 athena:loader + 类型值 athena:ctm 等
  if ($t -ne $n) { Set-Content $_.FullName $n -Encoding utf8NoBOM }
}
```

目录 `assets/<ns>/athena/` 若有使用，改名为 `assets/<ns>/abysslib/` 即可（内容不变）。

| 上游（Athena） | 本库（AbyssLib） |
|---|---|
| 声明键 `"athena:loader"` | `"abysslib:loader"` |
| 模型类型 `athena:ctm` / `athena:carpet_ctm` / `athena:pane_ctm` / `athena:giant` / `athena:pillar` / `athena:limited_pillar` / `athena:pane_pillar` | 同名换前缀：`abysslib:ctm` … |
| 几何加载器 `"loader": "athena:athena"` | `"loader": "abysslib:model"` |
| 定义目录 `assets/<ns>/athena/**.json` | `assets/<ns>/abysslib/**.json` |
| **其余内容**（`variants`、`ctm_textures`、`width`、`height`、文件位置） | **保持不变** |
| Java 包 `earth.terrarium.athena.**` | `com.altnoir.abysslib.model.**` |

### 6.2 迁移到 Reginth（1.4.0 起，破坏性）

1.4.0 把外部 `com.tterrag.registrate:Registrate` 换成了源码内置的 `com.altnoir.abysslib.reginth`。
**包前缀变了，且所有 `Registrate*` 类名都改成了 `Reginth*`**：

| 旧 | 新 |
|---|---|
| 包 `com.tterrag.registrate.**` | 包 `com.altnoir.abysslib.reginth.**` |
| `AbstractRegistrate` | `AbstractReginth` |
| `Registrate` | `Reginth` |
| `RegistrateBlockstateProvider` / `RegistrateItemModelProvider` / `RegistrateLangProvider` / `RegistrateRecipeProvider` / `RegistrateDataProvider` / `RegistrateTagsProvider` / `RegistrateLootTableProvider` / `RegistrateAdvancementProvider` / … | 同上规则：`Registrate` → `Reginth`，其余不变 |
| `ALRegistrate`（本库旧类，**已删除**） | `Reginth`（分区创造栏逻辑已并入其中） |
| `ALBlockBuilder` / `ALItemBuilder`（旧名） | `ReginthBlockBuilder` / `ReginthItemBuilder`（位于 `…reginth.builders`） |

**不含 `Registrate` 的类型名一律不动**：`ProviderType`、`DataGenContext`、`BlockEntry`、`ItemEntry`、
`RegistryEntry`、`NonNullFunction` / `NonNullSupplier`、`*Builder` 等。

批量改写（只动 import 行）：

```powershell
Get-ChildItem -Recurse src -Filter *.java | ForEach-Object {
  $t = Get-Content $_.FullName -Raw
  $n = $t -replace 'com\.tterrag\.registrate\.AbstractRegistrate', 'com.altnoir.abysslib.reginth.AbstractReginth'
  $n = $n -replace 'com\.tterrag\.registrate\.Registrate\b',        'com.altnoir.abysslib.reginth.Reginth'
  $n = $n -replace 'com\.tterrag\.registrate',                      'com.altnoir.abysslib.reginth'
  $n = $n -replace '\bALRegistrate\b', 'Reginth'
  if ($t -ne $n) { Set-Content $_.FullName $n -Encoding utf8NoBOM }
}
```

再把 `abysslib_version` 提到 `1.4.0`。注意 `Registrate*Provider` 这类**类名**也要跟着改成 `Reginth*Provider`
（上面的脚本只处理 import 行，代码体里的类型引用需一并替换；`\bRegistrate` → `Reginth` 的词边界替换即可）。

---

## 7. 排错

**模型没被接管？** 把日志级别开到 DEBUG，接管时会打印：

```
AbyssLib: replaced top-level model '<方块id>#<变体>' with model type abysslib:<类型>
```

没有这行说明 loader 声明没被找到——检查 `"abysslib:loader"` 键名、方块 id 与 blockstate 文件名是否对应。
（生产环境日志为 INFO，默认不打印。）

**发光叠加层没生效？** DEBUG 下会打印：

```
AbyssLib: emissive overlay enabled for '<blockstate>' (base=..., overlay=..., separatePass=...)
```

没有这行说明基贴图没找到同后缀贴图——检查后缀、贴图路径/命名空间、是否被 `emissiveExclude` 排除。

**贴图变成灰度 / 纯色？** 本库模型是**自己生成面**的，不读原版模型 JSON 里的 `tintindex`：

- 用**灰度贴图**的方块（`grass_block_top`、`oak_leaves`、红石线…）必须在定义里显式给颜色：
  `"tint": 0`（数字 = 原版 `BlockColor`/`ItemColor` 的 tint 索引，**生物群系染色走这条**）或
  `"tint": [r,g,b,a]`（固定色）。**不写 tint 就是贴图原样**，灰度贴图自然显示成灰度。
- 同时开了 `"abysslib:emissive": true` 会更明显：不受光照、无方向明暗，看上去就是一张平的灰图。

**原版方块（草方块/泥土/石头…）被替换成奇怪贴图？** 先确认 `build/resources/main/assets/` 下有没有
调试用的 `minecraft/**` 覆盖残留，然后重新 `gradlew build`。本库源码只含 `assets/abysslib/**`，
**从不覆盖原版资源**（`jar` 任务也硬排除了 `assets/minecraft/**`）。

**专用服务端报客户端类加载？** 不应发生。分区横幅与模型加载器都在 `AbyssLibClient`
（`@Mod(dist = CLIENT)`）里初始化，mixin 配置只有 `client` 段。

---

## 8. 分支与许可

**分支**（按 MC 线分开维护）：

| 分支 / 目录 | 目标 | 关键差异 |
|---|---|---|
| `1.21.1-NeoForge`（本文档）`D:\Minecraft\ModDev\AbyssLib` | NeoForge 1.21.1 / Java 21 | 源码内置 `Reginth`（1.4.0 起）与模型加载器（1.3.0 起） |
| `26.1.2-NeoForge`（worktree）`D:\Minecraft\ModDev\AbyssLib-26.1.2` | NeoForge 26.1.2.94 / Java 25 | 仍以外部依赖方式使用 Registrate `MC26.1-1.5.7`；分区栏为 MIA-26.1 模型；**暂未内置模型加载器，也未内置注册框架** |

**许可**：本库自身代码为 **MIT**，见 [`LICENSE`](LICENSE)（`Copyright (c) 2025 Altnoir`）。

内置的第三方源码各有其原始版权人，署名与许可原文集中在 [`NOTICE.md`](NOTICE.md)：

| 内置内容 | 来源 | 许可 |
|---|---|---|
| `Reginth`（注册框架） | Registrate `MC1.21-1.3.0+67`（tterrag1098） | MIT |
| CTM / 动态模型加载器 | Athena 1.21.1（Terrarium Earth） | MIT |

> 上述声明**随产物一同打包**进 jar：`META-INF/LICENSE` 与 `META-INF/NOTICE.md`
> （由 `build.gradle` 的 `processResources` 从仓库根目录复制）。MIT 要求再分发时保留版权与许可声明，
> 因此这两个文件**不要从 build 配置里删掉**。
