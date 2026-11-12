# Handoff: what is still open (13 Nov, from the author's list)

Read DESIGN.md first (CLAUDE.md). Build: `bash /tmp/claude-0/cp/build.sh` must print `== 0 errors`. Commits as tra, never push without a yes.

## Bugs still to fix
1. **Animations lag / FPS drops.** Star3D rebuilds everything every frame and submits 160-190k vertices (VoxTime on the bench: 6-7 ms a frame just to build). Plan, in order of gain:
   - Near ground is ~120k of those: in `Star3D.buildGround` use 8 texels a block near and 4 far (`px = de < 9.5f ? 8 : 4`), about a quarter of the vertices.
   - `nearGround` projects every vertex twice (`edgeR`, then `edge` inside `Mesh.v`): compute once.
   - `Vox3D.onSky` inverts the view matrix on every call (hundreds a frame): cache the inverse per frame.
   - Scenery boxes (spawn props) are rebuilt each frame: cache per boss like the ground.
   - CosmosHud's RNG shower (`burn`) queues ~150 GUI elements per meteor, several at once, exactly while the scene plays: cut the core line to ~12 squares and the smoke/flame counts by half.
   - SkyBox submits ~5,000 quads every frame at night: fewer stars (1,000) and 6-segment halos.
2. **Cloudflare "KV daily operation limit exceeded".** KV free tier is 1,000 writes a day; `/event` writes the profile on every drop batch, so two or three active players use it up. Plan: store profiles in D1 (free tier 100,000 writes a day) as a key/value table (`CREATE TABLE IF NOT EXISTS kv (k TEXT PRIMARY KEY, v TEXT)`), reading KV as a fallback so nobody loses progress, and keep working if no D1 is bound. The author must add a D1 binding and redeploy `tools/worn-worker.js`.
3. **"Waiting for the server" on some achievements.** Same root: the server's write fails over the limit (or the deployed worker is older than the mod's table). Fixed by 2 plus a redeploy; check `LocalLedger.WAITING` clears after a good `/event`.

## Redo still to do
4. **First Day achievements repeat daily.** Server (`worn-worker.js`): keep `p.fdAt`; on a drop, if `!p.fdAt || now - p.fdAt >= 86_400_000` then `fdAt = now, fd = 0, fdr = 0`; `stat("fd"/"fdr")` returns 0 once the window is over. Client (`LocalLedger`): same window in `P`, `firstDay()` becomes "the run is live", `firstDayLeft()` counts 24 h from `fdAt`; reword the seven descriptions in `tools/achievements_src.py` ("in one day"; run `tools/make_achievements.py`), Codex line ~970 and Ledger line 29.
5. **Remake the Visuals menu** once the author approves the mockup sent on 13 Nov.

## Done on 13 Nov (see DESIGN.md's last section)
Search no longer overlapped by the "Spotify Linked" chip; the speed gauge reads tick movement (no jitter); instruments renamed for what they show.
