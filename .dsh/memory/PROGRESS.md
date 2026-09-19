# TKR 进度记忆

> **最后更新**：2026-09-19（首次建立）
> **当前阶段**：① 骨架搭建 —— 结构已定，**玩法规格仍为空**，尚无任何游戏内容。
> **规则**：本文件是跨会话的唯一权威进度来源。每次实质性工作后必须更新，并修正过期条目。

---

## 当前状态一句话

项目是**可编译、可打包、可启动的空骨架**：四层目录已建、Curios 依赖已通、构建通过；但 `docs/idea.md` 尚未填写，所以**没有任何物品、方块、槽位、效果或网络包**。

## 已完成

| 日期 | 事项 | 证据 / 产出 |
|---|---|---|
| 2026-09-19 | 摸清项目现状：NeoForge 1.21.1 空骨架，仅 `TkrMod.java` + `mods.toml` | 见 SKILL.md §1 |
| 2026-09-19 | 选定并落地**方案 C「严格领域四层 + 公开 API」**目录结构 | `src/main/java/dev/tkr/{api,data,logic,client}/`，各层约束写入 `package-info.java` |
| 2026-09-19 | 资源目录骨架：`assets/tkr/{lang,models,textures}`、`data/tkr/{recipe,advancement,loot_table,tags,damage_type}` | 空目录 + `.gitkeep`（仅 `lang/`） |
| 2026-09-19 | **完全移除 Patchouli 依赖**（build.gradle、mods.toml、libs jar、patchouli_books 资源、run/ 残留） | `BUILD SUCCESSFUL`；打包 jar 内无 patchouli 条目 |
| 2026-09-19 | 建立项目级技能与记忆体系 | `.dsh/skills/tkr-context/SKILL.md`、`.dsh/memory/*`；`tkr-context` 已出现在 DSH 技能目录中（发现链路验证通过） |
| 2026-09-19 | **生成 1.21.1 全量属性清单（31 个）** | `references/mc-1.21.1-attributes.tsv`（机器提取）+ `.merged.tsv`（叠加手工简介）+ **`docs/attributes-1.21.1.xlsx`**（Excel 实测打开：10 列 × 32 行、冻结首行、自动筛选、中文正常） |
| 2026-09-19 | 纠正「属性 id 无前缀」的错误断言 | 见 DECISIONS D-006；`Attributes.class` 常量池核验裸 `max_health` 命中 0 次 |
| 2026-09-19 | **整理物品组件与堆叠，确立「物品不用 NBT」规范** | `references/item-components-1.21.1.md`；57 个组件清单 + 旧 NBT 对照表；核验出 3 个新陷阱（P-012/013/014） |
| 2026-09-19 | **完成 Curios 9.5.1 API 参考（444 行）** | `references/curios-9.5.1-api.md`；已独立复核 `ICurioItem` 26 方法、`SlotContext` 为 record、`curios/slots` datapack 资源根、`TriState` |
| 2026-09-19 | **完成 57 个物品组件清单（TSV + xlsx）** | `references/mc-1.21.1-item-components.tsv`、`docs/item-components-1.21.1.xlsx`（Excel 实测：`$A$1:$H$58`，8 列）；落盘否 2、显式同步否 7 |
| 2026-09-19 | **57 个物品组件数据通过独立审计（零分歧）** | 三路径交叉验证：源码解析 / `javap -v` 常量池 / 逐 lambda 字节码推导（55 调 persistent、50 调 networkSynchronized）；TSV 与 xlsx 行集 diff=0 |
| 2026-09-19 | **实现力量属性机制（已实跑验证）** | `tkr:strength`（全体生物注入）+ `tkr:min_strength` 组件 + 伤害结算 + 攻击门槛 + 中英语言文件；`BUILD SUCCESSFUL` 且 `runClient` 无错误，自检行确认 `id=tkr:strength 默认值=0.0 范围=[0.0,1024.0] 可同步=true` |
| 2026-09-19 | **实跑抓出并修复游戏/模组总线错注册** | `LivingIncomingDamageEvent` 被注册到模组总线导致模组加载失败；改用 `@EventBusSubscriber(bus = GAME)`。见 `PITFALLS.md` P-019 |
| 2026-09-19 | **p/T 改为物品组件数据 + 组件自带提示 + 测试物品** | 新增 `tkr:strength_damage` 组件（含 `TooltipProvider` 自动显示伤害系数/额值）与 `tkr:strength_blade` 测试物品（含模型贴图）；属性翻译键改为 `attribute.name.tkr.strength`；见 D-011/D-012/D-013 |
| 2026-09-19 | **放宽属性上限（已实跑验证）** | 反射改写 `RangedAttribute.maxValue` 至 `1e20`；**34 个属性全成功、0 失败**，钳制测试通过；只改上限不改下限（参照 AttributeFix）；见 D-014、P-021、P-022 |
| 2026-09-19 | **新增 `/tkr` 指令给手上物品赋组件** | `/tkr strength_damage <p> [T]` 与 `/tkr min_strength <value>`，各带 `remove`；含容器同步与中英反馈；编译通过、实跑无异常 |
| 2026-09-19 | **安装开发期伤害数字模组（非依赖）** | `run/mods/1.21-damage_number-2.0.3.jar`；FML 自动扫描加载（Mod List 已确认），**未改动 build.gradle**；见 SKILL.md §5b |
| 2026-09-19 | **推翻并更正自己的两处错误结论** | ① 属性 id 前缀（D-006）；② P-001 的「纯数字下界」建议经 Maven 实测为错，见 `PITFALLS.md` P-001 修正记录与 D-009 |

## 待办（按依赖顺序）

| # | 事项 | 完成判据 | 阻塞于 |
|---|---|---|---|
| 1 | **填写 `docs/idea.md` 玩法规格** | 明确核心玩法、目标物品/方块清单、是否需要自定义 Curios 槽位 | 用户决策 |
| 2 | 定模组引导方案（手册能力已随 Patchouli 移除） | 三选一有结论并记入 DECISIONS.md：自绘 GUI / 原版进度 / 重新引入 Patchouli 为**可选**依赖 | 用户决策，依赖 #1 |
| 3 | 实现 Curios 自定义槽位注册 | `logic/curios/` 下有注册代码，`runClient` 能进游戏且槽位可见 | 依赖 #1 |
| 4 | 首个物品 + 创造模式标签页 | `data/item/` 有物品，`data/registry/` 有注册，游戏内可取得 | 依赖 #1 |
| 5 | 为「禁止 client 泄漏」建立自动校验 | 有可执行命令能检出 `logic`/`data` 中的客户端 import | 可选，价值高 |

## 已知阻塞 / 风险

- **无阻塞性技术问题**。构建、依赖、目录均正常。
- **最大风险是需求空白**：结构只是容器，`logic/curios`、`logic/effect` 里放什么完全取决于尚未确定的玩法。在 #1 完成前推进 #3/#4 属于猜测用户意图，应先确认。
- `run/` 内有历史崩溃报告（2026-09-17、09-19 各若干），**根因（Patchouli 版本区间）已随依赖移除而消失**，属历史记录，不是当前故障。

## 环境事实（跨会话有效）

- 本机有 Gradle **离线镜像** init script（`[offline-mirror] ACTIVE`），构建不依赖网络；`maven.neoforged.net` 不可达。
- Excel 16.0 COM 可用；Python 3.14.4 在位但**无 openpyxl**（生成 xlsx 走 Excel COM 或自写 OOXML）。
- 本地 Minecraft 1.21.1 产物：`~/.gradle/caches/neoformruntime/artifacts/minecraft_1.21.1_client.jar`（含官方语言文件）、`build/moddev/artifacts/neoforge-21.1.232-sources.jar`（含反编译源码）。**这两个是核对 1.21.1 事实的最佳本地依据。**
