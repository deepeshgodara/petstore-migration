#!/usr/bin/env python3
"""
Data-driven PowerPoint renderer for the Pet Store modernization playback deck.

Reads a deck spec (JSON) and emits a 16:9 .pptx. Content lives in the JSON;
this file owns only the design system and layout engine.

Usage:
    python3 scripts/render_deck.py <deck.json> <out.pptx>

Deck spec shape:
{
  "deckTitle": str,
  "subtitle": str,
  "slides": [
    {
      "number": int,
      "kicker": str,                 # small uppercase pill above the title
      "title": str,
      "layout": "title|cards|two-column|table|code|diagram|metrics|screenshot",
      "cards":    [ {"heading": str, "bullets": [str]} ],
      "tableRows":[ [str, ...] ],    # row 0 is the header
      "codeBlock": str,
      "codeCaption": str,
      "metrics":  [ {"value": str, "label": str} ],
      "screenshot": str,             # filename in docs/demo_screenshots
      "speakerNotes": str
    }
  ]
}
"""

import json
import os
import sys

from pptx import Presentation
from pptx.dml.color import RGBColor
from pptx.enum.shapes import MSO_SHAPE
from pptx.enum.text import MSO_ANCHOR, PP_ALIGN
from pptx.util import Emu, Inches, Pt

# ---------------------------------------------------------------------------
# Design system.
#
# Colors are taken verbatim from the data-viz reference palette's DARK column,
# which is documented as validated against the dark surface #1a1a19. The first
# three categorical slots (blue/orange/aqua) are the ones certified for
# all-pairs use, so the deck never uses more than those three plus the fixed
# status palette. No hex here is invented.
# ---------------------------------------------------------------------------

PAGE_PLANE = RGBColor(0x0D, 0x0D, 0x0D)   # slide background
SURFACE = RGBColor(0x1A, 0x1A, 0x19)      # card surface
SURFACE_RAISED = RGBColor(0x24, 0x24, 0x22)

INK_PRIMARY = RGBColor(0xFF, 0xFF, 0xFF)
INK_SECONDARY = RGBColor(0xC3, 0xC2, 0xB7)
INK_MUTED = RGBColor(0x89, 0x87, 0x81)

HAIRLINE = RGBColor(0x2C, 0x2C, 0x2A)
BASELINE = RGBColor(0x38, 0x38, 0x35)

# categorical slots 1-3 (dark, all-pairs validated)
SLOT = [
    RGBColor(0x39, 0x87, 0xE5),  # blue
    RGBColor(0xD9, 0x59, 0x26),  # orange
    RGBColor(0x19, 0x9E, 0x70),  # aqua
]

# status palette (fixed, never themed)
GOOD = RGBColor(0x0C, 0xA3, 0x0C)
WARNING = RGBColor(0xFA, 0xB2, 0x19)
CRITICAL = RGBColor(0xD0, 0x3B, 0x3B)

SANS = "Helvetica Neue"
MONO = "Menlo"

SLIDE_W = 13.333
SLIDE_H = 7.5
MARGIN = 0.72
CONTENT_TOP = 1.72
CONTENT_H = SLIDE_H - CONTENT_TOP - 0.55
CONTENT_W = SLIDE_W - (2 * MARGIN)

SCREENSHOT_DIR = os.path.join(
    os.path.dirname(os.path.dirname(os.path.abspath(__file__))),
    "docs",
    "demo_screenshots",
)


# ---------------------------------------------------------------------------
# primitives
# ---------------------------------------------------------------------------

def _noline(shape):
    shape.line.fill.background()
    return shape


def rect(slide, left, top, width, height, fill, rounded=False, radius=0.04):
    shape = slide.shapes.add_shape(
        MSO_SHAPE.ROUNDED_RECTANGLE if rounded else MSO_SHAPE.RECTANGLE,
        Inches(left), Inches(top), Inches(width), Inches(height),
    )
    shape.fill.solid()
    shape.fill.fore_color.rgb = fill
    shape.shadow.inherit = False
    if rounded:
        try:
            shape.adjustments[0] = radius
        except (IndexError, KeyError):
            pass
    return _noline(shape)


def card(slide, left, top, width, height, accent=None):
    """A surface panel with a hairline ring and an optional accent top-rule."""
    shape = rect(slide, left, top, width, height, SURFACE, rounded=True, radius=0.03)
    shape.line.color.rgb = HAIRLINE
    shape.line.width = Pt(1)
    if accent is not None:
        rule = rect(slide, left + 0.001, top, min(width, 1.15), 0.055, accent)
        rule.shadow.inherit = False
    return shape


def textbox(slide, left, top, width, height, anchor=MSO_ANCHOR.TOP):
    box = slide.shapes.add_textbox(
        Inches(left), Inches(top), Inches(width), Inches(height)
    )
    tf = box.text_frame
    tf.word_wrap = True
    tf.margin_left = tf.margin_right = tf.margin_top = tf.margin_bottom = 0
    tf.vertical_anchor = anchor
    return tf


def write(tf, lines, first=True):
    """
    lines: list of dicts {text, size, color, bold, space_after, font, align, indent}
    """
    for i, spec in enumerate(lines):
        para = tf.paragraphs[0] if (first and i == 0) else tf.add_paragraph()
        para.text = spec.get("text", "")
        para.alignment = spec.get("align", PP_ALIGN.LEFT)
        if spec.get("indent"):
            para.level = spec["indent"]
        f = para.font
        f.name = spec.get("font", SANS)
        f.size = Pt(spec.get("size", 12))
        f.bold = spec.get("bold", False)
        f.color.rgb = spec.get("color", INK_PRIMARY)
        para.space_after = Pt(spec.get("space_after", 6))
        para.line_spacing = spec.get("line_spacing", 1.0)
    return tf


def _est_height(lines, box_w):
    """
    Estimate rendered height (inches) for a list of line specs in a box of
    box_w inches. Proportional sans averages ~0.50em per char, mono ~0.60em.
    Deliberately slightly pessimistic — better to shrink early than clip.
    """
    total = 0.0
    for spec in lines:
        text = spec.get("text", "") or ""
        size = spec.get("size", 12)
        mono = spec.get("font", SANS) == MONO
        adv = (0.60 if mono else 0.50) * size / 72.0
        per_line = max(1, int(box_w / adv)) if adv > 0 else 1
        n = max(1, -(-len(text) // per_line))
        lh = size * 1.2 * spec.get("line_spacing", 1.0) / 72.0
        total += n * lh + spec.get("space_after", 6) / 72.0
    return total


def write_fit(tf, lines, box_w, box_h, min_scale=0.60):
    """
    write(), but first shrink every font size (and the gaps) by a uniform
    factor until the estimated height fits box_h. Keeps relative hierarchy
    intact instead of clipping the tail of a card.
    """
    scale = 1.0
    while scale > min_scale and _est_height(lines, box_w) > box_h:
        scale -= 0.04
        lines = [
            dict(
                s,
                size=s.get("size", 12) * 0.96,
                space_after=s.get("space_after", 6) * 0.94,
            )
            for s in lines
        ]
    return write(tf, lines)


def background(slide):
    rect(slide, 0, 0, SLIDE_W, SLIDE_H, PAGE_PLANE)


def header(slide, title, kicker, number, total):
    """Kicker pill + title + a hairline rule + slide number."""
    if kicker:
        k = kicker.upper()
        pill_w = min(max(1.45, 0.093 * len(k) + 0.42), 6.2)
        pill = rect(slide, MARGIN, 0.46, pill_w, 0.30, SURFACE_RAISED,
                    rounded=True, radius=0.5)
        pill.line.color.rgb = BASELINE
        pill.line.width = Pt(0.75)
        ptf = pill.text_frame
        ptf.margin_left = ptf.margin_right = Inches(0.1)
        ptf.margin_top = ptf.margin_bottom = 0
        ptf.vertical_anchor = MSO_ANCHOR.MIDDLE
        write(ptf, [{
            "text": k, "size": 8.5, "bold": True,
            "color": SLOT[0], "align": PP_ALIGN.CENTER, "space_after": 0,
        }])

    size = 26 if len(title) <= 58 else (22 if len(title) <= 82 else 19)
    tf = textbox(slide, MARGIN, 0.90, CONTENT_W - 0.7, 0.72)
    write(tf, [{"text": title, "size": size, "bold": True,
                "color": INK_PRIMARY, "space_after": 0}])

    rule = rect(slide, MARGIN, CONTENT_TOP - 0.22, CONTENT_W, 0.012, BASELINE)
    rule.shadow.inherit = False

    ntf = textbox(slide, SLIDE_W - MARGIN - 0.9, 0.50, 0.9, 0.3)
    write(ntf, [{"text": f"{number} / {total}", "size": 9,
                 "color": INK_MUTED, "align": PP_ALIGN.RIGHT, "space_after": 0}])


def notes(slide, text):
    if text:
        slide.notes_slide.notes_text_frame.text = text


# ---------------------------------------------------------------------------
# layouts
# ---------------------------------------------------------------------------

def layout_title(slide, spec, ctx):
    background(slide)
    # accent bar
    rect(slide, 0, 0, SLIDE_W, 0.11, SLOT[0])

    tf = textbox(slide, 1.05, 2.05, 11.2, 3.6)
    lines = [
        {"text": spec.get("kicker", "").upper(), "size": 12.5, "bold": True,
         "color": SLOT[0], "space_after": 20},
        {"text": ctx["deckTitle"], "size": 40, "bold": True,
         "color": INK_PRIMARY, "space_after": 16, "line_spacing": 1.05},
    ]
    if ctx.get("subtitle"):
        lines.append({"text": ctx["subtitle"], "size": 15.5,
                      "color": INK_SECONDARY, "space_after": 0,
                      "line_spacing": 1.25})
    write(tf, lines)

    rect(slide, 1.05, 6.05, 2.0, 0.028, SLOT[2])
    ftf = textbox(slide, 1.05, 6.25, 11.2, 0.4)
    write(ftf, [{"text": spec.get("footer", "Modernization Factory · Playback Session"),
                 "size": 11, "bold": True, "color": INK_MUTED, "space_after": 0}])
    notes(slide, spec.get("speakerNotes"))


def _card_grid(slide, cards, top, height):
    n = max(1, len(cards))
    gap = 0.28
    width = (CONTENT_W - gap * (n - 1)) / n

    # scale type to the densest card
    max_bullets = max((len(c.get("bullets") or []) for c in cards), default=0)
    body = 12.0
    if max_bullets >= 5 or n >= 4:
        body = 10.5
    if max_bullets >= 7:
        body = 9.5
    head = 14.5 if n >= 4 else 15.5

    for i, c in enumerate(cards):
        left = MARGIN + i * (width + gap)
        accent = SLOT[i % len(SLOT)]
        card(slide, left, top, width, height, accent=accent)

        tf = textbox(slide, left + 0.28, top + 0.32, width - 0.56, height - 0.6)
        lines = [{"text": c.get("heading", ""), "size": head, "bold": True,
                  "color": accent, "space_after": 11, "line_spacing": 1.1}]
        for b in (c.get("bullets") or []):
            lines.append({"text": b, "size": body, "color": INK_SECONDARY,
                          "space_after": 7, "line_spacing": 1.18})
        write_fit(tf, lines, width - 0.56, height - 0.6)


def layout_cards(slide, spec, ctx):
    background(slide)
    header(slide, spec["title"], spec.get("kicker"), spec["number"], ctx["total"])
    cards = spec.get("cards") or []
    if cards:
        _card_grid(slide, cards, CONTENT_TOP, CONTENT_H)
    notes(slide, spec.get("speakerNotes"))


def layout_two_column(slide, spec, ctx):
    """Two cards, first tinted critical (legacy), second good (modern)."""
    background(slide)
    header(slide, spec["title"], spec.get("kicker"), spec["number"], ctx["total"])
    cards = (spec.get("cards") or [])[:2]
    if not cards:
        notes(slide, spec.get("speakerNotes"))
        return

    gap = 0.34
    width = (CONTENT_W - gap) / 2
    accents = [CRITICAL, GOOD]
    for i, c in enumerate(cards):
        left = MARGIN + i * (width + gap)
        accent = accents[i]
        card(slide, left, CONTENT_TOP, width, CONTENT_H, accent=accent)
        tf = textbox(slide, left + 0.3, CONTENT_TOP + 0.34, width - 0.6, CONTENT_H - 0.62)
        lines = [{"text": c.get("heading", ""), "size": 16, "bold": True,
                  "color": accent, "space_after": 12}]
        bullets = c.get("bullets") or []
        size = 12.0 if len(bullets) <= 5 else (11.0 if len(bullets) <= 7 else 10.0)
        for b in bullets:
            lines.append({"text": "— " + b, "size": size, "color": INK_SECONDARY,
                          "space_after": 8, "line_spacing": 1.2})
        write_fit(tf, lines, width - 0.6, CONTENT_H - 0.62)
    notes(slide, spec.get("speakerNotes"))


def layout_table(slide, spec, ctx):
    background(slide)
    header(slide, spec["title"], spec.get("kicker"), spec["number"], ctx["total"])
    rows = spec.get("tableRows") or []
    if not rows:
        notes(slide, spec.get("speakerNotes"))
        return

    n_rows, n_cols = len(rows), max(len(r) for r in rows)
    height = CONTENT_H
    row_h = height / n_rows
    shape = slide.shapes.add_table(
        n_rows, n_cols, Inches(MARGIN), Inches(CONTENT_TOP),
        Inches(CONTENT_W), Inches(height),
    )
    table = shape.table
    table.first_row = True
    table.horz_banding = False

    body = 11.0 if n_cols <= 4 else (10.0 if n_cols == 5 else 9.0)

    for r, row in enumerate(rows):
        table.rows[r].height = Inches(row_h)
        for c in range(n_cols):
            cell = table.cell(r, c)
            cell.text = row[c] if c < len(row) else ""
            cell.fill.solid()
            cell.fill.fore_color.rgb = SURFACE_RAISED if r == 0 else SURFACE
            cell.margin_left = cell.margin_right = Inches(0.13)
            cell.margin_top = cell.margin_bottom = Inches(0.05)
            cell.vertical_anchor = MSO_ANCHOR.MIDDLE
            for para in cell.text_frame.paragraphs:
                para.font.name = SANS
                para.font.size = Pt(body if r else body + 0.5)
                para.font.bold = bool(r == 0)
                para.font.color.rgb = SLOT[0] if r == 0 else INK_SECONDARY
                para.space_after = Pt(0)
    notes(slide, spec.get("speakerNotes"))


def layout_code(slide, spec, ctx):
    background(slide)
    header(slide, spec["title"], spec.get("kicker"), spec["number"], ctx["total"])

    code = (spec.get("codeBlock") or "").rstrip("\n")
    lines = code.split("\n") if code else []
    cards = spec.get("cards") or []

    if cards:
        code_w = CONTENT_W * 0.60
        side_w = CONTENT_W - code_w - 0.3
    else:
        code_w, side_w = CONTENT_W, 0

    panel = card(slide, MARGIN, CONTENT_TOP, code_w, CONTENT_H, accent=SLOT[0])

    size = 11.0
    if len(lines) > 12:
        size = 9.5
    if len(lines) > 18:
        size = 8.5
    longest = max((len(l) for l in lines), default=0)
    if longest > 78:
        size = min(size, 8.5)

    cap = spec.get("codeCaption")
    tf = textbox(slide, MARGIN + 0.26, CONTENT_TOP + 0.3, code_w - 0.5, CONTENT_H - 0.55)
    body = []
    if cap:
        body.append({"text": cap, "size": 10, "bold": True,
                     "color": INK_MUTED, "font": MONO, "space_after": 12})
    for line in lines:
        body.append({"text": line if line.strip() else " ", "size": size,
                     "color": INK_SECONDARY, "font": MONO,
                     "space_after": 1.5, "line_spacing": 1.08})
    write(tf, body)

    if cards:
        left = MARGIN + code_w + 0.3
        each_h = (CONTENT_H - 0.24 * (len(cards) - 1)) / len(cards)
        for i, c in enumerate(cards):
            top = CONTENT_TOP + i * (each_h + 0.24)
            accent = SLOT[(i + 1) % len(SLOT)]
            card(slide, left, top, side_w, each_h, accent=accent)
            ctf = textbox(slide, left + 0.24, top + 0.28, side_w - 0.48, each_h - 0.5)
            cl = [{"text": c.get("heading", ""), "size": 13, "bold": True,
                   "color": accent, "space_after": 9}]
            for b in (c.get("bullets") or []):
                cl.append({"text": b, "size": 10, "color": INK_SECONDARY,
                           "space_after": 6, "line_spacing": 1.16})
            write_fit(ctf, cl, side_w - 0.48, each_h - 0.5)
    notes(slide, spec.get("speakerNotes"))


def layout_metrics(slide, spec, ctx):
    """
    KPI row of stat tiles. Per the stat-tile contract: label in sentence case,
    value in the same sans (proportional figures), no decorative face.
    """
    background(slide)
    header(slide, spec["title"], spec.get("kicker"), spec["number"], ctx["total"])

    metrics = spec.get("metrics") or []
    cards = spec.get("cards") or []

    if metrics:
        if len(metrics) > 6:
            print(f"  ! slide {spec.get('number')}: {len(metrics)} metrics, "
                  f"rendering first 6 ({len(metrics) - 6} dropped)")
        n = min(len(metrics), 6)
        metrics = metrics[:n]
        gap = 0.24 if n >= 5 else 0.28
        width = (CONTENT_W - gap * (n - 1)) / n

        # The value must sit on ONE line: pick the largest size that fits the
        # tile width, then size the tile to value + label.  Digits in a bold
        # sans average ~0.56em.
        inner = width - 0.48
        vsizes = []
        for m in metrics:
            txt = str(m.get("value", "")) or " "
            fit = (inner / (0.56 * max(1, len(txt)))) * 72.0
            vsizes.append(max(15.0, min(34.0 if n < 5 else 30.0, fit)))
        vsize_common = min(vsizes)          # one size for the whole row

        label_h = max(
            _est_height([{"text": m.get("label", ""), "size": 9.5,
                          "space_after": 0, "line_spacing": 1.18}], inner)
            for m in metrics
        )
        value_h = vsize_common * 1.2 / 72.0
        tile_h = (1.62 if cards
                  else max(1.45, min(CONTENT_H, value_h + label_h + 0.72)))

        tile_top = CONTENT_TOP if cards else CONTENT_TOP + (CONTENT_H - tile_h) / 3
        for i, m in enumerate(metrics):
            left = MARGIN + i * (width + gap)
            accent = SLOT[i % len(SLOT)]
            card(slide, left, tile_top, width, tile_h, accent=accent)

            value = str(m.get("value", ""))
            tf = textbox(slide, left + 0.24, tile_top + 0.30, inner, tile_h - 0.46)
            write(tf, [
                {"text": value, "size": vsize_common, "bold": True,
                 "color": INK_PRIMARY, "space_after": 9},
                {"text": m.get("label", ""), "size": 9.5,
                 "color": INK_MUTED, "space_after": 0, "line_spacing": 1.18},
            ])

        if cards:
            top = tile_top + tile_h + 0.26
            _card_grid(slide, cards, top, CONTENT_TOP + CONTENT_H - top)
    elif cards:
        _card_grid(slide, cards, CONTENT_TOP, CONTENT_H)

    notes(slide, spec.get("speakerNotes"))


def layout_screenshot(slide, spec, ctx):
    background(slide)
    header(slide, spec["title"], spec.get("kicker"), spec["number"], ctx["total"])

    name = spec.get("screenshot")
    path = os.path.join(SCREENSHOT_DIR, name) if name else None
    has_img = bool(path and os.path.exists(path))
    cards = spec.get("cards") or []

    if has_img and cards:
        side_w = CONTENT_W * (0.40 if len(cards) >= 3 else 0.34)
        img_w = CONTENT_W - side_w - 0.3
        img_left = MARGIN + side_w + 0.3

        each_h = (CONTENT_H - 0.24 * (len(cards) - 1)) / len(cards)
        for i, c in enumerate(cards):
            top = CONTENT_TOP + i * (each_h + 0.24)
            accent = SLOT[i % len(SLOT)]
            card(slide, MARGIN, top, side_w, each_h, accent=accent)
            ctf = textbox(slide, MARGIN + 0.24, top + 0.26, side_w - 0.48, each_h - 0.48)
            cl = [{"text": c.get("heading", ""), "size": 13, "bold": True,
                   "color": accent, "space_after": 9}]
            for b in (c.get("bullets") or []):
                cl.append({"text": b, "size": 10, "color": INK_SECONDARY,
                           "space_after": 6, "line_spacing": 1.16})
            write_fit(ctf, cl, side_w - 0.48, each_h - 0.5)
        _place_image(slide, path, img_left, CONTENT_TOP, img_w, CONTENT_H)
    elif has_img:
        _place_image(slide, path, MARGIN, CONTENT_TOP, CONTENT_W, CONTENT_H)
    elif cards:
        _card_grid(slide, cards, CONTENT_TOP, CONTENT_H)

    notes(slide, spec.get("speakerNotes"))


def _place_image(slide, path, left, top, box_w, box_h):
    """Contain-fit the image in the box, centered, on a hairline panel."""
    from PIL import Image  # noqa: WPS433
    try:
        with Image.open(path) as im:
            iw, ih = im.size
    except Exception:
        iw, ih = (16, 9)

    scale = min(box_w / iw, box_h / ih)
    w, h = iw * scale, ih * scale
    x = left + (box_w - w) / 2
    y = top + (box_h - h) / 2

    frame = rect(slide, x - 0.05, y - 0.05, w + 0.10, h + 0.10, SURFACE,
                 rounded=True, radius=0.02)
    frame.line.color.rgb = BASELINE
    frame.line.width = Pt(1)
    slide.shapes.add_picture(path, Inches(x), Inches(y),
                             width=Inches(w), height=Inches(h))


def layout_diagram(slide, spec, ctx):
    """
    Horizontal flow of stages built from cards: heading = stage, bullets = detail.
    Chevrons between stages.
    """
    background(slide)
    header(slide, spec["title"], spec.get("kicker"), spec["number"], ctx["total"])

    cards = spec.get("cards") or []
    if not cards:
        notes(slide, spec.get("speakerNotes"))
        return
    if len(cards) > 5:
        return layout_cards(slide, spec, ctx)

    n = len(cards)
    arrow = 0.34
    width = (CONTENT_W - arrow * (n - 1)) / n
    height = min(CONTENT_H, 3.9)
    top = CONTENT_TOP + (CONTENT_H - height) / 2

    for i, c in enumerate(cards):
        left = MARGIN + i * (width + arrow)
        accent = SLOT[i % len(SLOT)]
        card(slide, left, top, width, height, accent=accent)

        tf = textbox(slide, left + 0.24, top + 0.34, width - 0.48, height - 0.6)
        lines = [
            {"text": f"0{i + 1}", "size": 10, "bold": True,
             "color": accent, "space_after": 8},
            {"text": c.get("heading", ""), "size": 14, "bold": True,
             "color": INK_PRIMARY, "space_after": 10, "line_spacing": 1.12},
        ]
        for b in (c.get("bullets") or []):
            lines.append({"text": b, "size": 10, "color": INK_SECONDARY,
                          "space_after": 6, "line_spacing": 1.16})
        write_fit(tf, lines, width - 0.48, height - 0.6)

        if i < n - 1:
            ch = slide.shapes.add_shape(
                MSO_SHAPE.CHEVRON,
                Inches(left + width + 0.055), Inches(top + height / 2 - 0.12),
                Inches(arrow - 0.11), Inches(0.24),
            )
            ch.fill.solid()
            ch.fill.fore_color.rgb = BASELINE
            ch.shadow.inherit = False
            _noline(ch)

    notes(slide, spec.get("speakerNotes"))


LAYOUTS = {
    "title": layout_title,
    "cards": layout_cards,
    "two-column": layout_two_column,
    "table": layout_table,
    "code": layout_code,
    "metrics": layout_metrics,
    "screenshot": layout_screenshot,
    "diagram": layout_diagram,
}


# ---------------------------------------------------------------------------
# driver
# ---------------------------------------------------------------------------

def build(deck, out_path):
    prs = Presentation()
    prs.slide_width = Inches(SLIDE_W)
    prs.slide_height = Inches(SLIDE_H)
    blank = prs.slide_layouts[6]

    slides = deck.get("slides") or []
    ctx = {
        "deckTitle": deck.get("deckTitle", "Untitled"),
        "subtitle": deck.get("subtitle", ""),
        "total": len(slides),
    }

    warnings = []
    for i, spec in enumerate(slides, 1):
        spec = dict(spec)
        spec.setdefault("number", i)
        layout_name = spec.get("layout", "cards")
        fn = LAYOUTS.get(layout_name)
        if fn is None:
            warnings.append(f"slide {i}: unknown layout '{layout_name}', using cards")
            fn = layout_cards
        if layout_name == "screenshot":
            name = spec.get("screenshot")
            if name and not os.path.exists(os.path.join(SCREENSHOT_DIR, name)):
                warnings.append(f"slide {i}: screenshot '{name}' not found")
        if not spec.get("speakerNotes"):
            warnings.append(f"slide {i}: no speaker notes")
        fn(prs.slides.add_slide(blank), spec, ctx)

    os.makedirs(os.path.dirname(os.path.abspath(out_path)), exist_ok=True)
    prs.save(out_path)
    return warnings


def main():
    if len(sys.argv) != 3:
        print(__doc__)
        return 2
    with open(sys.argv[1]) as fh:
        deck = json.load(fh)
    warnings = build(deck, sys.argv[2])

    n = len(deck.get("slides") or [])
    print(f"Wrote {sys.argv[2]}  ({n} slides)")
    if warnings:
        print(f"\n{len(warnings)} warning(s):")
        for w in warnings:
            print(f"  - {w}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
