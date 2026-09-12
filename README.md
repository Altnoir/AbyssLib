# AbyssLib (26.1.2)

Altnoir 系列模组的公共前置库（NeoForge **26.1.2.94** / Java 25 / moddev 2.0.143）。

- 包名：`com.altnoir.abysslib`
- 作者：Altnoir
- 分支：`26.1.2-NeoForge`（独立 git worktree：`D:\Minecraft\ModDev\AbyssLib-26.1.2`）
- 版本：`1.0.0`

> 与 1.21.1 线（`1.21.1-NeoForge`）的差异：**API 不通用**。
> - 26.1 使用 `Identifier`（无 `ResourceLocation`）、`GuiGraphicsExtractor`/`RenderPipelines` 渲染管线；
> - 分区栏为 MIA-26.1 模型（分区持 `ResourceKey<CreativeModeTab>` + `Identifier` + 可选 bannerSprite），不是 1.21.1 的 populator + ALBannerStyle 模型；
> - 访问创造栏私有成员用 **mixin `@Shadow`**（不用 AT）；
> - 26.1 线目前**只有注册框架模块**，1.21.1 线的 `ReLink`（模型加载器）与 `Atlas`（结构扩展）**尚未移植**。

## 模块化结构

与 1.21.1 线同形制：一个 Gradle 多项目，拆成「功能模块 + 聚合包」。

| 模块 | 坐标（group `com.altnoir.abysslib`） | modid | 内容 |
|---|---|---|---|
| **AbyssLib**（聚合） | `AbyssLib` | `abysslib` | 内嵌 `AbyssLib-Reginth` 的 jar（jarJar），**装这一个就等于装全部**；另含 `AbyssLib` 门面工具 |
| **AbyssLib-Reginth** | `AbyssLib-Reginth` | `abysslib_reginth` | 注册框架 **`Reginth`**（源码级内嵌自上游 Registrate `MC26.1-1.5.7`，包名与类名已改）+ 分区式创造栏 |

- 目录：`module.reginth/`、`module.main/`（详见仓库里的 `settings.gradle`）。
- **不再依赖 `maven.gegy.dev` 的 Registrate**，也不再 `jarJar` / `api` 暴露它 —— 类就在本库 jar 里，天然单副本、无版本冲突。
- **Simple Bedrock Model / mae 不由本库内置**；需要 SBM 的模组自行以本地 libs + jarJar 提供。

## 构建 / 发布

```bash
./gradlew build          # 两个 jar 分别在 module.*/build/libs/
./gradlew publish        # 两个坐标一起发布到 repo/（本地仓库）
./gradlew :AbyssLib:runClient    # run 配置只定义在聚合模块
./gradlew :AbyssLib:runServer
```

> artifactId 由 Gradle 项目名决定（`settings.gradle` 里改名），坐标为
> `com.altnoir.abysslib:AbyssLib:<版本>` 与 `com.altnoir.abysslib:AbyssLib-Reginth:<版本>`。

> **跑 run 需要 Java 21 工具链**：ModDevGradle 的 `downloadAssets` 任务要求 Java 21，而工程本身target Java 25。
> 本机已在 `GRADLE_USER_HOME/gradle.properties` 里用 `org.gradle.java.installations.paths` 指向 JDK-21；
> 换机器时若报 "Cannot find a Java installation … languageVersion=21"，照此补一行即可。

## 通用工具（AbyssLib 静态方法）

`AbyssLib` 聚合模块自带一组 `Identifier`/注册表路径工具：

| 方法 | 说明 |
|---|---|
| `AbyssLib.loc(path)` | `abysslib:<path>`（仅本库资源；消费方用 `modloc(自身MOD_ID, path)`） |
| `AbyssLib.modloc(namespace, path)` | 任意 `namespace:path` |
| `AbyssLib.mcloc(path)` | 原版 `minecraft:path` |
| `AbyssLib.parse(str)` / `tryParse(str)` | 严格 / 宽松解析 |
| `getItemPath(item)` / `getBlockPath(block)` / `getBlockKey(block)` | 物品/方块的注册名 |

## Reginth（通用注册框架实例）

**改名说明**：26.1 线原来自有的 `ALRegistrate` / `ALBlockBuilder` / `ALItemBuilder` 已删除，
统一为源码内嵌的注册框架 `Reginth` / `ReginthBlockBuilder` / `ReginthItemBuilder`
（包名 `com.altnoir.abysslib.reginth`，与 1.21.1 线一致）。

入口类持有绑定自己 mod id 的实例，并**自行挂事件总线**：

```java
@Mod(MyMod.MOD_ID)
public class MyMod {
    public static final String MOD_ID = "mymod";
    private static final Reginth REGINTH = Reginth.create(MOD_ID);

    public MyMod(IEventBus modEventBus) {
        REGINTH.registerEventListeners(modEventBus);   // 手动挂载（重要）
        MyItems.register();
        MyBlocks.register();
        MyItemGroups.register();
    }

    public static Reginth reginth() {
        return REGINTH;
    }
}
```

> **与 1.21.1 线的差异**：26.1 线的 `create()` **不会**自动挂事件总线（上游与 1.21.1 线会在 `create` 里
> 查 `ModList` 自动挂载）。所以上面那行 `registerEventListeners` 是**必须**的；也不要两处都挂（会重复注册）。

行为要点：
- 先 `REGINTH.defaultCreativeSection(section)` 设定默认分区，之后 `REGINTH.item(...)` 注册的物品会自动 `.tab(section.tab())` 并把注册名 `add` 进该分区；`block(...)` 本身不自动归类（其方块物品走 item 链触发同一逻辑）。
- `ReginthItemBuilder.ignore()` / `ReginthBlockBuilder.ignore()` 把条目从默认创造栏剔除。
- `object("name").creativeTab(tab -> ALSectionedCreativeModeTab.configure(...)).register()` 或 `creativeTab(...)` 便捷方法注册自定义标签页。

## 分区式创造栏（ALCreativeTabSection + ALSectionedCreativeModeTab）

分区以**注册名**收集（照 MIA-26.1），展示时经注册表惰性解析：

```java
public final class MyItemGroups {
    private static final Reginth REGINTH = MyMod.reginth();

    public static final ResourceKey<CreativeModeTab> TAB_KEY =
            ResourceKey.create(Registries.CREATIVE_MODE_TAB, AbyssLib.modloc(MyMod.MOD_ID, "main"));

    // (tab, id, title[, bannerSprite]) —— bannerSprite 指向
    // assets/<ns>/textures/gui/sprites/<path>.png（162x18）；省略则画默认绿色横幅
    public static final ALCreativeTabSection TS_ITEMS = new ALCreativeTabSection(
            TAB_KEY, AbyssLib.modloc(MyMod.MOD_ID, "main/items"),
            Component.translatable("itemGroup.mymod.section.items"));
    public static final ALCreativeTabSection TS_BLOCKS = new ALCreativeTabSection(
            TAB_KEY, AbyssLib.modloc(MyMod.MOD_ID, "main/blocks"),
            Component.translatable("itemGroup.mymod.section.blocks"));

    public static final RegistryEntry<CreativeModeTab, CreativeModeTab> TAB = REGINTH
            .object("main")
            .creativeTab(tab -> ALSectionedCreativeModeTab.configure(
                    tab.icon(MyItems.SOME_ITEM::asStack),
                    TS_ITEMS, TS_BLOCKS))
            .register();

    public static void register() {
    }
}
```

注册归类（在 init 类 static 初始化最前调用一次，再注册物品）：

```java
public final class MyItems {
    static {
        MyMod.reginth().defaultCreativeSection(MyItemGroups.TS_ITEMS);
    }

    public static final ItemEntry<Item> SOME_ITEM = MyMod.reginth().item("some_item", Item::new).register();
}
```

客户端标题渲染**开箱即用、无需任何客户端代码**：`AbyssLib-Reginth` 模块自带的两个 client mixin
（`CreativeModeInventoryScreen.extractBackground` TAIL 注入 + `CustomCreativeSlot.isHighlightable` 屏蔽标题槽）
会自动处理任何 `ALSectionedCreativeModeTab`。

## 消费方 build.gradle 接入

```gradle
repositories {
    maven { url = file("../AbyssLib-26.1.2/repo") }   // 本地发布仓库（先 ./gradlew publish）
    // 不再需要 maven.gegy.dev：Registrate 已源码内嵌，本库 POM 里没有外部 Registrate 依赖
}

dependencies {
    implementation("com.altnoir.abysslib:AbyssLib:1.0.0")          // 全套
    // 或只依赖注册框架模块：implementation("com.altnoir.abysslib:AbyssLib-Reginth:1.0.0")
    // 需要 SBM 的模组自行声明（本地 libs + jarJar / compileOnly），本库不提供。
}
```

mods.toml 声明：

```toml
[[dependencies.你的modid]]
modId = "abysslib"          # 只装单模块时用 abysslib_reginth
type = "required"
versionRange = "[1.0,)"
ordering = "AFTER"
side = "BOTH"
```

## 备注

- 渲染/屏蔽靠 `abysslib_reginth.mixins.json`（`compatibilityLevel JAVA_25`，两个 mixin 均在 `client` 段）。
  模块里还留着 `META-INF/accesstransformer.cfg`（`CreativeModeInventoryScreen.selectedTab/scrollOffs`）——
  实际用的是 mixin `@Shadow`，该 AT **可能是历史遗留**（未清理，保留以零风险；确认无用后可删）。
- 分区每占用一整个标题行（displayItems 内为真实 EMPTY 占位）；滚动行号公式与 26.1 `ItemPickerMenu` 一致。
- 数据生成注意：多模组并存时 Registrate 的 unassociated BLOCK_TAGS 生成器存在并发竞态，世界生成标签建议用自定义 DataProvider 在 addTags 阶段直填。
- 本分支**没有 `LICENSE` 文件**（1.21.1 分支有）。若要给 26.1 产物附许可声明，需要先把 `LICENSE`
  加到本分支，并在根 `build.gradle` 的 `processResources` 里复制进 `META-INF/`。
