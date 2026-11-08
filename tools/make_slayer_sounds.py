#!/usr/bin/env python3
"""Stardrops: the Slayer meteor scene's sounds (SlayerScene / AnimationManager.cueTimes), five moments, three takes of each:
  rise      (0.45 s)  the boss climbs out of its spawn                slr_rise_a/b/c
  approach  (1.45 s)  it walks toward you                              slr_approach_a/b/c
  then by type:
  No Drops: strike (2.55 s, lands +0.45), dust (3.05 s), nothing (4.0 s)
  a drop:   barrage (2.1 s, lands +.35 +.52 +.68 +.82 +.94 +1.04, big +1.26), reveal (3.4 s), close (end)
  RNG:      shower (1.9 s, lands +.35 +.52 +.67 +.80 +.91 +1.00, big +1.48, climbing like a gacha roll), jackpot (3.4 s), glow (4.0 s)
Writes sounds/slr_*.ogg, removes every older slr_/sl_ sound and merges into sounds.json and envelopes.json. Run from the repo root."""
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

def whistle(d, f0=2400, f1=500): t = T(d); return add(osc(f0, d, glide=f1) * .35, bp(noise(d), 800, 5000) * .3)[:len(t)] * (t / d) ** 1.5
def boom(s=1.0, f0=90, f1=30): return impact(f0, f1, 1.1 * s, .7, 1.4) * s
def thud(f=60, d=.22): return add(thump(f * 1.6, f, d, 14) * .9, lp(noise(d), 500) * dec(T(d), 22) * .4)

# ---- rise: the boss climbs out of its spawn
def rise_a():   # Grave: earth cracking, a low groan swelling up
    b = mix(1.5); [at(b, crunch(.25, .8) * .5, k * .18) for k in range(4)]; at(b, lp(glide_tone(N(33), N(40), 1.2, 'saw', 1.2), 500) * env(T(1.2), .4, .5) * .5, .2); return b
def rise_b():   # Summon: a reversed swell into a thump
    b = mix(1.3); at(b, rev(lp(noise(.7), 2500) * dec(T(.7), 4)) * .45, 0); at(b, thud(55, .5) * .9, .68); at(b, osc(N(43), .6, glide=N(36)) * dec(T(.6), 4) * .4, .68); return b
def rise_c():   # Portal: a rising shimmer with dirt falling
    b = mix(1.4); at(b, sweep_noise(1.0, 300, 3500, .3) * env(T(1.0), .5, .3) * .35, 0); at(b, pad([45, 52, 57], 1.2, 'tri', .006, .5) * .25, .1)
    for k in range(10): at(b, stone(.08, 300 + 80 * k) * .15, .5 + k * .07)
    return b

# ---- approach: footsteps toward you, tension rising
STEPS = [0, .27, .54, .8]
def approach_a():   # Stomp: heavy steps getting closer
    b = mix(1.4); [at(b, thud(52, .3) * (.45 + .17 * i), s) for i, s in enumerate(STEPS)]; return b
def approach_b():   # Drumline: steps on low toms under a rising drone
    b = mix(1.4); [at(b, thump(140, 70, .25, 12) * (.4 + .15 * i), s) for i, s in enumerate(STEPS)]
    at(b, lp(glide_tone(N(38), N(45), 1.2, 'saw', 1.5), 700) * env(T(1.2), .6, .2) * .3, 0); return b
def approach_c():   # Heartbeat: a double heartbeat on every step
    b = mix(1.4)
    for i, s in enumerate(STEPS): at(b, heart(55, .3) * (.5 + .15 * i), s); at(b, heart(50, .3) * (.35 + .1 * i), s + .12)
    return b

# ---- No Drops: one strike, then dust
def strike_a():   # Thunderbolt: a whistle and one big boom
    b = mix(1.8); at(b, whistle(.45), 0); at(b, boom(1.0), .45); return b
def strike_b():   # Meteor: a roaring fall, a crunch and debris
    b = mix(1.8); at(b, sweep_noise(.45, 600, 2400, .3) * (T(.45) / .45) ** 2 * .6, 0); at(b, boom(.9, 120, 40), .45); at(b, crunch(.6, 1.0) * .5, .47); return b
def strike_c():   # Falling star: a bright descending tone and a soft thud
    b = mix(1.6); at(b, osc(N(96), .45, glide=N(72)) * env(T(.45), .02, .1) * .35, 0); at(b, thud(60, .5) * .8, .45); at(b, bell(N(60), 1.0) * .25, .45); return b
def dust_a(): return add(lp(noise(1.2), 2500) * env(T(1.2), .05, .9) * .3, crackle(1.2, 30, .3)[:len(T(1.2))] * dec(T(1.2), 2)[:len(crackle(1.2, 30, .3))] * .5)   # ash blowing away
def dust_b():   # crumble: little stones tumbling
    b = mix(1.2); [at(b, stone(.1, 200 + 90 * (k % 5), .8) * (.3 - .02 * k), k * .08) for k in range(12)]; return b
def dust_c(): return add(rev(hp(noise(.9), 1800) * dec(T(.9), 5)) * .25, sweep_noise(.9, 3000, 400, .3) * env(T(.9), .1, .6) * .3)   # a sigh of wind

# ---- a drop: a barrage, then the reveal
HB = [.35, .52, .68, .82, .94, 1.04]; HBIG = 1.26
def barrage(small, big, lead=True):
    b = mix(2.8)
    if lead: at(b, sweep_noise(.35, 500, 2500, .3) * (T(.35) / .35) ** 2 * .35, 0)
    for i, h in enumerate(HB): at(b, small(i), h)
    at(b, big(), HBIG); return b
def barrage_a(): return barrage(lambda i: add(boom(.35, 140, 50), crunch(.2, .8) * .2), lambda: add(boom(1.1), crunch(.6) * .4))                                 # Bombardment
def barrage_b(): return barrage(lambda i: add(thud(70 + 6 * i, .25) * .6, whistle(.06, 3000, 1500) * .4), lambda: add(boom(1.0, 100, 32), whistle(.12) * .3))     # Rain of fire
def barrage_c(): return barrage(lambda i: add(chip(N(60 + 2 * i), .09) * .3, thud(65, .2) * .5), lambda: add(boom(1.0), chip(N(72), .3) * .3), False)          # Arcade

# ---- RNG: a shower that climbs like a gacha roll, then the jackpot
HR = [.35, .52, .67, .80, .91, 1.00]; HRBIG = 1.48
SCALE = [72, 74, 76, 79, 81, 84]
def shower(tick, big, sus):
    b = mix(3.0); at(b, riser(1.45, 300, 5000) * .45, 0)
    for i, h in enumerate(HR): at(b, tick(i), h)
    at(b, sus(), 1.05); at(b, big(), HRBIG); return b
def shower_a():   # Gacha roll: a rising chime on every hit, a held breath, a crash
    return shower(lambda i: add(glock(N(SCALE[i]), .5) * .4, thud(70, .2) * .4), lambda: add(boom(1.4, 80, 25), sparkle(1.4, 20, gain=.25)), lambda: osc(N(84), .43, vib=.02, vibf=14) * env(T(.43), .1, .05) * .18)
def shower_b():   # Star shower: bells climbing, a drumroll, a cymbal crash
    def roll(): x = mix(.43); [at(x, thump(160, 90, .06, 30) * (.15 + .5 * k / 14), k * .03) for k in range(14)]; return x
    return shower(lambda i: add(bell(N(SCALE[i] - 12), .6) * .35, thud(60, .2) * .4), lambda: add(boom(1.3), hp(noise(1.5), 4000) * dec(T(1.5), 2.5) * .3), roll)
def shower_c():   # Slot machine: chiptune ticks climbing, a spin, a big chord
    def spin(): x = mix(.43); [at(x, chip(N(84 + (k % 3) * 3), .03) * .2, k * .035) for k in range(12)]; return x
    return shower(lambda i: add(chip(N(SCALE[i]), .12) * .35, thud(65, .15) * .35), lambda: add(boom(1.2), chip(N(84), .5) * .25), spin)
def jackpot_a():   # Jackpot: a bright fanfare and coins
    b = mix(3.0); at(b, fanfare([72, 76, 79, 84], .09, 1.8, inst=glock, gain=.4), 0); at(b, supersaw([60, 67, 72, 76], 2.4, det=.014, cut=3500, a=.05) * .35, .3)
    for k in range(16): at(b, bell(N(96 + (k % 4) * 2), .4) * .12, .3 + k * .1)
    return b
def jackpot_b():   # Choir: a heavenly chord opening up
    b = mix(3.0); at(b, choir([60, 64, 67, 72, 76], 2.8, .2) * .45, 0); at(b, sparkle(2.4, 22, gain=.22), .1); return b
def jackpot_c():   # Level up: a quick chip arpeggio into a big chord
    b = mix(2.6); at(b, arp([72, 76, 79, 84, 88, 91], chip, .06, .3, .35), 0); at(b, supersaw([60, 64, 67, 72], 2.0, det=.012, cut=3000, a=.02) * .35, .38); return b
def glow_a(): return add(pad([72, 76, 79, 84], 2.2, 'sin', .004, .4) * .3, sparkle(1.8, 14, gain=.15))            # a warm glow, sparkles
def glow_b():   # a halo humming, slowly pulsing
    t = T(2.2); x = pad([60, 67, 72], 2.2, 'tri', .005, .5); return x * (.7 + .3 * np.sin(2 * np.pi * 2 * T(len(x) / SR)[:len(x)])) * .3
def glow_c():   # music box twinkles
    b = mix(2.4); [at(b, mbox(N(m), .8) * .25, i * .22) for i, m in enumerate([84, 88, 91, 96, 91, 88])]; return b

def none_a():   # two soft minor notes
    b = mix(1.8); [at(b, fm(N(m), 1.3, ratio=2.0, index=.5, k=2.8) * .3, i * .32) for i, m in enumerate([62, 58])]; return b
def none_b():   # a sad sine falling a fifth
    b = mix(1.8); at(b, (osc(N(69), 1.6, glide=N(62)) * .4 + osc(N(57), 1.6, glide=N(50)) * .25) * env(T(1.6), .05, .9), 0); return b
def none_c():   # radio static fading out, one low note
    b = mix(1.8); at(b, bp(noise(1.4), 1500, 5000) * dec(T(1.4), 2.5) * .25, 0); at(b, pad([50, 57], 1.5, 'tri', .004, .5) * .3, .1); return b

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


OUT = {}
for fam in ['rise', 'approach', 'strike', 'dust', 'none', 'barrage', 'reveal', 'close', 'shower', 'jackpot', 'glow']:
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
    sj[name] = {'sounds': [{'name': 'stardrops:' + name, 'stream': len(x) / SR > 2.5}]}
json.dump(sj, open(os.path.join(RES, 'sounds.json'), 'w'), indent=2)
json.dump(ej, open(os.path.join(RES, 'envelopes.json'), 'w'), separators=(',', ':'))
print('wrote', len(OUT), 'sounds:', ' '.join(f'{k}({len(v)/SR:.1f}s)' for k, v in OUT.items()))
