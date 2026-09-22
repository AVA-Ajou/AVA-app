"""Avamon 마스코트 벡터 생성기.

원본 PNG(`reference.png`, 640×640 — 팀이 만든 마스코트)에서 **실루엣을 직접 추출**하고 이목구비·방패·팔의 위치와 색을
픽셀로 측정해 그린다. 눈대중으로 그린 v1은 비율이 달랐다.

출력: SVG(미리보기) + Android VectorDrawable(앱). 좌표계는 200×200 (원본 ×0.3125).
"""
import os, sys, math
import numpy as np
from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
S = 200 / 640  # 원본 → viewBox

# ── 원본에서 잰 색 ──────────────────────────────────────────────────────────
BODY_TOP = "#C4A0FB"   # 머리 위 하이라이트
BODY_MID = "#B893F8"   # 몸통 중간
BODY_SIDE = "#A98AF3"  # 옆면
BODY_LOW = "#7A5CD0"   # 아랫부분 그늘
BODY_EDGE = "#6E52C8"
ANT_TIP = "#C29FFA"
ANT_BASE = "#8362D7"
INK = "#161133"
BLUSH = "#FCA3C3"
TONGUE = "#F77A9D"
SHIELD_RIM = "#FFFFFF"
SHIELD_IN_TOP = "#E1D5FC"
SHIELD_IN_BOT = "#D5C9FA"
CHECK = "#7B60E3"
ARM_HI = "#B08FF6"
ARM_LO = "#7A5CD0"
RED = "#F04452"
SKY = "#7CC4FF"
AMBER = "#FFB020"

# ── 실루엣 추출 ─────────────────────────────────────────────────────────────

REFERENCE = os.path.join(HERE, "reference.png")

def load_mask():
    a = np.array(Image.open(REFERENCE).convert("RGBA"))
    return a[:, :, 3] > 128

def contour_polar(mask, n=96):
    """마스크의 외곽을 무게중심 기준 각도로 n개 샘플링. 볼록에 가까운 덩어리에 쓴다."""
    ys, xs = np.where(mask)
    cx, cy = xs.mean(), ys.mean()
    # 경계 픽셀
    up = np.zeros_like(mask); up[1:] = mask[:-1]
    dn = np.zeros_like(mask); dn[:-1] = mask[1:]
    lf = np.zeros_like(mask); lf[:, 1:] = mask[:, :-1]
    rt = np.zeros_like(mask); rt[:, :-1] = mask[:, 1:]
    edge = mask & ~(up & dn & lf & rt)
    ey, ex = np.where(edge)
    ang = np.arctan2(ey - cy, ex - cx)
    rad = np.hypot(ex - cx, ey - cy)
    pts = []
    for i in range(n):
        a0 = -math.pi + 2 * math.pi * i / n
        a1 = a0 + 2 * math.pi / n
        sel = (ang >= a0) & (ang < a1)
        if sel.sum() == 0:
            continue
        # 같은 각도 구간에서 가장 바깥 점
        j = np.argmax(rad[sel])
        pts.append((ex[sel][j], ey[sel][j]))
    return pts

def smooth_path(pts, scale=S, tension=0.5):
    """Catmull-Rom → 닫힌 cubic bezier 경로."""
    n = len(pts)
    P = [(x * scale, y * scale) for x, y in pts]
    d = [f"M{P[0][0]:.1f},{P[0][1]:.1f}"]
    for i in range(n):
        p0, p1, p2, p3 = P[(i - 1) % n], P[i], P[(i + 1) % n], P[(i + 2) % n]
        c1 = (p1[0] + (p2[0] - p0[0]) * tension / 3, p1[1] + (p2[1] - p0[1]) * tension / 3)
        c2 = (p2[0] - (p3[0] - p1[0]) * tension / 3, p2[1] - (p3[1] - p1[1]) * tension / 3)
        d.append(f"C{c1[0]:.1f},{c1[1]:.1f} {c2[0]:.1f},{c2[1]:.1f} {p2[0]:.1f},{p2[1]:.1f}")
    d.append("Z")
    return " ".join(d)

def components(mask):
    seen = np.zeros_like(mask, bool); out = []
    H, W = mask.shape
    for y in range(H):
        for x in range(W):
            if mask[y, x] and not seen[y, x]:
                stack = [(y, x)]; seen[y, x] = True; comp = np.zeros_like(mask, bool)
                while stack:
                    cy, cx = stack.pop(); comp[cy, cx] = True
                    for ny, nx in ((cy - 1, cx), (cy + 1, cx), (cy, cx - 1), (cy, cx + 1)):
                        if 0 <= ny < H and 0 <= nx < W and mask[ny, nx] and not seen[ny, nx]:
                            seen[ny, nx] = True; stack.append((ny, nx))
                if comp.sum() > 200:
                    out.append(comp)
    return out

MASK = load_mask()
# 두 더듬이는 y=52 까지 떨어져 있다가 y≈56 에서 정수리(x≈290)로 합쳐진다 — 행별 구간 측정값.
ANT_SPLIT = 54
body_mask = MASK.copy(); body_mask[:ANT_SPLIT] = False
BODY_PATH = smooth_path(contour_polar(body_mask, 140), tension=0.6)

def lobe_masks():
    top = MASK.copy(); top[ANT_SPLIT:] = False
    out = []
    for c in components(top):
        ys, xs = np.where(c)
        # 밑동을 정수리 안쪽(y≈72)까지 늘려 머리와 이음새 없이 겹치게 한다
        bottom = ys.max()
        row = np.where(c[bottom])[0]
        x0, x1 = row.min(), row.max()
        ext = c.copy()
        for y in range(bottom + 1, 74):
            lo = int(x0 + (y - bottom) * 0.6); hi = int(x1 - (y - bottom) * 0.6)
            ext[y, lo:hi] = MASK[y, lo:hi]
        out.append(ext)
    return out

ANT_PATHS = [smooth_path(contour_polar(c, 48), tension=0.6) for c in lobe_masks()]

# ── 측정한 좌표 (원본 px × S) ────────────────────────────────────────────────
def s(v): return round(v * S, 1)

EYE_L, EYE_R = (s(220), s(251)), (s(421), s(250))
EYE_RX, EYE_RY = s(27), s(36)
HL_L, HL_R = (s(214), s(231)), (s(412), s(231))
BLUSH_L, BLUSH_R = (s(172), s(293)), (s(469), s(293))
BLUSH_RX, BLUSH_RY = s(27), s(20)
MOUTH_X0, MOUTH_X1, MOUTH_Y0, MOUTH_Y1 = s(263), s(376), s(276), s(318)
SHIELD_C = (s(317), s(481))
SHIELD_W, SHIELD_H = s(261), s(287)

# ── 부품 ──────────────────────────────────────────────────────────────────

def E(cx, cy, rx, ry, fill, **k): return ("ellipse", dict(cx=cx, cy=cy, rx=rx, ry=ry, fill=fill, **k))
def C(cx, cy, r, fill, **k): return ("circle", dict(cx=cx, cy=cy, r=r, fill=fill, **k))
def Pth(d, fill="none", **k): return ("path", dict(d=d, fill=fill, **k))

def body_layers():
    # 바탕 그라데이션 + 가장자리 어둡게 + 위쪽 광택
    return [Pth(BODY_PATH, "url(#bodyGrad)"),
            Pth(BODY_PATH, "url(#edgeGrad)"),
            E(s(300), s(150), s(120), s(70), "url(#sheen)")]

def antennae():
    return [Pth(p, "url(#antGrad)") for p in ANT_PATHS]

def eyes(kind="open", scale=1.0):
    out = []
    for (cx, cy), (hx, hy) in ((EYE_L, HL_L), (EYE_R, HL_R)):
        if kind == "open":
            out += [E(cx, cy, EYE_RX * scale, EYE_RY * scale, INK),
                    C(hx, hy, s(10) * scale, "#FFFFFF"),
                    C(cx + s(8), cy + s(12), s(4), "#FFFFFF", opacity=0.85)]
        elif kind == "closed":
            out += [Pth(f"M{cx-9},{cy} Q{cx},{cy+8} {cx+9},{cy}", stroke=INK, sw=4.2)]
        elif kind == "happy":
            out += [Pth(f"M{cx-9},{cy+3} Q{cx},{cy-8} {cx+9},{cy+3}", stroke=INK, sw=4.2)]
    return out

def blush(opacity=1.0):
    return [E(*BLUSH_L, BLUSH_RX, BLUSH_RY, BLUSH, opacity=opacity),
            E(*BLUSH_R, BLUSH_RX, BLUSH_RY, BLUSH, opacity=opacity)]

def mouth_smile():
    x0, x1, y0, y1 = MOUTH_X0, MOUTH_X1, MOUTH_Y0, MOUTH_Y1
    xm = (x0 + x1) / 2
    # 윗입술은 살짝 아래로 휜 호, 아래는 둥근 U
    dep = (y1 - y0) * 0.45
    d = f"M{x0},{y0} Q{xm},{y0+3} {x1},{y0} C{x1},{y1+dep} {x0},{y1+dep} {x0},{y0} Z"
    return [Pth(d, "#3A1F5E"),
            Pth(f"M{xm-9},{y1-6} Q{xm},{y1+5} {xm+9},{y1-6} Q{xm},{y1-3} {xm-9},{y1-6} Z", TONGUE),
            ("rect", dict(x=s(346), y=s(295), w=s(20), h=s(22), rx=2, fill="#FDFDFD"))]

def mouth_small():
    xm = (MOUTH_X0 + MOUTH_X1) / 2
    return [Pth(f"M{xm-8},{MOUTH_Y0+4} Q{xm},{MOUTH_Y0+12} {xm+8},{MOUTH_Y0+4}", stroke=INK, sw=4)]

def mouth_worried():
    xm = (MOUTH_X0 + MOUTH_X1) / 2
    return [Pth(f"M{xm-9},{MOUTH_Y0+12} Q{xm},{MOUTH_Y0+2} {xm+9},{MOUTH_Y0+12}", stroke=INK, sw=4)]

def brows():
    lx, rx = EYE_L[0], EYE_R[0]; y = EYE_L[1] - EYE_RY - 6
    return [Pth(f"M{lx-10},{y+4} L{lx+8},{y-2}", stroke=INK, sw=3.8),
            Pth(f"M{rx+10},{y+4} L{rx-8},{y-2}", stroke=INK, sw=3.8)]

def shield(cx=None, cy=None, k=1.0, glyph="check", inner_top=None, inner_bot=None, glyph_color=None):
    cx = SHIELD_C[0] if cx is None else cx; cy = SHIELD_C[1] if cy is None else cy
    w, h = SHIELD_W * k / 2, SHIELD_H * k / 2
    def P(x, y): return f"{cx+x*w:.1f},{cy+y*h:.1f}"
    outer = (f"M{P(0,-1)} C{P(0.45,-0.86)} {P(0.8,-0.82)} {P(1,-0.72)} "
             f"C{P(1.02,-0.1)} {P(0.75,0.6)} {P(0,1)} "
             f"C{P(-0.75,0.6)} {P(-1.02,-0.1)} {P(-1,-0.72)} "
             f"C{P(-0.8,-0.82)} {P(-0.45,-0.86)} {P(0,-1)} Z")
    r = 0.78
    inner = (f"M{P(0,-r)} C{P(0.45*r,-0.86*r)} {P(0.8*r,-0.82*r)} {P(r,-0.72*r)} "
             f"C{P(1.02*r,-0.1*r)} {P(0.75*r,0.6*r)} {P(0,r)} "
             f"C{P(-0.75*r,0.6*r)} {P(-1.02*r,-0.1*r)} {P(-r,-0.72*r)} "
             f"C{P(-0.8*r,-0.82*r)} {P(-0.45*r,-0.86*r)} {P(0,-r)} Z")
    items = [Pth(outer, SHIELD_RIM), Pth(inner, "url(#shieldGrad)")]
    if inner_top:
        items[1] = Pth(inner, inner_top)
    if glyph == "check":
        # 원본 체크: x 264~377, y 444~531 (중심 321,490), 굵기 ≈ 26px
        items.append(Pth(f"M{P(-0.40,0.02)} L{P(-0.10,0.30)} L{P(0.44,-0.28)}",
                         stroke=glyph_color or CHECK, sw=s(26) * k, cap="round"))
    elif glyph == "bang":
        items.append(Pth(f"M{P(0,-0.42)} L{P(0,0.18)}", stroke=glyph_color or RED, sw=s(26) * k, cap="round"))
        items.append(C(cx, cy + 0.48 * h, s(15) * k, glyph_color or RED))
    return items

def limb(cx, cy, rx, ry, rot, inward=1, shadow=True):
    """팔·손 덩어리. 원본의 팔은 배보다 한 톤 어두운 독립된 둥근 부피라 자체 조명을 준다.
    그림자는 원형 후광이 아니라 **안쪽·아래로만** 밀어 넣는다 — 팔이 몸 위에 얹힌 자리에만
    그늘이 생긴다. inward 는 그림자를 미는 방향(+1: 오른쪽으로, -1: 왼쪽으로)."""
    # 몸 밖으로 뻗은 팔(든 팔)에는 그림자를 깔지 않는다 — 실루엣 밖으로 새어 나와 얼룩이 된다.
    sh = [E(cx + inward * rx * 0.18, cy + ry * 0.25, rx * 1.02, ry * 0.95, "#3F2A8A", opacity=0.12, rot=rot),
          E(cx + inward * rx * 0.10, cy + ry * 0.14, rx * 0.98, ry * 0.92, "#3F2A8A", opacity=0.12, rot=rot)] if shadow else []
    return sh + [E(cx, cy, rx, ry, "url(#armGrad)", rot=rot)]

def arms_holding():
    # 어깨에서 손까지 이어진 팔 한 짝. 어깨 공과 손 공을 따로 두면 두 덩어리로 갈라져 보인다.
    return [*limb(s(148), s(470), s(60), s(120), -6, inward=1),
            *limb(s(492), s(472), s(60), s(120), 6, inward=-1)]

def shield_shadow():
    return [E(s(317), s(560), s(150), s(70), "#4A3298", opacity=0.28)]

def hands_holding():
    return [*limb(s(140), s(567), s(70), s(60), -15, inward=1),
            *limb(s(499), s(567), s(70), s(60), 15, inward=-1)]

def shadow():
    return [E(s(320), s(632), s(210), s(14), "#3A2A6E", opacity=0.10)]

def sweat(cx, cy):
    return [Pth(f"M{cx},{cy-10} C{cx+8},{cy} {cx+7},{cy+8} {cx},{cy+8} C{cx-7},{cy+8} {cx-8},{cy} {cx},{cy-10} Z", SKY),
            C(cx - 2, cy, 1.8, "#FFFFFF", opacity=0.9)]

def zzz(x, y):
    return [("text", dict(x=x, y=y, s=18, t="z", fill=CHECK, opacity=0.9)),
            ("text", dict(x=x + 14, y=y - 16, s=14, t="z", fill=CHECK, opacity=0.7)),
            ("text", dict(x=x + 25, y=y - 29, s=10, t="z", fill=CHECK, opacity=0.5))]

def sparkle(cx, cy, r=7, color=AMBER):
    return [Pth(f"M{cx},{cy-r} Q{cx},{cy} {cx+r},{cy} Q{cx},{cy} {cx},{cy+r} Q{cx},{cy} {cx-r},{cy} Q{cx},{cy} {cx},{cy-r} Z", color)]

def magnifier(cx, cy, r=18):
    return [Pth(f"M{cx+r*0.7},{cy+r*0.7} L{cx+r*1.55},{cy+r*1.55}", stroke=CHECK, sw=9, cap="round"),
            C(cx, cy, r, "#FFFFFF"), C(cx, cy, r, "none", stroke=CHECK, sw=6),
            C(cx, cy, r - 4, SKY, opacity=0.35),
            Pth(f"M{cx-9},{cy-4} Q{cx-4},{cy-11} {cx+2},{cy-9}", stroke="#FFFFFF", sw=3.5, cap="round")]

# ── 포즈 ──────────────────────────────────────────────────────────────────

def pose_guard():
    return [*shadow(), *antennae(), *body_layers(), *eyes(), *blush(), *mouth_smile(),
            *arms_holding(), *shield_shadow(), *shield(), *hands_holding()]

def pose_alert():
    return [*shadow(), *antennae(), *body_layers(), *brows(), *eyes(scale=0.9), *blush(0.7), *mouth_worried(),
            *sweat(s(510), s(200)),
            *arms_holding(), *shield_shadow(), *shield(glyph="bang", inner_top="#FFE3E5"), *hands_holding()]

def arm_left_rest():
    return limb(s(148), s(470), s(60), s(120), -6, inward=1)

def arm_right_rest():
    return limb(s(492), s(472), s(60), s(120), 6, inward=-1)

def pose_sleep():
    # 두 팔을 내리고 손을 배 앞에 모은다. 손끝은 좌우가 살짝 겹친다.
    return [*shadow(), *antennae(), *body_layers(), *eyes("closed"), *blush(), *mouth_small(),
            *arm_left_rest(), *arm_right_rest(),
            *limb(s(262), s(590), s(66), s(50), -6, inward=1),
            *limb(s(378), s(590), s(66), s(50), 6, inward=-1),
            *zzz(s(470), s(150))]

def pose_search():
    # 오른팔을 들어 돋보기를 든다. 팔은 어깨(490,400)에서 손(560,330)까지 한 덩어리.
    return [*shadow(), *antennae(), *body_layers(),
            *eyes(), *blush(), *mouth_small(),
            *arm_left_rest(),
            *limb(s(262), s(590), s(66), s(50), -6, inward=1),
            *limb(s(540), s(415), s(54), s(100), 52, inward=-1, shadow=False),
            *magnifier(s(530), s(268), s(58)),
            *limb(s(596), s(372), s(44), s(38), 30, inward=-1, shadow=False)]

def pose_wave():
    # 오른팔을 머리 옆까지 들어 흔든다.
    return [*shadow(), *antennae(), *body_layers(),
            *eyes("happy"), *blush(), *mouth_smile(),
            *arm_left_rest(),
            *limb(s(262), s(590), s(66), s(50), -6, inward=1),
            *limb(s(538), s(360), s(54), s(125), 40, inward=-1, shadow=False),
            *limb(s(612), s(238), s(42), s(38), 0, inward=-1, shadow=False),
            *sparkle(s(110), s(150), 6), *sparkle(s(560), s(90), 8), *sparkle(s(70), s(260), 4)]

POSES = {"guard": pose_guard, "alert": pose_alert, "sleep": pose_sleep, "search": pose_search, "wave": pose_wave}

# ── 그라데이션 정의 (SVG / VectorDrawable 공용 좌표: viewBox 200) ─────────────

GRADS = {
    "bodyGrad": dict(type="radial", cx=s(255), cy=s(120), r=s(640),
                     stops=[(0, "#C7A7FB"), (0.28, "#BC98F8"), (0.55, "#AC8CF4"), (0.8, "#9273E6"), (1, "#7052C8")]),
    "edgeGrad": dict(type="radial", cx=s(285), cy=s(265), r=s(360),
                     stops=[(0.55, "#00000000"), (0.82, "#5A40B030"), (1, "#3F2A8A9A")]),
    "bottomShade": dict(type="linear", x1=s(320), y1=s(440), x2=s(320), y2=s(640),
                        stops=[(0, "#00000000"), (1, "#4A3298A0")]),
    "sheen": dict(type="radial", cx=s(300), cy=s(150), r=s(120),
                  stops=[(0, "#FFFFFF33"), (1, "#FFFFFF00")]),
    "antGrad": dict(type="linear", x1=s(280), y1=s(75), x2=s(280), y2=s(0),
                    stops=[(0, ANT_BASE), (0.45, "#A785EE"), (1, ANT_TIP)]),
    "armGrad": dict(type="radial", cx=None, cy=None, r=None,
                    stops=[(0, "#BB98F7"), (0.5, "#A283EF"), (0.85, "#8A6BE0"), (1, "#7A5CD0")]),
    "shieldGrad": dict(type="linear", x1=s(317), y1=s(380), x2=s(317), y2=s(590),
                       stops=[(0, SHIELD_IN_TOP), (1, SHIELD_IN_BOT)]),
}

def hex_to_svg(c):
    # #RRGGBBAA → color + opacity
    if len(c) == 9:
        return c[:7], int(c[7:], 16) / 255
    return c, 1.0

def svg(items, size=640):
    defs = ["<defs>"]
    for name, g in GRADS.items():
        if name == "armGrad":
            continue
        stops = "".join(f'<stop offset="{o}" stop-color="{hex_to_svg(c)[0]}" stop-opacity="{hex_to_svg(c)[1]:.3f}"/>' for o, c in g["stops"])
        if g["type"] == "radial":
            defs.append(f'<radialGradient id="{name}" gradientUnits="userSpaceOnUse" cx="{g["cx"]}" cy="{g["cy"]}" r="{g["r"]}">{stops}</radialGradient>')
        else:
            defs.append(f'<linearGradient id="{name}" gradientUnits="userSpaceOnUse" x1="{g["x1"]}" y1="{g["y1"]}" x2="{g["x2"]}" y2="{g["y2"]}">{stops}</linearGradient>')
    out = [f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 200 200" width="{size}" height="{size}">']
    body = []
    armn = 0
    for kind, a in items:
        op = f' opacity="{a["opacity"]}"' if "opacity" in a else ""
        fill = a.get("fill", "none")
        if fill == "url(#armGrad)":
            armn += 1
            gid = f"arm{armn}"
            g = GRADS["armGrad"]
            stops = "".join(f'<stop offset="{o}" stop-color="{c}"/>' for o, c in g["stops"])
            defs.append(f'<radialGradient id="{gid}" gradientUnits="userSpaceOnUse" cx="{a["cx"]-a["rx"]*0.35:.1f}" cy="{a["cy"]-a["ry"]*0.45:.1f}" r="{a["rx"]*1.7:.1f}">{stops}</radialGradient>')
            fill = f"url(#{gid})"
        if kind == "path":
            stroke = f' stroke="{a["stroke"]}" stroke-width="{a["sw"]}" stroke-linecap="{a.get("cap","round")}" stroke-linejoin="round"' if "stroke" in a else ""
            body.append(f'<path d="{a["d"]}" fill="{fill}"{stroke}{op}/>')
        elif kind == "ellipse":
            tr = f' transform="rotate({a["rot"]} {a["cx"]} {a["cy"]})"' if "rot" in a else ""
            body.append(f'<ellipse cx="{a["cx"]}" cy="{a["cy"]}" rx="{a["rx"]}" ry="{a["ry"]}" fill="{fill}"{tr}{op}/>')
        elif kind == "circle":
            stroke = f' stroke="{a["stroke"]}" stroke-width="{a["sw"]}"' if "stroke" in a else ""
            body.append(f'<circle cx="{a["cx"]}" cy="{a["cy"]}" r="{a["r"]}" fill="{fill}"{stroke}{op}/>')
        elif kind == "rect":
            body.append(f'<rect x="{a["x"]}" y="{a["y"]}" width="{a["w"]}" height="{a["h"]}" rx="{a["rx"]}" fill="{fill}"{op}/>')
        elif kind == "text":
            body.append(f'<text x="{a["x"]}" y="{a["y"]}" font-family="Pretendard, sans-serif" font-weight="800" font-size="{a["s"]}" fill="{fill}"{op}>{a["t"]}</text>')
    defs.append("</defs>")
    return "\n".join(out + defs + body + ["</svg>"])

# ── VectorDrawable ───────────────────────────────────────────────────────────

def ellipse_path(cx, cy, rx, ry):
    return f"M{cx-rx:.1f},{cy} A{rx},{ry} 0 1 0 {cx+rx:.1f},{cy} A{rx},{ry} 0 1 0 {cx-rx:.1f},{cy} Z"

def rect_path(x, y, w, h, rx):
    return (f"M{x+rx},{y} L{x+w-rx},{y} Q{x+w},{y} {x+w},{y+rx} L{x+w},{y+h-rx} Q{x+w},{y+h} {x+w-rx},{y+h} "
            f"L{x+rx},{y+h} Q{x},{y+h} {x},{y+h-rx} L{x},{y+rx} Q{x},{y} {x+rx},{y} Z")

def z_path(x, y, sz):
    w, h = sz * 0.55, sz * 0.6
    return f"M{x},{y-h} L{x+w},{y-h} L{x},{y} L{x+w},{y}"

def vd_gradient(g, cx=None, cy=None, r=None):
    def col(c):
        if len(c) == 9:  # #RRGGBBAA → #AARRGGBB
            return "#" + c[7:] + c[1:7]
        return c
    items = "".join(f'\n                    <item android:offset="{o}" android:color="{col(c)}"/>' for o, c in g["stops"])
    if g["type"] == "radial":
        return (f'<gradient android:type="radial" android:centerX="{cx if cx is not None else g["cx"]}" '
                f'android:centerY="{cy if cy is not None else g["cy"]}" android:gradientRadius="{r if r is not None else g["r"]}">{items}\n                </gradient>')
    return (f'<gradient android:type="linear" android:startX="{g["x1"]}" android:startY="{g["y1"]}" '
            f'android:endX="{g["x2"]}" android:endY="{g["y2"]}">{items}\n                </gradient>')

def vector_drawable(items, name):
    out = ['<?xml version="1.0" encoding="utf-8"?>',
           f'<!-- Avamon 마스코트 `{name}` 포즈. tools/mascot/gen.py 가 원본 PNG에서 실루엣과 색을 재어 생성한다.',
           '     손으로 고치지 말고 생성기를 고칠 것. -->',
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
            d = ellipse_path(a["cx"], a["cy"], a["r"], a["r"])
        elif kind == "rect":
            d = rect_path(a["x"], a["y"], a["w"], a["h"], a["rx"])
        elif kind == "text":
            d = z_path(a["x"], a["y"], a["s"])
            a = dict(a, stroke=a["fill"], sw=max(2, a["s"] * 0.14), fill="none")
        fill = a.get("fill", "none")
        stroke = ""
        if "stroke" in a:
            stroke = (f' android:strokeColor="{a["stroke"]}" android:strokeWidth="{a["sw"]}"'
                      f' android:strokeLineCap="{a.get("cap","round")}" android:strokeLineJoin="round"')
            if "opacity" in a:
                stroke += f' android:strokeAlpha="{a["opacity"]}"'
        rot = a.get("rot")
        ind = "    "
        if rot:
            out.append(f'    <group android:rotation="{rot}" android:pivotX="{a["cx"]}" android:pivotY="{a["cy"]}">')
            ind = "        "
        if fill.startswith("url(#"):
            gname = fill[5:-1]
            if gname == "armGrad":
                g = vd_gradient(GRADS["armGrad"], round(a["cx"] - a["rx"] * 0.35, 1), round(a["cy"] - a["ry"] * 0.45, 1), round(a["rx"] * 1.7, 1))
            else:
                g = vd_gradient(GRADS[gname])
            out += [f'{ind}<path android:pathData="{d}">', f'{ind}    <aapt:attr name="android:fillColor">',
                    f'{ind}        {g}', f'{ind}    </aapt:attr>', f'{ind}</path>']
        else:
            fa = ""
            if fill != "none":
                fa = f' android:fillColor="{fill}"'
                if "opacity" in a:
                    fa += f' android:fillAlpha="{a["opacity"]}"'
            out.append(f'{ind}<path android:pathData="{d}"{fa}{stroke}/>')
        if rot:
            out.append('    </group>')
    out.append('</vector>')
    return "\n".join(out)

if __name__ == "__main__":
    target = sys.argv[1] if len(sys.argv) > 1 else None
    for name, fn in POSES.items():
        items = fn()
        open(f"{HERE}/{name}.svg", "w").write(svg(items))
        if target:
            open(f"{target}/ic_avamon_{name}.xml", "w").write(vector_drawable(items, name))
    print("ok")
