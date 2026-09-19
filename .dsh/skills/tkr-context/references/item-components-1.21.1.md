# 1.21.1 物品堆叠与数据组件（不用 NBT）参考

> **版本锁定**：Minecraft **1.21.1** / NeoForge **21.1.232**。
> **来源 artifact**：`build/moddev/artifacts/neoforge-21.1.232-sources.jar` 的
> `net/minecraft/core/component/DataComponents.java`、`DataComponentType.java`、
> `net/minecraft/world/item/Item.java`。以下签名与常量**逐行摘自该 jar**，不是记忆或网络资料。
> 项目原则（用户要求）：**物品上尽量不使用 NBT，一律走数据组件。**

---

## 1. 核心结论速查

| 事实 | 值 | 出处 |
|---|---|---|
| 数据组件总数（1.21.1 官方注册） | **57** | `DataComponents.java` 全部 `register(...)` 调用 |
| 默认堆叠上限常量 | `Item.DEFAULT_MAX_STACK_SIZE = 64` | `Item.java:64` |
| 堆叠硬上限常量 | `Item.ABSOLUTE_MAX_STACK_SIZE = 99` | `Item.java:65` |
| 堆叠组件持久化范围 | `ExtraCodecs.intRange(1, 99)` | `DataComponents.java:59` |
| 组件在物品上的载体 | `DataComponentMap`（不是 `CompoundTag`） | `Item.java:68, 89, 106` |
| 目标注册表 | `BuiltInRegistries.DATA_COMPONENT_TYPE` | `DataComponents.java:247` |

**最关键的一条硬校验**（`Item.java:455-461`）：

```java
public static DataComponentMap validateComponents(DataComponentMap map) {
    if (map.has(DataComponents.DAMAGE) && map.getOrDefault(DataComponents.MAX_STACK_SIZE, 1) > 1) {
        throw new IllegalStateException("Item cannot have both durability and be stackable");
    }
    ...
}
```

> **物品不能既有耐久又可堆叠。** 试图 `stacksTo(16).durability(100)` 会在**注册期直接抛异常**，不是运行期怪现象。这是 1.21 新增的强校验，旧版本教程完全没有。

---

## 2. 堆叠的正确写法

### 2.1 默认值从哪来

`Item.Properties.stacksTo(int)` 只是往组件表里塞 `MAX_STACK_SIZE`（`Item.java:398-400`）：

```java
public Item.Properties stacksTo(int size) {
    return this.component(DataComponents.MAX_STACK_SIZE, size);
}
```

而**普通物品的默认 64 来自公共组件集**（`DataComponents.java:233-240`）：

```java
public static final DataComponentMap COMMON_ITEM_COMPONENTS = DataComponentMap.builder()
    .set(MAX_STACK_SIZE, 64)
    .set(LORE, ItemLore.EMPTY)
    .set(ENCHANTMENTS, ItemEnchantments.EMPTY)
    .set(REPAIR_COST, 0)
    .set(ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY)
    .set(RARITY, Rarity.COMMON)
    .build();
```

**陷阱**：`Item.getMaxStackSize()` 的读法是 `getOrDefault(MAX_STACK_SIZE, 1)`（`Item.java:120`）——**兜底是 1 不是 64**。默认 64 只来自 `COMMON_ITEM_COMPONENTS`。所以：

- 用 `new Item.Properties()` 正常构造 → 拿到公共组件集 → 默认 **64**。
- 若你绕过公共组件集自行构造组件表（如直接 `DataComponentMap.builder()`），不显式设 `MAX_STACK_SIZE` 就得到 **1**，物品会莫名不可堆叠。

> **精度补充（独立审计核出）**：`ItemStack.getMaxStackSize()` 实际走到的是 NeoForge 的
> `IItemExtension.getMaxStackSize(ItemStack)`（`return stack.getOrDefault(DataComponents.MAX_STACK_SIZE, 1);`），
> 而不是 `Item.getMaxStackSize()`。另外 `Item.DEFAULT_MAX_STACK_SIZE = 64` 这个常量在
> `net/minecraft` 源码中**被声明但从未被引用** —— 真正生效的默认 64 来自 `COMMON_ITEM_COMPONENTS`。
> 结论不变（兜底 1、默认来自公共组件集），但引用时用 `IItemExtension` 那条路径更准确。

### 2.2 各种堆叠意图的标准写法

```java
// 普通可堆叠物品（默认 64，无需显式调用）
new Item(new Item.Properties())

// 自定义堆叠上限（1~99，超出范围会在组件校验/编解码阶段失败）
new Item(new Item.Properties().stacksTo(16))

// 不可堆叠
new Item(new Item.Properties().stacksTo(1))

// 有耐久的工具：durability() 会自动把堆叠设为 1 并初始化 damage=0
new Item(new Item.Properties().durability(250))
```

`durability(int)` 的真实实现（`Item.java:402-406`）——注意它**一次性设了三个组件**：

```java
public Item.Properties durability(int maxDamage) {
    this.component(DataComponents.MAX_DAMAGE, maxDamage);
    this.component(DataComponents.MAX_STACK_SIZE, 1);
    this.component(DataComponents.DAMAGE, 0);
    return this;
}
```

> 这就是为什么**不要**在 `durability()` 之后再 `stacksTo(n>1)`：会撞上 §1 的硬校验。

### 2.3 运行时读堆叠

- 物品侧：`Item.getMaxStackSize()`，或 `stack.getMaxStackSize()`。
- 组件侧：`stack.get(DataComponents.MAX_STACK_SIZE)`，可能为 `null`。
- `Item.java:308` 展示了「不可堆叠且有耐久」的判定惯用法：
  ```java
  stack.getMaxStackSize() == 1 && stack.has(DataComponents.MAX_DAMAGE)
  ```

---

## 3. 「不用 NBT」的完整替代表

1.21.1 里原先靠 NBT 存的东西**全部**有对应组件。以下为常用对照（键名逐字摘自 `DataComponents.java`）：

| 旧 NBT 写法（1.20 及以前） | 1.21.1 组件 | 组件类型 |
|---|---|---|
| `display.Name` | `CUSTOM_NAME` (`custom_name`) | `Component` |
| 物品基础名 | `ITEM_NAME` (`item_name`) | `Component` |
| `display.Lore` | `LORE` (`lore`) | `ItemLore` |
| `Damage` | `DAMAGE` (`damage`) | `Integer`（`NON_NEGATIVE_INT`） |
| 最大耐久 | `MAX_DAMAGE` (`max_damage`) | `Integer`（`POSITIVE_INT`） |
| `Unbreakable` | `UNBREAKABLE` (`unbreakable`) | `Unbreakable` |
| `Enchantments` | `ENCHANTMENTS` (`enchantments`) | `ItemEnchantments` |
| 附魔书附魔 | `STORED_ENCHANTMENTS` (`stored_enchantments`) | `ItemEnchantments` |
| `AttributeModifiers` | `ATTRIBUTE_MODIFIERS` (`attribute_modifiers`) | `ItemAttributeModifiers` |
| `CustomModelData` | `CUSTOM_MODEL_DATA` (`custom_model_data`) | `CustomModelData` |
| `HideFlags` | `HIDE_ADDITIONAL_TOOLTIP` (`hide_additional_tooltip`)、`HIDE_TOOLTIP` (`hide_tooltip`) | `Unit` |
| `RepairCost` | `REPAIR_COST` (`repair_cost`) | `Integer` |
| 稀有度 | `RARITY` (`rarity`) | `Rarity` |
| `display.color` / 皮革染色 | `DYED_COLOR` (`dyed_color`) | `DyedItemColor` |
| 食物属性 | `FOOD` (`food`) | `FoodProperties` |
| 火焰抗性 | `FIRE_RESISTANT` (`fire_resistant`) | `Unit` |
| 工具挖矿规则 | `TOOL` (`tool`) | `Tool` |
| `BlockEntityTag` | `BLOCK_ENTITY_DATA` (`block_entity_data`) | `CustomData` |
| `EntityTag` | `ENTITY_DATA` (`entity_data`) | `CustomData` |
| `ChargedProjectiles` | `CHARGED_PROJECTILES` (`charged_projectiles`) | 物品栈列表 |
| 药水内容 | `POTION_CONTENTS` (`potion_contents`) | `PotionContents` |
| 容器内容（潜影盒/收纳袋） | `CONTAINER` (`container`)、`BUNDLE_CONTENTS` (`bundle_contents`) | 物品栈列表 |

**结论**：凡「我想往物品上存点东西」的需求，**先在这 57 个组件里找现成的**。找不到再考虑下一步。

### 3.1 `CUSTOM_DATA` 是官方逃生舱，但有纪律

```java
public static final DataComponentType<CustomData> CUSTOM_DATA =
    register("custom_data", b -> b.persistent(CustomData.CODEC));
```

`custom_data` 是**唯一**允许塞任意 `CompoundTag` 的组件。使用纪律：

- **优先用专用组件**。有 `max_stack_size` 就别自己造一个 `"StackLimit"` 塞进 `custom_data`——那样数据包、配方、其他 mod 都读不到你的值。
- 仅在「自有的、纯内部的状态」且没有对应组件时才用 `custom_data`，例如机器进度、自定义状态机标记。
- 即使使用 `custom_data`，**读写也必须走组件 API**（`stack.get(DataComponents.CUSTOM_DATA)` / `stack.set(...)`），不要退回 `stack.getTag()` 这类旧接口。NeoForge 的兼容垫片即使存在，也是给旧 mod 过渡用的。

### 3.2 自定义组件的注册契约（`DataComponentType.java` 摘录）

```java
public interface DataComponentType<T> {
    Codec<T> codec();                                              // 可空；为 null 即 transient
    default Codec<T> codecOrThrow();                               // transient 时抛 IllegalStateException
    default boolean isTransient();                                 // codec() == null
    StreamCodec<? super RegistryFriendlyByteBuf, T> streamCodec();

    static <T> DataComponentType.Builder<T> builder();
    // Builder: .persistent(Codec<T>) / .networkSynchronized(StreamCodec) / .cacheEncoding()
}
```

要点：

- **`persistent(Codec)` 决定该组件能否保存到磁盘**。不调用它 → `codec() == null` → `isTransient() == true`
  → 该组件**不会写入物品的持久化数据**。机制在 `DataComponentPatch`：
  - `CODEC`（落盘路径，第 46 行）有 `if (!datacomponenttype.isTransient())` 过滤 → transient 组件被跳过；
  - `STREAM_CODEC`（网络路径，第 58 行起）走 `type.streamCodec()` 且**没有** `isTransient()` 过滤 → transient 组件**仍然可以上线**。
  所以 transient 表达的是「**不落盘**」，不是「不同步」。
- **同步是默认就有的，不是要额外开启的能力。** `Builder.build()`（第 73-75 行）对**每个**组件都会产出 stream codec：
  未调 `networkSynchronized` 时回退
  `ByteBufCodecs.fromCodecWithRegistries(requireNonNull(this.codec, "Missing Codec for component"))`。
  因此 `.networkSynchronized(...)` 的真实作用是**性能与格式优化**（避免经由 Codec 的通用编解码路径），
  **而不是**「不写就不能同步」。反过来：**既不调 `persistent` 也不调 `networkSynchronized` 会直接 NPE**
  （`requireNonNull(this.codec, "Missing Codec for component")`）。
- 注册进 `BuiltInRegistries.DATA_COMPONENT_TYPE`；NeoForge 下用 `DeferredRegister`（见 §4）。

---

## 4. 本项目落地骨架（NeoForge 21.1.232 + DeferredRegister）

放在哪一层：**组件声明归 `data/component/`，编解码归 `data/codec/`，物品类型归 `data/item/`。**
`logic/` 只消费组件值，`client/` 只渲染。详见 `.dsh/skills/tkr-context/SKILL.md` §3。

### 4.1 注册数据组件的正确 API（已从 NeoForge 源码核验）

**不要**用 `DeferredRegister.create(BuiltInRegistries.DATA_COMPONENT_TYPE, ...)` 那类通用写法。NeoForge 为此提供了专用工厂与注册方法：

```java
// DeferredRegister.java:169  ← 正确：带 registryKey
public static DataComponents createDataComponents(ResourceKey<Registry<DataComponentType<?>>> registryKey, String modid)

// DeferredRegister.java:184  ← 弃用，1.21.2 将移除
@Deprecated(since = "1.21.1", forRemoval = true)
public static DataComponents createDataComponents(String modid)
```

并且在 `DeferredRegister.DataComponents`（内部类，`DeferredRegister.java:649-670`）上有专门的注册方法，**它就是为规避泛型推断问题而设计的**：

```java
public <D> DeferredHolder<DataComponentType<?>, DataComponentType<D>> registerComponentType(
        String name, UnaryOperator<DataComponentType.Builder<D>> builder)
```

### 4.2 骨架代码

```java
// data/component/TkrComponents.java
public final class TkrComponents {
    public static final DeferredRegister.DataComponents COMPONENTS =
        DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, TkrMod.MOD_ID);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> CHARGE =
        COMPONENTS.registerComponentType("charge", builder -> builder
            .persistent(ExtraCodecs.intRange(0, 100))       // 可落盘（不给则 transient）
            .networkSynchronized(ByteBufCodecs.VAR_INT));   // 需同步到客户端

    private TkrComponents() {}
}
```

物品侧一律用组件，不碰 NBT：

```java
// data/item/TkrItems.java
public static final DeferredItem<Item> CHARM = ITEMS.register("charm",
    () -> new Item(new Item.Properties()
        .stacksTo(16)                                          // 可堆叠
        .component(TkrComponents.CHARGE.get(), 0)));           // 自定义状态

// 读取：组件可能不存在，必须给默认值
int charge = stack.getOrDefault(TkrComponents.CHARGE.get(), 0);
```

### 4.3 尚未核验的部分

- `ExtraCodecs.intRange` 属 Mojang 的 DFU 库（`com.mojang.serialization`），**不在 NeoForge sources jar 内**，故我无法在此引用其源码。但 `DataComponents.java:59` 自身就在用 `ExtraCodecs.intRange(1, 99)`，说明该类在 1.21.1 可用。若编译报找不到，改用 `Codec.intRange(...)` 或自行 `Codec.INT.validate(...)` 兜底。
- `DeferredItem` / `DeferredRegister.Items` 的用法沿用项目惯例，尚未编译验证。
- **本项目目前没有任何 Java 实现代码**，以上骨架首次落地时**必须实际 `.\gradlew.bat build --offline` 编译一次确认**，不要当作已验证代码直接依赖。

---

## 5. 陷阱清单

1. **既有耐久又可堆叠 → 注册期抛 `IllegalStateException`**（§1）。`durability()` 已自动置 1，别再改大。
2. **`getMaxStackSize()` 兜底是 1**（`Item.java:120`），默认 64 来自 `COMMON_ITEM_COMPONENTS`。自行构造组件表时忘设 `MAX_STACK_SIZE` → 物品不可堆叠。
3. **堆叠上限取值范围 `1..99`**（`ExtraCodecs.intRange(1, 99)`）。写 0 或 100+ 不合法；`ABSOLUTE_MAX_STACK_SIZE = 99` 是硬上限。
4. **组件不调用 `persistent(Codec)` 就不会落盘**，且会被 `PERSISTENT_CODEC` 报错。想要「仅运行时」状态才这么做。
5. **`build()` 时既无 codec 又无 stream codec → NPE**（`"Missing Codec for component"`）。
6. **不要在物品上新增自定义 NBT**。先查 §3 的 57 个组件；确实没有再用 `CUSTOM_DATA`，且必须走组件 API。
7. **旧教程不可信**。1.20 及以前的 `CompoundTag` / `ItemStack.getOrCreateTag()` 写法在 1.21.1 已不是正道，照抄会引入与数据包、配方的兼容问题。
8. **`custom_name` 与 `item_name` 不是一回事**：`custom_name` 是铁砧改名（玩家可见的「自定义名称」），`item_name` 是物品的基础名（覆盖语言文件里的名字）。混用会导致名称显示与预期不符。

---

## 6. 与本项目其它参考的关系

- 属性组件 `ATTRIBUTE_MODIFIERS` 里的属性 id **必须带前缀**（`generic.max_health` 等），见 `mc-1.21.1-attributes.tsv` 与 `PITFALLS.md` P-009。
- Curios 饰品物品同样走数据组件；其物品接口（`ICurio` 或 `ICurioItem`）在 9.5.1 下的确切形态见 `curios-9.5.1-api.md`（提取中）。
