"""guard.svg 를 640px 로 래스터화해 원본과 나란히·반투명 겹침·차이맵으로 비교한다."""
import os, subprocess
import numpy as np
from PIL import Image, ImageChops

HERE = os.path.dirname(os.path.abspath(__file__))
os.chdir(HERE)
for f in os.listdir("."):
    if f.endswith(".svg.png"):
        os.remove(f)
subprocess.run(["qlmanage", "-t", "-s", "640", "-o", ".", "guard.svg", "alert.svg", "sleep.svg", "search.svg", "wave.svg"],
               capture_output=True)

def load(name):
    im = Image.open(name).convert("RGBA")
    return im

orig = load("reference.png")
mine = load("guard.svg.png")
if mine.size != orig.size:
    mine = mine.resize(orig.size)
white = Image.new("RGBA", orig.size, (255, 255, 255, 255))
o = Image.alpha_composite(white, orig).convert("RGB")
m = Image.alpha_composite(white, mine).convert("RGB")
blend = Image.blend(o, m, 0.5)
diff = ImageChops.difference(o, m)
d = np.array(diff).mean(axis=2)
print("mean abs diff (0-255):", round(float(d.mean()), 2))
# 실루엣 IoU
# qlmanage 는 알파를 흰색으로 눌러 버리므로 "흰색이 아닌 픽셀"을 실루엣으로 본다 (둘 다 같은 기준)
def sil(img):
    x = np.array(img); return ~((x[:, :, 0] > 240) & (x[:, :, 1] > 240) & (x[:, :, 2] > 240))
oa = sil(o); ma = sil(m)
print("silhouette IoU:", round(float((oa & ma).sum() / (oa | ma).sum()), 4))

sheet = Image.new("RGB", (640 * 4 + 50, 640 + 20), "white")
for i, im in enumerate([o, m, blend, diff]):
    sheet.paste(im, (10 + i * 650, 10))
sheet.save("compare.png")

poses = ["guard", "alert", "sleep", "search", "wave"]
ims = [Image.alpha_composite(white, load(p + ".svg.png").resize(orig.size)).convert("RGB") for p in poses] + [o]
ps = Image.new("RGB", (640 * 6 + 70, 660), "white")
for i, im in enumerate(ims):
    ps.paste(im, (10 + i * 650, 10))
ps.save("poses.png")
print("ok")
