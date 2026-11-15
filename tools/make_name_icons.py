#!/usr/bin/env python3
"""Draws every Name Icon, all of them from space (16 Nov lineup). For each icon it writes
  textures/font/suffix_<id>.png           the strip: 60x10, six 10x10 frames (the Shooting Star keeps its old file name, sparkle.png)
  textures/gui/sprites/suffix/<id>.png    the same six frames stacked, for chat and the tab list (with its .mcmeta)
and the bitmap providers in font/emoji.json. With --sheet out.png it also writes a contact sheet to look at.
Needs nothing but Python: the PNG files are written here. Every icon is drawn pixel by pixel: a body lit from the top left, a darker
underside, a one-pixel outline and a small motion. The ids are the ones the server and cosmetics.json already use; only the pictures
and the names changed."""
import os, sys, json, math, zlib, struct

RES = os.path.join(os.path.dirname(__file__), '..', 'src', 'main', 'resources', 'assets', 'stardrops')
F = 6

def png(path, w, h, get):
    """get(x, y) -> (r, g, b, a)"""
    raw = b''.join(b'\x00' + b''.join(bytes(get(x, y)) for x in range(w)) for y in range(h))
    def chunk(t, d): return struct.pack('>I', len(d)) + t + d + struct.pack('>I', zlib.crc32(t + d) & 0xFFFFFFFF)
    open(path, 'wb').write(b'\x89PNG\r\n\x1a\n' + chunk(b'IHDR', struct.pack('>IIBBBBB', w, h, 8, 6, 0, 0, 0)) + chunk(b'IDAT', zlib.compress(raw, 9)) + chunk(b'IEND', b''))

def hx(s): return ((s >> 16) & 255, (s >> 8) & 255, s & 255)
def new(): return [[None] * 10 for _ in range(10)]
def px(g, x, y, c):
    x, y = int(round(x)), int(round(y))
    if 0 <= x < 10 and 0 <= y < 10 and c is not None: g[y][x] = hx(c) if isinstance(c, int) else c
def rect(g, x0, y0, x1, y1, c):
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1): px(g, x, y, c)
def disc(g, cx, cy, r, c):
    for y in range(10):
        for x in range(10):
            if (x - cx) ** 2 + (y - cy) ** 2 <= r * r + r * 0.6: px(g, x, y, c)
def inside(x, y, cx, cy, r): return (x - cx) ** 2 + (y - cy) ** 2 <= r * r + r * 0.6
def mix(a, b, k): return tuple(round(p + (q - p) * k) for p, q in zip(hx(a), hx(b)))

def outline(g, col):
    o = [row[:] for row in g]
    for y in range(10):
        for x in range(10):
            if g[y][x] is None and any(0 <= x + dx < 10 and 0 <= y + dy < 10 and g[y + dy][x + dx] is not None for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))):
                o[y][x] = hx(col)
    return o

ICONS = {}
def icon(name, edge):
    def deco(fn): ICONS[name] = (fn, edge); return fn
    return deco

# ---------------------------------------------------------------- Special
@icon('sparkle', 0x2A1E5A)
def shooting_star(f, g):                                          # Shooting Star: a gold four-point star, a violet trail behind it that shimmers
    trail = [(5, 4), (6, 3), (7, 2), (8, 1)]; cols = [0xE6DCFF, 0xB48CFF, 0x8A6AE8, 0x6A4AC8]
    for i, (x, y) in enumerate(trail): px(g, x, y, 0xFFFFFF if i == (f % 4) else cols[i])
    px(g, 6, 5, 0x8A6AE8 if f % 2 else 0x6A4AC8); px(g, 5, 2 if f % 3 else 3, 0x8A6AE8)
    for x, y in ((3, 4), (3, 8), (1, 6), (5, 6)): px(g, x, y, 0xFFC94A)
    for x, y in ((3, 5), (3, 7), (2, 6), (4, 6)): px(g, x, y, 0xFFE680)
    for x, y in ((2, 5), (4, 5), (2, 7), (4, 7)): px(g, x, y, 0xF0A83A if f % 3 else 0xFFE680)
    px(g, 3, 6, 0xFFFFFF)
    if f in (2, 5): px(g, 3, 3, 0xFFE680); px(g, 0, 6, 0xFFE680)

@icon('bug', 0x3A1410)
def glitch(f, g):                                                 # Glitch: a red block whose rows slip sideways, one row turning cyan, a white ! in the middle
    slip = [(0, 0, 0, 0, 0, 0), (1, 0, 0, -1, 0, 0), (0, 0, 1, 0, 0, -1), (0, -1, 0, 0, 1, 0), (0, 0, 0, 0, 0, 0), (-1, 0, 0, 1, 0, 0)][f]
    for r in range(6):
        y = 2 + r; dx = slip[r]; cy = (f * 2 + 1) % 6 == r and f in (1, 2, 3, 5)
        for x in range(2, 8): px(g, x + dx, y, 0x55FFFF if cy else (0xFF9A7A if r == 0 or x == 2 else 0xC8322A if r == 5 or x == 7 else 0xFF5A36))
    for y in (3, 4, 5): px(g, 4 + slip[y - 2], y, 0xFFFFFF); px(g, 5 + slip[y - 2], y, 0xFFE8E0)
    px(g, 4 + slip[5], 7, 0xFFFFFF); px(g, 5 + slip[5], 7, 0xFFE8E0)

@icon('code', 0x10324A)
def satellite(f, g):                                              # Satellite: two blue panels, a white body, a dish; its signal blinks out
    rect(g, 0, 4, 2, 6, 0x2A8FD0); rect(g, 7, 4, 9, 6, 0x2A8FD0)
    for x in (1, 8): rect(g, x, 4, x, 6, 0x55D8FF)
    gl = f % 6
    if gl < 3: px(g, gl, 4, 0xFFFFFF); px(g, 7 + gl, 4, 0xFFFFFF)
    rect(g, 4, 3, 5, 7, 0xE8ECFF); rect(g, 4, 7, 5, 7, 0xA8B0CC); px(g, 5, 3, 0xC8CCE0); px(g, 3, 5, 0x8A93B0); px(g, 6, 5, 0x8A93B0)
    px(g, 4, 5, 0xFFC83A)
    px(g, 4, 2, 0xA8B0CC); px(g, 3, 1, 0xE8ECFF); px(g, 2, 1, 0xE8ECFF); px(g, 3, 0, 0xE8ECFF)
    if f % 3 >= 1: px(g, 6, 1, 0x55FFFF)
    if f % 3 == 2: px(g, 7, 0, 0x55FFFF)

@icon('patron', 0x5A2E08)
def patron_sun(f, g):                                             # Patron Sun: a gold sun, its rays taking turns, a flare crossing its face
    rays = ((4, 0), (5, 0), (4, 9), (5, 9), (0, 4), (0, 5), (9, 4), (9, 5)) if f % 2 == 0 else ((1, 1), (8, 1), (1, 8), (8, 8))
    for x, y in rays: px(g, x, y, 0xFF8A1E if f % 2 else 0xFFB400)
    disc(g, 4.5, 4.5, 2.5, 0xFFC83A)
    for x, y in ((6, 6), (5, 7), (7, 5), (6, 7), (7, 6)):
        if inside(x, y, 4.5, 4.5, 2.5): px(g, x, y, 0xF09A1A)
    px(g, 3, 3, 0xFFF0A0); px(g, 4, 3, 0xFFE27A); px(g, 3, 4, 0xFFE27A)
    k = f % 6
    if k < 4: px(g, 2 + k, 6 - k, 0xFFFFFF)

@icon('betamark', 0x0E3A1E)
def beta_rocket(f, g):                                            # Beta Rocket: a white rocket with green fins climbing, its flame flickering
    o = 1 if f in (2, 3) else 0
    rect(g, 4, 2 - o, 5, 6 - o, 0xE8ECFF); rect(g, 5, 3 - o, 5, 6 - o, 0xB8C0DA)
    px(g, 4, 1 - o, 0x62E68A); px(g, 5, 1 - o, 0x3FD16A)
    px(g, 4, 3 - o, 0x2A8FD0); px(g, 5, 3 - o, 0x1E6AA8)
    for x in (3, 6): px(g, x, 5 - o, 0x3FD16A); px(g, x, 6 - o, 0x2A9A4A)
    px(g, 4, 7 - o, 0xFFE07A if f % 2 else 0xFF8A3A); px(g, 5, 7 - o, 0xFF8A3A if f % 2 else 0xFFE07A)
    if f % 2 == 0: px(g, 4 + (f // 2) % 2, 8 - o, 0xFF8A3A)
    if f % 3 == 0: px(g, 5 - (f // 3) % 2, 9, 0xC8C4E0)

# ---------------------------------------------------------------- Achievement
@icon('bell', 0x10324A)
def comet(f, g):                                                  # Comet: an icy head, a wide blue tail that ripples
    tail = [((4, 5), 0xBFE8FF), ((5, 4), 0x8FD0FF), ((6, 3), 0x6AB0E8), ((7, 2), 0x4A8FD0), ((8, 1), 0x2A5A9A), ((5, 5), 0x8FD0FF), ((6, 4), 0x6AB0E8), ((7, 3), 0x4A8FD0), ((8, 2), 0x2A5A9A), ((4, 4), 0x8FD0FF), ((5, 3), 0x6AB0E8), ((6, 2), 0x4A8FD0), ((7, 1), 0x2A5A9A)]
    for i, ((x, y), c) in enumerate(tail):
        if (x + f) % 5 == 0: c = 0xE8F6FF
        px(g, x, y, c)
    disc(g, 2.5, 6.5, 1.5, 0xBFE8FF); px(g, 2, 6, 0xFFFFFF); px(g, 3, 7, 0x8FD0FF); px(g, 2, 7, 0xE8F6FF); px(g, 3, 6, 0xE8F6FF)

@icon('clock', 0x2A2E4A)
def moon_phase(f, g):                                             # Moon Phase: the moon going through its phases
    off = [-7, -3.4, -1.4, 1.4, 3.4, 7][f]
    for y in range(10):
        for x in range(10):
            if not inside(x, y, 4.5, 4.5, 3.3): continue
            dark = inside(x, y, 4.5 + off, 4.5, 3.3)
            c = 0x3A3E66 if dark else (0xFFFFFF if x + y < 7 else 0xE8ECFF if x + y < 11 else 0xC8CCE0)
            if not dark and (x, y) in ((3, 3), (6, 5), (4, 6)): c = 0xB8BCD4
            px(g, x, y, c)

@icon('dice', 0x3A2410)
def ringed_planet(f, g):                                          # Ringed Planet: a banded world, a glint running round its ring
    ring = [(0, 6), (1, 6), (2, 5), (3, 5), (4, 5), (5, 4), (6, 4), (7, 4), (8, 3), (9, 3)]
    for x, y in ring[:2] + ring[-2:]: px(g, x, y - 1 if x > 4 else y, 0x9B7BFF)
    for y in range(10):
        for x in range(10):
            if inside(x, y, 4.5, 4.5, 2.5): px(g, x, y, 0xF0D0A0 if y == 3 else 0xC87848 if y == 6 else 0xB86A3A if y == 7 else 0xE8A868)
    px(g, 3, 3, 0xFFF0D0); px(g, 6, 6, 0xA85A2E)
    for i, (x, y) in enumerate(ring):
        if 3 <= x <= 6: continue
        px(g, x, y, 0xD8C8FF)
    for x, y in ((3, 5), (4, 5), (5, 5), (6, 4)): px(g, x, y, 0xD8C8FF)
    gx, gy = ring[(f * 2) % 10]
    if not (3 <= gx <= 6): px(g, gx, gy, 0xFFFFFF)
    else: px(g, gx, 5 if gx < 6 else 4, 0xFFFFFF)

@icon('anvil', 0x2A1008)
def meteor(f, g):                                                 # Meteor: a rock falling in flames
    fl = [((6, 4), 0), ((7, 3), 1), ((8, 2), 2), ((9, 1), 3), ((6, 3), 1), ((7, 2), 2), ((5, 3), 2), ((7, 4), 1), ((8, 3), 2)]
    for (x, y), d in fl:
        if d == 3 and f % 2: continue
        px(g, x, y, [0xFFE07A, 0xFFB03A, 0xFF7A2A, 0xC8421A][(d + f) % 4 if d else 0])
    rect(g, 2, 5, 5, 8, 0x8A6A5A); px(g, 2, 5, None); rect(g, 3, 4, 4, 4, 0x8A6A5A); px(g, 5, 5, 0xA88A78)
    g[5][2] = None; g[8][5] = None
    for x, y in ((3, 7), (4, 6)): px(g, x, y, 0x5A4034)
    px(g, 3, 5, 0xB89A88); px(g, 2, 8, 0x6A4E40); px(g, 3, 8, 0x6A4E40); px(g, 4, 8, 0x6A4E40)
    px(g, 5, 4, 0xFFB03A if f % 2 else 0xFFE07A); px(g, 6, 5, 0xFF7A2A if f % 2 else 0xFFB03A); px(g, 6, 6, 0xC8421A if f % 3 else 0xFF7A2A)

@icon('tractor', 0x1A1E2A)
def rover(f, g):                                                  # Rover: six wheels turning, a mast camera that blinks, a solar deck
    rect(g, 2, 4, 7, 5, 0xD8DCEC); rect(g, 2, 5, 7, 5, 0xA8B0CC); rect(g, 2, 3, 4, 3, 0x2A8FD0); px(g, 3, 3, 0x55D8FF)
    px(g, 6, 3, 0xA8B0CC); px(g, 6, 2, 0xA8B0CC); px(g, 6, 1, 0xE8ECFF); px(g, 7, 1, 0x55FFFF if f % 3 == 0 else 0x1E6AA8)
    for wx in (1, 4, 7):
        rect(g, wx, 6, wx + 1, 7, 0x3A3E52)
        hub = [(0, 0), (1, 0), (1, 1), (0, 1)][(f + wx) % 4]; px(g, wx + hub[0], 6 + hub[1], 0x9AA3B5)
    if f % 2: px(g, 0, 8, 0xB89A78)
    else: px(g, 9, 8, 0xB89A78)

@icon('chest', 0x2A1E10)
def supply_crate(f, g):                                           # Supply Crate: a crate coming down under a striped parachute, swaying
    s = [0, 1, 0, 0, -1, 0][f]
    for x in range(2, 8): px(g, x + s, 0, 0xFF5A5A if (x // 2) % 2 else 0xFFFFFF)
    for x in range(1, 9): px(g, x + s, 1, 0xE84A4A if (x // 2) % 2 else 0xE8ECFF)
    px(g, 2 + s, 2, 0x8A93B0); px(g, 7 + s, 2, 0x8A93B0); px(g, 3, 3, 0x8A93B0); px(g, 6, 3, 0x8A93B0)
    rect(g, 3, 4, 6, 8, 0xB8905A); rect(g, 3, 4, 6, 4, 0xD8B07A); rect(g, 3, 8, 6, 8, 0x7A5A34); rect(g, 3, 6, 6, 6, 0x7A5A34); rect(g, 6, 5, 6, 7, 0x9A7444)
    px(g, 4, 5, 0xFFE680 if f % 3 == 0 else 0xF0B83A); px(g, 5, 7, 0x8A6A3C)

@icon('disc', 0x4A3408)
def golden_record(f, g):                                          # Golden Record: a gold record turning, the light sliding round it
    for y in range(10):
        for x in range(10):
            d = math.hypot(x - 4.5, y - 4.5)
            if d > 4.0: continue
            px(g, x, y, 0xB88410 if 2.0 < d < 2.8 else 0xFFD24A if x + y < 8 else 0xE8B020 if x + y < 11 else 0xC8941A)
    rect(g, 4, 4, 5, 5, 0x2A8FD0); px(g, 4, 4, 0x8FD0FF)
    an = f * math.pi / 3
    for r in (1.6, 2.6, 3.4): px(g, 4.5 + math.cos(an) * r, 4.5 + math.sin(an) * r, 0xFFFFFF if r > 2 else 0xFFF0A0)
    px(g, 4.5 - math.cos(an) * 3.4, 4.5 - math.sin(an) * 3.4, 0x9A6E0E)

# id -> the first of its six characters in the emoji font (fixed: chat, the tab list and saved settings use them)
CODE = {'bug': 0xE100, 'code': 0xE110, 'patron': 0xE120, 'betamark': 0xE130, 'dice': 0xE150, 'chest': 0xE160, 'anvil': 0xE170, 'disc': 0xE180, 'tractor': 0xE1A0, 'bell': 0xE1B0, 'clock': 0xE1C0}
ORDER = ['sparkle', 'bug', 'code', 'patron', 'betamark', 'bell', 'clock', 'dice', 'anvil', 'tractor', 'chest', 'disc']

def frames(name):
    fn, edge = ICONS[name]; out = []
    for f in range(F):
        g = new(); fn(f, g); out.append(outline(g, edge))
    return out

if __name__ == '__main__':
    sheet = sys.argv[sys.argv.index('--sheet') + 1] if '--sheet' in sys.argv else None
    prov = os.path.join(RES, 'font', 'emoji.json'); data = json.load(open(prov))
    data['providers'] = [p for p in data['providers'] if not p.get('file', '').startswith('stardrops:font/suffix_')]
    os.makedirs(os.path.join(RES, 'textures', 'gui', 'sprites', 'suffix'), exist_ok=True)
    allf = {}
    for name in ORDER:
        fr = allf[name] = frames(name)
        cell = lambda f, x, y: (*fr[f][y][x], 255) if fr[f][y][x] is not None else (0, 0, 0, 0)
        png(os.path.join(RES, 'textures', 'font', 'sparkle.png' if name == 'sparkle' else f'suffix_{name}.png'), 60, 10, lambda x, y: cell(x // 10, x % 10, y))
        dst = os.path.join(RES, 'textures', 'gui', 'sprites', 'suffix', f'{name}.png')
        png(dst, 10, 60, lambda x, y: cell(y // 10, x, y % 10)); open(dst + '.mcmeta', 'w').write('{"animation": {"frametime": 2}}')
        if name in CODE: data['providers'].append({'type': 'bitmap', 'file': f'stardrops:font/suffix_{name}.png', 'ascent': 8, 'height': 9, 'chars': [''.join(chr(CODE[name] + i) for i in range(6))]})
    json.dump(data, open(prov, 'w'), indent=2)
    if sheet:
        S = 8; W = 6 * 10 * S + 8 + 6 * 12; H = len(ORDER) * (10 * S + 6)
        def at(x, y):
            k, yy = divmod(y, 10 * S + 6); xx = x - 4
            if yy >= 10 * S or xx < 0: return (58, 70, 96, 255)
            f, fx = divmod(xx, 10 * S + 12)
            if f >= 6 or fx >= 10 * S: return (58, 70, 96, 255)
            c = allf[ORDER[k]][f][yy // S][fx // S]
            return (*c, 255) if c is not None else ((70, 84, 112, 255) if ((fx // S) + (yy // S)) % 2 else (58, 70, 96, 255))
        png(sheet, W, H, at)
