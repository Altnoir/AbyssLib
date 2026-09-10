# AbyssLib

Altnoir 系列模组的公共前置库（NeoForge 1.21.1 / Java 21）。

- 命名空间（mod id）：`abysslib`
- 包名：`com.altnoir.abysslib`
- 类前缀：`AL`（本库自己的类，如 `ALCreativeTabSection`）；内置框架为 `Reginth`
- 作者：Altnoir
- 功能：**源码级内置注册框架 `Reginth`**（fork 自 Registrate，命名空间 `com.altnoir.abysslib.reginth`）与分区式创造栏；**1.3.0 起源码内置 CTM / 动态模型加载器（移植自上游 Athena，命名统一为 `abysslib`）**（Simple Bedrock Model 不再由本库内置）

> **1.4.0 变更（破坏性）**：原本以 `jarJar` 引入的外部 `com.tterrag.registrate:Registrate` 已改为**源码级内置**，
> 包名与两个类名发生了变化，消费方需改 import——见 [迁移到 Reginth](#4-迁移到-reginth140-起)。

## 分支说明

本库按 MC 线分开维护（git 各占一个分支/工作树）：

| 分支/目录 | 目标 | 关键差异 |
|---|---|---|
| `1.21.1-NeoForge`（本文档）`D:\Minecraft\ModDev\AbyssLib` | NeoForge 1.21.1 / Java 21 | **源码内置注册框架 `Reginth`**（1.4.0 起，fork 自 Registrate `MC1.21-1.3.0+67`）；分区栏=populator + ALBannerStyle（纯色/格数贴图，`AbyssLibClient` 事件自动渲染）；**源码内置模型加载器**（`abysslib:model`、`abysslib:loader`） |
| `26.1.2-NeoForge`（worktree）`D:\Minecraft\ModDev\AbyssLib-26.1.2` | NeoForge 26.1.2.94 / Java 25 | 仍以外部依赖方式使用 Registrate `MC26.1-1.5.7`（gegy.dev）；分区栏=MIA-26.1 模型（`ALCreativeTabSection` 按 `Identifier` 收集 + 每分区 bannerSprite + mixin 渲染）；暂未内置该模型加载器 |

两线的共同策略：**只内置注册框架；Simple Bedrock Model / mae 不由本库内置**，需要 SBM 的模组自行声明。
（该模型加载器目前只在 1.21.1 线内置；注册框架的源码内置目前也只在 1.21.1 线。）

## 内置库（唯一提供者）

| 库 | 提供方式 | 版本 | 说明 |
|---|---|---|---|
| Reginth（fork 自 Registrate） | **源码级合并**（1.4.0 起） | 上游 `MC1.21-1.3.0+67` | 注册框架；包名 `com.altnoir.abysslib.reginth`，见 [§4 迁移到 Reginth](#4-迁移到-reginth140-起) |
| Athena | **源码级合并**（1.3.0 起） | 上游 1.21.1 分支 `4.0.6` | CTM / 动态模型加载器，已改名到 `abysslib` 命名空间，见 [内置模型加载器](#3-内置模型加载器ctm--动态模型130-起) |

**铁律（注册框架）**：`Reginth` 的类**就在 AbyssLib 的 jar 里**，不再来自外部 Maven 坐标。因此：
- 依赖本库的模组**无需**声明任何 Registrate / Reginth 依赖，也**不要**再 `jarJar` 它——类随 `abysslib` 模组一起提供
- 运行时天然只有一份类（不存在 jarJar 嵌套副本），跨模组传 `ItemEntry`/`BlockEntry` 不会类型分裂
- 消费方的 `mods.toml` 声明 required 依赖 `abysslib` 即可

> **注册框架已不再需要 `mvn.devos.one` 仓库**（该仓库原本只为解析 Registrate 构件而留）。
> 消费方若没有其它依赖用到它，可以删掉那行 `maven { url = "https://mvn.devos.one/snapshots" }`。

> **Simple Bedrock Model / mae 仍不由本库内置**。需要 SBM 的模组
> （如 PoopSkyMod）自行声明（jitpack 坐标 + 各自 jarJar / compileOnly mae），本库不保证其单副本。


## 构建 / 发布

```bash
./gradlew build          # 产物在 build/libs/
./gradlew publish        # 发布到 repo/（本地仓库，供其他模组引用）
```

> 注意：maven-publish 的 artifactId 取**项目名** `AbyssLib`，坐标为 `com.altnoir.abysslib:AbyssLib:<版本>`。

## 提供给模组的功能

### 0. 通用 ResourceLocation / 注册表路径工具

`AbyssLib` 入口类自带一组静态工具，消费方无需再各自复制这些方法：

| 方法 | 说明 |
|---|---|
| `AbyssLib.modloc(namespace, path)` | 任意 `namespace:path` |
| `AbyssLib.mcloc(path)` | 原版 `minecraft:path` |
| `AbyssLib.parse(str)` / `AbyssLib.tryParse(str)` | 解析 `"ns:path"`（严格抛错 / 宽松返回 null） |
| `AbyssLib.getItemPath(item)` / `getBlockPath(block)` / `getBlockKey(block)` | 物品/方块的注册名 path 或 ResourceLocation |

典型用法：各模组入口的 `loc` 委托即可（如 PoopSky/FilthDomain）：

```java
public static ResourceLocation loc(String path) {
    return AbyssLib.modloc(MOD_ID, path); // MOD_ID = 自己的 mod id
}
```

### 1. Reginth（通用注册框架实例）

模组入口持有一个绑定自己 mod id 的实例（注册名全部是自己的命名空间）：

```java
import com.altnoir.abysslib.reginth.Reginth;

public class MyMod {
    private static final Reginth REGINTH = Reginth.create(MyMod.MOD_ID);

    public static Reginth reginth() {
        return REGINTH;
    }
}
```

`Reginth` 继承自 `AbstractReginth<Reginth>`（fork 自上游 Registrate 的 `AbstractRegistrate<T>`），
上游的 `entry(...)` / `generic(...)` / `block(...)` / `item(...)` / `addDataGenerator(...)` 等 API 全部保留，
只是包名与类名换了命名空间。

### 2. 分区式创造栏（ALCreativeTabSection + ALSectionedCreativeModeTab）

建自己的分区与标签页：

> 分区标题横幅**开箱即用**：客户端渲染随 `AbyssLibClient` 自动注册，消费方无需任何客户端代码。
> 横幅样式（纯色 / 预设贴图 / 自定义贴图）、"格数"、物品列数联动等详见下方 **2.1 横幅样式**。

```java
public final class MyItemGroups {
    private static final Reginth REGINTH = MyMod.reginth();

    public static final ALCreativeTabSection TS_ITEMS = section("itemGroup.mymod.section.items");
    public static final ALCreativeTabSection TS_BLOCKS = section("itemGroup.mymod.section.blocks");

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

    private static ALCreativeTabSection section(String key) {
        return new ALCreativeTabSection(key);
    }

    public static void register() {
    }
}
```

注册内容走 `Reginth` 的 `block()` / `item()`（自动模型/语言/战利品），并用 `defaultCreativeSection(...)` 自动归类：

```java
public final class MyItems {
    public static final ItemEntry<Item> SOME_ITEM = REGINTH.item("some_item", Item::new)
            .register();

    // 链式 API：排除默认分区 / 额外加入某分区（block()/item() 均支持）
    public static final ItemEntry<Item> NO_TAB_ITEM = REGINTH.item("no_tab_item", Item::new)
            .ignore()
            .register();
    public static final ItemEntry<Item> EXTRA_TAB_ITEM = REGINTH.item("extra_tab_item", Item::new)
            .addTabSection(MyItemGroups.TS_BLOCKS)
            .register();
}
```

说明：`defaultCreativeSection(TS)` 设定后，后续注册的方块/物品自动归入 `TS`；
`ignore()` 把它排除（链式任意位置调用均生效）；`addTabSection(TS2)` 让它额外出现在 `TS2`。
**注意**：分区会在每次 `buildContents` 清空后由标签页的 populate 重新填充，因此依赖
"注册期自动归类"的条目必须能被 populate 覆盖到（如遍历 `getAllItems()` 重新 add），
或直接使用上述链式 API 手动归类——这与纯 populate 驱动的写法等价。

### 2.1 横幅样式（ALBannerStyle）

分区横幅是每个分区标题上方的那一条色带/贴图。**样式按标签页各自独立**，建标签页时作为
`ALSectionedCreativeModeTab.configure(...)` 的第二个参数传入；不传则使用 AbyssLib 默认样式
`ALBannerStyle.DEFAULT`（绿色系纯色，整行）。样式分**纯色**与**贴图**两类，统一用
"格数"（1~9）描述长度：每格 = 18px（创造栏一格宽），**9 = 整行 162px**。

| API | 说明 |
|---|---|
| `ALBannerStyle.colors(背景, 暗边框, 亮边框, 文字)` | 纯色，9 格整行（颜色为 ARGB，如 `0xFF123456`） |
| `ALBannerStyle.colors(格数, 背景, 暗边框, 亮边框, 文字)` | 纯色 + 指定格数 |
| `ALBannerStyle.texture(格数)` | 内置预设贴图（见下表） |
| `ALBannerStyle.texture(格数, "路径")` | 自定义贴图（支持 `"ns:path"` 或 ResourceLocation），拉伸到指定格数 |
| `样式.withUnits(格数)` | 在已有样式上改格数（纯色/贴图都有） |

**格数与像素宽**：`1→18`、`2→36`、`3→54`、`4→72`、`5→90`、`6→108`、`7→126`、
`8→144`、`9→162`；越界抛 `IllegalArgumentException`。

**内置预设贴图**：位于本库 jar 的 `assets/abysslib/textures/gui/section/banner_1~9.png`，
N 号贴图宽 N×18、高 18，与格数精确对应，`texture(N)` 自动引入、像素级 1:1：

| `texture(n)` | 1 | 2 | 3 | 4 | 5 | 6 | 7 | 8 | 9 |
|---|---|---|---|---|---|---|---|---|---|
| 对应像素宽 | 18 | 36 | 54 | 72 | 90 | 108 | 126 | 144 | 162 |

完整示例：

```java
public final class MyItemGroups {
    // …TS_ITEMS / TS_BLOCKS 等分区同上…

    // 方式一：内置预设贴图，只写格数（texture(4) → banner_4.png，72px）
    public static final RegistryEntry<CreativeModeTab, CreativeModeTab> TAB_A = REGINTH.generic("tab_a",
            Registries.CREATIVE_MODE_TAB, () ->
                    ALSectionedCreativeModeTab.configure(
                            CreativeModeTab.builder()
                                    .title(Component.translatable("itemGroup.mymod.a"))
                                    .icon(MyItems.SOME_ITEM::asStack),
                            ALBannerStyle.texture(4),
                            MyItemGroups::populate, TS_ITEMS
                    ).build()
    ).register();

    // 方式二：纯色 + 自定义格数（颜色模式同样支持长度）
    public static final RegistryEntry<CreativeModeTab, CreativeModeTab> TAB_B = REGINTH.generic("tab_b",
            Registries.CREATIVE_MODE_TAB, () ->
                    ALSectionedCreativeModeTab.configure(
                            CreativeModeTab.builder()
                                    .title(Component.translatable("itemGroup.mymod.b"))
                                    .icon(MyItems.SOME_ITEM::asStack),
                            ALBannerStyle.colors(6, 0xFF123456, 0xFF789ABC, 0xFFABCDEF, 0xFFFFFFFF),
                            MyItemGroups::populate, TS_BLOCKS
                    ).build()
    ).register();

    // 方式三：自定义贴图 + 格数
    // ALBannerStyle.texture(3, "mymod:textures/gui/creative/banner");
}
```

要点：

- **横幅与物品同行接续**：横幅 N 格时，该分区标题行行首 N 格被横幅占据，物品从右侧
  第 N+1 格开始同行排布（满 9 格换行）；N = 9 即横幅独占一整行、物品从下一行开始，
  与默认外观一致。
- 同一标签页内的所有分区共用该标签页的样式与格数（暂不支持一个标签页里分区各异；如有需要可扩展为分区级样式）。
- 贴图模式标题文字固定白色带阴影（保证任何贴图上可读）；纯色模式用样式里的文字色。
- 自定义贴图建议为 18 的倍数宽、18 高；非匹配尺寸会整张拉伸到横幅宽度。

### 3. 内置模型加载器（CTM / 动态模型，1.3.0 起）

[Athena](https://github.com/terrarium-earth/Athena)（MIT，Terrarium Earth）的 1.21.1 **NeoForge 部分已源码级并入本库**：
消费方可以直接写连接纹理（CTM）/ 拼接 / 柱状等动态模型，**无需再装 Athena 模组，也无需自行打包**。
上游来源、许可原文与"相对上游的改动清单"见 `NOTICE.md`（MIT 要求保留声明）。

> **命名统一**：本库内的类名（`AL*` 前缀，如 `ALQuad` / `ALBlockModel`）、包名（`com.altnoir.abysslib.model.**`）与资源 id 都已改为 `abysslib`。下文出现的 `athena:*` / `earth.terrarium.athena` 一律指**上游**写法，仅用于署名与迁移对照。

**命名空间已全部归入 `abysslib`**（上游为 `athena`），因此**既有 Athena 格式资源需要按对照表改写**：

| 上游（Athena） | 本库（AbyssLib） |
|---|---|
| 声明键 `"athena:loader"` | `"abysslib:loader"` |
| 模型类型 `athena:ctm` / `athena:carpet_ctm` / `athena:pane_ctm` / `athena:giant` / `athena:pillar` / `athena:limited_pillar` / `athena:pane_pillar` | 同名换前缀：`abysslib:ctm` … |
| 几何加载器 `"loader": "athena:athena"` | `"loader": "abysslib:model"` |
| 定义目录 `assets/<ns>/athena/**.json` | `assets/<ns>/abysslib/**.json` |
| **其余内容（`variants`、`ctm_textures`、`width`、`height`、文件位置）** | **保持不变** |
| Java 包 `earth.terrarium.athena.**` | `com.altnoir.abysslib.model.**` |

#### 3.1 用法（上游 wiki 的写法可以直接照抄）

上游文档：<https://wiki.terrarium.earth/athena>。**wiki 里的内容可以照搬使用，唯一要改的是命名空间前缀**。
wiki 的规范写法是把 loader 信息与数据写在**方块 blockstate JSON 的根**上，`variants` 里的 `model` 只是占位
（会被本库模型替换掉，wiki 统一写 `minecraft:block/air`）：

```json
{
  "variants": { "": { "model": "minecraft:block/air" } },

  "abysslib:loader": "abysslib:ctm",
  "ctm_textures": {
    "center": "chipped:block/amethyst_block/ctm/cut_amethyst_block_column_ctm/3",
    "empty": "chipped:block/amethyst_block/ctm/cut_amethyst_block_column_ctm/0",
    "horizontal": "chipped:block/amethyst_block/ctm/cut_amethyst_block_column_ctm/2",
    "particle": "chipped:block/amethyst_block/cut_amethyst_block_column",
    "vertical": "chipped:block/amethyst_block/ctm/cut_amethyst_block_column_ctm/1"
  }
}
```

（上例就是 wiki "Loader Introduction" 的示例，只把 `athena:` 换成 `abysslib:`；文件位置仍为
`assets/<自己的modid>/blockstates/<方块名>.json`。）

另外两种等效写法（**wiki 未收录**，上游代码与本库同样支持）：

1. **模型文件**：把上面那个对象（含 `abysslib:loader`）放进 `assets/<ns>/models/**.json`，
   并补一个 `"loader": "abysslib:model"`（走几何加载器注册），blockstate 里以**字符串**引用该模型；
2. **定义目录**：不带 `loader`，把该对象放到 `assets/<ns>/abysslib/<方块名>.json`（datagen 默认产出的就是这个）。

> ⚠️ 1.21.1 原版限制：blockstate 的 `variants.*.model` **只能写字符串**；写"内联模型对象"会在加载时报
> `Expected model to be a string, was an object`（实测确认）。需要 `"loader"` 的写法必须放进**模型文件**。

> **匹配语义**：本库用**方块 id**（即 `blockstates/<名字>.json` 的名字，如 `mymod:foo`）查找定义，
> 而不是方块所用模型的 id；物品模型变体（`inventory`）会被自动跳过，所以物品栏图标仍走普通模型。

**把既有 Athena 资源 / wiki 示例批量改成本库命名空间**（只需两步替换，顺序不能反）：

```powershell
Get-ChildItem -Recurse -Filter *.json | ForEach-Object {
  $t = Get-Content $_.FullName -Raw
  $n = $t -replace 'athena:athena', 'abysslib:model'   # 先处理几何加载器 id（避免被下一步拆坏）
  $n = $n -replace 'athena:', 'abysslib:'                # 键名 athena:loader + 类型值 athena:ctm 等
  if ($t -ne $n) { Set-Content $_.FullName $n -Encoding utf8NoBOM }
}
```

目录 `assets/<ns>/athena/` 若有使用，改名为 `assets/<ns>/abysslib/` 即可（内容不变）。

**wiki 未记录、但代码支持的字段**（上游 1.21.1 代码就有）：
`connect_to`（连接条件树：`not` / `and` / `or` / `xor` / `state`（可带 `properties`）/ `tag` / `sameBlock` / `sameState`）、
`render_type`（`solid` / `cutout` / `cutout_mipped` / `translucent`）、`tint`（数字索引，或 `{r,g,b,a}` / `[r,g,b,a]` 固定色）。

#### 3.1.1 发光 / emissive（本库扩展，上游 Athena 与 wiki 都没有）

有两种写法，效果相同：

**① 让原版/已有模型直接发亮（最省事，不需要任何贴图字段）** —— blockstate 里**只写** `abysslib:emissive`：

```json
{
  "variants": { "": { "model": "minecraft:block/stone" } },

  "abysslib:emissive": true
}
```

不写 `abysslib:loader` 时，本库**不替换模型**：保留 `variants` 指向的那个模型（原版石头、楼梯、台阶，
或你自己的模型 JSON），只把它的每个面**复制一份**并写满光照 —— 形状、贴图、`tintindex`/生物群系染色全部照旧。
适合"只想让某个现成方块在黑暗里发亮"。物品栏变体（`inventory`）不受影响。
该键也可写进 `assets/<ns>/abysslib/<方块>.json` 定义文件（同样不需要 `abysslib:loader`）。

**② 本库模型 + 发光** —— 与 `abysslib:loader` 同级写（三种摆放方式都适用，所有内置类型都生效）：

```json
{
  "variants": { "": { "model": "minecraft:block/stone" } },

  "abysslib:loader": "abysslib:ctm",
  "abysslib:emissive": true,
  "ctm_textures": { "center": "…", "empty": "…", "horizontal": "…", "vertical": "…", "particle": "…" }
}
```

两种写法都让该模型的所有面**强制 15/15 光照并关闭 AO 与方向性明暗**，于是
**不受环境光照影响、贴图什么颜色就显示什么颜色**，在黑暗环境里看起来就是发光。

实现走 NeoForge 原生机制（不需要光影）：`FaceBakery` 把 15/15 写进 quad 顶点光照，渲染时
`QuadLighter`/`applyBakedLighting` 取 `max(烘焙光照, 世界光照)` → 永远满亮。

注意事项：

- **只影响外观**，不会让方块照亮周围；要真正发光请另外在 Java 侧用
  `BlockBehaviour.Properties.lightLevel(state -> 15)`（Registrate 的 `block(...).properties(...)` 直接可写）。
- 可与 `tint` / `render_type` 叠加（例如 `"render_type": "translucent"` + 发光玻璃）。
- 发光面是"平"的（无 AO、无方向明暗层次），这是刻意的，符合发光材质预期。
- 光影（Iris/Oculus）下是否被当作 emissive 由光影包决定；原版渲染路径下必定满亮。
- 只写 `abysslib:emissive` 的模型若同时需要 `tint` 的**颜色**，两者可共存（颜色仍由 `ExtraFaceData.color` 施加）。
- **写法①是"包裹"、写法②是"重建"**：① 保留模型原本的 `tintindex` / 生物群系染色（不会出现 §3.1.2 的灰度问题）；
  ② 由本库生成面，颜色需自己用 `tint` 指定。①改写的是 quad 的**副本**，原版模型实例（可能被多个方块/状态共享）不受污染。

#### 3.1.2 常见现象：为什么贴图变成灰度 / 纯色？

本库模型是**自己生成面**的，不读原版模型 JSON 里的 `tintindex`：

- 用**灰度贴图**的方块（`grass_block_top`、`oak_leaves`、红石线…）必须在模型定义里显式给颜色：
  `"tint": 0`（数字 = 原版 `BlockColor`/`ItemColor` 的 tint 索引，**生物群系染色走这条**）或
  `"tint": [r,g,b,a]`（固定色）。**不写 tint 就是贴图原样**，灰度贴图自然显示成灰度。
- 同时开了 `"abysslib:emissive": true` 会更明显：不受光照、无方向明暗，看上去就是一张平的灰图。
- 排错用：把日志级别开到 DEBUG，模型被接管时会打印
  `AbyssLib: replaced top-level model '<方块id>#<变体>' with model type abysslib:<类型>`；
  没有这行说明 loader 声明没被找到（检查键名与方块 id 对应关系）。
- 若在 AbyssLib 开发环境里发现**原版方块**（草方块/泥土/石头…）被替换成奇怪的贴图，先确认
  `build/resources/main/assets/` 下有没有调试用的 `minecraft/**` 覆盖残留并重新 `gradlew build`：
  本库源码只含 `assets/abysslib/**`，**从不覆盖原版资源**。

#### 3.1.3 OptiFine 式发光叠加层（画一张 `_e` 贴图就发光，客户端配置）

想复刻 OptiFine "画一张 `iron_ore_e.png` 就发光"的体验时用这个（**默认关闭**）：

1. 配置 `config/abysslib-client.toml`（游戏内 **模组列表 → AbyssLib → Config** 也能改）：

   ```toml
   # 是否启用发光叠加层（默认 false = 关闭）
   emissiveLayer = false
   # 叠加层贴图后缀（默认 "_e"；留空 = 关闭）
   emissiveSuffix = "_e"
   # 不应用叠加层的贴图 / 命名空间前缀（前缀匹配，防止第三方 _e 贴图被误用）
   emissiveExclude = []
   ```

2. 画叠加贴图：基贴图 `iron_ore.png` → 同名加后缀 `iron_ore_e.png`，**只画发光像素**，其余留透明。
   放在与基贴图同级的位置，例如原版铁矿要放 `assets/minecraft/textures/block/iron_ore_e.png`；
   自己的方块则 `assets/<你的modid>/textures/block/xxx_e.png`。

3. 打开 `emissiveLayer` 后**自动生效**，不需要手动 F3+T，也不需要写任何模型/blockstate JSON。
   改动配置后的行为分级（日志里会打印一行说明做了什么）：
   - **关闭开关 / 改后缀 / 改排除表** → 自动重建区块网格（等价 F3+A，代价很小）；
   - **从关闭切到开启** → 自动重载一次客户端资源（方块模型需要重新包一层，无法只靠重建网格完成）。

渲染语义（对齐 OptiFine）：

- 叠加层**满亮、不受环境光照影响**（写满 15/15 光照 + 关 AO 与方向明暗）；
- 叠加层**跟随基贴图的渲染层**；基贴图只有 `SOLID` 层时改用 `CUTOUT`（这样叠加图里的透明像素会被 alpha 剔除，
  而不是画成黑块）；
- 叠加贴图的 alpha 参与混合（半透明像素 = 半亮），可做柔光边缘；
- 基贴图 quad 完全不动（叠加层是**副本**），不会影响共用同一模型/贴图的其它方块；
- 只对**方块**生效（物品 / 生物 / 方块实体的 `_e` 暂不支持）；
- 与 §3.1.1 的 `abysslib:emissive`（整模型满亮）互不冲突，可叠加使用。

排错：日志开到 DEBUG 后会打印
`AbyssLib: emissive overlay enabled for '<blockstate>' (base=..., overlay=..., separatePass=...)`；
没有这行说明基贴图没找到同后缀贴图（查后缀、贴图路径/命名空间、是否被 `emissiveExclude` 排除）。

#### 3.2 内置模型类型（对照 wiki 页面）

| wiki 页面 | wiki 标识符 | 本库标识符 | 用途 | `ctm_textures` 键 |
|---|---|---|---|---|
| Full Cube CTM | `athena:ctm` | `abysslib:ctm` | 整面连接纹理 | `center` / `empty` / `horizontal` / `vertical` / `particle` |
| Carpet CTM | `athena:carpet_ctm` | `abysslib:carpet_ctm` | 地毯 / 薄板连接 | 同上五项 |
| Pane CTM | `athena:pane_ctm` | `abysslib:pane_ctm` | 玻璃板连接（含竖向剔除） | 同上五项 |
| Mural | `athena:giant` | `abysslib:giant`（别名 `abysslib:mural`） | 多格拼接大图 | `"1"`…`"width*height"` + `particle`，另需 `width` / `height` |
| Pillar | `athena:pillar` | `abysslib:pillar` | 带 `AXIS` 属性的柱 | `self` / `top` / `center` / `bottom` / `particle` |
| Limited Pillar | `athena:limited_pillar` | `abysslib:limited_pillar` | 仅竖向的柱 | 同上五项 |
| Pane Pillar | `athena:pane_pillar` | `abysslib:pane_pillar` | 玻璃板柱 | 同上五项（代码另读 `edge` / `side_edge`） |

> 注意：wiki "Mural" 页给出的标识符是 `athena:giant`（不是 `mural`）；本库两者都注册，与上游一致。
> `athena:ctm` 还额外支持"按方向分别给贴图 + `default` 回退"的 `ctm_textures` 写法（wiki 未记录）。
>
> **属性键对所有内置类型都生效**：上游只有 `athena:ctm` 解析 `tint` / `render_type`，本库在注册内置类型时
> 统一套了一层属性装饰器，因此 `carpet_ctm` / `pane_ctm` / `giant` / `pillar` / `limited_pillar` / `pane_pillar`
> 也能写 `render_type`、`tint` 与 `abysslib:emissive`（例如玻璃板 CTM 需要 `"render_type": "translucent"`）。

自定义类型（Java）：`FactoryManager.register(ResourceLocation, ALModelFactory)` 注册新类型，
实现 `ALBlockModel`（`getQuads` / `getTextures` / 可选的 `getDefaultQuads` / `getAttributes`）。

#### 3.3 铁律（模型加载器）

- **模型加载器由 AbyssLib 唯一提供**：消费方与整合**不要**再安装上游 Athena 模组、也不要自行 jarJar Athena。
  本库的加载器 id（`abysslib:model`）、资源目录（`assets/<ns>/abysslib/`）与类包名都与上游不同，
  两套并存时不会崩，但**各自只认自己的定义**，会出现"模型莫名不生效"的隐性故障。
- 消费方 `build.gradle` **不需要**新增任何依赖：这些类就在 `abysslib.jar` 内（编译期可见、运行时由本库提供）。
- 本库相对上游的实现差异：顶层模型替换改用公开事件 `ModelEvent.ModifyBakingResult`
  （上游用 mixin 注入 `ModelBakery` 内 lambda `method_61072`，该合成方法名在 NeoForge 1.21.1 官方命名下不存在，
  且 ModDevGradle 不生成 refmap）；其余逻辑与上游一致，包括上游的已知行为特征
  （如 `CtmUtils.getFromDir` 会无守卫读取方块的 `UP/DOWN/NORTH/SOUTH/WEST/EAST` 属性）。
- **排错**：把日志级别开到 DEBUG 后，模型被接管时会打印
  `AbyssLib: replaced top-level model '<方块id>#<变体>' with model type abysslib:<类型>`；
  没有这行说明 loader 数据没被找到（检查 `"abysslib:loader"` 键名、方块 id 与 blockstate 文件名是否对应）。
  （生产环境日志为 INFO，默认不打印。）

#### 3.5 用 datagen 生成模型定义（推荐做法）

CTM 定义文件可以像其它资源一样用 datagen 生成：本库提供 `ALModelDefinitionProvider`，
它写的是**定义目录**形式（`assets/<modid>/abysslib/<方块>.json`），因此**不改写 blockstate**，
可以和你现有的 `RegistrateBlockstateProvider`（如 PoopSky 的 `BlockStateGen`）**共存、互不覆盖**。

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

| 入口（`ALModelDefinitionProvider`） | 生成类型 | 必填字段（缺了 datagen 直接抛错） |
|---|---|---|
| `ctm(block)` | `abysslib:ctm` | particle / center / empty / vertical / horizontal |
| `carpetCtm(block)` | `abysslib:carpet_ctm` | 同上五张 |
| `paneCtm(block)` | `abysslib:pane_ctm` | 同上五张（可选 `paneEdges(edge, sideEdge)`） |
| `giant(block)` / `mural(block)` | 多格拼接 | `size(w,h)` + `"1".."w*h"` + particle |
| `pillar(block)` / `limitedPillar(block)` / `panePillar(block)` | 柱类 | particle / self / top / center / bottom |

链式方法（`ALModelDefinition`）：`baseTexture(...)`、`ctmDir(...)`、`texture(key, rl)`、
`pillarTextures(...)`、`paneEdges(...)`、`size(w,h)`、`numberedTextures(dir)`、
`emissive()`、`renderType("cutout"|"translucent"|…)`、`tint(0)` / `tint(r,g,b,a)`、
`property(key, json)`（逃生口，例如 `connect_to` 条件树）。

- 生成路径：`assets/<你的modid>/abysslib/<方块注册名>.json`，与手写文件**完全等价**（运行时优先读它）；
- 生成时会检查引用到的贴图是否存在，缺图汇总成一条 WARN（**不阻断**，因为贴图也可能来自资源包/其它模组）；
- 同一个方块重复声明会 WARN，并以最后一次为准。

### 4. 消费方 build.gradle 接入

```gradle
repositories {
    maven { url = file("../AbyssLib/repo") }      // 本地发布仓库（先 ./gradlew publish）
    // 1.4.0 起注册框架已源码内置，不再需要 mvn.devos.one（Registrate）仓库。
    // 需要 SBM 的模组请自行再加 jitpack（SBM 不再由 AbyssLib 提供）
    // maven { url = "https://jitpack.io" }
}

dependencies {
    // AbyssLib 自带注册框架（Reginth）与 CTM/动态模型加载器，均在同一份 jar 里：
    // 本模组无需声明 Registrate / Reginth / 其它任何额外依赖，也不要再 jarJar 它们。
    implementation("com.altnoir.abysslib:AbyssLib:1.4.0")
    // 需要 SBM / mae 的模组自行声明（jitpack 坐标 + jarJar / compileOnly），不再由本库提供。
}
```

> **不需要 `mvn.devos.one` 仓库**。旧版本（≤1.3.0）需要它来解析 AbyssLib `api` 依赖传递出的
> Registrate 构件；1.4.0 起 Reginth 的类直接随 `AbyssLib` 的 jar 发布，那条仓库可以删掉。
> 同理，把 AbyssLib 设成 `{ transitive = false }` 也不再需要补 Registrate 的 `runtimeOnly`。

mods.toml 声明：

```toml
[[dependencies.你的modid]]
modId = "abysslib"
type = "required"
versionRange = "[1.0,)"
ordering = "AFTER"
side = "BOTH"
```

### 5. 迁移到 Reginth（1.4.0 起）

1.4.0 把外部 `com.tterrag.registrate:Registrate` 换成了源码内置的 `com.altnoir.abysslib.reginth`。
**只有包前缀和两个类名变了**，其余 60 个类型名全部保持原样：

| 旧 | 新 |
|---|---|
| `com.tterrag.registrate.**` | `com.altnoir.abysslib.reginth.**` |
| `com.tterrag.registrate.AbstractRegistrate` | `com.altnoir.abysslib.reginth.AbstractReginth` |
| `com.tterrag.registrate.Registrate` | `com.altnoir.abysslib.reginth.Reginth` |
| `ALRegistrate`（本库旧类，已删除） | `Reginth`（分区创造栏逻辑已并入其中） |

像 `RegistrateBlockstateProvider` / `RegistrateItemModelProvider` / `RegistrateLangProvider` /
`RegistrateRecipeProvider` / `RegistrateDataProvider` / `RegistrateTagsProvider` / `ProviderType` /
`DataGenContext` / `BlockEntry` / `ItemEntry` / `RegistryEntry` / `NonNullFunction` 这些
**只改包名，类名不动**。

批量改写（只动 import 行，其余步骤顺序不能反）：

```powershell
Get-ChildItem -Recurse src -Filter *.java | ForEach-Object {
  $t = Get-Content $_.FullName -Raw
  $n = $t -replace 'com\.tterrag\.registrate\.AbstractRegistrate', 'com.altnoir.abysslib.reginth.AbstractReginth'
  $n = $n -replace 'com\.tterrag\.registrate\.Registrate\b', 'com.altnoir.abysslib.reginth.Reginth'
  $n = $n -replace 'com\.tterrag\.registrate', 'com.altnoir.abysslib.reginth'
  if ($t -ne $n) { Set-Content $_.FullName $n -Encoding utf8NoBOM }
}
```

然后把你代码里的 `ALRegistrate` 改成 `Reginth`（构造/持有/方法返回类型），
并把 `abysslib_version` 提到 `1.4.0`。

## 备注

- （1.21.1 线）分区标题横幅渲染内置并随 `AbyssLibClient` 自动注册，无需客户端 hook；样式（纯色/预设/自定义贴图）与格数见 **2.1 横幅样式**。
- （1.21.1 线）内置模型加载器也全部走客户端：模型类型注册、几何加载器与顶层模型替换都在 `AbyssLibClient`
  （`@Mod(dist = CLIENT)`）里初始化，**专用服务端不会加载这些类**；mixin 配置为 `abysslib.mixins.json`
  （`com.altnoir.abysslib.mixin`，仅 `client` 段），已在 `neoforge.mods.toml` 模板以 `[[mixins]]` 声明。
- 数据生成注意：多模组并存时 Registrate 的 unassociated BLOCK_TAGS 生成器存在并发竞态（ConcurrentModificationException），世界生成标签建议用自定义 DataProvider 在 addTags 阶段直填（参考 PoopSky-FilthDomain 的 FDTagsProvider 做法）。
