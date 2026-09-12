# MoreInformation

Calendar dates match main: a new world starts at year 0, day 0. Each 24,000 DayTime ticks advances the date; sleeping follows vanilla DayTime.

A client-side information HUD for Minecraft 1.21.1 NeoForge, version 1.0.2. By default it shows TPS / MSPT / RAM and player info. Extra lines and mod integrations can be enabled in the config.

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

Nature's Compass and Explorer's Compass are on by default. The other integrations are off by default. The master switch is `integrations.enabled`.

Providers registered by other mods through `HudInfoRegistry` are enabled by default, without additional provider ids in the config. The `integrations.enabled` master switch still applies.

## Development

Use JDK 21 and run `gradlew.bat test build`. Artifacts are written to `build/libs/`.
This NeoForge branch does not require the Forge 1.20.1 local jars used by main. Optional integrations use reflection; install matching NeoForge mods and their dependencies for in-game integration testing.
