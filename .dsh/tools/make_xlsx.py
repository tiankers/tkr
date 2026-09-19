#!/usr/bin/env python3
"""把属性 TSV 转换成 xlsx（仅用 Python 标准库，不依赖 openpyxl / Office）。

用法:
    python make_xlsx.py <input.tsv> <output.xlsx> [--sheet 名称]

输入 TSV 要求: UTF-8、第一行为表头、制表符分隔。
输出: 冻结首行 + 自动筛选 + 表头样式 + 列宽 + 描述列自动换行。

生成的是最小合法 OOXML (SpreadsheetML)，Excel / LibreOffice / WPS 均可打开。
所有字符串以 inline string 写入，因此不维护 sharedStrings。
"""

from __future__ import annotations

import argparse
import datetime as _dt
import sys
import zipfile
from xml.sax.saxutils import escape


# ---------------------------------------------------------------- 样式定义
# 字体 / 填充 / 边框 / cellXfs 的索引必须与下面 styles_xml() 里的顺序一致。
STYLE_HEADER = 1   # 加粗白字 + 蓝底 + 居中 + 细边框
STYLE_TEXT = 2     # 文本：左对齐 + 垂直居中 + 细边框
STYLE_CENTER = 3   # 短文本：水平居中 + 细边框
STYLE_WRAP = 4     # 长文本：左对齐 + 自动换行 + 垂直靠上 + 细边框
STYLE_NUMBER = 5   # 数值：numFmt 164（0.0###）保留至少一位小数 + 居中

# 自定义数字格式 id（内置格式用 0..163，自定义从 164 起）
NUMFMT_ID = 164
NUMFMT_CODE = "0.0###"

# 列宽（字符数），按列序号；未列出的用默认宽度。
WIDTHS = {
    1: 26,   # id
    2: 22,   # en_name
    3: 22,   # zh_name
    4: 28,   # default / min
    5: 28,   # range / max
    6: 58,   # desc_en
    7: 58,   # desc_zh
    8: 34,   # translation_key
}
DEFAULT_WIDTH = 18
# 需要自动换行的列（长描述类）
WRAP_COLUMNS = {6, 7}


def col_letter(index: int) -> str:
    """1 -> A, 27 -> AA"""
    letters = ""
    while index > 0:
        index, rem = divmod(index - 1, 26)
        letters = chr(ord("A") + rem) + letters
    return letters


def numeric_cell(raw: str):
    """若字符串是干净的数字则返回 float，否则返回 None。"""
    text = raw.strip()
    if not text:
        return None
    try:
        return float(text)
    except ValueError:
        return None


def styles_xml() -> str:
    fonts = (
        '<fonts count="2">'
        '<font><sz val="11"/><name val="Calibri"/></font>'
        '<font><b/><sz val="11"/><color rgb="FFFFFFFF"/><name val="Calibri"/></font>'
        '</fonts>'
    )
    fills = (
        '<fills count="3">'
        '<fill><patternFill patternType="none"/></fill>'
        '<fill><patternFill patternType="gray125"/></fill>'
        '<fill><patternFill patternType="solid"><fgColor rgb="FF2F5597"/>'
        '<bgColor indexed="64"/></patternFill></fill>'
        '</fills>'
    )
    borders = (
        '<borders count="2">'
        '<border><left/><right/><top/><bottom/><diagonal/></border>'
        '<border>'
        '<left style="thin"><color rgb="FFBFBFBF"/></left>'
        '<right style="thin"><color rgb="FFBFBFBF"/></right>'
        '<top style="thin"><color rgb="FFBFBFBF"/></top>'
        '<bottom style="thin"><color rgb="FFBFBFBF"/></bottom>'
        '<diagonal/></border>'
        '</borders>'
    )
    cell_style_xfs = (
        '<cellStyleXfs count="1">'
        '<xf numFmtId="0" fontId="0" fillId="0" borderId="0"/>'
        '</cellStyleXfs>'
    )
    cell_xfs = (
        '<cellXfs count="6">'
        # 0: 默认
        '<xf numFmtId="0" fontId="0" fillId="0" borderId="0" xfId="0"/>'
        # 1: 表头
        '<xf numFmtId="0" fontId="1" fillId="2" borderId="1" xfId="0" applyFont="1" applyFill="1" applyBorder="1" applyAlignment="1">'
        '<alignment horizontal="center" vertical="center" wrapText="1"/></xf>'
        # 2: 文本
        '<xf numFmtId="0" fontId="0" fillId="0" borderId="1" xfId="0" applyBorder="1" applyAlignment="1">'
        '<alignment horizontal="left" vertical="center"/></xf>'
        # 3: 居中
        '<xf numFmtId="0" fontId="0" fillId="0" borderId="1" xfId="0" applyBorder="1" applyAlignment="1">'
        '<alignment horizontal="center" vertical="center"/></xf>'
        # 4: 换行长文本
        '<xf numFmtId="0" fontId="0" fillId="0" borderId="1" xfId="0" applyBorder="1" applyAlignment="1">'
        '<alignment horizontal="left" vertical="top" wrapText="1"/></xf>'
        # 5: 数值（保留至少一位小数）
        f'<xf numFmtId="{NUMFMT_ID}" fontId="0" fillId="0" borderId="1" xfId="0" applyNumberFormat="1" applyBorder="1" applyAlignment="1">'
        '<alignment horizontal="center" vertical="center"/></xf>'
        '</cellXfs>'
    )
    num_fmts = (
        f'<numFmts count="1"><numFmt numFmtId="{NUMFMT_ID}" formatCode="{NUMFMT_CODE}"/></numFmts>'
    )
    return (
        '<?xml version="1.0" encoding="UTF-8" standalone="yes"?>'
        '<styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">'
        f'{num_fmts}{fonts}{fills}{borders}{cell_style_xfs}{cell_xfs}'
        '</styleSheet>'
    )


def build_worksheet(rows: list[list[str]], widths: dict[int, int],
                    wrap_columns: set[int] | None = None) -> str:
    """按 ECMA-376 的元素顺序拼装 worksheet：sheetPr? -> dimension? -> sheetViews -> cols -> sheetData -> autoFilter。"""
    wrap = WRAP_COLUMNS if wrap_columns is None else wrap_columns
    ncols = max((len(r) for r in rows), default=0)
    out: list[str] = []
    out.append('<?xml version="1.0" encoding="UTF-8" standalone="yes"?>')
    out.append(
        '<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" '
        'xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">'
    )
    # 冻结首行（必须在 sheetData 之前）
    out.append(
        '<sheetViews><sheetView workbookViewId="0" tabSelected="1">'
        '<pane ySplit="1" topLeftCell="A2" activePane="bottomLeft" state="frozen"/>'
        '<selection pane="bottomLeft" activeCell="A2" sqref="A2"/>'
        '</sheetView></sheetViews>'
    )
    # 列宽
    out.append("<cols>")
    for i in range(1, ncols + 1):
        width = widths.get(i, DEFAULT_WIDTH)
        out.append(
            f'<col min="{i}" max="{i}" width="{width}" customWidth="1"/>'
        )
    out.append("</cols>")
    out.append("<sheetData>")

    for r_i, row in enumerate(rows, start=1):
        out.append(f'<row r="{r_i}">')
        for c_i in range(1, ncols + 1):
            value = row[c_i - 1] if c_i - 1 < len(row) else ""
            ref = f"{col_letter(c_i)}{r_i}"
            if r_i == 1:
                out.append(
                    f'<c r="{ref}" s="{STYLE_HEADER}" t="inlineStr">'
                    f'<is><t xml:space="preserve">{escape(value)}</t></is></c>'
                )
                continue
            number = numeric_cell(value)
            if number is not None:
                # 数值单元格：便于排序与统计；原样保留字面量（如 0.0 / 0.08），
                # 配合 numFmt 0.0### 保证小数位不被显示层吞掉。
                out.append(
                    f'<c r="{ref}" s="{STYLE_NUMBER}"><v>{escape(value.strip())}</v></c>'
                )
            else:
                style = STYLE_WRAP if c_i in wrap else (
                    STYLE_CENTER if c_i in (1, 2, 3, 8) else STYLE_TEXT
                )
                out.append(
                    f'<c r="{ref}" s="{style}" t="inlineStr">'
                    f'<is><t xml:space="preserve">{escape(value)}</t></is></c>'
                )
        out.append("</row>")
    out.append("</sheetData>")

    # autoFilter 必须排在 sheetData 之后
    last_col = col_letter(ncols) if ncols else "A"
    out.append(f'<autoFilter ref="A1:{last_col}{max(len(rows), 1)}"/>')
    out.append("</worksheet>")
    return "".join(out)


def workbook_xml(sheet_name: str) -> str:
    return (
        '<?xml version="1.0" encoding="UTF-8" standalone="yes"?>'
        '<workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" '
        'xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">'
        '<sheets>'
        f'<sheet name="{escape(sheet_name)}" sheetId="1" r:id="rId1"/>'
        '</sheets>'
        '</workbook>'
    )


def workbook_rels_xml() -> str:
    return (
        '<?xml version="1.0" encoding="UTF-8" standalone="yes"?>'
        '<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">'
        '<Relationship Id="rId1" '
        'Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" '
        'Target="worksheets/sheet1.xml"/>'
        '<Relationship Id="rId2" '
        'Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" '
        'Target="styles.xml"/>'
        '</Relationships>'
    )


def root_rels_xml() -> str:
    return (
        '<?xml version="1.0" encoding="UTF-8" standalone="yes"?>'
        '<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">'
        '<Relationship Id="rId1" '
        'Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" '
        'Target="xl/workbook.xml"/>'
        '<Relationship Id="rId2" '
        'Type="http://schemas.openxmlformats.org/package/2006/relationships/metadata/core-properties" '
        'Target="docProps/core.xml"/>'
        '<Relationship Id="rId3" '
        'Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/extended-properties" '
        'Target="docProps/app.xml"/>'
        '</Relationships>'
    )


def content_types_xml() -> str:
    return (
        '<?xml version="1.0" encoding="UTF-8" standalone="yes"?>'
        '<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">'
        '<Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>'
        '<Default Extension="xml" ContentType="application/xml"/>'
        '<Override PartName="/xl/workbook.xml" '
        'ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>'
        '<Override PartName="/xl/worksheets/sheet1.xml" '
        'ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>'
        '<Override PartName="/xl/styles.xml" '
        'ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/>'
        '<Override PartName="/docProps/core.xml" '
        'ContentType="application/vnd.openxmlformats-package.core-properties+xml"/>'
        '<Override PartName="/docProps/app.xml" '
        'ContentType="application/vnd.openxmlformats-officedocument.extended-properties+xml"/>'
        '</Types>'
    )


def core_xml(title: str) -> str:
    stamp = _dt.datetime.now().strftime("%Y-%m-%dT%H:%M:%SZ")
    return (
        '<?xml version="1.0" encoding="UTF-8" standalone="yes"?>'
        '<cp:coreProperties '
        'xmlns:cp="http://schemas.openxmlformats.org/package/2006/metadata/core-properties" '
        'xmlns:dc="http://purl.org/dc/elements/1.1/" '
        'xmlns:dcterms="http://purl.org/dc/terms/" '
        'xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">'
        f'<dc:title>{escape(title)}</dc:title>'
        '<dc:creator>TKR</dc:creator>'
        f'<dcterms:created xsi:type="dcterms:W3CDTF">{stamp}</dcterms:created>'
        f'<dcterms:modified xsi:type="dcterms:W3CDTF">{stamp}</dcterms:modified>'
        '</cp:coreProperties>'
    )


def app_xml() -> str:
    return (
        '<?xml version="1.0" encoding="UTF-8" standalone="yes"?>'
        '<Properties xmlns="http://schemas.openxmlformats.org/officeDocument/2006/extended-properties" '
        'xmlns:vt="http://schemas.openxmlformats.org/officeDocument/2006/docPropsVTypes">'
        '<Application>TKR make_xlsx.py</Application>'
        '</Properties>'
    )


def read_tsv(path: str) -> list[list[str]]:
    with open(path, "r", encoding="utf-8-sig", newline="") as handle:
        rows = []
        for line in handle:
            line = line.rstrip("\r\n")
            if not line:
                continue
            rows.append([cell.strip() for cell in line.split("\t")])
        return rows


def parse_widths(spec: str) -> dict[int, int]:
    """解析 `1:26,2:22` 形式的列宽覆盖。"""
    widths: dict[int, int] = {}
    for part in spec.split(","):
        part = part.strip()
        if not part:
            continue
        index, _, width = part.partition(":")
        widths[int(index)] = int(width)
    return widths


def parse_cols(spec: str) -> set[int]:
    """解析 `4,7,8` 形式的列号列表。"""
    return {int(p) for p in spec.split(",") if p.strip()}


def main(argv: list[str]) -> int:
    parser = argparse.ArgumentParser(description="TSV -> xlsx (stdlib only)")
    parser.add_argument("input", help="输入 TSV 路径")
    parser.add_argument("output", help="输出 xlsx 路径")
    parser.add_argument("--sheet", default="Sheet1", help="工作表名称")
    parser.add_argument("--widths", default="",
                        help="列宽覆盖，如 1:26,2:22（列号基于 1）")
    parser.add_argument("--wrap-cols", default="",
                        help="需要自动换行的列号，如 4,7,8")
    args = parser.parse_args(argv)

    rows = read_tsv(args.input)
    if not rows:
        print(f"error: {args.input} 没有数据行", file=sys.stderr)
        return 2

    widths = dict(WIDTHS)
    if args.widths:
        widths.update(parse_widths(args.widths))
    wrap = parse_cols(args.wrap_cols) if args.wrap_cols else None

    sheet = build_worksheet(rows, widths, wrap)
    with zipfile.ZipFile(args.output, "w", zipfile.ZIP_DEFLATED) as zf:
        zf.writestr("[Content_Types].xml", content_types_xml())
        zf.writestr("_rels/.rels", root_rels_xml())
        zf.writestr("docProps/core.xml", core_xml(args.sheet))
        zf.writestr("docProps/app.xml", app_xml())
        zf.writestr("xl/workbook.xml", workbook_xml(args.sheet))
        zf.writestr("xl/_rels/workbook.xml.rels", workbook_rels_xml())
        zf.writestr("xl/styles.xml", styles_xml())
        zf.writestr("xl/worksheets/sheet1.xml", sheet)

    print(f"OK: {args.output}  行数={len(rows)} (含表头)  列数={len(rows[0])}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main(sys.argv[1:]))
