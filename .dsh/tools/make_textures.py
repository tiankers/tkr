#!/usr/bin/env python3
"""生成 TKR 模组的占位物品贴图（16x16 RGBA PNG，仅用标准库）。

用法:
    python make_textures.py

输出:
    src/main/resources/assets/tkr/textures/item/strength_blade.png

设计：一把斜置的剑 —— 灰白刀刃 + 蓝色力量纹 + 棕色剑柄。
写成脚本而非直接塞二进制，是为了让贴图可复现、可审阅、可微调。
"""

from __future__ import annotations

import pathlib
import struct
import zlib

ROOT = pathlib.Path(__file__).resolve().parents[2]
OUT = ROOT / "src" / "main" / "resources" / "assets" / "tkr" / "textures" / "item" / "strength_blade.png"

SIZE = 16
TRANSPARENT = (0, 0, 0, 0)
# 调色板
BLADE_LIGHT = (232, 236, 245, 255)   # 刀刃受光面
BLADE_DARK = (150, 158, 175, 255)    # 刀刃背光面
GLOW = (90, 170, 255, 255)           # 力量纹（蓝）
GLOW_DIM = (45, 110, 190, 255)
HILT = (196, 160, 70, 255)           # 护手（金）
HANDLE = (110, 74, 44, 255)          # 剑柄（木）
HANDLE_DARK = (78, 50, 28, 255)
POMMEL = (196, 160, 70, 255)


def build_pixels() -> list[list[tuple[int, int, int, int]]]:
    """构造 16x16 像素矩阵。坐标 (x, y)，y 向下增大。"""
    px = [[TRANSPARENT for _ in range(SIZE)] for _ in range(SIZE)]

    def put(x: int, y: int, color):
        if 0 <= x < SIZE and 0 <= y < SIZE:
            px[y][x] = color

    # 刀刃：从右上到左下的斜线，宽度 2（受光 + 背光）
    for i in range(11):
        x = 13 - i
        y = 2 + i
        put(x, y, BLADE_LIGHT)
        put(x - 1, y, BLADE_DARK)

    # 剑尖加亮
    put(13, 1, BLADE_LIGHT)
    put(14, 2, BLADE_LIGHT)

    # 力量纹：沿刀身中段的两点蓝色
    put(10, 5, GLOW)
    put(8, 7, GLOW)
    put(6, 9, GLOW_DIM)

    # 护手：与刀身垂直的短横线
    for dx in range(-2, 3):
        put(4 + dx, 12, HILT)
    put(4, 11, HILT)

    # 剑柄：向左下延伸
    put(3, 13, HANDLE)
    put(2, 14, HANDLE)
    put(1, 15, HANDLE_DARK)
    put(3, 12, HANDLE_DARK)

    # 剑首
    put(1, 14, POMMEL)

    return px


def png_bytes(px: list[list[tuple[int, int, int, int]]]) -> bytes:
    """把像素矩阵编码成最小合法 PNG（RGBA8，无隔行）。"""
    raw = bytearray()
    for row in px:
        raw.append(0)  # filter type 0 (None)
        for r, g, b, a in row:
            raw += bytes((r, g, b, a))

    def chunk(tag: bytes, data: bytes) -> bytes:
        return (struct.pack(">I", len(data)) + tag + data
                + struct.pack(">I", zlib.crc32(tag + data) & 0xFFFFFFFF))

    ihdr = struct.pack(">IIBBBBB", SIZE, SIZE, 8, 6, 0, 0, 0)  # 8bit, RGBA(6)
    return (b"\x89PNG\r\n\x1a\n"
            + chunk(b"IHDR", ihdr)
            + chunk(b"IDAT", zlib.compress(bytes(raw), 9))
            + chunk(b"IEND", b""))


def main() -> int:
    OUT.parent.mkdir(parents=True, exist_ok=True)
    data = png_bytes(build_pixels())
    OUT.write_bytes(data)
    print(f"OK: {OUT}  ({len(data)} B)")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
