# CurseForge 描述文件 — TKR Lib

> 上传时把 `==== 正文 ====` 之间的内容整体粘贴到 Description 字段。
> 设计目标：**后续新增内容无需改这份描述**。

---

## ==== 正文开始 ====

# TKR Lib

TKR Lib is a library mod. It adds a **Strength** attribute usable by all living entities, and item components that let weapons scale their damage with the wielder's Strength. It provides the mechanics; the content that grants Strength is meant to come from datapacks, other mods, or the included commands.

## What it provides

**`tkr:strength`** — a standard Minecraft attribute carried by every living entity, so any mod or datapack can raise it through the vanilla `attribute_modifiers` component or `/attribute`.

**`tkr:strength_damage`** — put it on a weapon and that weapon deals extra damage based on the attacker's Strength. Works on melee and ranged weapons alike. The coefficient and the damage threshold are stored per item, so each weapon can behave differently. Bonus damage rises, then bends smoothly toward a cap of twice the threshold.

**`tkr:min_strength`** — a Strength requirement for using an item. Attacks are cancelled while the requirement is unmet.

It also raises the maximum value of all attributes, so large values are no longer silently clamped by the vanilla caps.

Items carrying these components show their values in the tooltip.

## Commands

All commands start with `/tkr`. Run **`/tkrlist`** in game to list them with their current syntax.

## Requirements

- **Minecraft 1.21.1 with NeoForge**
- No other mods required

## License

LGPL-3.0-only. You may depend on it, including from closed-source projects; redistributed derivative works must stay under the LGPL or GPL.

## ==== 正文结束 ====

---

## Summary（单独填到 Summary 字段，勿照抄正文）

```
A library mod providing a Strength attribute for all living entities and item components that scale weapon damage with Strength.
```

---

## 为什么这份描述「后续基本不用改」

| 我砍掉的内容 | 砍掉的原因 |
|---|---|
| 「本版本没有武器/护甲/饰品/配方」 | 一旦加了物品就变成**假话** |
| 「Strength 目前无获取途径」 | 一旦加了获取途径就过期 |
| Getting started 的 4 步操作流程 | 含示例数值（`0.5`/`10`/`20`），改默认值就过期 |
| 具体公式与数值表 | 属机制细节；玩家要看数值可悬停物品，作者要看公式可读源码 |
| 耐久规则细节（1 点 / 2 点） | 属实现细节，改平衡就过期 |
| 属性上限改为 `1e20` 的具体数字 | 改配置就过期 |
| 「Reporting issues」小节 | 依赖 issue tracker 链接，平台已有独立入口 |

**保留的只有不会变的**：它是什么（库）、提供哪三样东西（属性 + 两个组件）、依赖什么、什么协议。

**关于「近战与远程」的处理**：描述里写的是 *"Works on melee and ranged weapons alike"* ——
这是**能力声明**，不是**变更记录**。以后新增投掷类、魔法类武器，这句话依然成立，不必再改。
**注意不要写成「现已支持远程」**，那会像更新日志，且下次加内容就显得过时。

**`/tkrlist` 是关键技巧**：指令列表由 Brigadier 指令树**动态生成**，所以描述里不需要抄一份指令表——游戏内查到的永远是当前版本。这样描述与实现不会脱节。

## Summary 为什么这样写

官方要求 Summary *"preferably no longer than 1 sentence"* 且 *"Try to avoid copying the same text from the description"*。这一句与正文措辞不同，且一句话说清了「是什么 + 给什么」。

## 上传字段速查

| 字段 | 填什么 |
|---|---|
| **Name** | `TKR Lib` |
| **Summary** | 上面 Summary 小节的一句 |
| **Description** | 上面 `==== 正文 ====` 之间的内容 |
| **License** | GNU Lesser General Public License v3.0（下拉没有就选 Custom 粘贴 `LICENSE`） |
| **Class** | Mods |
| **Category** | `Adventure and RPG` 或 `Game Mechanics` |
| **Logo** | 你已有的 400×400 PNG |
| **File** | `tkr-0.1.0.jar` |
| **Release Type** | Beta |
| **Game Version** | 1.21.1 |
| **Related Projects** | 无必需依赖，无需填写（Curios 依赖已移除） |

**上传前请先在 CurseForge 搜索 `TKR Lib` 确认不重名**（官方：*"If the name is already taken it will be rejected."*）。
