---
name: tkr-context
description: TKR mod 项目的强制上下文基线：版本锁定、四层架构约束、构建命令、版本陷阱，以及跨会话记忆协议。任何涉及本项目的代码、构建、依赖、结构或数据的工作开始前必须先加载本技能，并按协议读写 .dsh/memory/PROGRESS.md，防止上下文丢失后重复决策或引入版本错配代码。
whenToUse: 当前工作目录是 C:\Users\26461\Desktop\tkr，或任务提到 TKR / tkr 这个 Minecraft 1.21.1 NeoForge 模组；以及在需要写任何 Java 代码、改 build.gradle 或 neoforge.mods.toml、调用 Curios API、查询原版属性/注册表时。
---

# TKR 项目上下文基线

本技能是**项目记忆的入口**。上下文会丢失，磁盘不会。任何工作开始前先执行 §0 的协议。

---

## §0 记忆协议（强制）

### 开始任何工作前，按顺序执行

1. 读 `.dsh/memory/PROGRESS.md` —— 当前进度、已决策项、待办、已知阻塞。**这是权威来源，优先于你对该项目的任何记忆或猜测。**
2. 读 `.dsh/memory/DECISIONS.md` —— 架构与依赖决策及其理由。不要重新推翻已决策项，除非用户明确要求。
3. 读 `.dsh/memory/PITFALLS.md` —— 已踩过的坑（版本陷阱、构建失败、崩溃根因）。
4. 若任务涉及原版属性/注册表 → 读 `references/mc-1.21.1-attributes.tsv`（**先读 `_raw-attributes-notes.md` 确认口径**）。
5. 若任务涉及饰品/槽位/Curios → 读 `references/curios-9.5.1-api.md`。**禁止凭记忆写 Curios 代码**，该文件里的签名是从本项目实际链接的 jar 用 `javap` 提取的。
6. 确认 `PROGRESS.md` 顶部记录的「当前阶段」与本次任务是否一致；不一致则先向用户确认，不要擅自推进。

### 完成任何实质性工作后，必须更新记忆

- **每次**改变了代码结构、新增/删除文件、改了依赖、跑了构建或客户端、修了 bug、或用户做了新决策 → 在同一轮内更新 `.dsh/memory/PROGRESS.md`（追加或修改对应条目，不要只追加不复核）。
- 新增了一条「以后不能再犯」的教训 → 写入 `PITFALLS.md`。
- 用户做了一个有取舍的选择（方案 A/B/C、依赖去留、命名规范） → 写入 `DECISIONS.md`，含**理由**和被否决的替代项。
- 更新时同步改文档头部的「最后更新」日期与实际状态，不要留下自相矛盾的旧条目。

### 记忆文件的写法要求

- 用**事实与状态**，不要写叙事或过程感想。可核对、可执行。
- 每条待办要有明确完成判据。
- 过期的条目要删除或标记 `已废弃（原因）`，不要堆在原地。

---

## §1 项目身份与版本锁定

| 项 | 值 | 来源 |
|---|---|---|
| 项目名 / modId | TKR / `tkr` | `gradle.properties` |
| 作者 / 协议 | tiankers / LGPL-2.1-only | `README.md` |
| Minecraft | **1.21.1**（只支持这一个版本） | `gradle.properties: minecraft_version` |
| NeoForge | **21.1.232** | `gradle.properties: neo_version` |
| Java | **21**（`options.release = 21`） | `build.gradle` |
| Gradle | 9.2.1（wrapper，华为云镜像分发） | `gradle/wrapper/gradle-wrapper.properties` |
| 构建插件 | ModDevGradle `net.neoforged.moddev` 2.0.141 | `build.gradle` |
| Maven 组 / 版本 | `dev.tkr` / `0.1.0` | `gradle.properties` |
| 外部依赖 | **仅 Curios `9.5.1+1.21.1`**（`libs/` 本地 jar） | `build.gradle` |

> **版本纪律**：本项目是**单版本锁定**的。任何网上抄来的代码，若来自 1.20.x、1.20.1、1.21.4、1.21.5+ 或 Forge（非 NeoForge），都必须先核对 §4 的陷阱清单再落地。NeoForge 21.1.x / MC 1.21.1 与相邻版本之间**存在真实的 API 断裂**，尤其：数据组件、注册表、Curios、渲染管线。

**为什么锁 21.1.232 而不是最新的 21.1.250**：`gradle.properties` 里已注明，21.1.250 需要 `fancymodloader 4.0.44`，它只存在于本机不可达的 `maven.neoforged.net`。**不要在没有网络镜像的情况下升级 NeoForge 版本。**

---

## §2 构建与验证命令

```powershell
.\gradlew.bat build --offline --console=plain   # 编译 + 打包，产出 build/libs/tkr-0.1.0.jar
.\gradlew.bat runClient                          # 启动客户端
.\gradlew.bat runServer                          # 启动服务端（--nogui）
.\gradlew.bat runGameTestServer                  # GameTest，命名空间 tkr
```

- **本机有 Gradle 离线镜像**：构建日志首行会出现 `[offline-mirror] ACTIVE gradleUserHome=C:\Users\26461\.gradle`，并 pin `NeoFormRuntime 2.0.18`。由 `~/.gradle` 下的 init script 提供，**不在本项目内**。
- 因此本机构建**不依赖网络**；加 `--offline` 可确定性地避免网络等待。不要无故去掉。
- **判据**：构建成功的标志是 `BUILD SUCCESSFUL`。退出码 1 若伴随 `NativeCommandError`（来自 PowerShell 把 stderr 当错误处理）而输出含 `BUILD SUCCESSFUL`，属于假失败 —— 看输出而非退出码。
- 源码/资源改动后必须至少跑一次 `build --offline`，并检查 `build/resources/main/META-INF/neoforge.mods.toml` 中 `${version}` 是否已正确展开。

---

## §3 架构约束（方案 C：严格领域四层 + 公开 API）

依赖方向**严格单向**，`api` ← `data` ← `logic` ← `client`：

| 层 | 路径 | 可依赖 | 绝对禁止 |
|---|---|---|---|
| 契约层 | `dev/tkr/api/` | JDK、原版/NeoForge 公共 API、Curios 公共 API | 另外三层 |
| 定义层 | `dev/tkr/data/` | `api` | `client`；尽量避免 `logic` |
| 逻辑层 | `dev/tkr/logic/` | `api`、`data` | **任何 `client` 或 `net.minecraft.client.*`** |
| 表现层 | `dev/tkr/client/` | 前三层 | 无（依赖链末端） |

各层的完整说明见对应目录下的 `package-info.java`（**那是权威，不是本表**）。

**这条约束的意义**：`logic` 不得引用客户端类，把「服务端专用环境因客户端类泄漏而崩溃」从联机测试才能发现，提前到编译期。写 `logic` 代码时，需要表现层配合一律走 `logic/event` 发布事件或 `logic/network`，让 `client` 主动订阅。

**尚未实现的层次**：方案 C 原始提案里的 `platform/`（加载器适配）与 `internal/`（不对外承诺的实现细节）被有意省略。除非有明确需要，不要自行添加。

---

## §4 版本陷阱清单（摘要）

完整清单与根因见 `.dsh/memory/PITFALLS.md`。以下是最容易犯的：

1. **属性 id 在 1.21.1 带前缀，不是裸名。** 真实注册名是 `generic.max_health`、`player.block_break_speed`、`zombie.spawn_reinforcements`（编译产物 `Attributes.class` 常量池已核验）；翻译键是 `attribute.name.generic.max_health`。**扁平化（去掉 `generic.`）是更晚的版本才发生的** —— 网上大量资料按新版本写法给出裸名，照抄会拿到错误的注册名。
2. **Curios 是 9.x，不是 1.20.1 时代的 5.x。** 物品接口、槽位注册、属性修饰符 API 都已变更。**一律以 `references/curios-9.5.1-api.md` 为准。**
3. **依赖版本区间不可推理，只能实跑验证。** FML 的 `versionRange` 解析与 Maven **不同构**，本机又**没有** FML 的比较器（属 launcher，不在 merged jar 内），所以离线复现不了。实测证据：Maven 下 `[1.21.1-93-NEOFORGE,)` 判**满足** `1.21.1-93-NEOFORGE`，而纯数字下界 `[93,)` 判**不满足** —— 本仓库曾据此给错建议（见 `PITFALLS.md` P-001 修正记录）。**有运行时证据通过的区间不要动**；当前 `curios` 的 `[9.5.1+1.21.1]` 已实测通过，维持原样。
4. **1.21 起物品/方块用数据组件（DataComponent），不是 `CompoundTag` 裸写 NBT。** 属性/附魔等都在组件上。
5. **注册一律走 `DeferredRegister` + `RegisterEvent`**，不要用已废弃的静态注册或 `@Mod.EventBusSubscriber` 旧写法。属性的目标注册表是 **`BuiltInRegistries.ATTRIBUTE`**（`Attributes` 只是持有 `Holder<Attribute>` 的门面类）。
6. **`run/` 目录是开发环境状态，不是源码。** 不要把它当交付物，也不要把里面的日志/崩溃报告当作当前状态的证据（可能过期）。
7. **本机 PowerShell 是 5.1，不是 7。** 读 UTF-8 中文文件必须用 `[System.IO.File]::ReadAllText($p, [Text.Encoding]::UTF8)`；`Get-Content -Raw | ConvertFrom-Json` 会按 ANSI/GBK 解码而**静默取到空值**（不报错）。另外 `ConvertFrom-Json -AsHashtable` 在 5.1 不存在。

---

## §5 参考数据文件

位于 `.dsh/skills/tkr-context/references/`。这些是**从本机真实产物提取的**，不是网上抄的：

| 文件 | 内容 | 权威来源 |
|---|---|---|
| `mc-1.21.1-attributes.tsv` | 1.21.1 **全部**原版属性：id、英/中文名、默认值、取值范围、官方描述（本版本为空）、翻译键。**机器生成，不要手改。** | `build/moddev/artifacts/neoforge-21.1.232-sources.jar` 的 `Attributes.java` + `minecraft_1.21.1_client.jar` 的 `en_us.json` + assetIndex `17.json` 指向的 `zh_cn.json` |
| `mc-1.21.1-attributes.merged.tsv` | 上述 TSV 叠加**手工撰写**的中文简介与来源列，是 xlsx 的直接数据源 | 同上 + 本项目手工简介 |
| `_raw-attributes-notes.md` | 属性提取口径、注册代码片段、孤儿键与异常项 | 同上 |
| `curios-9.5.1-api.md` | Curios 9.5.1 真实签名、槽位注册机制、客户端边界、5.x→9.x 迁移陷阱 | `libs/curios-neoforge-9.5.1+1.21.1.jar`（`javap`） |
| `item-components-1.21.1.md` | **物品堆叠与数据组件**：堆叠上限/耐久硬校验、57 个组件清单与旧 NBT 对照表、「不用 NBT」的落地写法、组件注册 API | `neoforge-21.1.232-sources.jar` 的 `DataComponents.java`、`DataComponentType.java`、`Item.java`、`DeferredRegister.java` |
| `mc-1.21.1-item-components.tsv` | 全部 **57 个**原版物品组件：id、手工命名（英/中）、Java 类型、是否落盘、是否显式同步、简介、来源。**机器生成，不要手改。** | 同上 + `DataComponents.class` 常量池交叉验证 |
| `strength-spec.md` | **力量属性最终规格**：伤害曲线公式与数值表、耐久规则、力量下限组件、挂载点、代码位置、已否决方案。**改动力量机制前必读。** | 用户多轮澄清后的定稿 + 源码核验 |
| `docs/item-components-1.21.1.xlsx` | 上述 TSV 的表格交付物（表头 8 列：id/en_name/zh_name/java类型/是否落盘/是否同步/简介/来源） | 由 `.dsh/tools/build_components_data.py` 生成 |

**组件速查**：共 **57** 个；`是否落盘=否`（transient）**2 个**：`creative_slot_lock`、`map_post_processing`；`是否显式同步=否` **7 个**：`custom_data`、`intangible_projectile`、`map_decorations`、`debug_stick_state`、`recipes`、`lock`、`container_loot`。
**注意**：1.21.1 **没有**任何组件的官方英/中文显示名（语言文件只有 `item.components` 这一个计数键），故表中英/中文名均为本项目手工命名；**且「未显式同步」不等于「不能同步」**（见 `PITFALLS.md` P-015）。

**1.21.1 属性事实速查（31 个）**：前缀分布 generic 23 / player 7 / zombie 1；`en_us.json` 有 34 个 `attribute.name.*` 键，多出的 3 个是**孤儿键**（`attribute.name.generic.block_interaction_range`、`attribute.name.generic.entity_interaction_range`、`attribute.name.horse.jump_strength`）。`_desc` 键在 1.21.1 **不存在**，故官方描述列必为空。

**再次生成的方式**（幂等，改了数据只需重跑）：
```powershell
python .dsh\tools\build_attributes_data.py    # TSV → merged TSV → docs/attributes-1.21.1.xlsx
```

**使用纪律**：这些文件的价值在于「锁定到具体 artifact」。它们**不会随依赖升级自动更新**。一旦 `libs/` 里的 jar 或 `neo_version` 变更，必须重新提取并更新表头记录的 artifact 标识，否则整份参考数据失效 —— 这种情况下先把它标记为 `已过期`，再重新生成。

### 用户要求的交付物（xlsx）

用户明确要求属性的 xlsx 版本（英文名、中文名、简单介绍）。生成物：`docs/attributes-1.21.1.xlsx`。TSV 是它的数据源；**改数据时两份都要同步**，或从 TSV 重新生成 xlsx。

---

## §5b 开发环境专用模组（不是依赖，勿加入构建）

`run/mods/` 是 FML 的**自动扫描目录**（`run/` 即 gameDir），放在这里的 jar 会在启动时加载，
**完全不经过 Gradle / classpath**，因此**不是依赖**、不进产物。
`run/` 已在 `.gitignore` 中，所以这些 jar 也不会被提交。

| jar | 用途 | 说明 |
|---|---|---|
| `run/mods/1.21-damage_number-2.0.3.jar` | **伤害数字可视化**，用于调试力量伤害曲线 | 仅客户端表现；含 mixin（`defaultRequire: 0`，注入失败也不会崩） |

**纪律**：
- **禁止**把这类 jar 写进 `build.gradle` 或 `mods.toml` 依赖 —— 用户明确要求「不作为依赖」。
- 它们只是**开发期工具**，发布产物不应包含、也不应要求玩家安装。
- 需要撤销时直接删 `run/mods/` 下对应文件即可，不影响任何源码或构建配置。

---

## §6 交付与汇报纪律

- 改完代码/配置后，报告里要给出**可核对的证据**：命令、关键输出、产出的文件路径。
- 不要声称「已发送/已交付」文件；只说「已生成/已就绪」，并用 `present` 工具登记。
- 发现与既有决策冲突的事实，**先停下来告知用户**，不要静默改掉别人的设计。
- 中文回答（用户使用中文）。代码标识符、命令、路径保留英文原样。
