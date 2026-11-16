> [!WARNING]
> Every file in this repository – code, README, everything – was written by Claude (Anthropic's AI). No line was written by a human. Use at your own risk.

# FabisOSC

An **Orbital Strike Cannon** on a fishing rod. Cast the rod at a block up to 256 blocks away and TNT
rains down on it from the sky.

It recreates a friend's command-block OSC, which needed Carpet and Tweakeroo. This version needs no
client mods and comes in two flavours built from the same code:

| | Runs on | Who needs it |
|---|---|---|
| **Paper plugin** | Paper 26.1 – 26.3 | Only the server |
| **Fabric mod** | Minecraft 26.3 with Fabric | The server, or just you in singleplayer |

## Modes

**Stab** – A TNT charge falls from 100 blocks above the target and then drills a column straight down
to bedrock, layer by layer.

**Nuke** – A single TNT falls towards the target. After 60 % of the drop a strength-16 explosion blows
the TNT apart, and it spreads out into rings while it keeps falling. Each TNT lands, lies on the
ground for 5 ticks and then explodes. The number of rings is up to you (1–20).

## Using the rod

| Action | Effect |
|---|---|
| **Right-click** (cast) | Fire at the block you are looking at |
| **Left-click** | Switch between Stab and Nuke |
| **Sneak + left-click** | Change the number of rings (Nuke) |

The range is 256 blocks on X and Z. Height does not matter, so you can hit the bottom of a ravine
from a mountain top.

## Commands

| Command | What it does | Permission |
|---|---|---|
| `/osc give [player] [stab\|nuke] [rings]` | Give the OSC rod | Paper: `fabisosc.admin` · Fabric: op / cheats |
| `/osc mode <stab\|nuke>` | Set the mode of the rod in your hand | Paper: `fabisosc.use` |
| `/osc rings <count>` | Set the rings of the rod in your hand | Paper: `fabisosc.use` |
| `/osc reload` | Reload the config | Paper: `fabisosc.admin` · Fabric: op / cheats |

On Paper both permissions default to op. On Fabric anyone holding the rod can fire it; getting
one needs cheats, so turn cheats on in singleplayer.

## Install

There are no prebuilt downloads yet, so build it yourself (see below).

- **Paper:** put `FabisOSC-Paper-<version>.jar` into the server's `plugins/` folder.
- **Fabric:** put `FabisOSC-Fabric-<version>.jar` and [Fabric API](https://modrinth.com/mod/fabric-api)
  into the `mods/` folder of your server or your client instance (Fabric Loader 0.20.1 or newer).
  Players with a vanilla client can still join a Fabric server that has the mod.

## Config

Paper: `plugins/FabisOSC/config.yml` · Fabric: `config/fabisosc.json` (same options in camelCase).
Use `/osc reload` after editing.

| Option | Default | Meaning |
|---|---|---|
| `range` | 256 | Max distance to the target on X and Z |
| `cooldown-seconds` | 3 | Wait time between two shots per player |
| `drop-height` | 100 | How far above the target the TNT starts |
| `drop-items` | false | Let destroyed blocks drop items (laggy) |
| `stab.tnt-per-layer` | 12 | TNT per drill layer (more = wider hole) |
| `stab.layer-step` | 3 | Blocks between two drill layers |
| `stab.layers-per-tick` | 1 | Drill speed |
| `nuke.default-rings` | 8 | Rings when none are given |
| `nuke.max-rings` | 20 | Upper limit for rings |
| `nuke.ring-spacing` | 4 | Blocks between two rings |
| `nuke.point-spacing` | 3 | Blocks between two TNT on one ring |
| `nuke.tnt-per-point` | 2 | TNT per impact point |
| `nuke.split-at` | 0.6 | Part of the drop after which the TNT splits |
| `nuke.split-power` | 16 | Strength of the split explosion (normal TNT = 4) |
| `nuke.prime-offset` | 0.2 | Random sideways offset per TNT, like prime momentum |
| `nuke.ground-ticks` | 5 | Ticks the TNT lies on the ground before exploding |
| `nuke.center-hit` | true | Also hit the exact centre |

Big nukes (many rings, small spacing) mean hundreds of explosions at once and can lag the server.

## Build

Needs Java 25.

```bash
./gradlew build
```

The jars end up in `paper/build/libs/` and `fabric/build/libs/`.

| Folder | Contents |
|---|---|
| `common/` | Strike logic, TNT physics, settings – no Minecraft code |
| `paper/` | Paper plugin |
| `fabric/` | Fabric mod |

For local testing, `update-testserver.ps1` rebuilds both jars and starts a Paper test server
(`server/`, port 25565) and a Fabric dev server (`fabric/run/`, port 25567). The server files
themselves are not in the repository.
