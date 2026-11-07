"""Writes the achievement table (tools/achievements_src.py) into the mod (src/main/resources/fancydrops/achievements.json) and into
the server (between the ACH markers in tools/worn-worker.js), so both judge the same stars the same way."""
import json, os, re, sys
here = os.path.dirname(os.path.abspath(__file__)); sys.path.insert(0, here)
import achievements_src as S
rows = [dict(id=a[0], ch=a[1], tier=a[2], name=a[3], desc=a[4], icon=a[5], goal=a[6], stat=a[7], reward=a[8], secret=a[9], first=a[10]) for a in S.A]
ids = [r["id"] for r in rows]; assert len(ids) == len(set(ids)), "duplicate id"
out = {"chapters": dict(S.CH), "list": rows}
json.dump(out, open(os.path.join(here, "..", "src/main/resources/fancydrops/achievements.json"), "w"), indent=1)
w = os.path.join(here, "worn-worker.js"); s = open(w).read()
block = "// ACH-START (written by tools/make_achievements.py)\nconst TABLE = " + json.dumps(out) + ";\n// ACH-END"
s = re.sub(r"// ACH-START.*?// ACH-END", lambda m: block, s, flags=re.S)
open(w, "w").write(s)
print(len(rows), "achievements")
