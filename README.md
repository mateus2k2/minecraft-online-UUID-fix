# Online UUID Fix

[![Modrinth](https://img.shields.io/modrinth/dt/online-uuid-fix?label=Modrinth&logo=modrinth)](https://modrinth.com/mod/online-uuid-fix)

> **Warning:** Always back up your entire world folder before installing this mod or performing any UUID migration. UUID changes affect player data, advancements, stats, and pet ownership. There is no automatic rollback.

> **Note:** This mod's code was generated with the assistance of AI (Claude by Anthropic). Use it at your own risk and review the code before deploying on a production server.

A Fabric server-side mod for **offline-mode servers** that assigns players their real Mojang/Microsoft UUID instead of the vanilla offline UUID.

When a server runs in offline mode (`online-mode=false`), Minecraft generates a UUID from the player's username using a local hash. This means the same player gets a different UUID than they would on an online-mode server, breaking compatibility with player data from online servers and causing issues when players switch usernames.

This mod intercepts UUID assignment at login and fetches the real UUID from the Mojang API, falling back to the vanilla offline UUID for cracked/non-Mojang accounts or when the API is unreachable.

---

## Requirements

- Minecraft **1.21.11**
- Fabric Loader **0.18.2+**
- Server-side only — does not need to be installed on clients

---

## Installation

1. Download `modid-1.0.0.jar` from the releases or build it yourself (see below)
2. Drop it into your server's `mods/` folder
3. Start the server — no configuration needed

The mod will log resolved UUIDs on first login:
```
[OnlineUuidFix] Resolved Steve -> xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx
```

UUID lookups are cached to `config/online-uuid-fix/uuid-cache.json` so the Mojang API is only called once per username.

---

## Building from source

Requirements: JDK 21 ([Eclipse Adoptium](https://adoptium.net) — install the **JDK**, not JRE)

```powershell
# Windows — set JDK if you have both JDK and JRE installed
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-21.0.10.7-hotspot"

./gradlew build
```

Output jar: `build/libs/modid-1.0.0.jar`

---

## Migrating an existing server to online UUIDs

If your server already has player data stored under offline UUIDs, you need to rename those files to the correct online UUIDs, otherwise players will start fresh.

Player data is stored in:
```
world/playerdata/xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx.dat
world/advancements/xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx.json
world/stats/xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx.json
```

### Step-by-step migration

**1. Find each player's offline UUID (the current filename)**

The file is named after the UUID the server assigned. List `world/playerdata/` to see all files.

**2. Find the player's real online UUID**

Look it up at [namemc.com](https://namemc.com) or use the Mojang API directly:
```
https://api.mojang.com/users/profiles/minecraft/<username>
```
The `id` field in the response is the UUID without dashes — insert dashes in the pattern `8-4-4-4-12`.

You can also use the calculator at:
```
https://minecraft.wiki/w/Calculators/Player_UUID
```

**3. Rename the files**

With the server **stopped**, rename each file:
```
world/playerdata/<offline-uuid>.dat  →  world/playerdata/<online-uuid>.dat
world/advancements/<offline-uuid>.json  →  world/advancements/<online-uuid>.json
world/stats/<offline-uuid>.json  →  world/stats/<online-uuid>.json
```

**4. Start the server with this mod installed**

New logins will now receive the online UUID, which matches the renamed files.

---

## What migration does NOT cover

### Pets and tamed animals
Pet ownership is stored inside the pet's NBT data as the owner's UUID. After renaming player files, pets will still reference the old offline UUID and won't recognize the player as their owner.

There are two ways to fix this:

**Option A — In-game command (server must be running)**

Stand next to the pets and run:
```
/execute as @e[type=wolf,distance=..10] run data modify entity @s Owner set from entity @p UUID
```
This reassigns ownership of all wolves within 10 blocks to the nearest player. Replace `wolf` with other pet types (e.g. `cat`, `parrot`, `horse`) and adjust the distance as needed.

**Option B — NBTExplorer (server must be offline)**

Use **[NBTExplorer](https://github.com/jaquadro/NBTExplorer)** to edit world NBT data directly:
- Search for the old offline UUID string in region files
- Replace each occurrence with the new online UUID

### Advancements
Advancements are stored in separate `.json` files (not inside `.dat`). These also need to be renamed — see step 3 above.

### Other players' inventories
If you need to copy a player's inventory to another player (e.g. merging accounts):
1. Back up both `.dat` files
2. Copy and rename the source player's `.dat` to the target player's UUID
3. The target player will then have the source player's inventory, XP, and position

This will not transfer pet ownership — see the note above.

---

## How it works

The mod injects into `net.minecraft.core.UUIDUtil.createOfflinePlayerUUID` via a Mixin. Every time the server would generate an offline UUID from a username, the mod first queries the Mojang API (`api.mojang.com/users/profiles/minecraft/<name>`). If a result is returned, that UUID is used instead. If the API returns 404 (no Mojang account) or is unreachable, the original vanilla offline UUID is used as a fallback.
