#!/usr/bin/env python3
"""Stardrops: the Slayer black-hole sound set (sl_*). Its own style, made for that one animation: deep sub-bass and gravity
(pitch dragging down, Doppler warble), digital tearing for the boss being pulled apart pixel by pixel, and glassy, wide reveals.
Writes sounds/sl_*.ogg and merges their entries into sounds.json and envelopes.json (other sounds are left alone).
Needs numpy, scipy and ffmpeg. Run from the repo root: python3 tools/make_slayer_sounds.py"""
import os, json, subprocess, tempfile, wave
src = open(os.path.join(os.path.dirname(os.path.abspath(__file__)), 'make_scene_sounds.py')).read()
exec(src[:src.index('# ---- 1.0b')])
__file__ = os.path.abspath(__file__)

def crush(x, bits=5, hold=6):                       # bit-crush and sample-hold: the "pixels tearing" texture
    y = np.repeat(x[::hold], hold)[:len(x)]; q = 2 ** bits; return np.round(y * q) / q
def stutter(x, seg=.03, rep=3):
    n = int(seg * SR); out = []
    for i in range(0, len(x), n * rep): out.append(np.tile(x[i:i + n], rep)[:n * rep])
    return np.concatenate(out)[:len(x)]
def sub(f0, f1, d, k=2.5): t = T(d); return osc(f0, d, glide=f1) * dec(t, k) * np.minimum(1, t / .01)
def warble(f, d, depth=.06, rate=7): return osc(f, d, 'sin', vib=depth, vibf=rate)
def rev(x): return x[::-1].copy()
def tail(x, d=.25, g=.4): return reverb(x, d, g)

OUT = {}
def S(name, x): OUT[name] = x

# the hole opens: a reversed swell sucked into a deep bend-down "whomm"
def sl_open():
    b = mix(1.8); sw = rev(lp(noise(.7), 2500) * dec(T(.7), 4)) * .45; at(b, sw, 0)
    at(b, sub(110, 32, 1.2, 2.2) * 1.0, .68); at(b, warble(220, 1.0, .04, 5) * env(T(1.0), .02, .8) * .18, .7)
    at(b, lp(osc(55, 1.1, 'saw', glide=30), 300) * dec(T(1.1), 2.5) * .35, .7); return tail(b, .3, .3)
S('sl_open', sl_open())
# a gulp: a deep drop with digital tearing on top (the boss coming apart)
def sl_gulp():
    b = mix(.9); at(b, sub(160, 45, .55, 5) * .9, 0)
    tear = stutter(crush(bp(noise(.35), 600, 5000), 4, 9), .025, 2) * dec(T(.35), 7) * .55; at(b, tear, .02)
    at(b, crush(osc(900, .25, 'sq', glide=120), 3, 12) * dec(T(.25), 10) * .25, .04); return tail(b, .2, .22)
S('sl_gulp', sl_gulp())
# no drop: everything rushes in (rising, tightening), a hard cut, then a tiny "tik" as the point winks out
def sl_collapse():
    b = mix(2.0); d = 1.15; t = T(d)
    rush = sweep_noise(d, 300, 6000, .25) * (t / d) ** 2.2 * .6; at(b, rush, 0)
    at(b, osc(80, d, 'saw', glide=900) * (t / d) ** 2 * .18, 0)
    at(b, osc(3200, .05) * dec(T(.05), 60) * .5, 1.17); at(b, osc(1600, .3) * dec(T(.3), 14) * .12, 1.2); return tail(b, .35, .3)
S('sl_collapse', sl_collapse())
# a drop: the hole spits it out: a sharp pitched "thwup", the jets hissing out both ways
def sl_spit():
    b = mix(1.6); at(b, osc(180, .22, 'tri', glide=1400) * dec(T(.22), 6) * .6, 0)
    at(b, hp(noise(.9), 2500) * env(T(.9), .01, .7) * .3, .03); at(b, sub(90, 50, .5, 6) * .7, 0)
    at(b, fm(N(84), .8, ratio=1.5, index=1.5, k=4) * .2, .08); return tail(b, .3, .3)
S('sl_spit', sl_spit())
# RNG: the collapse, then the supernova: sub boom, a blast of air, a distorted bright ring, the long settle
def sl_nova():
    b = mix(4.0)
    at(b, rev(sub(60, 200, .5, 3)) * .6, 0)                                                    # the in-breath
    at(b, impact(55, 18, 2.8, 1.0, 1.6) * 1.1, .45)
    at(b, lp(noise(2.4), 1400) * dec(T(2.4), 1.6) * .55, .45)
    at(b, np.tanh(3 * fm(N(57), 2.4, ratio=1.41, index=4, k=1.4)) * .22, .47)
    at(b, sweep_noise(2.0, 6000, 400, .3) * env(T(2.0), .05, 1.6) * .25, .5); return tail(b, .4, .35)
S('sl_nova', sl_nova())
# the drop's chime: glassy, a little detuned, three notes falling into place
def sl_chime():
    b = mix(2.0)
    for i, m in enumerate([81, 88, 93]):
        at(b, (fm(N(m), 1.4, ratio=2.0, index=.7, k=2.6) + fm(N(m) * 1.004, 1.4, ratio=2.0, index=.7, k=2.6)) * (.3 - .05 * i), i * .09)
    return tail(b, .4, .4)
S('sl_chime', sl_chime())
# RNG reveal: a wide, slow chord swelling up out of the blast, with a bright glass bell on top
def sl_reveal():
    b = mix(3.6); at(b, supersaw([50, 57, 62, 69, 74], 3.2, det=.018, cut=2600, a=.6) * .5, 0)
    at(b, pad([74, 81, 86], 3.0, 'tri', .005, .5) * .35, .1)
    for i, m in enumerate([86, 93, 98]): at(b, glock(N(m), 1.6) * (.25 - .05 * i), .35 + i * .14)
    return tail(b, .45, .4)
S('sl_reveal', sl_reveal())
# after a drop: a low glow, breathing
S('sl_glow', tail(drone_ := (lambda d: lp(sum(osc(N(43) * m * (1 + dd), d) * a for m, a in ((1, 1), (2, .3), (3, .1)) for dd in (-.003, .003)), 900) * env(T(d), .6, 1.2) * (1 + .3 * np.sin(2 * np.pi * .8 * T(d))) * .3)(2.4), .3, .3))
# RNG afterglow: the pulsar, ticking fast under a hum
def sl_pulsar():
    d = 3.0; b = mix(d); t = T(d)
    hum = lp(osc(N(38), d, 'saw'), 400) * env(t, .4, 1.2) * .25; at(b, hum, 0)
    for k in range(int(d * 9)):
        tk = k / 9; g0 = .35 * min(1, tk / .4) * max(0, 1 - tk / d); at(b, osc(1200, .03) * dec(T(.03), 120) * g0, tk); at(b, osc(240, .05) * dec(T(.05), 60) * .6 * g0, tk)
    return tail(b, .3, .3)
S('sl_pulsar', sl_pulsar())
# it all closes: a soft reversed sigh into silence
def sl_close():
    b = mix(1.4); at(b, rev(fm(N(76), 1.0, ratio=2.0, index=.6, k=3)) * .25, 0); at(b, sweep_noise(.9, 2400, 300, .3) * env(T(.9), .3, .5) * .25, .2); return tail(b, .35, .3)
S('sl_close', sl_close())

# ------------------------------------------------------------------ write: only these, merged into the json files
def norm(x, peak=.8): return x / max(1e-6, np.abs(x).max()) * peak
sj = json.load(open(os.path.join(RES, 'sounds.json'))); ej = json.load(open(os.path.join(RES, 'envelopes.json')))
for name, x in OUT.items():
    x = norm(np.nan_to_num(x)); x[-300:] *= np.linspace(1, 0, 300)
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
    sj[name] = {'sounds': [{'name': 'fancydrops:' + name, 'stream': False}]}
json.dump(sj, open(os.path.join(RES, 'sounds.json'), 'w'), indent=2)
json.dump(ej, open(os.path.join(RES, 'envelopes.json'), 'w'), separators=(',', ':'))
print('wrote', len(OUT), 'sounds:', ' '.join(OUT))
