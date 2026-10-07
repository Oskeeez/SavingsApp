"""Splits the supplied room/shelf sheet into wall, floor and shelf layers and builds the colour variants.

Source: Assets/Cropped/Shelf/Split View_ Empty Room and Wooden Shelf.png  (left: room, right: shelf cut-out).
Output (res/drawable-nodpi): wall_*.webp, floor_*.webp, shelf_*.webp, shelf_shadow.webp.
The variants are recolours of the supplied art (placeholders until real artwork is drawn).
"""
import colorsys, sys
import numpy as np
from PIL import Image, ImageFilter
from scipy import ndimage

SRC = 'Assets/Cropped/Shelf/Split View_ Empty Room and Wooden Shelf.png'
OUT = 'app/src/main/res/drawable-nodpi/'
ROOM_W, ROOM_H, WALL_H = 664, 1186, 764
SHELF_BOX = (760, 227, 1251, 886)

im = Image.open(SRC).convert('RGBA')
room = im.crop((0, 0, ROOM_W, ROOM_H)).convert('RGB')
wall = room.crop((0, 0, ROOM_W, WALL_H))
floor = room.crop((0, WALL_H, ROOM_W, ROOM_H))

# ---- shelf cut-out: remove the coloured fringe at the edges
sh = np.array(im.crop(SHELF_BOX)).astype(np.float32)
alpha = sh[..., 3]
solid = alpha > 235
idx = ndimage.distance_transform_edt(~solid, return_distances=False, return_indices=True)
rgb = sh[..., :3][idx[0], idx[1]]  # every pixel takes the colour of its nearest solid pixel
alpha2 = np.where(alpha < 60, 0, alpha)
alpha2 = ndimage.grey_erosion(alpha2, size=(3, 3))
shelf = Image.fromarray(np.dstack([rgb, alpha2]).astype(np.uint8), 'RGBA')


def hsv_arrays(img):
    return np.array(img.convert('HSV')).astype(np.float32)


def recolour(img, hue=None, sat_mul=1.0, sat_add=0.0, val_mul=1.0, val_add=0.0):
    """Shift an RGB(A) image toward a hue (0-360) keeping its light and dark structure."""
    has_alpha = img.mode == 'RGBA'
    a = img.getchannel('A') if has_alpha else None
    hsv = hsv_arrays(img.convert('RGB'))
    if hue is not None:
        hsv[..., 0] = hue / 360 * 255
    hsv[..., 1] = np.clip(hsv[..., 1] * sat_mul + sat_add * 255, 0, 255)
    hsv[..., 2] = np.clip(hsv[..., 2] * val_mul + val_add * 255, 0, 255)
    out = Image.fromarray(hsv.astype(np.uint8), 'HSV').convert('RGB')
    if has_alpha:
        out.putalpha(a)
    return out


def save(img, name, q=90):
    img.save(OUT + name + '.webp', quality=q, method=6)


WALLS = {
    'wall_cream': None,
    'wall_sage': dict(hue=95, sat_mul=0.40, sat_add=0.0, val_mul=0.97),
    'wall_blush': dict(hue=8, sat_mul=0.55, sat_add=0.0, val_mul=1.0),
    'wall_mist': dict(hue=205, sat_mul=0.42, sat_add=0.0, val_mul=0.98),
    'wall_butter': dict(hue=46, sat_mul=1.25, sat_add=0.02, val_mul=1.0),
}
FLOORS = {
    'floor_oak': None,
    'floor_walnut': dict(sat_mul=0.85, val_mul=0.62),
    'floor_ash': dict(hue=36, sat_mul=0.40, val_mul=1.12),
    'floor_slate': dict(hue=210, sat_mul=0.28, val_mul=0.78),
}
SHELVES = {
    'shelf_honey': None,
    'shelf_walnut': dict(sat_mul=0.85, val_mul=0.58),
    'shelf_sage': dict(hue=105, sat_mul=0.38, val_mul=0.95),
    'shelf_white': dict(hue=38, sat_mul=0.12, val_mul=1.55),
    'shelf_charcoal': dict(hue=220, sat_mul=0.10, val_mul=0.50),
}
for table, base in ((WALLS, wall), (FLOORS, floor), (SHELVES, shelf)):
    for name, kw in table.items():
        save(base if kw is None else recolour(base, **kw), name)

# ---- the shelf's soft shadow on the wall and floor (separate so it stays whatever the shelf colour)
pad = 40
w, h = shelf.size
canvas = Image.new('L', (w + 2 * pad, h + 2 * pad), 0)
canvas.paste(shelf.getchannel('A'), (pad, pad))
shadow = canvas.filter(ImageFilter.GaussianBlur(9))
shadow = shadow.point(lambda v: int(v * 0.30))
sh_img = Image.new('RGBA', canvas.size, (70, 40, 20, 0))
sh_img.putalpha(shadow)
sh_img.save(OUT + 'shelf_shadow.webp', quality=85, method=6)
print('built; shelf', shelf.size, 'shadow', sh_img.size)
