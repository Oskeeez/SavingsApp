"""Mirrors the app's room rendering (cover-scaled scene, shelf, lighting, contact shadows) so it can be inspected
without a device. Usage: python3 preview_room.py out.png [screenW screenH] [nolight]"""
import sys, math, re
import numpy as np
from PIL import Image, ImageDraw, ImageFont, ImageFilter

R = 'app/src/main/res/drawable-nodpi/'
SCENE_W, SCENE_H, WALL_H = 664, 1186, 764
S = 0.92
SHELF_W, SHELF_H = 491, 659
LEFT = (SCENE_W - SHELF_W * S) / 2
BOTTOM = 806.0
TOP = BOTTOM - SHELF_H * S
STAND = [42, 178, 313, 445, 590]
CEIL = [-1, 64, 204, 341, 480]
LN = [44, 61, 61, 62, 67]
RN = [448, 429, 429, 429, 429]
nolight = 'nolight' in sys.argv

def scene_y(n): return TOP + n * S
def scene_x(n): return LEFT + n * S
def stand(t): return scene_y(STAND[t])
def comp_h(t): return 100.0 if t == 0 else (STAND[t] - CEIL[t]) * S
def slot_x(t, i): return scene_x(LN[t]) + (scene_x(RN[t]) - scene_x(LN[t])) / 4 * (i + .5)

# zone lighting copied from ShelfLighting.kt
ZONES = [(1.04, 0.015, 0.24, 2.4, 0.6, 0.75), (1.0, 0.025, 0.28, 2.6, 0.7, 0.7), (0.97, 0.032, 0.31, 2.8, 0.8, 0.65), (0.945, 0.04, 0.34, 3.0, 0.9, 0.6), (0.92, 0.048, 0.36, 3.2, 1.0, 0.55)]
PROFILES = {'clock':(.85,4.5,.9),'film_camera':(.9,5,1.0),'gacha':(1.08,10,1.2),'jar':(1.0,8,1.1),'cat_calico':(1.08,8,.95),
            'books_stack':(.9,3.5,1.0),'books_standing':(.9,3.5,1.0),'anthurium':(1.05,8,.9),'monstera':(1.05,8,.9),'bonsai':(1.05,8,.9),
            'mushroom_lamp':(.95,6,.95),'rabbit':(1.05,7,.95),'world_globe':(.92,6,1.0),'flower_vase':(1.05,8,.9),'framed_landscape':(.9,4,.9),
            'wooden_house':(.92,6,1.0),'moon_lamp':(.95,6,.95),'pothos_potted':(1.05,8,.9),'basket':(.92,6,1.0),'snow_globe':(.92,6,1.0)}
ASPECT = {}
def art(key):
    names = {'clock':'item_desk_clock','gacha':'item_gacha_machine','jar':'jar_state_5'}
    n = names.get(key, 'item_' + key)
    return Image.open(R + n + '.png').convert('RGBA')

def light(key, tier, xfrac):
    b, w, so, dx, dy, bl = ZONES[tier]
    prof = PROFILES.get(key, (.95, 6, 1.0))
    side = max(-1, min(1, (xfrac * SCENE_W - LEFT) / (SHELF_W * S) * 2 - 1))
    return dict(b=b * (1 - .028 * side), w=w * (1 - .18 * side), so=min(.5, so * prof[2] * (1 + .06 * side)), ws=prof[0], th=prof[1], dx=dx, dy=dy, bl=bl)

def tint(img, l):
    a = np.array(img).astype(float)
    r = l['b'] * (1 + l['w'] * .7); g = l['b'] * (1 + l['w'] * .1); b = l['b'] * (1 - l['w'] * 1.3)
    a[..., 0] *= r; a[..., 1] *= g; a[..., 2] *= b
    return Image.fromarray(np.clip(a, 0, 255).astype(np.uint8), 'RGBA')

def shadow_layer(size, cx, cy, width, thick, alpha, blur):
    """Radial-gradient ellipse like Brush.radialGradient in the app."""
    W, H = size
    ys, xs = np.mgrid[0:H, 0:W]
    rx = width / 2; ry = max(1.0, thick / 2)
    d = np.sqrt(((xs - cx) / rx) ** 2 + ((ys - cy) / ry) ** 2)
    inner = .12 + .5 * (1 - blur)
    a = np.where(d <= inner, alpha, np.where(d < 1, alpha * .7 * (1 - (d - inner) / (1 - inner)) + 0, 0))
    a = np.where(d <= inner, alpha * (1 - .3 * d / inner), a)
    return a

def render(items, W=1080, H=2400, header=True):
    k0 = max(W / SCENE_W, H / SCENE_H)
    scene = Image.new('RGBA', (SCENE_W, SCENE_H))
    scene.paste(Image.open(R + 'wall_cream.webp').convert('RGBA'), (0, 0))
    scene.paste(Image.open(R + 'floor_oak.webp').convert('RGBA'), (0, WALL_H))
    sh = Image.open(R + 'shelf_shadow.webp').convert('RGBA'); sh = sh.resize((int(571 * S), int(739 * S)), Image.LANCZOS)
    scene.alpha_composite(sh, (int(LEFT - 40 * S + 10), int(TOP - 40 * S + 8)))
    shelf = Image.open(R + 'shelf_honey.webp').convert('RGBA').resize((int(SHELF_W * S), int(SHELF_H * S)), Image.LANCZOS)
    scene.alpha_composite(shelf, (int(LEFT), int(TOP)))
    # render objects at screen resolution for crispness: build upscaled scene first
    big = scene.resize((int(SCENE_W * k0), int(SCENE_H * k0)), Image.LANCZOS)
    for key, tier, slot, hfix in items:
        im = art(key)
        h = hfix if hfix else min(min(comp_h(tier) - 6, 80), 74 / (im.width / im.height))
        w = h * im.width / im.height
        cx = slot_x(tier, slot); feet = stand(tier)
        L = light(key, tier, cx / SCENE_W) if not nolight else dict(b=1, w=0, so=0, ws=1, th=1, dx=0, dy=0, bl=.5)
        pw, ph = int(w * k0), int(h * k0)
        im2 = im.resize((pw, ph), Image.LANCZOS)
        if not nolight: im2 = tint(im2, L)
        ox, oy = int((cx - w / 2) * k0), int((feet - h) * k0)
        if not nolight:
            lay = Image.new('RGBA', big.size, (0, 0, 0, 0))
            sw = w * L['ws'] * k0; th = L['th'] * k0
            cxs = (cx - w / 2) * k0 + pw / 2
            for (ccx, ccy, ww, tt, aa) in [
                (cxs + L['dx'] * k0, feet * k0 + L['dy'] * k0 - th * .3, sw * 1.0, th, L['so']),
                (cxs, feet * k0 - th * .2, sw * .8, th * .6, min(.5, L['so'] * 1.25))]:
                a = shadow_layer(big.size, ccx, ccy, ww, tt, aa, L['bl'])
                col = np.zeros((big.size[1], big.size[0], 4)); col[..., 0] = 0x3A; col[..., 1] = 0x20; col[..., 2] = 0x10; col[..., 3] = a * 255
                lay = Image.alpha_composite(lay, Image.fromarray(col.astype(np.uint8), 'RGBA'))
            big.alpha_composite(lay)
        big.alpha_composite(im2, (ox, oy))
    # crop to screen (centred)
    left = (big.width - W) // 2; top = (big.height - H) // 2
    out = big.crop((left, top, left + W, top + H))
    if header:
        d = ImageDraw.Draw(out)
        try: f1 = ImageFont.truetype('app/src/main/res/font/mali_semibold.ttf', 70); f2 = ImageFont.truetype('app/src/main/res/font/mali_regular.ttf', 36)
        except Exception: f1 = f2 = ImageFont.load_default()
        x = 22 * 2.6; y = 30 * 2.6 + 14 * 2.6
        d.text((x, y), 'My shelf', fill=(60, 45, 35), font=f1)
        d.text((x, y + 90), 'A collection of good decisions.', fill=(110, 95, 80), font=f2)
        d.text((x, y + 135), 'Each item is a reminder of your patience.', fill=(110, 95, 80), font=f2)
    return out

if __name__ == '__main__':
    W = int(sys.argv[2]) if len(sys.argv) > 3 and sys.argv[2].isdigit() else 1080
    H = int(sys.argv[3]) if len(sys.argv) > 3 and sys.argv[3].isdigit() else 2400
    items = [('clock', 0, 0, 44), ('bonsai', 0, 1, None), ('flower_vase', 0, 3, None),
             ('jar', 1, 1, 88), ('gacha', 1, 2, 88), ('books_stack', 1, 0, None),
             ('cat_calico', 2, 0, None), ('anthurium', 2, 1, None), ('film_camera', 2, 2, None), ('world_globe', 2, 3, None),
             ('mushroom_lamp', 3, 0, None), ('rabbit', 3, 1, None), ('books_standing', 3, 2, None), ('wooden_house', 3, 3, None),
             ('pothos_potted', 4, 0, None), ('basket', 4, 1, None), ('moon_lamp', 4, 2, None), ('snow_globe', 4, 3, None)]
    render(items, W, H).save(sys.argv[1])
