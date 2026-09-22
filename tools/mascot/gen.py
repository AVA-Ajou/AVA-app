"""Avamon 마스코트 벡터 생성기. 같은 부품으로 SVG(미리보기)와 VectorDrawable(앱)을 뽑는다."""
import os, sys

OUT = os.path.dirname(os.path.abspath(__file__))

# 원본 PNG에서 샘플링한 팔레트
BODY_HI = "#CDB4FF"   # 위쪽 하이라이트
BODY_MID = "#A78BF5"  # 중간
BODY_LO = "#7C5CE0"   # 아래 그늘
BODY_EDGE = "#6A4BD0"
INK = "#1E1736"
BLUSH = "#F49BC0"
TONGUE = "#F06E92"
SHIELD_IN = "#DDD4FF"
CHECK = "#6D4FDE"
RED = "#F04452"
AMBER = "#FFB020"
SKY = "#7CC4FF"

# ── 부품 ──────────────────────────────────────────────────────────────────

def body(lean=0):
    # 위가 좁고 아래가 넓은 젤리 몸통. 아래는 발 두 개로 갈라진다. lean 은 좌우 기울기(px)
    l = lean
    return (f"M{100+l},18 C{150+l},18 178,70 180,132 C181,164 172,182 136,184 "
            f"C120,185 108,180 100,180 C92,180 80,185 64,184 C28,182 19,164 20,132 "
            f"C22,70 {50+l},18 {100+l},18 Z")

def antennae(tilt=0):
    t = tilt
    # 정수리 한 점(100,26)에서 양쪽으로 벌어지는 새싹 두 잎
    left = (f"M100,28 C96,20 90,8 80,{4+t} C70,{0+t} 62,{8+t} 66,{16+t} "
            f"C72,{26+t} 88,30 100,34 Z")
    right = (f"M100,28 C104,20 110,8 120,{4+t} C130,{0+t} 138,{8+t} 134,{16+t} "
             f"C128,{26+t} 112,30 100,34 Z")
    return left, right

def eye(cx, cy, rx=11, ry=13, closed=False, squint=False):
    if closed:
        # 감은 눈: 아래로 볼록한 호
        return [("path", dict(d=f"M{cx-10},{cy} Q{cx},{cy+9} {cx+10},{cy}", stroke=INK, sw=4.5, fill="none"))]
    if squint:
        return [("path", dict(d=f"M{cx-10},{cy+3} Q{cx},{cy-8} {cx+10},{cy+3}", stroke=INK, sw=4.5, fill="none"))]
    return [("ellipse", dict(cx=cx, cy=cy, rx=rx, ry=ry, fill=INK)),
            ("circle", dict(cx=cx-4, cy=cy-5, r=4, fill="#FFFFFF")),
            ("circle", dict(cx=cx+3, cy=cy+4, r=1.4, fill="#FFFFFF", opacity=0.8))]

def blush(cx, cy, opacity=0.85):
    return ("ellipse", dict(cx=cx, cy=cy, rx=11, ry=6.5, fill=BLUSH, opacity=opacity))

def mouth_smile():
    return [("path", dict(d="M80,106 C86,132 114,132 120,106 Q100,112 80,106 Z", fill="#3A1F5E")),
            ("path", dict(d="M88,118 Q100,132 112,118 Q100,124 88,118 Z", fill=TONGUE)),
            ("rect", dict(x=108, y=107, w=6, h=6, rx=2, fill="#FFFFFF"))]

def mouth_small():
    return [("path", dict(d="M92,110 Q100,118 108,110", stroke=INK, sw=4, fill="none"))]

def mouth_worried():
    return [("path", dict(d="M90,116 Q100,106 110,116", stroke=INK, sw=4, fill="none"))]

def mouth_o():
    return [("ellipse", dict(cx=100, cy=113, rx=6, ry=7.5, fill="#3A1F5E"))]

def brows(angry=False):
    if angry:
        return [("path", dict(d="M60,70 L80,78", stroke=INK, sw=4, fill="none")),
                ("path", dict(d="M140,70 L120,78", stroke=INK, sw=4, fill="none"))]
    return [("path", dict(d="M62,74 L82,68", stroke=INK, sw=4, fill="none")),
            ("path", dict(d="M138,74 L118,68", stroke=INK, sw=4, fill="none"))]

def arm(cx, cy, rot, rx=22, ry=19):
    return ("ellipse", dict(cx=cx, cy=cy, rx=rx, ry=ry, fill="url(#armGrad)", rot=rot))

def hand(cx, cy):
    return ("ellipse", dict(cx=cx, cy=cy, rx=12, ry=10, fill="url(#armGrad)"))

def shield(cx=100, cy=150, s=1.0, check=True, color=None, inner=None, glyph=None):
    # 방패 외곽(흰) + 안쪽(연보라) + 체크
    def P(x, y): return f"{cx+x*s},{cy+y*s}"
    outer = (f"M{P(0,-34)} L{P(30,-22)} C{P(30,10)} {P(18,30)} {P(0,40)} "
             f"C{P(-18,30)} {P(-30,10)} {P(-30,-22)} Z")
    inner_p = (f"M{P(0,-27)} L{P(23,-17)} C{P(23,8)} {P(14,24)} {P(0,32)} "
               f"C{P(-14,24)} {P(-23,8)} {P(-23,-17)} Z")
    items = [("path", dict(d=outer, fill="#FFFFFF")),
             ("path", dict(d=inner_p, fill=inner or SHIELD_IN))]
    if glyph == "check":
        items.append(("path", dict(d=f"M{P(-12,2)} L{P(-4,10)} L{P(13,-8)}", stroke=color or CHECK, sw=7*s, fill="none", cap="round")))
    elif glyph == "bang":
        items.append(("path", dict(d=f"M{P(0,-12)} L{P(0,8)}", stroke=color or RED, sw=7*s, fill="none", cap="round")))
        items.append(("circle", dict(cx=cx, cy=cy+18*s, r=4*s, fill=color or RED)))
    return items

def magnifier(cx, cy, r=18):
    return [("path", dict(d=f"M{cx+r*0.7},{cy+r*0.7} L{cx+r*1.6},{cy+r*1.6}", stroke=CHECK, sw=9, fill="none", cap="round")),
            ("circle", dict(cx=cx, cy=cy, r=r, fill="#FFFFFF")),
            ("circle", dict(cx=cx, cy=cy, r=r, fill="none", stroke=CHECK, sw=6)),
            ("circle", dict(cx=cx, cy=cy, r=r-4, fill=SKY, opacity=0.35)),
            ("path", dict(d=f"M{cx-9},{cy-4} Q{cx-4},{cy-11} {cx+2},{cy-9}", stroke="#FFFFFF", sw=3.5, fill="none", cap="round"))]

def sweat(cx, cy):
    return [("path", dict(d=f"M{cx},{cy-10} C{cx+8},{cy} {cx+7},{cy+8} {cx},{cy+8} C{cx-7},{cy+8} {cx-8},{cy} {cx},{cy-10} Z", fill=SKY)),
            ("circle", dict(cx=cx-2, cy=cy, r=1.8, fill="#FFFFFF", opacity=0.9))]

def zzz(x, y):
    return [("text", dict(x=x, y=y, s=18, t="z", fill=CHECK, opacity=0.9)),
            ("text", dict(x=x+14, y=y-16, s=14, t="z", fill=CHECK, opacity=0.7)),
            ("text", dict(x=x+25, y=y-29, s=10, t="z", fill=CHECK, opacity=0.5))]

def sparkle(cx, cy, r=7, color=AMBER):
    return [("path", dict(d=f"M{cx},{cy-r} Q{cx},{cy} {cx+r},{cy} Q{cx},{cy} {cx},{cy+r} Q{cx},{cy} {cx-r},{cy} Q{cx},{cy} {cx},{cy-r} Z", fill=color))]

def shadow():
    return ("ellipse", dict(cx=100, cy=188, rx=62, ry=6, fill="#3A2A6E", opacity=0.10))

def feet():
    return [("ellipse", dict(cx=70, cy=182, rx=22, ry=9, fill="url(#armGrad)")),
            ("ellipse", dict(cx=130, cy=182, rx=22, ry=9, fill="url(#armGrad)"))]

# ── 포즈 ──────────────────────────────────────────────────────────────────

def pose_guard():
    a1, a2 = antennae()
    return [shadow(), *feet(),
            ("path", dict(d=a1, fill="url(#antGrad)")), ("path", dict(d=a2, fill="url(#antGrad)")),
            ("path", dict(d=body(), fill="url(#bodyGrad)")),
            *eye(70, 88), *eye(130, 88), blush(46, 110), blush(154, 110), *mouth_smile(),
            arm(42, 150, -20, 24, 22), arm(158, 150, 20, 24, 22),
            *shield(100, 152, 1.25, glyph="check"),
            hand(68, 166), hand(132, 166)]

def pose_alert():
    a1, a2 = antennae(tilt=-4)
    return [shadow(), *feet(),
            ("path", dict(d=a1, fill="url(#antGrad)")), ("path", dict(d=a2, fill="url(#antGrad)")),
            ("path", dict(d=body(), fill="url(#bodyGrad)")),
            *brows(), *eye(70, 92, 10, 12), *eye(130, 92, 10, 12), blush(46, 112, 0.6), blush(154, 112, 0.6), *mouth_worried(),
            *sweat(166, 66),
            arm(42, 150, -20, 24, 22), arm(158, 150, 20, 24, 22),
            *shield(100, 152, 1.25, glyph="bang", inner="#FFE3E5"),
            hand(68, 166), hand(132, 166)]

def pose_sleep():
    a1, a2 = antennae(tilt=6)
    return [shadow(), *feet(),
            ("path", dict(d=a1, fill="url(#antGrad)")), ("path", dict(d=a2, fill="url(#antGrad)")),
            ("path", dict(d=body(), fill="url(#bodyGrad)")),
            *eye(70, 92, closed=True), *eye(130, 92, closed=True), blush(46, 110), blush(154, 110), *mouth_small(),
            arm(46, 158, -8, 26, 20), arm(154, 158, 8, 26, 20),
            hand(82, 170), hand(118, 170),
            *zzz(150, 56)]

def pose_search():
    a1, a2 = antennae()
    return [shadow(), *feet(),
            ("path", dict(d=a1, fill="url(#antGrad)")), ("path", dict(d=a2, fill="url(#antGrad)")),
            ("path", dict(d=body(), fill="url(#bodyGrad)")),
            *eye(70, 88, 10, 12), *eye(130, 88, 12, 14), blush(46, 110), blush(154, 110), *mouth_small(),
            arm(42, 150, -20, 24, 22),
            arm(158, 140, 30, 24, 20),
            *magnifier(168, 112, 22),
            hand(150, 138),
            hand(68, 166)]

def pose_wave():
    a1, a2 = antennae()
    return [shadow(), *feet(),
            ("path", dict(d=a1, fill="url(#antGrad)")), ("path", dict(d=a2, fill="url(#antGrad)")),
            ("path", dict(d=body(), fill="url(#bodyGrad)")),
            *eye(70, 88, squint=True), *eye(130, 88, squint=True), blush(46, 108), blush(154, 108), *mouth_smile(),
            arm(42, 150, -20, 24, 22),
            arm(166, 118, 65, 26, 18), hand(184, 92),
            hand(68, 166),
            *sparkle(36, 44, 6), *sparkle(178, 40, 8), *sparkle(18, 96, 4)]

POSES = {"guard": pose_guard, "alert": pose_alert, "sleep": pose_sleep, "search": pose_search, "wave": pose_wave}

# ── 출력: SVG ─────────────────────────────────────────────────────────────

def svg(items):
    defs = f'''<defs>
<radialGradient id="bodyGrad" cx="0.36" cy="0.28" r="0.85"><stop offset="0" stop-color="{BODY_HI}"/><stop offset="0.55" stop-color="{BODY_MID}"/><stop offset="1" stop-color="{BODY_LO}"/></radialGradient>
<radialGradient id="armGrad" cx="0.35" cy="0.3" r="0.9"><stop offset="0" stop-color="{BODY_MID}"/><stop offset="1" stop-color="{BODY_LO}"/></radialGradient>
<linearGradient id="antGrad" x1="0" y1="1" x2="0" y2="0"><stop offset="0" stop-color="{BODY_MID}"/><stop offset="1" stop-color="{BODY_HI}"/></linearGradient>
</defs>'''
    out = [f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 200 200" width="600" height="600">', defs]
    for kind, a in items:
        op = f' opacity="{a["opacity"]}"' if "opacity" in a else ""
        if kind == "path":
            stroke = f' stroke="{a["stroke"]}" stroke-width="{a["sw"]}" stroke-linecap="{a.get("cap","round")}" stroke-linejoin="round"' if "stroke" in a else ""
            out.append(f'<path d="{a["d"]}" fill="{a.get("fill","none")}"{stroke}{op}/>')
        elif kind == "ellipse":
            tr = f' transform="rotate({a["rot"]} {a["cx"]} {a["cy"]})"' if "rot" in a else ""
            out.append(f'<ellipse cx="{a["cx"]}" cy="{a["cy"]}" rx="{a["rx"]}" ry="{a["ry"]}" fill="{a["fill"]}"{tr}{op}/>')
        elif kind == "circle":
            stroke = f' stroke="{a["stroke"]}" stroke-width="{a["sw"]}"' if "stroke" in a else ""
            out.append(f'<circle cx="{a["cx"]}" cy="{a["cy"]}" r="{a["r"]}" fill="{a["fill"]}"{stroke}{op}/>')
        elif kind == "rect":
            out.append(f'<rect x="{a["x"]}" y="{a["y"]}" width="{a["w"]}" height="{a["h"]}" rx="{a["rx"]}" fill="{a["fill"]}"{op}/>')
        elif kind == "text":
            out.append(f'<text x="{a["x"]}" y="{a["y"]}" font-family="Pretendard, sans-serif" font-weight="800" font-size="{a["s"]}" fill="{a["fill"]}"{op}>{a["t"]}</text>')
    out.append("</svg>")
    return "\n".join(out)

# ── 출력: VectorDrawable ─────────────────────────────────────────────────

import math

def ellipse_path(cx, cy, rx, ry, rot=0):
    # 타원을 4개의 호로. 회전은 group 으로 처리한다.
    return (f"M{cx-rx},{cy} A{rx},{ry} 0 1 0 {cx+rx},{cy} A{rx},{ry} 0 1 0 {cx-rx},{cy} Z")

def circle_path(cx, cy, r):
    return ellipse_path(cx, cy, r, r)

def rect_path(x, y, w, h, rx):
    return (f"M{x+rx},{y} L{x+w-rx},{y} Q{x+w},{y} {x+w},{y+rx} L{x+w},{y+h-rx} Q{x+w},{y+h} {x+w-rx},{y+h} "
            f"L{x+rx},{y+h} Q{x},{y+h} {x},{y+h-rx} L{x},{y+rx} Q{x},{y} {x+rx},{y} Z")

def z_path(x, y, s):
    # 글자 z 를 획으로 — 벡터 드로어블에는 텍스트가 없다
    w, h = s*0.55, s*0.6
    return f"M{x},{y-h} L{x+w},{y-h} L{x},{y} L{x+w},{y}"

def vd_fill(a, alpha_attr=True):
    fill = a.get("fill", "none")
    op = a.get("opacity")
    if fill in ("url(#bodyGrad)", "url(#armGrad)", "url(#antGrad)"):
        return None, fill  # 그라데이션은 aapt:attr 로
    if fill == "none":
        return "", None
    s = f' android:fillColor="{fill}"'
    if op is not None:
        s += f' android:fillAlpha="{op}"'
    return s, None

GRADS = {
    "url(#bodyGrad)": f'''<gradient android:type="radial" android:centerX="72" android:centerY="56" android:gradientRadius="170">
                    <item android:offset="0" android:color="{BODY_HI}"/>
                    <item android:offset="0.55" android:color="{BODY_MID}"/>
                    <item android:offset="1" android:color="{BODY_LO}"/>
                </gradient>''',
    "url(#armGrad)": f'''<gradient android:type="radial" android:centerX="@CX@" android:centerY="@CY@" android:gradientRadius="@R@">
                    <item android:offset="0" android:color="{BODY_MID}"/>
                    <item android:offset="1" android:color="{BODY_LO}"/>
                </gradient>''',
    "url(#antGrad)": f'''<gradient android:type="linear" android:startX="100" android:startY="42" android:endX="100" android:endY="10">
                    <item android:offset="0" android:color="{BODY_MID}"/>
                    <item android:offset="1" android:color="{BODY_HI}"/>
                </gradient>''',
}

def vector_drawable(items, name):
    out = ['<?xml version="1.0" encoding="utf-8"?>',
           f'<!-- Avamon 마스코트 `{name}` 포즈. tools/mascot/gen.py 가 생성한다 — 손으로 고치지 말고 생성기를 고칠 것. -->',
           '<vector xmlns:android="http://schemas.android.com/apk/res/android"',
           '    xmlns:aapt="http://schemas.android.com/aapt"',
           '    android:width="200dp"', '    android:height="200dp"',
           '    android:viewportWidth="200"', '    android:viewportHeight="200">']
    for kind, a in items:
        if kind == "path":
            d = a["d"]
        elif kind == "ellipse":
            d = ellipse_path(a["cx"], a["cy"], a["rx"], a["ry"])
        elif kind == "circle":
            d = circle_path(a["cx"], a["cy"], a["r"])
        elif kind == "rect":
            d = rect_path(a["x"], a["y"], a["w"], a["h"], a["rx"])
        elif kind == "text":
            d = z_path(a["x"], a["y"], a["s"])
            a = dict(a, stroke=a["fill"], sw=max(2, a["s"]*0.14), fill="none")
        fill_attr, grad = vd_fill(a)
        stroke = ""
        if "stroke" in a:
            stroke = (f' android:strokeColor="{a["stroke"]}" android:strokeWidth="{a["sw"]}"'
                      f' android:strokeLineCap="{a.get("cap","round")}" android:strokeLineJoin="round"')
            if "opacity" in a:
                stroke += f' android:strokeAlpha="{a["opacity"]}"'
        rot = a.get("rot")
        if rot:
            out.append(f'    <group android:rotation="{rot}" android:pivotX="{a["cx"]}" android:pivotY="{a["cy"]}">')
            ind = "        "
        else:
            ind = "    "
        if grad:
            g = GRADS[grad]
            if grad == "url(#armGrad)":
                g = g.replace("@CX@", str(a["cx"]-a["rx"]*0.3)).replace("@CY@", str(a["cy"]-a["ry"]*0.4)).replace("@R@", str(a["rx"]*1.8))
            out.append(f'{ind}<path android:pathData="{d}">')
            out.append(f'{ind}    <aapt:attr name="android:fillColor">')
            out.append(f'{ind}        {g}')
            out.append(f'{ind}    </aapt:attr>')
            out.append(f'{ind}</path>')
        else:
            out.append(f'{ind}<path android:pathData="{d}"{fill_attr}{stroke}/>')
        if rot:
            out.append('    </group>')
    out.append('</vector>')
    return "\n".join(out)

if __name__ == "__main__":
    target = sys.argv[1] if len(sys.argv) > 1 else None
    html = ['<html><body style="background:#F3F3F8;font-family:sans-serif;display:flex;gap:24px;flex-wrap:wrap;padding:24px">']
    for name, fn in POSES.items():
        items = fn()
        s = svg(items)
        open(f"{OUT}/{name}.svg", "w").write(s)
        html.append(f'<div style="text-align:center"><div style="background:#fff;border-radius:20px;padding:12px">{s}</div><p>{name}</p></div>')
        if target:
            open(f"{target}/ic_avamon_{name}.xml", "w").write(vector_drawable(items, name))
    html.append('<div style="text-align:center"><img src="orig.png" width="224" style="border-radius:20px"/><p>original</p></div></body></html>')
    open(f"{OUT}/index.html", "w").write("\n".join(html))
    print("ok")
