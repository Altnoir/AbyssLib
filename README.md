# AbyssLib (26.1.2)

Altnoir 系列模组的公共前置库（NeoForge **26.1.2.94** / Java 25 / moddev 2.0.143）。

- 命名空间（mod id）：`abysslib`
- 包名：`com.altnoir.abysslib`
- 类前缀：`AL`（如 `ALRegistrate` / `ALCreativeTabSection`）
- 作者：Altnoir
- 分支：`26.1.2-NeoForge`（独立 git worktree：`D:\Minecraft\ModDev\AbyssLib-26.1.2`）
- 功能：**jarJar 内置 Registrate（`MC26.1-1.5.7`）**，提供 `ALRegistrate` 与**照 MIA-26.1 移植的分区式创造栏**（按注册名分区 + 每分区可选 sprite 横幅 + mixin 渲染）

> 与 1.21.1 线（`1.21.1-NeoForge`）的差异：API 不通用。
> - 26.1 使用 `Identifier`（无 `ResourceLocation`）、`GuiGraphicsExtractor`/`RenderPipelines` 渲染管线；
> - 分区栏为 MIA-26.1 模型（分区持 `ResourceKey<CreativeModeTab>` + `Identifier` + 可选 bannerSprite），不是 1.21.1 的 populator + ALBannerStyle 模型；
> - 访问创造栏私有成员用 **mixin @Shadow**（不用 AT）。

## 内置库（jarJar）

| 库 | 版本 | 来源 | 说明 |
|---|---|---|---|
| Registrate | `MC26.1-1.5.7` | `https://maven.gegy.dev/releases` | 注册框架 |

- Registrate 以 `implementation jarJar(...)` 内嵌（运行时唯一副本）+ `api` 暴露编译期（消费方无需自行声明）。
- **Simple Bedrock Model / mae 不由本库内置**（26.1 无公开 SBM maven，本地文件依赖也无法进发布元数据）；需要 SBM 的模组自行以本地 libs/jarJar 提供。

## 构建 / 发布

```bash
./gradlew build          # 产物在 build/libs/
./gradlew publish        # 发布到 repo/（本地仓库，供 26.1 线消费方引用）
```

> maven-publish 的 artifactId 取项目名 `AbyssLib`，坐标为 `com.altnoir.abysslib:AbyssLib:<版本>`。

## 通用工具（AbyssLib 静态方法）

`AbyssLib` 入口类自带一组 `Identifier`/注册表路径工具（26.1 返回 `net.minecraft.resources.Identifier`）：

| 方法 | 说明 |
|---|---|
| `AbyssLib.loc(path)` | `abysslib:<path>`（仅本库资源；消费方用 `modloc(自身MOD_ID, path)`） |
| `AbyssLib.modloc(namespace, path)` | 任意 `namespace:path` |
| `AbyssLib.mcloc(path)` | 原版 `minecraft:path` |
| `AbyssLib.parse(str)` / `tryParse(str)` | 严格 / 宽松解析 |
| `getItemPath(item)` / `getBlockPath(block)` / `getBlockKey(block)` | 物品/方块的注册名 |

## ALRegistrate（通用 Registrate 实例，MIA-26.1 归类模型）

入口类持有绑定自己 mod id 的实例，并**自行挂事件总线**（26.1 版 `create()` 不再自动挂载）：

```java
@Mod(MyMod.MOD_ID)
public class MyMod {
    public static final String MOD_ID = "mymod";
    private static final ALRegistrate REGISTRATE = ALRegistrate.create(MOD_ID);

    public MyMod(IEventBus modEventBus) {
        REGISTRATE.registerEventListeners(modEventBus);   // 手动挂载（重要）
        MyItems.register();
        MyBlocks.register();
        MyItemGroups.register();
    }

    public static ALRegistrate registrate() {
        return REGISTRATE;
    }
}
```

行为要点（照 MiaRegistrate）：
- 先 `REGISTRATE.defaultCreativeSection(section)` 设定默认分区，之后 `REGISTRATE.item(...)` 注册的物品会自动 `.tab(section.tab())` 并把注册名 `add` 进该分区；`block(...)` 本身不自动归类（其方块物品走 item 链触发同一逻辑）。
- `ALItemBuilder.ignore()` / `ALBlockBuilder.ignore()` 把条目从默认创造栏剔除。
- `object("name").creativeTab(tab -> ALSectionedCreativeModeTab.configure(...)).register()` 或 `creativeTab(...)` 便捷方法注册自定义标签页。

## 分区式创造栏（ALCreativeTabSection + ALSectionedCreativeModeTab）

分区以**注册名**收集（照 MIA-26.1），展示时经注册表惰性解析：

```java
public final class MyItemGroups {
    private static final ALRegistrate REGISTRATE = MyMod.registrate();

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

    public static final RegistryEntry<CreativeModeTab, CreativeModeTab> TAB = REGISTRATE
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
        MyMod.registrate().defaultCreativeSection(MyItemGroups.TS_ITEMS);
    }

    public static final ItemEntry<Item> SOME_ITEM = MyMod.registrate().item("some_item", Item::new).register();
}
```

客户端标题渲染**开箱即用、无需任何客户端代码**：`abysslib` 自带的两个 client mixin
（`CreativeModeInventoryScreen.extractBackground` TAIL 注入 + `CustomCreativeSlot.isHighlightable` 屏蔽标题槽）
会自动处理任何 `ALSectionedCreativeModeTab`。

## 消费方 build.gradle 接入

```gradle
repositories {
    maven { url = file("../AbyssLib-26.1.2/repo") }   // 本地发布仓库（先 ./gradlew publish）
    maven { url = "https://maven.gegy.dev/releases" } // Registrate（api 传递解析用）
}

dependencies {
    // Registrate 编译期由 AbyssLib api 传递；运行时由它 jarJar 唯一提供。
    implementation("com.altnoir.abysslib:AbyssLib:1.0.0")
    // 需要 SBM 的模组自行声明（本地 libs + jarJar / compileOnly），本库不再提供。
}
```

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

- 渲染/屏蔽靠 `abysslib.mixins.json`（`compatibilityLevel JAVA_25`，两个 mixin 均在 `client` 段），无需 AT。
- 分区每占用一整个标题行（displayItems 内为真实 EMPTY 占位）；滚动行号公式与 26.1 `ItemPickerMenu` 一致。
- 数据生成注意：多模组并存时 Registrate 的 unassociated BLOCK_TAGS 生成器存在并发竞态，世界生成标签建议用自定义 DataProvider 在 addTags 阶段直填。
