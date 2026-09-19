# TKR 决策记录

> 格式：每个决策记录 **结论 / 理由 / 被否决的替代项 / 日期**。
> 不要静默推翻已有决策；要改就在此追加新条目并注明取代了哪一条。

---

## D-001 采用「方案 C：严格领域四层 + 公开 API」的目录结构

- **日期**：2026-09-19
- **结论**：代码分 `api` → `data` → `logic` → `client` 四层，依赖方向严格单向；各层约束写入 `package-info.java`。
- **理由**：
  1. 用户从三个方案中明确选择了 C。
  2. 项目强制依赖 Curios，而 Curios 集成横跨「注册、逻辑、客户端渲染」三处，需要结构性隔离，而非靠自觉。
  3. MC 模组最常见的生产事故是「逻辑层引用客户端类 → 服务端专用环境启动崩溃」。四层结构把这个错误从联机测试才能发现提前到**编译期**。
  4. 协议为 **LGPL-3.0-only**（**注**：D-001 立论时为 LGPL-2.1-only，经 D-016 短暂改为
     CC BY-NC-ND，再由 D-017 改为 LGPL-3.0-only；详见 D-017 的来回记录），
     `api/` 作为对外兼容性承诺有实际意义。
- **被否决的替代项**：
  - **方案 A（按技术角色扁平分层）**：上手最快、最贴近 NeoForge 教程，但每个新系统要同时改 6 个包，删功能要跨包删，系统一多 `content/` 会变成垃圾堆。
  - **方案 B（按功能纵切 + core）**：隔离性同样优秀且增量开发最友好，是当时的推荐项；用户未选。
- **刻意省略**：方案 C 原始提案中的 `platform/`（加载器适配）与 `internal/`（不对外承诺的实现细节）**被有意省略**，以保持「严格四层」的干净。需要有明确理由才补。

## D-002 完全移除 Patchouli 依赖

- **日期**：2026-09-19
- **结论**：Patchouli 从 `build.gradle`、`neoforge.mods.toml`、`libs/`、资源目录与 `run/` 残留中**彻底移除**。项目现在只有 Curios 一个外部依赖。
- **理由**：用户明确要求「去除帕秋莉手册的依赖」。
- **附带效果**：`neoforge.mods.toml` 中 `versionRange="[1.21.1-93-NEOFORGE,)"` 导致的 Mod 加载崩溃（`run/crash-reports/` 三次记录）随之消失。
- **被否决的替代项**：改为**可选**依赖（`type="optional"`，装了才生效）—— 用户未选此项，故当前为完全移除。若日后需要游戏内引导，这是可重新考虑的选项。
- **遗留影响**：`src/main/java/dev/tkr/client/gui/` 目前无用途。引导方案待定（见 PROGRESS 待办 #2）。

## D-003 不升级 NeoForge 版本（锁定 21.1.232）

- **日期**：2026-09-19（沿用项目原有决定并显式记录）
- **结论**：保持 `neo_version=21.1.232`。
- **理由**：`gradle.properties` 已注明，21.1.250 需要 `fancymodloader 4.0.44`，该版本只存在于本机**不可达**的 `maven.neoforged.net`。升级会直接破坏构建。
- **被否决的替代项**：升级到最新 21.1.x —— 无网络镜像支撑，构建会失败。

## D-004 版本敏感数据一律从本机真实产物提取，不依赖记忆或网络

- **日期**：2026-09-19
- **结论**：属性清单从 `neoforge-21.1.232-sources.jar` 的 `Attributes.java` + `minecraft_1.21.1_client.jar` 的官方语言文件提取；Curios API 签名从 `libs/curios-neoforge-9.5.1+1.21.1.jar` 用 `javap` 提取。
- **理由**：用户明确要求「防止受其他版本不同函数影响」。网上教程绝大多数是 1.20.1 / Curios 5.x 时代，**签名与当前 9.x 不兼容**，照抄必然编译失败或运行崩溃。
- **执行纪律**：参考文件必须标注其来源 artifact；artifact 变更时参考文件即失效，须重新提取。

## D-005 属性清单的「简介」列由本项目手工撰写，并与机器提取数据分列标注

- **日期**：2026-09-19
- **结论**：xlsx 的「简介」列是**手工撰写**的；同时保留 `desc_en`/`desc_zh` 两列作为**官方描述**位，以及 `来源` 列逐行标注出处。
- **理由**：1.21.1 的官方语言文件中 `attribute.name.*_desc` 键命中数为 **0**，即**官方根本不存在属性描述文本**。用户要求的「简单介绍」无官方来源，只能自撰；但不能因此让读者误以为它是 Mojang 原文。
- **被否决的替代项**：
  - 留空「简介」列 —— 不满足用户明确要求。
  - 把自撰文本填进 `desc_en`/`desc_zh` —— 会冒充官方数据，破坏参考文件的可信度。**坚决不做。**
- **数据流**：`mc-1.21.1-attributes.tsv`（机器生成，**禁止手改**）→ 叠加手工简介 → `mc-1.21.1-attributes.merged.tsv` → `docs/attributes-1.21.1.xlsx`。重跑 `python .dsh/tools/build_attributes_data.py` 即可再生。

## D-006 属性 id 以带前缀形式记录（纠正一次错误断言）

- **日期**：2026-09-19
- **结论**：1.21.1 属性 id 是 `generic.max_health` / `player.block_break_speed` / `zombie.spawn_reinforcements`，**不是**裸名。
- **理由**：本任务最初下达给提取代理的指令里断言「1.21.1 id 是 `max_health`，不是 `generic.max_health`」——该断言**是错的**。代理用三份本地产物证伪，随后由我独立复核确认（`Attributes.class` 常量池裸 `max_health` 命中 0 次）。
- **教训**：连「项目内部任务指令」本身都可能带错误前提。**版本敏感事实必须回到 artifact 验证**，不能因为是指令就采信。

## D-007 物品状态一律用数据组件，禁止新增自定义 NBT

- **日期**：2026-09-19
- **结论**：物品上**不使用 NBT**。任何物品状态走 1.21.1 的 57 个数据组件；确实没有对应组件时，才用官方的 `CUSTOM_DATA`（`custom_data`）逃生舱，且读写仍走组件 API。
- **理由**：用户明确要求「尽量在物品上不使用 NBT」。1.21 起物品状态本就是组件模型（`DataComponentMap` 挂在 `Item.Properties` 上），用 NBT 会与数据包、配方、其他 mod 的数据读取脱节。
- **执行纪律**：
  1. 先查 `references/item-components-1.21.1.md` §3 的对照表，优先用专用组件（如 `max_stack_size` / `damage` / `custom_name`）。
  2. 自定义状态注册在 `data/component/`，用 `DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, MOD_ID)` + `registerComponentType(...)`。
  3. 需要存档 → `.persistent(Codec)`；仅运行时状态 → 故意不调 `persistent`（transient），**这正是不用 NBT 存临时状态的正确手段**。
- **被否决的替代项**：
  - 退回 `ItemStack.getOrCreateTag()` / `CompoundTag` 手工读写 —— 旧教程写法，与 1.21 组件体系脱节。
  - 自定义一个 `"StackLimit"` 之类的键塞进 `custom_data` —— 已有 `max_stack_size` 组件，自造键会让数据包/配方/其他 mod 都读不到。

## D-008 参考数据文件的「未核验部分」必须显式标注，不得伪装成已验证

- **日期**：2026-09-19
- **结论**：参考文档中凡未能从 artifact 直接验证的内容（如需要编译才能确认的 API 组合、不在本地 jar 内的第三方类型），一律写明「未核验」及原因。
- **理由**：本项目参考数据的全部价值在于「锁定到具体 artifact、可按需复核」。一旦混入未标注的猜测，读者无法区分事实与推测，整份文件的可信度归零。用户的原话是「防止受其他版本不同函数影响」——猜测正是版本错配的来源。
- **执行方式**：`item-components-1.21.1.md` §4.3 即按此格式标注了三项未核验内容（DFU 的 `ExtraCodecs`、`DeferredItem` 用法、骨架未编译）。

## D-009 依赖 `versionRange` 一律维持实测通过的现值，不做文本层「优化」

- **日期**：2026-09-19
- **结论**：`neoforge.mods.toml` 中 `curios` 的 `versionRange="[9.5.1+1.21.1]"` **维持不动**。未来任何依赖区间改动，都必须实跑客户端/服务端确认后才算完成。
- **理由**：
  1. FML 的区间解析与 Maven **不同构**，Maven 结论不可移植。
  2. 本机**没有** FML 的版本比较类（属 launcher，不在 `neoforge-21.1.232-merged.jar` 内），**无法离线验证**。
  3. 该区间已有运行时证据：`run/logs/latest.log`（2026-09-19 21:03，FML 4.0.42）Mod List 正常且无 `Mod tkr requires curios` 失败。
- **被否决的替代项**：把区间「清理」成 `[9.5.1,)` 或 `[9.5,9.6)` —— Maven 语义下为 true，但 FML 下**未验证**，而 Patchouli 先例证明这种推理会出错。
- **附带更正**：本仓库 `PITFALLS.md` 原 P-001 曾建议「改用纯数字下界 `[93,)`」，经 maven-artifact 3.9.1 实测，该写法**反而判不满足**。原建议已作废并改写。**教训：不要用 Maven 复现来代替 FML 实测。**

## D-010 参考数据在写入记忆前必须独立复核关键主张

- **日期**：2026-09-19
- **结论**：子代理/外部产出的事实，在写入「权威记忆」前必须由我独立复核至少一条最关键的主张。
- **理由**：本轮两次复核都改变了结论：
  1. 属性 id 前缀 → **代理对、我错**（我的任务指令本身带错前提）。
  2. Curios 槽位注册机制 → **代理对**，我原先假设存在 Java 侧注册事件（实际只有 datapack JSON）。
  3. 代理关于 Maven 版本语义的断言 → **代理对**，推翻了我已写入 P-001 的建议。
  反之，若不复核直接采信，错误就会被固化进记忆并长期误导后续会话。
- **执行方式**：复核手段限于可从本机 artifact 直接验证的（`javap`、ZipFile 常量池、语言文件、Maven 实测）。**无法本地验证的主张必须标注为未验证，不得写成结论**（见 D-008）。

## D-011 力量伤害的 p 与 T 存放在**物品组件**上，不做全局配置

- **日期**：2026-09-19
- **结论**：`tkr:strength_damage` 组件携带 `bonus_ratio`(p) 与 `threshold`(T)；
  `StrengthConfig` 中**删除**全局 `THRESHOLD` / `BONUS_RATIO`。没有该组件的武器完全不受影响。
- **理由**：用户原始需求是「一个组件可以让武器赋予玩家**可自定义的**百分比数值」——
  「可自定义」意味着每件武器可以不同，必须随物品走，而不是全局常量。
- **附带收益**：参数跟物品存档、可被数据包修改、可在提示中显示、可在配方/命令中引用。
- **被否决的替代项**：把 p/T 放在 config 或静态常量里 —— 那样所有武器只能共用一套参数，
  与「可自定义」矛盾，也失去了按武器差异化的玩法空间。

## D-012 组件提示由组件自身实现（`TooltipProvider`），不写客户端代码

> **⚠️ 本条已被 D-015 推翻。** 实测证明 `TooltipProvider` 对**自定义组件无效**。
> 保留此条是为了记录「曾这样决定、以及为什么它不成立」，避免后来者重复尝试。

- **日期**：2026-09-19
- **结论**：`StrengthDamage implements TooltipProvider`，在 `addToTooltip` 中输出
  「伤害系数」「伤害额值」与派生信息；不新建客户端提示类。
- **理由**：
  1. `TooltipProvider` 是**通用**接口（在 `data` 层可用），不是客户端专属，因此不违反分层。
  2. 与原版组件（`Unbreakable`、`ItemLore` 等）做法一致，最内聚：知道数据的人负责显示数据。
  3. `logic` 层因此完全不必关心「怎么显示」，只消费组件的数值。
- **被否决的替代项**：在 `client/tooltip` 里写一个组件提示处理器 —— 会把某个组件的显示知识
  放到另一层，形成不必要的耦合；且需要额外的注册与 Dist 处理。
- **❌ 为什么失败**：原版 `ItemStack.getTooltipLines()` **不遍历所有组件**，
  而是硬编码调用 8 个固定组件（JUKEBOX_PLAYABLE / TRIM / STORED_ENCHANTMENTS /
  ENCHANTMENTS / DYED_COLOR / LORE / UNBREAKABLE），自定义组件的
  `addToTooltip` **永不触发**，且没有任何报错。详见 `PITFALLS.md` P-023。

## D-013 属性翻译键采用原版惯例 `attribute.name.<namespace>.<path>`

- **日期**：2026-09-19
- **结论**：`tkr:strength` 的翻译键是 **`attribute.name.tkr.strength`**（不是 `attribute.tkr.strength`）。
- **理由**：原版属性键形式为 `attribute.name.generic.armor` / `attribute.name.player.block_break_speed`
  （已从 `Attributes.java` 核验），即 `attribute.name.<路径含命名空间>`。
  用户要求「使用模组命名空间」，键中的 `tkr` 即命名空间段。
- **验证**：启动自检打印 `翻译键=attribute.name.tkr.strength`，与语言文件键一致。
- **注意**：若键写错，界面会直接显示原始键名（如 `attribute.name.tkr.strength`）而非「力量」，
  这是最容易发现的症状。

## D-014 属性上限用**反射改写字段**放宽，不用 mixin

- **日期**：2026-09-19
- **结论**：`AttributeRangeExpander` 通过反射把 `RangedAttribute.maxValue` 改写为
  `StrengthConfig.ATTRIBUTE_MAX`（默认 `1.0e20`），在 `FMLCommonSetupEvent` 中遍历
  `BuiltInRegistries.ATTRIBUTE` 全量处理。**只改上限，不动下限**。
- **为什么必须这么改**：`RangedAttribute` 的 `sanitizeValue()` 是
  `Mth.clamp(v, minValue, maxValue)`，而 `AttributeInstance.getValue()` 会调用它 ——
  **任何超上限的赋值都会被钳制回去**。这两个字段是 `private final`，
  且 NeoForge 21.1.232 **没有**提供改属性范围的 Hook（已核验 jar 内无相关 neoforged 类）。
- **可行性已实测**：MC 类位于**未命名模块**（`RangedAttribute.class.getModule().isNamed() == false`），
  Java 21 下对未命名模块的类 `setAccessible(true)` 后写 final 字段是允许的，
  **不需要 `--add-opens`**。实测 `sanitizeValue(1e300)` 返回 `1e300`。
- **被否决的替代项**：
  - **Mixin**：语义上更「正统」，但本项目**未配置 mixin 基础设施**
    （无 `META-INF/*.mixin.json`、无 refmap、无 mixin gradle 插件），
    且 mixin 失败会在类加载期直接崩溃，风险高于反射（反射失败只打警告并保持原上限）。
  - 保持原版上限 + 只在 `logic` 里自行累加：无法绕过 `sanitizeValue`，属性值仍被钳制。
- **为什么不取 `Double.MAX_VALUE`**：属性修饰符有加法与乘法，两次相乘即溢出为 `Infinity`，
  生命值等变为无限，破坏存档与实体逻辑。故取**极大且有限**的值。
- **参照现有模组**：AttributeFix 采用同一思路（默认把所有属性 max 设为 `1000000`，
  且**只改 max 不改 min**），见 <https://www.mcmod.cn/class/2264.html>。
  本项目取 `1e20`，比它更激进。
  **未能核实**：AttributeFix 究竟是反射还是 mixin（`raw.githubusercontent.com` 在本机
  DNS 不可达），故此处只称「思路一致」，不称「实现一致」。
- **已知代价（须知）**：这是**全局**放宽，会影响原版与其他模组对同一属性的使用。
  可用 `StrengthConfig.EXPAND_VANILLA_ATTRIBUTE_RANGES = false` 关闭
  （只保留 TKR 自己属性的放宽）。


## D-015 自定义组件提示改用表现层 `ItemTooltipEvent`（推翻 D-012）

- **日期**：2026-09-19
- **结论**：组件提示**不能**靠实现 `TooltipProvider` 自动生效。
  改由 `client/tooltip/StrengthTooltip` 订阅 `ItemTooltipEvent`，
  主动读取组件并追加提示行；`StrengthDamage` 回归**纯数据**（不再实现该接口）。
- **理由**：原版 `ItemStack.getTooltipLines()` 只对**硬编码的 8 个组件**调用
  `addToTooltip`，自定义组件不在其中，其实现**永远不会被调用**且**不报错**。
  NeoForge 21.1.232 也没有提供注册自定义组件提示的钩子（已核验全 jar）。
- **被否决的替代项**：
  - 保留 `TooltipProvider` 实现「以防万一」 —— 它永远不会被调用，留着会误导后来的会话
    以为提示已经由组件负责，反而增加排查成本。**故明确移除并写下原因。**
  - 用 mixin 往 `getTooltipLines` 里插逻辑 —— 本项目无 mixin 基础设施，且风险远高于收益。
- **附带收益**：反而更符合四层结构 —— `data` 层只提供数据，显示归 `client` 层。
- **验证**：用户实测提示已正常显示（`伤害系数：700%` / `伤害额值：9` /
  `力量达到 1.29 时取得完整线性加成`），并有一次性诊断日志佐证。
- **教训**：这条错误**编译期、启动期都发现不了**，只有真正悬停物品才会暴露。
## D-016 许可证由 LGPL-2.1-only 改为 CC BY-NC-ND 4.0

- **日期**：2026-09-19
- **结论**：项目协议改为 **CC BY-NC-ND 4.0**。同步修改的文件：
  `gradle.properties: mod_license`、`META-INF/neoforge.mods.toml: license`、
  `README.md`、`LICENSE`、`api/package-info.java` 的协议说明。
- **理由**：用户明确要求改为「可依赖、不允许修改、不允许商用」。
  CC BY-NC-ND 的 NoDerivatives + NonCommercial 正好对应这两个诉求。
- **为什么改协议前要先纠正一处错误说法**：原 `README.md` 写「LGPL-2.1-only（可依赖，
  **不允许修改代码**）」——**这句话与 LGPL 的实际条款不符**。LGPL 明确允许他人修改并重新分发
  （只要求同样开源）。也就是说项目原先的**意图**与所选**许可证**不一致；改到 CC BY-NC-ND 后
  意图与条款才真正对齐。
- **⚠️ 已知代价（用户已知悉）**：
  1. **CC 官方不建议把 CC 许可用于软件** —— CC 自己的 FAQ 指出 CC 许可不含专利授权，
     且不适合软件；软件应优先用软件专用许可证。
  2. **可能失去 CurseForge 收益计划资格** —— CurseForge 的 Reward Program 属于
     商业化使用，与 NonCommercial 条款冲突。
  3. **NoDerivatives 与模组生态摩擦** —— 整合包作者无法制作修改版或移植版；
     只能原样分发（已在 LICENSE 里显式写明整合包分发是允许的）。
  4. **SPDX 标识符**：`CC-BY-NC-ND-4.0`。CurseForge 许可证下拉里若不直接提供，
     需选择 Custom 并粘贴 `LICENSE` 内容。
- **被否决的替代项**：若用户想要的是「可依赖、不可修改、但允许商用」，CC BY-ND 4.0
  更合适（去掉 NC）；若想要「软件专用 + 禁止闭源再分发」，PolyForm 系列更贴切。
  这两项已在回复中提示，用户未采纳。
## D-017 许可证最终定为 LGPL-3.0-only（取代 D-016）

- **日期**：2026-09-19
- **结论**：协议定为 **LGPL-3.0-only**（SPDX: `LGPL-3.0-only`），取代 D-016 的
  CC BY-NC-ND 4.0。同步修改 `gradle.properties`、`neoforge.mods.toml`、`README.md`、
  `LICENSE`、`api/package-info.java`。
- **理由**：用户放弃 CC BY-NC-ND 方案，直接指定 LGPL-3.0。
- **许可证变更史（重要，勿再混淆）**：
  | 顺序 | 协议 | 决策 |
  |---|---|---|
  | 1 | LGPL-2.1-only | 项目初始（D-001 立论时的状态） |
  | 2 | CC BY-NC-ND 4.0 | D-016（用户要求「不许改、不许商用」） |
  | 3 | **LGPL-3.0-only** | **D-017（当前）** |
- **订正一处长期存在的错误说法**：变更前的 `README.md` 写「LGPL-2.1-only（可依赖，
  **不允许修改代码**）」——**在 LGPL 下这不成立**（LGPL 明确允许修改后以 LGPL/GPL 重新分发）。
  当前 README 已改为「可依赖、含闭源项目；修改后重新分发须同样开源」，与实际条款一致。
- **LGPL-3.0 的一个结构要点**：它**不是独立文档**，前言写明
  *"incorporates the terms and conditions of version 3 of the GNU General Public License,
  supplemented by the additional permissions listed below"* —— 必须与 **GPL-3.0** 一起构成完整条款。
  因此 `LICENSE` 里：
  1. 完整收录可获取的 LGPL-3.0 文本（来自 `https://www.gnu.org/licenses/lgpl-3.0.txt`）；
  2. 对 GPL-3.0 **采用引用方式**（给出 `https://www.gnu.org/licenses/gpl-3.0.txt` 链接），
     以免在仓库里存放一份会过期的第三方文档副本。这一处理与 D-016 的引用式做法一致。
- **为什么这个选择比 CC BY-NC-ND 更契合软件**：LGPL 是软件专用协议，含专利条款，
  且「可作为库被闭源项目依赖」这一模式正是为模组 API 场景设计的。
  CC 官方本身也不建议把 CC 许可用于软件（见 D-016 的记录）。
- **用户已知悉的差异**：LGPL **允许**他人修改后开源再分发，也**允许**商用
  （与 CC BY-NC-ND 的 ND + NC 相反）。若日后仍要禁止修改/商用，需回到 D-016 的方案
  或改用自定义协议。
## D-018 移除 Curios API 依赖，项目成为零外部依赖

- **日期**：2026-09-19
- **结论**：Curios 从 `build.gradle`、`neoforge.mods.toml`、`libs/` 与文档中移除。
  项目现在**只需 Minecraft 1.21.1 + NeoForge**。
- **理由**：用户明确要求去除。同时这也消除了唯一的编译期外部依赖 ——
  之前 `libs/curios-neoforge-9.5.1+1.21.1.jar` 是**唯一**的外部 jar。
- **改动清单**：
  | 文件 | 改动 |
  |---|---|
  | `build.gradle` | 删除 `implementation files('libs/curios-...jar')`，`dependencies` 留空并加注释 |
  | `neoforge.mods.toml` | 删除 `[[dependencies.tkr]] modId="curios"` 块；description 改为 "Requires no other mods." |
  | `libs/` | 删除 Curios jar（目录现为空） |
  | `README.md` | 依赖表 → 「无外部模组依赖」 |
  | `CHANGELOG.md` | 记录本次移除 |
  | `api/package-info.java`、`logic/package-info.java`、`MinStrengthGate.java` | 去掉 Curios 相关表述 |
  | `docs/curseforge-description.md` | Requirements 去掉 Curios；Related Projects 说明无需填写 |
  | `SKILL.md` | 依赖表、架构表、陷阱清单、参考表同步 |
  | `references/curios-9.5.1-api.md` | **保留但标记为历史资料**（依赖已移除，jar 已删） |
- **实跑验证**：`runClient` 正常启动，Mod List 只剩 `damage_number` / `minecraft` / `neoforge` / `tkr`，
  资源包列表无 `mod/curios`，无 `Mod tkr requires curios` 报错，属性自检与上限放宽均正常。
- **副作用（须知）**：
  1. **护具穿戴门槛**彻底没有实现路径了 —— 原先设想的 `CurioCanEquipEvent` 随依赖移除而不可用。
     原版装备事件又不可取消。若日后要做该门槛，需要另找可拦截的挂载点。
  2. `logic/curios/` 空包目录已不再是规划中的模块（`logic/package-info.java` 的职责列表已改为
     `strength`）。
  3. `run/config/` 下仍有 Curios 生成的配置文件残留（`curios-client.toml` 等），
     它们不再被任何模组读取，可安全删除。
- **教训（复用 P-001 的纪律）**：**依赖变更必须实跑验证**。P-001 正是「区间写错导致加载失败」
  的先例，因此本次移除后仍然启动了一次客户端确认，而不是只看 `build` 成功。