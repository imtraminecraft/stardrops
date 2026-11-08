#!/usr/bin/env python3
"""Stardrops: the Slayer black hole's sounds, split into the five moments of the animation (SlayerHole / AnimationManager.cueTimes),
three takes of each so the player can pick in the Sounds tab:
  open    (0.1 s)  the hole tears open                      slr_open_a/b/c
  pull    (1.15 s) the pull: heartbeats on the hole's throb  slr_pull_a/b/c   (beats at +0.05 +0.67 +1.15 +1.53 +1.82 +2.05, turn at +2.25)
  then by type, at the turn (3.4 s) and after it:
  No Drops: swallow (slr_swallow_*), wink (slr_wink_*), nothing (slr_none_*)
  a drop:   spit (slr_spit_*), reveal (slr_reveal_*), close (slr_close_*)
  RNG:      supernova (slr_nova_*), fanfare (slr_fanfare_*), pulsar (slr_pulsar_*)
Writes sounds/slr_*.ogg (removing the old one-piece soundtracks) and merges them into sounds.json and envelopes.json. Run from the repo root."""
import os, json, subprocess, tempfile, wave
src = open(os.path.join(os.path.dirname(os.path.abspath(__file__)), 'make_scene_sounds.py')).read()
exec(src[:src.index('# ---- 1.0b')]); exec(src[src.index('def chip'):src.index('def std_set')])
__file__ = os.path.abspath(__file__)

def crush(x, bits=5, hold=6): y = np.repeat(x[::hold], hold)[:len(x)]; q = 2 ** bits; return np.round(y * q) / q
def rev(x): return x[::-1].copy()
def glide_tone(f0, f1, d, shape='saw', curve=2.0):
    t = T(d); fr = f0 + (f1 - f0) * (t / d) ** curve; ph = 2 * np.pi * np.cumsum(fr) / SR
    return {'saw': 2 * ((ph / (2 * np.pi)) % 1) - 1, 'sin': np.sin(ph), 'tri': 2 / np.pi * np.arcsin(np.sin(ph))}[shape]
def heart(f=55, d=.35): t = T(d); return np.sin(2 * np.pi * f * t * (1 - .3 * t / d)) * dec(t, 9) * np.minimum(1, t / .004)
BEATS = [.05, .67, 1.15, 1.53, 1.82, 2.05]; TURN = 2.25

# ---- open
def open_a():   # Rift: a reversed swell into a low bend-down
    b = mix(1.3); at(b, rev(lp(noise(.5), 2200) * dec(T(.5), 4.5)) * .4, 0); at(b, osc(98, 1.0, glide=36) * dec(T(1.0), 2.4) * .8, .45); return b
def open_b():   # Gravity well: a deep sub drop under a rumble
    b = mix(1.5); at(b, impact(70, 22, 1.4, .4, 1.6) * .8, 0); at(b, lp(noise(1.4), 400) * dec(T(1.4), 2.2) * .5, 0); return b
def open_c():   # Event horizon: a glassy shimmer bending down into a whoosh
    b = mix(1.4); t = T(1.2); at(b, glide_tone(N(88), N(64), 1.2, 'sin', .6) * dec(t, 2.2) * .3 + glide_tone(N(95), N(71), 1.2, 'sin', .6) * dec(t, 2.6) * .2, 0)
    at(b, sweep_noise(1.0, 6000, 300, .3) * env(T(1.0), .05, .6) * .35, .1); return b

# ---- pull (each lasts to the turn)
def pull_bed(d, base=38):
    t = T(d); dr = lp(glide_tone(N(base), N(base) * 1.5, d, 'saw', 2.4) + glide_tone(N(base + 7), N(base + 7) * 1.5, d, 'saw', 2.4) * .6, 320)
    return dr * (.2 + .8 * (t / d) ** 1.6)
def pull_a():   # Heartbeat: a drone bending up, sub heartbeats, crushed tearing growing
    d = TURN + .1; b = mix(d); at(b, pull_bed(d) * .35, 0)
    for i, bt in enumerate(BEATS): k = .55 + .45 * i / 5; at(b, heart(52, .3) * k, bt); at(b, heart(46, .25) * .45 * k, bt + .14)
    rr = np.random.default_rng(7)
    for _ in range(40): u = rr.uniform(0, 1) ** .6; at(b, crush(bp(noise(.05), 800, 6000), 4, 7) * dec(T(.05), 40) * (.08 + .3 * u), .15 + u * 1.95)
    at(b, sweep_noise(.45, 400, 5000, .25) * (T(.45) / .45) ** 2 * .35, TURN - .45); return b
def pull_b():   # Spaghettify: glitchy, a rising whine stretched thin, kicks on the beats
    d = TURN + .1; b = mix(d); t = T(d); at(b, crush(glide_tone(N(57), N(81), d, 'saw', 1.8), 5, 5) * (.1 + .25 * (t / d) ** 2), 0)
    for i, bt in enumerate(BEATS): at(b, thump(140, 40, .25, 14) * (.5 + .4 * i / 5), bt); at(b, crush(hp(noise(.08), 1500), 3, 9) * dec(T(.08), 30) * .3, bt + .02)
    rr = np.random.default_rng(11)
    for _ in range(60): u = rr.uniform(0, 1) ** .5; at(b, chip(rr.choice([880, 1320, 1760, 2640]), .03) * dec(T(.03), 60) * (.05 + .15 * u), .1 + u * 2.0)
    return b
def pull_c():   # Tidal choir: a choir rising, low toms on the beats
    d = TURN + .1; b = mix(d); t = T(d); at(b, choir([50, 57, 62], d, .6) * (.15 + .6 * (t / d) ** 2) * .5, 0)
    at(b, pull_bed(d, 26) * .25, 0)
    for i, bt in enumerate(BEATS): at(b, osc(90, .4, glide=50) * dec(T(.4), 8) * (.5 + .4 * i / 5), bt)
    at(b, riser(.8, 300, 4000) * .4, TURN - .8); return b

# ---- No Drops
def swallow_a():   # Implode: everything rushes inward, rising, then a hard cut
    d = 1.0; t = T(d); return sweep_noise(d, 300, 7000, .25) * (t / d) ** 2.4 * .5 + glide_tone(80, 900, d, 'saw', 3) * (t / d) ** 2 * .14
def swallow_b():   # Gulp: one huge low gulp, a reversed tail
    b = mix(1.2); at(b, osc(220, .5, glide=40) * dec(T(.5), 4) * .8, 0); at(b, rev(lp(noise(.6), 1200) * dec(T(.6), 5)) * .4, .4); return b
def swallow_c():   # Vanish: a falling glide thinning to nothing
    d = 1.1; t = T(d); return (glide_tone(N(76), N(40), d, 'tri', .7) * .5 + glide_tone(N(83), N(47), d, 'sin', .7) * .3) * dec(t, 2.2)
def wink_a(): return osc(3000, .06) * dec(T(.06), 70) * .6
def wink_b(): b = mix(.3); at(b, osc(500, .12, glide=1800) * dec(T(.12), 25) * .6, 0); at(b, hp(noise(.03), 3000) * dec(T(.03), 90) * .25, .1); return b
def wink_c(): b = mix(.3); at(b, chip(N(88), .05) * .4, 0); at(b, chip(N(93), .07) * .35, .09); return b
def none_a():   # two soft minor notes
    b = mix(1.8); [at(b, fm(N(m), 1.3, ratio=2.0, index=.5, k=2.8) * .3, i * .32) for i, m in enumerate([62, 58])]; return b
def none_b():   # a sad sine falling a fifth
    b = mix(1.8); at(b, (osc(N(69), 1.6, glide=N(62)) * .4 + osc(N(57), 1.6, glide=N(50)) * .25) * env(T(1.6), .05, .9), 0); return b
def none_c():   # radio static fading out, one low note
    b = mix(1.8); at(b, bp(noise(1.4), 1500, 5000) * dec(T(1.4), 2.5) * .25, 0); at(b, pad([50, 57], 1.5, 'tri', .004, .5) * .3, .1); return b

# ---- a drop
def spit_a():   # thwup and jets
    b = mix(1.0); at(b, osc(160, .24, 'tri', glide=1500) * dec(T(.24), 6) * .6, 0); at(b, hp(noise(.8), 2500) * env(T(.8), .01, .65) * .25, .02)
    at(b, osc(85, .6, glide=45) * dec(T(.6), 5) * .6, 0); return b
def spit_b():   # ejection: a laser zap and a boom
    b = mix(1.0); t = T(.3); at(b, (2 * ((np.cumsum(1800 - 1500 * t / .3) / SR) % 1) - 1) * dec(t, 9) * .35, 0); at(b, impact(110, 40, .8, .8, 1.0) * .6, .02); return b
def spit_c():   # jet stream: a hiss rising and bursting
    b = mix(1.0); at(b, sweep_noise(.5, 800, 9000, .3) * (T(.5) / .5) * .4, 0); at(b, crunch(.4, 1.2) * .4, .45); return b
def reveal_a(): # D major release and glassy chimes
    b = mix(3.0); at(b, supersaw([62, 66, 69, 74], 2.8, det=.012, cut=3200, a=.1) * .4, 0)
    [at(b, (fm(N(m), 1.5, ratio=2.0, index=.7, k=2.6) + fm(N(m) * 1.004, 1.5, ratio=2.0, index=.7, k=2.6)) * (.22 - .03 * i), .15 + i * .09) for i, m in enumerate([81, 86, 90, 93])]; return b
def reveal_b(): # glass bells arpeggio
    b = mix(2.4); [at(b, bell(N(m), 1.6) * (.35 - .04 * i), i * .11) for i, m in enumerate([74, 78, 81, 86, 90])]; return b
def reveal_c(): # starlight: sparkles over a soft pad
    b = mix(2.6); at(b, pad([62, 69, 74, 78], 2.4, 'sin', .004, .3) * .35, 0); at(b, sparkle(2.0, 18, gain=.2), .05); return b
def close_a(): return rev(lp(noise(.7), 3000) * dec(T(.7), 4)) * .35                        # a reversed sigh
def close_b(): return pad([74, 69, 62], 1.4, 'tri', .004, .2, .9) * .35                     # a soft chord fading
def close_c(): return glock(N(86), 1.2) * .45                                                # one soft bell

# ---- RNG
def nova_a():   # in-breath, a sub boom, air
    b = mix(3.0); at(b, rev(osc(60, .25, glide=220) * dec(T(.25), 3)) * .5, 0); at(b, impact(52, 17, 2.6, 1.0, 1.7) * 1.1, .24)
    at(b, lp(noise(2.2), 1300) * dec(T(2.2), 1.5) * .5, .24); return b
def nova_b():   # shockwave: two booms, an air blast
    b = mix(3.0); at(b, impact(70, 25, 2.0, 1.2, 1.4), .05); at(b, impact(50, 20, 2.0, .6, 1.6) * .7, .5); at(b, sweep_noise(1.5, 6000, 200, .4) * dec(T(1.5), 1.8) * .5, .05); return b
def nova_c():   # collapse: a distorted ring and a boom
    b = mix(3.0); at(b, np.tanh(3 * fm(N(57), 2.4, ratio=1.41, index=4, k=1.3)) * .3, .1); at(b, impact(60, 20, 2.4, .8, 1.5) * .9, 0); return b
def fanfare_a(): # choir and a wide supersaw
    b = mix(4.0); at(b, choir([62, 69, 74, 78], 3.8, .6) * .4, 0); at(b, supersaw([50, 57, 62, 66, 69], 3.8, det=.016, cut=2400, a=.6) * .3, .1); return b
def fanfare_b(): # cosmic fanfare: rising brassy stabs
    b = mix(3.5); [at(b, supersaw(c, .5 if i < 3 else 2.6, det=.01, cut=3600, a=.01) * .35, i * .22) for i, c in enumerate([[62, 66, 69], [64, 67, 71], [66, 69, 74], [62, 69, 74, 78]])]; return b
def fanfare_c(): # a glock run over a pad
    b = mix(3.5); at(b, pad([62, 69, 74], 3.0, 'sin', .005, .4) * .35, 0); [at(b, glock(N(m), 1.4) * (.3 - .03 * i), .1 + i * .12) for i, m in enumerate([74, 78, 81, 86, 90, 93])]; return b
def pulsar_a():  # a ticking groove, 132 bpm eighths, a sub on each bar
    d = 3.0; b = mix(d + .4); step = 60 / 132 / 2; tk = 0; i = 0
    while tk < d:
        acc = 1.0 if i % 2 == 0 else .55
        at(b, osc(1400, .03) * dec(T(.03), 130) * .3 * acc, tk); at(b, osc(2800, .02) * dec(T(.02), 200) * .12 * acc, tk)
        if i % 8 == 0: at(b, heart(49, .4) * .5, tk)
        tk += step; i += 1
    at(b, pad([62, 69, 74, 81], 1.6, 'sin', .005, .6) * .3, d - 1.4); return b
def pulsar_b():  # beacon: slow pulsing beeps over a hum
    d = 3.0; b = mix(d + .3); at(b, lp(osc(N(38), d) + osc(N(45), d) * .5, 400) * env(T(d), .3, .8) * .3, 0)
    for k in range(6): at(b, osc(N(81), .18) * dec(T(.18), 14) * .35, k * .5); at(b, osc(N(93), .1) * dec(T(.1), 20) * .15, k * .5 + .02)
    return b
def pulsar_c():  # quasar: a sweeping filtered pad with ticks
    d = 3.0; b = mix(d + .3); t = T(d); sw = supersaw([50, 57, 62, 69], d, det=.014, cut=1800, a=.3) * (.5 + .5 * np.sin(2 * np.pi * 1.5 * t)) * .35; at(b, sw, 0)
    for k in range(12): at(b, osc(2200, .02) * dec(T(.02), 160) * .2, k * .25)
    return b

OUT = {}
for fam in ['open', 'pull', 'swallow', 'wink', 'none', 'spit', 'reveal', 'close', 'nova', 'fanfare', 'pulsar']:
    for v in 'abc': OUT[f'slr_{fam}_{v}'] = reverb(globals()[f'{fam}_{v}'](), .35, .3)

def norm(x, peak=.85): return x / max(1e-6, np.abs(x).max()) * peak
sj = json.load(open(os.path.join(RES, 'sounds.json'))); ej = json.load(open(os.path.join(RES, 'envelopes.json')))
for k in [k for k in list(sj) if k.startswith('slr_') or k.startswith('sl_')]:                      # the old soundtracks and per-beat set go
    sj.pop(k, None); ej.pop(k, None); p = os.path.join(RES, 'sounds', k + '.ogg'); os.path.exists(p) and os.remove(p)
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
    sj[name] = {'sounds': [{'name': 'fancydrops:' + name, 'stream': len(x) / SR > 2.5}]}
json.dump(sj, open(os.path.join(RES, 'sounds.json'), 'w'), indent=2)
json.dump(ej, open(os.path.join(RES, 'envelopes.json'), 'w'), separators=(',', ':'))
print('wrote', len(OUT), 'sounds:', ' '.join(f'{k}({len(v)/SR:.1f}s)' for k, v in OUT.items()))
