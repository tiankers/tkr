# 力量属性（tkr:strength）最终规格

> **状态**：已定稿，**已实跑验证**（客户端启动无错误，属性注册确认）。
> 本文件是数值语义的唯一权威来源 —— 该规格经过多轮澄清，极易被后来的会话误解，**改动前必读**。

## 0. 实跑验证证据（2026-09-19）

`runClient` 输出中的启动自检行（由 `StrengthStartupCheck` 打印）：

```
[TKR] 力量属性自检: id=tkr:strength 默认值=0.0 范围=[0.0, 1024.0] 可同步=true
```

- 客户端启动全程**无 ERROR / 异常**，Mod List 正确列出 `TKR Lib 0.1.0 (tkr)`。
- 注：该证据取自仍带 Curios 依赖的版本；Curios 已于 2026-09-19 移除，之后需重新实跑确认。

**同时修掉一个只有实跑才能发现的错误**：`LivingIncomingDamageEvent` 属游戏总线，
最初被注册到模组总线，导致构造器抛 `IModBusEvent` 校验异常、模组加载失败。
详见 `PITFALLS.md` P-019。

---

## 1. 一句话概括

模组为**全体生物**提供 `tkr:strength` 力量属性；物品可用组件声明**力量下限**；
武器命中时按力量产生**额外伤害增量 D**，并消耗武器耐久。

---

## 2. 伤害曲线（最终版，唯一形态）

**横轴是力量 S，纵轴是该组件贡献的伤害增量 D**（注意：早期讨论中一度误把横轴当成伤害值）。

```
线性段   D(S) = S × p                          当 S ≤ T/p
曲线段   D(S) = 2T − T·e^(−p/T · (S − T/p))     当 S >  T/p
```

| 参数 | 含义 | 默认值 |
|---|---|---|
| `p` | 百分比：线性段**每点力量贡献的伤害** | `0.5` |
| `T` | 伤害额值（threshold） | `10.0` |
| `T/p` | **弯折点力量** | `20.0` |

### 已验证的数值（p=0.5, T=10）

| 力量 S | 0 | 5 | 10 | **20** | 40 | 100 | ∞ |
|---|---|---|---|---|---|---|---|
| 增量 D | 0 | 2.5 | 5 | **10** | 16.32 | 19.82 | **20** |

### 三条硬性质

1. **弯折点 C¹ 连续** —— 弯折处左右斜率都等于 `p`（数值验证：0.500000 / 0.499999），无折角。
2. **渐近线 = 2T** —— 「力量无穷大时趋近额值 2 倍」严格成立。
3. **D(0) = 0** —— 力量为 0 时该组件不提供任何加成（**不是**武器基础伤害）。

> 用户曾写「力量 40 → 15」，本模型给出 **16.32**。这是用连续性反解参数后的唯一结果，
> 用户已确认接受该差异（原 15 为手估）。

---

## 3. 耐久规则

比较对象是**伤害增量 D**（不是力量，也不是武器基础伤害）：

```
D < T  → 扣 1 点耐久
D ≥ T  → 扣 2 点耐久
```

由于 `D = T` 恰好发生在弯折点，「进入曲线段」与「耐久翻倍」在同一力量值触发。

**已被否决的早期方案**（不要再实现）：
- 「耐久 -= 伤害/额值」连续扣减 + 余数累积组件 —— 已删除 `tkr:durability_remainder`。
- 「按力量与额值比较」—— 用户明确纠正为按伤害。

---

## 4. 力量下限组件

- 组件 id：`tkr:min_strength`，类型 `double`，范围 `[0, 1024]`，**落盘 + 同步**。
- 组件缺失或 `≤ 0` 视为无要求。
- **武器**：力量不足时**攻击被拦截**（`LivingIncomingDamageEvent.setCanceled(true)`）——
  可以拿在手里，但挥不动。
- **护具**：**尚未实现**。原版 `LivingEquipmentChangeEvent` 的 javadoc 明确写着
  *"This event is not ICancellableEvent"*，**无法**用它阻止穿戴。
  注：Curios 依赖已于 2026-09-19 移除，因此不再有「用 Curios 的 `CurioCanEquipEvent` 实现」这条路径。

## 4b. 力量伤害组件（p 与 T 的归属）

- 组件 id：`tkr:strength_damage`，类型为 `record StrengthDamage(double bonusRatio, double threshold)`，
  **落盘 + 同步**。
- **p 与 T 是每件武器各自的数据**，不是全局配置 —— 用户要求「可以让武器赋予玩家可自定义的
  百分比与额值」。因此 `StrengthConfig` 中<b>不再有</b>全局 `THRESHOLD` / `BONUS_RATIO`。
- **没有该组件的武器完全不受本机制影响**，即模组内容外的原版武器行为不变。
- 序列化字段名为 `bonus_ratio` 与 `threshold`（下划线）。
- **提示由组件自己负责**：`StrengthDamage implements TooltipProvider`，
  在 `addToTooltip` 中输出三行（蓝字）：
  - `伤害系数：50%`
  - `伤害额值：10`
  - `力量达到 20 时取得完整线性加成`（派生信息，暗灰）
  这样做的好处是**不需要任何客户端代码**，且 `logic` 层不必关心显示。
- 派生方法 `bendStrength()`（= `T/p`）与 `maxBonus()`（= `2T`）留在 record 内：
  它们是纯数据推导，而 `data` 层<b>不允许</b>依赖 `logic` 层，不能调用 `StrengthMath`。

## 4c. 测试物品

- `tkr:strength_blade`（力量之刃），定义在 `data/item/TkrItems.java`，
  默认参数 `p=0.5, T=10`，**不带** `min_strength`（便于单独验证曲线）。
- 贴图由 `.dsh/tools/make_textures.py` 生成（16x16 RGBA PNG，纯标准库，
  可复现可微调），模型 `assets/tkr/models/item/strength_blade.json` 继承 `item/handheld`。
- 用途：在创造模式中验证**组件提示是否显示伤害系数与伤害额值**。

---

## 5. 挂载点（均已从源码核验）

| 用途 | 事件 / API | 关键事实 |
|---|---|---|
| 注入力量到全体生物 | `EntityAttributeModificationEvent`（Mod bus） | `getTypes()` 已预过滤 `DefaultAttributes::hasSupplier`；用 `add(type, attribute, value)` |
| 改伤害 + 拦攻击 | `LivingIncomingDamageEvent` | 在护甲/附魔减免**之前**；武器解析见下 |
| 属性注册 | `DeferredRegister.create(Registries.ATTRIBUTE, MOD_ID)` | `RangedAttribute(key, default, min, max).setSyncable(true)` |
| 组件注册 | `DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, MOD_ID)` | 用 `registerComponentType(...)`，避免泛型推断问题 |

### 5b. 武器解析：近战 + 远程（已实现）

**不能只用 `DamageSource.getWeaponItem()`**。其实现是：
```java
return this.directEntity != null ? this.directEntity.getWeaponItem() : null;
```
它问的是**直接伤害来源实体**：

| 情形 | directEntity | 该方法返回 |
|---|---|---|
| 近战 | 攻击者 | ✅ 手持武器 |
| 弓弩射出的箭 | `AbstractArrow` | ✅ 该箭记录的弓/弩 |
| 雪球/投掷三叉戟等 | 普通投射物 | ❌ 空 → 远程会被整体漏掉 |

因此 `StrengthDamageHandler.resolveWeapon` 分两种情形：

1. **投射物伤害**（`source.is(DamageTypeTags.IS_PROJECTILE)`）：
   优先 `directEntity.getWeaponItem()`；为空则**回退到攻击者主手物品**
   （投掷时主手拿的正是那件投掷物，所以主手就是正确的「武器」）。
2. **其余（近战等）**：`source.getWeaponItem()`。

**已验证的 API**：`Projectile.getOwner()`、`AbstractArrow.getWeaponItem()`、
`Entity.getWeaponItem()`、`DamageTypeTags.IS_PROJECTILE`。
**已删除** `StrengthConfig.MELEE_ONLY`（远程无条件支持后成为死配置）。

---

## 6. 代码位置（四层结构）

| 文件 | 层 | 职责 |
|---|---|---|
| `data/attribute/TkrAttributes.java` | 定义 | 注册 `tkr:strength` |
| `data/component/TkrComponents.java` | 定义 | 注册 `tkr:min_strength` |
| `logic/strength/StrengthMath.java` | 逻辑 | **纯数学**，无 MC 依赖，可单测 |
| `logic/strength/StrengthConfig.java` | 逻辑 | 数值集中管理（`p=0.5, T=10`） |
| `logic/strength/StrengthAttributes.java` | 逻辑 | 注入全体生物 |
| `logic/strength/StrengthDamageHandler.java` | 逻辑 | 伤害结算 + 攻击拦截 + 耐久 |
| `logic/strength/MinStrengthGate.java` | 逻辑 | 下限判定 |
| `client/tooltip/StrengthTooltip.java` | 表现 | 「需要力量：X」提示 |

---

## 7. 属性上限放宽（已实跑验证）

**问题**：`RangedAttribute.sanitizeValue()` 是 `Mth.clamp(v, min, max)`，而
`AttributeInstance.getValue()` 会调用它 —— **超出上限的赋值一律被钳制回去**。
两个字段是 `private final`，NeoForge 21.1.232 **没有**改范围的 Hook。

**做法**：`AttributeRangeExpander` 反射改写 `maxValue`（只改上限，下限保持原值），
在 `FMLCommonSetupEvent` 中遍历 `BuiltInRegistries.ATTRIBUTE` 全量处理。

| 项 | 值 |
|---|---|
| 上限目标 | `StrengthConfig.ATTRIBUTE_MAX = 1.0e20` |
| 是否含原版属性 | 是（`EXPAND_VANILLA_ATTRIBUTE_RANGES = true`） |
| 是否改下限 | **否**（负下限无意义且污染语义） |

**实跑验证输出**：
```
[TKR] 属性上限放宽: 已处理 34 个属性，改写失败 0 个，目标上限=1.0E20
[TKR] 力量属性自检: id=tkr:strength 范围=[0.0, 1.0E20]
[TKR]   钳制测试 minecraft:generic.max_health: sanitizeValue(1.0E20) = 1.0E20  ✔ 未被钳制
[TKR]   钳制测试 tkr:strength: sanitizeValue(1.0E20) = 1.0E20  ✔ 未被钳制
```

**关键技术点**：字段查找必须**沿继承链**（`getDeclaredField` 只看本类，
而 NeoForge 的 `PercentageAttribute` 把字段留在父类 `RangedAttribute` 上）。
不这么做会漏掉 `generic.knockback_resistance`、`generic.movement_speed`、
`neoforge.swim_speed` 三个，且**失败是静默的**（只打警告）。见 `PITFALLS.md` P-021。

**参照**：AttributeFix 思路一致（默认 max=1000000，只改 max）。见 D-014。

---

## 7b. `/tkr` 系列指令

定义在 `logic/strength/TkrCommands.java`，用 `@EventBusSubscriber(bus = GAME)` 挂
`RegisterCommandsEvent`（该事件不实现 `IModBusEvent`，不能注册到模组总线）。

```
/tkr strength_damage <p> [T]          给主手物品赋予力量伤害组件（省略 T 时用默认 10）
/tkr strength_damage remove           移除该组件
/tkr min_strength <value>             给主手物品设置力量下限
/tkr min_strength remove              移除该组件
/tkrattribute <对象> <属性名> <数值>    设置实体的 TKR 属性基础值（需权限等级 2）
/tkrlist                              列出所有 TKR 指令
```

**设计要点**：
- `/tkr*` 作用于**主手物品**；`/tkrattribute` 用 `EntityArgument.entities()` 支持 `@e` 等选择器。
- **T 可省略**：避免「必须同时手输两个数才能试效果」，默认值取
  `StrengthConfig.DEFAULT_STRENGTH_DAMAGE_THRESHOLD = 10.0`（与测试物品一致）。
- **改写物品后必须同步容器**：`player.inventoryMenu.broadcastChanges()`。
  不做这一步会出现「指令提示成功但物品提示没变」。
- **`/tkrattribute` 只接受 `tkr:` 命名空间**的属性；别的命名空间会被拒绝并提示
  （原版属性请用原版 `/attribute`）。属性解析用 `TkrAttributes.holderById(...)`
  返回**注册时的那个 Holder** —— 不能用 `Holder.direct(attribute)`，
  因为属性实例按注册表 Holder 存储，身份不同会查不到。
- **`/tkrattribute` 的属性名有 Tab 补全**：`ResourceLocationArgument.id()` 自带的补全
  **不限定注册表**（用户反馈「按不到」），因此显式挂了
  `SharedSuggestionProvider.suggestResource(TkrAttributes.tkrAttributeIds(), builder)`。
  候选从注册表动态取（按 `tkr` 命名空间过滤），新增属性会自动出现。
  注意 1.21.1 **没有** `net.minecraft.commands.synchronization.SuggestionProvider` 类，
  用的是 Brigadier 的 `com.mojang.brigadier.suggestion.SuggestionProvider`。见 P-025。
- **`/tkrlist` 从指令树动态生成**（`dispatcher.getSmartUsage`），不写死，因此不会与实现脱节。
  但有两处必须处理，**均已用离线 Brigadier 复现验证**：
  1. `getSmartUsage` 返回**相对用法**，必须补根名前缀，否则打印成 `/strength_damage ...`；
  2. 它**只对子节点生成用法**，像 `tkrlist` 这种无子节点但自身可执行的指令返回**空 map**，
     会整个漏掉 —— 需在空 map 时回退为「输出指令自身」。
- 反馈文本走 `commands.tkr*` 翻译键，中英双语齐备。

**验证状态**：
- 编译通过。
- **启动自检已确认三条指令全部注册**（`ServerStartedEvent` 时读指令树）：
  ```
  [TKR] 指令自检: /tkr 已注册，子节点=[strength_damage, min_strength]
  [TKR] 指令自检: /tkrattribute 已注册，子节点=[targets]
  [TKR] 指令自检: /tkrlist 已注册（无子节点）
  ```
- `/tkr strength_damage` 与 `/tkrlist` **已由用户在游戏内实际执行**。
- `/tkrattribute` **尚未人工实测**（注册已确认，但未敲过）。

---

## 8. 已知未完成项

1. **护具穿戴门槛**未实现 —— 受原版 `LivingEquipmentChangeEvent` 不可取消所限（见 §4）。
   Curios 依赖已移除，实现该门槛需要另有可拦截的挂载点。
2. **`tkr:min_strength` 没有任何物品在使用** —— 门槛逻辑（`MinStrengthGate` + 攻击拦截）
   已实现，但目前**跑不到**。需要给某件物品加上该组件才能验证。
3. **未接入真正的 config 文件** —— `StrengthConfig` 仍是常量。改动数值需重新编译，
   且无法在打包后调整（AttributeFix 是 JSON 配置，本项目尚未对齐）。
4. **无自动化测试** —— `StrengthMath` 是纯数学、无 MC 依赖，**最适合单测但尚未写**
   （`src/test` 不存在；`build.gradle` 的 `compileTestJava` 报 NO-SOURCE）。
5. **`docs/idea.md` 仍为空** —— 玩法总规格未确定，因此除测试物品外无任何内容物品。

## 9. 数学与实现陷阱（本轮踩过）

- **`java.lang.Math` 没有 `acosh` / `asinh` / `atanh`** —— 只有 `sinh/cosh/tanh`。
  需要时自行实现：`acosh(x) = ln(x + sqrt(x² − 1))`。本轮因此编译失败过一次。
- **反射改属性字段必须沿继承链查** —— 否则漏掉 NeoForge 的 `PercentageAttribute`
  三个属性，且失败静默。见 `PITFALLS.md` P-021。
- **属性下限不要改成负值** —— 只放宽上限。见 `PITFALLS.md` P-022。
