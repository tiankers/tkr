#!/usr/bin/env python3
"""把手工撰写的属性简介叠加到提取出的 TSV 上，生成带出处的合并 TSV。

背景：1.21.1 的官方语言文件里**没有**属性描述键（`attribute.name.*_desc` 命中数为 0），
因此「简介」列没有官方来源，由本项目手工撰写，并在「来源」列显式标注。

用法:
    python build_attributes_data.py

输入 : references/mc-1.21.1-attributes.tsv   (机器提取，不要手改)
输出 : references/mc-1.21.1-attributes.merged.tsv  (叠加简介 + 来源标注)
       docs/attributes-1.21.1.xlsx                 (最终交付)
"""

from __future__ import annotations

import pathlib
import subprocess
import sys

ROOT = pathlib.Path(__file__).resolve().parents[2]
SRC = ROOT / ".dsh" / "skills" / "tkr-context" / "references" / "mc-1.21.1-attributes.tsv"
MERGED = ROOT / ".dsh" / "skills" / "tkr-context" / "references" / "mc-1.21.1-attributes.merged.tsv"
OUT_XLSX = ROOT / "docs" / "attributes-1.21.1.xlsx"
MAKE_XLSX = ROOT / ".dsh" / "tools" / "make_xlsx.py"

# 机器可验证的事实（来自 jar / 反编译源码）→ 短标注
MACHINE_NOTE = "jar 提取（机器生成，未手改）"
HAND_NOTE = "jar 提取；简介为手工撰写"

# ---------------------------------------------------------------- 手工简介
# 键 = TSV 的 id 列（含前缀，与 Attributes.class 常量池一致）
# 值 = 2~3 句中文简介，讲清「影响什么」与「关键陷阱」。
INTRO: dict[str, str] = {
    "generic.armor": "降低绝大多数来源造成的伤害。每点护甲按伤害值分段减伤，收益递减；受盔甲韧性影响，高伤害攻击会削弱护甲效果。",
    "generic.armor_toughness": "降低「高伤害攻击削弱护甲」的程度。原版从不给实体赋非默认值，是 mod 常改项。",
    "generic.attack_damage": "近战攻击的基础伤害。玩家实际值由手持物品的武器组件决定，此默认值生效于无武器实体。",
    "generic.attack_knockback": "攻击命中时施加的额外击退强度，主要供横扫与特定武器使用。",
    "generic.attack_speed": "每秒攻击次数。数值越高攻击冷却越快，HUD 上的攻击条长度即由此换算。",
    "player.block_break_speed": "方块破坏速度倍率。1.0 为原版基准，工具效率与挖掘速度在此之上叠加。",
    "player.block_interaction_range": "可触及/交互方块的最大距离（原版生存为 4.5）。创造模式会另加距离。",
    "generic.burning_time": "被点燃后的燃烧时长倍率。1.0 为原版基准，大于 1 会烧更久。原版不显式赋非默认值，是 mod 常改项。",
    "generic.explosion_knockback_resistance": "对爆炸击退的抗性。与普通击退抗性分开计算，原版不显式赋值，属 mod 常用属性。",
    "player.entity_interaction_range": "可攻击/交互实体的最大距离（原版生存为 3.0）。影响攻击判定与部分交互。",
    "generic.fall_damage_multiplier": "摔落伤害倍率。低于 1 减伤，高于 1 增伤；与安全摔落高度配合决定总摔落伤害。",
    "generic.flying_speed": "飞行速度。仅对具备飞行能力的实体（如鹦鹉螺/特定坐骑）生效，非玩家创造飞行。",
    "generic.follow_range": "生物发现目标的索敌距离。是 AI 目标选择的判定半径，不是视野。",
    "generic.gravity": "每 tick 的重力加速度。原版不显式赋非默认值，改它可做轻重力/悬浮效果。",
    "generic.jump_strength": "跳跃初速度。原版曾是马匹专用（旧键 attribute.name.horse.jump_strength 作为孤儿键仍留在语言文件中）。",
    "generic.knockback_resistance": "抵抗被击退的比例（0~1）。NeoForge 中为 PercentageAttribute，tooltip 按百分比显示，数值与范围同原版。",
    "generic.luck": "影响战利品表与钓鱼的品质判定，即「幸运」效果所改的属性。",
    "generic.max_absorption": "伤害吸收（金心）的上限。原版不显式赋非默认值，是 mod 常改项。",
    "generic.max_health": "最大生命值，默认 20（即 10 颗心）。生命值上限的显示与判定均以此为准。",
    "player.mining_efficiency": "挖掘效率加成，作用于方块破坏速度。原版不显式赋非默认值，是 mod 常改项。",
    "generic.movement_efficiency": "移动效率，用于抵消部分移动惩罚（如跳跃疲劳）。原版不显式赋非默认值。",
    "generic.movement_speed": "移动速度。**注意**：属性注册默认值为 0.7，但玩家实际速度由 Player.createAttributes() 覆盖为 0.1，改造此属性前务必确认目标实体。",
    "generic.oxygen_bonus": "额外氧气，延长水下憋气时间。原版不显式赋非默认值，是 mod 常改项。",
    "generic.safe_fall_distance": "安全摔落高度，超过此高度才开始计算摔落伤害，默认 3 格。",
    "generic.scale": "实体尺寸缩放倍率。**陷阱**：模型/碰撞箱的缩放并不自动跟随，需自行处理渲染与判定箱。",
    "player.sneaking_speed": "潜行时的移动速度倍率，默认 0.3。",
    "zombie.spawn_reinforcements": "僵尸受击时召唤增援的概率（0~1）。仅对僵尸类生效。",
    "generic.step_height": "可直接迈上的最大高度，默认 0.6 格（半格台阶+）。改大可自动上整格。",
    "player.submerged_mining_speed": "水下挖掘速度倍率，默认 0.2（即水下挖得极慢）。",
    "player.sweeping_damage_ratio": "横扫攻击对周围实体造成的伤害比例，默认 0。",
    "generic.water_movement_efficiency": "水中移动效率，影响水中移动速度。原版不显式赋非默认值，是 mod 常改项。",
}

HEADER = ["id", "en_name", "zh_name", "简介", "default", "range",
          "desc_en", "desc_zh", "translation_key", "来源"]


def main() -> int:
    rows = [ln.rstrip("\n").split("\t") for ln in
            SRC.read_text(encoding="utf-8-sig").splitlines() if ln.strip()]
    src_header, data = rows[0], rows[1:]
    if src_header[:3] != ["id", "en_name", "zh_name"]:
        print(f"error: 源 TSV 表头异常: {src_header}", file=sys.stderr)
        return 2

    missing = [r[0] for r in data if r[0] not in INTRO]
    if missing:
        print(f"error: 以下属性缺手工简介: {missing}", file=sys.stderr)
        return 3
    unused = sorted(set(INTRO) - {r[0] for r in data})
    if unused:
        print(f"error: 简介表中存在源数据没有的 id: {unused}", file=sys.stderr)
        return 4

    out = ["\t".join(HEADER)]
    for r in data:
        rid = r[0]
        out.append("\t".join([
            rid, r[1], r[2], INTRO[rid], r[3], r[4], r[5], r[6],
            r[7] if len(r) > 7 else "", HAND_NOTE,
        ]))
    MERGED.parent.mkdir(parents=True, exist_ok=True)
    MERGED.write_text("\n".join(out) + "\n", encoding="utf-8")
    print(f"OK merged: {MERGED}  数据行={len(data)}")

    OUT_XLSX.parent.mkdir(parents=True, exist_ok=True)
    cmd = [
        sys.executable, str(MAKE_XLSX), str(MERGED), str(OUT_XLSX),
        "--sheet", "1.21.1属性",
        "--wrap-cols", "4,7,8",
        "--widths", "1:26,2:22,3:22,4:64,5:12,6:16,7:8,8:8,9:38,10:24",
    ]
    proc = subprocess.run(cmd, capture_output=True, text=True)
    sys.stdout.write(proc.stdout)
    if proc.returncode != 0:
        sys.stderr.write(proc.stderr)
        return proc.returncode
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
