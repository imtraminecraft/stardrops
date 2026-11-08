#!/usr/bin/env python3
"""Stardrops: the Slayer soundtracks. Not a sound per beat: one piece of music per animation, scored to its timeline (SlayerHole):
  0.1 s the hole opens . 0.45-1.05 the boss beams in . 1.2-3.3 the pull (continuous, speeding up) . 3.4 the turn
  slr_nothing (5.5 s): the scene is swallowed, a wink, two soft minor notes under "Nothing dropped".
  slr_drop    (7 s):   the jets fire, the drop is spat out, the tension resolves into a bright major chord and a glassy chime.
  slr_rng     (9.5 s): the collapse, the supernova, then the pulsar: a ticking groove over a choir, closing on a wide chord.
Style: a dark drone in D that bends upward as the pull tightens (tension), sub-bass heartbeats that come faster and faster,
bit-crushed tearing that grows as the boss comes apart; the endings are where it opens up.
Writes sounds/slr_*.ogg and merges them into sounds.json and envelopes.json. Run from the repo root."""
import os, json, subprocess, tempfile, wave
src = open(os.path.join(os.path.dirname(os.path.abspath(__file__)), 'make_scene_sounds.py')).read()
exec(src[:src.index('# ---- 1.0b')])
__file__ = os.path.abspath(__file__)

def crush(x, bits=5, hold=6): y = np.repeat(x[::hold], hold)[:len(x)]; q = 2 ** bits; return np.round(y * q) / q
def rev(x): return x[::-1].copy()
def glide_tone(f0, f1, d, shape='saw', curve=2.0):
    t = T(d); fr = f0 + (f1 - f0) * (t / d) ** curve; ph = 2 * np.pi * np.cumsum(fr) / SR
    return {'saw': 2 * ((ph / (2 * np.pi)) % 1) - 1, 'sin': np.sin(ph), 'tri': 2 / np.pi * np.arcsin(np.sin(ph))}[shape]
def heart(f=55, d=.35): t = T(d); return np.sin(2 * np.pi * f * t * (1 - .3 * t / d)) * dec(t, 9) * np.minimum(1, t / .004)

def intro_and_pull(total):
    b = mix(total)
    # the hole opens: a reversed swell into a low bend-down
    at(b, rev(lp(noise(.6), 2200) * dec(T(.6), 4.5)) * .35, 0)
    at(b, osc(98, 1.0, glide=36) * dec(T(1.0), 2.4) * .8, .1)
    # the boss beams in: a rising shimmer
    at(b, hp(sweep_noise(.65, 900, 7000, .2), 600) * env(T(.65), .3, .2) * .2, .45)
    # the drone under it all: D, bending upward as the pull tightens (1.0 s -> 3.4 s), louder, brighter
    d = 2.6; t = T(d); dr = lp(glide_tone(N(38), N(38) * 1.5, d, 'saw', 2.4) + glide_tone(N(45), N(45) * 1.5, d, 'saw', 2.4) * .6, 300)
    dr += lp(glide_tone(N(50), N(50) * 1.5, d, 'saw', 2.4), 900) * .25
    at(b, dr * (0.15 + 0.85 * (t / d) ** 1.6) * .35, .85)
    # heartbeats, closer and closer together
    tk = 1.2; gap = .62
    while tk < 3.35: at(b, heart(52, .3) * (.55 + .45 * (tk - 1.2) / 2.2), tk); at(b, heart(46, .25) * .45, tk + .14); tk += gap; gap = max(.17, gap * .78)
    # the tearing: crushed noise bursts, more and more of them as the boss comes apart
    rr = np.random.default_rng(7)
    for _ in range(46):
        u = rr.uniform(0, 1) ** .6; t0 = 1.35 + u * 1.95
        at(b, crush(bp(noise(.05), 800, 6000), 4, 7) * dec(T(.05), 40) * (.08 + .3 * u), t0)
    # the last breath before the turn: a short rising suck
    at(b, (sweep_noise(.45, 400, 5000, .25) * (T(.45) / .45) ** 2) * .35, 2.95)
    return b

def slr_nothing():
    b = intro_and_pull(5.6)
    # the scene is swallowed: everything rushes inward, rising, then a hard cut
    d = 1.05; t = T(d); at(b, sweep_noise(d, 300, 7000, .25) * (t / d) ** 2.4 * .45 + glide_tone(80, 900, d, 'saw', 3) * (t / d) ** 2 * .12, 3.45)
    at(b, osc(3000, .05) * dec(T(.05), 70) * .45, 4.5)                                             # the wink
    for i, m in enumerate([62, 58]): at(b, fm(N(m), 1.2, ratio=2.0, index=.5, k=2.8) * .22, 4.75 + i * .32)   # D, then a soft B flat: nothing
    return reverb(b, .35, .3)

def slr_drop():
    b = intro_and_pull(7.2)
    at(b, osc(160, .24, 'tri', glide=1500) * dec(T(.24), 6) * .55, 3.4)                          # the spit
    at(b, hp(noise(.8), 2500) * env(T(.8), .01, .65) * .25, 3.42)                                 # the jets
    at(b, osc(85, .6, glide=45) * dec(T(.6), 5) * .6, 3.4)
    at(b, supersaw([62, 66, 69, 74], 3.0, det=.012, cut=3200, a=.12) * .4, 3.45)                    # the release: D major
    for i, m in enumerate([81, 86, 90, 93]): at(b, (fm(N(m), 1.5, ratio=2.0, index=.7, k=2.6) + fm(N(m) * 1.004, 1.5, ratio=2.0, index=.7, k=2.6)) * (.22 - .03 * i), 3.6 + i * .09)
    at(b, pad([62, 69, 74, 78], 2.6, 'tri', .004, .5) * .25, 4.2)
    return reverb(b, .4, .35)

def slr_rng():
    b = intro_and_pull(9.6)
    at(b, rev(osc(60, .25, glide=220) * dec(T(.25), 3)) * .5, 3.38)                              # the collapse (in-breath)
    at(b, impact(52, 17, 3.0, 1.0, 1.7) * 1.1, 3.62)                                             # the supernova
    at(b, lp(noise(2.4), 1300) * dec(T(2.4), 1.5) * .5, 3.62)
    at(b, np.tanh(3 * fm(N(57), 2.4, ratio=1.41, index=4, k=1.3)) * .2, 3.64)
    at(b, choir([62, 69, 74, 78], 4.6, .8) * .35, 4.0)                                             # a choir rising out of it
    at(b, supersaw([50, 57, 62, 66, 69], 5.0, det=.016, cut=2400, a=1.0) * .3, 4.1)
    # the pulsar: a ticking groove, eighth notes at 132 bpm, accents on the beat, a soft sub under each bar
    step = 60 / 132 / 2; tk = 4.0; i = 0
    while tk < 8.6:
        acc = 1.0 if i % 2 == 0 else .55
        at(b, osc(1400, .03) * dec(T(.03), 130) * .3 * acc, tk); at(b, osc(2800, .02) * dec(T(.02), 200) * .12 * acc, tk)
        if i % 8 == 0: at(b, heart(49, .4) * .5, tk)
        tk += step; i += 1
    for i, m in enumerate([86, 90, 93, 98]): at(b, glock(N(m), 1.6) * (.22 - .04 * i), 4.3 + i * .16)
    at(b, pad([62, 69, 74, 81], 2.6, 'sin', .005, .6) * .3, 6.8)                                   # the close: a wide D
    return reverb(b, .45, .38)

OUT = {'slr_nothing': slr_nothing(), 'slr_drop': slr_drop(), 'slr_rng': slr_rng()}
def norm(x, peak=.85): return x / max(1e-6, np.abs(x).max()) * peak
sj = json.load(open(os.path.join(RES, 'sounds.json'))); ej = json.load(open(os.path.join(RES, 'envelopes.json')))
for k in [k for k in sj if k.startswith('sl_')]: sj.pop(k, None); ej.pop(k, None); p = os.path.join(RES, 'sounds', k + '.ogg'); os.path.exists(p) and os.remove(p)   # the old per-beat set is gone
for name, x in OUT.items():
    x = norm(np.nan_to_num(x)); x[-600:] *= np.linspace(1, 0, 600)
    nz = np.where(np.abs(x) > 0.002)[0]; x = x[:nz[-1] + 400] if len(nz) else x
    with tempfile.TemporaryDirectory() as td:
        wp = os.path.join(td, 'a.wav')
        with wave.open(wp, 'wb') as w:
            w.setnchannels(1); w.setsampwidth(2); w.setframerate(SR); w.writeframes((x * 32767).astype('<i2').tobytes())
        subprocess.run(['ffmpeg', '-y', '-loglevel', 'error', '-i', wp, '-c:a', 'libvorbis', '-q:a', '4', os.path.join(RES, 'sounds', name + '.ogg')], check=True)
    d = round(len(x) / SR, 2); n = int(d * 40) + 1; hop = SR // 40
    e = [float(np.sqrt(np.mean(x[i * hop:(i + 1) * hop] ** 2))) if i * hop < len(x) else 0 for i in range(n)]
    m = max(e) or 1
    ej[name] = {'d': d, 'e': [int(round(v / m * 99)) for v in e]}
    sj[name] = {'sounds': [{'name': 'fancydrops:' + name, 'stream': True}]}
json.dump(sj, open(os.path.join(RES, 'sounds.json'), 'w'), indent=2)
json.dump(ej, open(os.path.join(RES, 'envelopes.json'), 'w'), separators=(',', ':'))
print('wrote', ' '.join(f'{k} ({len(v)/SR:.1f}s)' for k, v in OUT.items()))
