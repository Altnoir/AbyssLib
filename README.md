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

> **分区标题已开箱即用**：AbyssLib 自带客户端标题渲染器
> （`ALSectionedCreativeTabRenderer`，随 `AbyssLibClient` 自动注册），
> 打开创造栏时会在分区前的空行上绘制标题横幅，消费方无需任何客户端代码。
> 配色可用 `ALSectionedCreativeTabRenderer.setPalette(...)` 一行定制
> （构造参数：背景/暗边框/亮边框/文字 ARGB）。

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
    implementation("com.altnoir.abysslib:AbyssLib:1.1.0")
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

- 分区标题渲染已内置并随 `AbyssLibClient` 自动注册（见上文第 2 节）；无需客户端 hook。
- 数据生成注意：多模组并存时 Registrate 的 unassociated BLOCK_TAGS 生成器存在并发竞态（ConcurrentModificationException），世界生成标签建议用自定义 DataProvider 在 addTags 阶段直填（参考 PoopSky-FilthDomain 的 FDTagsProvider 做法）。
