# Stardrops design requirements

Standing rules from the author (ImTra). Read this before changing the menu, animations or drop settings, and keep it up to date. When a change is made to one page, change every other page and button it affects (for example, if the Mythological Animations page is copied to Slayers, the Slayers buttons must describe the Slayers scenes).

## Working rules
- Short UI text. Releases are manual (the author runs the "Release Stardrops" workflow with the version).
- Per update: bump `version=` in `gradle.properties`, add a `## x.y.z` section at the top of `CHANGELOG.md`, commit and push to `main`. The version was restarted at 0.4.1 for the full rework (author request): count up from there, 0.4.2, 0.4.3, 0.4.4, 0.4.5, 0.4.6, 0.4.7, 0.4.8, 0.4.9, 0.4.10, 0.4.11 and so on.
- Commit as `tra` (`git -c user.name=tra -c user.email=imtraminecraft@gmail.com commit`).
- Make a mockup first for anything visual and big, and wait for approval before building it.
- Nothing can be compiled or run in the cloud workspace. Say so honestly. Check for duplicate methods and variable names by hand.
- Icons must be hand-pixeled, centred in their 16x16 square, animated, and unique to the thing they show. They must not look AI-made.
- The mod is free, with Patreon supporters getting extra customisation later.

## Drop Library
- The "Show" filter has four buttons: **Enabled, Normal, RNG, Disabled**. Make it obvious these are the three animation states.
  - Enabled: anything with Normal on, RNG on, or Chat Alert on.
  - Normal: only items whose animation state is Normal.
  - RNG: only items whose animation state is RNG.
  - Disabled: items whose animation state is Disabled, or whose Chat Alert is off.
- Each item row has three controls: the **animation state** button (short name: Off / Normal / RNG), the **Chat Alert** toggle, and the **Preview** button.
- Choosing Normal turns Chat Alert OFF. Choosing RNG turns Chat Alert ON. The user can then toggle Chat Alert by hand.

## Animation states
- Disabled, Normal, RNG. A fourth scene (No Drops) exists but the user cannot pick it.
- **Normal** and **No Drops**: a short scene shown above the SkyBlock action bar. The hotbar, hearts and the rest of the HUD stay visible. 3 seconds by default.
- **RNG**: the big scene. Black bars come in and the whole HUD (hotbar and everything) is hidden. 5 seconds by default.
- The user can change each duration in the Animations tab. The maximum is **7 seconds**.
- The chat alert is sent first. The message stays hidden from the player until the animation has fully played.
- If the animation text is at the bottom and the scene position is at the bottom, the text must not be covered by the SkyBlock action bar. Move the scene up a little and draw the text over the scene (so the scene does not cover the middle of the screen).
- When animations change, re-check every text and position setting so none is left broken.

## Animations tab
- The "Changes" buttons (black bars, animation text, item changes) sit at the very top.
- The Animation scenes page stays as it is. Length maximum is 7 seconds.

## Sounds
- The sound system stays. The sound waves are redrawn so they look clean.

## Chat
- The Message format editor is visible without scrolling. Previews live outside the main menu panel.

## Mythological Rituals (first finished category; Slayers, Dye, Farming, Experiments, Mining and Fishing follow)
- Mythological opens a hub with two sub pages:
  - **Mythological Mobs**: the creatures, and the Griffin Pet picker.
  - **Treasure**: the spade picker, the Erudite reforge and the treasure drops with their chances.
- Redo all icons for Mythological, and make sure each one is correct.
- Redo the Mythological scenes. Mobs keep the lightning idea. Treasure shows someone digging with a shovel until the treasure is found. As cool as possible within the rules above.
- Mythological and Bosses (Slayers inside it) are live (since 0.4.37). Dye, Farming, Experiments, Mining and Fishing are WIP: a WIP label in the sidebar and the construction-site page in Drop Library, Animations, Sounds and Chat. Their code stays, it is just hidden until each one is rebuilt.
- Categories that are not finished keep the "Coming Soon" construction-site page (hazard tape, hard hats, crane, bulldozer). The author wants it kept but tidy: fixed layout on one ground line, few props, nothing overlapping, nothing interactive. (A crane-builds-then-explodes scene was an earlier idea; the author later said to keep this page.)

## Animation look (author feedback, Oct 25)
- Scenes must be properly animated, never bland: real motion, particles, light, a distinct idea per category.
- Normal and No Drops scenes are wide banners (not square), sitting right above the SkyBlock action bar with only a small gap. Not floating in the middle of the screen.
- The text keeps the earlier look (name, price, magic find lines); make it cooler where possible.

## Mythological Rituals Normal scenes (author feedback, Oct 25)
- Work on ONE part at a time (Mythological Mobs + Treasure first). Do not mock up other categories until asked.
- Built like the old Farming scene: no box. Ground patch with shadow, glows, strikes, soil glow, column of light, rings, grains of light, item springs out. Small and translucent, not square.
- Items can be 3D (blocks like gold blocks): show those rotating in 3D, flat icons stay flat.
- Mobs: a ritual circle, three lightning strikes, the minotaur is summoned, struck, shatters, item springs out. Treasure: a digger with a shovel digs three times, a chest is found and opens.
- Chat alert (to do when we reach the Chat page): lay the message out like the author's reference video: a centred block, "RARE DROP!" header in gold, the item name in its rarity colour, a small odds/source line, the price in gold, and the price source in dim grey. Every line must be customisable.

## Mythological state (Oct 25, round 2)
- Drop Library > Mythological opens a hub with two tiles: Mythological Mobs and Treasure. The sidebar lists them too.
- Per-item controls (right to left): Chat Alert, Preview, Animation state (Off / Normal / RNG). The Show filter is Enabled / Normal / RNG / Disabled.
- Normal = never the big scene. RNG = the big scene. There is no price-based auto escalation any more.
- Scenes: Normal (small, above the action bar, 3 s, text 1.5 s longer), RNG (big, bars, HUD hidden, 5 s, same story with night sky, god rays, halos, feathers), No Drops (small, one weak strike, a pale minotaur crumbles). Animation length is 2 to 7 s.
- Chat lines: `RARE DROP! You dug out a {Drop}!`, `{Random words} You dug out {amount} coins!` (any amount), `RARE DROP! {drop}` for creatures. A creature's drop plays that creature's own scene (the Minotaur has its pixel sprite, the others use their silhouettes).
- Not triggerable yet: No Drops for a real Diana kill (no known chat line). Preview and `/fd` only.

## Standing requirements added in 0.4.4
- Chat Alert follows the state: Normal off, RNG on. Rare drops default to RNG.
- HUD is hidden only by the RNG scene. Normal and No Drops keep it.
- New pages go straight to the sidebar under their category (Mythological lists Mobs and Treasure first). Mythological is always the first category.
- Choosing a cosmetic set turns off the previous set's suffix, aura and name effect.
- The Chat preview lives in its own panel above the menu, not inside it.
- No Comets style. Remove buttons and pages that do nothing, but keep the WIP pages.

## Standing requirements added in 0.4.5
- Every preview must show the CURRENT look and text of the page it belongs to. When a page is rebuilt, update every preview that reads it (dock, editors, chat, sound waves) in the same change.
- WIP pages never show a preview (animation dock, sound waves). They show the construction page. Sounds works like Animations.
- Mythological has Mobs and Treasure pages in Drop Library, Sounds and Stats. Stats lists the same categories as the Drop Library, Mythological first, with WIP tags.
- Words must stay readable: they enter with the reveal and stay at least ~2 s.
- Text position labels: "Top of animation" / "Bottom of animation".
- No releases on GitHub: the update line says "No Releases found yet."

## Standing requirements added in 0.4.6
- Scene effects (flashes, rays, rings) are drawn over the whole screen, never inside the scene's picture box.
- The words follow the animation's position: top = words above it, middle = words above or below by choice, bottom = as before.
- Mythological size: 50-200%, 100% default.

## Standing requirements added in 0.4.7
- Items are flat 2D in animations; only skull items are 3D. There is no Item Changes setting.
- Placeholders for the chat format are small buttons outside the menu, far left, never shifting the menu.
- The construction page loops: slow build, an accident, collapse, restart.
- The Active lever eases over with no bounce.
- Mythological Treasure has its own Animations and Sounds pages. Spade, Pet and Erudite live on the main Ritual page.

## Standing requirements added in 0.4.8
- Mythological is the baseline for every drop library. Slayers is next, and the existing Slayer boss identification stays untouched (a spawned boss that drops "RARE FIND!" must keep tracking).
- Each creature or source is a sidebar entry under its page.
- Bars and scene share one curve (curveR / blitRounded).
- Heavy effects are drawn in a few bands, not hundreds of fills.

## Standing requirements added in 0.4.9
- Item art comes from NEU (skull items are 3D heads that face front at the end). Every drop id must exist in the NEU repo; coins use the Total profit coin.
- The construction page: Coming Soon sits under the title, the scene is a looping story (build, accident, collapse) plus idle builders.

## Standing requirements added in 0.4.10
- /fd simulate only lists finished pages. Add a kind when its page is rebuilt.
- Skull items always show the 3D head in lists.

## Standing requirements added in 0.4.11
- The item stays up as long as the words. Its spin is sized to that time and ends facing front.

- Chat preview height is measured at the dock's real width. Dice icon is a per-pixel rotating cube: white die, black pips, red 1, grey-blue outline, a shadow under it, hops twice and lands on a 6 (matches the author's reference video). Descriptions never mention a screen position (the player picks it).
- Items turn slowly and steadily like a dropped item and end facing front. The Mythological sound bank (`myth_*`) is the default; `_big` twins play in RNG. Sounds are timed to the whole animation (words included) from one function used by the real scene, previews and the Sounds page. `/fd simulate`, Play buttons and menu use `launchMyth` / `launchTreasure`.
- Cosmetics: the page layout was approved as a mockup (try-on previews follow the hovered tile; headers show a bar that fills once). New suffixes need a 60x10 strip (six 10x10 frames) in textures/font, a provider in font/emoji.json, a Cosmetics.SUFFIXES entry and a CosmeticData.Def. New name effects live in NameFx.color.

- 2D item sprites stay flat in the animation (no turning); only 3D heads/blocks spin. Switching pages never moves the content (no slide or bounce). The Cosmetics page keeps its layout and has a Collection bar on top; the Aura preview is a lit stage with a bobbing character.

## Rewrite brief (0.4.13, author, Oct 26) - supersedes older rules where they differ
- Idea: a mini animation when a player drops something, fully customisable. Mythological first, then Slayers, Dyes, Farming, Experiments, Mining, Fishing.
- Main pages (no scrolling): Home, Drop Library, Animations, Sounds, Chat, Stats (WIP), Settings (bottom left). Keep Cosmetics, the Community buttons (top right) and the Active lever.
- Sub pages: Mythological, Slayers, Farming, Experiments, Mining, Fishing, Dyes. Sub-sub pages: the mobs of each. New ones appear in Drop Library, Animations and Sounds automatically. Unfinished ones are WIP.
- Home: description, update tracker, Resources sub page.
- Drop Library: drops sorted by source; per-item animation and chat alert; icons match NEU; skulls are 3D and animated.
- Animations: main settings (Bar, Text, Item) on top, then sub menus that open the editor. Types: No Drops, Normal, RNG, each with preview, length, sounds, position (Bottom / Center / Top Center) and size. Only RNG has cinematic bars and blur (toggleable). Previews always show the current settings.
- Sounds: main settings on top, editor with live preview below, ONE sound set for now, a clean wave drawing (no overlaying colours).
- Chat: description plus two sub pages ("Announce and Format", with the Test button, and "Chat Enabled"). Placeholders sit beside the Message format field (0.4.15). No Custom Drops.
- Icons are `:name:` only (no `suf_`). New animated icons with depth come later. Sparkle is the only suffix.
- Mythological animation: all three types 5-7 s (user-settable). The first 2.5-4.5 s are the same for all three, then each pivots. No Drops shows no item, text "Nothing :(" / "Better luck next time". Normal shows the item with an effect, no background. RNG has the most unique last 2.5 s, with bars. Order: item appears, background fades while text shows, item fades slowly, text fades 0.5 s later.
- Text placement: animation at Top Center or Center -> text slightly below; Bottom -> text above. RNG with bars: Top/Bottom Center put the text outside the bars; Center follows the normal rule.
- Delivery: rewrite pushed straight to main. The author syncs with `git fetch origin` then `git reset --hard origin/main`.

## Standing requirements added in 0.4.14
- (Reversed in 0.4.15: Treasure is a sub-sub page of Mythological again.) Single-group and single-source pages open their editor or drop list straight away.
- Commands: /fd, /fd help, /fd mute, /fd toggle, /fd simulate Mythological <Mob|Treasure> <Drop 1> <mf>. Nothing else.
- No tip bar on Home.
- Animation timeline lives in Ritual.java (opening = S - 2.5 s, pivot = last 2.5 s). Icon art lives in IconArt.java (16x16, shaded from the top left, outlined, up to 8 frames).
- Icons never copy another company's logo; the community buttons use generic symbols.

## Standing requirements added in 0.4.15
- Mythological is ONE sub page with two sub-sub pages: Mythological Mobs (pet + creatures) and Treasure (spade + Erudite + drops). It reverses the 0.4.14 split. Treasure and Mobs are also the two animation groups of the Animations and Sounds pages.
- The Drop Library has no per-page enable switch. Defaults on first start: every drop Normal, rare drops (rank 2+) RNG; Normal = chat alert OFF, RNG = chat alert ON, set at once when the state button is pressed.
- Animation size: the scene is drawn at 0.75 x the Size slider, so 100% is 25% smaller than in 0.4.14; the slider ends at 120%. After the pivot the scene lasts 3.2 s (Ritual.TAIL).
- Skulls are real 3D blocks that turn smoothly (SkullSpin). Coin icons use the Hypixel coin head.
- Animations and Chat > Message & Channel must not need scrolling. Test is on the Chat landing page; placeholder buttons sit beside the Message format field (not outside the menu).
- Chat Enabled lists each sub page and creature with a switch per drop. "Also post in public chat" lights Public in the channel picker.
- Every on/off is the yellow/grey ON/OFF button.
- Sounds: Play plays only the sounds; the live preview shows a time ruler, one lane per sound with its length, a playhead and the clock.
- Animated menu off: all decoration stands still except icons (icons follow Animated icons).
- No Menu Font setting.

## 0.4.16 additions
- Mythological: one animation shape per creature (Ritual.KINDS / shape()); Animations page keeps a single "Mythological Mobs" group. Treasure uses the digger (Ritual.digger) with a chest for Normal and RNG.
- Creature icons are `mob_<id>` in IconArt.mob(); they appear on the creature tiles and under Mythological Mobs in the sidebar.
- Cosmetics has no search box. Name effects are only the Special sets and Beta (Pulse). Hidden (scrolled-out) tiles are not hoverable.
- Where to edit: icons in IconArt.java, scenes in Ritual.java, timing/cues in AnimationManager.java, item art in ItemIcons.java / SkullSpin.java, defaults in StardropsConfig.java.

## 0.4.17
- Item art lives in config/stardrops/textures/ (<ID>.png flat, <ID>_head.png skin); user files override downloaded art. Creature icons `mob_<id>` draw the creature skull in 3D (ItemIcons MOB_SKINS).

## 0.4.18
- Main (landing) pages must never scroll: build() shrinks tile heights, then hides the intro text, then clips.

## 0.4.19
- Animation 100% = Ritual.SMALLER 0.6 (about a third of the screen width). The inhale pillar is short.

## 0.4.20
- Icons are overridable at runtime from config/stardrops/icons/<name>.png (Icons.scanOverrides, hot reload). Add new names to Icons.EXPORT.

## 0.4.21
- The sidebar squeezes (StardropsScreen.sq, 0.7 to 1) instead of scrolling. Emblems: `emblem` (tinted shield + star) and `emblem_special`.

## 0.4.22
- Icons have 12 frames (IconArt.FRAMES). The sidebar is not squeezed (sq stays 1); main pages never scroll.

## 0.4.25
- "Animation Changes" has Bar, Text, Item and Position Changes. Position rules: Top Center = words below only, Center = above or below, Above Action Bar = words above only (StardropsConfig.textAbove()).
- Chat format limit: 65 letters, placeholders not counted.
- Source art files stay in src/main/java/dev/stardrops (the IDE needs package = folder; see ART.md); runtime art in config/stardrops/art/{icons,heads,items,animations} (DataFiles.art). All heads turn in sync.

## Standing requirements added in 0.4.28
- Page headers show the title only: no line of words under it.
- Item changes sliders (spin, glow, float) and toggles must change what the animation draws; the Item preview uses a 3D head so spin shows. Bar "See-through" 0% is solid. Animation Size is 50-150%.

## Standing requirements added in 0.4.29
- The back button lives in the page header (under the title), never as a row in the list.
- A button inside a search result must work like on its own page: it moves to that page and clears the search.
- Heads are traced off the render thread. Animation frames are real PNGs in art/animations; an edited frame replaces the drawn one.

## Standing requirements added in 0.4.30
- Animation size is one global setting (Animations > Animation Changes), shown 75-125%. Categories have no size.
- Item Changes is a single page. Floating off hides Float height. Bar pillars are as thick as the bars.
- Every sub page has a header back button to its main page; every tab with sub pages has a main page (Settings too).
- Exported icon and animation PNGs never overwrite a file the author edited, but refresh when untouched (original.txt lists what the mod wrote).

## Standing requirements added in 0.4.31
- Animations and Sounds main pages show their description, one row that opens the shared settings (Animation Changes / Sound Settings) and the category tiles. The settings live on their own sub page so the main page never scrolls.

## Standing requirements added in 0.4.34
- The construction page loop (14 s): stand, tremble, collapse with dust, foreman reacts, the crane lifts the pile in three loads and rebuilds, window lights, restart. Nothing floats: every hanging thing hangs from the crane or a lamp. Keep the title and Coming Soon tag as they are.
- Depth layers on the construction page: far skyline and cranes, unfinished buildings with tiny workers, fog, a dark foreground edge. Several lamp posts.

## Standing requirements added in 0.4.35
- The Coming Soon tag on the construction page is a hanging marquee board (chains from the top tape, chasing bulbs, glow, flicker, shine) with two builders on it (hammer on top, welder underneath). No road signs: the author found them ugly. No ladder worker.

## Standing requirements added in 0.4.36
- The construction scene lives in Site.java, a port of the approved mockup in its own 421 x 169 grid (ground at y 150) that the page shifts onto the panel's ground line. When the mockup changes, change Site.java the same way. Builders are 12 px tall. No road signs at the top corners.

## Standing requirements added in 0.4.37
- The main page is **Bosses** (id `bosses`), with one sub page **Slayers** (group `slayer`, types No Drops, Normal, RNG; keys `slayer.nodrops|normal|rng`) and the six bosses as sub sub pages. Future boss types (Rift, Nether...) are new groups of Bosses. Chat has one message for Bosses.
- Slayer data lives in `slayers.json` (same shapes as `mythological.json`, loaded by `SlayerData`). Drop ranks come from the old odds names (Occasional 0, Rare 1, Extraordinary 2, Pray RNGesus / RNGesus Incarnate 3); rank 2 and up default to RNG.
- The slayer scene uses the Mythological ritual engine in `Ritual.java` (`slayerMode`): the same ground circle, boss silhouette (kinds 12..17: zombie, spider, wolf, enderman, blaze, vampire), strike beats, shatter and item pivot, with a sword slashing the boss instead of lightning (`slash`). No backdrop or platform: the screen behind stays visible. The old software-3D SlayerScene, Depth and Tex are gone.
- The slayer sound bank is `slay_*` (slash, cleave, reveal, reveal_epic, glow, end). The sword cues sit on the Mythological strike beats (0.34, 0.58, 0.80 of the opening).
- Every page of Slayers must describe slayer scenes (text on the Animations page says a sword slices the boss).

## Standing requirements added in 0.4.38
- Every animation is 5 seconds by default. The slayer scene is as wide as the Mythological circle (scale 1.9 x size). Its light flashes sit on the sword slices (0.536, 0.753, 0.943 of the opening).
- WIP pages show the page's icon and name on a hanging sign at the top right (the Coming Soon board stays centred). Boss icons are `:boss_<id>:`, drawn by IconArt, never wiki pictures.
- Menu previews repaint the scene at about 30 fps (`AnimationManager.previewing`).

## Standing requirements added in 0.4.39
- Boss icons are skull items (`:boss_<id>:`: zombie head, then the Spider, Wolf, Enderman, Blaze and Bat pet heads), never drawn art. The Bosses page has three sub pages: Slayers (live), The Rift (a mirror dimension of the main server) and Crimson Isle (the Nether of SkyBlock), the last two WIP. Each has its own hand-pixeled animated icon (`grp_slayers`, `grp_rift`, `grp_crimson`). Future boss types are more groups of Bosses.
- A WIP page shows only the page's icon and name on the hanging sign (no "drops" / "animations" suffix).

## Standing requirements added in 0.4.40
- The item in every animation is drawn 1.5x the old size; that is the new 100% of the Item size slider.
- Category tiles show pages, sub pages and items. Unfinished groups carry a WIP tag in the sidebar. Mythological creature tiles take the colour of the pet rarity they first spawn for.
- Menu previews must not issue hundreds of fills per frame: the backdrop is a texture (`BackdropTex`).

## Standing requirements added in 0.4.41
- Text that does not fit a tile pans (marquee: slide, pause, slide back) rather than being cut or shrunk (`StardropsScreen.scrollText`).

## Standing requirements added in 0.4.45
- Slayer bosses use heads the mod draws itself (`@HEAD_ZOMBIE`, `@HEAD_BLAZE`) as 3D spinning icons, never a flat item.
- Slayer scenes never crack the ground; the hologram is slashed, with random cuts per drop that stay until the shatter.
- The Drop Library landing shows a "Tracked Drops" counter (top right of the hero).
- Marquee text on long tiles moves in sync: fixed duration, eased, same wait.

## Standing requirements added in 0.4.46
- The Chat main page has no "Your message" row; the live message preview lives on the sub pages.
- Leaving a search through the side menu always clears the box and drops its focus.

## Standing requirements added in 0.4.47
- The hotbar and status bars are hidden only while the cinematic bars are on screen, never earlier.

## Standing requirements added in 0.4.48
- Every main page (Home, Drop Library, Animations, Sounds, Chat, Settings) opens with the same banner (icon, title, short words) and then its tiles. A main page must fit without scrolling.
- The Tracked Drops counter stays a small one-line chip.

## Standing requirements added in 0.4.49
- The page title never moves when a sub page opens: main and sub pages draw it at the same height.

## Standing requirements added in 0.4.50
- Slayer boss icons use the creature's real texture (zombie, blaze) as a 3D head; the hand-drawn head is only the fallback.
- A slayer scene has exactly three cuts, and every cut opens the hologram.
- Item Changes has no Outline or 3D items buttons; Glints draws sparks around the item.

## Standing requirements added in 0.4.51
- Top Center places the highest point of the animation at the top edge (below the top bar when bars show).
- Text position is locked by animation position: Top = below, Above Action Bar = above, Center = free; the blocked button explains why.
- Soft edge on the bars is a smooth gradient of thin rows, never a few thick steps.

## Standing requirements added in 0.4.52
- A slayer scene plays on an opaque panel, the same size as the Mythological scene; its three cuts split the hologram into pieces.
- A hand-drawn stand-in icon is never saved to the icons folder; only real loaded art is.
- A source has the same icon on every page (Drop Library, Chat Enabled, sidebar).

## Standing requirements added in 0.4.53
- While searching, the left menu lists every page that holds a match (categories and the pages inside them) and highlights it; a click opens that page.


## Standing requirements added in 0.4.54
- A slayer scene has no ritual look (no runes, rings or circle): the boss stands as a hologram in the place it belongs to (SlayerScenery), on an opaque panel with depth.

## Standing requirements added in 0.4.55
- Nothing unused stays: when a feature or icon goes, its code, config field, resources and menu entry go with it.

## Standing requirements added in 0.4.56
- A slayer boss is fully standing before the first cut.
- The slayer panel is translucent, compact and rounded; only scenery in front of it may cross the frame, and never far.

## Standing requirements added in 0.4.57
- Every slayer place is centred on the boss: the focal object behind it, the rest mirrored left and right.

## Standing requirements added in 0.4.58
- A slayer place is clipped to its frame, with at most 4 px of spill above and 2 px at the sides.

## Standing requirements added in 0.4.59
- Only a moon's glow may leave the frame of a slayer place; everything else is clipped to it (4 px above, 2 px at the sides).

## Standing requirements added in 0.5.0
- Farming has three pages (Rare Crops, Harvest Feast, Pest Drops) and Experiments one; each has Normal and RNG animations only, and neither shows the construction sign any more.
- Farming keeps its old drop lists and chat detection; Experiments is one group with Normal and RNG.
- Slayers and Experiments support two items in one animation: side by side, scaled down, with stacked compact text. The type is RNG if either item is RNG.
- /fd simulate reflects two drops for Slayer and Experiments.
- Every new animation has its own sounds in the Sounds tab and its own hand-pixeled, animated icon.

## Standing requirements added in 0.5.1
- A rebuilt data file gets a higher "v" than any file ever shipped, so an old copy in the config folder is replaced; a data file with the wrong ids falls back to the bundled one.

## Standing requirements added in 0.6.0
- Every Farming scene plays in a place of its own (FarmScenery): Rare Crops a golden-hour field, Harvest Feast a harvest night, Pest Drops the inside of a barn, Harvest Bounty a morning field with a tractor. Pests are never in a field.
- A Farming or Experiment drop has no rings, dashed halos, runes or lightning round the item: light, petals, leaves and sparks only.
- Farming has four pages: Rare Crops, Harvest Feast, Harvest Bounty, Pest Drops. Harvest Bounty items live only on the Harvest Bounty page, with no "(Harvest Bounty)" in their odds.
- Experiments is called Rare Drops: Experimentation Table, Rare Mob Drops (WIP) and Events (WIP). Every page of a category appears on all four main pages (Drop Library, Animations, Sounds, Chat).
- The Experimentation Table book turns exactly three pages in both animations; RNG is the same three pages made far more dramatic.
- A new data file version is higher than every version ever shipped (farming.json is 10).

## Standing requirements added in 0.6.1
- Harvest Bounty: one tractor pass, fast; each crop falls exactly as the mower behind the tractor reaches it. No scythe sweeps.
- Pest Drops: nothing on the barn floor but the pest.
- Experimentation Table list: other drops first, then books by level (all V, then all VI, then all VII), A to Z inside a level. Every VII book is RNG.
- Book flip sounds play when a page starts to turn, never before the animation.
- Farming drops use their own wiki page for the picture; `img` is only the fallback. Farming icon caches are version 14 (a refetch also replaces the saved art/items picture).
- Page descriptions stay short and have no "more later" notes.

## Standing requirements added in 0.6.2
- The Harvest Bounty tractor is never visible after its pass.
- A farming icon refetch replaces the picture saved in art/items, so a stale file cannot override the right one.

## Standing requirements added in 0.6.3
- Draw 16x16 pictures with PixTex (one quad); never one fill per pixel run in a list. Anything asked every frame (skullFlag, isBook, unlocked) is remembered.
- Pictures saved on disk load on the FAST pool, downloads on the pool of six.
- Every name effect is ONE main colour plus white. Starlight is pale blue, Critical red, Aqua Flow aqua, Royal gold; Beta keeps the name's own colour. No other colours, no darkening.
- "Suffix" is called "Name Icon" in all visible text (the code keeps the old name).
- Name Icons are grouped Special (Bug Finder, Contributors, Patreon only), Legendary, Rare, Common. The list is shown to the author for approval before any icon is added.

## Standing requirements added in 0.7.0
- Name Icon rarities, rarest first: Special (Bug Finder, Contributors, Patreon ONLY), Legendary, Beta, Rare, Common. Legendary, Rare and Common unlock rules are set by the author after he reviews the icons in game; until then only admins have them.
- A new Name Icon is drawn in tools/make_name_icons.py (add it to ICONS and ORDER), then run tools/make_suffix_sprites.py; add a Cosmetics.SUFFIXES entry (font base 0xE100 + 0x10 per icon) and a CosmeticData.Def.
- Admins are the SHA-256 codes in Cosmetics.ADMINS. They have every cosmetic and the Admin Panel.

## Standing requirements added in 0.7.1
- Name Icons now: Special (Bug, Code, Patreon), Beta (Beta), Legendary (King Minos, Lucky Dice), Rare (Treasure Chest, Anvil, Music Disc), Common (Fishing Rod, Tractor, Bell, Clock). Do not bring back the removed ones (Hammer, Griffin, Gold Coin, Minotaur, Sphinx, Harpy, Shovel, Wheat, Pickaxe, Dye Drop, Book).
- Admin is the owner only. Do not add other admin IDs unless the author asks.
- When an icon is removed, delete its strip, its chat sprite and its font entry too (tools/make_name_icons.py rewrites emoji.json).

## Standing requirements added in 0.7.2
- /fd help lists the main commands and categories; each category has its own page (/fd help simulate). New commands go in the matching category.
- /fd <words> opens the menu with the search filled in. Real commands (help, mute, toggle, simulate) win over a search.
- Experimentation Table list: non-books first, then books by level (IV, V, VI, VII), then by odds, then A to Z.

## Standing requirements added in 0.7.3
- All six slayer boss icons are flat 2D mob pictures in one style (the wiki renders for Tarantula, Sven, Voidgloom; hand-pixeled, outline-free ones for Revenant Horror, Inferno Demonlord, Riftstalker Bloodfiend, in `ItemIcons.bossSprite`). No 3D skulls for slayer bosses.
- The Mythological mobs stay as 3D heads (author decision, Oct 26).

## Standing requirements added in 0.7.4
- The Discord community button opens https://discord.gg/xeBfsVRfkT (`StardropsMod.DISCORD`); the README links it too. The #releases channel is fed by a GitHub release webhook.

## Standing requirements added in 0.7.5
- A slayer drop line is never left in chat when its animation or chat alert is on: names are matched by their words (book names with or without "Enchanted Book" and brackets), and any unreadable "DROP!" line is written to drop-log.txt.

## Standing requirements added in 0.7.6
- Hypixel's slayer lines: `RARE DROP! ({item}) ({mf})` (item may have `353x  ` in front, brackets may be missing), `SLAYER BOSS! The {Boss} spawned!` (sets the boss), `SLAYER QUEST COMPLETE!` (No Drops scene if no rare drop came since the spawn). The spawn and complete lines are never hidden.
- Slayer scenes are 3 seconds by default; Length goes from 3 to 7 seconds. The scenes need 3.6 s of timeline, so a shorter length is played faster (`AnimationManager.pace`).

## Standing requirements added in 0.7.7
- Pre-reset rules for slayers return: cocoon (`cocooned your slayer boss`) plays the next slayer scene at 1.8x (`Anim.speed`), and `Your Slayer Kill gave you` is a kill. Slayers, Farming and the Experimentation Table share the loose name match `DropData.loose` and the drop line parser.

## Standing requirements added in 0.7.8
- "Also post in public chat" sends a drop to public chat when its animation is RNG (`Item.rngPick`: the pick in the Drop Library, or RNG by rank when on Default), not by rank alone.
- The Length slider of every animation has a (Default) button: 5 s. Every animation, slayers included, defaults to 5 s (supersedes the 0.7.6 3 s slayer default; config ver 6 moves 3 s picks to 5 s).
- Drop icons follow the wiki: Wisp's potion = potion, Wilson's Engineering Plans = paper, Scorched Books and Subzero Inverter = flat wiki pictures (not books/heads).

## Standing requirements added in 0.7.9
- Cosmetic tile rarity numbers: 0 Common, 1 Rare, 2 Beta, 3 Legendary, 4 Special. Only Special (and Beta) tiles have a background effect; Special tiles always play it, Legendary has none.
- Removed for good: the King Minos and Fishing Rod name icons.
- The chat hover card of a name (`Cosmetics.tip`): header, a long divider, then per cosmetic its kind, name, rarity mark (coloured gradient symbol: a hover cannot animate), set and how to get it.
- Pets with several drop rarities have one Drop Library entry per rarity ("Epic Slug Pet", "Legendary Slug Pet", "Legendary Rat Pet"), each with its own chance and George price. A chat rarity in front picks the entry.

- Releases have no description text, and the downloaded file is `Stardrops-<version>.jar` (release.yml no longer reads CHANGELOG.md).

- Commits carry no `Co-Authored-By` or `Claude-Session` trailers (author decision, Oct 27). Author them as tra only.

## Standing requirements added in 0.7.10
- A pet's Value in the animation (and its chat alert price) is what George pays for its rarity (`Item.fixedPrice`, source "George"); the auction price is never looked up for it.

## Standing requirements added in 0.8.0
- Auras are drawn in the level pass (`LevelRenderEvents.END_EXTRACTION` reads each player's body position from its `AvatarRenderState`, `COLLECT_SUBMITS` draws camera-facing quads with `RenderTypes.lightning()`), never projected on the HUD. They follow the body exactly and are depth tested. The moving tail is at most 0.4 s long.
- Compile check for Claude sessions: stage `minecraft-merged-*-26.2.jar` from the project's `.gradle/loom-cache/minecraftMaven` and the Fabric API / library jars from `~/.gradle/caches/modules-2/files-2.1`, then compile with JDK 25 (`javac --release 25 -proc:none`). A clean compile is required before every push.
- Menu look (0.8.0, "vault"): colours come from `Ui` tokens only (PANEL, PANEL2, ROW, ROW_HOVER, BORDER, ACCENT gold, TEXT, SUB, DIM, OFF, HAIR, SHINE). Chrome is neutral; colour is for icons, states and rarities. The open page is a quiet pill with a gold bar. The animation text keeps its own colours (`Ui.ANIM_SUB`), not the menu's.
- The render thread never touches the disk: icon overrides (`Icons.scanNow`), edited animation frames (`AnimFrames.scan`) and the drop log (`DropListener.dlog`) run on background threads. Gradients and borders are drawn in bands, never one fill per pixel.
- Tab icons (0.8.0 "vault" set, IconArt home/dropped/animations/sounds/chat/settings/stats): one family with gold trim, a dark outline and one small motion each. `IconArt.render` is synchronized (the icon export runs off the render thread).
- Shortcuts: Backspace and mouse button 4 run the header back button; Ctrl+F and / focus the search box (only when no text box has focus). The search box hint says so.
- Settings > Cosmetics shows "On you" and "On other players" as one row each of Icon / Effect / Aura chips (`Chip`).
- Normal drops pick a random flourish per drop (`Anim.variant`: 0 rings, 1 starburst + fall-in entrance, 2 spiral + spiral-up entrance). RNG and No Drops always use the original. `StardropsConfig.normalSize` (50-100, default 80) shrinks Normal and No Drops only; their full-screen flicker is at most alpha 10.
- Name tags: cosmetics are applied to the render states at END_EXTRACTION (`AuraFx.styleNameTags`): a player's own tag, or floating text within 1.2 blocks above them, gets their name effect and icon. No mixin.
- Mod colorway everywhere (0.8.1): chat prefix, /fd help and every mod chat line use the menu's gold (`Chat.GRADIENT`, warm white TEXT, violet COMMAND, gold rules `Chat.LINE`). /fd help is framed by gold rules with ▸ bullets.
- Name hover card: [Stardrops], the player's name, a gold rule, one line per worn cosmetic (rarity mark, name in its colour, kind, rarity word), and the set + how to get it once at the bottom when everything worn is from one set.
- Given cosmetics are opt-in for everyone: once `wornUrl` is in cosmetics.json (the Cloudflare Worker in tools/worn-worker.js), other players only see what the owner turned on, and only items they were given. Each player's mod publishes its pick (Mojang joinServer + hasJoined proves who it is) 2 s after a change and on join; the list is read every 60 s. Without `wornUrl` the old behaviour stays (whole set shown).

## Standing requirements added in 0.8.1
- Reworked icons (author request Oct 31): raredrops (open book + floating gem), sounds (gold speaker + arcs), slayers (skull + sword, red eyes), mining (pickaxe striking an ore block), settings (slider panel, not a gear), snd_beat (front-view drum + ring), snd_linger (note with fading echoes), grp_crops (golden sheaf with red band), stats (chart window with a climbing gold line).
- Critical (glitch) name effect: red with a lub-dub heartbeat, a white pulse from the middle outwards. No flashing letter, no siren.

## Standing requirements added in 0.9.0
- Drops stack: an animation holds up to 8 drops (`Anim.more`). A drop of the same group and kind joins a waiting animation, or the playing one before its items appear (0.35 s before the pivot). Items are spaced evenly (`Ritual.offsetsOf`, never wider than 60% of the screen), three or more sit on a gentle arc, they pop in left to right 0.07 s apart and shrink with the count (`sizeOf`). The words list up to three drops (rarest first) and "+ N more".
- Calm mode is applied in `NameFx.color` (one still frame everywhere) and slows auras (moving and idle). The name effect speed setting is applied there too.
- HARVEST BOUNTY! lines: "You found a {item} when harvesting a crop in the Greenhouse!" are farm drops.
- Achievements (0.9.0): the Stats tab is now **Achievements**, an open leather ledger (`Ledger.java`): chapter ribbons over the top edge (The Ledger, Fortune, Mythological, Bosses, Farming, Rare Drops), the left page has the chapter's numbers on ruled lines, a progress bar, a wax seal stamped at 100% and a short quote; the right page has the achievements as gold coins (locked: dashed ink circle with a gold progress ring, secret ones are "???"), 3 per row, page x of y with ‹ › and a page-turn animation. Hover shows progress, the reward and the unlock date.
- Achievements and earned cosmetics live ONLY on the server (tools/worn-worker.js: definitions, stats, unlocks). The mod reports each real drop (never simulated ones) signed with the Minecraft login, and reads /profile/<code>. Rewards: Town Crier → Bell, Regular → Clock, Treasure Hunter (10 RNG drops worth 1M+) → Treasure Chest, Blessed (50 RNG) → Lucky Dice, Executioner (25 slayer drops) → Anvil, Green Thumb (100 farming drops) → Tractor, Crate Digger (5 Vinyls) → Music Disc. RNG means the drop's own rank (2+), not the player's animation pick. The server only accepts worn cosmetics the player owns.
- New libraries (0.9.0): Fishing (Water / Lava / Spooky / Winter sea creatures), Bosses > Crimson Isle, Rare Drops > Rare Mob Drops and Dyes, in fishing.json, crimson.json, mobs.json and dyes.json (`ExtraData`; each source names its animation `group`). `Categories` builds every page (Drop Library, Animations, Sounds, Chat) from the same groups, so editing a file changes all of them. A group with more than one source lists its creatures as sub pages.
- Their drops play the Showcase scenes (`ShowScene`): a pool with a bobber (water, lava, spooky), an ice hole, meteors on basalt (Crimson), a spawner cage (mobs), drops of colour into a basin (dyes, in the dye's own colour). Same three beats as every scene. Routing order: farming, mythological, slayer, then these; a dye that no other library claims plays the Dyes scene. Dye lines are "{Random Word}! [rank] {Player} found {Dye}!" (anything after the dye, like "#1,234", is ignored); the animation plays only when {Player} is your own IGN.
- The ledger has a seventh chapter, The Wilds (fishing, Crimson Isle, rare mobs, dyes). Server group ids may contain underscores.
- **The Collection** (replaces the Achievements and Cosmetics pages, `Codex.java`): a leather book behind one glowing button outside the menu, bottom right (like the community buttons). It flies out of the button over the dimmed menu, the cover swings open on the spine, gold sparks; pages turn as real sheets (lift off the spine, bend with light and shadow, land on the other side with the next page printed on the back). Right-edge bookmarks: Contents + the seven ledger chapters; left-edge bookmarks: the wardrobe (Sets, Name Icons, Name Effects, Auras) with the chat/tab preview and the aura stage. Esc, a click outside or the wax seal closes it. Arrows, space and the scroll wheel turn pages. `/fd cosmetics` opens it at Name Icons.
- Every animation has its own synthesized sound bank (tools/make_scene_sounds.py): `<bank>_beat`, `_burst`, `_reveal` (+`_reveal_big` for RNG), `_glow` (+`_glow_big`), `_end`. Banks: rit (Mythological), tre, sly, crp, fst, bty, pst, tab, wat, lav, spk, ice, crm, mob, dye. The book has book_whoosh/open/land/close/page/tab/locked/equip/unequip.
- Scenes are compact framed vignettes (640x360 canvas, `ShowScenery`), never wider than about a sixth of the screen; RNG finishes stay round the item (no full-screen rays).
- (0.9.0, Nov 4) Main pages: Mythological, **Rare Drops** (Slayers, Crimson Isle, Rare Mob Drops, Experimentation Table, Rift/Events soon), Farming, Mining (soon), Fishing, Dyes. The old Bosses page is gone (its chat/config key stays "bosses").
- Fishing sub sub pages are locations, built from the wiki's Sea Creatures list (tools: /tmp gen2/fish.py pattern): Open Water, Backwater Bayou, Lotus Atoll, Moonglade Marsh, Torrhus Canyon, Fishing Festival, Water Hotspots, Special Catches (Water); Crimson Isle, Lava Hotspots, Magma Fields, Plhlegblast Pool (Lava); Spooky Festival; Jerry's Workshop. Every drop at 20% or less from every creature there; the odds line names the creatures. A dye always plays the Dyes scene.
- The Drop Library sidebar lists a group's creatures/locations only while you are inside that group.
- Several drops in one animation: the finish is held 1 s per extra drop (at most 3 s, `Anim.hold()`), and the words of each drop come in 0.4 s apart, so all of them can be read.
- The Collection's ledger chapters match the main pages (The Ledger, Fortune, Mythological, Rare Drops, Farming, Fishing, Dyes). The wardrobe starts with **Rewards** (every cosmetic the ledger gives, next to its achievement, with progress), then Name Icons, Name Effects, Auras, Sets. Each achievement coin carries a small medal of its reward; locked cosmetics say which achievement unlocks them. The left page is a mirror (chat line + name tag, aura stage on aura pages) that tries on whatever the mouse rests on.
- Ledger rewards: First Find → Light Sweep, Hoarder → Ripple (name effects), Answered Prayers → Neon, Mythic Hunter → Spark; Millionaire → Coin Shower, Mastermind → Runes, Angler → Bubbles, Exterminator → Fireflies (auras); plus the older icon rewards. Non-special name effects always keep the player's own name colour and only add light/movement (Neon pushes that colour to full brightness).
- Aura sparkles have no round halo (a star has none; dots have a faint smooth one).
- (Nov 4, evening) Cosmetics are their own page again (purple button outside the menu, bottom left; `/fd cosmetics`). **Achievements are a star map** (`Codex.java`, button bottom right): one night sky, one constellation per ledger chapter (The Quill, The Coin, The Horn, The Sword, The Sheaf, The Fish, The Drop), each achievement a star; earned = lit star, and a figure's lines light between two lit stars. Click a constellation to fly in (names, progress rings, "→ reward", the chapter's stats panel on the left); right click / Esc / "All constellations" flies out; arrows and the wheel move between constellations. Sounds: sky_open, sky_close, sky_zoom, sky_star. The old book (Ledger UI, book_* sounds, STATS tab) is gone; Ledger.java only keeps the chapter stats.
- Auras are submitted with their own fresh PoseStack and only particles within a few blocks of the player (finite positions) are drawn.
- Star map look (Nov 4 night): a painted deep-sky texture (`NebulaTex`: violet/teal/rose nebula, Milky Way band with dust lanes, 1400 stars) with parallax and a slow drift; twinkling near stars; a warp of streaking stars when it opens and when you fly in/out; shooting stars; telescope vignette; gold star-chart frame with degree ticks and a turning compass rose. Unearned stars are real cool-white stars; earned ones burn in the chapter colour with rays, a halo and sparks running along lit lines; a progress arc and a row of dots per constellation; hovering traces the figure. Stars earned since you last looked ignite (flash + ring) when the sky opens (`StardropsConfig.seenAch`). Drawn on its own GUI stratum so the menu's text never shows through.
- Cosmetic rarities: every ledger reward is rarity **Achievement** (light blue); Beta counts as **Legendary**.
- Auras use their own render type (the lightning pipeline drawn into the main target). Vanilla's lightning type draws into the weather layer, which some setups compose late: that washed the aura's colour over the sky for a frame.

## Star map v3 (performance, stargazer) and the aura flash
- Codex lights are one soft-glow texture each (NebulaTex.glow, 64x64 radial, tinted/stretched); lines, rings and streaks are rotated single fills. ~800 fills + ~180 glows a frame (was many thousands).
- Bottom left of the overview: a hill silhouette with a stargazer, a lantern and a telescope that turns to the hovered constellation (faint sight line). Fades out when zoomed in.
- Progress arcs only show while hovering a constellation; completed constellations get a turning halo of lights. Dyes moved on screen (HOME table).
- Codex.openAt(btn, achId): clicking a locked Achievement cosmetic flies to its constellation and makes that star blink (three pulses of rings).
- Aura flash fix: particles fade out near the camera (gone inside ~0.9 blocks + their size); a particle right at the eye was a screen-filling additive quad in the aura colour. Broken camera vectors skip the frame.
- Beta set is Special rarity with its own green rising-sparkle tile effect; Achievement rarity tiles are light blue with tiny twinkling stars.

## Slayer drops restored, dyes shared (author request Nov 4)
- slayers.json v6: every slayer drop is back (91 items, as in 0.7.8), the six slayer dyes included. The overnight trim was too much.
- A dye has one setting wherever it is listed: `StardropsConfig.drop()` stores any "* Dye" under the "dyes" source, so the slayer page and the Dyes page show and change the same thing (older per-slayer settings move over).
- A dye heard twice within 20 s (the slayer's RARE DROP line and the "WOW! ... found ... Dye" line) plays once.

## Slayer scenes, one per boss (author request Nov 4: "redo all the animations")
- The hologram, the three cuts and the shatter stay (rules above); what makes the cuts differs per boss: Revenant a scythe's curved cut dripping rot, Tarantula silk strands that snap then a cut dripping venom, Sven three claw marks with fur, Voidgloom a jagged rift pulling void motes in, Inferno a burning blade with flames along the cut, Bloodfiend a crimson crescent throwing blood (bats on the last cut).
- Each boss has its own finish round the drop (`Ritual.slFinish`, big for RNG, lighter for Normal): Revenant green souls spiralling up over a ring of rot; Tarantula a web spinning itself with dew glints; Sven a moon ring, claw marks and wind; Voidgloom void cubes orbiting and motes pulled in; Inferno turning blaze rods and climbing flames; Bloodfiend a blood-moon crescent, circling bats and falling drops.

## Slayer scenes rebuilt as Showcase scenes (author request Nov 5: "full revamp, make them look like the other animations")
- SUPERSEDES every earlier slayer-scene rule (hologram, three sword cuts, opaque/tall slayer panel, SlayerScenery places). There is no boss hologram and no sword.
- A slayer drop plays like the Fishing/Crimson/Mobs scenes: the same framed place panel (ShowScenery motifs 9..14, painted by `SlayerShow.place`), three beats at 0.34 / 0.58 / 0.80 of the opening (`SlayerShow.event`), then the drop bursts out, then the boss's finish (`Ritual.slFinish`).
  - Revenant: graveyard under a green moon, iron fence, fog; a rotting hand claws out of the grave on each beat; the grave bursts in a column of souls.
  - Tarantula: Spider's Den (stalactites, webs, red eyes, dangling spiders); a cocoon on a thread jolts and cracks each beat, little spiders scatter; it splits, silk flies.
  - Sven: snowy pines under a full moon; a wolf on a rock howls each beat (rings on the moon, snow gusts); a moonbeam strikes the rock and the wolf leaps away.
  - Voidgloom: the End (obsidian pillars with crystals, floating islets); crystals beam into a void orb over a portal frame; it implodes and bursts, void cubes fly.
  - Inferno: Smoldering Tomb (lava falls, braziers, glowing cracks); three fire vents erupt one by one; a column of fire.
  - Bloodfiend: the Rift château under a blood moon; a coffin rattles each beat (bats escape); its lid flies off and bats pour out.
- No Drops: the place stirs, then falls quiet (the hands sink, the cocoon droops, the wolf fades, the orb shrinks, the vents smoke, the coffin stays shut).
- SlayerScenery now holds only the shared frame corners, ridge and pine.

## Every animation is 6 seconds by default (author request Nov 5)
- AnimCfg.secs default 6 s; the (Default) button sets 6 s; config ver 7 moves picks still at the old 5 s default to 6 s (other picked lengths stay). Supersedes the 5 s rule.

## 1.0 package (author request Nov 6: "update the mod entirely")
- Lengths are FIXED (no Length slider): No Drops 4 s, Normal 4 s, RNG 6 s (bars and blur as before). The words (and the item) stay 2.5 s after the animation ends. The drop appears 1.4 s before the animation ends; finish effects end with the animation. `AnimationManager.animLen / sceneLen / opening` are the only source of these numbers. Supersedes every earlier Length/secs rule.
- Auras: no hand-drawn geometry in the world any more (that path caused the coloured screen flash). `AuraSim` is a small particle system per player (pure Java; also drives the menu preview), `AuraWorld` hands every particle to Minecraft's particle engine as a `SingleQuadParticle` with our sprites (textures/particle/aura_*.png: glow, star, spark, bubble, ember, coin, coin edge, four runes), full bright, translucent. Particles fade out within ~1 block of the camera. Running leaves a short wake; standing still 5 s winds two soft strands round you.
- Achievements: each chapter is a skill tree. Branch = achievements with the same icon (one stat), chained from the smallest goal to the largest, growing from the chapter's heart star. Lit path = earned; dashed path to the next node (it breathes and shows its progress ring); further nodes are faint. The sky is a star chart (declination circles, hour lines, a dotted ecliptic, degree labels in gold ink). Overview cards: Nearly There (3 closest, with bars; click flies there and blinks it), Recently Lit; a legend at the bottom. The stargazer's tripod never moves (the tube turns on its head). The close button is a ring with the X through its exact middle.
- Music: Settings > Music. Spotify login is PKCE with the player's own Spotify app Client ID (Redirect URI http://127.0.0.1:43821/callback). Now-playing polled every 3 s while lyrics are on; synced lyrics from LRCLIB (get, then search by closest length). Lines float in the world like a lyric video: pinned in front of you (alternating left/right, a little up), typed in, warm glow, fading up when the next line comes; the song title appears first. Drawn on the HUD at the projected anchor (never in the world pass). Size and distance sliders.
- Sounds: tools/make_scene_sounds.py remade every bank with new instruments (FM bells/kalimba/celesta/glock, layered impacts, risers, sparkle grains, pumped beds).

## Now Playing and lyrics v2 (author request Nov 6)
- Now Playing card, top middle (`NowPlayingHud`): album cover (`AlbumArt`, 64 px texture, its colour tints the card), title (scrolls when long), artists, a dancing equalizer, a time bar with times, PAUSED. Slides in/out; hides while an RNG animation has its bars down. Settings > Music: on/off, size.
- Lyrics default style is CINEMATIC: always on screen (moving never hides them); the line slides in dim, each word lights up as it is sung (word times spread by length inside the line), pops with a bounce and a glow; the old line floats up, the next waits faintly below; the block hangs on a spring that sways with your view and settles. IN THE WORLD keeps the pinned lyric-video style. Settings: style, font (Minecraft/Clean/Bold/Serif), colour (8 incl. "Cover" from the album), height (low/middle/high), glow, next line, shadow, size, distance.
- tools/worn-worker.js: hasJoined is retried 5 times (Mojang's session server often answers 429/204 to Cloudflare); the 403 says why. Needs a redeploy.

## Visuals page, island, lyrics in your surroundings, achievements galaxy (author request Nov 6 evening)
- New main page VISUALS (sidebar, after Chat): Now Playing, Lyrics, Spotify. Music is gone from Settings. Lyric font and colour choices were removed: words use the default font and take their colour from the album cover.
- Now Playing has two looks: ISLAND (default; a black capsule like a phone's live activity: round cover with a progress ring, title, waveform in the cover's colour; springs open on song start, play/pause and every 45 s to show cover, artists, the current lyric line, the time bar) and CARD (glass card, equalizer). Visuals > Now Playing > Move HUD opens `HudEditScreen`: drag it (snaps to the middle), scroll to resize, R resets. npX/npY/npScale.
- Lyrics style "Your surroundings" (default): each line looks around first (8 horizontal rays, one up). Open sky -> written across the sky far ahead (letters light like stars, a shooting star writes it in, it drifts apart). Ceiling + walls -> stuck flat on a wall you can see like a neon sign (flickers on/off), re-placed if you walk 16+ blocks away. Otherwise -> in the air ahead. Sky and air lines ride along with you: they follow at any speed (never more than a block behind). Words pop (bounce, white flash, glow). "On screen" keeps the karaoke block. No lyrics -> "No lyrics for this one" with drifting music notes (the island says so too).
- Achievements: a galaxy of six groups. The heart, "Every Drop" (the Ledger and Fortune chapters), sits in the middle; round it, one group per Drop Library page (Mythological, Rare Drops, Farming, Fishing, Dyes), each tree growing away from the heart with the page's icon faint behind it. Lanes of light run from the heart to each group and fill with its completion.
- Drops that can't be sent (no verification, no network) are kept in config/stardrops/unsent_drops.json and retried every minute; the star map says how many are waiting.
- Auras log a status line every 30 s ("[Stardrops] auras: ...") to find why an aura may not show; particles are larger.

## Round: walls, vinyl, HUD auras
- Auras are drawn on the HUD by projecting each AuraSim particle (no particle engine); your own shows in F5.
- Lyrics: a wall first (rays ±50°, room checked), else sky/air; the follow is smoothed; neon flicker is fixed-rate, not random.
- Now Playing: default look is "Vinyl" (spinning record with the cover as its label); the Card look stays. It slides below boss bars when they're drawn at the top.
- Achievements: bigger trees, ring round the heart, "Your Ledger" card top left, labels kept apart.

## Round: figures, captions
- Achievements: each group is a figure (Pole Star, Griffin, Sword, Sheaf, Fish, Drop): faint outline + stars on key points, joined by a minimum spanning tree. No chart rings, lanes, faint icons or ledger card. Labels avoid each other.
- Lyrics: one caption (wraps to 2 rows, shrinks if needed), words fill with the album colour, thin time bar; "On walls" writes it flat on a wall in front (9 fixed rays, once per line), else on screen. Sky/air modes and all flicker effects removed.
- Flicker the user saw: Sodium Extra had the sky turned off (not Stardrops).

## Round: floating lyrics, new Now Playing, Ambience
- Now Playing looks: Glass (default), Vinyl, Mini. When paused: stays / hides after 8 s / hides at once.
- Lyrics: Floating (default, follows you, glides back, comes closer than walls, never clipped, no line under it), On walls, On screen (2-row wrap, time line). Song intro toggle, timing offset.
- Ambience page: album glow on the screen edges, music notes by the hotbar (both off by default).
- Music keys: play/pause, next, previous (Options > Controls > Stardrops: Music; Premium; needs re-link for the new scope).
- Drop animations draw on a new stratum (above lyrics); lyrics/ambience fade out during them. Perf: blur/flash/item glow are glows, not disc fills; outlines 4-way; scene upload is one memory copy.

## Round: screen-held floating lyrics, spinning vinyl, Ambience+
- Floating lyrics are held to the screen on a spring (sway on turn, settle back), alternating sides, bob, drift up on exit. No world anchoring (it lagged).
- Lyrics styles: Floating, On screen (On walls removed; old value maps to On screen).
- Vinyl: the record turns (groove highlights, rim mark), the cover label stays upright.
- Ambience: album glow (edge frame, strength), visualizer bars, music notes, song wave, word sparkles. All off by default.
- Visuals pages grouped into sections; each page hides its options when switched off.

## Round: popping lyrics, opaque-text fix
- Floating lyrics pop out at a new random spot each line (band set by Height, away from the crosshair and the last spot), overshoot in, pop away (grow, fade, rise). Wrap results cached.
- Text with alpha < 6 is skipped (Gfx.txt): the game draws alpha < 4 text fully opaque, which froze the music notes and made fading words flash.
- Album glow uses 16 larger glows instead of 32.

## Round: lyrics written into the world, lighter everything
- Floating = signs in the world (like the reference video): each line is placed 5-8 blocks ahead to one side, turned at its own angle with a slight tilt, nearer if a wall is closer; it stays put; words appear one by one (pop) in white/album colours; it rises and fades when the next comes. Drawn per word with 3 projected points (real perspective), no glows or boxes.
- Float precision bug: (now / 1000f) can't move with today's epoch ms; use (now % 3_600_000) — fixed music notes, Codex dashes/streaks/rings, menu pulses.
- Ambience lighter: 4 edge glows, 32 bars, 8 notes without glows.
- PC lag: tab-list name animation only while Tab is held (else once a second); chat effects only when chat changed; Spotify polls on one low-priority thread; auras capped (160 per player, 700 sprites, tiny ones skipped).

## Round: smoother world lyrics
- Spotify.position never steps back by < 1.5 s (poll jitter made lines flick back and forth, each flick writing a new sign).
- World signs use the caption look (all words grey, filling with the album colour, nod, glow under the sung word), fade in over 350 ms, gentler angles.
- Music notes: curling paths, grow/shrink, colour to white, faint trail, small burst at the end.
- Signs 10-14 blocks away (never nearer than 4.5 even at a wall); star bursts back on world lyrics (Lyrics > Star bursts, on by default; was the Ambience "Word sparkles").

## Round: cool, softer colours
- AlbumArt.accent(): the cover colour moved into the cool range (teal..violet, 170-278 deg), saturation <= 0.42, brightness 0.86. Used by Now Playing, lyrics (lightened 25%), ambience, stars. Glows and star points toned down.
- Builds are no longer copied into the mods folder while the game may be running (replacing the jar mid-game crashed it); they go to Downloads.

## Round: one Now Playing
- Vinyl and Mini removed; npStyle gone. The Glass card is simpler: dark panel, faint lighter top half, cover (flip-in, pause sign), title, artists, optional lyric line, 4-bar equalizer, thin time bar with elapsed and length. No drifting lights, sheen or colour wash.

## Round: Lyric line HUD, Playtime constellation
- Lyric line: its own small glass card (LyricLineHud), movable/resizable (Visuals > Lyrics > Lyric line > Move it; HudEditScreen takes NOW_PLAYING or LINE). Removed from Now Playing. Height picker only for On screen lyrics.
- Playtime: minutes played with the mod on (in a world, not paused), kept in config (playMinutes, playDone). 8 milestone stars 1h..500h as chapter "time", merged into Achievements.profile(); constellation "The Hourglass" (Playtime) at (0.6, 0.21). Rare Drops moved to (0.8, 0.77); Recently Lit card removed; key is earned / in progress / reward.

## Round: tracking, library cleanup, mining, new lyrics, clearer stars, short sounds
- Tracking: every real drop is counted locally first (LocalLedger, config/stardrops/ledger_local.json, rules mirror tools/worn-worker.js) and merged with the server's profile (max of each number, a star is lit if either says so). Cosmetics still come only from the server. The server answering 403 "not verified" no longer loses anything.
- Library: Crimson Isle is one list (minibosses + Magma Boss); Crimson Isle Mobs, Ender Belt, Ender Necklace, Obsidian Chestplate, The Rift and Events pages removed. Rare Drops = Slayers, Crimson Isle, The End (group "dragon": Ender Dragon pet, AOTD, Dragon Horn/Claw, Summoning Eye), Experimentation Table. Mining (no longer WIP) = Dwarven Mines (group "mobs") and Crystal Hollows (group "nucleus": Crystal Nucleus loot, Bal, Synthetic Heart, Jasper). New icons grp_end, grp_nucleus, grp_mines.
- New scenes: MINES (a pickaxe strikes a mithril block three times, it bursts), NUCLEUS (five gem crystals lock into the core one beat at a time), END (Summoning Eyes light, a dragon's eye opens, a violet roar), each with its own backdrop. Nucleus loot is read after "CRYSTAL NUCLEUS LOOT BUNDLE"; dragon loot after "DRAGON DOWN!" from "<you> has obtained <item>!".
- Timings: No Drops / Normal 4 s, RNG 6 s, words stay 2 s after (TEXT_HOLD 2.0); multi-drop hold 0.5 s per extra drop, max 1.5 s.
- Sounds: two sets only. std_* (soft, short, alike) for every Normal and No Drops; rng_* (cinematic) for RNG. Minecraft-sound defaults follow the same idea. Old per-group banks deleted (2.3 MB -> 0.2 MB).
- Borders: Normal scenes keep a faint thin edge; only RNG gets the living border (inner line, two sparks racing round it).
- Lyrics rewritten: screen-space only, no world signs, no panels. Pop (words pop in as sung) or Karaoke (line fills with colour, last line above, next below). Movable/resizable (lineX, lyY, lyricsSize via HudEditScreen LINE). Word timing spread over ~85 ms a letter, not the whole gap. LyricLineHud removed. Spotify clock glides to the reported position (no stalls) and takes the request's midpoint.
- Constellations: stars ordered easiest first and placed along the figure's outline, joined 1 -> 2 -> 3; lit stars are big with a halo and a slow rotating flare; the next star pulses with its number and a dashed line leads to it; later stars are small grey. Playtime stars announce in chat when earned.

## Round: achievements opening film, grounded telescope, sky furniture
- Opening film (StardropsConfig.achIntro, default on; click or Esc skips): INTRO 1.9 s before openAt. 0-0.9 s: the stargazer close up ("looking up"), camera at ~3.5x. Then the camera pushes into the telescope's lens (scale up to 70x, cubic) while the lens's light floods the screen. The page then opens out of the lens like an iris (IRIS 0.9 s, brass rim) while the whole sky pulls back from 1.45x to 1x; the warp streaks fly out as before.
- Telescope stands on the ground: ground(x) gives the hill's top; the tripod's three feet, the lantern, the person's feet, the tree and the cat all stand on it.
- Added: a little tree at the hill's edge, a cat by the lantern (tail swishing), fireflies over the hill, a crescent moon with a breathing halo (top middle), a ringed planet (right), far hills with twinkling town lights along the bottom, a satellite crossing now and then.

## Round: smoother, new sounds (1.0b)
- Drop scenes: the pixel scene repaints ~33/s in game (was ~45), 25/s in menu previews; the item, glows and words still move every frame.
- Achievements film: lighter (lens mask fills whole bands above/below the circle and 3-px rows inside; the hill uses 4-px columns while zoomed; the lens position is worked out once).
- Sounds 1.0b: no hits or noise. Normal/No Drops: soft kalimba in D (tick, two-note pop, little arpeggio, quiet hum, two-note close; No Drops = two notes going down). RNG: bell over a low swell, harp glissando, celesta with a soft choir, a shimmering pad, a glockenspiel close.

## Round: drop animations remade (DropShow), Mob Drops
- DropShow replaces the pixel-canvas scenes in play and in the menu previews (the Item / Text / Bars editors keep the classic Ritual scene). Everything is drawn on the GPU each frame: soft dim round the middle, motes spiralling in, a core that beats three times with a ring per beat, a flash and shockwave on the drop (RNG: second ring, spark burst, light rays, cinematic bars, small shake), the item springing in on a halo with glints (RNG: a turning flare). Words: "R N G   D R O P" title (RNG), the name letter by letter rising in (RNG: a shine runs across it), a line drawing itself open, then odds / chance / MF and the price. No Drops: the core folds in and puffs away. Colour = the group's colour (RNG mixes in the rarity colour). Timing unchanged (4 / 4 / 6 s, words 2 s after).
- Rare Drops is now "Mob Drops" and holds Slayers, Crimson Isle, The End, Dwarven Mines, Crystal Hollows and the Experimentation Table; the Mining category is gone. crimson.json / mobs.json bumped to v3 so old copies in config are replaced.
- Home > Resources credits every outside service the mod uses: Spotify Web API, LRCLIB, Mojang API (with Hypixel, NEU, the wiki, Fabric, Minecraft). No Client ID or service keys ship in the repo: the player pastes their own Spotify Client ID.

## Round: small, quick pop-ups (author request Nov 7, reference: Butcher's pop-ups)
- Drop animations must be SHORT and SMALL: never cover the screen. Normal fits about 120 x 60 GUI px, RNG about 180 x 110. No full-screen dim; RNG gets only a soft shade behind the pop-up and thin bars (5.5% of the height).
- Lengths (supersede every earlier rule): No Drops 1.6 s (opening 1.0), Normal 2.4 s (opening 1.2) + 1.4 s words, RNG 3.6 s (opening 2.2) + 1.8 s words. Multi-drop hold 0.4 s per extra drop, max 1.2 s. `AnimationManager.animLen / opening / textHold` are the only source.
- Look: crisp pixel squares (sparks, rings of squares, a pixel diamond core, four-point twinkles), size set by animScale only (not by screen size). The core flickers colours while it builds and settles on the true colour at the last beat: the only tell, and an honest one.
- Words: outlined like the game's drop messages; name typed in left to right, a line opening under it, then two short lines (chance + MF, then price). RNG adds a small "RNG DROP" tag and a shine.
- Bottom position sits just above the hotbar and action bar (item centre H - 104).

## Round: pixel cutscenes in Butcher's style, sounds 2.0, Mining (author request Nov 7)
- Supersedes "small, quick pop-ups": the author wants Butcher's animation AND text style, big and sick, RNG special. Size/place start fresh (config ver 8: animScale 1, Center).
- DropShow 2.0: every shape whole cells on one grid U = H/180 x scale. A rock in the group's colour on a strip of ground takes three strikes (pickaxe for mobs / nucleus / corpse / fiesta / treasure, a blade's slash for the rest): chips, a jolt, a crack per strike. Crack light flickers colours and settles on the true colour at the third strike (the honest tell). Burst: rock cells fly and fall, a diamond flash of cells, a ring, a column of light. Showcase: item rises and pops (back-out) to H*0.085 (RNG H*0.16), rays of cells turning, motes, four-point sparkles.
- Text like Butcher's showcase: black-outlined; name typed in letter by letter (1.25 ts, RNG 1.6 ts); odds line grey; price COUNTS UP in gold ("%,d coins"); RNG: the odds as a bold BANNER stamped in big over the item with a flash of its colour, the price source under the price.
- RNG only: HUD hidden, bars 7.5%, four bands of cells glowing round the screen's edges pulsing with each strike, bigger veined rock, shakes, a pause before the burst with light pouring up, two rings.
- No Drops: three strikes, grey cracks, the rock crumbles into dust.
- Sounds 2.0 (tools/make_scene_sounds.py): stone strikes with an 8-bit tick, a crunch on the burst, a chiptune fanfare reveal; RNG: heavy strike + clang, a boom, a full chip fanfare with a choir and bells.
- Categories: Mob Drops = Slayers, Crimson Isle, The End, Mines (the Dwarven Mines' and Crystal Hollows' creatures, group "mobs"). New Mining category: Crystal Nucleus ("nucleus", chances from the loot table: 16.5 rolls over 307,212 weight), Frozen Corpses ("corpse", Vanguard weights, heard after "<TYPE> CORPSE LOOT!"), Mining Events ("fiesta": Refined Mineral, Glossy Gemstone). "Table & Dyes" (category id "dyes") holds the Experimentation Table and Dyes. mobs.json v4.

## Round: the classic scenes are back, with the new words (author request Nov 7, 19:41)
- The author rejected both DropShow scene versions (the smooth "video" one and the pixel rock). The animations are the classic Ritual scenes again, in play and in previews, with their old timings: Normal / No Drops 4 s, RNG 6 s, words 2 s after, multi-drop hold 0.5 s per extra drop (max 1.5 s).
- KEEP the Butcher-style words: DropShow now holds only them (outlined name typed in, grey odds, gold price counting up; RNG: bold stamped odds banner over the item, price source). Ritual.draw calls DropShow.words; the Text editor still previews the editable TextFx block.
- Sounds 2.0 and the Mining / Table & Dyes categories stay.

## Round: Text Changes for the new words, Frozen Corpse scene (Nov 7, 19:45)
- Text Changes now edits the Butcher-style words (DropShow.words) and its preview plays them (a Normal drop, then an RNG one, in turn). Pages: Style (Name types in, Price counts up, RNG banner: wType / wCount / wBanner), Layout (overall / title / sub-title size, sub-title delay, show chance / price / price source / MF), Font (font, bold name, outline). The old 25 text effects are gone from the menu.
- New scene GLACITE (ShowScene.corpse + ShowScenery.mineshaft) for Frozen Corpses: a Glacite Mineshaft (blue walls, frosted timber, icicles, a cold lamp, packed snow); a dwarf's corpse frozen in a block of ice; a pickaxe strikes three times, frost puffs and ice chips, white cracks deepen, the corpse's eyes glow brighter each strike; it shatters into ice shards with a cold ring; snow drifts the whole time. Crystal Nucleus keeps its own scene; Mining Events use the mithril ore scene.
- The achievement chapter "Rare Drops" is now "Mob Drops & Mining".

## Round: old words back with a counting price, smoother scenes, mining rework, Drop Data, Spotify guide (Nov 7, 19:55)
- The author wants the OLD text (TextFx block, the 25 effects, every Text Changes setting) - only the price counts up now (TextFx.counted: 0.9 s, easing out, from when the price line shows; widths use the full price so nothing jumps). DropShow and its Style page are gone.
- The pixel scene repaints every frame (reuse window 7 ms in game, 16 ms in previews): painting costs under 1 ms.
- MINES: a Minecraft-style ore block (8 x 8 texels, a top face); a pickaxe swung from the right lands its point on the block's top corner (wind-up, a fast 0.09 s blow with a smear, a bite, a recoil); texels are knocked out and breaking cracks grow each strike; chips and sparks; it breaks into little cubes.
- GLACITE (Frozen Corpses): someone random (skin, hair, shirt) walks in with a key (Tungsten / Umber / Skeleton colour), turns it in a lock on the ice on each beat while the corpse's eyes light up; the ice splits in two and falls open; they jump for joy. ShowScene.seed is set per animation.
- Settings > Drop Data: your counts (drops, RNG, unique, value, best), one line per category, and "All drop data as a text file" (config/stardrops/drop_data.txt, opened in your editor).
- Visuals > Spotify > Setup is a 6-step guide (Open dashboard, Create app, Redirect URI with a Copy button, Web API, paste Client ID, Log in), then "Music Functions": the three music keys and what they're bound to, with Change opening Key Binds.

## Round: fishing icons and treasure, icon redo, flat textures, achievements 2.0, /fd achievement (Nov 7-8)
- Fishing sources have their own drawn icons (:src_<id>:), never the dye they carry. New group fish_treasure (Fishing > Treasure): "GREAT/OUTSTANDING CATCH! You found a X." plays; an unlisted treasure still plays (Great = Normal, Outstanding = RNG). Good Catches stay quiet.
- Redrawn: grp_slayers, grp_mines, grp_crimson (a volcano), mining, grp_corpse, grp_crops, cat_table. Drops Hypixel draws with its own texture carry "flat": true and never show a skull skin.
- Achievements come from ONE table: tools/achievements_src.py -> tools/make_achievements.py writes src/main/resources/stardrops/achievements.json and the TABLE block in tools/worn-worker.js. Tiers: Common, Uncommon, Rare, Legendary. RNG = a drop worth 10M coins or more. First-day stars count drops while playtime < 24 h (missed after). Groups keep {drops, RNG, coins}.
- Constellations: Every Drop, Mythological, Mob Drops, Farming, Fishing, Table & Dyes, Playtime (played time to the second), Mining (The Pickaxe). Stars whose goal moved past your numbers go dark; new stars you already earned light on load.
- /fd achievement (and /fd achievements) opens the sky straight away.

## Round: achievements sky polish (Nov 8, 01:31)
- The star card shows the achievement's own tier (Common / Uncommon / Rare / Legendary, coloured), never a cosmetic rarity; "Lit" / "Not yet".
- Stars by tier: lit stars sized 0.8 / 0.92 / 1.05 / 1.3, rimmed in the tier colour; rays: Common one bar, Uncommon and Rare a cross, Legendary an eight-point star with a gold crown ring. Unlit stars are visible small stars rimmed in their tier colour, so every figure reads.
- Less clutter: constellation names are small spaced caps with a thin progress bar and a small count; the top hint is gone; the key at the bottom shows the four tiers, the reward mark and "click a constellation to fly in". An aurora waves across the top of the sky.
- Mob Drops is two crossed swords (14 key points); figures are drawn a little larger (0.12).
- The deep sky (Codex.deepSky): a Milky Way band of glow and ~500 dust stars with dark lanes from the lower left to the upper right, a slowly turning two-armed spiral galaxy in the gap between Every Drop and Playtime, five bright stars with diffraction spikes, and a comet crossing the upper sky every 2.5 minutes. All parallaxed with the camera.

## Round: 01:51 request (achievements, lyrics, Now Playing, size, smoothness, icons)
- Achievement tiers are three: Bronze, Silver, Gold (achievements_src.py C/U = Bronze, R = Silver, L = Gold). Tooltips show a medal.
- Constellations are seeded random star clouds joined by their shortest links (MST). There is no order: no numbers, no "next" star, no dashed path. Any star can be lit anytime; unlit stars show a progress arc once started.
- Depth: each constellation has a DEPTH (size, light, parallax). The sky layers slide by depth with the mouse. Far planets are shaded spheres (terminator, atmosphere, ringed planet).
- Performance: ring() max 32 segments, Milky Way dust 240, small unlit stars without spikes; ~1900 draws a frame (was ~3450). Fly time 480 ms.
- Lyrics styles: Pop, Karaoke, Scroll. Now Playing redesigned (record behind the cover, equalizer, glowing progress bar).
- Item size: old 50% is now 75%; cap 110%. The animation clock runs on nanoTime.
- Sung line (Visuals > Lyrics > Sung line): its own strip docked to the Now Playing card, sliding out from behind it, Off / Above / Below (config npLine; Above falls back to Below near the top of the screen). Lines roll in, sung letters take the album colour, a tick runs along the edge. The menu control is a three-cell "Dock" picker with tiny cards. The card itself no longer holds the lyric.
- Song intro: a vinyl pops up beside the title, spins, a tone arm swings on and notes drift off.
- Achievements update live: Playtime progress, "First day … left" and "Next star in" count down to the second (LocalLedger.live / firstDayLeft).
- New star lit (StarToast): a card slides in at the top right (the star ignites in its constellation's colour, rays by tier, "NEW STAR LIT" in the tier colour, name, constellation · tier · reward, a timer bar) and a chat line with [View]. Clicking the line runs /fd star <id>: the sky opens flown to that star, blinking. Both Playtime and drop stars use it, once per star.
- Opening shot (achievements film, 2.6 s): dusk on the hill (sunset gradient, sun glow), night falls as a time-lapse (stars come out one by one), the camera cranes up off the hill with parallax (sky moves little, the hill a lot), constellations come into focus from soft blurs. No lens push.
- Backdrop: two mountain ranges with haze, a horizon glow and a far town, thin drifting clouds, near out-of-focus motes. Hill grass is fine pixel blades (no swaying lines); firs bend from the foot; the stargazer has a knitted bobble hat, a long scarf, a pack, a blanket, a steaming mug, and leans back to look every ~14 s.
- Tiers: bronze 0xB4602E (deep copper), silver 0xD2DCEC, gold 0xFFE04A.
- Sung line is a lyrics style (4th), docked Above/Below the Now Playing card; Position/Size/Next/Previous hidden for it. Strip: clean glass, note in a soft light, hairline progress. "Previous line" toggle (off by default). Star bursts only for Pop; Next line hidden for Scroll.
- Word pacing: syllables and punctuation (commas hold) set when each word is sung; line spread over most of the gap unless the gap is long.
- Icons: sty_pop, sty_kara, sty_scroll, sty_sung, lyr_prev; mus_play is a round button switching play/pause (never both).
- Performance: the landscape (two mountain ranges, the hill with grass, three firs) is baked once into two textures at 2x (Codex.bake, NebulaTex.layer) and redrawn only on resize; the menu is not drawn at all while the sky covers it (Codex.covering). ~1200 draws a frame (was ~3100).
- No mouse parallax: constellations and the sky stay still. Depth is by size and light only.
- Opening (2.6 s): out of black, the last sunset glow low on the horizon fading, bright stars first then faint ones (eyes adjusting), the Milky Way last, a slow tilt up (ground slides more than sky), constellations come into focus and their lines draw themselves star to star.
- Mountains: dark night silhouettes from ridged noise, a little mist at the foot. No outlines on the stargazer; the lantern lights a warm pool on the grass and the nearby grass, two moths circle it.
- Tiers at a glance: the lit star itself is in its tier colour. Bronze: small copper diagonal cross; Silver: white long cross; Gold: large yellow eight-point star with a ring and halo.
- "Ledger since" renamed "Counting since" (the day Stardrops started counting your drops).
- Lyrics: word-timed (enhanced) LRC is used when LRCLIB has it (exact word sync); otherwise the pacing guess. Now Playing's colour wash starts at the very edge (no dark line on the left).
- Lyrics sources: NetEase Cloud Music first (search by title + first artist, length within 4.5 s; its "yrc" lyrics time every word), then LRCLIB. latest.log says which: "[Stardrops] lyrics: <song>: ...". The menu note shows "Word-synced lyrics from NetEase" when it worked.
- Per-song timing: StardropsConfig.songOffset (track id -> ms) on top of lyricsOffset. Visuals > Lyrics > This song slider (while a song plays) and two keys, Lyrics earlier / later (±0.1 s, shown above the hotbar), in Controls > Stardrops: Music.
- Anti-cheat (server, tools/worn-worker.js): a drop's coins are capped at 1.6x its market price (Hypixel bazaar + moulberry lowestbin, cached 15 min in KV "market"), times its amount (event "n", max 64); items the markets don't know count at most 25M. Daily limits: 1500 drops, 600 per group, 30 RNG. Playtime is counted by the server from the gaps between heartbeats/drops (<= 7 min apart); the game sends an empty /event every 5 min as a heartbeat. First-day stars use the server's playtime. The client's "pt" is no longer sent or trusted.
- Only server-confirmed stars are lit. Stars counted locally but not confirmed are "waiting" (LocalLedger.WAITING): a breathing ring in the tier colour, "Earned: waiting for the server to confirm it" in the tooltip, "N waiting for the server" in the header, left out of Nearly There. "New star lit" cards only come from the server.
- ledger_local.json is sealed (HMAC in ledger_local.sig); a hand-edited file is ignored. Files from before the seal are accepted once (StardropsConfig.ledgerSigned).
- Long opening (Codex.longTimeline, 7 s): the first time the sky opens after each game start. Afternoon sky, the sun sets behind the far mountains (sunset gradient, daySky), night falls and stars come out, a long exposure draws star trails round a pole star (trails), the trails pull back to points while "ACHIEVEMENTS / every drop is a star" rises and fades, then the constellations focus and draw themselves. Every later opening uses the 2.6 s night-falls one. Clicking skips either.
- /fd star <id>: tab completion lists every achievement (id or name prefix), with name, constellation and tier. It opens with the short opening, flies to the star and blinks it; while in that view only that star responds (others dimmed, constellations not clickable, scroll/arrows off) until you go back (the All constellations button, right-click or Esc).
- NetEase lyrics removed again (the user found them worse: lines vanished after their sung length and credit lines showed). Lyrics come from LRCLIB only (with its word timing when present, else the pacing guess). Credit lines ("作曲 : …", "Lyrics by: …") are dropped. Per-song timing and the earlier/later keys stay.
- Minecraft stargazer (Codex.stargazer, mcBox): Steve (skin, brown hair, cyan shirt, blue legs, grey shoes, Minecraft proportions, p = 0.8k) sits with his legs out along the slope on a red carpet, looking up through a copper spyglass with an amethyst lens that follows the constellation under the mouse (every 14 s he lowers it to his lap for 3 s). A ginger tabby cat lies by a Minecraft lantern (iron frame, flame, hook) with moths, a chest beside them. Spruces in the baked land layer step by whole blocks. Colours are darkened for night (lit factor), the flame and the cat's eye stay bright.
- Now Playing redone: plain dark glass (0x0D0E13) with a fine light edge and a top sheen, no album-colour wash or tinted glows; a black shadow under the cover; grey labels; white equalizer; a white progress bar and knob. The Sung line strip uses the same glass.
- Beat line (Beat.java): the per-word sync helpers are gone (Pop's gliding underline, Karaoke's bouncing dot, the strip's hairline). Under the current line in all 4 styles: four short bars, one per beat of the bar, the current one lighting and fading (the first beat brighter). Tempo from Deezer's public API (search by title + artist, length within 6 s, track bpm, folded into 70-190); the beat's phase from where the lyric lines start (circular mean). No tempo: the line breathes slowly. latest.log: "[Stardrops] beat: <song>: <bpm>".
- Lyrics show whole lines (no word timing). Countdown line under every style: fills from the line's start to the next line, its end lights as the next line nears (Countdown line toggle). Styles: Pop (white words cascade in with a spring), Glow (bold words in the album colour track in, a sweep of light crosses once), Scroll, Sung line. Extras: Countdown line, Sparkles (new line), Glow (light behind), Backdrop (dark shade), Song intro, Timing, This song. Beat.java removed.
- Achievement page figures are silhouettes (mcBox: only lit >= 1, i.e. the lantern flame, keeps colour). Steve's far arm removed (read as an X).
- Now Playing vinyl: slides 25 px out (card 226 wide, text from x 84); fine grooves, two fixed highlights, the album art as a spinning label, a spindle, the cover's shadow on it.
- SPACE THEME (the mod is becoming stargazing / space themed). Lyrics first:
  - Styles: Starlight (each word flares as a star with a cross of rays, then opens into the word; pale starlight 0xEEF2FF), Constellation (a star above each word at slightly different heights, joined left to right by fine lines like a star chart; the stars twinkle), Drift (the lyric sheet, neighbours in cool blue-white), Sung line.
  - Countdown: a comet with a fading tail crossing a dotted path to a small star that brightens as the next line nears ("Comet countdown").
  - Extras renamed: Stardust (sparkles), Nebula glow (album colour blended with violet 0x6A4AD8), Backdrop. Song intro: a ringed planet rises beside the title with a moon circling it.
  - Icons sty_pop / sty_kara / sty_scroll redrawn as star bloom / constellation / drifting lines.
- Menu, space theme pass 1: Ui palette is the "observatory" look: night-blue panels (PANEL 0x0B0E1C, ROW 0x121630), brass accent 0xE8C26A, starlight text 0xEEF2FF. Inside the panel (spaceBack): the achievements' night sky texture faintly (42%, darkened), 46 twinkling stars, a shooting star every 9 s, brass astrolabe ticks at the corners. Section headers end in a fading star-chart line starting at a small brass star. (The menu can't be rendered in the bench; check in game.)
- The mod is renamed STARDROPS (display name only: fabric.mod.json name, menu title, chat prefix, guides, keybind category, achievement texts). The mod id, package, config folder and jar name (Stardrops-0.9.0.jar) stay.
- Palette "deep nebula": PANEL 0x0E1030, PANEL2 0x070816, ROW 0x14163E, BORDER 0x2C2A66, ACCENT violet 0x9B7BFF, ACCENT2 nebula pink 0xFF7AD9, TEXT 0xEEF0FF. Title and chat name run violet into pink. The menu backdrop adds pink and violet nebula clouds. The achievements frame and titles are violet (0xA88BFF / 0xE6DCFF); tier colours unchanged.
- Now Playing (space): a window into space (stars drifting past), a moon circling the cover (violet rim), "NOW PLAYING" in lavender, signal bars violet into pink, and the progress is a pixel rocket flying along a dotted course to a ringed planet with a pink-to-violet exhaust trail. On a song change it blasts off: stars stretch into warp streaks, the rocket fires off the right edge (a pink flash where it left), the new song slides in, and the rocket flies back in from the left. The Sung line strip uses the same glass.
- Achievement page: the cat sits upright (clear silhouette: haunches, chest, head with two ears, tail round its paws, tip flicks); the chest is gone (it read as a black box).

## Round: Music / Cosmos, launch control, cleanup
- Long lyric lines: rows fit the width (scaled down when the widest row is too long); Constellation rows sit ROW_C apart with stars per row (no links across rows); Drift pushes neighbours clear of a two-row middle line.
- Visuals has two subs: **Music** (picker: Now Playing / Lyrics / Ambience / Spotify) and **Cosmos**. New Visuals tab icon (telescope + stars).
- **Cosmos** (CosmosHud): shooting stars (optionally night only), starry corners at night, aurora, a big meteor before an RNG drop, celestial clock (sun/moon arc + moon phase, top left). Everything fades during drop animations.
- **Active control** (LaunchPad): no lever. A launch site in the pill: off = rocket docked on its pad, "DOCKED"; on = lift-off with flame and smoke, climbs into orbit round a ringed planet, "LAUNCHING" then "IN ORBIT"; switching off plays the landing. Click behaviour unchanged.
- Cleanup: removed the unused old Codex intro/lensMask, per-letter lyric timing (wordAt/pace, Lyrics.WORDS; enhanced-LRC word tags are now just stripped), one shared HttpClient (Net.HTTP) instead of a new client per request, lyric row words split once and cached.
- Now Playing course matches the Active control: launch pad at the start (light green playing / red paused), lift-off with smoke in the song's first moments, dotted course with a pink-violet trail, and in the last seconds the rocket swings into orbit round the ringed planet (passing behind it).
- Now Playing > Background slider (npBg, 20..70 %, default 60): how solid the card's glass is; the Sung line strip uses the same value.
- Now Playing is a voyage (replaces the straight course): every song is its own world (Worlds: ringed violet, cratered moon, banded gas giant, ice, lava, ocean; picked from the track id). The rocket flies an arc (up and over, an "n") from the last song's world on the left to this song's world on the right, dotted ahead, a fading trail behind, and in the last 3 s falls into orbit round it. On a new song the view pans 1.3 s: the world it circles slides left to become the departure, a new world drifts in from the right, and it sets off. Time shows small under the arc ("1:14 / 3:28"). Card is 64 tall (72 with a lyric). No warp streaks. The moon round the cover stays inside the card.
- Active control is a window onto a launch: off = docked on the pad on the ground under a dusk sky with hills; on (2.6 s) = lift-off with smoke, the camera follows it up so the ground drops away, the sky darkens to space, stars streak past then settle, the violet ringed world rises into view and the rocket swings into orbit ("LAUNCHING" / "ARRIVING" / "IN ORBIT"); off (1.8 s) plays it backwards ("LANDING").
- Active control is a status light, not a show: reopening the menu shows the current state (docked or in orbit) without replaying; only a real switch animates. Landing (3 s) is its own path: in orbit the rocket turns round to fly engine-first (retro burn), drops out of orbit, swings upright, and descends tail-first on its flame as the ground rises, the burn easing off and landing legs folding out for touchdown. Orbit is a smooth ellipse (one turn ~7 s), passing behind the planet.
- Commands are /stardrops and /sd (the old /stardrops and /fd are gone). /sd edit opens the HUD editor for every placeable HUD at once (Now Playing, lyrics, celestial clock; not the drop animation): drag any, scroll over one to resize, R resets the one under the mouse (or all), Esc saves. The celestial clock is now movable and sizeable (cosClockX/Y/Size).
- Ambience is space themed (same toggles): Nebula (clouds drift along the screen edges in the album's colours with stars caught in them; strength slider), Star equalizer (columns of stars beside the hotbar, the top star brightest), Comets (curve up beside the hotbar, trail light, burst into sparks), Supernova (new song: a flash and a ring of light expanding across the screen). New icons for each.
- Achievements: every group is a real constellation, credited under its count: Every Drop = Ursa Major (Polaris, the heart, where the pointers aim), Mythological = Orion, Mob Drops = Scorpius, Farming = Virgo, Fishing = Pisces, Table & Dyes = Cassiopeia, Playtime = Cygnus, Mining = Taurus. Achievements take the stars in id order; extras sit halfway along lines, missing ones leave the faintest off.
- Stargazer scene redone: no carpet (it drew a wavy line), a calm seated silhouette (knees up, body, head that tips a little with the scope), a telescope on a tripod whose tube turns smoothly (time-based easing) to the constellation under the mouse, the arm steadying it; the cat sits right beside the figure; the lantern a little way off.
- "N waiting for the server" = stars earned locally that the worker hasn't confirmed; they stay queued (pending file) and are sent once the worker answers (needs tools/worn-worker.js deployed).
- Hill scene: the figure (Minecraft proportions, silhouette) sits with legs out on the grass, leaning back, gazing up; the cat lies down beside them (head at the lantern end, tail along the grass, flicks). At random moments (65% of 9 s slots, 3.2 s each) the figure looks down and strokes the cat's back; the cat lifts its head, its tail swishes, a little warmth rises. The telescope stands on its tripod beside them and turns to the hovered constellation. The lantern casts soft shadows across the grass (away from it) and lights the edges facing it (warm rim). 11 fireflies.
- Hovering a constellation lights it clearly: stronger glow, lines in its colour with a soft glow, every star brightens. The hill scene (figure, cat, telescope, lantern) is not a hover/click target for constellations (Fishing used to open from it).
- Background loops seamlessly: the sky texture drifts out and back (cosine) instead of jumping every 4 min; clouds wrap fully off screen on exact periods.
- Arrow keys no longer flip between constellations (Esc still goes back / closes).
- Hill scene, cleaner: the figure lies on their back in the grass (Minecraft proportions, along the slope): head by the cat, hands behind the head, one knee up, chest rising and falling. The cat is curled up by their head (its own frame on the hill), head toward the lantern. Petting: one arm comes over to stroke the cat's back. Telescope stands to the right of their feet.
- Server auth: Mojang's session server refuses Cloudflare workers (403), so stars stayed "waiting". Now every account has a key (config/stardrops/keys/<uuid>.key) sent with uuid on every request; the worker ties it to the account whenever Mojang confirms, and accepts it alone when Mojang refuses (first key seen for an account is kept). Needs tools/worn-worker.js redeployed.
- Song intro (4.4 s), a hyperspace arrival: streaks rush in to the title's point, a flash and a shockwave ring in the album's colour; the cover drops in on a glowing frame; the title decodes letter by letter from scrambled glyphs while its spacing tightens (a spark as each letter locks); the artist types out with a blinking cursor; two little star chains draw above and below; then everything jumps to lightspeed to the right with streaks.
- Hill figure's bent leg is two limbs joined at the knee (hip→knee→foot), not two boxes pivoting from their corners.
- Song intro bigger (title 2.2× size, cover 36), "Now playing" small over the title's first letter (no spaced-out caps), sits at 31% height so it clears the Now Playing card.
- Achievements sky has depth: three star layers drift sideways at different speeds (far slow and faint, near bright and quicker); five planets at different depths drift right across the sky on their own periods (near ones bigger, brighter, faster; each wraps fully off screen so nothing jumps); the spiral galaxy is about three times bigger with glowing arms, dust lanes, 140 stars and pink star-forming knots, turning once every 40 minutes.
- Space redo, round 1:
  - Sounds: a new space pack (tools/make_space_sounds.py, same 15 names): laser zaps on the beats, a plasma pop / supernova for the bursts, a star chime and a cosmic fanfare (C maj9 supersaw + choir + glock) for the reveals, a space hum / deep-space drone for the lingers, warp whooshes for the ends, a power-down for No Drops; the achievements sky's open/close/fly/star are warp and twinkle sounds. Labels in the Sounds tab renamed to match.
  - Animations (SpaceScenes): Slayer = a cosmic bounty hunt (warp in, a hostile world in the boss's colour with a debris ring and a watching eye, each boss its own surface motif, a reticle locks on, three laser strikes on the beats crack it, it shudders, implodes and goes supernova; RNG adds an accretion disk, rays and stars spiralling in; No Drops: it warps away). Every other group plays the WIP scene for now: a hazard-striped stargate assembling segment by segment while a little drone welds it (sparks on the beats) beside a pixel "WIP" sign; the gate powers up into a swirling nebula and the drop comes out (RNG: rays and a burst of stars; No Drops: it sputters out). SpaceScenes.ON switches back to the old scenes (still in Ritual).
  - UI icons redrawn space-style: home (observatory dome on a moon), drop library (meteor into a glowing crater), animations (planet with an orbiting moon), sounds (radio dish), chat (nebula bubble with stars), settings (ringed-planet cog), collection (star atlas), trophy (starlight cup), stats (star chart), move (drifting astronaut), size (growing planet), back (rocket turning home), updates (satellite), switch (star knob), speed (rocket), clock (moon phases). Content icons (fishing, mobs, items, sources) unchanged.
- SLAYER, remade (SlayerHole): a black hole in the boss's colour eats the boss. Own timeline (AnimationManager.animLen/opening/textHold/sceneLen by group): hole opens, the boss (new pixel art: Revenant zombie in diamond, Tarantula, Sven wolf, Voidgloom enderman, Inferno blaze, Riftstalker vampire) beams in beside it, leans in, and is torn apart pixel by pixel (spaghettified, spun into the disk, nearest first) in three gulps at 1.75/2.35/2.95 s (the sound beats); gone at 3.4 s. The hole: lensed stars, a tilted accretion disk (Doppler-bright left side, its far side bent up over the top), photon ring, pure black horizon; a patch of space, never the whole screen.
  - No Drops 5.5 s: the hole swallows the scene into a point that winks out; "Nothing dropped" + the boss's name fade in (no old "Nothing :(" text).
  - A drop 6 s: polar jets fire, a ring, sparks, the drop is spat out of the heart; the hole shrinks away; the words stay 3 s after.
  - RNG 7 s: collapse, supernova flash, two shock fronts, a remnant nebula with filaments, a pulsar sweeping two beams behind the drop, an Einstein ring, shards of the boss circling; bars close in at 2.9 s and the HUD (hotbar, hearts, and our own Now Playing / lyrics / Cosmos) slides away, all coming back when the animation ends (7 s), the words staying 3 s.
  - Cocooned boss: speed = animLen / 3.5, so it plays in about 3.5 s.
- Every other animation group is marked WIP in the menu (tiles say "WIP · being remade", the editor has a WIP note) and plays the space-construction stargate.
- Correction (author): "WIP" means the page is LOCKED with the original WIP panel, not a new scene. Animations and Sounds: every group but Slayer shows "Soon™" tiles and the wipPanel; in-game they keep playing their old animations. The space-construction stargate (SpaceScenes) is removed.
- Slayer has its own sound set, its own style (tools/make_slayer_sounds.py, sl_*): sl_open (reversed swell into a sub bend-down, plays as the hole opens, fixed), sl_gulp (sub drop + bit-crushed stuttering "pixels tearing"), sl_collapse (everything rushing in, hard cut, a tiny wink), sl_spit (pitched thwup + jet hiss), sl_nova (in-breath, sub boom, air blast, distorted ring, long settle), sl_chime (glassy detuned three notes), sl_reveal (wide slow supersaw chord + glass bells), sl_glow (low breathing drone), sl_pulsar (fast ticking pulses over a hum), sl_close (reversed sigh). Slot names in the Sounds tab follow the animation: Gulp / Collapse|Spit|Supernova / Reveal / Afterglow|Pulsar / Close. Gulps play slightly lower each time.
- Performance: the black hole's big soft patch of space is drawn as GPU glows under the pixel scene (SlayerHole.backdrop), not per pixel; the pixel scene repaints at most ~90 times a second in game. Scene paint ~0.7-0.9 ms a frame.
- Song intro smoother: cover and underline move on sub-pixel positions (no 1-gui-px steps), the scramble ticks 16/s, each letter drifts down and settles from 125% size, 32 entry streaks.
- Locking (author): in Animations and Sounds every category but Mob Drops is locked (sidebar shows it as WIP, opening it shows the WIP page); inside Mob Drops every group but Slayers shows "Soon™" and the WIP page. In-game those keep their old animations. The Drop Library is unaffected. Mob Drops is now the first category everywhere.
- WIP page (SpaceSite): a space station under construction instead of the old building site: stars, a planet below, a half-built ring station turning (built modules lit, empty slots only hazard-yellow scaffolding), a crane swinging the next module in, an astronaut on a sagging tether welding (sparks), a cargo shuttle drifting in to dock and leaving (14 s loop). Hazard tape and the "Coming Soon" / name boards stay.
- Slayer sound: not a sound per beat any more. One soundtrack per animation, scored to its timeline (tools/make_slayer_score.py): slr_nothing 5.6 s, slr_drop 7.2 s, slr_rng 9.6 s. A dark D drone bending upward through the pull, heartbeats coming faster (1.2 s, gaps 0.62 x 0.78, min 0.17), bit-crushed tearing growing as the boss comes apart; nothing: swallowed, a wink, two soft minor notes; drop: spit, jets, a release into D major with glass chimes; RNG: collapse, supernova, a choir and a ticking pulsar groove (132 bpm eighths) closing on a wide D. The Sounds tab shows one "Soundtrack" slot for Slayer. The old sl_* set is deleted.
- Slayer animation: one continuous pull (no gulps), speeding up 1.15 -> 3.4 s; the hole throbs on the soundtrack's heartbeat (same timing); pieces never cross the horizon: they slow, smear round it, redden and fade (time dilation).
- Round "Slayers only" (author: "Just redo the entire animation and sound package for Slayers ONLY ... Delete the other animations, and slap the WIP page on it"):
  - Only Slayer animates. Every other group sends its chat alert and plays nothing (AnimationManager.play) until its new animation is made. The old scenes are deleted: FarmScene, FarmScenery, BookScene, ShowScene, ShowScenery, SlayerShow, SlayerScenery, Site, AnimFrames (frame export) and the old Ritual opening/finish code (Ritual is now the frame: canvas, bars, item, words; SlayerHole paints the scene). The std_*/rng_* sounds are deleted (files, sounds.json, registrations); non-Slayer sound defaults are empty; achievements chime with sky_star.
  - Drop Library stays usable for every page (pick each drop's animation and chat alert now; the picks carry over when the animations arrive). A WIP tag sits next to every category and group in the sidebar that is not animated yet, and the area under a non-Slayer page's tiles/drops shows the WIP panel ("Animation" section). Animations and Sounds stay locked except Slayer.
  - WIP panel redone (SpaceSite), an orbital construction yard: three parallax star layers, a ringed planet, a comet now and then; a truss with a lit habitat module, a solar wing unfolding and a crane swinging a girder; astronauts in white suits with gold visors and jetpacks (one welding with sparks, one tumbling on a tether with a wrench, a far one waving); bubbles floating up and popping; a spinning wrench, nut and crate; the page name as a cyan hologram (UNDER CONSTRUCTION header, scanlines, glitch, a build bar) projected by a hovering drone. Hazard tape drawn over it. The gold "Coming Soon" and name boards are gone.
  - Slayer camera (Ritual.slayerCam), like a film: dolly in during the pull, a kick and a jolt on every heartbeat, a slight roll, a punch-in on the last gulp that eases back to 1 as the drop appears (the drop sits on the pivot so it stays lined up); No Drops dives and spins into the hole; RNG is blown back by the supernova and rumbles. The previews use the same draw, so they show it too.
  - Header shows a green "Spotify Linked" chip after the version when Spotify is linked (its dot breathes while music plays).
  - Settings icon: a clean eight-tooth gear with a glinting hub (held still: a turning 16 px gear jitters).
  - Sky flicker: the three star layers no longer drift (sub-pixel sliding of 1 px stars read as flicker) and the planets are drawn on whole-pixel centres.
- Follow-up (author): the WIP panel under drop pages has no "Animation" section line and is 112 tall (fits without scrolling); the yard scales with the panel. Station redone: a spinning habitat ring (solid band, six pods with lit windows sliding round) on a hub with a blinking green docking port, a truss with a cyan chase light, white radiator fins, a pair of gold-framed solar wings with light gliding over the cells, a red strobe, a maintenance drone circling the ring, the crane at the open end; the ringed planet rises at the bottom right.
- "Spotify Linked" chip moved into the sidebar, centred under the logo above Home.
- Category tiles (Drop Library, Animations, Sounds) show the WIP tag for every category but Mob Drops, and group tiles for every group but Slayers. No counts or other lines under tile names ("4 pages · 9 animations", "30 drops", "Soon™" are gone).
- Menu tiles (not cosmetics) are space glass: cut corners, faint scanlines and a few twinkling stars, a thin edge and bright corner brackets in the tile's colour, a light beam sweeping across on hover, a glowing base line on the open one; WIP is an amber holo chip with a blinking light.
- Sizes (author): the "Everyday drops size" setting is removed; every animation type plays at the one Animation size.
- Text duration: Animations > Text Changes > Layout has "Text stays after the animation" (1-8 s, default 3, StardropsConfig.textHold). Every Slayer type uses it, No Drops included ("Nothing dropped" now stays as long as a drop's words).
- Slayer sounds split again into five moments, three takes each (tools/make_slayer_sounds.py, slr_<family>_a/b/c; the one-piece soundtracks are gone). Open (0.1 s: Rift / Gravity well / Event horizon), Pull (1.15 s, heartbeats on the hole's throb: Heartbeat / Spaghettify / Tidal choir), then by type: No Drops Swallow (Implode / Gulp / Vanish), Wink (Wink / Pop / Blink), Nothing (Minor notes / Falling sine / Static); a drop Spit (Thwup and jets / Ejection / Jet stream), Reveal (Major chord / Glass bells / Starlight), Close (Sigh / Fading chord / Soft bell); RNG Supernova (Supernova / Shockwave / Collapse), Fanfare (Choir / Cosmic fanfare / Glock run), Pulsar (Pulsar groove / Beacon / Quasar). Each slot's dropdown offers its three takes (plus None); the first take is the default. Future animations follow the same pattern: five moments, three takes each.
- Slayer bosses redrawn closer to their Minecraft mobs (author: "cleaner and more original to the minecraft version, not flat"): Revenant is a zombie (MC head and face) in a diamond chestplate and leggings with red glowing eyes; Tarantula a spider face-on with eight red eyes, mandibles and splayed legs; Sven a white wolf side-on with a red collar; Voidgloom an enderman (8-wide head, magenta eyes with light centres); Inferno a blaze with its rods; Riftstalker a vampire with a red-lined cape and fangs. Each pixel is shaded once like an MC texture (grain, light top/left edges, shadow bottom/right edges, darker toward the feet); glowing eyes stay pure.
- Slayer bosses redrawn again at Minecraft model proportions, one sprite pixel per model pixel, drawn at exactly one canvas pixel each (crisp, much smaller than the hole): Revenant a classic zombie side-on shambling at the hole with both arms out (red eye, torn cyan shirt, blue trousers, legs mid-stride); Tarantula a spider face-on with an abdomen, eight eyes and four jointed legs each side; Sven a white wolf side-on with a red collar and raised tail; Voidgloom an enderman; Inferno a blaze with rods round and below; Riftstalker a vampire with a high-collared red-lined cape. The hole is a little bigger (rh0 24) and the boss waits just outside the disk (rh0 x 3.7).
- The jar is named Stardrops-<version>.jar (was Stardrops-...).
- Sky flicker report (in-game sky alternating blue/grey every frame, name-tag backgrounds alternating too): Stardrops does not touch the world sky, fog or options (no mixins); same symptom as the earlier Sodium Extra sky setting.
- Sky flicker (confirmed Stardrops by the author): AuraWorld drew auras with cam.getViewRotationProjectionMatrix() from the HUD. That getter caches the matrix and clears the camera's dirty flag, so the world's own sky/fog pass every other frame used a stale matrix. Auras now build their own view-projection from cam.rotation() and cam.getFov(); never call the camera's matrix getters from the HUD.
- Drop Library tiles (categories and pages) show no WIP tag (the sidebar keeps its WIP marks; the WIP panel stays under non-Slayer pages). Tiles show their counts again under the name everywhere (Drop Library: pages · sub · drops / "N drops"; Animations: pages · animations / "N animations"; Sounds: sound sets / "N animations · M sounds"). Animations and Sounds tiles keep their WIP tags.
- Drop rows: columns left to right are Preview, Animation, Chat Alert. The Preview column exists only on pages with an animation (Slayer); other pages have just Animation and Chat Alert (their header matches).
- Name: every "Stardrops" the player can see is "Stardrops" (the chat tag on drop alerts is "[Stardrops] | ", hover cards, user agents, thread names, log tags). The mod id, package, config folder (config/stardrops) and asset paths stay "stardrops" so settings and saves carry over.
- Voidgloom Seraph icon: an enderman's head (black, faint mottling, glowing purple eyes, ender particles rising at the sides).
- Full rename to Stardrops (author: "change everything to Stardrops"; the GitHub repo becomes imtraminecraft/stardrops): mod id stardrops, package dev.stardrops, StardropsMod / StardropsScreen / StardropsConfig, assets/stardrops (sound, font and texture ids stardrops:*), data in resources/stardrops, keybind keys key.stardrops.*, gradle project stardrops. Settings move once from config/fancydrops.json and config/fancydrops/ to config/stardrops.json and config/stardrops/ (StardropsMod.migrateConfig, before anything reads them). The deployed worker keeps its hostname (fancydrops-worn.imtraminecraft.workers.dev) until it is redeployed under a new one.
- Star-lit chat line: "[Stardrops] ★ New star lit: <Name bold, constellation colour> (<constellation>, <tier in its colour>) [View]" (star and tier in bronze/silver/gold, underline only under [View], brighter brackets on the tag). Click/hover open the star.
- Achievement sounds remade (tools/make_sky_sounds.py; make_space_sounds.py removed): sky_open (airy swell, glass bells rising), sky_close (reversed shimmer into two falling bells), sky_zoom (airy whoosh with a gliding chime), sky_star (crystal ping), and sky_lit (new: the "star lit" chime, a low bloom, glock/celesta arpeggio, choir swell, sparkles) played by SoundFx.achievement().
- New icons: stardrops (violet droplet with a gold star, sparkles trailing), resources (telescope looking at a twinkling star; the Resources page uses it), visuals (HUD viewfinder brackets round a ringed planet), sounds (speaker with star rings), chat (night-sky speech bubble with a gold star and typing dots), discord (a crew radio with voices), patreon (a gold star held in cupped hands), github (a code "</>" disc with an orbiting commit). The community icons are our own space drawings, not the brands' logos; players can drop official icons into config/stardrops/icons/<name>.png to replace them.
- Round "meteor slayer" (author, from a Butcher video reference, but our own art): no black hole any more (SlayerHole deleted, SlayerScene new). The boss climbs out of its spawn (Revenant: gravestone; Tarantula: drops from a web; Sven: den; Voidgloom: end portal frame; Inferno: lava pool; Riftstalker: rift) at RISE 0.45 s and walks toward you on a floating island in its own soil, WALK 1.45-2.5 s. Then meteors: No Drops one strike (lands 3.0 s), the boss crumbles to ash; a drop: a barrage of seven (2.45-3.36 s), the last shatters it and the drop rises in a beam; RNG: a shower of seven whose colours climb the rarity tiers like a gacha roll (white, blue, purple, pink, gold), the sky dims and holds its breath, the golden one lands at 3.38 s, a gold pillar, rings and confetti, and the bars show. Pivot stays 3.4 s. Never full screen (a patch of night sky round the island) and no game-preview window.
- Meteors are big: head 4.8 / 8.5 / 11 (RNG big) canvas units times sz, a 22-segment trail with an outer glow, a core and a white centre; impacts scale 0.95 / 1.7 / 2.3.
- Slayer sounds remade for the meteor scene (tools/make_slayer_sounds.py; removes every older slr_/sl_ sound): rise (Grave / Summon / Portal), approach (Stomp / Drumline / Heartbeat), No Drops strike (Thunderbolt / Meteor / Falling star) + dust (Ash / Crumble / Wind) + none; a drop barrage (Bombardment / Rain of fire / Arcade) + reveal + close; RNG shower (Gacha roll / Star shower / Slot machine) + jackpot (Jackpot / Choir / Level up) + glow (Warm glow / Halo / Music box). The strike sounds are scored so their impacts land on SlayerScene.HITS; AnimationManager.cueTimes reads RISE / WALK / HITS so they stay in step.
- Settings carry over: a Slayer sound pick from an older set becomes the same take (a/b/c) of the slot's new family (SoundFx.chosen, saved back). Music keys bound under the old key.fancydrops.* names are copied to key.stardrops.* in options.txt before the game reads it (only where the new key is unbound).
- Lyrics: each word sits a little above or below the line (a fixed offset per word plus a slow sway) and pops in with a small scale overshoot. Now Playing and the song intro draw dots, stars and the worlds at sub-pixel positions so they glide instead of stepping.
- Standing rule (author): no update may ship with the light sky flicker. `/sd flicker` steps through switching off the mod's parts one by one (auras, name tags, HUD, slide-ins, tick work, everything) so the part that causes it can be found and fixed.
- Stardrops icon is a shooting star (gold five-point star, pink-to-violet tail of shrinking balls, sparkles). The Resources page hero uses the resources icon.
- Round "3D slayer box" (author: "make the animation 3d like the video ... do the box please! Make sure all the background is cool as well! add some depth to it"): Slayer3D draws the meteor scene in real 3D with the game's own models, as a picture-in-picture (Fabric PictureInPictureRendererRegistry) in a box round the stage, never the whole screen. Our own perspective camera (40 deg, reversed depth like the game) renders into the box's texture; the crater is pinned to where the item rises (Ritual passes craterY), and the texture is blitted with the scene's fade. The item, words and bars stay Ritual's 2D layer on top. If the 3D scene is unavailable or ever throws, the pixel scene (SlayerScene) plays instead.
  - Island: real blocks (block items, centred by their model box), radius 3.6, in the boss's ground (Revenant grass/dirt/stone with coarse dirt and podzol; Tarantula coarse dirt/deepslate; Sven grass/moss; Voidgloom end stone/obsidian; Inferno netherrack/basalt/magma; Riftstalker crimson nylium/blackstone). Props: a mossy headstone with a cross and a dirt mound; cobwebs; a mossy den; a ring of end portal frames; a magma lava pool; crying obsidian round a red rift.
  - Bosses are the real mobs: Revenant a zombie in a diamond chestplate and leggings, Tarantula a spider, Sven a wolf, Voidgloom an enderman, Inferno a blaze, Riftstalker a vindicator with an axe. They arrive their own way (rise from the ground, drop on a thread, bound out of the den, flicker in, rise from lava, step out of the rift), walk toward you with their real walk animation, flash red and get knocked back on every hit. No drops: the death fall-over, then they sink into ash. A drop / RNG: they burst into cubes of their own colours.
  - Meteors: a spinning magma block (gold for RNG's big one) in a burning glow with a 23-step trail (rainbow for RNG's big one), sparks, its light growing on the ground; impacts: white flash, shock rings, ground blocks thrown up, embers, a dark crater whose rim cools, smoke.
  - Depth, far to near: a soft elliptical backdrop (the window has no hard edge), a starfield with twinkles and glints, nebula clouds, a shaded ringed planet, shooting stars, haze at two depths, six distant floating islands that bob, the island, then dust and soft bokeh drifting in front of the lens. The camera swoops in, dollies in as the boss walks, leans in before RNG's big one and shakes on hits, so the layers move against each other. Everything fades toward the window's soft edge (glows per vertex, small blocks shrink away).
  - After the pivot: a drop gets a beam of its colour with sparkles and a ground ring; RNG gets a pulsing gold pillar, rising rings, orbiting glints, god rays and confetti.
- Round "like Butcher's, smaller" (author: the real-model 3D was "hella ugly"; "make it like butcher's ... but smaller in the screen"): the 3D scene is now our own chunky geometry (Vox3D), not game models. Every object is boxes with pixel-textured faces, lit by a soft bluish moonlight, wrapped in a black outline (an inside-out shell, so only the rim shows), Butcher's cartoon look. Slayer3D only hands Vox3D's meshes to the GPU (solid, sky, veil, glow pipelines); the bench (VoxShot) draws the same triangles in software, so every change is previewed before shipping.
  - The window is smaller: 260 x 180 canvas units round the stage (was 370 x 224). Its night sky (per-boss gradient, pixel moon top-left, stars, glints) fades to nothing before the window's edge, and every glow and veil fades per vertex (big glows in rings) so nothing shows a hard rectangle.
  - Dioramas per boss on a floating island (oval 4.6 x 3.4, shallow cliff): Revenant a graveyard (a big mossy R.I.P. headstone, the grave mound it bursts out of, small graves, wooden crosses, rain); Tarantula a grey den with cobwebs and rocks; Sven a grass clearing with oak trees, a stone den, light rain; Voidgloom end stone with obsidian pillars, purple crystals and a ring of portal frames; Inferno netherrack with nether-brick pillars topped by fire and a lava pool, embers; Riftstalker purple ground, crying obsidian and a red rift.
  - Mobs at Minecraft model proportions, built from their parts: the zombie in diamond chestplate and leggings (arms reaching forward), spider, wolf (red eyes), enderman, blaze (three rings of rods), vampire (red cape). Walk cycles swing the limbs; hits flash them pale and knock them back; a drop / RNG shatters them into their own spinning parts; no drops topples them and they sink, greying, into ash.
  - Meteors: an outlined spinning rock cube with glowing cracks (gold for RNG's big one) in a fiery trail, with sparks; impacts throw outlined chunks of the island's own ground.
  - Camera: 30 deg, from about 19.5 out to 16 as the boss walks, a lean in before RNG's big one, a kick on hits; the crater stays pinned under the item.
- Mobs made cooler (author: "make the mob look cooler"): bosses are bigger (Revenant 1.5x, Tarantula 1.8x, Sven 2.1x, Voidgloom 1.2x, Inferno 1.5x, Riftstalker 1.45x), shaded darker toward the bottom of each part with a lit top edge, and rim-lit in their energy colour where they turn away. Eyes glow (skin pixels tagged 0x02 are unshaded and get a glow): Revenant red, Tarantula red, Sven red, Voidgloom purple, Inferno white-hot, Riftstalker red. The Revenant is a hunched rotting zombie (mottled flesh, bone at the wrists, sockets, a torn grin) in diamond chestplate, shoulder pads and leggings with lit edges and sparkles, breathing and reaching. Tarantula is black with a glowing red mark on its back; Riftstalker has a high red collar and a swaying cape. Every boss has a presence: a shadow, dark smoke at its feet, wisps of its energy spiralling up, and a backlight just behind it.
- Smoother movement (author: "make the movement more smooth"): the walk ramps in and out (legs swing up from still, with a small step bob) instead of switching on; the boss turns toward its path and back to you with eased, shortest-way blends instead of snapping; knockback is a quick eased push and a soft return; the Revenant rises with an ease-in-out and a little lift; camera shake is made of smooth sines instead of stepped noise. Faster too: the island and still scenery are built once per boss (pass 1) and only moving parts are rebuilt each frame (pass 2, dyn), and runs of same-coloured pixels become one quad (about 1 ms a frame).
- Round "change the entire animation" (author picked Butcher's cutscenes; mob, island, meteors, camera all "funky"): Cut3D replaces the meteor scene. A close little night scene per slayer, no mob, no island, no meteors: a tool strikes a themed object on every beat of SlayerScene.HITS until it opens at the pivot and the drop rises out (PIN, pinned under the item). Revenant: a shovel digs the mound in front of a mossy R.I.P. headstone (graves, crosses, a lantern, rain), the coffin's lid flips open. Tarantula: a sword slashes a silk cocoon hanging in a web (bright slash arcs, it swings), it splits in two. Sven: a pickaxe knocks a cairn of stones apart from the top, wolves' eyes blink between two trees. Voidgloom: an eye of ender flies into one of eight portal frames per strike (the last fills the rest), the portal opens into a starry void. Inferno: a blaze rod hammers a magma core on a brazier between two fire pillars, it glows hotter with each strike and erupts. Riftstalker: a mallet breaks the three chains round a standing coffin, its door bursts open and bats fly out. No drops: one strike, it opens on nothing, a grey puff of dust. RNG: strike sparks climb the rarity colours, the sky dims before the gold strike, gold pillar, rings, glints, rays, confetti.
  - The tool arrives at RISE, winds up at WALK, snaps down onto each hit (0.09 s) and springs back (0.16 s), and leaves after the last. The camera is close (FOV 34, about 9 to 7.6 out), drifts, kicks on strikes, leans in before RNG's gold strike. The ground is a soft translucent disc of the theme's blocks that fades into the dark, so the scene has no hard edge; the sky is drawn against the pin's shift so the window stays centred. Sound slots keep their families; their names now read Arrive, Wind up, Strike(s)/Gacha strikes, Reveal/Jackpot/Dust, after.
- Meteors, not tools (author: "i want it to be a meteor strike instead of slices"): the cutscenes keep their scenes, but every beat is a meteor (Cut3D.meteors): a spinning outlined rock (gold for RNG's last) with a long burning trail from the upper right, landing on hits[i] on that scene's target (the grave, the cocoon, the top stone of the cairn, the next portal frame, the magma core, the next chain), with a shock ring, a flash and smoke; the target's own pieces fly off (burst). The tools and the sword's slash arcs are gone. Sound slot names: Arrive, Build up, Strike / Meteors / Meteor shower, then the reveal.
