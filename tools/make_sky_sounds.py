#!/usr/bin/env python3
"""Stardrops: the achievement sounds. sky_open / sky_close (the sky), sky_zoom (flying to a constellation), sky_star (pointing at a star),
sky_lit (a new star lit: the achievement chime). Merges into sounds.json and envelopes.json. Run from the repo root."""
import os, json, subprocess, tempfile, wave
src = open(os.path.join(os.path.dirname(os.path.abspath(__file__)), 'make_scene_sounds.py')).read()
exec(src[:src.index('# ---- 1.0b')]); exec(src[src.index('def chip'):src.index('def std_set')])
__file__ = os.path.abspath(__file__)


def rev(x): return x[::-1].copy()
# ---- the achievements sky (Codex) and a star being lit
def sky_open():      # a slow airy swell, glass bells rising through D major pentatonic, a shimmer that hangs
    b = mix(2.6); at(b, pad([50, 57, 62, 69], 2.4, 'sin', .004, .9, 1.2) * .3, 0)
    for i, m in enumerate([74, 76, 78, 81, 83, 86, 88]): at(b, (fm(N(m), 1.6, ratio=2.0, index=.6, k=3.0) + fm(N(m) * 1.003, 1.6, ratio=2.0, index=.6, k=3.0)) * (.2 - .015 * i), .15 + i * .1)
    at(b, sparkle(1.8, 16, 3000, 8000, .12), .5); at(b, hp(sweep_noise(1.2, 400, 6000, .3), 300) * env(T(1.2), .5, .6) * .12, 0); return b
def sky_close():     # the sky folds away: a reversed shimmer into two soft falling bells
    b = mix(1.6); at(b, rev(sparkle(.6, 8, 3000, 7000, .14)), 0); at(b, sweep_noise(.8, 5000, 300, .3) * env(T(.8), .05, .6) * .14, .2)
    for i, m in enumerate([81, 74]): at(b, fm(N(m), 1.0, ratio=2.0, index=.5, k=3.5) * .25, .55 + i * .16)
    return b
def sky_zoom():      # flying to a constellation: an airy whoosh with a gliding chime riding it
    b = mix(1.4); t = T(1.1); at(b, sweep_noise(1.1, 300, 4500, .35) * np.sin(np.pi * t / 1.1) ** 1.5 * .35, 0)
    tt = T(.9); f = N(76) * (1 + .5 * tt / .9); ph = 2 * np.pi * np.cumsum(f) / SR; at(b, np.sin(ph) * dec(tt, 3) * env(tt, .2, .5) * .12, .2); return b
def sky_star():      # pointing at a star: one crystal ping, a detuned twin, a tiny sparkle
    b = mix(1.0); at(b, (fm(N(88), .9, ratio=3.0, index=.8, k=5) + fm(N(88) * 1.006, .9, ratio=3.0, index=.8, k=5)) * .3, 0)
    at(b, sparkle(.4, 4, 5000, 9000, .1), .03); return b
def sky_lit():       # a star lit: a soft low bloom, a bright arpeggio on glass and celesta, a choir swell, sparkles drifting down
    b = mix(3.2); at(b, impact(80, 40, .8, .2, .8) * .25, 0); at(b, lp(noise(.4), 800) * dec(T(.4), 7) * .15, 0)
    for i, m in enumerate([74, 78, 81, 86]): at(b, glock(N(m), 1.6) * (.3 - .03 * i), .06 + i * .09); at(b, celesta(N(m + 12), 1.2) * .1, .1 + i * .09)
    at(b, choir([62, 66, 69, 74], 2.6, .5) * .25, .25); at(b, pad([50, 57, 62], 2.6, 'sin', .004, .4, 1.4) * .2, .2)
    at(b, sparkle(2.0, 20, 3500, 9000, .13), .4); return b

OUT = {'sky_open': reverb(sky_open(), .4, .35), 'sky_close': reverb(sky_close(), .35, .3), 'sky_zoom': reverb(sky_zoom(), .3, .25),
       'sky_star': reverb(sky_star(), .35, .3), 'sky_lit': reverb(sky_lit(), .45, .35)}

def norm(x, peak=.85): return x / max(1e-6, np.abs(x).max()) * peak
sj = json.load(open(os.path.join(RES, 'sounds.json'))); ej = json.load(open(os.path.join(RES, 'envelopes.json')))
for k in [k for k in list(sj) if k in OUT]:                      # the old soundtracks and per-beat set go
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
