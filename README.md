# AbyssLib

Altnoir 系列模组的公共前置库（NeoForge 1.21.1 / Java 21）。

- 命名空间（mod id）：`abysslib`
- 包名：`com.altnoir.abysslib`
- 类前缀：`AL`（如 `ALRegistrate` / `ALCreativeTabSection`）
- 作者：Altnoir
- 功能：**jarJar 内置 Registrate 与 Simple Bedrock Model（简单基岩模型）**，提供通用 `ALRegistrate` 与分区式创造栏

## 内置库（jarJar，唯一提供者）

| 库 | 版本 | 说明 |
|---|---|---|
| Registrate | `MC1.21-1.3.0+67` | 注册框架 |
| Simple Bedrock Model | `2.5.1` | 简单基岩模型 |

**铁律**：这两者只由 AbyssLib 在运行时提供。依赖本库的模组一律：
- `compileOnly` Registrate / sbm（编译期可见）
- 运行时通过 AbyssLib 的 jarJar 拷贝获得（mods.toml 声明 required 依赖 `abysslib`）
- 绝不各自 jarJar，否则运行时出现多份类、跨模组传 `ItemEntry`/`BlockEntry` 会类型分裂

## 构建 / 发布

```bash
./gradlew build          # 产物在 build/libs/
./gradlew publish        # 发布到 repo/（本地仓库，供其他模组引用）
```

> 注意：maven-publish 的 artifactId 取**项目名** `AbyssLib`，坐标为 `com.altnoir.abysslib:AbyssLib:<版本>`。

## 提供给模组的功能

### 1. ALRegistrate（通用 Registrate 实例）

模组入口持有一个绑定自己 mod id 的实例（注册名全部是自己的命名空间）：

```java
public class MyMod {
    private static final ALRegistrate REGISTRATE = ALRegistrate.create(MyMod.MOD_ID);

    public static ALRegistrate registrate() {
        return REGISTRATE;
    }
}
```

### 2. 分区式创造栏（ALCreativeTabSection + ALSectionedCreativeModeTab）

建自己的分区与标签页：

> 分区标题横幅**开箱即用**：客户端渲染随 `AbyssLibClient` 自动注册，消费方无需任何客户端代码。
> 横幅样式（纯色 / 预设贴图 / 自定义贴图）、"格数"、物品列数联动等详见下方 **2.1 横幅样式**。

```java
public final class MyItemGroups {
    private static final ALRegistrate REGISTRATE = MyMod.registrate();

    public static final ALCreativeTabSection TS_ITEMS = section("itemGroup.mymod.section.items");
    public static final ALCreativeTabSection TS_BLOCKS = section("itemGroup.mymod.section.blocks");

    public static final RegistryEntry<CreativeModeTab, CreativeModeTab> TAB = REGISTRATE.generic("main",
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

注册内容走 `ALRegistrate` 的 `block()` / `item()`（自动模型/语言/战利品），并用 `defaultCreativeSection(...)` 自动归类：

```java
public final class MyItems {
    public static final ItemEntry<Item> SOME_ITEM = REGISTRATE.item("some_item", Item::new)
            .register();

    // 链式 API：排除默认分区 / 额外加入某分区（block()/item() 均支持）
    public static final ItemEntry<Item> NO_TAB_ITEM = REGISTRATE.item("no_tab_item", Item::new)
            .ignore()
            .register();
    public static final ItemEntry<Item> EXTRA_TAB_ITEM = REGISTRATE.item("extra_tab_item", Item::new)
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
    public static final RegistryEntry<CreativeModeTab, CreativeModeTab> TAB_A = REGISTRATE.generic("tab_a",
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
    public static final RegistryEntry<CreativeModeTab, CreativeModeTab> TAB_B = REGISTRATE.generic("tab_b",
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

### 3. 消费方 build.gradle 接入

```gradle
repositories {
    maven { url = file("../AbyssLib/repo") }      // 本地发布仓库（先 ./gradlew publish）
    maven { url = "https://mvn.devos.one/snapshots" } // Registrate
    maven { url = "https://jitpack.io" }              // Simple Bedrock Model（如代码直接用其 API）
}

dependencies {
    // 保留 POM 传递：AbyssLib 的 runtime 依赖（Registrate / SBM）会进入本模组 dev 运行 classpath，
    // 而生产环境这两者只由 AbyssLib 的 jarJar 唯一提供（本 jar 不会内嵌它们）。
    implementation("com.altnoir.abysslib:AbyssLib:1.2.0")
    // 编译期 API（与 AbyssLib 内置版本必须一致，禁止再各自 jarJar）：
    compileOnly "com.tterrag.registrate:Registrate:MC1.21-1.3.0+67"
    compileOnly "com.github.mcmodderanchor:simplebedrockmodel:2.5.1-neoforge-mc1.21.1"
}
```

> 若出于隔离需要坚持 `{ transitive = false }`，请自行把 Registrate / SBM 加到
> dev 运行 classpath（如 `runtimeOnly`），否则 `runClient` 会缺类。

mods.toml 声明：

```toml
[[dependencies.你的modid]]
modId = "abysslib"
type = "required"
versionRange = "[1.0,)"
ordering = "AFTER"
side = "BOTH"
```

## 备注

- 分区标题横幅渲染已内置并随 `AbyssLibClient` 自动注册，无需客户端 hook；样式（纯色/预设/自定义贴图）与格数见 **2.1 横幅样式**。
- 数据生成注意：多模组并存时 Registrate 的 unassociated BLOCK_TAGS 生成器存在并发竞态（ConcurrentModificationException），世界生成标签建议用自定义 DataProvider 在 addTags 阶段直填（参考 PoopSky-FilthDomain 的 FDTagsProvider 做法）。
