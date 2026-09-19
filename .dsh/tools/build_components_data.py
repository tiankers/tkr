#!/usr/bin/env python3
"""把物品组件 TSV 转换成最终交付的 xlsx（物品组件清单，MC 1.21.1 / NeoForge 21.1.232）。

与 build_attributes_data.py 的差异：
    属性表的数据源是「机器提取的 TSV + Python 里手写的简介」，所以要先生成 merged TSV；
    物品组件表的 TSV **本身就已经包含** id / 类型 / 落盘 / 同步 / 简介 / 来源 全部列，
    因此这里不再做叠加，只做严格校验后直接转 xlsx。

背景：1.21.1 的官方语言文件里**没有**逐个组件的显示名键（只有唯一的 `item.components`
= `"%s component(s)"` / `"%s个组件"`），组件提示名由序列化 id 现场生成。
故 en_name / zh_name / 简介 均为本项目手工撰写，并在「来源」列显式标注。

用法:
    python build_components_data.py

输入 : .dsh/skills/tkr-context/references/mc-1.21.1-item-components.tsv  (UTF-8 无 BOM, LF)
输出 : docs/item-components-1.21.1.xlsx
"""

from __future__ import annotations

import pathlib
import subprocess
import sys

ROOT = pathlib.Path(__file__).resolve().parents[2]
SRC = ROOT / ".dsh" / "skills" / "tkr-context" / "references" / "mc-1.21.1-item-components.tsv"
OUT_XLSX = ROOT / "docs" / "item-components-1.21.1.xlsx"
MAKE_XLSX = ROOT / ".dsh" / "tools" / "make_xlsx.py"

# 1.21.1 官方注册的物品数据组件总数（DataComponents.java 的 register(...) 调用数）
EXPECTED_ROWS = 57

HEADER = ["id", "en_name", "zh_name", "java类型", "是否落盘", "是否同步", "简介", "来源"]

# 「来源」列必须逐字等于此串（表明数据来自 jar 提取 + 手工命名）
EXPECTED_SOURCE = "jar 提取；名称与简介为手工撰写"

# 简介所在列（1 基），供 make_xlsx.py 设自动换行
INTRO_COL = 7


def main() -> int:
    if not SRC.is_file():
        print(f"error: 输入 TSV 不存在: {SRC}", file=sys.stderr)
        return 2

    # utf-8-sig：既接受无 BOM，也能容忍误加的 BOM（BOM 状态另外单独检查）
    lines = [
        ln for ln in SRC.read_text(encoding="utf-8-sig").split("\n") if ln.strip()
    ]
    if not lines:
        print(f"error: {SRC} 为空", file=sys.stderr)
        return 3

    rows = [ln.split("\t") for ln in lines]
    header, data = rows[0], rows[1:]

    # ---- 表头校验：列名与顺序必须完全一致
    if header != HEADER:
        print(f"error: 表头不符。期望 {HEADER}\n        实际 {header}", file=sys.stderr)
        return 4

    # ---- 行数校验
    if len(data) != EXPECTED_ROWS:
        print(
            f"error: 数据行数 {len(data)} != 期望 {EXPECTED_ROWS}（1.21.1 官方组件总数）",
            file=sys.stderr,
        )
        return 5

    # ---- 列数与逐列内容校验（任何一行缺列都会在这里炸掉，不会静默生成 xlsx）
    problems: list[str] = []
    seen: set[str] = set()
    for n, r in enumerate(data, start=2):
        if len(r) != len(HEADER):
            problems.append(f"第 {n} 行有 {len(r)} 列，期望 {len(HEADER)} 列")
            continue
        rid, en, zh, jtype, persist, sync, intro, source = r
        if not rid:
            problems.append(f"第 {n} 行 id 为空")
        elif rid in seen:
            problems.append(f"第 {n} 行 id 重复: {rid}")
        else:
            seen.add(rid)
        if not en or not zh:
            problems.append(f"第 {n} 行 ({rid}) en_name/zh_name 为空")
        if not jtype.startswith("DataComponentType<") or not jtype.endswith(">"):
            problems.append(f"第 {n} 行 ({rid}) java类型 不是 DataComponentType<...>: {jtype!r}")
        if persist not in ("是", "否"):
            problems.append(f"第 {n} 行 ({rid}) 是否落盘 非法: {persist!r}")
        if sync not in ("是", "否"):
            problems.append(f"第 {n} 行 ({rid}) 是否同步 非法: {sync!r}")
        if not intro:
            problems.append(f"第 {n} 行 ({rid}) 简介为空")
        if source != EXPECTED_SOURCE:
            problems.append(f"第 {n} 行 ({rid}) 来源 不符: {source!r}")

    if problems:
        print("error: TSV 校验失败：", file=sys.stderr)
        for p in problems:
            print(f"  - {p}", file=sys.stderr)
        return 6

    # ---- BOM / 换行 硬校验：xlsx 生成前先确认数据源干净
    raw = SRC.read_bytes()
    if raw[:3] == b"\xef\xbb\xbf":
        print(f"error: {SRC.name} 带 UTF-8 BOM，必须无 BOM", file=sys.stderr)
        return 7
    if b"\r\n" in raw:
        print(f"error: {SRC.name} 含 CRLF，必须为 LF", file=sys.stderr)
        return 8

    n_persist_no = sum(1 for r in data if r[4] == "否")
    n_sync_no = sum(1 for r in data if r[5] == "否")

    OUT_XLSX.parent.mkdir(parents=True, exist_ok=True)
    cmd = [
        sys.executable, str(MAKE_XLSX), str(SRC), str(OUT_XLSX),
        "--sheet", "物品组件",
        "--wrap-cols", str(INTRO_COL),
        "--widths", "1:26,2:24,3:20,4:44,5:10,6:10,7:80,8:32",
    ]
    proc = subprocess.run(cmd, capture_output=True, text=True)
    sys.stdout.write(proc.stdout)
    if proc.returncode != 0:
        sys.stderr.write(proc.stderr)
        return proc.returncode

    print(f"OK components: {len(data)} 行 -> {OUT_XLSX}")
    print(f"   是否落盘=否: {n_persist_no} 个；是否同步=否: {n_sync_no} 个")
    print("   BOM=无  CRLF=0  列数=8")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
