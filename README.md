# NeoBugify — NeoForge port of debugify

NeoForge build of NeoBugify, ported from the Fabric Debugify
(`Debugify-1.21.1+1.0.jar`, Loom/Fabric) whose sources come from
`github.com/isXander/Debugify.

NeoBugify is not the original Debugify and is not affiliated with its author. It is
distributed under the same license, the **GNU Lesser General Public License v3.0 only**
(SPDX `LGPL-3.0-only`), and credits isXander's Debugify plus the projects Debugify itself
was based on in `META-INF/neoforge.mods.toml`.

`LICENSE` is **byte identical to the one Debugify ships** (7,652 bytes, the FSF's LGPL-3.0
text as it stands in `archive/1.21` and `main`). gnu.org publishes the LGPL-3.0 text on its
own, `lgpl-3.0.txt`; the longer LGPLv3+GPLv3 concatenation that circulates as "the LGPL-3.0
document" is a convenience rendering, not what the project carries. NeoBugify carries the
same file as the project it derives from. "v3.0 only" is expressed by the SPDX identifier in
`META-INF/neoforge.mods.toml` and `gradle.properties`; the `"or any later version"` wording
inside the "Revised Versions" section is the FSF explaining the option, not a grant of it.

## Licence compliance

LGPL-3.0 permits exactly this: modify the work, and convey the modified version under the
same licence. The obligations that come with it, and where each is satisfied:

| Obligation | Where |
|---|---|
| Ship the licence text unaltered | `LICENSE`, identical to Debugify's |
| State the licence (SPDX) | `license = "LGPL-3.0-only"` in `neoforge.mods.toml`, `modLicense` in `gradle.properties` |
| Modified versions must say they were modified, with a date (GPLv3 §5(a), via LGPLv3) | `NOTICE`, and summarised in the `neoforge.mods.toml` description |
| Keep every existing notice intact (§4/§5) | no upstream notice was removed; `MixinTreeFeature.java` keeps Ampflower's Zlib header untouched |
| Carry third party notices | TieFix / Sodium Extra / 2x2 Saplings Fix listed in `NOTICE` and in the mod description |
| Licence identifiable per file | SPDX header on all 93 `.java` files: 92 `LGPL-3.0-only`, 1 `Zlib` |
| Object code must be accompanied by its source (GPLv3 §6) | `NeoBugify-<version>-sources.jar` is produced by the build and must be published with the mod; the repository is the corresponding source |
| No further restrictions on use (§10) | no EULA-ish or "no reverse engineering" terms added anywhere |
| Don't misrepresent origin / no trademark claim | renamed, `authors` is not isXander, description disclaims affiliation |


| | Fabric original | This port |
|---|---|---|
| Mod id | | `debugify` | `neobugify` |
| Name | | Debugify | NeoBugify |
| License | | LGPL-3.0-only | LGPL-3.0-only |
| Loader metadata | | `fabric.mod.json` | `META-INF/neoforge.mods.toml` |
| Build plugin | | `fabric-loom` 1.7 | `net.neoforged.moddev` 2.0.147 |
| Loader | | Fabric Loader 0.16.2 | NeoForge 21.1.251 |
| Widening | | `debugify.accesswidener` | `META-INF/accesstransformer.cfg` |
| Refmaps | | `Debugify-refmap.json`, `client-Debugify-refmap.json` | not used (NeoForge runs on official mappings) |
| Entry class | | `Debugify::onInitialize` | `@Mod` class `DebugifyMod` |
| Config | | `config/debugify.json` | `config/neobugify.json` |
| Translations | | `assets/debugify/lang/*.json` | `assets/neobugify/lang/*.json` |

Renamed for the NeoBugify identity: mod id, display name, jar/artifact name, icon, config
file names, translation namespace and keys, the telemetry option key in `options.txt`, and the
`-Dneobugify.force*Fixes` debug flags. Deliberately **not** renamed: the Java package
(`dev.isxander.debugify`), the class names, the Mojang bug ids in `@BugFix`, and the mixin
configs' `package`/`plugin` entries — they are internal wiring that the mixin plugin resolves
by fully qualified name, and a rename there is a pure refactor with no user visible effect.

Build: `gradle build` → `build/libs/NeoBugify-1.21.1+1.0-neoforge.jar`
Dev runs: `gradle runClient`, `gradle runServer`.

## Supported versions

The mod is built and tested against **Minecraft 1.21.1 on NeoForge 21.1.x**, and the
`neoforge.mods.toml` ranges say exactly that: `minecraft [1.21.1,1.21.2)` and
`neoforge [21.1.0,21.2.0)`.

This matters more than a normal version bump, because NeoForge has no mixin refmap: every
member reference in the 70 mixins is matched literally against the patched Minecraft classes,
and those members move between 1.21.x releases. `_verify/check_other_versions.py` asks
Mojang's official mappings for another version whether the referenced members are still there:

| Version | Result vs 1.21.1 |
|---|---|
| 1.21.1 | baseline (only non-Minecraft and nested-name noise) |
| 1.21.4 | `ArmorStand#hurt`, `Boat#tick` gone |
| 1.21.11 | `Boat`, `Util`, `MouseHandler#onPress`, `Gui#renderExperienceBar` gone, plus `ArmorStand#hurt` |

An injection whose target cannot be found is a *critical* mixin failure, not a degraded one: it
throws `InjectionError` and takes the game down (this is exactly what MC-577 did before it was
ported). So a wide range would mean "the mod loads, then crashes". Widen the ranges only after
building and testing against the new version — which is what upstream does with its own
multi-version source sets.

## Loader API replacements

* `FabricLoader#isModLoaded` → `Debugify.isModLoaded` (`ModList`, null safe because the
  mixin config plugin runs before the mod list is populated)
* `FabricLoader#getModContainer(..).getMetadata().getName()` → `Debugify.getModName`
* `FabricLoader#getConfigDir` → `Debugify.getConfigDir` (`FMLPaths.CONFIGDIR`)
* `FabricLoader#getEnvironmentType` → `FMLEnvironment.dist`
* Fabric `"modmenu"` entrypoint (`ModMenuIntegration`, deleted) → NeoForge's built in mod
  list asks the mod container for an `IConfigScreenFactory`
* Fabric `"client"` entrypoint → `FMLClientSetupEvent` listener
* Fabric `preLaunch` `MixinExtrasBootstrap` → not needed, NeoForge ships
  `mixinextras-neoforge` and self bootstraps it

## DebugifyApi

Fabric mods implement `DebugifyApi` and declare a `debugify` entrypoint, which Debugify reads
while mixins bootstrap. NeoForge has no entrypoint containers, so implementations now call
`Debugify.registerApi(modId, api)` (usually from their mod constructor). Conflicts are applied
at registration time, and the mixin plugin additionally applies everything registered so far
via `Debugify.applyRegisteredApis()`.

## Mixin fixes required by the port

Loom's mixin compile extensions silently repair stale member references at build time and
Fabric's refmap resolves them at runtime; NeoForge has neither, so these had to be ported by
hand. Each one is annotated in the source.

* **MC-121706** `RangedBowAttackGoalMixin` — NeoForge patches `lookAt(Entity, float, float)`
  out of `LivingEntity` into `Mob`. The `@Shadow T mob` (erasing to `Monster`) no longer
  matched, so it shadows `Mob`, and the `@At` target owner is now `Mob`.
* **MC-93384** `mc93384/LivingEntityMixin` — NeoForge relocated the submerged bubble spawn
  from `LivingEntity#baseTick` to `Entity#doWaterSplashEffect`, so the mixin follows it there
  and takes the eye height from a checked `LivingEntity` cast.
* **MC-577** `AbstractContainerScreenMixin` — NeoForge's recompiled
  `AbstractContainerScreen#mouseClicked` names its locals `flag`/`slot` rather than
  `isPickItem`/`hoveredSlot`, so `LocalCapture` by name (and `@Local(type = Slot.class)`) no
  longer resolves and the injection scans 0 targets, which is a *critical* mixin failure that
  crashes the client rather than being downgraded by `DebugifyErrorHandler`. The capture is
  replaced by a `@Shadow` of the `hoveredSlot` field, which `findSlot` assigns before the
  `Util.getMillis` call, so it holds exactly what the local did.
* **MC-176559** `MultiPlayerGameModeMixin` — NeoForge rewrote
  `MultiPlayerGameMode#sameDestroyTarget`; it no longer calls
  `ItemStack#isSameItemSameComponents` at all, but
  `IItemStackExtension#shouldCauseBlockBreakReset` (whose default implementation in NeoForge
  already ignores `DataComponents.DAMAGE`, i.e. upstream ships this fix). The wrap target is
  repointed at that call and negates Debugify's durability-insensitive comparison, which
  reproduces the upstream behaviour.
* **MC-237493** access widener entries became access transformer entries
  (`Options$FieldAccess`, `TelemetryEventWidget$Content`, `TelemetryEventWidget$ContentBuilder`).

## Not ported

`src/gametest` from the Fabric project is not included: it depends on the Fabric gametest API
(`FabricGameTest` / `FabricGameTestHelper`) and has no NeoForge equivalent in this build.

## Verification

* `gradle build` — clean
* `gradle runServer` — 28 server side fixes enabled, server reaches `Done`, no mixin failures
* `gradle runClient` — 58 fixes enabled, client reaches the main menu, no mixin failures
* all 53 Minecraft class references and all 44 member references used in injection points were
  checked against the patched 21.1.251 classes
* `_verify/audit_mixins.py` re-checks every mixin statically: for each `@Mixin` target it
  disassembles the class and its whole superclass chain out of
  `build/moddev/artifacts/neoforge-21.1.251.jar` and verifies that every `method = "..."`
  (64), every `@At` invocation target (54) and every `@At` field target (2) exists there with
  the exact owner and descriptor Mixin will match against. Result: **0 problems**. It is what
  caught MC-176559, which no dev run had reached yet, because the client mixins only load once
  the client joins a world.

  ```powershell
  python _verify\audit_mixins.py
  ```

  Two things to keep in mind when re-running it: javap omits the owner prefix for references to
  the class being disassembled (`// Method stopRiding:()V` inside `ServerPlayer`) and prints
  `invokeinterface` as `// InterfaceMethod`; both are valid `@At` owners and have to be
  normalised, otherwise the audit drowns in false positives.

## Known gaps

* The audit checks member *references* only. It cannot see `LocalCapture` / `@Local`
  injections, which depend on the local variable table of the patched method, and client mixins
  are only applied once their target class loads — i.e. after the client joins a world. A
  `runClient` that only reaches the main menu therefore leaves a tail of client fixes
  (anything under `client/mixins/gameplay`, container screens, block breaking, …) unexercised.
  Play through a world once with the mod installed and check the log for
  `Critical injection failure` before shipping.
* `src/gametest` is not ported, so none of the 1.21.1 gametest suites are running against this
  build.

## Layout

* `src/main/java` — the whole mod. The Fabric project's split `main`/`client` source sets are
  merged into one; client-only classes are reached exclusively through
  `DebugifyMod`'s `FMLEnvironment.dist == Dist.CLIENT` branch, so a dedicated server never
  loads them.
* `_verify/` — the static mixin audit and the helper scripts used while porting. Not part of
  the mod jar.
