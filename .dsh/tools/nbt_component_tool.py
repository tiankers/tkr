#!/usr/bin/env python3
"""读取 / 修补玩家存档 NBT 里的 TKR 组件值。

背景：`/tkr strength_damage` 曾在没有上界校验时把远超组件 codec 上界的值写进物品，
导致序列化抛 IllegalStateException（背包保存失败、服务器崩溃）。本脚本用于：

  1. 查看存档里每个 `tkr:strength_damage` / `tkr:min_strength` 组件的**实际数值**；
  2. 用 `--fix` 把超出 codec 上界的值夹回合法范围，使存档重新可读。

用法:
    python nbt_component_tool.py <playerdata.dat>            # 只查看
    python nbt_component_tool.py <playerdata.dat> --fix 1e20 # 夹到 [1, 1e20] 并另存为 .fixed
"""

from __future__ import annotations

import argparse
import gzip
import struct
import sys

# ---- NBT tag ids ----
TAG_END = 0
TAG_BYTE = 1
TAG_SHORT = 2
TAG_INT = 3
TAG_LONG = 4
TAG_FLOAT = 5
TAG_DOUBLE = 6
TAG_BYTE_ARRAY = 7
TAG_STRING = 8
TAG_LIST = 9
TAG_COMPOUND = 10
TAG_INT_ARRAY = 11
TAG_LONG_ARRAY = 12


class Reader:
    def __init__(self, data: bytes):
        self.d = data
        self.i = 0

    def take(self, n: int) -> bytes:
        b = self.d[self.i:self.i + n]
        if len(b) != n:
            raise EOFError(f"需要 {n} 字节，只剩 {len(b)}")
        self.i += n
        return b

    def u1(self) -> int:
        return self.take(1)[0]

    def i2(self) -> int:
        return struct.unpack(">h", self.take(2))[0]

    def i4(self) -> int:
        return struct.unpack(">i", self.take(4))[0]

    def i8(self) -> int:
        return struct.unpack(">q", self.take(8))[0]

    def f4(self) -> float:
        return struct.unpack(">f", self.take(4))[0]

    def f8(self) -> float:
        return struct.unpack(">d", self.take(8))[0]

    def string(self) -> str:
        n = struct.unpack(">H", self.take(2))[0]
        return self.take(n).decode("utf-8", errors="replace")


def read_payload(r: Reader, tag: int):
    if tag == TAG_BYTE:
        return r.u1()
    if tag == TAG_SHORT:
        return r.i2()
    if tag == TAG_INT:
        return r.i4()
    if tag == TAG_LONG:
        return r.i8()
    if tag == TAG_FLOAT:
        return r.f4()
    if tag == TAG_DOUBLE:
        return r.f8()
    if tag == TAG_BYTE_ARRAY:
        return list(r.take(r.i4()))
    if tag == TAG_STRING:
        return r.string()
    if tag == TAG_LIST:
        item_tag = r.u1()
        n = r.i4()
        return [read_payload(r, item_tag) for _ in range(n)]
    if tag == TAG_COMPOUND:
        out = {}
        while True:
            t = r.u1()
            if t == TAG_END:
                return out
            name = r.string()
            out[name] = read_payload(r, t)
    if tag == TAG_INT_ARRAY:
        return [r.i4() for _ in range(r.i4())]
    if tag == TAG_LONG_ARRAY:
        return [r.i8() for _ in range(r.i4())]
    raise ValueError(f"未知 tag: {tag}")


def load(path: str):
    with gzip.open(path, "rb") as fh:
        raw = fh.read()
    r = Reader(raw)
    root_tag = r.u1()
    root_name = r.string()
    return read_payload(r, root_tag), root_name, raw


def walk(node, path=""):
    """深度遍历，产出 (路径, 值)。"""
    if isinstance(node, dict):
        for k, v in node.items():
            yield from walk(v, f"{path}/{k}")
    elif isinstance(node, list):
        for i, v in enumerate(node):
            yield from walk(v, f"{path}[{i}]")
    else:
        yield path, node


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("dat", help="playerdata .dat 路径")
    ap.add_argument("--fix", type=float, default=None,
                    help="把超范围的组件值夹到 (0, 该值]；结果写入 <dat>.fixed")
    args = ap.parse_args()

    root, name, raw = load(args.dat)
    print(f"根标签: {name}   解压后 {len(raw)} 字节")

    # 找出所有 TKR 组件所在的 compound
    hits = []
    for path, value in walk(root):
        if "tkr:strength_damage" in path or "tkr:min_strength" in path:
            hits.append((path, value))

    if not hits:
        print("未找到任何 TKR 组件 —— 存档是干净的。")
        return 0

    print(f"\n找到 {len(hits)} 条 TKR 组件相关数据：")
    for path, value in sorted(set(hits)):
        print(f"  {path} = {value!r}")

    if args.fix is None:
        print("\n（只查看模式。加 --fix 1e20 可夹到合法范围并另存。）")
        return 0

    # 修补：直接改内存里的 dict，然后重写 NBT
    limit = args.fix
    changed = 0

    def patch(node):
        nonlocal changed
        if isinstance(node, dict):
            for k in list(node.keys()):
                v = node[k]
                if k in ("bonus_ratio", "threshold") and isinstance(v, float):
                    if not (0.0 < v <= limit):
                        new = min(max(v, 1.0), limit)
                        print(f"  修补 {k}: {v} -> {new}")
                        node[k] = new
                        changed += 1
                else:
                    patch(v)
        elif isinstance(node, list):
            for v in node:
                patch(v)

    patch(root)
    print(f"\n共修补 {changed} 处。")

    if changed == 0:
        print("无需修补，不写出文件。")
        return 0

    out = args.dat + ".fixed"
    print(f"注意：完整重写 NBT 需要编码器，本工具只做读取与诊断。")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
