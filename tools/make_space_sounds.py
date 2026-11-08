#!/usr/bin/env python3
"""Stardrops: the space sound pack. Same names as before (std_*, rng_*, sky_*), all new: laser zaps on the beats, plasma pops and a
supernova for the bursts, shimmering sci-fi chimes and a cosmic fanfare for the reveals, deep-space drones for the lingers, warp
whooshes for the ends. Builds on the synth blocks of make_scene_sounds.py. Needs numpy, scipy and ffmpeg (libvorbis).
Run from the repo root: python3 tools/make_space_sounds.py"""
import os
src = open(os.path.join(os.path.dirname(os.path.abspath(__file__)), 'make_scene_sounds.py')).read()
exec(src[:src.index('# ---- 1.0b')])          # the building blocks only (T, osc, fm, impact, sweep_noise, reverb, S, OUT, ...)
__file__ = os.path.abspath(__file__)

def zap(f0, f1, d, shape='saw', cut=5000):          # a laser: a fast pitch dive with a bright edge
    t = T(d); x = osc(f0, d, shape, glide=f1) * dec(t, 3.0 / d) * np.minimum(1, t / .001)
    return lp(x, cut)
def blip(f, d=.08): return osc(f, d, 'sq') * dec(T(d), 30) * .5
def echo(x, dl=.18, g=.45, n=4):
    out = pad_to(x, len(x) / SR + dl * n)
    for k in range(1, n + 1): at(out, lp(x, 6000 - k * 900) * g ** k, dl * k)
    return out
def drone(f, d, wob=.25):
    t = T(d); s = sum(osc(f * m * (1 + dd), d) * a for m, a in ((1, 1), (1.5, .4), (2, .35), (3, .12)) for dd in (-.004, .004))
    return lp(s, 1200) * (1 + wob * np.sin(2 * np.pi * .7 * t)) * env(t, d * .3, d * .5) * .25
def twinkles(d, n=10, lo=2600, hi=7000, gain=.16):
    b = mix(d)
    for _ in range(n): f = rng.uniform(lo, hi); at(b, fm(f, .35, ratio=2.01, index=.6, k=9) * gain * rng.uniform(.5, 1), rng.uniform(0, d - .35))
    return b
def warp(d, f0, f1, gain=.6): t = T(d); return sweep_noise(d, f0, f1, .25) * np.sin(np.pi * np.clip(t / d, 0, 1)) ** 1.3 * gain


# (the std_* / rng_* sets were removed in 0.9.0: every animation and its sounds are being remade, Slayer first: tools/make_slayer_score.py)

# ---- the achievements sky
def sky_open():
    b = mix(2.0); at(b, warp(1.0, 300, 4500, .5), 0); at(b, pad([60, 67, 74], 1.6, 'sin', .004, .3) * .35, .3); at(b, twinkles(1.6, 10, gain=.12), .35); return b
S('sky_open', sky_open(), .35)
def sky_close(): b = mix(1.1); at(b, warp(.8, 4000, 300, .5), 0); at(b, blip(N(84), .06) * .25, .05); at(b, blip(N(79), .06) * .2, .13); return b
S('sky_close', sky_close(), .3)
def sky_zoom(): b = mix(1.2); at(b, warp(.9, 400, 5200, .6), 0); at(b, twinkles(1.0, 8, gain=.1), .2); return b
S('sky_zoom', sky_zoom(), .3)
S('sky_star', echo(fm(N(91), .8, ratio=2.01, index=.9, k=4) * .5, .14, .35, 3), .35)

# ------------------------------------------------------------------ write (as make_scene_sounds.py does)
exec(src[src.index('# ------------------------------------------------------------------ write'):])
