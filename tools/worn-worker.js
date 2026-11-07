// FancyDrops server: a Cloudflare Worker with one KV namespace bound as WORN.
//
// Everything a player earns (drops counted, achievements, the cosmetics they unlock) lives HERE, not in the
// player's own files, so editing a file on your computer cannot give you anything.
// Every write is tied to a real Minecraft account: the mod first calls Mojang's joinServer with a random
// serverId, and this server asks Mojang (hasJoined) which account that was.
//
//   GET  /worn                 every player's worn cosmetics      { code: { s, f, a, t } }
//   POST /worn                 { name, serverId, suffix, fx, aura }   (only cosmetics you own are accepted)
//   POST /event                { name, serverId, events: [ { g, src, id, item, r, rng, v, n } ] }   drops you found
//   GET  /profile/<code>       stats, every achievement with progress, and the cosmetics you have unlocked
//
// A player's code is the SHA-256 of their lowercase dashed UUID (Cosmetics.code() in the mod).

const CORS = { "Access-Control-Allow-Origin": "*" };
const COSMETICS_JSON = "https://raw.githubusercontent.com/imtraminecraft/fancydrops/main/cosmetics.json";
const OWNER = "7ad5d2b31b239f2ed148d1de18f8214967bf7d1a411c838d466e9829ab7c2bf8";    // the mod owner (Cosmetics.OWNER): owns every cosmetic; more admins can be listed as "admins" in cosmetics.json

// ---------------------------------------------------------------- achievements (the only place they are defined)
// stat(p) reads the player's stats; goal is the number to reach. reward: the cosmetic it unlocks.
// ACH-START (written by tools/make_achievements.py)
const TABLE = {"chapters": {"ledger": "Every Drop", "time": "Playtime", "myth": "Mythological", "mob": "Mob Drops", "mine": "Mining", "farm": "Farming", "fish": "Fishing", "dye": "Table & Dyes"}, "list": [{"id": "first_find", "ch": "ledger", "tier": 0, "name": "First Find", "desc": "Find your first drop with FancyDrops on.", "icon": "seal", "goal": 1, "stat": "drops", "reward": "", "secret": false, "first": false}, {"id": "town_crier", "ch": "ledger", "tier": 0, "name": "Starting Out", "desc": "Find 50 drops with FancyDrops on.", "icon": "bell", "goal": 50, "stat": "drops", "reward": "bell", "secret": false, "first": false}, {"id": "curator", "ch": "ledger", "tier": 0, "name": "Curator", "desc": "Find 25 different drops with FancyDrops on.", "icon": "shelf", "goal": 25, "stat": "uniq", "reward": "", "secret": false, "first": false}, {"id": "rngesus", "ch": "ledger", "tier": 0, "name": "First RNG", "desc": "Find your first RNG drop (worth 10M coins or more).", "icon": "dice", "goal": 1, "stat": "rng", "reward": "", "secret": false, "first": false}, {"id": "prayed", "ch": "ledger", "tier": 0, "name": "Answered Prayer", "desc": "Find your first Pray RNGesus drop.", "icon": "star", "goal": 1, "stat": "r3", "reward": "", "secret": false, "first": false}, {"id": "millionaire", "ch": "ledger", "tier": 0, "name": "Millionaire", "desc": "Find 25M coins worth of drops.", "icon": "coin", "goal": 25000000, "stat": "value", "reward": "", "secret": false, "first": false}, {"id": "collector", "ch": "ledger", "tier": 2, "name": "Collector", "desc": "Find 100 drops with FancyDrops on.", "icon": "sack", "goal": 100, "stat": "drops", "reward": "", "secret": false, "first": false}, {"id": "archivist", "ch": "ledger", "tier": 2, "name": "Archivist", "desc": "Find 100 different drops with FancyDrops on.", "icon": "shelf", "goal": 100, "stat": "uniq", "reward": "", "secret": false, "first": false}, {"id": "frequent", "ch": "ledger", "tier": 2, "name": "Frequent Finder", "desc": "Find 10 RNG drops.", "icon": "dice", "goal": 10, "stat": "rng", "reward": "", "secret": false, "first": false}, {"id": "devout", "ch": "ledger", "tier": 2, "name": "Devout", "desc": "Find 3 Pray RNGesus drops.", "icon": "star", "goal": 3, "stat": "r3", "reward": "", "secret": false, "first": false}, {"id": "multimillionaire", "ch": "ledger", "tier": 2, "name": "Multimillionaire", "desc": "Find 250M coins worth of drops.", "icon": "coin", "goal": 250000000, "stat": "value", "reward": "", "secret": false, "first": false}, {"id": "fast_start", "ch": "ledger", "tier": 2, "name": "Fast Starter", "desc": "Find 20 drops on your first day with FancyDrops.", "icon": "clock", "goal": 20, "stat": "fd", "reward": "", "secret": false, "first": true}, {"id": "hoarder", "ch": "ledger", "tier": 3, "name": "Hoarder", "desc": "Find 500 drops with FancyDrops on.", "icon": "sack", "goal": 500, "stat": "drops", "reward": "sweep", "secret": false, "first": false}, {"id": "rng_magnet", "ch": "ledger", "tier": 3, "name": "RNG Magnet", "desc": "Find 100 RNG drops.", "icon": "crown", "goal": 100, "stat": "rng", "reward": "ripple", "secret": false, "first": false}, {"id": "blessed", "ch": "ledger", "tier": 3, "name": "Blessed", "desc": "Find 50 RNG drops.", "icon": "dice", "goal": 50, "stat": "rng", "reward": "dice", "secret": false, "first": false}, {"id": "pray", "ch": "ledger", "tier": 3, "name": "Chosen by RNGesus", "desc": "Find 10 Pray RNGesus drops.", "icon": "star", "goal": 10, "stat": "r3", "reward": "coinaura", "secret": false, "first": false}, {"id": "tycoon", "ch": "ledger", "tier": 3, "name": "Billionaire", "desc": "Find 1B coins worth of drops.", "icon": "coin", "goal": 1000000000, "stat": "value", "reward": "neon", "secret": false, "first": false}, {"id": "prodigy", "ch": "ledger", "tier": 3, "name": "Prodigy", "desc": "Find 50 drops on your first day with FancyDrops.", "icon": "clock", "goal": 50, "stat": "fd", "reward": "", "secret": false, "first": true}, {"id": "jackpot", "ch": "ledger", "tier": 3, "name": "Jackpot", "desc": "One drop worth 100M coins or more.", "icon": "gem", "goal": 1, "stat": "best100", "reward": "", "secret": true, "first": false}, {"id": "regular", "ch": "ledger", "tier": 2, "name": "Week of Luck", "desc": "Find drops on 7 different days.", "icon": "clock", "goal": 7, "stat": "days", "reward": "", "secret": false, "first": false}, {"id": "devoted", "ch": "ledger", "tier": 3, "name": "Devoted", "desc": "Find drops on 30 different days.", "icon": "clock", "goal": 30, "stat": "days", "reward": "", "secret": false, "first": false}, {"id": "time_1h", "ch": "time", "tier": 0, "name": "First Hour", "desc": "Play for an hour with FancyDrops on.", "icon": "clock", "goal": 60, "stat": "pt", "reward": "", "secret": false, "first": false}, {"id": "time_5h", "ch": "time", "tier": 0, "name": "Settling In", "desc": "Play for 5 hours with FancyDrops on.", "icon": "clock", "goal": 300, "stat": "pt", "reward": "", "secret": false, "first": false}, {"id": "fresh_face", "ch": "time", "tier": 0, "name": "Fresh Face", "desc": "Find 5 drops on your first day with FancyDrops.", "icon": "seal", "goal": 5, "stat": "fd", "reward": "", "secret": false, "first": true}, {"id": "time_10h", "ch": "time", "tier": 0, "name": "Regular", "desc": "Play for 10 hours with FancyDrops on.", "icon": "clock", "goal": 600, "stat": "pt", "reward": "", "secret": false, "first": false}, {"id": "time_24h", "ch": "time", "tier": 1, "name": "Full Day", "desc": "Play for 24 hours with FancyDrops on.", "icon": "clock", "goal": 1440, "stat": "pt", "reward": "", "secret": false, "first": false}, {"id": "quick_learner", "ch": "time", "tier": 1, "name": "Quick Learner", "desc": "Find 25 drops on your first day with FancyDrops.", "icon": "seal", "goal": 25, "stat": "fd", "reward": "", "secret": false, "first": true}, {"id": "beginners_luck", "ch": "time", "tier": 1, "name": "Beginner's Luck", "desc": "Find an RNG drop on your first day with FancyDrops.", "icon": "dice", "goal": 1, "stat": "fdr", "reward": "", "secret": false, "first": true}, {"id": "natural", "ch": "time", "tier": 3, "name": "Natural", "desc": "Find 50 drops on your first day with FancyDrops.", "icon": "seal", "goal": 50, "stat": "fd", "reward": "", "secret": false, "first": true}, {"id": "born_lucky", "ch": "time", "tier": 3, "name": "Born Lucky", "desc": "Find 5 RNG drops on your first day with FancyDrops.", "icon": "dice", "goal": 5, "stat": "fdr", "reward": "", "secret": false, "first": true}, {"id": "time_50h", "ch": "time", "tier": 3, "name": "Dedicated", "desc": "Play for 50 hours with FancyDrops on.", "icon": "clock", "goal": 3000, "stat": "pt", "reward": "", "secret": false, "first": false}, {"id": "time_100h", "ch": "time", "tier": 3, "name": "Centurion", "desc": "Play for 100 hours with FancyDrops on.", "icon": "clock", "goal": 6000, "stat": "pt", "reward": "clock", "secret": false, "first": false}, {"id": "first_myth", "ch": "myth", "tier": 0, "name": "First Contact", "desc": "Find your first drop from a Mythological creature.", "icon": "horn", "goal": 1, "stat": "g:myth", "reward": "", "secret": false, "first": false}, {"id": "first_dig", "ch": "myth", "tier": 0, "name": "First Dig", "desc": "Find your first treasure from a Mythological burrow.", "icon": "shovel", "goal": 1, "stat": "g:treasure", "reward": "", "secret": false, "first": false}, {"id": "minos_bane", "ch": "myth", "tier": 0, "name": "Minos' Bane", "desc": "Find 10 drops from Mythological creatures.", "icon": "horn", "goal": 10, "stat": "g:myth", "reward": "", "secret": false, "first": false}, {"id": "digger", "ch": "myth", "tier": 0, "name": "Beginner Digger", "desc": "Find 25 treasures from Mythological burrows.", "icon": "shovel", "goal": 25, "stat": "g:treasure", "reward": "", "secret": false, "first": false}, {"id": "myth_hunter", "ch": "myth", "tier": 2, "name": "Mythological Hunter", "desc": "Find 100 drops from Mythological creatures.", "icon": "horn", "goal": 100, "stat": "g:myth", "reward": "", "secret": false, "first": false}, {"id": "adv_digger", "ch": "myth", "tier": 2, "name": "Advanced Digger", "desc": "Find 100 treasures from Mythological burrows.", "icon": "shovel", "goal": 100, "stat": "g:treasure", "reward": "", "secret": false, "first": false}, {"id": "griffins_favour", "ch": "myth", "tier": 2, "name": "Griffin's Favour", "desc": "Find 5 RNG drops in the Mythological Ritual (creatures or treasure).", "icon": "dice", "goal": 5, "stat": "gr:myth+treasure", "reward": "", "secret": false, "first": false}, {"id": "mythbreaker", "ch": "myth", "tier": 3, "name": "Mythbreaker", "desc": "Find 250 drops from Mythological creatures.", "icon": "horn", "goal": 250, "stat": "g:myth", "reward": "", "secret": false, "first": false}, {"id": "deep_digger", "ch": "myth", "tier": 3, "name": "Deep Digger", "desc": "Find 250 treasures from Mythological burrows.", "icon": "shovel", "goal": 250, "stat": "g:treasure", "reward": "", "secret": false, "first": false}, {"id": "dianas_chosen", "ch": "myth", "tier": 3, "name": "Diana's Chosen", "desc": "Find 10 RNG drops in the Mythological Ritual (creatures or treasure).", "icon": "dice", "goal": 10, "stat": "gr:myth+treasure", "reward": "spark", "secret": false, "first": false}, {"id": "golden_burrows", "ch": "myth", "tier": 3, "name": "Golden Burrows", "desc": "Find 100M coins worth of drops in the Mythological Ritual.", "icon": "coin", "goal": 100000000, "stat": "gv:myth+treasure", "reward": "", "secret": false, "first": false}, {"id": "first_blood", "ch": "mob", "tier": 0, "name": "First Blood", "desc": "Find your first slayer drop.", "icon": "sword", "goal": 1, "stat": "g:slayer", "reward": "", "secret": false, "first": false}, {"id": "isle_visitor", "ch": "mob", "tier": 0, "name": "Isle Visitor", "desc": "Find your first drop on the Crimson Isle.", "icon": "flame", "goal": 1, "stat": "g:crimson", "reward": "", "secret": false, "first": false}, {"id": "dragon_hunter", "ch": "mob", "tier": 0, "name": "Dragon Hunter", "desc": "Find your first drop from an Ender Dragon.", "icon": "skull", "goal": 1, "stat": "g:dragon", "reward": "", "secret": false, "first": false}, {"id": "underground", "ch": "mob", "tier": 0, "name": "Underground", "desc": "Find your first drop from a creature of the Mines.", "icon": "skull", "goal": 1, "stat": "g:mobs", "reward": "", "secret": false, "first": false}, {"id": "executioner", "ch": "mob", "tier": 2, "name": "Executioner", "desc": "Find 25 slayer drops.", "icon": "anvil", "goal": 25, "stat": "g:slayer", "reward": "anvil", "secret": false, "first": false}, {"id": "all_bosses", "ch": "mob", "tier": 2, "name": "Monster Hunter", "desc": "Find a drop from 4 different slayer bosses.", "icon": "sword", "goal": 4, "stat": "bosses", "reward": "", "secret": false, "first": false}, {"id": "isle_raider", "ch": "mob", "tier": 2, "name": "Isle Raider", "desc": "Find 25 drops on the Crimson Isle.", "icon": "flame", "goal": 25, "stat": "g:crimson", "reward": "", "secret": false, "first": false}, {"id": "dragon_slayer", "ch": "mob", "tier": 2, "name": "Dragon Slayer", "desc": "Find 10 drops from Ender Dragons.", "icon": "skull", "goal": 10, "stat": "g:dragon", "reward": "", "secret": false, "first": false}, {"id": "rare_breed", "ch": "mob", "tier": 2, "name": "Rare Breed", "desc": "Find 25 drops from the creatures of the Mines.", "icon": "skull", "goal": 25, "stat": "g:mobs", "reward": "", "secret": false, "first": false}, {"id": "boss_breaker", "ch": "mob", "tier": 3, "name": "Boss Breaker", "desc": "Find 10 RNG slayer drops.", "icon": "skull", "goal": 10, "stat": "gr:slayer", "reward": "", "secret": false, "first": false}, {"id": "slayer_supreme", "ch": "mob", "tier": 3, "name": "Slayer Supreme", "desc": "Find 250 slayer drops.", "icon": "sword", "goal": 250, "stat": "g:slayer", "reward": "", "secret": false, "first": false}, {"id": "warlord", "ch": "mob", "tier": 3, "name": "Warlord of the Isle", "desc": "Find 100 drops on the Crimson Isle.", "icon": "flame", "goal": 100, "stat": "g:crimson", "reward": "", "secret": false, "first": false}, {"id": "dragonbane", "ch": "mob", "tier": 3, "name": "Dragonbane", "desc": "Find 50 drops from Ender Dragons.", "icon": "skull", "goal": 50, "stat": "g:dragon", "reward": "", "secret": false, "first": false}, {"id": "mines_menace", "ch": "mob", "tier": 3, "name": "Menace of the Mines", "desc": "Find 100 drops from the creatures of the Mines.", "icon": "skull", "goal": 100, "stat": "g:mobs", "reward": "", "secret": false, "first": false}, {"id": "first_bundle", "ch": "mine", "tier": 0, "name": "First Bundle", "desc": "Find your first rare Crystal Nucleus reward.", "icon": "gem", "goal": 1, "stat": "g:nucleus", "reward": "", "secret": false, "first": false}, {"id": "cold_case", "ch": "mine", "tier": 0, "name": "Cold Case", "desc": "Find your first rare drop in a Frozen Corpse.", "icon": "skull", "goal": 1, "stat": "g:corpse", "reward": "", "secret": false, "first": false}, {"id": "fiesta_goer", "ch": "mine", "tier": 0, "name": "Fiesta Goer", "desc": "Find your first rare drop during a Mining Fiesta.", "icon": "gem", "goal": 1, "stat": "g:fiesta", "reward": "", "secret": false, "first": false}, {"id": "nucleus", "ch": "mine", "tier": 2, "name": "Heart of the Mountain", "desc": "Find 10 rare Crystal Nucleus rewards.", "icon": "gem", "goal": 10, "stat": "g:nucleus", "reward": "", "secret": false, "first": false}, {"id": "grave_robber", "ch": "mine", "tier": 2, "name": "Grave Robber", "desc": "Find 25 rare drops in Frozen Corpses.", "icon": "skull", "goal": 25, "stat": "g:corpse", "reward": "", "secret": false, "first": false}, {"id": "fiesta_regular", "ch": "mine", "tier": 2, "name": "Fiesta Regular", "desc": "Find 10 rare drops during Mining Fiestas.", "icon": "gem", "goal": 10, "stat": "g:fiesta", "reward": "", "secret": false, "first": false}, {"id": "divans_heir", "ch": "mine", "tier": 3, "name": "Divan's Heir", "desc": "Find 100 rare Crystal Nucleus rewards.", "icon": "gem", "goal": 100, "stat": "g:nucleus", "reward": "chest", "secret": false, "first": false}, {"id": "frozen_fortune", "ch": "mine", "tier": 3, "name": "Frozen Fortune", "desc": "Find 100 rare drops in Frozen Corpses.", "icon": "skull", "goal": 100, "stat": "g:corpse", "reward": "", "secret": false, "first": false}, {"id": "mining_magnate", "ch": "mine", "tier": 3, "name": "Mining Magnate", "desc": "Find 100M coins worth of Mining drops.", "icon": "coin", "goal": 100000000, "stat": "gv:nucleus+corpse+fiesta", "reward": "", "secret": false, "first": false}, {"id": "first_harvest", "ch": "farm", "tier": 0, "name": "First Harvest", "desc": "Find your first rare crop.", "icon": "wheat", "goal": 1, "stat": "g:crops", "reward": "", "secret": false, "first": false}, {"id": "crop_picker", "ch": "farm", "tier": 0, "name": "Crop Picker", "desc": "Find 25 rare crops.", "icon": "wheat", "goal": 25, "stat": "g:crops", "reward": "", "secret": false, "first": false}, {"id": "feast_guest", "ch": "farm", "tier": 0, "name": "Feast Guest", "desc": "Find your first Harvest Feast drop.", "icon": "wheat", "goal": 1, "stat": "g:feast", "reward": "", "secret": false, "first": false}, {"id": "feast", "ch": "farm", "tier": 0, "name": "Feast", "desc": "Find 25 Harvest Feast drops.", "icon": "wheat", "goal": 25, "stat": "g:feast", "reward": "", "secret": false, "first": false}, {"id": "bounty_seeker", "ch": "farm", "tier": 0, "name": "Bounty Seeker", "desc": "Find your first Harvest Bounty.", "icon": "leaf", "goal": 1, "stat": "g:bounty", "reward": "", "secret": false, "first": false}, {"id": "greenhouse", "ch": "farm", "tier": 0, "name": "Greenhouse Keeper", "desc": "Find 10 Harvest Bounties.", "icon": "leaf", "goal": 10, "stat": "g:bounty", "reward": "", "secret": false, "first": false}, {"id": "pest_control", "ch": "farm", "tier": 0, "name": "Pest Control", "desc": "Find your first pest drop.", "icon": "bug", "goal": 1, "stat": "g:pests", "reward": "", "secret": false, "first": false}, {"id": "exterminator", "ch": "farm", "tier": 0, "name": "Exterminator", "desc": "Find 50 pest drops.", "icon": "bug", "goal": 50, "stat": "g:pests", "reward": "fireflyaura", "secret": false, "first": false}, {"id": "crate_digger", "ch": "farm", "tier": 2, "name": "Crate Digger", "desc": "Find 5 different Vinyls.", "icon": "disc", "goal": 5, "stat": "vinyl", "reward": "disc", "secret": false, "first": false}, {"id": "green_thumb", "ch": "farm", "tier": 3, "name": "Golden Fields", "desc": "Find 250 rare crops.", "icon": "wheat", "goal": 250, "stat": "g:crops", "reward": "tractor", "secret": false, "first": false}, {"id": "feast_master", "ch": "farm", "tier": 3, "name": "Feast Master", "desc": "Find 250 Harvest Feast drops.", "icon": "wheat", "goal": 250, "stat": "g:feast", "reward": "", "secret": false, "first": false}, {"id": "bounty_king", "ch": "farm", "tier": 3, "name": "Bounty King", "desc": "Find 100 Harvest Bounties.", "icon": "leaf", "goal": 100, "stat": "g:bounty", "reward": "", "secret": false, "first": false}, {"id": "plague_doctor", "ch": "farm", "tier": 3, "name": "Plague Doctor", "desc": "Find 500 pest drops.", "icon": "bug", "goal": 500, "stat": "g:pests", "reward": "", "secret": false, "first": false}, {"id": "first_catch", "ch": "fish", "tier": 0, "name": "First Catch", "desc": "Find your first drop from a sea creature.", "icon": "fish", "goal": 1, "stat": "g:fish_water+fish_lava+fish_spooky+fish_winter", "reward": "", "secret": false, "first": false}, {"id": "sunken_treasure", "ch": "fish", "tier": 0, "name": "Sunken Treasure", "desc": "Pull up your first Great or Outstanding Catch.", "icon": "chest", "goal": 1, "stat": "g:fish_treasure", "reward": "", "secret": false, "first": false}, {"id": "angler", "ch": "fish", "tier": 0, "name": "Angler", "desc": "Find 10 drops from sea creatures.", "icon": "fish", "goal": 10, "stat": "g:fish_water+fish_lava+fish_spooky+fish_winter", "reward": "bubbleaura", "secret": false, "first": false}, {"id": "sea_hunter", "ch": "fish", "tier": 2, "name": "Sea Hunter", "desc": "Find 100 drops from sea creatures.", "icon": "fish", "goal": 100, "stat": "g:fish_water+fish_lava+fish_spooky+fish_winter", "reward": "", "secret": false, "first": false}, {"id": "treasure_diver", "ch": "fish", "tier": 2, "name": "Treasure Diver", "desc": "Pull up 25 Great or Outstanding Catches.", "icon": "chest", "goal": 25, "stat": "g:fish_treasure", "reward": "", "secret": false, "first": false}, {"id": "deep_sea", "ch": "fish", "tier": 2, "name": "Deep Sea Legend", "desc": "Find 3 RNG drops from sea creatures.", "icon": "fish", "goal": 3, "stat": "gr:fish_water+fish_lava+fish_spooky+fish_winter", "reward": "", "secret": false, "first": false}, {"id": "leviathan", "ch": "fish", "tier": 3, "name": "Leviathan", "desc": "Find 500 drops from sea creatures.", "icon": "fish", "goal": 500, "stat": "g:fish_water+fish_lava+fish_spooky+fish_winter", "reward": "", "secret": false, "first": false}, {"id": "davy_jones", "ch": "fish", "tier": 3, "name": "Davy Jones", "desc": "Pull up 100 Great or Outstanding Catches.", "icon": "chest", "goal": 100, "stat": "g:fish_treasure", "reward": "", "secret": false, "first": false}, {"id": "abyssal_fortune", "ch": "fish", "tier": 3, "name": "Abyssal Fortune", "desc": "Find 10 RNG drops while fishing.", "icon": "fish", "goal": 10, "stat": "gr:fish_water+fish_lava+fish_spooky+fish_winter+fish_treasure", "reward": "", "secret": false, "first": false}, {"id": "living_colour", "ch": "dye", "tier": 0, "name": "Living Colour", "desc": "Find a dye.", "icon": "dye", "goal": 1, "stat": "g:dyes", "reward": "", "secret": true, "first": false}, {"id": "palette", "ch": "dye", "tier": 2, "name": "Palette", "desc": "Find 3 different dyes.", "icon": "dye", "goal": 3, "stat": "dyes", "reward": "", "secret": false, "first": false}, {"id": "rainbow", "ch": "dye", "tier": 3, "name": "Every Colour", "desc": "Find 10 different dyes.", "icon": "dye", "goal": 10, "stat": "dyes", "reward": "", "secret": false, "first": false}, {"id": "scholar", "ch": "dye", "tier": 0, "name": "Scholar", "desc": "Find 10 Experimentation Table rewards.", "icon": "book", "goal": 10, "stat": "g:exp", "reward": "", "secret": false, "first": false}, {"id": "mastermind", "ch": "dye", "tier": 3, "name": "Mastermind", "desc": "Find 5 RNG Experimentation Table rewards.", "icon": "book", "goal": 5, "stat": "gr:exp", "reward": "runeaura", "secret": false, "first": false}]};
// ACH-END
const CH = TABLE.chapters, ACH = TABLE.list;
/** A star's number: see tools/achievements_src.py for the words. RNG = a drop worth 10M coins or more. */
function stat(p, s) {
  const m = /^(g|gr|gv):(.+)$/.exec(s);
  if (m) return m[2].split("+").reduce((a, k) => { const v = p.g[k] || {}; return a + (m[1] === "g" ? v.d || 0 : m[1] === "gr" ? v.rng || 0 : v.v || 0); }, 0);
  switch (s) {
    case "drops": return p.drops; case "rng": return p.rng; case "r3": return p.r3; case "value": return p.value; case "best": return p.best;
    case "best100": return p.best >= 100_000_000 ? 1 : 0;
    case "uniq": return Object.keys(p.uniq).length; case "days": return Object.keys(p.days).length; case "bosses": return Object.keys(p.bosses).length;
    case "vinyl": return Object.keys(p.vinyl).length; case "dyes": return Object.keys(p.dyes || {}).length;
    case "fd": return p.fd || 0; case "fdr": return p.fdr || 0; case "pt": return p.pt || 0;
  }
  return 0;
}

// the Special and Beta sets, by grant key in cosmetics.json
const SETS = {
  sparkle: ["sparkle", "sparklefx", "sparkleaura"],
  bugfinder: ["bug", "glitch", "glitchaura"],
  contributors: ["code", "contribfx", "stardust"],
  patreon: ["patron", "royal", "patronaura"],
};
const GRANT_KEY = { sparkle: "sparkle", patreon: "patreon", bughunter: "bugfinder", contributor: "contributors" };
const BETA = ["betamark", "pulse"];

// ---------------------------------------------------------------- helpers
const json = (o, extra = {}) => new Response(JSON.stringify(o), { headers: { "content-type": "application/json", ...CORS, ...extra } });
const blankProfile = () => ({ drops: 0, rng: 0, bigRng: 0, value: 0, best: 0, r3: 0, uniq: {}, days: {}, vinyl: {}, bosses: {}, dyes: {}, g: {}, unlocked: {}, recent: [], first: Date.now() });

async function codeOf(uuid) {
  const d = await crypto.subtle.digest("SHA-256", new TextEncoder().encode(uuid));
  return [...new Uint8Array(d)].map((x) => x.toString(16).padStart(2, "0")).join("");
}
/** Asks Mojang which account just called joinServer with this serverId. Returns its code, or null. */
async function verify(b) {
  const name = String(b.name || ""), serverId = String(b.serverId || "");
  if (!/^[A-Za-z0-9_]{1,16}$/.test(name) || !/^[0-9a-f]{8,64}$/.test(serverId)) { verify.why = "bad request"; return null; }
  // Mojang's session server is shared by every Cloudflare worker and often answers 429 (too many requests) or 204 for a moment:
  // ask again a few times before giving up, and say why in the answer so the game's log shows it.
  for (let i = 0; i < 5; i++) {
    const r = await fetch(`https://sessionserver.mojang.com/session/minecraft/hasJoined?username=${encodeURIComponent(name)}&serverId=${serverId}`, { headers: { "user-agent": "FancyDrops-worker" } });
    if (r.status === 200) {
      const id = (await r.json()).id;
      return codeOf(`${id.slice(0, 8)}-${id.slice(8, 12)}-${id.slice(12, 16)}-${id.slice(16, 20)}-${id.slice(20)}`.toLowerCase());
    }
    verify.why = "mojang " + r.status;
    await new Promise((res) => setTimeout(res, 300 + i * 400));
  }
  return null;
}
/** Cosmetics given by hand (cosmetics.json), cached for 5 minutes. */
async function grantsFor(env, code) {
  let raw = await env.WORN.get("cosmetics_cache", "json");
  if (!raw || Date.now() - raw.at > 300_000) {
    try { const r = await fetch(COSMETICS_JSON); raw = { at: Date.now(), body: await r.json() }; await env.WORN.put("cosmetics_cache", JSON.stringify(raw)); } catch { raw = raw || { at: 0, body: {} }; }
  }
  const out = new Set(BETA), body = raw.body || {};
  for (const [key, codes] of Object.entries(body.special || {})) if (Array.isArray(codes) && codes.map((c) => String(c).toLowerCase()).includes(code)) for (const c of SETS[GRANT_KEY[key]] || []) out.add(c);
  for (const c of (body.extra || {})[code] || []) out.add(c);
  const admins = new Set([OWNER, ...(body.admins || [])].map((x) => String(x).toLowerCase()));
  return { owned: out, admin: admins.has(code) };
}
async function load(env, code) { return (await env.WORN.get("p:" + code, "json")) || blankProfile(); }
function view(p) {
  const list = ACH.map((a) => {
    const v = Math.min(stat(p, a.stat), a.goal), done = !!p.unlocked[a.id];
    return { id: a.id, ch: a.ch, chapter: CH[a.ch], name: a.name, desc: a.desc, icon: a.icon, goal: a.goal, progress: v, done, at: p.unlocked[a.id] || 0, reward: a.reward || "", secret: !!a.secret, tier: a.tier, first: !!a.first };
  });
  const cosmetics = ACH.filter((a) => p.unlocked[a.id] && a.reward).map((a) => a.reward);
  const stats = { fd: p.fd || 0, fdr: p.fdr || 0, pt: p.pt || 0, drops: p.drops, rng: p.rng, bigRng: p.bigRng, value: p.value, best: p.best, r3: p.r3, unique: Object.keys(p.uniq).length, days: Object.keys(p.days).length, bosses: Object.keys(p.bosses).length, vinyls: Object.keys(p.vinyl).length, dyes: Object.keys(p.dyes || {}).length, groups: p.g, since: p.first };
  return { stats, achievements: list, cosmetics, chapters: CH };
}

// ---------------------------------------------------------------- the server
export default {
  async fetch(req, env) {
    const url = new URL(req.url), path = url.pathname;

    if (path === "/worn" && req.method === "GET") return json((await env.WORN.get("all", "json")) || {}, { "cache-control": "max-age=30" });

    if (path.startsWith("/profile/") && req.method === "GET") {
      const code = path.slice(9).toLowerCase();
      if (!/^[0-9a-f]{64}$/.test(code)) return new Response("bad code", { status: 400 });
      return json(view(await load(env, code)), { "cache-control": "max-age=15" });
    }

    if (req.method !== "POST" || (path !== "/worn" && path !== "/event")) return new Response("FancyDrops", { status: 404 });
    let b;
    try { b = await req.json(); } catch { return new Response("bad json", { status: 400 }); }
    const code = await verify(b);
    if (!code) return new Response("not verified (" + (verify.why || "?") + ")", { status: 403 });

    if (path === "/worn") {
      const p = await load(env, code), { owned, admin } = await grantsFor(env, code);
      for (const a of ACH) if (p.unlocked[a.id] && a.reward) owned.add(a.reward);
      const ok = (v) => (typeof v === "string" && /^[a-z0-9_]{0,32}$/.test(v) && (v === "" || admin || owned.has(v)) ? v : "");    // only what you own
      const entry = { s: ok(b.suffix), f: ok(b.fx), a: ok(b.aura), t: Date.now() };
      const all = (await env.WORN.get("all", "json")) || {};
      if (!entry.s && !entry.f && !entry.a) delete all[code]; else all[code] = entry;
      await env.WORN.put("all", JSON.stringify(all));
      return json({ ok: true, worn: entry });
    }

    // /event: drops you found
    const events = Array.isArray(b.events) ? b.events.slice(0, 40) : [];
    const p = await load(env, code), now = Date.now();
    p.recent = (p.recent || []).filter((t) => now - t < 3_600_000);
    const day = new Date(now).toISOString().slice(0, 10);
    let taken = 0;
    for (const e of events) {
      if (p.recent.length >= 400) break;                                                  // more than 400 drops an hour is not real play
      const grp = String(e.g || ""), id = String(e.id || "").slice(0, 64), item = String(e.item || "").slice(0, 64);
      if (!/^[a-z_]{2,16}$/.test(grp) || !id) continue;
      const v = Math.max(0, Math.min(Number(e.v) || 0, 5_000_000_000)), rng = v >= 10_000_000, r = Math.max(0, Math.min(3, Number(e.r) || 0));
      const pt = Math.max(0, Math.min(Number(e.pt) || 0, 10_000_000)); p.pt = Math.max(p.pt || 0, pt);
      p.drops++; p.recent.push(now); taken++;
      if (e.pt !== undefined && pt < 1440) { p.fd = (p.fd || 0) + 1; if (rng) p.fdr = (p.fdr || 0) + 1; }       // your first day of playing with FancyDrops
      if (rng) p.rng++;
      if (rng && v >= 1_000_000) p.bigRng++;
      if (r >= 3) p.r3++;
      p.value += v; p.best = Math.max(p.best, v);
      p.uniq[id] = 1; p.days[day] = 1;
      if (/vinyl/i.test(item)) p.vinyl[id] = 1;
      if (grp === "dyes") (p.dyes = p.dyes || {})[id] = 1;
      if (grp === "slayer" && e.src) p.bosses[String(e.src).slice(0, 24)] = 1;
      const gg = (p.g[grp] = p.g[grp] || { d: 0, rng: 0, v: 0 }); gg.d++; if (rng) gg.rng++; gg.v = (gg.v || 0) + v;
    }
    const fresh = [];
    for (const a of ACH) if (!p.unlocked[a.id] && stat(p, a.stat) >= a.goal) { p.unlocked[a.id] = now; fresh.push(a.id); }
    await env.WORN.put("p:" + code, JSON.stringify(p));
    return json({ ok: true, taken, unlocked: fresh, ...view(p) });
  },
};
