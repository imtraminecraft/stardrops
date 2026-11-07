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

# ---- the soft set (Normal and No Drops)
S('std_beat', add(zap(1800, 260, .16, 'saw', 4500) * .7, blip(2400, .04) * .25), .3)
def std_burst():
    b = mix(1.0); at(b, impact(140, 45, .7, .6, .6) * .7, 0); at(b, hp(noise(.25), 1500) * dec(T(.25), 14) * .35, 0)
    at(b, fm(N(88), .8, ratio=1.5, index=2, k=4) * .25, .02); return b
S('std_burst', std_burst(), .3)
def std_reveal():
    b = mix(1.6)
    for i, m in enumerate([76, 83, 88]): at(b, fm(N(m), 1.0, ratio=3.01, index=1.2, k=3.2) * (.45 - .08 * i), i * .07)
    return echo(b, .16, .4, 3)
S('std_reveal', std_reveal(), .35)
S('std_glow', add(drone(N(52), 1.8, .3), twinkles(1.8, 6, gain=.1)), .3)
def std_end(): b = mix(1.0); at(b, warp(.8, 2600, 260, .55), 0); at(b, zap(900, 180, .3, 'tri') * .3, .15); return b
S('std_end', std_end(), .25)
def std_fizzle():
    d = 1.1; t = T(d); x = lp(osc(420, d, 'saw', glide=50), 900) * dec(t, 2.2) * .5
    b = mix(d); at(b, x, 0); at(b, crackle(.8, 40, .25), .1); return b
S('std_fizzle', std_fizzle(), .25)

# ---- the RNG set: big, cinematic
def rng_beat():
    b = mix(.9); at(b, zap(2600, 120, .35, 'saw', 6000) * .7, 0); at(b, thump(90, 40, .5, 7) * .8, 0); at(b, riser(.25, 800, 5000) * .25, 0); return b
S('rng_beat', rng_beat(), .3)
def rng_burst():
    b = mix(2.6)
    at(b, riser(.35, 300, 6000) * .5, 0)                                                # the charge
    at(b, impact(70, 22, 2.0, 1.0, 1.4) * 1.0, .32)                                     # the supernova
    at(b, lp(noise(1.6), 1800) * dec(T(1.6), 2.4) * .45, .32)
    at(b, fm(N(64), 1.8, ratio=1.41, index=3, k=1.8) * .25, .34)                        # a metallic ring through it
    at(b, twinkles(1.6, 14, gain=.12), .5); return b
S('rng_burst', rng_burst(), .35)
def rng_reveal():
    b = mix(3.0)
    at(b, supersaw([60, 67, 71, 74, 79], 2.6, det=.014, cut=3000, a=.05) * .55, 0)       # a cosmic chord (C maj9)
    at(b, choir([72, 76, 79], 2.4, .2) * .35, .05)
    for i, m in enumerate([79, 83, 86, 91, 95, 98]): at(b, glock(N(m), 1.2) * (.3 - .03 * i), .12 + i * .07)
    at(b, twinkles(2.6, 16, gain=.12), .2); return b
S('rng_reveal', rng_reveal(), .4)
S('rng_glow', add(drone(N(43), 3.0, .35) * 1.3, twinkles(3.0, 12, gain=.1)), .35)
def rng_end():
    b = mix(2.0); at(b, riser(.4, 500, 6000) * .4, 0); at(b, warp(1.2, 5000, 180, .7), .35)
    at(b, fm(N(79), 1.4, ratio=2.01, index=.8, k=2.5) * .25, .4); at(b, fm(N(72), 1.4, ratio=2.01, index=.8, k=2.5) * .2, .5); return b
S('rng_end', rng_end(), .35)

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
