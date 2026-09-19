# Curios API 9.5.1 真实接口参考（MC 1.21.1 / NeoForge 21.1.232）

> **⚠️ 历史资料（2026-09-19 起）**：本项目的 **Curios 依赖已移除**，`libs/curios-neoforge-9.5.1+1.21.1.jar`
> 也已删除。本文件保留是因为：(1) 其中的签名是从真实 jar 用 `javap` 逐类提取的，
> 是「如何用 artifact 核验版本敏感 API」的范例；(2) 若日后重新引入 Curios，可直接复用。
> **当前项目不需要 Curios，也不应据此写新代码。**

> 提取日期：2026-09-26。方式：对本项目 `libs/curios-neoforge-9.5.1+1.21.1.jar` 逐类跑 `javap` / `javap -v` / `javap -c`。**未使用 Web 搜索。**
> 权威 artifact：size **410690**，SHA256 **A45DF2125C26219974ABA7507FFC9AFE7B83ACC941A386AF3FAACB1CC0056FDE**，文件时间 2026-09-16 23:08:40。
> 复现：`javap -classpath "libs/curios-neoforge-9.5.1+1.21.1.jar" <FQCN>`（`javap` = `C:\Java\jdk-21.0.12.1+1\bin\javap.exe`）。
> 下文代码块中**每个签名都逐字复制自 javap**；为控制篇幅，部分短签名同行排列，分隔符 ` ; ` 不是源码。
> 过期触发条件：`libs/` 内 jar 变更、`neo_version` 变更、Curios 升级。届时先标记「已过期」再重新提取。

---

## 1. 版本锁定事实

**本项目编译期链接的就是这个 jar；下述签名均以此为准。**

jar 内 `META-INF/neoforge.mods.toml`（逐字摘录关键行）：

```toml
modLoader="javafml"
loaderVersion="[1,)"
[[mods]]
    modId="curios"
    version="9.5.1+1.21.1"
    displayName="Curios API"
[[dependencies.curios]]
    modId="neoforge"   type="required" versionRange="[21.1.60,)"    side="BOTH"
[[dependencies.curios]]
    modId="minecraft"  type="required" versionRange="[1.21, 1.22)"  side="BOTH"
```

| 项 | 值 | 来源 |
|---|---|---|
| jar 文件名 | `curios-neoforge-9.5.1+1.21.1.jar` | `build.gradle:56` `implementation files('libs/curios-neoforge-9.5.1+1.21.1.jar')` |
| Curios 版本 | **`9.5.1+1.21.1`** | jar 内 `neoforge.mods.toml` 的 `version=` |
| modId / `CuriosApi.MODID` | `curios` / `public static final java.lang.String MODID = "curios";` | toml + `javap -constants` 常量池 `ConstantValue` |
| MC 目标 | `1.21.1`（Curios 自声明 `[1.21, 1.22)`） | jar toml |
| NeoForge 目标 | `21.1.232`（Curios 自声明 `[21.1.60,)`） | `gradle.properties: neo_version` + jar toml |
| 运行时 FML | `4.0.42` | `run/logs/latest.log` 启动参数 `--fml.fmlVersion 4.0.42` |

**结构事实（影响怎么写代码）**：`CuriosApi` 的所有方法在本 jar 中都是**桩**——`javap -c` 显示方法体只有 `invokestatic apiError()` + 默认返回值，文案 `"Missing Curios API implementation!"`。真实实现由 `top.theillusivec4.curios.mixin.core.MixinCuriosApi` 运行时注入（`CallbackInfo.cancel()` / `setReturnValue`）。**结论**：签名可用于编译；不要读 `CuriosApi` 字节码推断行为，也不要在 Curios 缺席时调用它。

---

## 2. 关键入口类型与真实签名

### 2.1 `top.theillusivec4.curios.api.CuriosApi`

`public final class`，**类级未弃用**，有 `public` 构造函数。`javap` 逐字全量：

```java
public static final java.lang.String MODID;
public top.theillusivec4.curios.api.CuriosApi();
public static void registerCurio(net.minecraft.world.item.Item, top.theillusivec4.curios.api.type.capability.ICurioItem);
public static java.util.Optional<top.theillusivec4.curios.api.type.ISlotType> getSlot(java.lang.String, net.minecraft.world.level.Level); public static java.util.Optional<top.theillusivec4.curios.api.type.ISlotType> getSlot(java.lang.String, boolean);
public static java.util.Map<java.lang.String, top.theillusivec4.curios.api.type.ISlotType> getSlots(net.minecraft.world.level.Level); public static java.util.Map<java.lang.String, top.theillusivec4.curios.api.type.ISlotType> getSlots(boolean);
public static java.util.Map<java.lang.String, top.theillusivec4.curios.api.type.ISlotType> getPlayerSlots(net.minecraft.world.level.Level); public static java.util.Map<java.lang.String, top.theillusivec4.curios.api.type.ISlotType> getPlayerSlots(boolean); public static java.util.Map<java.lang.String, top.theillusivec4.curios.api.type.ISlotType> getPlayerSlots(net.minecraft.world.entity.player.Player);
public static java.util.Map<java.lang.String, top.theillusivec4.curios.api.type.ISlotType> getEntitySlots(net.minecraft.world.entity.LivingEntity); public static java.util.Map<java.lang.String, top.theillusivec4.curios.api.type.ISlotType> getEntitySlots(net.minecraft.world.entity.EntityType<?>, net.minecraft.world.level.Level); public static java.util.Map<java.lang.String, top.theillusivec4.curios.api.type.ISlotType> getEntitySlots(net.minecraft.world.entity.EntityType<?>, boolean);
public static java.util.Map<java.lang.String, top.theillusivec4.curios.api.type.ISlotType> getItemStackSlots(net.minecraft.world.item.ItemStack, net.minecraft.world.level.Level); public static java.util.Map<java.lang.String, top.theillusivec4.curios.api.type.ISlotType> getItemStackSlots(net.minecraft.world.item.ItemStack, boolean); public static java.util.Map<java.lang.String, top.theillusivec4.curios.api.type.ISlotType> getItemStackSlots(net.minecraft.world.item.ItemStack, net.minecraft.world.entity.LivingEntity);
public static java.util.Optional<top.theillusivec4.curios.api.type.capability.ICurio> getCurio(net.minecraft.world.item.ItemStack);
public static java.util.Optional<top.theillusivec4.curios.api.type.capability.ICuriosItemHandler> getCuriosInventory(net.minecraft.world.entity.LivingEntity);
public static boolean isStackValid(top.theillusivec4.curios.api.SlotContext, net.minecraft.world.item.ItemStack);
public static com.google.common.collect.Multimap<net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute>, net.minecraft.world.entity.ai.attributes.AttributeModifier> getAttributeModifiers(top.theillusivec4.curios.api.SlotContext, net.minecraft.resources.ResourceLocation, net.minecraft.world.item.ItemStack);
public static void addSlotModifier(com.google.common.collect.Multimap<net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute>, net.minecraft.world.entity.ai.attributes.AttributeModifier>, java.lang.String, net.minecraft.resources.ResourceLocation, double, net.minecraft.world.entity.ai.attributes.AttributeModifier$Operation);
public static void addSlotModifier(net.minecraft.world.item.ItemStack, java.lang.String, net.minecraft.resources.ResourceLocation, double, net.minecraft.world.entity.ai.attributes.AttributeModifier$Operation, java.lang.String);
public static net.minecraft.world.item.component.ItemAttributeModifiers withSlotModifier(net.minecraft.world.item.component.ItemAttributeModifiers, java.lang.String, net.minecraft.resources.ResourceLocation, double, net.minecraft.world.entity.ai.attributes.AttributeModifier$Operation, net.minecraft.world.entity.EquipmentSlotGroup);
public static void addModifier(net.minecraft.world.item.ItemStack, net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute>, net.minecraft.resources.ResourceLocation, double, net.minecraft.world.entity.ai.attributes.AttributeModifier$Operation, java.lang.String);
public static void registerCurioPredicate(net.minecraft.resources.ResourceLocation, java.util.function.Predicate<top.theillusivec4.curios.api.SlotResult>); public static java.util.Optional<java.util.function.Predicate<top.theillusivec4.curios.api.SlotResult>> getCurioPredicate(net.minecraft.resources.ResourceLocation);
public static java.util.Map<net.minecraft.resources.ResourceLocation, java.util.function.Predicate<top.theillusivec4.curios.api.SlotResult>> getCurioPredicates(); public static boolean testCurioPredicates(java.util.Set<net.minecraft.resources.ResourceLocation>, top.theillusivec4.curios.api.SlotResult);
public static net.minecraft.resources.ResourceLocation getSlotId(top.theillusivec4.curios.api.SlotContext);
public static void broadcastCurioBreakEvent(top.theillusivec4.curios.api.SlotContext);
```

**以下 12 个方法被 `@Deprecated` 标注**（`javap -v` 的 `RuntimeVisibleAnnotations: java.lang.Deprecated`）：

```java
@Deprecated public static java.util.Optional<top.theillusivec4.curios.api.type.ISlotType> getSlot(java.lang.String); @Deprecated public static net.minecraft.resources.ResourceLocation getSlotIcon(java.lang.String); @Deprecated public static java.util.Map<java.lang.String, top.theillusivec4.curios.api.type.ISlotType> getSlots();
@Deprecated public static java.util.Map<java.lang.String, top.theillusivec4.curios.api.type.ISlotType> getEntitySlots(net.minecraft.world.entity.EntityType<?>); @Deprecated public static java.util.Map<java.lang.String, top.theillusivec4.curios.api.type.ISlotType> getPlayerSlots(); @Deprecated public static java.util.Map<java.lang.String, top.theillusivec4.curios.api.type.ISlotType> getItemStackSlots(net.minecraft.world.item.ItemStack);
@Deprecated public static void setIconHelper(top.theillusivec4.curios.api.type.util.IIconHelper); @Deprecated public static top.theillusivec4.curios.api.type.util.IIconHelper getIconHelper(); @Deprecated public static void setCuriosHelper(top.theillusivec4.curios.api.type.util.ICuriosHelper); @Deprecated public static top.theillusivec4.curios.api.type.util.ICuriosHelper getCuriosHelper(); @Deprecated public static top.theillusivec4.curios.api.type.util.ISlotHelper getSlotHelper(); @Deprecated public static void setSlotHelper(top.theillusivec4.curios.api.type.util.ISlotHelper);
```

| 名字 | 9.5.1 状态 |
|---|---|
| `getCuriosInventory(LivingEntity)` → `Optional<ICuriosItemHandler>` | ✅ 存在且未弃用，查询已装备饰品的正路 |
| `getCurio(ItemStack)` → `Optional<ICurio>` | ✅ 存在且未弃用 |
| `getCuriosHelper()` / `getSlotHelper()` / `getIconHelper()` | ⚠️ 存在但 **@Deprecated** |
| `createCurioProvider(ICurio)` | ❌ **不存在**（Forge `ICapabilityProvider` 专用，已删） |
| `getSlotUuid(SlotContext)` → `UUID` | ❌ **不存在**，改为 `getSlotId(SlotContext)` → `ResourceLocation` |

### 2.2 物品侧契约：`ICurioItem`（**9.5.1 就是它，不是 `ICurio`**）

`public interface top.theillusivec4.curios.api.type.capability.ICurioItem`，**类级未弃用**。逐字全量（**就是这 26 个 default 方法 + 1 个静态字段，没有任何其它重载**）：

```java
public static final top.theillusivec4.curios.api.type.capability.ICurio defaultInstance;
public default boolean hasCurioCapability(net.minecraft.world.item.ItemStack);
public default void curioTick(top.theillusivec4.curios.api.SlotContext, net.minecraft.world.item.ItemStack); public default void onEquip(top.theillusivec4.curios.api.SlotContext, net.minecraft.world.item.ItemStack, net.minecraft.world.item.ItemStack); public default void onUnequip(top.theillusivec4.curios.api.SlotContext, net.minecraft.world.item.ItemStack, net.minecraft.world.item.ItemStack);
public default boolean canEquip(top.theillusivec4.curios.api.SlotContext, net.minecraft.world.item.ItemStack); public default boolean canUnequip(top.theillusivec4.curios.api.SlotContext, net.minecraft.world.item.ItemStack);
public default java.util.List<net.minecraft.network.chat.Component> getSlotsTooltip(java.util.List<net.minecraft.network.chat.Component>, net.minecraft.world.item.Item$TooltipContext, net.minecraft.world.item.ItemStack);
@Deprecated public default java.util.List<net.minecraft.network.chat.Component> getSlotsTooltip(java.util.List<net.minecraft.network.chat.Component>, net.minecraft.world.item.ItemStack);
public default com.google.common.collect.Multimap<net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute>, net.minecraft.world.entity.ai.attributes.AttributeModifier> getAttributeModifiers(top.theillusivec4.curios.api.SlotContext, net.minecraft.resources.ResourceLocation, net.minecraft.world.item.ItemStack);
@Deprecated public default com.google.common.collect.Multimap<net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute>, net.minecraft.world.entity.ai.attributes.AttributeModifier> getAttributeModifiers(top.theillusivec4.curios.api.SlotContext, java.util.UUID, net.minecraft.world.item.ItemStack);
public default void onEquipFromUse(top.theillusivec4.curios.api.SlotContext, net.minecraft.world.item.ItemStack); public default top.theillusivec4.curios.api.type.capability.ICurio$SoundInfo getEquipSound(top.theillusivec4.curios.api.SlotContext, net.minecraft.world.item.ItemStack); public default boolean canEquipFromUse(top.theillusivec4.curios.api.SlotContext, net.minecraft.world.item.ItemStack);
public default void curioBreak(top.theillusivec4.curios.api.SlotContext, net.minecraft.world.item.ItemStack); public default boolean canSync(top.theillusivec4.curios.api.SlotContext, net.minecraft.world.item.ItemStack); public default net.minecraft.nbt.CompoundTag writeSyncData(top.theillusivec4.curios.api.SlotContext, net.minecraft.world.item.ItemStack); public default void readSyncData(top.theillusivec4.curios.api.SlotContext, net.minecraft.nbt.CompoundTag, net.minecraft.world.item.ItemStack);
public default top.theillusivec4.curios.api.type.capability.ICurio$DropRule getDropRule(top.theillusivec4.curios.api.SlotContext, net.minecraft.world.damagesource.DamageSource, boolean, net.minecraft.world.item.ItemStack);
@Deprecated public default top.theillusivec4.curios.api.type.capability.ICurio$DropRule getDropRule(top.theillusivec4.curios.api.SlotContext, net.minecraft.world.damagesource.DamageSource, int, boolean, net.minecraft.world.item.ItemStack);
public default java.util.List<net.minecraft.network.chat.Component> getAttributesTooltip(java.util.List<net.minecraft.network.chat.Component>, net.minecraft.world.item.Item$TooltipContext, net.minecraft.world.item.ItemStack);
@Deprecated public default java.util.List<net.minecraft.network.chat.Component> getAttributesTooltip(java.util.List<net.minecraft.network.chat.Component>, net.minecraft.world.item.ItemStack);
public default int getFortuneLevel(top.theillusivec4.curios.api.SlotContext, net.minecraft.world.level.storage.loot.LootContext, net.minecraft.world.item.ItemStack); public default int getLootingLevel(top.theillusivec4.curios.api.SlotContext, net.minecraft.world.level.storage.loot.LootContext, net.minecraft.world.item.ItemStack);
public default boolean makesPiglinsNeutral(top.theillusivec4.curios.api.SlotContext, net.minecraft.world.item.ItemStack); public default boolean canWalkOnPowderedSnow(top.theillusivec4.curios.api.SlotContext, net.minecraft.world.item.ItemStack); public default boolean isEnderMask(top.theillusivec4.curios.api.SlotContext, net.minecraft.world.entity.monster.EnderMan, net.minecraft.world.item.ItemStack);
```

**能力注册路径（从字节码验证，不是猜的）**：`top.theillusivec4.curios.Curios` 在 `RegisterCapabilitiesEvent` 中对 `BuiltInRegistries.ITEM` 全体执行 `RegisterCapabilitiesEvent.registerItem(CuriosCapability.ITEM, provider, items...)`。`javap -c` 反编译出的 provider（`lambda$registerCaps$2`）逻辑：① 查 `CuriosImplMixinHooks.getCurioFromRegistry(item)`（即 `CuriosApi.registerCurio` 写入的 `Map<Item, ICurioItem>`）；② 查不到 **且 `item instanceof ICurioItem`** → 直接把该 Item 当 `ICurioItem`；③ 要求 `hasCurioCapability(stack)` 为真；④ 返回 `new ItemizedCurioCapability(curioItem, stack)`，否则 `null`。

→ **Item 类 `implements ICurioItem` 即自动获得 `curios:item` 能力，无需调用 `CuriosApi.registerCurio`**；`registerCurio` 用于给「你不拥有的 Item」（如原版物品）挂外部实现。

### 2.3 `ICurio`（能力接口，不是物品接口）

`public interface top.theillusivec4.curios.api.type.capability.ICurio`，**类级未弃用**。物品作者通常**不实现它**（由 `ItemizedCurioCapability` 桥接）。逐字全量：

```java
public abstract net.minecraft.world.item.ItemStack getStack();
public default void curioTick(top.theillusivec4.curios.api.SlotContext); public default void onEquip(top.theillusivec4.curios.api.SlotContext, net.minecraft.world.item.ItemStack); public default void onUnequip(top.theillusivec4.curios.api.SlotContext, net.minecraft.world.item.ItemStack); public default boolean canEquip(top.theillusivec4.curios.api.SlotContext); public default boolean canUnequip(top.theillusivec4.curios.api.SlotContext);
public default java.util.List<net.minecraft.network.chat.Component> getSlotsTooltip(java.util.List<net.minecraft.network.chat.Component>, net.minecraft.world.item.Item$TooltipContext); @Deprecated public default java.util.List<net.minecraft.network.chat.Component> getSlotsTooltip(java.util.List<net.minecraft.network.chat.Component>);
@Deprecated public default com.google.common.collect.Multimap<net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute>, net.minecraft.world.entity.ai.attributes.AttributeModifier> getAttributeModifiers(top.theillusivec4.curios.api.SlotContext, java.util.UUID); public default com.google.common.collect.Multimap<net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute>, net.minecraft.world.entity.ai.attributes.AttributeModifier> getAttributeModifiers(top.theillusivec4.curios.api.SlotContext, net.minecraft.resources.ResourceLocation);
public default void onEquipFromUse(top.theillusivec4.curios.api.SlotContext); public default top.theillusivec4.curios.api.type.capability.ICurio$SoundInfo getEquipSound(top.theillusivec4.curios.api.SlotContext); public default boolean canEquipFromUse(top.theillusivec4.curios.api.SlotContext); public default void curioBreak(top.theillusivec4.curios.api.SlotContext);
public default boolean canSync(top.theillusivec4.curios.api.SlotContext); public default net.minecraft.nbt.CompoundTag writeSyncData(top.theillusivec4.curios.api.SlotContext); public default void readSyncData(top.theillusivec4.curios.api.SlotContext, net.minecraft.nbt.CompoundTag);
public default top.theillusivec4.curios.api.type.capability.ICurio$DropRule getDropRule(top.theillusivec4.curios.api.SlotContext, net.minecraft.world.damagesource.DamageSource, boolean); @Deprecated public default top.theillusivec4.curios.api.type.capability.ICurio$DropRule getDropRule(top.theillusivec4.curios.api.SlotContext, net.minecraft.world.damagesource.DamageSource, int, boolean);
@Deprecated public default java.util.List<net.minecraft.network.chat.Component> getAttributesTooltip(java.util.List<net.minecraft.network.chat.Component>); public default java.util.List<net.minecraft.network.chat.Component> getAttributesTooltip(java.util.List<net.minecraft.network.chat.Component>, net.minecraft.world.item.Item$TooltipContext);
public default int getFortuneLevel(top.theillusivec4.curios.api.SlotContext, net.minecraft.world.level.storage.loot.LootContext); public default int getLootingLevel(top.theillusivec4.curios.api.SlotContext, net.minecraft.world.level.storage.loot.LootContext);
public default boolean makesPiglinsNeutral(top.theillusivec4.curios.api.SlotContext); public default boolean canWalkOnPowderedSnow(top.theillusivec4.curios.api.SlotContext); public default boolean isEnderMask(top.theillusivec4.curios.api.SlotContext, net.minecraft.world.entity.monster.EnderMan);
public static void playBreakAnimation(net.minecraft.world.item.ItemStack, net.minecraft.world.entity.LivingEntity);
```

嵌套类型：`ICurio$DropRule`（`public final class ... extends java.lang.Enum`，常量 `DEFAULT, ALWAYS_DROP, ALWAYS_KEEP, DESTROY`）；`ICurio$SoundInfo`（`public final class ... extends java.lang.Record`，组件 `(net.minecraft.sounds.SoundEvent, float, float)`，同时提供 `getSoundEvent()/getVolume()/getPitch()` 与 record 访问器 `soundEvent()/volume()/pitch()`）。

### 2.4 `SlotContext`（**record**；**不存在 `ISlotContext`**）

`public final class top.theillusivec4.curios.api.SlotContext extends java.lang.Record`，逐字全量：

```java
public top.theillusivec4.curios.api.SlotContext(java.lang.String, net.minecraft.world.entity.LivingEntity, int, boolean, boolean);
public java.lang.String identifier(); public net.minecraft.world.entity.LivingEntity entity(); public int index(); public boolean cosmetic(); public boolean visible();
public final java.lang.String toString(); public final int hashCode(); public final boolean equals(java.lang.Object);
```

⚠️ **`ISlotContext` 不存在**（jar 内无此类，也没有任何变体）；`getWearer()` / `getIdentifier()` / `getIndex()` 在 9.5.1 **已彻底删除**（5.14.1 尚存但已 @Deprecated）。

### 2.5 容器与槽位处理器

- `top.theillusivec4.curios.api.type.capability.ICuriosItemHandler`（`public interface`，类级未弃用）逐字全量：
```java
public static final org.slf4j.Logger LOGGER;
public abstract java.util.Map<java.lang.String, top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler> getCurios(); public abstract void setCurios(java.util.Map<java.lang.String, top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler>); public abstract int getSlots(); public default int getVisibleSlots(); public abstract void reset();
public abstract java.util.Optional<top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler> getStacksHandler(java.lang.String); public abstract net.neoforged.neoforge.items.IItemHandlerModifiable getEquippedCurios(); public abstract void setEquippedCurio(java.lang.String, int, net.minecraft.world.item.ItemStack);
public default boolean isEquipped(net.minecraft.world.item.Item); public default boolean isEquipped(java.util.function.Predicate<net.minecraft.world.item.ItemStack>); public default boolean isSlotActive(java.lang.String, int); public default void setSlotActive(java.lang.String, int, boolean); public default void setSlotsActive(java.lang.String, boolean);
public abstract java.util.Optional<top.theillusivec4.curios.api.SlotResult> findFirstCurio(net.minecraft.world.item.Item); public abstract java.util.Optional<top.theillusivec4.curios.api.SlotResult> findFirstCurio(java.util.function.Predicate<net.minecraft.world.item.ItemStack>); public abstract java.util.Optional<top.theillusivec4.curios.api.SlotResult> findFirstCurio(java.util.function.Predicate<net.minecraft.world.item.ItemStack>, java.lang.String); public default java.util.Optional<top.theillusivec4.curios.api.SlotResult> findFirstCurio(java.util.function.Predicate<net.minecraft.world.item.ItemStack>, boolean, java.lang.String);
public abstract java.util.List<top.theillusivec4.curios.api.SlotResult> findCurios(net.minecraft.world.item.Item); public abstract java.util.List<top.theillusivec4.curios.api.SlotResult> findCurios(java.util.function.Predicate<net.minecraft.world.item.ItemStack>); public default java.util.List<top.theillusivec4.curios.api.SlotResult> findCurios(java.util.function.Predicate<net.minecraft.world.item.ItemStack>, boolean, java.lang.String); public abstract java.util.List<top.theillusivec4.curios.api.SlotResult> findCurios(java.lang.String...); public default java.util.List<top.theillusivec4.curios.api.SlotResult> findCurios(boolean, java.lang.String...);
public abstract java.util.Optional<top.theillusivec4.curios.api.SlotResult> findCurio(java.lang.String, int); public default java.util.Optional<top.theillusivec4.curios.api.SlotResult> findCurio(java.lang.String, int, boolean);
public abstract net.minecraft.world.entity.LivingEntity getWearer(); public abstract void loseInvalidStack(net.minecraft.world.item.ItemStack); public abstract void handleInvalidStacks(); public abstract int getFortuneLevel(net.minecraft.world.level.storage.loot.LootContext); public abstract int getLootingLevel(net.minecraft.world.level.storage.loot.LootContext);
public abstract net.minecraft.nbt.ListTag saveInventory(boolean); public abstract void loadInventory(net.minecraft.nbt.ListTag); public abstract java.util.Set<top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler> getUpdatingInventories();
public default void addTransientSlotModifier(java.lang.String, net.minecraft.resources.ResourceLocation, double, net.minecraft.world.entity.ai.attributes.AttributeModifier$Operation); public abstract void addTransientSlotModifiers(com.google.common.collect.Multimap<java.lang.String, net.minecraft.world.entity.ai.attributes.AttributeModifier>);
public default void addPermanentSlotModifier(java.lang.String, net.minecraft.resources.ResourceLocation, double, net.minecraft.world.entity.ai.attributes.AttributeModifier$Operation); public abstract void addPermanentSlotModifiers(com.google.common.collect.Multimap<java.lang.String, net.minecraft.world.entity.ai.attributes.AttributeModifier>);
public default void removeSlotModifier(java.lang.String, net.minecraft.resources.ResourceLocation); public abstract void removeSlotModifiers(com.google.common.collect.Multimap<java.lang.String, net.minecraft.world.entity.ai.attributes.AttributeModifier>); public abstract void clearSlotModifiers(); public abstract com.google.common.collect.Multimap<java.lang.String, net.minecraft.world.entity.ai.attributes.AttributeModifier> getModifiers();
public abstract net.minecraft.nbt.Tag writeTag(); public abstract void readTag(net.minecraft.nbt.Tag); public abstract void clearCachedSlotModifiers(); public default java.util.Set<java.lang.String> getLockedSlots(); public default void unlockSlotType(java.lang.String, int, boolean, boolean); public default void lockSlotType(java.lang.String); public default void processSlots(); public default int getFortuneBonus(); public abstract void growSlotType(java.lang.String, int); public abstract void shrinkSlotType(java.lang.String, int);
```
- `top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler`（`public interface`）逐字全量：
```java
public abstract top.theillusivec4.curios.api.type.inventory.IDynamicStackHandler getStacks(); public abstract top.theillusivec4.curios.api.type.inventory.IDynamicStackHandler getCosmeticStacks(); public abstract net.minecraft.core.NonNullList<java.lang.Boolean> getRenders();
public default net.minecraft.core.NonNullList<java.lang.Boolean> getActiveStates(); public default void updateActiveState(int); public default boolean canToggleRendering(); public default top.theillusivec4.curios.api.type.capability.ICurio$DropRule getDropRule();
public abstract int getSlots(); public abstract boolean isVisible(); public abstract boolean hasCosmetic(); public abstract net.minecraft.nbt.CompoundTag serializeNBT(); public abstract void deserializeNBT(net.minecraft.nbt.CompoundTag); public abstract java.lang.String getIdentifier();
public abstract java.util.Map<net.minecraft.resources.ResourceLocation, net.minecraft.world.entity.ai.attributes.AttributeModifier> getModifiers(); public abstract java.util.Set<net.minecraft.world.entity.ai.attributes.AttributeModifier> getPermanentModifiers(); public abstract java.util.Set<net.minecraft.world.entity.ai.attributes.AttributeModifier> getCachedModifiers(); public abstract java.util.Collection<net.minecraft.world.entity.ai.attributes.AttributeModifier> getModifiersByOperation(net.minecraft.world.entity.ai.attributes.AttributeModifier$Operation);
public abstract void addTransientModifier(net.minecraft.world.entity.ai.attributes.AttributeModifier); public abstract void addPermanentModifier(net.minecraft.world.entity.ai.attributes.AttributeModifier); public abstract void removeModifier(net.minecraft.resources.ResourceLocation); public abstract void clearModifiers(); public abstract void clearCachedModifiers(); public abstract void copyModifiers(top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler);
public abstract void update(); public abstract net.minecraft.nbt.CompoundTag getSyncTag(); public abstract void applySyncTag(net.minecraft.nbt.CompoundTag); public abstract int getSizeShift(); public abstract void grow(int); public abstract void shrink(int);
```
- `top.theillusivec4.curios.api.type.inventory.IDynamicStackHandler`（`public interface extends net.neoforged.neoforge.items.IItemHandlerModifiable`）逐字全量。**不存在 `ICurioStackHandler`**（jar 内无此类）：
```java
public abstract void setStackInSlot(int, net.minecraft.world.item.ItemStack); public abstract net.minecraft.world.item.ItemStack getStackInSlot(int); public abstract void setPreviousStackInSlot(int, net.minecraft.world.item.ItemStack); public abstract net.minecraft.world.item.ItemStack getPreviousStackInSlot(int); public abstract int getSlots(); public abstract void grow(int); public abstract void shrink(int);
public abstract net.minecraft.nbt.CompoundTag serializeNBT(net.minecraft.core.HolderLookup$Provider); public abstract void deserializeNBT(net.minecraft.core.HolderLookup$Provider, net.minecraft.nbt.CompoundTag);
```
- `top.theillusivec4.curios.api.type.ISlotType`（`public interface ... extends java.lang.Comparable<top.theillusivec4.curios.api.type.ISlotType>`）：`getIdentifier() ; getIcon() ; getOrder() ; getSize() ; useNativeGui() ; hasCosmetic() ; canToggleRendering() ; getDropRule()`（以上 abstract）+ `getValidators() ; writeNbt()`（default）+ **@Deprecated** `isLocked() ; getPriority() ; isVisible()`。
- `top.theillusivec4.curios.api.SlotResult`（`public final class ... extends java.lang.Record`，组件 `(SlotContext, ItemStack)`）：`slotContext()` / `stack()`。
- `top.theillusivec4.curios.api.type.ICuriosMenu`（`public interface`）：唯一方法 `public abstract void resetSlots();`。

### 2.6 槽位注册：**没有 Java 侧注册事件，走 datapack JSON**

`top/theillusivec4/curios/api/` 下**不存在** `RegisterCuriosSlotsEvent` / `CuriosSlotType`；`CuriosRegistry` 只存在于 `top.theillusivec4.curios.common`（Curios 内部注册器，`public static void init(IEventBus)`，不供 mod 使用）；`CuriosApi` **没有** `registerSlot` / `registerCurioSlot`。

真实机制（`javap -c` 常量池）：`top.theillusivec4.curios.common.data.CuriosSlotManager extends SimpleJsonResourceReloadListener`，资源根 **`curios/slots`**；`CuriosEntityManager` 资源根 **`curios/entities`**。即：

- 槽位定义 `data/<namespace>/curios/slots/<id>.json`
- 实体→槽位映射 `data/<namespace>/curios/entities/<id>.json`

由 `top.theillusivec4.curios.api.CuriosDataProvider`（`public abstract class ... implements net.minecraft.data.DataProvider`）在 datagen 生成，逐字：

```java
public top.theillusivec4.curios.api.CuriosDataProvider(java.lang.String, net.minecraft.data.PackOutput, net.neoforged.neoforge.common.data.ExistingFileHelper, java.util.concurrent.CompletableFuture<net.minecraft.core.HolderLookup$Provider>);
public abstract void generate(net.minecraft.core.HolderLookup$Provider, net.neoforged.neoforge.common.data.ExistingFileHelper); public java.util.concurrent.CompletableFuture<?> run(net.minecraft.data.CachedOutput); public final java.lang.String getName();
public final top.theillusivec4.curios.api.type.data.ISlotData createSlot(java.lang.String); public final top.theillusivec4.curios.api.type.data.ISlotData copySlot(java.lang.String, java.lang.String); public final top.theillusivec4.curios.api.type.data.IEntitiesData createEntities(java.lang.String); public final top.theillusivec4.curios.api.type.data.IEntitiesData copyEntities(java.lang.String, java.lang.String);
```

`ISlotData`（全部返回 `ISlotData`，除 `serialize`）：`replace(boolean) ; order(int) ; size(int) ; operation(java.lang.String) ; operation(net.minecraft.world.entity.ai.attributes.AttributeModifier$Operation) ; useNativeGui(boolean) ; addCosmetic(boolean) ; renderToggle(boolean) ; icon(net.minecraft.resources.ResourceLocation) ; dropRule(top.theillusivec4.curios.api.type.capability.ICurio$DropRule) ; addCondition(net.neoforged.neoforge.common.conditions.ICondition) ; addValidator(net.minecraft.resources.ResourceLocation) ; com.google.gson.JsonObject serialize(net.minecraft.core.HolderLookup$Provider)`。

`IEntitiesData`：`replace(boolean) ; addPlayer() ; addEntities(net.minecraft.world.entity.EntityType<?>...) ; addSlots(java.lang.String...) ; addCondition(net.neoforged.neoforge.common.conditions.ICondition) ; com.google.gson.JsonObject serialize(net.minecraft.core.HolderLookup$Provider)`。

JSON 字段名（从 `CuriosDataProvider` / `CuriosSlotManager.fromJson` / `CuriosEntityManager` 常量池读出）：
槽位 = `replace`(bool), `size`(int), `operation`(`"SET"`/`"ADD"`/`"REMOVE"`), `order`(int), `icon`(ResourceLocation), `render_toggle`(bool), `add_cosmetic`(bool), `use_native_gui`(bool), `drop_rule`(string), `validators`(ResourceLocation 数组)。
实体 = `replace`(bool), `entities`, `slots`（常量池另有 `#`、`{} is not a registered entity type!`、`{} is not a registered slot type!`，说明存在 `#` tag 语法）。

### 2.7 事件类（`top/theillusivec4/curios/api/event/`，jar 内共 7 个，无其它）

| 类 | 父类 | 用途与逐字方法名 |
|---|---|---|
| `CurioAttributeModifierEvent` | `net.neoforged.bus.api.Event` | 改某 `ItemStack` 在某 slot 上的属性修饰符：`getModifiers() ; getOriginalModifiers() ; addModifier(Holder<Attribute>, AttributeModifier) ; removeModifier(Holder<Attribute>, AttributeModifier) ; removeAttribute(Holder<Attribute>) ; clearModifiers() ; getSlotContext() ; getItemStack() ; getId()` |
| `CurioCanEquipEvent` | `net.neoforged.neoforge.event.entity.living.LivingEvent` | 可否装备，结果为 `net.neoforged.neoforge.common.util.TriState`：`getEquipResult() ; setEquipResult(TriState) ; getSlotContext() ; getStack()` |
| `CurioCanUnequipEvent` | 同上 | 可否卸下：`getUnequipResult() ; setUnequipResult(TriState) ; getSlotContext() ; getStack()` |
| `CurioChangeEvent` | 同上 | 槽位内容变更：`getIdentifier() ; getSlotIndex() ; getFrom() ; getTo()` |
| `CurioDropsEvent` | 同上，`implements net.neoforged.bus.api.ICancellableEvent` | 死亡掉落：`getCurioHandler() ; getSource() ; getDrops() ; getLootingLevel() ; isRecentlyHit()` |
| `DropRulesEvent` | 同上 | 覆写掉落规则：`addOverride(Predicate<ItemStack>, ICurio.DropRule) ; getOverrides()` → `ImmutableList<Tuple<Predicate<ItemStack>, ICurio.DropRule>>` |
| `SlotModifiersUpdatedEvent` | 同上 | 槽位修饰符更新：`getTypes()` → `Set<String>` |

**已消失**：5.14.1 的 `CurioEquipEvent` / `CurioUnequipEvent` 在 9.5.1 **不存在**。

### 2.8 物品作者常用的周边类型

- `top.theillusivec4.curios.api.CuriosCapability`（`public class`）：`ID_INVENTORY, ID_ITEM_HANDLER, ID_ITEM`（`ResourceLocation`）；`INVENTORY` = `EntityCapability<ICuriosItemHandler, Void>`；`ITEM_HANDLER` = `EntityCapability<net.neoforged.neoforge.items.IItemHandler, Void>`；`ITEM` = `ItemCapability<top.theillusivec4.curios.api.type.capability.ICurio, Void>`。**`ITEM` 的能力类型是 `ICurio`，不是 `ICurioItem`。**
- `top.theillusivec4.curios.api.CurioAttributeModifiers`（`public final class ... extends java.lang.Record`）：`EMPTY ; CODEC ; STREAM_CODEC ; builder() ; withTooltip(boolean) ; withModifierAdded(ResourceLocation, AttributeModifier, String) ; forEach(String, BiConsumer<ResourceLocation, AttributeModifier>)`；组件 `(List<CurioAttributeModifiers$Entry> modifiers, boolean showInTooltip)`。`Entry`（record）组件 `(ResourceLocation attribute, AttributeModifier modifier, String slot)`。`Builder.add(net.minecraft.core.Holder<Attribute>, AttributeModifier, String)`。落地为数据组件 **`curios:attribute_modifiers`**（`CuriosRegistry` 常量池：命名空间 `curios` + 名 `attribute_modifiers`，类型 `DataComponentType<CurioAttributeModifiers>`）。
- `top.theillusivec4.curios.api.SlotAttribute extends net.minecraft.world.entity.ai.attributes.Attribute`：`public static net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> getOrCreate(java.lang.String)`。
- `top.theillusivec4.curios.api.CuriosTags`（`public final class`）：`BACK, BELT, BODY, BRACELET, CHARM, CURIO, HANDS, HEAD, NECKLACE, RING`（`TagKey<Item>`）+ `createItemTag(java.lang.String)`。
- `top.theillusivec4.curios.api.SlotTypePreset`（`public final class ... extends java.lang.Enum`）：`HEAD, NECKLACE, BACK, BODY, BRACELET, HANDS, RING, BELT, CHARM, CURIO`；`findPreset(String)`、`getIdentifier()`、`getMessageBuilder()` 均 **@Deprecated**。
- `top.theillusivec4.curios.api.SlotTypeMessage`（`public final class`，类级未弃用）：`REGISTER_TYPE = "register_type"`、`MODIFY_TYPE = "modify_type"`（IMC 遗留）+ 内嵌 `Builder`（`Builder(String) ; icon ; priority ; size ; lock ; hide ; cosmetic ; build`）。
- `top.theillusivec4.curios.api.extensions`：`RegisterCuriosExtensionsEvent extends net.neoforged.bus.api.Event implements net.neoforged.fml.event.IModBusEvent`（**MOD 总线**），`registerSlotExtension(ICurioSlotExtension, java.lang.String...)`、`isSlotExtensionRegistered(java.lang.String)`。`ICurioSlotExtension`：`DEFAULT ; from(String) ; getDisplayStack(SlotContext, ItemStack) ; getCloneStack(SlotContext, ItemStack) ; getSlotTooltip(SlotContext, TooltipFlag)`。
- `top.theillusivec4.curios.api.CuriosTooltip`：`append ; appendHeader ; appendSlotHeader ; appendAdditive ; appendSubtractive ; appendEqual ; forSlots(String...) ; forSlots(ItemStack) ; forSlots(ItemStack, LivingEntity) ; build()`。
- 已弃用 helper：`api.type.util.ICuriosHelper`（**类级未弃用，但全部 18 个方法逐个带 `@Deprecated`**）、`ISlotHelper`（类级未弃用，全部 18 个方法 @Deprecated）、`IIconHelper`（类级未弃用，全部 3 个方法 @Deprecated）。**新代码不要用。**

---

## 3. 客户端 vs 服务端边界

判定方式：对 jar 内每个 `.class` 扫描常量池是否含 `net/minecraft/client` 或 `com/mojang/blaze3d`。

**`top/theillusivec4/curios/api/` 下唯一直接引用客户端类的类型**：
- `top.theillusivec4.curios.api.client.ICurioRenderer` — 签名含 `net.minecraft.client.model.EntityModel`、`net.minecraft.client.renderer.entity.RenderLayerParent`、`net.minecraft.client.renderer.MultiBufferSource`、`com.mojang.blaze3d.vertex.PoseStack`、`net.minecraft.client.model.geom.ModelPart`、`net.minecraft.client.model.HumanoidModel`。**纯客户端。**

**按包/签名判定为客户端**：
- `top.theillusivec4.curios.api.client.CuriosRendererRegistry` — 自身常量池无客户端字符串，但 `register(Item, Supplier<ICurioRenderer>)` / `getRenderer(Item)` → `Optional<ICurioRenderer>` 绑定客户端类型，**按客户端处理**。
- `top.theillusivec4.curios.api.client.ICuriosScreen` — 空标记接口（无成员，字节码**无**客户端引用），但只服务饰品界面，**按客户端处理**。
- 整个 `top.theillusivec4.curios.client.*`（`ClientEventHandler`、`CuriosClientConfig`、`gui/*`、`IconHelper`、`KeyRegistry`、`render/CuriosLayer`）—— 客户端专属实现层。

**实现层中引用客户端类的非 `client` 包类**（服务端不可加载）：`common/integration/CuriosExclusionAreas`、`common/integration/emi/CuriosEmiIntegration`、`common/integration/emi/CuriosEmiPlugin`、`common/integration/jei/CuriosContainerHandler`、`common/integration/rei/CuriosReiPlugin`、`common/inventory/CosmeticCurioSlot`、`common/inventory/CurioSlot`、`common/network/client/CuriosClientPackets`、`Curios$ClientProxy`。

**服务端安全（`logic/` / `data/` 可直接用）**：`CuriosApi`、`CuriosCapability`、`SlotContext`、`SlotResult`、`CurioAttributeModifiers`(+`Builder`/`Entry`)、`api/` 根下其余类、整个 `api.event`、`api.extensions`、`api.type`、`api.type.capability`（**含 `ICurio` / `ICurioItem`**）、`api.type.data`、`api.type.inventory`、`api.type.util`、`CuriosDataProvider`（datagen 期）。逐类扫描确认这些类常量池中**没有** `net/minecraft/client` / `com/mojang/blaze3d`。

⚠️ `CuriosApi` 与 `RegisterCuriosExtensionsEvent` 的常量池含 `net/neoforged/fml`（`FMLEnvironment`、`IModBusEvent`）——那是**加载器**类不是客户端类，服务端存在。

**`logic/` 的可执行判据**：只要不 import `top.theillusivec4.curios.api.client.*` 与 `top.theillusivec4.curios.client.*`，就不会因 Curios 把客户端类拖进 `logic`。

---

## 4. 1.20.1 (curios-forge 5.14.1) → 1.21.1 (curios-neoforge 9.5.1) 迁移陷阱

**两侧都逐字核验才列入本表**。新 = 本 jar `javap`；旧 = `~/.gradle/caches/forge_gradle/maven_downloader/top/theillusivec4/curios/curios-forge/5.14.1+1.20.1/curios-forge-5.14.1+1.20.1-sources.jar`（**仅供参考，绝不可当作当前签名**）。

| # | 5.14.1（1.20.1）写法 | 9.5.1（1.21.1）实际要求 | 后果 |
|---|---|---|---|
| M1 | `ICurioItem` 里写 `curioTick(String identifier, int index, LivingEntity, ItemStack)`、`curioAnimate(...)`、`getTagsTooltip(List, ItemStack)`、`canRightClickEquip(ItemStack)`、`playRightClickEquipSound(...)`、`showAttributesTooltip(String, ItemStack)`、`getFortuneBonus(...)`、`getLootingBonus(...)`、`readSyncData(CompoundTag, ItemStack)` | 这些重载**全部不存在**（9.5.1 的 `ICurioItem` 只有 26 个方法，见 §2.2） | `@Override` 编译失败 |
| M2 | 用 `ICurio` 当物品接口直接实现（1.16–1.18 风格教程） | 物品实现 **`ICurioItem`**；`CuriosCapability.ITEM` 的能力类型才是 `ICurio` | 编译失败或能力查不到 |
| M3 | `slotContext.getWearer()` / `.getIdentifier()` / `.getIndex()` | `SlotContext` 是 record，只有 `entity()` / `identifier()` / `index()` / `cosmetic()` / `visible()` | 编译失败 |
| M4 | `CuriosApi.getSlotUuid(SlotContext)` → `UUID` | `CuriosApi.getSlotId(SlotContext)` → `ResourceLocation`；`ICurioStacksHandler.getModifiers()` 键由 `UUID` 变 `ResourceLocation`；`removeModifier(UUID)` → `removeModifier(ResourceLocation)` | 编译失败 |
| M5 | `CuriosApi.addSlotModifier(ItemStack, String identifier, String name, UUID uuid, double amount, AttributeModifier.Operation op)` | `addSlotModifier(net.minecraft.world.item.ItemStack, java.lang.String, net.minecraft.resources.ResourceLocation, double, net.minecraft.world.entity.ai.attributes.AttributeModifier$Operation, java.lang.String)`（参数顺序也变了）；新增 `withSlotModifier(ItemAttributeModifiers, String, ResourceLocation, double, Operation, EquipmentSlotGroup)` | 编译失败 |
| M6 | `CuriosApi.addModifier(ItemStack, Attribute, String name, UUID uuid, double, Operation, String slot)` | `addModifier(ItemStack, net.minecraft.core.Holder<Attribute>, ResourceLocation, double, Operation, String)` —— `Attribute`→`Holder<Attribute>`，`UUID`→`ResourceLocation` | 编译失败 |
| M7 | `getAttributeModifiers(SlotContext, UUID, ItemStack)` | 新签名 `getAttributeModifiers(SlotContext, ResourceLocation, ItemStack)`；`UUID` 版仍存在但 **@Deprecated** | 能用但过时 |
| M8 | `ICurioItem.getLootingLevel(SlotContext, DamageSource, LivingEntity target, int baseLooting, ItemStack)` | `getLootingLevel(SlotContext, net.minecraft.world.level.storage.loot.LootContext, ItemStack)`；handler 侧 `getLootingLevel(DamageSource, LivingEntity, int)` → `getLootingLevel(LootContext)` | 编译失败 |
| M9 | `getSlotsTooltip(List<Component>, ItemStack)` / `getAttributesTooltip(List<Component>, ItemStack)` | 新增带 `Item$TooltipContext` 的版本；旧版仍存在但 **@Deprecated** | 能用但过时 |
| M10 | `CuriosApi.getCurio(ItemStack)` → Forge `LazyOptional<ICurio>` | NeoForge `java.util.Optional<ICurio>` | 所有 `LazyOptional` 代码编译失败 |
| M11 | `CuriosApi.createCurioProvider(ICurio)` + 注册 `ICapabilityProvider` | 该方法**不存在**；改为「Item `implements ICurioItem`」或 `CuriosApi.registerCurio(Item, ICurioItem)`，能力由 Curios 自己在 `RegisterCapabilitiesEvent` 注册 | 编译失败 |
| M12 | `CuriosCapability.ITEM` 是 Forge `Capability<ICurio>`（`CapabilityManager.get(CapabilityToken)`） | `net.neoforged.neoforge.capabilities.ItemCapability<ICurio, Void>`；另新增 `ITEM_HANDLER`、`ID_ITEM_HANDLER` | 编译失败 |
| M13 | 用 `CuriosApi.getCuriosHelper()` / `ISlotHelper` / `IIconHelper` 作为主查询路径 | 类与方法都在，但**全部 @Deprecated**；正路是 `CuriosApi.getCuriosInventory(entity)` → `ICuriosItemHandler` → `findFirstCurio`/`findCurios`/`getStacksHandler` | 能编译但走死路 |
| M14 | `CurioEquipEvent` / `CurioUnequipEvent` | 两者**不存在**；改用 `CurioCanEquipEvent` / `CurioCanUnequipEvent`，结果为 `net.neoforged.neoforge.common.util.TriState`（不是 boolean） | 编译失败 |
| M15 | `IDynamicStackHandler.serializeNBT()` / `deserializeNBT(CompoundTag)` | `serializeNBT(net.minecraft.core.HolderLookup$Provider)` / `deserializeNBT(HolderLookup$Provider, CompoundTag)` | 编译失败 |
| M16 | `ICurioStacksHandler` 无 `getActiveStates()/updateActiveState(int)/canToggleRendering()/getDropRule()` | 9.5.1 新增这 4 个（均 default） | 不影响编译，读状态要用新方法 |
| M17 | `CuriosDataProvider` + `createSlot/copySlot/createEntities/copyEntities` + `generate(HolderLookup.Provider, ExistingFileHelper)` | **签名完全一致**；槽位注册一直是 datapack JSON，**不是** Java 事件 | 这块不用改 |
| M18 | 用 `SlotTypePreset` / `SlotTypeMessage` / IMC `"register_type"` 注册槽位 | 类型仍在，但 `SlotTypePreset.findPreset/getIdentifier/getMessageBuilder` **@Deprecated**；`SlotTypeMessage.REGISTER_TYPE/MODIFY_TYPE` 仍是 IMC 遗留路径 | 能编译但过时；新代码用 `CuriosDataProvider` |

**明确排除的误传**：网上说「9.x 把 `ICurioItem` 改名成 `ICurio`」——**不对**。两侧都有 `ICurio` 与 `ICurioItem`，分工一致（`ICurioItem` = 物品实现接口，`ICurio` = 每堆叠能力接口）。真正的断裂点是 **`ICurioItem` 的 legacy 重载被整批删除**（M1）与 **UUID → ResourceLocation 迁移**（M4–M7）。

---

## 5. 依赖声明怎么填

### 5.1 当前值 + 实测结论

`src/main/resources/META-INF/neoforge.mods.toml` 现为：

```toml
[[dependencies.tkr]]
modId="curios"  type="required"  versionRange="[9.5.1+1.21.1]"  ordering="AFTER"  side="BOTH"
```

**该值已被真实运行时验证通过**。证据：`run/logs/latest.log`（2026-09-19 21:03，FML 4.0.42）Mod List 输出 `Curios API 9.5.1+1.21.1 (curios)`，且**没有**任何 `Mod tkr requires curios ...` 失败。→ **推荐维持不动。**

### 5.2 为什么不要「顺手改成纯数字下界」

本仓库 P-001 记录：Patchouli 用 `versionRange="[1.21.1-93-NEOFORGE,)"` 被判不满足，崩溃报告原文（`run/crash-reports/crash-2026-09-19_18.49.19-fml.txt`）：

```
Failure message: Mod tkr requires patchouli Patchouli-1.21.1-93-NEOFORGE.jar
	Currently, patchouli is 1.21.1-93-NEOFORGE
```

**注意**：P-001 里「改用纯数字下界 `[93,)`」这条建议，用 Maven 语义实测是**错的**。实测（`org.apache.maven.artifact.versioning.VersionRange` / `ComparableVersion`，maven-artifact **3.9.1**，SHA256 `1B673D76CF8777344C6350D588533EA394DD93FE9A1B2BFF319D96CB0DDE74F2`）：

```
compare("9.5.1+1.21.1", "9.5.1") = 2      # 即 "9.5.1+1.21.1" > "9.5.1"
compare("9.5.1+1.21.1", "9.5")   = 1
compare("9.5.1+1.21.1", "9.6")   = -1
range [9.5.1+1.21.1]         contains "9.5.1+1.21.1" -> true
range [9.5.1+1.21.1,)        contains "9.5.1+1.21.1" -> true
range [9.5.1,)               contains "9.5.1+1.21.1" -> true
range [9.5,9.6)              contains "9.5.1+1.21.1" -> true
range [1.21.1-93-NEOFORGE,)  contains "1.21.1-93-NEOFORGE" -> true
range [93,)                  contains "1.21.1-93-NEOFORGE" -> **false**   ← P-001 的建议会失败
```

**关键推论**：`[1.21.1-93-NEOFORGE,)` 在 Maven 语义下**满足**，但 FML 4.0.42 运行时判**不满足** → **FML 的版本区间解析与 Maven 不一致**。因此上面这组 Maven 数据只能当参考（**未验证** FML 同构），**不能**作为改 `versionRange` 的依据。

### 5.3 结论与可选替代

- **推荐（已验证）**：`versionRange="[9.5.1+1.21.1]"`。闭区间两端都是 jar 自报的完整版本串，比较退化为字符串自等，绕开 `+` 的解析歧义。
- **若日后必须放行更新的 9.x**：可选 `versionRange="[9.5.1+1.21.1,)"`（Maven 语义 true；**未验证** FML 是否接受，Patchouli 先例说明有风险）。
- **若要完全避开 `+`**：`versionRange="[9.5,9.6)"`（Maven 语义 true；同样**未验证** FML）。
- **改动流程（强制）**：改完必须真正启动一次（`.\gradlew.bat runClient` / `runServer`），日志里确认出现 `Curios API 9.5.1+1.21.1 (curios)` 且**没有** `Mod tkr requires curios` 失败。不要只看文本「写得像」。
- `ordering="AFTER"` 与 `side="BOTH"` 保持不变（Curios 自声明 `side="BOTH"`）。

---

## 6. 最小可用骨架（NeoForge 1.21.1 + Curios 9.5.1）

```java
// ① 物品注册（标准 NeoForge 1.21.1）
public final class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems("tkr");
    public static final DeferredRegister.ItemRegisterEntry<AmuletItem> AMULET =
        ITEMS.registerItem("amulet", AmuletItem::new, new Item.Properties().stacksTo(1));
}

// ② 物品实现 ICurioItem —— 无需调用 CuriosApi.registerCurio（见 §2.2 的字节码证据）
public class AmuletItem extends Item implements ICurioItem {

    public AmuletItem(Properties properties) { super(properties); }

    @Override
    public void curioTick(SlotContext slotContext, ItemStack stack) {
        LivingEntity wearer = slotContext.entity();   // 不是 getWearer()/getIdentifier()/getIndex()
        if (!wearer.level().isClientSide && wearer.tickCount % 20 == 0) { /* ... */ }
    }

    @Override
    public boolean canEquip(SlotContext slotContext, ItemStack stack) { return true; }

    @Override
    public boolean canUnequip(SlotContext slotContext, ItemStack stack) { return true; }

    @Override
    public Multimap<Holder<Attribute>, AttributeModifier> getAttributeModifiers(
            SlotContext slotContext, ResourceLocation id, ItemStack stack) {
        Multimap<Holder<Attribute>, AttributeModifier> map = HashMultimap.create();
        map.put(Attributes.MAX_HEALTH,
                new AttributeModifier(id, 4.0, AttributeModifier.Operation.ADD_VALUE));
        return map;
    }

    @Override
    public void onEquip(SlotContext slotContext, ItemStack prevStack, ItemStack stack) { }

    @Override
    public void onUnequip(SlotContext slotContext, ItemStack newStack, ItemStack stack) { }
}
```

**查询已装备饰品（服务端安全，`logic/` 直接用）**：

```java
CuriosApi.getCuriosInventory(player).ifPresent(handler -> {
    handler.findFirstCurio(ModItems.AMULET.get()).ifPresent(result -> {
        SlotContext ctx   = result.slotContext();
        ItemStack   stack = result.stack();
    });
    handler.getStacksHandler("ring").ifPresent(sh -> { /* ICurioStacksHandler */ });
});
```

**注册自定义槽位——只有 datapack JSON 一条路**（§2.6）。两种等价写法：

(a) datagen（类型名来自 §2.6）：

```java
public class TkrCuriosDataProvider extends CuriosDataProvider {
    public TkrCuriosDataProvider(PackOutput output, ExistingFileHelper helper,
                                 CompletableFuture<HolderLookup.Provider> registries) {
        super("tkr", output, helper, registries);
    }

    @Override
    public void generate(HolderLookup.Provider registries, ExistingFileHelper helper) {
        createSlot("talisman").size(2).order(100)
            .icon(ResourceLocation.fromNamespaceAndPath("tkr", "slot/talisman"))
            .addCosmetic(true).renderToggle(true).useNativeGui(true)
            .dropRule(ICurio.DropRule.ALWAYS_KEEP);
        createEntities("tkr_entities").addPlayer().addSlots("talisman");
    }
}
```

(b) 手写资源。`src/main/resources/data/tkr/curios/slots/talisman.json`：

```json
{ "size": 2, "order": 100, "icon": "tkr:slot/talisman",
  "add_cosmetic": true, "render_toggle": true, "use_native_gui": true, "drop_rule": "ALWAYS_KEEP" }
```

`src/main/resources/data/tkr/curios/entities/tkr_entities.json`：

```json
{ "entities": ["minecraft:player"], "slots": ["talisman"] }
```

**本节未验证的部分**：见 §7 的 U3–U8。

---

## 7. 未验证清单（汇总）

| # | 未验证项 | 说明 |
|---|---|---|
| U1 | FML 4.0.42 的版本区间解析是否与 maven-artifact 3.9.1 一致 | 已证明**不一致**（Patchouli 反例）；§5.2 的 Maven 实测因此只作参考 |
| U2 | `"[9.5.1+1.21.1,)"` / `"[9.5,9.6)"` 在 FML 下是否真的匹配 | 只有 `[9.5.1+1.21.1]` 有运行时证据 |
| U3 | `curios/slots/*.json` 的 `drop_rule` 取值拼写 | 只验证了 Java 枚举常量名 `DEFAULT/ALWAYS_DROP/ALWAYS_KEEP/DESTROY` |
| U4 | `curios/slots/*.json` 的 `icon` 序列化形式 | 只验证了 `ISlotData.icon(ResourceLocation)` |
| U5 | `curios/entities/*.json` 的完整 schema（`entities` 数组 / `#` tag 语法） | 只读到常量池字符串 |
| U6 | `GatherDataEvent` 在 NeoForge 21.1.232 的精确回调签名 | 不属本 jar |
| U7 | `DeferredRegister.Items` / `registerItem` 在 NeoForge 21.1.232 的精确签名 | 不属本 jar |
| U8 | `ICurioRenderer` 在 1.21.1 渲染管线中的正确用法 | 签名已验证，用法未验证 |
| U9 | `ICuriosHelper`/`ISlotHelper`/`IIconHelper` 各自计划移除版本 | 本 jar 只留 `java.lang.Deprecated`（`ApiStatus.ScheduledForRemoval` 编译期被剥离），旧源码写的是 1.22 |
| U10 | `ICurioSlotExtension` 各方法在实际流程中的调用时机 | 签名已验证，触发点未追 |
