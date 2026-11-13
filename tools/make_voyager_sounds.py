#!/usr/bin/env python3
"""Stardrops: VOYAGER, the Mob Drops sound pack (13 Nov). One set for every Mob Drops scene (the Slayers and every Location), the
fourth take "d" of each Slayer moment, made to sound like space and exploration instead of war: sonar pings in a huge empty room,
telemetry chirps, thrusters, warp whooshes, a lydian choir of synth pads and crystal bells (lydian: the "wonder" mode), long
ping-pong echoes.

Same moments and the same cue times as tools/make_slayer_sounds.py (SoundFx.SL_SLOTS, AnimationManager.cueTimes):
  rise (0.45 s)  approach (1.45 s)
  No Drops: strike (lands +0.45), dust, none
  a drop:   barrage (lands +.35 +.52 +.68 +.82 +.94 +1.04, big +1.26), reveal, close
  RNG:      shower (six land +.35 +.52 +.67 +.80 +.91 +1.00, a hush from +1.04, the gold strike +1.48), jackpot, glow
Writes sounds/slr_<moment>_d.ogg and merges sounds.json / envelopes.json. Run from the repo root: python3 tools/make_voyager_sounds.py
"""
import os, sys, json, subprocess, tempfile, wave
import numpy as np
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from make_slayer_sounds import (SR, RES, T, N, noise, lp, hp, bp, env, dec, sat, glide, saw, bank, sweep_lp, place, stereo, make_ir, hall,
                                sub_drop, glock, bell, cymbal, rev_cymbal, shimmer, choir, master)

rng = np.random.default_rng(1977)                                                   # (Voyager's launch year)
SPACE = make_ir(4.2, 3800, .05)                                                       # a vast, dark room: space as it sounds in films
D3 = 50; D4 = 62; D5 = 74
LYD = [0, 2, 4, 6, 7, 9, 11]                                                          # D lydian
def lyd(deg, base=D4): o, k = divmod(deg, 7); return base + 12 * o + LYD[k]

# ------------------------------------------------------------------ the instruments of the pack
def ping(m, d=2.4, bright=1.0):
    """A sonar ping: a pure tone with a tiny pitch blip at the start, a long soft decay."""
    t = T(d); f = N(m); ph = 2 * np.pi * f * t + 0.6 * np.exp(-t * 40) * np.sin(2 * np.pi * f * 2 * t)
    x = np.sin(ph) * np.exp(-t * 1.8) + .18 * bright * np.sin(2 * ph) * np.exp(-t * 5)
    return x * np.minimum(1, t / .003) * .6
def echo(x, delay=.32, fb=.45, n=6, pan=.6):
    """Ping-pong echoes, each a little darker than the one before."""
    L = len(x) + int(delay * SR * (n + 1)); out = np.zeros((L, 2)); y = x.copy(); out[:len(x), 0] += x * .7; out[:len(x), 1] += x * .7
    for k in range(1, n + 1):
        y = lp(y, 5200 - 600 * k) * fb; i = int(delay * k * SR); side = k % 2
        out[i:i + len(y), side] += y * (1 + pan) / 2 * 1.4; out[i:i + len(y), 1 - side] += y * (1 - pan) / 2 * .6
    return out
def chirp(m, d=.07):
    """A telemetry blip: a soft square through a band filter."""
    t = T(d); x = np.sign(np.sin(2 * np.pi * N(m) * t)) * .5 + np.sin(2 * np.pi * N(m) * t) * .5
    return bp(x, N(m) * .7, N(m) * 3) * env(d, .004, .03) * .5
def pad(ms, d, a=.6, bright=2600, r=1.2):
    """A wide synth pad: detuned saws, gently filtered, breathing."""
    t = T(d); x = np.zeros(len(t))
    for m in ms:
        for k in range(5): x += saw(2 * np.pi * N(m) * (1 + (k - 2) * .0045) * t + k * 1.7)
    x = lp(x / (5 * len(ms)), bright, 4) * (0.85 + .15 * np.sin(2 * np.pi * .35 * t))
    return x * np.minimum(1, t / a) * np.minimum(1, (d - t) / r)
def drone(m, d, a=.8):
    """The deep space hum: two low sines beating slowly, a thin shimmer of noise on top."""
    t = T(d); x = np.sin(2 * np.pi * N(m) * t) + .7 * np.sin(2 * np.pi * N(m) * 1.003 * t) + .3 * np.sin(2 * np.pi * N(m + 12) * t + np.sin(2 * np.pi * .2 * t))
    x += .08 * bp(noise(d), 1800, 5000) * (0.5 + .5 * np.sin(2 * np.pi * .5 * t))
    return lp(x / 2.2, 900) * env(d, a, d * .4)
def thruster(d, a=.3):
    """An engine lighting: a low roar that swells, crackle on it."""
    t = T(d); x = lp(noise(d), 260) * 3 + .4 * bp(noise(d), 600, 2400) * (np.abs(lp(noise(d), 30)) * 7).clip(0, 1)
    return sat(x * env(d, a, d * .5), 1.4) * .6
def warp(d=.6, f0=200, f1=4200, up=True):
    """A warp whoosh with a phaser sweep and a rising (or falling) tone inside it."""
    t = T(d); n = bank(noise(d), lambda y, f: bp(y, f * .7, f * 1.5), f0 if up else f1, f1 if up else f0, 1.4) * 1.8
    tone = np.sin(glide(N(55) if up else N(91), N(91) if up else N(55), d, 1.6)) * .25
    e = np.sin(np.pi * t / d) ** 1.5 if up else (t / d) ** 2.2
    return (n + tone) * e
def zip_(d=.32, m=86):
    """A star-fire streaking down: a falling whistle with a burst of noise, landing with a soft ping."""
    t = T(d); w = np.sin(glide(N(m + 10), N(m - 14), d, .6)) * (t / d) ** 1.4 * .35 + bp(noise(d), 2500, 9000) * (t / d) ** 3 * .4
    return w
def thud(f=44, d=1.4):
    """A soft, far-away impact: deep and round, no crack (things land on alien ground, they do not explode)."""
    return sat(np.sin(glide(f * 2.6, f, d, .15)) * dec(d, 3.2) * np.minimum(1, T(d) / .003) + lp(noise(d), 380) * dec(d, 11) * .5, 1.3)
def static(d):
    """Radio static breathing in and out."""
    t = T(d); x = bp(noise(d), 1200, 6500) * (0.3 + .7 * np.abs(lp(noise(d), 6)) * 6).clip(0, 1)
    return x * env(d, .05, d * .6) * .35
def theremin(ms, d, step):
    """A wavering glide from note to note: the voice of the unknown."""
    seq = np.concatenate([np.full(int(step * SR), N(m)) for m in ms]); seq = np.pad(seq, (0, max(0, int(d * SR) - len(seq))), 'edge')[:int(d * SR)]
    seq = lp(seq, 9) * (1 + .006 * np.sin(2 * np.pi * 5.5 * T(d)))
    ph = 2 * np.pi * np.cumsum(seq) / SR
    return (np.sin(ph) + .15 * np.sin(2 * ph)) * env(d, .08, .6) * .45
def to_st(x): return np.stack([x, x], 1) if x.ndim == 1 else x
def put(b, x, at, g=1.0, pan=0.0):
    if x.ndim == 2: i = int(at * SR); n = min(len(x), len(b) - i); b[i:i + n] += x[:n] * g
    else: place(b, x, at, g, pan)

# ------------------------------------------------------------------ the moments (take "d")
def rise_d():       # Launch: an engine lights, a sonar ping finds you across the dark
    b = stereo(4.2)
    put(b, thruster(1.4, .35) * .9, 0); put(b, drone(D3 - 12, 3.8, .3) * .7, 0)
    put(b, echo(ping(lyd(4, D5), 2.0), .29, .5, 6) * .55, .42)
    put(b, warp(.7, 160, 2600, True) * .45, .05, pan=-.3)
    return hall(b, .45, SPACE)
def approach_d():   # Signal acquired: telemetry chirps quicken, a pad rises, the sonar pings faster
    b = stereo(3.0)
    put(b, pad([lyd(0, D3), lyd(4, D3), lyd(3, D4)], 2.4, 1.6, 1800, .6) * .55, 0)
    times = [0, .32, .58, .8, .98, 1.13, 1.25, 1.35]
    for i, at in enumerate(times): put(b, chirp(lyd(6 + i % 4, D5)) * .5, at, pan=(-.6 if i % 2 else .6))
    for i, at in enumerate([.1, .7, 1.1, 1.35]): put(b, ping(lyd(i % 3 + 4, D4), 1.0) * .4, at, pan=(.3 - i * .2))
    put(b, drone(D3 - 12, 2.6, .2) * .6, 0); put(b, warp(1.1, 300, 3600, True) * .3, .4)
    return hall(b, .4, SPACE)
def strike_d():     # Re-entry: something tears down through the atmosphere and lands, far away
    b = stereo(3.6)
    put(b, warp(.48, 4200, 220, False) * .9, -.03 if False else 0); put(b, thud(42, 2.2) * 1.0, .45); put(b, sub_drop(1.8, 80, 26, 2.2) * .6, .45)
    put(b, echo(ping(lyd(0, D4), 1.6), .3, .4, 4) * .35, .5)
    for i in range(10): put(b, ping(lyd(rng.integers(7, 14), D4), .4) * .08, .6 + i * .09, pan=rng.uniform(-.8, .8))   # debris settling like little bells
    return hall(b, .5, SPACE)
def dust_d():       # Static: the signal breaks up into radio hiss and one distant beep
    b = stereo(4.0)
    put(b, static(3.2) * .8, 0); put(b, drone(D3 - 12, 3.4, .1) * .4, 0)
    put(b, echo(chirp(lyd(4, D5), .12), .4, .5, 5) * .5, 1.1)
    return hall(b, .5, SPACE)
def none_d():       # Silence of space: two lonely lydian tones, answered by their echoes
    b = stereo(4.8)
    put(b, echo(ping(lyd(0, D4), 2.6, .5), .42, .55, 6) * .55, 0); put(b, echo(ping(lyd(3, D4), 2.6, .5), .42, .55, 6) * .45, .6)
    put(b, pad([lyd(0, D3), lyd(4, D3)], 3.6, 1.2, 900, 1.8) * .3, .2)
    return hall(b, .55, SPACE)
def barrage_d():    # Meteor field: six star-fires streak in and land as bright pings, then the big find lands
    b = stereo(4.6)
    lands = [.35, .52, .68, .82, .94, 1.04]
    for i, at in enumerate(lands):
        z = zip_(.3, 84 + i); put(b, z * .55, at - .3, pan=(-.7 + i * .28))
        put(b, echo(ping(lyd(i + 2, D5), 1.2), .21, .35, 3) * .45, at)
        put(b, thud(55 + i * 3, .5) * .25, at)
    put(b, warp(.5, 3000, 300, False) * .5, .78)
    put(b, thud(40, 2.2) * .95, 1.26); put(b, sub_drop(2.0, 90, 28, 1.8) * .7, 1.26)
    put(b, bell(lyd(0, D4), 2.6) * .4, 1.26, pan=-.3); put(b, bell(lyd(4, D4), 2.6) * .35, 1.28, pan=.3); put(b, bell(lyd(3, D5), 2.4) * .3, 1.3)
    put(b, pad([lyd(0, D3), lyd(4, D3), lyd(3, D4), lyd(6, D4)], 2.8, .05, 2400, 1.6) * .45, 1.26)
    return hall(b, .45, SPACE)
def reveal_d():     # Discovery: a lydian chord blooms, crystal bells climb, a voice-like glide sings over it
    b = stereo(5.4); ch = [lyd(0, D3), lyd(4, D3), lyd(3, D4), lyd(6, D4), lyd(2, D5)]
    put(b, pad(ch, 4.4, .25, 3000, 2.0) * .55, 0); put(b, choir([lyd(0, D4), lyd(4, D4)], 4.0, .5, 'o') * .3, .1)
    for i in range(10): put(b, glock(lyd(i, D5), 1.4) * .4, .05 + i * .11, pan=(-.6 + i * .13))
    put(b, theremin([lyd(4, D5), lyd(3, D5), lyd(6, D5), lyd(7, D5)], 3.2, .55) * .35, .6)
    put(b, drone(D3 - 12, 4.6, .3) * .5, 0); put(b, shimmer(3.6, 14, 86, 110) * .4, .3)
    return hall(b, .5, SPACE)
def close_d():      # Drift: the pad lets go, one last ping drifts off into the dark
    b = stereo(4.4)
    put(b, pad([lyd(0, D3), lyd(4, D3), lyd(3, D4)], 3.4, .4, 1200, 2.4) * .4, 0)
    put(b, echo(ping(lyd(4, D5), 2.2, .6), .38, .55, 7) * .4, .3)
    return hall(b, .55, SPACE)
def shower_d():     # Star rain: six star-fires climbing in pitch, a breath of silence, then the golden strike
    b = stereo(6.6)
    lands = [.35, .52, .67, .80, .91, 1.00]
    for i, at in enumerate(lands):
        put(b, zip_(.3, 86 + i * 2) * .6, at - .3, pan=(.7 - i * .28))
        put(b, echo(ping(lyd(i + 4, D5), 1.0), .17, .35, 3) * .5, at); put(b, thud(60 + i * 4, .4) * .25, at)
    put(b, rev_cymbal(.44) * .45, 1.04); put(b, warp(.44, 200, 5200, True) * .5, 1.04)                     # the hush: everything breathes in
    at = 1.48
    put(b, thud(36, 2.6) * 1.0, at); put(b, sub_drop(2.6, 100, 25, 1.4) * .85, at); put(b, cymbal(3.4, .003, 1.1) * .3, at)
    ch = [lyd(0, D3), lyd(4, D3), lyd(0, D4), lyd(3, D4), lyd(6, D4), lyd(2, D5)]
    put(b, pad(ch, 4.6, .03, 4200, 2.4) * .6, at); put(b, choir([lyd(0, D4), lyd(4, D4), lyd(3, D5)], 4.4, .08, 'a', 1.2) * .45, at)
    for i in range(14): put(b, glock(lyd(i + 3, D5), 1.0) * .35, at + .02 + i * .06, pan=np.sin(i * .9) * .8)
    put(b, shimmer(4.0, 26, 88, 112) * .5, at + .1)
    return hall(b, .5, SPACE)
def jackpot_d():    # First light: the whole sky opens, a lydian fanfare of bells over choir and pads, the sun of a new world rising
    b = stereo(8.2)
    prog = [(0, [lyd(0, D3), lyd(4, D3), lyd(0, D4), lyd(3, D4), lyd(6, D4)], D3 - 24, 2.0), (2.0, [lyd(1, D3), lyd(5, D3), lyd(1, D4), lyd(4, D4)], D3 - 22, 1.6),
            (3.6, [lyd(0, D3), lyd(4, D3), lyd(0, D4), lyd(2, D4), lyd(4, D4), lyd(6, D4), lyd(2, D5)], D3 - 24, 4.2)]
    for at, ch, low, d in prog:
        put(b, pad(ch, d + .4, .3, 3600, 1.4) * .5, at); put(b, choir(ch[:3], d + .4, .5, 'o', 1.1) * .35, at)
        put(b, drone(low + 12, d + .4, .4) * .6, at); put(b, bell(ch[0] + 24, 2.8) * .35, at, pan=-.4); put(b, bell(ch[-1] + 12, 2.6) * .3, at + .07, pan=.4)
    fan = [lyd(4, D4), lyd(0, D5), lyd(2, D5), lyd(4, D5), lyd(3, D5), lyd(6, D5), lyd(7, D5)]
    for i, m in enumerate(fan): put(b, glock(m, 1.6) * .45, .2 + i * .2, pan=(-.5 + i * .16)); put(b, glock(m + 12, 1.2) * .2, .2 + i * .2 + .1)
    put(b, theremin([lyd(4, D5), lyd(6, D5), lyd(7, D5), lyd(9, D5)], 3.8, .9) * .35, 3.6)
    put(b, thud(38, 2.4) * .8, 0); put(b, thud(36, 2.6) * .9, 3.6); put(b, shimmer(4.5, 24, 90, 114) * .5, 3.7); put(b, cymbal(3.6, .003, 1.0) * .25, 3.6)
    return hall(b, .55, SPACE)
def glow_d():       # Nebula: a slow shimmering cloud of sound, beating softly, sparks drifting through it
    b = stereo(7.0); t = T(6.2)
    x = sum(np.sin(2 * np.pi * N(m) * t * (1 + .0012 * k) + np.sin(2 * np.pi * .13 * (k + 1) * t)) for k, m in enumerate([lyd(0, D4), lyd(4, D4), lyd(3, D5), lyd(6, D5), lyd(2, 86)])) / 5
    put(b, x * (.7 + .3 * np.sin(2 * np.pi * .7 * t)) * env(6.2, 1.2, 3.2) * .5, 0)
    put(b, drone(D3 - 12, 6.2, 1.0) * .45, 0); put(b, shimmer(5.4, 8, 92, 112) * .35, .5)
    put(b, echo(ping(lyd(4, D5), 2.0, .5), .45, .5, 6) * .25, 1.2)
    return hall(b, .55, SPACE)

FAMS = ['rise', 'approach', 'strike', 'dust', 'none', 'barrage', 'reveal', 'close', 'shower', 'jackpot', 'glow']
PEAK = {'rise': .8, 'approach': .78, 'strike': .9, 'dust': .5, 'none': .55, 'barrage': .92, 'reveal': .85, 'close': .42, 'shower': .95, 'jackpot': .9, 'glow': .5}
if __name__ == '__main__':
    only = sys.argv[1:]
    sj = json.load(open(os.path.join(RES, 'sounds.json'))); ej = json.load(open(os.path.join(RES, 'envelopes.json'))); done = []
    for fam in FAMS:
        name = f'slr_{fam}_d'
        if only and not any(o in name for o in only): continue
        x = master(globals()[f'{fam}_d'](), PEAK[fam])
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
