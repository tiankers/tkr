# 原版属性提取口径（MC 1.21.1 / NeoForge 21.1.232）

提取日期：2026-09-19。数据文件：`mc-1.21.1-attributes.tsv`（31 行数据 + 1 行表头）。

## 1. 使用的源与条目

| 用途 | 路径 / jar 条目 | 标识 |
|---|---|---|
| 属性注册表（权威） | `build/moddev/artifacts/neoforge-21.1.232-sources.jar` → `net/minecraft/world/entity/ai/attributes/Attributes.java` | jar size 9401926, SHA256 `62DC04D381181792FEFB6B5F73D7A227A917BE6265C56EC56468BE71B6D46D16` |
| 范围属性构造器 | 同 jar → `net/minecraft/world/entity/ai/attributes/RangedAttribute.java` | 同上 |
| 基类（sentiment/sync） | 同 jar → `net/minecraft/world/entity/ai/attributes/Attribute.java` | 同上 |
| 实体默认属性 | 同 jar → `DefaultAttributes.java`、`LivingEntity.java`、`Mob.java`、`Player.java` | 用于「哪些属性真被实体用」 |
| 英文名（权威） | `C:\Users\26461\.gradle\caches\neoformruntime\artifacts\minecraft_1.21.1_client.jar` → `assets/minecraft/lang/en_us.json` | jar size 26836906, SHA1 `30c73b1c5da787909b2f73340419fdf13b9def88`（与 1.21.1 版本清单声明的 client SHA1 逐字节一致） |
| 运行时编译产物（交叉验证） | `build/moddev/artifacts/neoforge-21.1.232-merged.jar` → `net/minecraft/world/entity/ai/attributes/Attributes.class` | size 32418966, SHA256 `F3B1671EE28BE3BD2982F2CA1F27A3B58D1A9D4007B324ACDC7BFBF968380B02` |
| 中文名 | `C:\Users\26461\.gradle\caches\neoformruntime\assets\objects\a3\a39b6311c1c167c40da1af6c81a680e2a0cb6b01` | SHA1 `a39b6311c1c167c40da1af6c81a680e2a0cb6b01`, size 554929 |

**zh_cn 的来源偏差（重要）**：`minecraft_1.21.1_client.jar` **只含 `assets/minecraft/lang/en_us.json`，不含 zh_cn.json**（整个 jar 只有一个 `/lang/` 条目；`client-extra` jar 同样只有 en_us）。1.21.1 的官方中文由资源索引以独立 asset object 下发。取值路径：1.21.1 版本清单 → `assetIndex` id `17`, SHA1 `76d7a97b9e0778fda3b14e474f012450ca0de1bb`, size 449557 → 本机 `assets/indexes/17.json`（SHA1 校验通过，逐字节一致）→ 其中 `"minecraft/lang/zh_cn.json"` = `{"hash":"a39b6311...","size":554929}` → 上述 object（SHA1 校验通过）。该链条每一环都做了 SHA1 校验，因此**仍锁定到 1.21.1**，但来源是 asset object 而非 jar。

## 2. `generic.` 前缀：**存在**

1.21.1 的属性 id **仍然带 `generic.` / `player.` / `zombie.` 前缀**，翻译键仍是 `attribute.name.generic.*`。
**1.21.1 没有扁平化**（网上常见的「1.21 去掉了 generic. 前缀」说法对本版本**不成立**，扁平化发生在更晚的版本）。三条独立证据：

1. `Attributes.java` 源码：`register("generic.max_health", new RangedAttribute("attribute.name.generic.max_health", ...))`。
2. `en_us.json`（jar 内）：34 个 `attribute.name.*` 键全部带前缀，`attribute.name.max_health` 这类裸键**数量为 0**。
3. **编译产物** `neoforge-21.1.232-merged.jar` 的 `Attributes.class` 常量池中，注册 id 字面量为 `generic.armor`、`generic.max_health`、`player.block_break_speed`、`zombie.spawn_reinforcements` —— 这是 mod 运行时实际面对的注册名。

**实际前缀**：`generic.`（23 个）、`player.`（7 个）、`zombie.`（1 个）。无 `horse.` 前缀属性（`horse.jump_strength` 已并入 `generic.jump_strength`）。

## 3. 数量

**31** 个属性，全部列在 TSV 中。en_us 与 zh_cn 对 31 个键**全部命中，无缺失**。

## 4. 注册代码片段（可照抄）

```java
public static final Holder<Attribute> MAX_HEALTH = register(
    "generic.max_health", new RangedAttribute("attribute.name.generic.max_health", 20.0, 1.0, 1024.0).setSyncable(true)
);

private static Holder<Attribute> register(String p_22291_, Attribute p_22292_) {
    return Registry.registerForHolder(BuiltInRegistries.ATTRIBUTE, ResourceLocation.withDefaultNamespace(p_22291_), p_22292_);
}
```

- **注册表目标：`BuiltInRegistries.ATTRIBUTE`**（不是 `Attributes` 类；`Attributes` 只是持有 `Holder<Attribute>` 常量的门面类）。注册键为 `ResourceLocation.withDefaultNamespace(id)`，即 `minecraft:` 命名空间。
- 注册顺序 = `Attributes.java` 中字段声明顺序（TSV 行序即此顺序）。注意它**不是严格的字典序**：`EXPLOSION_KNOCKBACK_RESISTANCE` 排在 `ENTITY_INTERACTION_RANGE` 之前。
- **mod 应使用 `DeferredRegister.create(BuiltInRegistries.ATTRIBUTE, "tkr")`**（NeoForge），不要试图往 `Attributes` 里塞字段。

## 5. 构造器与修饰方法

```java
public RangedAttribute(String descriptionId, double defaultValue, double minValue, double maxValue)
```
校验（否则抛 `IllegalArgumentException`）：`min <= max`、`min <= default <= max`。

- `Attribute.setSyncable(boolean)` → 返回 `this`（链式）。置位后该属性同步到客户端；**默认 false**。F3/属性面板可见的属性基本都是 `setSyncable(true)`。
- `Attribute.setSentiment(Attribute.Sentiment)` → 返回 `this`。枚举 `POSITIVE`（默认）/`NEUTRAL`/`NEGATIVE`，只影响 tooltip 着色（`getMergedStyle`），不影响数值。
- `Attribute.sanitizeValue(double)`：基类原样返回；`RangedAttribute` 覆写为 NaN→min，其余 clamp 到 [min,max]。

**NeoForge 偏差（2 个属性）**：`generic.knockback_resistance` 与 `generic.movement_speed` 在 NeoForge 中**不是** `RangedAttribute`，而是 `net.neoforged.neoforge.common.PercentageAttribute`（继承 `RangedAttribute`，多一个 scaleFactor：前者默认 100，后者 1000），仅改变 tooltip 的百分比显示。**数值与范围与 vanilla 相同**，TSV 中的 default/range 因此仍然有效。

## 6. 注册表内存在、但没有任何原版实体显式使用的属性

**31 个属性全部都至少进入了一个原版实体的 `AttributeSupplier`**，不存在「注册了但任何实体都没有」的属性。但其中 **9 个从未被任何原版实体显式 `.add(...)` 过**，它们只通过 `LivingEntity.createLivingAttributes()` 这个所有生物共用的基座被加入，原版从不赋非默认值：

`generic.armor_toughness`、`generic.burning_time`、`generic.explosion_knockback_resistance`、`generic.gravity`、`generic.max_absorption`、`generic.movement_efficiency`、`generic.oxygen_bonus`、`generic.scale`、`generic.water_movement_efficiency`

→ **这 9 个正是「mod / 附魔 / 效果」最常去改的属性**（原版留了钩子但没用满）。

另有 4 个只被玩家显式使用（`player.mining_efficiency`、`player.sneaking_speed`、`player.submerged_mining_speed`、`player.sweeping_damage_ratio`，加 `player.block_break_speed`、`player.block_interaction_range`、`player.entity_interaction_range`），非玩家生物基本不会出现。

## 7. 其他发现 / 与常见预期的矛盾

- **`_desc` 描述键在 1.21.1 不存在**：en_us 与 zh_cn 中 `attribute.name.*_desc` 匹配数均为 **0**（`_desc` 是 1.21.2+ 才加的）。因此 TSV 的 `desc_en` / `desc_zh` 两列**全部为空**，这不是遗漏，而是本版本没有官方描述。
- **en_us 有 34 个 `attribute.name.*` 键，但只有 31 个属性**。多出的 3 个是**孤儿键**（Mojang 未删除的历史遗留）：`attribute.name.generic.block_interaction_range`、`attribute.name.generic.entity_interaction_range`（这两个已改挂到 `player.` 下）、`attribute.name.horse.jump_strength`（已并入 `generic.jump_strength`）。注册一个同名属性前不要被这些键误导。
- **1.21.1 的中文 lang 是一份「超集」文件**：本机该 zh_cn.json 有 8557 个键（en_us 只有 6881），其中同时含 `attribute.name.generic.max_health`（旧式）与 `attribute.name.max_health`（裸键），还含 1.21.1 **不存在**的属性键（`air_drag_modifier`、`bounciness`、`camera_distance`、`friction_modifier`、`nameplate_distance`、`below_name_distance`、`tempt_range`、`waypoint_receive_range/transmit_range` 等，属更晚版本）以及 `creaking`/`breeze` 相关文案。**结论：必须按精确键名查表，不能用「键集合」推断版本**；所需 31 个带前缀的键在该文件中齐全且取值正常。
- 本机 PowerShell 是 **5.1**，`Get-Content -Raw | ConvertFrom-Json` 会按 ANSI(GBK) 解码无 BOM 的 UTF-8 中文文件，导致静默取不到值/报错。**读中文 lang 必须用 `[System.IO.File]::ReadAllText($p, [Text.Encoding]::UTF8)`**。
- 未使用网络搜索；全部结论来自上述本地产物。
