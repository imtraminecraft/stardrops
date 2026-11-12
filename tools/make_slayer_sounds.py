#!/usr/bin/env python3
"""Stardrops: the Slayer scene's sounds, the EPIC set (cinematic trailer style: war drums, low brass, choir, sub drops, big halls).
Five moments per animation, three takes of each (SoundFx.SL_SLOTS, AnimationManager.cueTimes):
  rise      (0.45 s)  the boss comes out of its spawn                  slr_rise_a/b/c
  approach  (1.45 s)  it roars, the tension builds                     slr_approach_a/b/c
  then by type:
  No Drops: strike (2.55 s, lands +0.45), dust (3.05 s), nothing (4.0 s)
  a drop:   barrage (2.1 s, lands +.35 +.52 +.68 +.82 +.94 +1.04, big +1.26), reveal (3.4 s), close (end)
  RNG:      shower (1.9 s, six star-fires land +.35 +.52 +.67 +.80 +.91 +1.00, the hush from +1.04, the gold strike +1.48), jackpot (3.4 s), glow (4.0 s)
Everything is synthesized here (numpy/scipy), in stereo, through a convolution hall. Writes sounds/slr_*.ogg and merges sounds.json / envelopes.json.
Run from the repo root: python3 tools/make_slayer_sounds.py"""
import os, json, subprocess, tempfile, wave
import numpy as np
from scipy.signal import butter, sosfilt, fftconvolve

SR = 44100
RES = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..', 'src', 'main', 'resources', 'assets', 'stardrops')
rng = np.random.default_rng(7)

# ------------------------------------------------------------------ basics (mono building blocks)
def T(d): return np.arange(int(d * SR)) / SR
def N(m): return 440.0 * 2 ** ((m - 69) / 12)
def noise(d): return rng.uniform(-1, 1, int(d * SR))
def filt(x, kind, f, order=2): return sosfilt(butter(order, np.clip(np.array(f, dtype=float), 20, SR / 2 - 200) / (SR / 2), kind, output='sos'), x)
def lp(x, f, o=2): return filt(x, 'low', f, o)
def hp(x, f, o=2): return filt(x, 'high', f, o)
def bp(x, a, b, o=2): return filt(x, 'band', [a, b], o)
def env(d, a=.01, r=.1, hold=1.0):
    t = T(d); return np.minimum(1, t / max(a, 1e-4)) * np.minimum(1, (d - t) / max(r, 1e-4)) * hold
def dec(d, k): return np.exp(-T(d) * k)
def sat(x, g=2.0): return np.tanh(x * g) / np.tanh(g)
def glide(f0, f1, d, curve=1.0):
    t = T(d); fr = f0 * (f1 / f0) ** ((t / d) ** curve); return 2 * np.pi * np.cumsum(fr) / SR
def saw(ph):
    """A band-limited saw (polyBLEP): the plain one aliases into a hiss over a whole orchestra of them."""
    t = (ph / (2 * np.pi)) % 1.0; dt = np.maximum(1e-6, np.abs(np.gradient(ph)) / (2 * np.pi)); y = 2 * t - 1
    m = t < dt; u = t[m] / dt[m]; y[m] -= u + u - u * u - 1
    m = t > 1 - dt; u = (t[m] - 1) / dt[m]; y[m] -= u * u + u + u + 1
    return y
def bank(x, make, f0, f1, curve=1.0, K=14):
    """A filter that moves over the sound: K fixed filters, crossfaded along the sweep (no clicks, no zipper)."""
    n = len(x); fs = f0 * (f1 / f0) ** (np.arange(K) / (K - 1)); ys = [make(x, f) for f in fs]
    pos = (np.arange(n) / max(1, n - 1)) ** curve * (K - 1); i = np.minimum(K - 2, pos.astype(int)); w = pos - i; out = np.zeros(n)
    for k in range(K - 1): m = i == k; out[m] = ys[k][m] * (1 - w[m]) + ys[k + 1][m] * w[m]
    return out
def sweep_lp(x, f0, f1, curve=1.0): return bank(x, lambda y, f: lp(y, f), f0, f1, curve)

def place(buf, x, at, gain=1.0, pan=0.0):
    """Mix a mono (or stereo) sound into the stereo buffer at a time, panned -1..1."""
    i = int(at * SR)
    if x.ndim == 1: x = np.stack([x * np.sqrt((1 - pan) / 2) * 1.414, x * np.sqrt((1 + pan) / 2) * 1.414], 1)
    n = min(len(x), len(buf) - i)
    if n > 0: buf[i:i + n] += x[:n] * gain
def stereo(d): return np.zeros((int(d * SR), 2))

# ------------------------------------------------------------------ the hall
def make_ir(d, bright=5000, pre=.012):
    t = T(d); out = []
    for ch in range(2):
        x = rng.normal(0, 1, len(t)) * np.exp(-t * (6.9 / d)); x = lp(x, bright); x = hp(x, 120)
        x[:int(pre * SR)] *= np.linspace(0, 1, int(pre * SR)) ** 2
        for k in range(6): j = int((.011 + .013 * k + .004 * ch) * SR); x[j] += (1.6 - .2 * k) * (1 if k % 2 == 0 else -1)      # early reflections
        out.append(x / np.sqrt(np.sum(x ** 2)))
    return np.stack(out, 1)
HALL = make_ir(3.2, 5200); ROOM = make_ir(1.1, 6500); CATHEDRAL = make_ir(5.0, 4200, .03)
def hall(buf, wet=.3, ir=None, tail=None):
    ir = HALL if ir is None else ir
    w = np.stack([fftconvolve(buf[:, c], ir[:, c]) for c in range(2)], 1)
    n = len(buf) + (len(ir) if tail is None else int(tail * SR)); out = np.zeros((n, 2)); out[:len(buf)] += buf * (1 - wet * .35); m = min(n, len(w)); out[:m] += w[:m] * wet
    return out

# ------------------------------------------------------------------ instruments
def sub_drop(d=1.6, f0=95, f1=27, k=2.6):
    return sat(np.sin(glide(f0, f1, d, .35)) * np.exp(-T(d) * k) * np.minimum(1, T(d) / .004), 1.6)
def kick(f=58, d=.5): return np.sin(glide(f * 3.2, f, d, .12)) * dec(d, 9) + lp(noise(d), 1800) * dec(d, 60) * .5
def taiko(f=62, d=.9, hard=1.0):
    body = np.sin(glide(f * 2.4, f, d, .1)) * dec(d, 5.5) + .5 * np.sin(glide(f * 3.5, f * 1.52, d, .1)) * dec(d, 9)
    slap = bp(noise(d), 700, 3800) * dec(d, 45) * .55 * hard + lp(noise(d), 300) * dec(d, 14) * .5
    return sat(body + slap, 1.5)
def boom(size=1.0, d=2.4):
    """The cinematic hit: a sub drop, a chest thump, a crack on top, gravel after."""
    x = np.zeros(int(d * SR)); a = sub_drop(d, 100, 26, 2.2 / size); x[:len(a)] += a
    b = taiko(48, min(d, 1.2), 1.2); x[:len(b)] += b * .9
    c = hp(noise(.5), 1500) * dec(.5, 26) * .5; x[:len(c)] += c
    g = bp(noise(d), 200, 1400) * np.exp(-T(d) * 3.0) * (np.abs(lp(noise(d), 18)) * 9).clip(0, 1) * .5; x += g
    return sat(x * size, 1.3)
def crack(d=.35): return sat(hp(noise(d), 900) * dec(d, 30) + bp(noise(d), 2000, 7000) * dec(d, 55) * .8, 2)
def rubble(d=1.4, dens=26):
    x = np.zeros(int(d * SR))
    for _ in range(int(d * dens)):
        at = rng.uniform(0, d * .85) ** 1.3; f = rng.uniform(180, 900); n = int(.08 * SR); i = int(at * SR)
        s = np.sin(2 * np.pi * f * T(.08) * (1 - 2 * T(.08))) * dec(.08, 60) + bp(noise(.08), f, f * 3) * dec(.08, 70)
        x[i:i + n] += s[:len(x[i:i + n])] * rng.uniform(.2, .7) * (1 - at / d)
    return x
def brass(m, d, a=.09, bright=2400, vib=.012, n=5):
    """Low brass: detuned saws through a lowpass that opens with the swell, a little growl."""
    t = T(d); x = np.zeros(len(t))
    for k in range(n):
        det = 1 + (k - (n - 1) / 2) * .0045; ph = 2 * np.pi * N(m) * det * t + vib * np.sin(2 * np.pi * 5.2 * t + k); x += saw(ph + k)
    e = np.minimum(1, t / a) ** 1.5 * np.minimum(1, (d - t) / .12)
    x = sweep_lp(x / n, bright * .25, bright, .6) * e
    return lp(sat(x * 1.6, 1.4), min(9000, bright * 2.2))
def braam(m=26, d=2.2, bright=1500):
    """The trailer horn: a stack of low brass a fifth and an octave apart, distorted, swelling and falling."""
    x = brass(m, d, .18, bright, .02, 7) + .7 * brass(m + 12, d, .22, bright * 1.2, .02, 5) + .45 * brass(m + 7, d, .25, bright, .02, 5) + .3 * brass(m + 24, d, .3, bright * 1.5)
    t = T(d); x *= (1 - .25 * np.exp(-t * 3)) * np.exp(-np.maximum(0, t - d * .45) * 1.6)
    return lp(sat(x * .9, 2.2), bright * 3) + .6 * lp(np.sin(2 * np.pi * N(m) * t), 200) * env(d, .1, .6)
def strings(ms, d, a=.25, bright=3600, trem=0.0):
    t = T(d); x = np.zeros(len(t))
    for m in ms:
        for k in range(4):
            det = 1 + (k - 1.5) * .006; x += saw(2 * np.pi * N(m) * det * t + .02 * np.sin(2 * np.pi * (5 + k * .3) * t) + k * 1.3)
    x = lp(x / (4 * len(ms)), bright, 4) * np.minimum(1, t / a) * np.minimum(1, (d - t) / .25)
    if trem: x *= .55 + .45 * np.sin(2 * np.pi * trem * t) ** 2
    return x
def choir(ms, d, a=.4, vowel='a', bright=1.0):
    """Voices: saw stacks through vowel formants, slow vibrato, many slightly different singers."""
    F = {'a': [(800, 1.0), (1150, .6), (2900, .35)], 'o': [(450, 1.0), (800, .5), (2830, .25)], 'e': [(400, 1.0), (1700, .5), (2600, .3)]}[vowel]
    t = T(d); x = np.zeros(len(t))
    for m in ms:
        for k in range(5):
            det = 1 + (k - 2) * .004; vib = .035 * np.sin(2 * np.pi * (4.6 + .35 * k) * t + k * 2.1) * np.minimum(1, t / .5)
            x += saw(2 * np.pi * N(m) * det * t + vib * N(m) / 5.0 / 12 + k)
    x /= 5 * len(ms); y = np.zeros(len(t))
    for f, g in F: y += bp(x, f * .82, f * 1.2) * g * (bright if f > 1000 else 1)
    y += lp(x, 500) * .25
    return y * np.minimum(1, t / a) ** 1.3 * np.minimum(1, (d - t) / .35) * 3.0
def bell(m, d=2.2, hard=1.0):
    t = T(d); f = N(m); x = np.zeros(len(t))
    for r, g, k in [(1, 1, 2.2), (2.0, .6, 3.2), (2.76, .45, 4.5), (5.4, .25, 7), (8.9, .15, 10)]: x += g * np.sin(2 * np.pi * f * r * t + (hard * 1.2 * np.sin(2 * np.pi * f * 3.5 * t) * np.exp(-t * 14) if r == 1 else 0)) * np.exp(-t * k)
    return x * np.minimum(1, t / .002) * .5
def glock(m, d=1.2): t = T(d); f = N(m); return (np.sin(2 * np.pi * f * t + 2.2 * np.sin(2 * np.pi * f * 4.0 * t) * np.exp(-t * 22)) * np.exp(-t * 4.5) + .3 * np.sin(2 * np.pi * f * 2.76 * t) * np.exp(-t * 8)) * np.minimum(1, t / .001) * .5
def harp(m, d=1.6): t = T(d); f = N(m); return sum(np.sin(2 * np.pi * f * h * t) * np.exp(-t * (2.5 + h * 1.4)) / h for h in range(1, 7)) * np.minimum(1, t / .002) * .45
def cymbal(d=3.0, a=.004, k=1.6):
    t = T(d); x = hp(noise(d), 3200) * .7
    for f in [3120, 4270, 5530, 6890, 8110, 9730]: x += np.sin(2 * np.pi * f * t + 3 * np.sin(2 * np.pi * f * .31 * t)) * .12
    return x * np.minimum(1, t / a) * np.exp(-t * k)
def rev_cymbal(d=1.2): return cymbal(d, .004, 3.0 / d * 1.2)[::-1].copy() * .9
def riser(d, f0=180, f1=2600):
    t = T(d); x = sweep_lp(noise(d), f0 * 2, f1 * 3, 1.6) * (t / d) ** 2 * .7
    x += .35 * saw(glide(N(38), N(62), d, 1.8)) * (t / d) ** 2 + .25 * saw(glide(N(38) * 1.006, N(62) * 1.01, d, 1.8)) * (t / d) ** 2
    x *= .7 + .3 * np.sin(2 * np.pi * (4 + 14 * (t / d) ** 2) * t)
    return x
def whoosh(d=.45, f0=500, f1=3200, fall=True):
    """Something burning past: a band of noise sweeping, loudest at the end (it lands) or in the middle (it passes)."""
    t = T(d); x = bank(noise(d), lambda y, f: bp(y, f * .6, f * 1.7), f0, f1) * 1.6
    e = (t / d) ** 2.4 if fall else np.sin(np.pi * t / d) ** 2
    return (x + .4 * lp(noise(d), 240) * 2) * e
def fire(d, k=1.0): return (lp(noise(d), 900) * (0.5 + .5 * np.abs(lp(noise(d), 12)) * 6).clip(0, 1.4) + .35 * hp(noise(d), 2500) * (np.abs(lp(noise(d), 40)) * 8).clip(0, 1) ** 2) * k
def roar(d=1.3, f0=120, f1=70):
    """A monster's roar: a growling saw falling in pitch through a throat, torn at the edges."""
    t = T(d); ph = glide(f0, f1, d, .7) + 6 * np.cumsum(lp(noise(d), 30)) / SR * 60; x = saw(ph) + .6 * saw(ph * .5) + .4 * saw(ph * 1.5 + 1)
    x *= .6 + .4 * np.sin(2 * np.pi * 27 * t + 2 * np.sin(2 * np.pi * 7 * t))
    y = bp(x, 300, 950) + .7 * bp(x, 1100, 2100) + .5 * lp(x, 260) + .25 * bp(noise(d), 1500, 5000) * (.5 + .5 * np.sin(2 * np.pi * 31 * t))
    return sat(y * env(d, .12, .5) * (1 - .5 * (t / d) ** 2), 2.5)
def heartbeat(f=46): return np.concatenate([kick(f, .22) * .9, np.zeros(int(.04 * SR)), kick(f * .92, .34)])
def shimmer(d, dens=22, lo=84, hi=108):
    x = np.zeros(int(d * SR))
    for _ in range(int(d * dens)):
        at = rng.uniform(0, d * .9); g = glock(rng.integers(lo, hi), .5) * rng.uniform(.08, .3); i = int(at * SR); x[i:i + len(g)] += g[:len(x[i:i + len(g)])]
    return x
def wind(d, f=900): return bp(noise(d), f * .5, f * 2.2) * (0.4 + .6 * np.abs(lp(noise(d), 2.5)) * 5).clip(0, 1.3) * env(d, d * .3, d * .5)

D1, D2, D3, D4 = 26, 38, 50, 62          # the set is in D: D minor for the fight, D major for the prize

# ------------------------------------------------------------------ rise: the boss comes out of its spawn
def rise_a():   # Awakening: the ground groans, a low horn swells under it
    b = stereo(2.0); place(b, lp(noise(1.6), 140) * env(1.6, .5, .6) * 1.6, 0); place(b, braam(D1, 1.6, 900) * .55, .05)
    place(b, rubble(1.2, 20) * .5, .1, pan=-.3); place(b, rubble(1.0, 14) * .4, .3, pan=.4); place(b, taiko(50, .9) * .7, .62)
    return hall(b, .32)
def rise_b():   # War horn: one long horn call, a fifth opening over a drum
    b = stereo(2.0); place(b, brass(D2, 1.5, .25, 1900) * .6, 0, pan=-.15); place(b, brass(D2 + 7, 1.25, .3, 2100) * .45, .22, pan=.2); place(b, brass(D1, 1.5, .3, 700) * .5, 0)
    place(b, taiko(55, 1.0) * .8, .0); place(b, taiko(46, 1.0) * .9, .7)
    return hall(b, .4)
def rise_c():   # From the abyss: voices rushing backwards into one deep hit
    b = stereo(2.0); x = choir([D3, D3 + 3, D3 + 7], 1.0, .05, 'o')[::-1] * .5; place(b, x * (T(1.0) / 1.0) ** 1.5, 0); place(b, rev_cymbal(.9) * .35, .08)
    place(b, boom(.8, 1.6) * .9, .95); place(b, rubble(.9, 16) * .35, 1.0)
    return hall(b, .38)

# ------------------------------------------------------------------ approach: it roars and the tension builds (1.4 s into the strikes)
def approach_a():   # War drums: the drums close in under low strings
    b = stereo(1.6)
    for i, (at, f) in enumerate([(0, 58), (.32, 58), (.56, 52), (.78, 58), (.94, 52), (1.08, 62), (1.2, 62), (1.3, 66)]): place(b, taiko(f, .7) * (.45 + .07 * i), at, pan=(-.35 if i % 2 else .35))
    place(b, strings([D2, D2 + 7], 1.5, .5, 1800, 11) * .55, 0); place(b, riser(1.45, 200, 1800) * .22, 0)
    return hall(b, .3, ROOM)
def approach_b():   # The roar: it roars at you, the ground answers
    b = stereo(1.7); place(b, roar(1.25) * .85, .05); place(b, roar(1.1, 180, 95) * .35, .1, pan=.3); place(b, lp(noise(1.3), 120) * env(1.3, .2, .6) * 1.4, .05)
    place(b, taiko(48, 1.0) * .8, .0); place(b, taiko(54, .8) * .6, 1.18)
    return hall(b, .36)
def approach_c():   # Heart of the planet: a huge heartbeat getting faster, strings shivering upward
    b = stereo(1.7)
    for i, at in enumerate([0, .42, .76, 1.02, 1.22]): place(b, heartbeat(44 + i) * (.6 + .09 * i), at)
    place(b, strings([D3, D3 + 3], 1.5, .7, 3000, 15) * .4 * (T(1.5) / 1.5), 0, pan=.2); place(b, strings([D2 + 12, D3 + 7], 1.5, .9, 2400, 13) * .3 * (T(1.5) / 1.5), 0, pan=-.2)
    return hall(b, .3)

# ------------------------------------------------------------------ No Drops: one strike (lands +0.45), then the dust
def strike_a():   # Thunderbolt: it screams down and the whole hall shakes
    b = stereo(2.6); place(b, whoosh(.45, 700, 4200) * .7, 0, pan=.5); place(b, boom(1.0), .45); place(b, crack(.4) * .7, .45); place(b, rubble(1.3) * .4, .5)
    return hall(b, .4)
def strike_b():   # Meteorite: a burning roar, a crunch, stones raining
    b = stereo(2.6); place(b, fire(.5) * (T(.5) / .5) ** 2 * .9, 0, pan=-.4); place(b, whoosh(.45, 300, 2000) * .5, 0, pan=-.4); place(b, boom(.9), .45); place(b, rubble(1.6, 34) * .55, .47)
    return hall(b, .36)
def strike_c():   # Falling star: a bright tone falling out of the sky into a deep bell
    b = stereo(2.8); t = T(.45); place(b, np.sin(glide(N(98), N(74), .45, 2)) * env(.45, .02, .05) * (t / .45) * .4, 0, pan=.3)
    place(b, boom(.75), .45); place(b, bell(D2, 2.2) * .7, .45); place(b, bell(D3 + 3, 2.0) * .3, .47)
    return hall(b, .45)
def dust_a():   # Ash: it crumbles and the wind takes it
    b = stereo(2.0); place(b, wind(1.8, 1400) * .6, 0); place(b, rubble(1.0, 30) * .35, 0); return hall(b, .35)
def dust_b():   # Crumble: stone falling apart piece by piece
    b = stereo(1.8); place(b, rubble(1.5, 44) * .7, 0, pan=-.2); place(b, lp(noise(1.2), 300) * env(1.2, .05, 1.0) * .8, 0); return hall(b, .3, ROOM)
def dust_c():   # A last breath: a low voice sighing out
    b = stereo(2.2); place(b, choir([D2 + 12, D3 + 3], 1.8, .3, 'o') * np.exp(-T(1.8) * 1.2) * .5, 0); place(b, wind(1.8, 700) * .35, .1); return hall(b, .45)
def none_a():   # Two notes: a slow minor fall on deep bells
    b = stereo(3.0); place(b, bell(D3 + 5, 2.4) * .6, 0, pan=-.2); place(b, bell(D3, 2.6) * .7, .55, pan=.2); place(b, strings([D2], 2.6, 1.0, 900) * .3, .2); return hall(b, .5)
def none_b():   # Cello: one falling line, alone
    b = stereo(3.0); t = T(2.2); x = lp(saw(glide(N(D3 + 7), N(D3), 2.2, .5) + .03 * np.sin(2 * np.pi * 5 * t)), 1400) * env(2.2, .25, 1.2) * .5; place(b, x, 0); return hall(b, .5)
def none_c():   # Hollow: wind through an empty place, one far bell
    b = stereo(3.2); place(b, wind(2.8, 500) * .5, 0); place(b, bell(D4 + 3, 2.4) * .25, .7, pan=.4); return hall(b, .55, CATHEDRAL)

# ------------------------------------------------------------------ a drop: seven meteorites (six, then the big one), the reveal, the close
HB = [.35, .52, .68, .82, .94, 1.04]; HBIG = 1.26
def barrage_a():   # Bombardment: each one lands harder than the last, then the big one
    b = stereo(3.4)
    for i, h in enumerate(HB):
        place(b, whoosh(.22, 600, 3600) * .3, h - .22, pan=(-.6 if i % 2 else .6)); place(b, taiko(50 + 3 * i, .8, 1.3) * (.5 + .08 * i), h, pan=(-.3 if i % 2 else .3)); place(b, crack(.2) * (.2 + .04 * i), h)
    place(b, whoosh(.3, 400, 5000) * .6, HBIG - .3); place(b, boom(1.15, 2.2), HBIG); place(b, crack(.5) * .8, HBIG); place(b, rubble(1.4, 30) * .5, HBIG + .04); place(b, cymbal(1.8, .003, 2.6) * .25, HBIG)
    return hall(b, .36)
def barrage_b():   # Rain of fire: burning rocks whistling in, the ground cracking open
    b = stereo(3.4); place(b, fire(1.5) * env(1.5, .3, .2) * .4, 0)
    for i, h in enumerate(HB):
        place(b, whoosh(.3, 1800, 500) * .45, h - .3, pan=(.7 if i % 2 else -.7)); place(b, kick(60 + 4 * i, .5) * (.7 + .05 * i), h); place(b, rubble(.4, 24) * .35, h, pan=(.4 if i % 2 else -.4))
    place(b, whoosh(.35, 2400, 300) * .7, HBIG - .35); place(b, boom(1.1, 2.4), HBIG); place(b, rubble(1.6, 38) * .6, HBIG); place(b, fire(1.6) * dec(1.6, 2.2) * .5, HBIG)
    return hall(b, .34)
def barrage_c():   # Orchestra: strings and brass stab every hit, climbing, then everyone at once
    b = stereo(3.6); steps = [0, 3, 5, 7, 8, 10]
    for i, h in enumerate(HB):
        m = D3 + steps[i]; place(b, strings([m, m + 7, m + 12], .22, .008, 4200) * .7, h, pan=-.25); place(b, brass(m - 12, .24, .015, 2400) * .55, h, pan=.25); place(b, taiko(54, .5) * .55, h)
    place(b, rev_cymbal(.22) * .4, HBIG - .22)
    place(b, strings([D3, D3 + 7, D4, D4 + 3], 1.3, .01, 4600) * .8 * dec(1.3, 2.4), HBIG); place(b, braam(D1, 1.6, 1900) * .7, HBIG); place(b, boom(1.0, 2.0) * .9, HBIG); place(b, cymbal(2.2) * .3, HBIG)
    return hall(b, .4)
def reveal_a():   # Triumph: horns and voices open into D major over a timpani roll
    b = stereo(4.2); ch = [D3, D3 + 4, D3 + 7, D4]
    place(b, brass(D2, 2.6, .12, 2400) * .5, 0); place(b, brass(D3 + 4, 2.4, .18, 2600) * .32, .05, pan=-.3); place(b, brass(D3 + 7, 2.4, .2, 2600) * .32, .08, pan=.3); place(b, brass(D4, 2.2, .3, 3000) * .28, .2)
    place(b, choir(ch, 3.0, .5, 'a') * .55, .05); place(b, strings([D2, D3, D3 + 7], 3.0, .4, 3200) * .5, 0)
    for k in range(14): place(b, taiko(74, .3, .4) * (.12 + .02 * k), k * .045)                                       # the roll
    place(b, taiko(50, 1.2) * .9, 0); place(b, cymbal(2.6) * .28, .02); place(b, shimmer(2.2, 16) * .5, .3)
    for i, m in enumerate([D4 + 12, D4 + 16, D4 + 19, D4 + 24]): place(b, glock(m, 1.4) * .5, .12 + i * .09, pan=(-.4 + .25 * i))
    return hall(b, .42)
def reveal_b():   # Glass bells: a peal of bells rising over warm strings
    b = stereo(4.2); place(b, strings([D2, D3, D3 + 4, D3 + 7], 3.2, .5, 2600) * .55, 0)
    for i, m in enumerate([D3, D3 + 7, D4, D4 + 4, D4 + 7, D4 + 12, D4 + 16]): place(b, bell(m, 2.6) * (.6 - .04 * i), i * .11, pan=(-.5 + i * .16))
    place(b, shimmer(2.4, 18) * .45, .5); place(b, kick(50, .6) * .8, 0)
    return hall(b, .5)
def reveal_c():   # Starlight: a harp sweeps up into soft voices and falling sparkles
    b = stereo(4.2)
    for i, m in enumerate([D3, D3 + 4, D3 + 7, D3 + 9, D4, D4 + 4, D4 + 7, D4 + 9, D4 + 12, D4 + 16]): place(b, harp(m, 1.8) * .7, i * .045, pan=(-.6 + i * .13))
    place(b, choir([D3 + 12, D4 + 4, D4 + 7], 3.0, .7, 'o') * .45, .25); place(b, shimmer(2.8, 26) * .55, .35); place(b, lp(np.sin(2 * np.pi * N(D1) * T(2.5)), 200) * env(2.5, .05, 1.8) * .6, 0)
    return hall(b, .5)
def close_a():   # Rest: the last chord settles
    b = stereo(2.6); place(b, strings([D2, D3 + 4, D3 + 7], 2.0, .4, 1800) * .45 * dec(2.0, 1.0), 0); place(b, bell(D4, 2.0) * .3, .1); return hall(b, .5)
def close_b():   # Echo: one soft voice fading far away
    b = stereo(2.6); place(b, choir([D4, D4 + 7], 1.8, .3, 'o') * dec(1.8, 1.4) * .4, 0); return hall(b, .6, CATHEDRAL)
def close_c():   # One bell
    b = stereo(2.6); place(b, bell(D4 + 12, 2.2) * .5, 0, pan=.2); place(b, glock(D4 + 24, 1.2) * .25, .12, pan=-.3); return hall(b, .5)

# ------------------------------------------------------------------ RNG: six star-fires climbing, the hush, the gold strike; then the jackpot and its glow
HR = [.35, .52, .67, .80, .91, 1.00]; HUSH = 1.04; GOLD = 1.48
CLIMB = [0, 3, 5, 7, 10, 12]                                             # D minor pentatonic, up an octave
def shower_a():   # Ascension: a voice joins on every strike until a whole choir holds its breath; then the sky falls
    b = stereo(5.4)
    for i, h in enumerate(HR):
        m = D3 + CLIMB[i]; place(b, whoosh(.2, 800, 4500) * .3, h - .2, pan=(-.7 if i % 2 else .7)); place(b, taiko(48 + 2 * i, .9, 1.2) * (.55 + .07 * i), h)
        place(b, choir([m, m + 12], GOLD - h + .05, .06, 'a') * (.22 + .03 * i), h, pan=(-.5 + .2 * i)); place(b, bell(m + 12, 1.6) * .35, h, pan=(.5 - .2 * i))
    place(b, riser(GOLD - .3, 200, 3200) * .4, .3)
    hush = stereo(GOLD)                                                   # the hush: everything ducks, a heartbeat, air rushing backwards
    place(b, heartbeat(40) * .9, HUSH + .04); place(b, rev_cymbal(GOLD - HUSH - .02) * .6, HUSH + .02); place(b, np.sin(glide(N(D4 + 12), N(D4 + 36), GOLD - HUSH, 2.5)) * env(GOLD - HUSH, .2, .01) * .12, HUSH)
    i0, i1 = int(HUSH * SR), int(GOLD * SR); duck = np.ones(len(b)); duck[i0:i1] = np.linspace(1, .35, i1 - i0); b *= duck[:, None]; b[i1 - 300:i1] *= np.linspace(1, 0, 300)[:, None]   # a breath of silence right before
    place(b, boom(1.4, 3.4), GOLD); place(b, sub_drop(3.0, 120, 24, 1.3) * 1.0, GOLD); place(b, braam(D1, 3.2, 2200) * .95, GOLD); place(b, crack(.6) * .9, GOLD)
    place(b, cymbal(3.6, .003, 1.2) * .45, GOLD); place(b, rubble(2.0, 40) * .5, GOLD + .05); place(b, choir([D3, D3 + 7, D4, D4 + 4], 3.2, .02, 'a') * .5 * dec(3.2, .9), GOLD)
    return hall(b, .42)
def shower_b():   # Gacha roll: the wheel ticks up through the tiers, slower, slower... gold
    b = stereo(5.2)
    for i, h in enumerate(HR):
        m = D4 + CLIMB[i]; place(b, glock(m, .8) * .55, h, pan=(-.5 + .2 * i)); place(b, glock(m + 12, .6) * .3, h + .03, pan=(.5 - .2 * i)); place(b, kick(55 + 3 * i, .35) * (.6 + .05 * i), h); place(b, bell(m - 12, 1.2) * .3, h)
    for k in range(9): at = HUSH + .02 + (k / 9) ** 1.7 * (GOLD - HUSH - .08); place(b, glock(D4 + 24 + (k % 2) * 2, .12) * (.5 - .03 * k), at, pan=(.6 if k % 2 else -.6))     # the wheel slowing
    place(b, strings([D4 + 12, D4 + 13], GOLD - HUSH, .2, 5000, 17) * .3 * (T(GOLD - HUSH) / (GOLD - HUSH)), HUSH)
    place(b, boom(1.3, 3.0), GOLD); place(b, braam(D1, 2.8, 2000) * .7, GOLD); place(b, cymbal(3.2, .003, 1.4) * .45, GOLD)
    for i, m in enumerate([D4, D4 + 4, D4 + 7, D4 + 12, D4 + 16, D4 + 19, D4 + 24]): place(b, bell(m, 2.6) * .5, GOLD + i * .035, pan=(-.6 + .2 * i))
    place(b, shimmer(2.4, 30) * .5, GOLD + .1)
    return hall(b, .42)
def shower_c():   # Titan: the horns climb a step with every strike, a drum roll swells, and it all comes down
    b = stereo(5.4)
    for i, h in enumerate(HR):
        place(b, braam(D1 + i, .5, 1200 + 240 * i) * (.45 + .07 * i), h); place(b, taiko(46 + i, .8, 1.4) * (.6 + .06 * i), h, pan=(-.3 if i % 2 else .3)); place(b, crack(.2) * .25, h)
    for k in range(22): at = HUSH + k * (GOLD - HUSH - .03) / 22; place(b, taiko(82, .2, .5) * (.1 + .028 * k), at, pan=(.25 if k % 2 else -.25))       # the roll
    place(b, riser(GOLD - HUSH, 400, 5000) * .5, HUSH); place(b, strings([D3, D3 + 1], GOLD - HUSH, .2, 4000, 19) * .4, HUSH)
    place(b, boom(1.5, 3.6), GOLD); place(b, sub_drop(3.2, 130, 22, 1.1), GOLD); place(b, braam(D1, 3.4, 2600), GOLD); place(b, braam(D1 + 12, 3.0, 2600) * .5, GOLD + .02); place(b, crack(.7), GOLD)
    place(b, cymbal(3.6, .003, 1.1) * .5, GOLD); place(b, rubble(2.2, 44) * .55, GOLD + .05)
    return hall(b, .42)

def timp_roll(d, f=73, g0=.08, g1=.4):
    x = np.zeros(int((d + .35) * SR)); n = int(d / .05)
    for k in range(n): h = taiko(f, .3, .4) * (g0 + (g1 - g0) * k / n); i = int(k * .05 * SR); x[i:i + len(h)] += h
    return x
def fanfare(b, at, notes, step=.17, g=.5, hold=1.6):
    for i, m in enumerate(notes):
        last = i == len(notes) - 1; d = hold if last else step * 1.25
        place(b, brass(m, d, .025, 3200) * g, at + i * step, pan=-.2); place(b, brass(m - 12, d, .03, 2200) * g * .7, at + i * step, pan=.2); place(b, glock(m + 24, .8) * .25, at + i * step, pan=.5)
def jackpot_a():   # Coronation: the full fanfare. Horns call, the choir answers, timpani and cymbals under a sky of bells
    b = stereo(6.2)
    place(b, strings([D2, D3, D3 + 7], 2.2, .15, 3600) * .55, 0); place(b, choir([D3, D3 + 4, D3 + 7], 2.2, .25, 'a') * .5, .05)
    fanfare(b, .05, [D3 + 12, D3 + 12, D3 + 16, D3 + 19, D4 + 12], .16, .5, 1.3)                                    # D D F# A D'
    place(b, taiko(50, 1.2) * .9, 0); place(b, cymbal(2.6) * .3, .02); place(b, timp_roll(.7, 73) * .8, 1.15)
    # the answer: bVI - bVII - I, the big cadence
    for at, ch, low in [(1.85, [D3 - 4, D3, D3 + 3, D3 + 8], D1 - 4), (2.5, [D3 - 2, D3 + 2, D3 + 5, D3 + 10], D1 - 2), (3.15, [D3, D3 + 4, D3 + 7, D4, D4 + 4], D1)]:
        fin = at > 3; d = 2.8 if fin else .75
        place(b, choir(ch, d, .08, 'a', 1.2) * (.75 if fin else .6), at); place(b, strings(ch + [low + 24], d, .05, 4200) * .6, at); place(b, braam(low, d + .2, 2000) * (.75 if fin else .5), at)
        place(b, taiko(48, 1.2) * .95, at); place(b, cymbal(3.0 if fin else 1.2, .003, 1.3 if fin else 3.0) * (.42 if fin else .25), at)
    fanfare(b, 3.2, [D4, D4 + 4, D4 + 7, D4 + 12], .1, .4, 2.0)
    place(b, shimmer(2.8, 30) * .6, 3.2); place(b, sub_drop(2.4, 90, 30, 1.6) * .8, 3.15)
    for i in range(12): place(b, glock(D4 + 12 + [0, 4, 7, 12, 16, 19, 24][i % 7], 1.0) * .35, 3.3 + i * .07, pan=(-.7 + (i % 7) * .23))
    return hall(b, .44)
def jackpot_b():   # Heaven's gate: a great choir opens chord by chord, bells ringing through it
    b = stereo(6.4)
    for at, ch, low, d in [(0, [D3, D3 + 7, D4], D1, 1.5), (1.25, [D3 - 4, D3 + 3, D3 + 8, D4], D1 - 4, 1.2), (2.2, [D3 - 2, D3 + 5, D3 + 10, D4 + 2], D1 - 2, 1.2), (3.15, [D3, D3 + 4, D3 + 7, D4, D4 + 4, D4 + 7], D1, 3.1)]:
        place(b, choir(ch, d + .3, .3, 'a', 1.15) * .8, at); place(b, choir([m + 12 for m in ch[:2]], d + .3, .45, 'o') * .3, at, pan=.3); place(b, strings(ch, d + .3, .35, 3000) * .45, at)
        place(b, lp(np.sin(2 * np.pi * N(low) * T(d + .3)), 220) * env(d + .3, .05, .5) * .8, at); place(b, bell(ch[0] + 12, 2.6) * .45, at, pan=-.4); place(b, bell(ch[-1] + 12, 2.4) * .35, at + .08, pan=.4)
    place(b, taiko(48, 1.3) * .9, 0); place(b, taiko(48, 1.3), 3.15); place(b, cymbal(3.2, .003, 1.2) * .4, 3.15); place(b, rev_cymbal(.9) * .35, 2.25); place(b, shimmer(3.0, 26) * .55, 3.2)
    return hall(b, .5, CATHEDRAL)
def supersaw(ms, d, bright=5200):
    t = T(d); x = np.zeros(len(t))
    for m in ms:
        for k in range(7): x += saw(2 * np.pi * N(m) * (1 + (k - 3) * .007) * t + k * .9)
    return lp(x / (7 * len(ms)), bright, 4)
def jackpot_c():   # Supernova: a wall of synths blooming, an arpeggio racing up it, the bass pulsing like a star
    b = stereo(6.0); ch = [D3, D3 + 4, D3 + 7, D3 + 11, D4 + 2]
    place(b, sweep_lp(supersaw(ch, 5.0), 500, 6500, .5) * env(5.0, .4, 2.2) * .9, 0)
    t = T(5.0); place(b, sat(lp(saw(2 * np.pi * N(D1) * t), 300) * (.4 + .6 * np.abs(np.sin(2 * np.pi * 2.0 * t)) ** 3), 2) * env(5.0, .02, 2.0) * .8, 0)
    arp = [D4, D4 + 4, D4 + 7, D4 + 11, D4 + 12, D4 + 16, D4 + 19, D4 + 23]
    for i in range(32): m = arp[i % 8] + 12 * (i // 16); place(b, glock(m, .5) * .4 + 0, .1 + i * .095, pan=np.sin(i * .9) * .7); place(b, supersaw([m], .14, 7000) * env(.14, .004, .1) * .3, .1 + i * .095, pan=-np.sin(i * .9) * .7)
    place(b, boom(1.0, 2.4) * .8, 0); place(b, cymbal(3.2, .003, 1.2) * .35, 0); place(b, choir([D4, D4 + 7], 4.0, 1.0, 'a') * .4, .6); place(b, shimmer(3.5, 28) * .5, .8)
    return hall(b, .4)
def glow_a():   # Afterglow: the chord rings on, voices and sparkles drifting
    b = stereo(4.6); ch = [D3, D3 + 7, D4 + 4]
    place(b, choir(ch, 4.0, 1.2, 'o') * .4, 0); place(b, strings([D2, D3 + 4], 4.0, 1.5, 1600) * .4, 0); place(b, shimmer(3.6, 12, 86, 104) * .4, .2); return hall(b, .55, CATHEDRAL)
def glow_b():   # Halo: a ring of light humming, slowly beating
    b = stereo(4.6); t = T(4.0); x = sum(np.sin(2 * np.pi * N(m) * t * (1 + .0015 * k)) for k, m in enumerate([D3 + 12, D3 + 19, D4 + 12, D4 + 16, D4 + 19])) / 5
    place(b, x * (.7 + .3 * np.sin(2 * np.pi * .9 * t)) * env(4.0, 1.0, 2.2) * .5, 0); place(b, lp(np.sin(2 * np.pi * N(D1) * t), 200) * env(4.0, .8, 2.4) * .5, 0); place(b, shimmer(3.4, 6) * .3, .4); return hall(b, .5)
def glow_c():   # Music box: a little tune turning over the afterglow
    b = stereo(4.8); tune = [D4 + 12, D4 + 16, D4 + 19, D4 + 24, D4 + 19, D4 + 16, D4 + 21, D4 + 19, D4 + 16, D4 + 12, D4 + 14, D4 + 12]
    for i, m in enumerate(tune): place(b, glock(m, 1.4) * .5, i * .26, pan=(-.4 if i % 2 else .4))
    place(b, strings([D3, D3 + 7], 3.8, 1.2, 1500) * .3, 0); return hall(b, .5)

# ------------------------------------------------------------------ write
FAMS = ['rise', 'approach', 'strike', 'dust', 'none', 'barrage', 'reveal', 'close', 'shower', 'jackpot', 'glow']
PEAK = {'rise': .8, 'approach': .8, 'strike': .95, 'dust': .5, 'none': .55, 'barrage': .95, 'reveal': .85, 'close': .42, 'shower': .97, 'jackpot': .9, 'glow': .5}
def master(x, peak=.92):
    x = np.nan_to_num(x); x = np.stack([hp(x[:, c], 24) for c in range(2)], 1)
    x /= max(1e-6, np.abs(x).max()); x = np.tanh(x * 1.15) / np.tanh(1.15)                              # a soft limiter: dense and loud without clipping
    nz = np.where(np.abs(x).max(1) > 0.0015)[0]; x = x[:nz[-1] + 600] if len(nz) else x
    x[-1200:] *= np.linspace(1, 0, 1200)[:, None]; x[:60] *= np.linspace(0, 1, 60)[:, None]
    return x / np.abs(x).max() * peak
if __name__ == '__main__':
    import sys
    only = sys.argv[1:]
    sj = json.load(open(os.path.join(RES, 'sounds.json'))); ej = json.load(open(os.path.join(RES, 'envelopes.json'))); done = []
    for fam in FAMS:
        for v in 'abc':
            name = f'slr_{fam}_{v}'
            if only and not any(o in name for o in only): continue
            x = master(globals()[f'{fam}_{v}'](), PEAK[fam])
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
