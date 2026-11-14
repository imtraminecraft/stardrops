#!/usr/bin/env python3
"""Stardrops: the Mythological sound packs (14 Nov), so every catalog's animation has a voice of its own.
  Mob Drops    -> VOYAGER (take "d", tools/make_voyager_sounds.py): space and exploration.
  Mythological creatures -> RELIC (take "e"): an old temple. Lyre, frame drums, bronze, a blade, voices in an ancient mode.
  Mythological treasure  -> TROVE (take "f"): a treasure hunt. A spade in sand, wood and rusty hinges, coins, pizzicato, a music box.
Also sky_epic, the score of the achievements' grand opening (Codex.epicTimeline: the burst at 2.1 s, comets landing from 3.22 s
every 0.19 s, the title at 2.95 s).

The moments land on the dig scene (Dig3D; AnimationManager.cueTimes):
  rise (0.30 s): the spade bites at +.20 +.55 +.90 +1.25
  approach (1.45 s): the creature climbs out / the spade rings on the chest at +.27 and the chest comes up until +1.15
  creatures: strike (the cut lands +.45), barrage (cuts +.35 +.65 +.95), shower (cuts +.35 +.60 +.85 +1.07, the last +1.41)
  treasure:  strike (the lid opens +.45), barrage (+.35), shower (+.35)
  then dust / reveal / jackpot, and none / close / glow at the end.
Run from the repo root: python3 tools/make_dig_sounds.py [name parts...]
"""
import os, sys, json, subprocess, tempfile, wave
import numpy as np
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from make_slayer_sounds import (SR, RES, T, N, noise, lp, hp, bp, env, dec, sat, glide, saw, bank, sweep_lp, place, stereo, make_ir, hall,
                                sub_drop, kick, taiko, boom, crack, rubble, brass, strings, choir, bell, glock, harp, cymbal, rev_cymbal,
                                riser, whoosh, fire, roar, shimmer, wind, timp_roll, master)
rng = np.random.default_rng(776)
TEMPLE = make_ir(2.8, 4200, .02)                                                      # stone walls, open to the night
ROOM = make_ir(1.3, 5200, .008)                                                       # a small warm room: wood and sand
def put(b, x, at, g=1.0, pan=0.0): place(b, x, max(0, at), g, pan)

# ------------------------------------------------------------------ instruments
def pluck(m, d=1.8, bright=.5, damp=.996):
    """A plucked gut string (Karplus-Strong): the lyre, and with more damping a pizzicato."""
    n = int(SR / N(m)); L = int(d * SR); buf = lp(rng.uniform(-1, 1, n), 1500 + 6000 * bright); reps = L // n + 1; out = np.zeros(reps * n)
    for r in range(reps):                                                                 # one period at a time: each pass through the string averages neighbours
        out[r * n:(r + 1) * n] = buf; buf = damp * .5 * (buf + np.roll(buf, -1))
    out = out[:L]
    return out * np.minimum(1, (d - T(d)) / .05)[:L] * .9
def lyre(m, d=2.0):
    x = pluck(m, d, .6); y = pluck(m + 12, d * .7, .8); x[:len(y)] += .35 * y; return x
def pizz(m, d=.5): return lp(pluck(m, d, .3, .985), 2600) * 1.3
def frame_drum(f=95, d=.7, hard=1.0):
    t = T(d); return sat(np.sin(glide(f * 1.6, f, d, .08)) * dec(d, 7) + .4 * np.sin(2 * np.pi * f * 1.59 * t) * dec(d, 11) + bp(noise(d), 900, 4200) * dec(d, 60) * .5 * hard, 1.3)
def bronze(m=50, d=2.6, hard=1.0):
    """Struck bronze: a shield, a gong. Inharmonic partials, a hard edge."""
    t = T(d); f = N(m); x = sum(a * np.sin(2 * np.pi * f * r * t + p) * np.exp(-t * k) for r, a, k, p in
        [(1, 1, 1.4, 0), (2.32, .7, 2.0, 1), (3.61, .55, 2.8, 2), (4.9, .4, 3.6, .5), (6.27, .3, 5, 1.7), (8.13, .2, 7, 2.6)])
    return x * .3 + hp(noise(d), 3000) * dec(d, 70) * .5 * hard
def shing(d=.5, f=4200):
    """A blade drawn through the air and biting: a bright metal ring with a hiss on its front."""
    t = T(d); x = sum(np.sin(2 * np.pi * f * r * t + r) * np.exp(-t * (7 + 3 * r)) for r in (1, 1.37, 1.83, 2.51)) * .22
    return x + bp(noise(d), 3500, 11000) * np.exp(-t * 26) * .7
def slash(pitch=1.0, hard=1.0):
    """A whole cut: the swing through the air, the bite, the ring."""
    b = np.zeros(int(.9 * SR)); w = whoosh(.16, 900, 5200 * pitch, True) * .6; b[:len(w)] += w
    s = shing(.6, 3800 * pitch) * hard; i = int(.15 * SR); b[i:i + len(s)] += s
    th = (lp(noise(.25), 420) * dec(.25, 22) * 1.6 + np.sin(glide(150, 60, .25, .2)) * dec(.25, 16)) * hard; b[i:i + len(th)] += th
    return b
def dig(pitch=1.0, sandy=0.0):
    """The spade going in: a crunch of earth, a low thunk, grit falling after."""
    d = .42; t = T(d); x = bp(noise(d), 500 * pitch, 3200 * pitch) * np.exp(-t * (16 - 6 * sandy)) * (1 + .6 * np.sign(np.sin(2 * np.pi * 70 * t)) * np.exp(-t * 30))
    x += np.sin(glide(130 * pitch, 62 * pitch, d, .2)) * np.exp(-t * 15) * .9
    g = hp(noise(d), 2500) * (np.abs(lp(noise(d), 50)) * 9).clip(0, 1) ** 2 * np.minimum(1, t / .1) * np.exp(-(t - .1).clip(0) * 9) * (.35 + .5 * sandy)
    return sat(x + g, 1.6)
def creak(d=.8, f0=70, f1=140):
    """A rusty hinge / old wood giving: stick-slip, a pitched groan that wobbles."""
    t = T(d); f = f0 + (f1 - f0) * (t / d) ** .7 + 14 * np.sin(2 * np.pi * 3.1 * t) + 30 * lp(noise(d), 8)
    ph = 2 * np.pi * np.cumsum(f) / SR; x = saw(ph) * (0.5 + .5 * np.sign(np.sin(ph * .5)))
    return (bp(x, 500, 1700) * 1.2 + bp(x, 2100, 3600) * .7) * env(d, .05, .15) * (0.6 + .4 * np.abs(lp(noise(d), 14)) * 5).clip(0, 1.2) * .7
def knock(f=180, d=.25): t = T(d); return (np.sin(2 * np.pi * f * t) * np.exp(-t * 34) + .5 * np.sin(2 * np.pi * f * 2.7 * t) * np.exp(-t * 60) + bp(noise(d), 800, 3000) * np.exp(-t * 90) * .5)
def click(d=.06, f=2600): t = T(d); return (np.sin(2 * np.pi * f * t) * np.exp(-t * 120) + hp(noise(d), 4000) * np.exp(-t * 200)) * .7
def coin(m=None, d=.35):
    f = N(m if m is not None else rng.uniform(98, 110)); t = T(d)
    return (np.sin(2 * np.pi * f * t) + .6 * np.sin(2 * np.pi * f * 1.52 * t + 1) + .35 * np.sin(2 * np.pi * f * 2.41 * t + 2)) * np.exp(-t * 16) * np.minimum(1, t / .001) * .3
def coins(b, at, n, span, g=.3, up=False):
    for i in range(n):
        q = rng.uniform(0, 1) ** 1.5 * span; m = rng.uniform(96, 108) + (6 * q / span if up else 0); put(b, coin(m, rng.uniform(.2, .5)), at + q, g * rng.uniform(.5, 1), rng.uniform(-.8, .8))
def mbox(m, d=1.6): t = T(d); f = N(m); return (np.sin(2 * np.pi * f * t) * np.exp(-t * 3.2) + .4 * np.sin(2 * np.pi * f * 3 * t) * np.exp(-t * 9) + .2 * np.sin(2 * np.pi * f * 5.4 * t) * np.exp(-t * 16)) * np.minimum(1, t / .002) * .5
def growl(d=1.0, f=62):
    t = T(d); ph = 2 * np.pi * f * t + 3 * np.cumsum(lp(noise(d), 25)) / SR * 50; x = saw(ph) * (.55 + .45 * np.sin(2 * np.pi * 23 * t))
    return sat((bp(x, 180, 700) + .5 * lp(x, 160)) * env(d, .2, .3), 2.2) * .8

# ancient mode (creatures): D phrygian dominant (D Eb F# G A Bb C). treasure: G major.
PD = [0, 1, 4, 5, 7, 8, 10]
def pd(deg, base=62): o, k = divmod(deg, 7); return base + 12 * o + PD[k]
MJ = [0, 2, 4, 5, 7, 9, 11]
def mj(deg, base=67): o, k = divmod(deg, 7); return base + 12 * o + MJ[k]
DIGS = [.20, .55, .90, 1.25]

# ------------------------------------------------------------------ RELIC (take "e"): the creatures of myth
def rise_e():       # Breaking ground: the spade bites four times, each deeper; a lyre asks its question over a low drone
    b = stereo(3.4)
    for i, at in enumerate(DIGS): put(b, dig(1.1 - .09 * i) * (.8 + .08 * i), at, pan=-.25 + .12 * i); put(b, frame_drum(88 - 6 * i, .5, .4) * .35, at)
    put(b, brass(38, 2.6, .9, 700) * .28, 0); put(b, wind(3.0, 700) * .18, 0)
    for i, (dg, at) in enumerate([(0, .02), (1, .38), (4, .72), (3, 1.08), (1, 1.42)]): put(b, lyre(pd(dg), 1.6) * .42, at, pan=.3)
    return hall(b, .32, TEMPLE)
def approach_e():   # It wakes: the earth shakes, it growls its way out, frame drums close in
    b = stereo(3.2)
    put(b, lp(noise(1.6), 120) * env(1.6, .25, .6) * 2.4, 0); put(b, rubble(1.3, 30) * .5, .05, pan=-.3); put(b, rubble(1.0, 22) * .4, .4, pan=.4)
    put(b, growl(1.0, 58) * .7, .12); put(b, roar(1.1, 135, 72) * .55, .5)
    for i, at in enumerate([0, .3, .55, .75, .9, 1.02, 1.12, 1.2]): put(b, frame_drum(80 + (i % 2) * 14, .5, .8) * (.4 + .05 * i), at, pan=(-.4 if i % 2 else .4))
    put(b, brass(pd(0, 38), 1.5, .5, 1400) * .4, .2); put(b, brass(pd(1, 38), 1.0, .2, 1800) * .35, .9)
    return hall(b, .3, TEMPLE)
def strike_e():     # One cut: a single clean stroke, the body falls, the temple holds its breath
    b = stereo(3.4)
    put(b, slash(.9, 1.0), .45 - .15); put(b, taiko(52, 1.2) * .7, .45); put(b, sub_drop(1.4, 70, 30, 3) * .5, .45)
    put(b, lp(noise(.5), 300) * dec(.5, 9) * 1.4, .95); put(b, rubble(.9, 14) * .3, 1.0)                 # it falls
    put(b, bronze(38, 2.4, .3) * .25, .47)
    return hall(b, .4, TEMPLE)
def dust_e():       # To dust: it crumbles; the wind carries it off; one low string
    b = stereo(3.8)
    put(b, rubble(1.6, 34) * .55, 0); put(b, wind(3.2, 1100) * .4, .1); put(b, whoosh(1.2, 2600, 500, False) * .2, .2)
    put(b, lyre(pd(0, 50), 2.6) * .4, .9)
    return hall(b, .45, TEMPLE)
def none_e():       # Empty hands: three falling notes on the lyre, alone
    b = stereo(4.2)
    for i, (dg, at) in enumerate([(4, 0), (1, .55), (0, 1.15)]): put(b, lyre(pd(dg), 2.4) * .5, at, pan=-.2 + .2 * i)
    put(b, lyre(pd(0, 50), 2.6) * .35, 1.15); put(b, wind(3.6, 600) * .15, 0)
    return hall(b, .5, TEMPLE)
def barrage_e():    # Three cuts: the blade sings three times, a drum under each, bronze on the last
    b = stereo(4.6)
    for i, at in enumerate([.35, .65, .95]):
        put(b, slash(.85 + .12 * i, .8 + .15 * i), at - .15, pan=(-.5, .5, 0)[i]); put(b, taiko(64 - 6 * i, 1.0) * (.5 + .15 * i), at)
        put(b, strings([pd(0, 50), pd(4, 50), pd(i, 62)], .32, .01, 3000) * .3, at)
    at = .95; put(b, bronze(38, 3.0) * .5, at); put(b, boom(.8, 2.2) * .55, at); put(b, choir([pd(0, 50), pd(4, 50), pd(2, 62)], 2.6, .05, 'a') * .35, at + .02)
    put(b, lp(noise(.5), 300) * dec(.5, 9) * 1.2, at + .5)
    return hall(b, .35, TEMPLE)
def reveal_e():     # Olympus: the lyre runs up the old scale into a great open chord of voices and strings
    b = stereo(5.4)
    for i in range(9): put(b, lyre(pd(i, 62), 1.8) * .4, i * .085, pan=-.6 + i * .15)
    ch = [38, 50, 57, 62, 66, 69]
    put(b, strings(ch, 4.2, .35, 3400) * .5, .5); put(b, choir([62, 66, 69, 74], 4.0, .4, 'a') * .45, .6); put(b, brass(50, 3.4, .5, 1800) * .3, .6)
    put(b, bronze(50, 3.4, .5) * .3, .72); put(b, bell(74, 2.8) * .3, .74, pan=.3); put(b, shimmer(3.2, 14, 86, 104) * .3, .9)
    for i, dg in enumerate([4, 7, 9, 11]): put(b, lyre(pd(dg, 62), 1.6) * .3, 1.5 + i * .3, pan=.4)
    return hall(b, .45, TEMPLE)
def close_e():      # Embers: a brazier crackles; the lyre closes the story
    b = stereo(4.0)
    put(b, fire(3.2, .5) * env(3.2, .4, 1.8) * .35, 0)
    for i, (dg, at) in enumerate([(4, 0), (2, .35), (0, .8)]): put(b, lyre(pd(dg), 2.2) * .4, at)
    put(b, lyre(50, 2.6) * .35, .8)
    return hall(b, .45, TEMPLE)
def shower_e():     # Five cuts: faster, higher, a breath... and the last one splits the sky
    b = stereo(6.8)
    for i, at in enumerate([.35, .60, .85, 1.07]):
        put(b, slash(.85 + .1 * i, .75 + .08 * i), at - .15, pan=(-.6, .6, -.3, .3)[i]); put(b, frame_drum(90 + 8 * i, .5) * .6, at); put(b, taiko(60, .6) * .35, at)
        put(b, strings([pd(i + 2, 62), pd(i + 4, 62)], .26, .01, 3600, 1.0) * .3, at)
    put(b, rev_cymbal(.34) * .5, 1.07); put(b, riser(.34, 400, 5000) * .4, 1.07)
    at = 1.41; put(b, slash(1.25, 1.3), at - .15); put(b, boom(1.2, 2.8) * .8, at); put(b, bronze(38, 3.6) * .6, at); put(b, bronze(50, 3.0) * .35, at + .03, pan=.4); put(b, cymbal(3.4, .003, 1.0) * .35, at)
    put(b, choir([50, 57, 62, 66, 69], 4.4, .06, 'a', 1.2) * .55, at); put(b, strings([38, 50, 57, 62, 66], 4.4, .05, 4000) * .45, at); put(b, brass(38, 3.6, .08, 2200) * .4, at)
    for i in range(10): put(b, lyre(pd(i + 2, 62), 1.4) * .3, at + .1 + i * .07, pan=np.sin(i) * .7)
    return hall(b, .42, TEMPLE)
def jackpot_e():    # Gift of the gods: horns call in the old mode and resolve to a blazing major; choir, timpani, a lyre racing upward
    b = stereo(8.4)
    put(b, boom(1.0, 2.4) * .6, 0); put(b, bronze(38, 4.0) * .5, 0)
    call = [(62, 0, .3), (62, .3, .16), (63, .46, .3), (66, .76, .3), (69, 1.06, .55), (70, 1.6, .28), (69, 1.88, .28), (74, 2.2, 1.6)]
    for m, at, d in call: put(b, brass(m, d * 1.3, .03, 3200) * .5, at, pan=-.2); put(b, brass(m - 12, d * 1.3, .04, 2200) * .38, at, pan=.25)
    put(b, strings([38, 50, 57, 63, 66], 2.3, .2, 3200, .6) * .4, 0); put(b, choir([50, 57, 62, 66], 2.3, .3, 'o') * .35, 0)
    put(b, timp_roll(.7, 73, .1, .45) * .6, 3.1); at = 3.8                                              # the turn to D major
    put(b, boom(1.1, 2.6) * .7, at); put(b, cymbal(3.8, .003, .9) * .35, at); put(b, bronze(50, 3.6) * .4, at)
    ch = [38, 50, 57, 62, 66, 69, 74]
    put(b, strings(ch, 4.4, .08, 4200) * .55, at); put(b, choir([62, 66, 69, 74, 78], 4.4, .1, 'a', 1.2) * .55, at); put(b, brass(50, 4.0, .06, 2600) * .45, at); put(b, brass(57, 4.0, .06, 2600) * .35, at)
    for i in range(15): put(b, lyre(62 + [0, 4, 7, 12, 16, 19, 24][i % 7] + 12 * (i // 7 % 2), 1.6) * .32, at + .08 + i * .09, pan=np.sin(i * .8) * .7)
    put(b, shimmer(4.0, 22, 88, 110) * .4, at + .2)
    return hall(b, .45, TEMPLE)
def glow_e():       # Hymn: voices holding one soft chord, a lyre turning slowly through it
    b = stereo(7.0)
    put(b, choir([50, 57, 62, 66], 6.0, 1.4, 'o', .8) * .4, 0); put(b, strings([38, 50, 57], 6.0, 1.6, 1800) * .3, 0)
    for i, m in enumerate([62, 69, 66, 74, 69, 78, 74]): put(b, lyre(m, 2.2) * .3, .4 + i * .62, pan=np.sin(i * 1.3) * .5)
    put(b, shimmer(5.0, 8, 90, 108) * .25, .6)
    return hall(b, .5, TEMPLE)

# ------------------------------------------------------------------ TROVE (take "f"): buried treasure
def rise_f():       # X marks the spot: a spade in sand, four scoops, a pizzicato bass tiptoeing along with it
    b = stereo(3.0)
    for i, at in enumerate(DIGS):
        put(b, dig(1.25 - .06 * i, .9) * .75, at, pan=.2 - .12 * i); put(b, pizz(mj([0, 2, 4, 5][i], 43), .5) * .7, at, pan=-.2); put(b, pizz(mj([0, 2, 4, 5][i], 55), .4) * .35, at + .175, pan=.3)
    put(b, click(.05, 5200) * .25, 0, pan=.6)
    return hall(b, .2, ROOM)
def approach_f():   # Something solid: the spade rings on iron-bound wood; a creak, a rumble, and it comes up out of the ground
    b = stereo(3.0)
    at = .27; put(b, knock(170, .3) * 1.0, at); put(b, bronze(69, 1.2, 1.2) * .5, at); put(b, click(.05, 3400) * .5, at)
    put(b, strings([mj(4, 55), mj(6, 55)], 1.0, .25, 3000, 1.0) * .28, .35)                              # could it be...?
    put(b, lp(noise(.9), 150) * env(.9, .3, .3) * 1.8, .35); put(b, creak(.8, 60, 130) * .6, .4, pan=-.3); put(b, rubble(.9, 26) * .4, .45, pan=.3)
    for i in range(8): put(b, harp(mj(i, 67), 1.0) * .3, .6 + i * .065, pan=-.6 + i * .17)
    put(b, knock(120, .3) * .7, 1.15); put(b, coin(100, .4) * .5, 1.17)
    return hall(b, .25, ROOM)
def strike_f():     # Rusty hinge: the latch, the lid groaning open... and stale air
    b = stereo(3.0)
    put(b, click(.06, 2400) * .8, .2); put(b, creak(.7, 80, 170) * .8, .45); put(b, knock(110, .3) * .6, 1.12)
    put(b, lp(wind(1.6, 500), 900) * .5, .6); put(b, pizz(mj(4, 43), .5) * .4, .5)
    return hall(b, .3, ROOM)
def dust_f():       # Cobwebs: a moth, and three notes walking sadly downstairs
    b = stereo(3.0); t = T(.5)
    put(b, bp(noise(.5), 300, 900) * (np.sin(2 * np.pi * 26 * t) > 0) * env(.5, .05, .3) * .3, .1, pan=.5)
    for i, m in enumerate([mj(2, 55), mj(1, 55) - 1, mj(0, 55) - 1]): put(b, pizz(m, .7) * .7, .35 + i * .3); put(b, pizz(m - 12, .7) * .5, .35 + i * .3)
    return hall(b, .3, ROOM)
def none_f():       # Better luck next time: a music box winds down
    b = stereo(3.6)
    for i, (m, at) in enumerate([(79, 0), (76, .42), (74, .95), (67, 1.6)]): put(b, mbox(m, 1.8) * (.6 - .08 * i), at, pan=-.2 + .13 * i)
    put(b, click(.04, 3000) * .2, 2.0)
    return hall(b, .35, ROOM)
def barrage_f():    # Unlocked: latch, lid, and light pours out with the first coins
    b = stereo(4.2)
    put(b, click(.06, 2600) * .8, .12); put(b, click(.05, 3200) * .6, .2); put(b, creak(.42, 90, 210) * .7, .35 - .12)
    at = .35; put(b, knock(130, .3) * .7, at + .3); put(b, whoosh(.5, 500, 5200, False) * .4, at); put(b, taiko(70, .8, .5) * .4, at)
    for i in range(10): put(b, harp(mj(i, 67), 1.2) * .32, at + i * .05, pan=-.7 + i * .15)
    put(b, strings([55, 62, 67, 71], 2.6, .15, 3400) * .4, at + .1); put(b, glock(mj(7, 67), 1.4) * .4, at + .5); put(b, glock(mj(9, 67), 1.4) * .35, at + .62)
    coins(b, at + .15, 14, 1.4, .3)
    return hall(b, .28, ROOM)
def reveal_f():     # Glitter: coins spilling, a music box tune, warm strings
    b = stereo(5.0)
    put(b, strings([43, 55, 62, 67, 71], 4.0, .3, 3200) * .45, 0); coins(b, 0, 26, 2.2, .3); put(b, shimmer(3.2, 12, 90, 108) * .3, .2)
    tune = [(79, 0), (83, .2), (86, .4), (91, .6), (88, .9), (86, 1.1), (83, 1.3), (86, 1.6), (91, 1.9)]
    for m, at in tune: put(b, mbox(m, 1.6) * .5, .15 + at, pan=.25); put(b, glock(m + 12, .9) * .12, .15 + at, pan=-.4)
    for i, m in enumerate([43, 50, 47, 50]): put(b, pizz(m, .6) * .5, .15 + i * .45, pan=-.3)
    return hall(b, .32, ROOM)
def close_f():      # Pocketed: two coins into the pouch, a little cadence
    b = stereo(2.8)
    put(b, coin(101, .4) * .7, 0, pan=-.2); put(b, coin(104, .4) * .7, .13, pan=.2); put(b, lp(noise(.2), 500) * dec(.2, 20) * .8, .3)
    for i, m in enumerate([62, 59, 55]): put(b, pizz(m, .7) * .55, .5 + i * .2)
    put(b, mbox(79, 1.6) * .4, .9)
    return hall(b, .3, ROOM)
def shower_f():     # The big lock: three tumblers, a held breath, and the lid is thrown open by what is inside
    b = stereo(6.4)
    for i, at in enumerate([.02, .12, .22]): put(b, click(.06, 2200 + 500 * i) * .8, at, pan=-.4 + .4 * i); put(b, pizz(mj(i * 2, 55), .3) * .5, at)
    put(b, strings([mj(4, 67), mj(6, 67)], .34, .2, 4200, 1.0) * .3, .02); put(b, rev_cymbal(.3) * .4, .05)
    at = .35; put(b, knock(120, .35) * 1.0, at); put(b, boom(.9, 2.4) * .65, at); put(b, cymbal(3.2, .003, 1.1) * .3, at); put(b, whoosh(.7, 400, 6500, False) * .5, at)
    put(b, strings([43, 55, 62, 67, 71, 74], 4.6, .05, 4200) * .5, at); put(b, choir([67, 71, 74, 79], 4.2, .1, 'a', 1.2) * .4, at); put(b, brass(55, 3.6, .08, 2400) * .35, at)
    for i in range(16): put(b, glock(mj(i, 67), 1.0) * .32, at + .03 + i * .045, pan=np.sin(i * .9) * .8)
    coins(b, at + .05, 60, 3.2, .34, True); put(b, shimmer(3.8, 24, 90, 112) * .45, at + .1)
    return hall(b, .32, ROOM)
def jackpot_f():    # Dragon's hoard: a jolly brass fanfare over a waterfall of gold
    b = stereo(7.6)
    fan = [(67, 0, .14), (67, .14, .14), (67, .28, .14), (71, .42, .3), (74, .72, .3), (71, 1.02, .16), (74, 1.18, .16), (79, 1.34, 1.5)]
    for m, at, d in fan: put(b, brass(m, d * 1.25, .02, 3400) * .5, at, pan=-.2); put(b, brass(m - 12, d * 1.25, .03, 2400) * .35, at, pan=.2); put(b, glock(m + 12, .7) * .22, at, pan=.5)
    put(b, taiko(55, 1.0) * .6, 0); put(b, taiko(55, 1.2) * .7, 1.34); put(b, cymbal(3.4, .003, 1.0) * .3, 1.34)
    put(b, strings([43, 55, 62, 67, 71], 2.0, .1, 3600) * .45, 1.34); put(b, strings([48, 55, 60, 64, 67], 1.4, .15, 3600) * .45, 3.0); put(b, strings([43, 55, 62, 67, 71, 74, 79], 3.2, .15, 4200) * .5, 4.2)
    put(b, choir([67, 71, 74], 3.0, .3, 'a') * .35, 4.2); put(b, boom(.8, 2.2) * .5, 4.2); put(b, cymbal(3.0, .003, 1.2) * .25, 4.2)
    coins(b, .3, 120, 6.0, .3, False)
    tune = [(79, 3.0), (84, 3.2), (88, 3.4), (84, 3.6), (86, 3.8), (91, 4.2), (95, 4.5), (98, 4.8)]
    for m, at in tune: put(b, mbox(m, 1.4) * .5, at, pan=.3); put(b, glock(m, 1.0) * .2, at, pan=-.3)
    put(b, shimmer(3.0, 20, 92, 112) * .4, 4.3)
    return hall(b, .34, ROOM)
def glow_f():       # Lullaby: the music box plays the treasure's little tune while the gold glints
    b = stereo(6.6)
    tune = [79, 83, 86, 83, 84, 88, 91, 88, 86, 83, 79, 74, 79]
    for i, m in enumerate(tune): put(b, mbox(m, 1.8) * .45, .1 + i * .38, pan=np.sin(i * .7) * .3); (i % 4 == 0) and put(b, mbox(m - 24, 2.2) * .3, .1 + i * .38)
    put(b, strings([43, 55, 62], 5.6, 1.4, 1600) * .25, 0); coins(b, .5, 9, 4.5, .12)
    return hall(b, .4, ROOM)

# ------------------------------------------------------------------ the achievements' grand opening
def sky_epic():
    b = stereo(8.4); B = 2.1; SPACE = make_ir(4.0, 4200, .04)
    t = T(B); hum = (np.sin(2 * np.pi * 36.7 * t) + .6 * np.sin(2 * np.pi * 55.2 * t) + .3 * np.sin(2 * np.pi * 73.6 * t + np.sin(2 * np.pi * .7 * t))) * (t / B) ** 1.6
    put(b, lp(hum, 400) * .7, 0); put(b, riser(1.5, 160, 5200) * .5, .6); put(b, rev_cymbal(1.2) * .5, .9)
    for i in range(22): at = .65 + 1.4 * (i / 22) ** .6; put(b, whoosh(.2, 800, 3000 + 150 * i, False) * (.1 + .012 * i), at, pan=rng.uniform(-.9, .9))   # the sky rushing in
    put(b, bell(86, 2.0) * .22, .1); put(b, glock(98, 1.4) * .16, .12)                                 # the first star
    for i in range(3): put(b, kick(48, .4) * (.3 + .15 * i), 1.3 + i * .27)                             # its heart, quicker
    put(b, boom(1.3, 3.0) * .9, B); put(b, sub_drop(2.6, 110, 26, 1.5) * .8, B); put(b, cymbal(4.0, .003, .8) * .4, B); put(b, whoosh(1.2, 6000, 300, False) * .5, B)
    ch = [38, 50, 57, 62, 66, 69, 76]                                                                 # D major with the lydian ninth: open sky
    put(b, strings(ch, 5.6, .25, 3800) * .5, B + .05); put(b, choir([62, 66, 69, 74, 76], 5.4, .5, 'a', 1.1) * .5, B + .1); put(b, brass(38, 4.6, .4, 1500) * .35, B + .05)
    notes = [62, 66, 69, 73, 74, 78, 81, 86]                                                           # eight comets land: eight bells climbing
    for i, m in enumerate(notes):
        at = 3.22 + i * .19; put(b, whoosh(.3, 500, 4000, True) * .16, at - .3, pan=np.sin(i * 1.9) * .8)
        put(b, glock(m + 12, 1.6) * .42, at, pan=np.sin(i * 1.9) * .7); put(b, bell(m, 2.2) * .22, at, pan=-np.sin(i * 1.9) * .5); put(b, harp(m, 1.4) * .25, at)
    for i in range(12): put(b, glock(74 + [0, 4, 7, 11, 12, 16][i % 6], .4) * .09, 2.95 + i * .034, pan=-.5 + i * .09)   # the title being struck
    put(b, taiko(50, 1.4) * .5, 4.75); put(b, bell(62, 3.2) * .3, 4.75); put(b, bell(74, 3.0) * .28, 4.78, pan=.3); put(b, shimmer(3.4, 30, 86, 112) * .5, 4.6)
    put(b, whoosh(.8, 300, 2600, False) * .25, 4.6)
    return hall(b, .45, SPACE)

FAMS = ['rise', 'approach', 'strike', 'dust', 'none', 'barrage', 'reveal', 'close', 'shower', 'jackpot', 'glow']
PEAK = {'rise': .8, 'approach': .82, 'strike': .88, 'dust': .5, 'none': .55, 'barrage': .92, 'reveal': .85, 'close': .45, 'shower': .95, 'jackpot': .9, 'glow': .5}
if __name__ == '__main__':
    only = sys.argv[1:]
    sj = json.load(open(os.path.join(RES, 'sounds.json'))); ej = json.load(open(os.path.join(RES, 'envelopes.json'))); done = []
    jobs = [(f'slr_{fam}_{tk}', globals()[f'{fam}_{tk}'], PEAK[fam]) for tk in 'ef' for fam in FAMS] + [('sky_epic', sky_epic, .9)]
    for name, fn, peak in jobs:
        if only and not any(o in name for o in only): continue
        x = master(fn(), peak)
        with tempfile.TemporaryDirectory() as td:
            wp = os.path.join(td, 'a.wav')
            with wave.open(wp, 'wb') as w: w.setnchannels(2); w.setsampwidth(2); w.setframerate(SR); w.writeframes((x * 32767).astype('<i2').tobytes())
            subprocess.run(['ffmpeg', '-y', '-loglevel', 'error', '-i', wp, '-c:a', 'libvorbis', '-q:a', '5', os.path.join(RES, 'sounds', name + '.ogg')], check=True)
            if os.environ.get('WAV_OUT'): subprocess.run(['cp', wp, os.path.join(os.environ['WAV_OUT'], name + '.wav')])
        m = x.mean(1); d = round(len(m) / SR, 2); n = int(d * 40) + 1; hop = SR // 40
        e = [float(np.sqrt(np.mean(m[i * hop:(i + 1) * hop] ** 2))) if i * hop < len(m) else 0 for i in range(n)]; mx = max(e) or 1
        ej[name] = {'d': d, 'e': [int(round(q / mx * 99)) for q in e]}
        sj[name] = {'sounds': [{'name': 'stardrops:' + name, 'stream': False}]}
        done.append(f'{name}({d}s)')
    json.dump(sj, open(os.path.join(RES, 'sounds.json'), 'w'), indent=2)
    json.dump(ej, open(os.path.join(RES, 'envelopes.json'), 'w'), separators=(',', ':'))
    print('wrote', len(done), 'sounds:', ' '.join(done))
