# MoreInformation

Calendar dates never rewind: setting an earlier time of day advances to tomorrow; a later or equal time stays on the same day. Natural rollover, sleep and multi-day additions advance the date.

Client-local records in `moreinformation/calendars/` are isolated by save or server address and dimension, even while the HUD is disabled. Initial dates come from world time; earlier or offline time-reset history cannot be reconstructed. Replacing a server world at the same address does not automatically reset its local record.

A client-side information HUD for Minecraft 1.21.1 NeoForge, version 1.0.3. By default it shows TPS / MSPT / RAM and player info. Extra lines and mod integrations can be enabled in the config.

[中文](README.md)

## Default display

- TPS, MSPT, RAM
- Player name, FPS, real-world clock
- Connection latency icon

## Optional vanilla info

- Minecraft calendar time and moon phase on one line, with English and Chinese names for all eight phases
- Dimension and loaded chunks
- Biome and weather
- Entity, hostile mob and rendered counts, in that order on one line
- Coordinates and facing, plus corresponding overworld/nether XYZ: only X/Z use the 8:1 conversion; Y retains the current height. Coordinates remain visible near the origin, and negative coordinates round down. These are not guaranteed portal destinations; other dimensions omit the conversion
- Light levels
- Status effects: icons with name and remaining time above the HUD, while the vanilla effect icons are hidden

## Config

Config file: `config/moreinformation-client.toml`

- `hud.position`: `top_left`, `middle_left`, `bottom_left`, `top_right`, `middle_right`, `bottom_right`
- `hud.offsetX` / `hud.offsetY`: pixel offset from the anchor
- `hud.scale`: HUD scale, default `1.0`, range `0.25` to `4.0`
  Oversized content is automatically reduced to fit the viewport.
- `hud.effectsIconScale`: status effect icon size, default `1.0`, range `0.5` to `3.0`
- `hud.templates`: per-line format. Use `{placeholders}` and `&` color codes (`&a` green, `&e` yellow, `&c` red, `&r` reset; `&&` for a literal `&`)
- Resetting an option restores the default format for the current language

Status effects start at the left edge of the HUD. Each entry places its icon beside two text rows: name and level, then remaining time. Container screens retain vanilla effects. Entity counts and hostile counts share one line; MC time and moon phase share another. Legacy showHostile and showMoon switches enable their respective combined lines.

- Enable effects with `hud.showEffects`. Up to three entries per row and two rows are shown; narrow screens use fewer columns. Overflow appears as `+N`; open the inventory for the full list.
- Harmful effects appear first, followed by a stable order. Long names are shortened while timers remain visible.
- Enabling effects reserves two rows and overflow space, even with no active effects or while a container is open. Information stays in place as effects change. Disabling `hud.showEffects` removes the reservation.
- At the default icon scale, the reserved height is 44 HUD logical pixels. It follows overall HUD scaling and adjusts with `hud.effectsIconScale`.
- Disabling the HUD or opening the debug view restores vanilla effect icons.
- Effect replacement now also makes client-side `showIcon()` queries for the local player's effects report invisible. Mods such as Xaero that respect icon visibility no longer reserve space for hidden icons; the Xaero-specific patch has been removed. Disabling replacement, F3, F1 and container screens immediately restore the original visibility.
- Active effects, original icon flags, particles and other entities are unchanged; vanilla save/network serialization retains the real flag. The HUD's own reserved effect area is unchanged. Mods that reserve space from the effect count alone, ignoring icon visibility, may still need individual compatibility fixes.
- Entity templates support `{entities}`, `{hostile}`, `{rendered}`; time templates support `{year}`, `{day}`, `{time}`, `{period}`, `{phase}`.

## Mod integrations

After installing the matching mod, turn on its switch under `integrations`. Missing mods will not crash the game.

| Mod | Option | What it shows |
| --- | --- | --- |
| Ecliptic Seasons | `showEclipticSeasons` | Season and solar term |
| TerraFirmaCraft | `showTerraFirmaCraft` | Rainfall and temperature |
| L2Hostility | `showL2Hostility` | Player / chunk / max hostility |
| ProjectE | `showProjectE` | Player EMC. Large values use units; hold Shift for the exact number |
| Nature's Compass | `showNaturesCompass` | Search status while locating a biome; hides the original HUD |
| Explorer's Compass | `showExplorersCompass` | Search status while locating a structure; hides the original overlay |
| ExplorersCompassEdited | `showExplorersCompass` | Same as above |
| Nature's Aura | `showNaturesAura` | Aura strength around the current position (same math as the Aura Eye) |
| Neo Vitae (1.21.1 Blood Magic reimagining) | `showNeoVitae` | Personal Essentia Vitae (approximate EV) |

Nature's Compass and Explorer's Compass are on by default. The other integrations are off by default. The master switch is `integrations.enabled`.

Neo Vitae APIs were checked against `1.21.1-1.1.51`. Only the synchronized client Anima cache is read. The icon is the Ara Vitae altar, and values display as `amount EV` without an approximation prefix: the host checks every 60 ticks and rounds to 500 EV. Nothing is shown before initial sync; reconnecting with the same UUID can briefly show the host's previous cached value. No server SavedData access or additional synchronization is used, and orb capacity/tier is not misrepresented as a player-wide network limit.

Providers registered by other mods through `HudInfoRegistry` are enabled by default, without additional provider ids in the config. The `integrations.enabled` master switch still applies.

## Development

Use JDK 21 and run `gradlew.bat test build`. Artifacts are written to `build/libs/`.
This NeoForge branch does not require the Forge 1.20.1 local jars used by main. Optional integrations use reflection; install matching NeoForge mods and their dependencies for in-game integration testing.
