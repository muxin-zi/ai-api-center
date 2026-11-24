# -*- coding: utf-8 -*-
"""AI API 中心图标 v2：深色底 + 蓝紫渐变圆环 + 中心 AI 字标

比 v1 更克制：去掉铺满整块的渐变和下方横线，把视觉重量集中在一个环上。
小尺寸下环和字都能立住。
"""
import os
from PIL import Image, ImageDraw, ImageFont

HERE = os.path.dirname(os.path.abspath(__file__))
S = 2048  # 超采样画布

BLUE = (108, 140, 255)
PURPLE = (168, 85, 247)
BG_DARK = (10, 13, 20)


def lerp(a, b, t):
    return tuple(int(a[i] + (b[i] - a[i]) * t) for i in range(3))


def diag_gradient(c1, c2):
    """左上 → 右下的对角线渐变"""
    g = Image.new("RGB", (S, S))
    gd = ImageDraw.Draw(g)
    for i in range(S * 2):
        t = i / float(S * 2 - 1)
        gd.line([(i, 0), (0, i)], fill=lerp(c1, c2, t), width=2)
    return g


def make_master():
    cx = cy = S // 2
    img = Image.new("RGBA", (S, S), (0, 0, 0, 0))

    # 1. 深色圆角底
    r = int(S * 0.228)
    ImageDraw.Draw(img).rounded_rectangle(
        [0, 0, S - 1, S - 1], radius=r, fill=BG_DARK + (255,))

    # 2. 环
    outer = int(S * 0.305)
    ring_w = int(S * 0.082)
    inner = outer - ring_w

    ring = Image.new("L", (S, S), 0)
    rd = ImageDraw.Draw(ring)
    rd.ellipse([cx - outer, cy - outer, cx + outer, cy + outer], fill=255)
    rd.ellipse([cx - inner, cy - inner, cx + inner, cy + inner], fill=0)

    img.paste(diag_gradient(BLUE, PURPLE), (0, 0), ring)

    # 3. 环内侧一圈很淡的高光，让环看起来有厚度
    halo = Image.new("L", (S, S), 0)
    hd = ImageDraw.Draw(halo)
    hd.ellipse([cx - inner - 6, cy - inner - 6, cx + inner + 6, cy + inner + 6], fill=26)
    hd.ellipse([cx - inner + 4, cy - inner + 4, cx + inner - 4, cy + inner - 4], fill=0)
    img.paste(Image.new("RGB", (S, S), (150, 175, 255)), (0, 0), halo)

    # 4. 中心 AI
    d = ImageDraw.Draw(img)
    fpath = "C:/Windows/Fonts/arialbd.ttf"
    fs = int(S * 0.235)
    font = ImageFont.truetype(fpath, fs) if os.path.exists(fpath) \
        else ImageFont.load_default()

    txt = "AI"
    bbox = d.textbbox((0, 0), txt, font=font)
    tw, th = bbox[2] - bbox[0], bbox[3] - bbox[1]
    tx = cx - tw // 2 - bbox[0]
    ty = cy - th // 2 - bbox[1]

    # 白色字，底部略偏冷
    tmask = Image.new("L", (S, S), 0)
    ImageDraw.Draw(tmask).text((tx, ty), txt, font=font, fill=255)

    tgrad = Image.new("RGB", (S, S))
    tgd = ImageDraw.Draw(tgrad)
    for y in range(S):
        tgd.line([(0, y), (S, y)], fill=lerp((255, 255, 255), (214, 222, 255), y / float(S - 1)))
    img.paste(tgrad, (0, 0), tmask)

    return img


SIZES = {
    "mipmap-mdpi": 48,
    "mipmap-hdpi": 72,
    "mipmap-xhdpi": 96,
    "mipmap-xxhdpi": 144,
    "mipmap-xxxhdpi": 192,
}


def main(write_res=False):
    master = make_master()
    master.resize((432, 432), Image.LANCZOS).save(
        os.path.join(HERE, "icon_preview_v2.png"), "PNG")
    print("preview -> icon_preview_v2.png")

    if not write_res:
        return

    res = os.path.join(HERE, "app", "src", "main", "res")
    for folder, px in SIZES.items():
        p = os.path.join(res, folder)
        os.makedirs(p, exist_ok=True)
        master.resize((px, px), Image.LANCZOS).save(
            os.path.join(p, "ic_launcher.png"), "PNG")
        print("wrote", folder, px)


if __name__ == "__main__":
    import sys
    main("--write" in sys.argv)
