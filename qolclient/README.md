# QoL Client

A Fabric client mod of purely visual/comfort toggles for 1.21.11. Nothing in
here touches hitboxes, reach, movement speed, sends different packets to the
server, or gives any real gameplay advantage — it's the same category of
thing as Sodium's "reduced particles" option or vanilla's fullbright/gamma
setting, just gathered into one menu. It won't get you banned on servers
that only ban actual cheats (killaura, x-ray, reach hacks, etc.), but always
check DonutSMP's current rules yourself, since server rules can change and
some anarchy-adjacent economy servers do explicitly ban even fullbright.

Open the menu with **Right Shift**. Other keybinds (rebindable in Controls):
- **C** — hold to zoom
- (unbound by default) — toggle-sprint, only active if you enable "Toggle-Sprint Mode" in the menu

## Toggles

| Toggle | What it does |
|---|---|
| No Hurt Cam | Removes the red flash/camera jolt when you take damage |
| No Fog | Pushes render-distance/biome fog out so it's not visible |
| No Nausea | Removes the swirly screen warp from Nausea / portals |
| Fullbright | Maxes out in-game brightness (no darkness) |
| No Pumpkin Overlay | Removes the blur when wearing a carved pumpkin |
| Low Fire Overlay | Dims (doesn't fully remove) the on-fire screen overlay |
| Reduced Particles | Sets particles to Minimal (vanilla option, for FPS) |
| Simplified Clouds | Sets clouds to Fast (vanilla option, for FPS) |
| Toggle-Sprint Mode | Sprint toggles on/off instead of needing to hold the key |
| Show FPS | Small FPS counter, top-left |
| Show Coordinates | Small coordinate readout, top-left |
| Block Outline Resize | Makes the block-selection outline thicker/thinner (slider) |
| Totem Resize | Scales the Totem of Undying model up or down (slider), both held and during the "saved by totem" pop animation |
| Custom Item Textures | Swaps in your own texture (and optionally a full 3D model) per item, toggleable per-item in its own screen |
| Keystrokes HUD | WASD + left/right mouse button indicator, bottom-right |
| Session Timer | How long you've been playing this session, top-left |
| Custom Cape | Your own cape texture - **see the caveat below, this is self-view only** |
| Custom Elytra | Your own elytra texture - **self-view only, see caveat below, riskier than the cape** |
| Custom Crosshair | Scale + color tint for the crosshair (slider + toggle) |
| Hide Crosshair When Not Targeting | Crosshair only shows while looking at a block/entity |
| Chat Opacity Boost | Forces chat text to full opacity |
| Item Cooldown % | Shows remaining cooldown % over the hotbar for items like ender pearl |
| Blocks Mined Counter | Session block-break counter, next to the FPS/coords/timer group |
| World Border Warning | Red warning text when you get within a configurable distance of the world border |

These are all under **Menu → More Options...** to keep the main menu from overflowing.

## Search Keybinds

Menu → **Search Keybinds...** opens a standalone searchable list of every
registered keybind - vanilla's AND every installed mod's (including this
one) - with a text box to filter by name/category and click-to-rebind, same
as vanilla's Controls screen. I built this as its own screen rather than
bolting a search box onto vanilla's actual Controls screen: vanilla's list
widget internals shift between versions and are fragile to hook into, while
`client.options.allKeys` (which already contains every mod's keybinds) is a
stable, simple thing to build a reliable list from instead. Rebinding here
writes to the same underlying `KeyBinding` objects vanilla uses, so it stays
in sync with the real Controls screen either direction.

## Custom Cape

Menu → toggle **Custom Cape (self-view only)**, then **Open Cape Folder**
and drop in a `cape.png` (standard Minecraft cape texture dimensions, same
as any cape resource).

**Read this part:** this only changes what YOU see on your own player model
(e.g. in third person, or reflections). It does not make other players see
your cape - not even other people running this exact mod - because it's a
locally bound texture, not something synced anywhere. Real shared capes
(Optifine capes, MinecraftCapes, Mojang capes) work because there's a server
somewhere that every client checks; building that is out of scope here.

## Custom Elytra

Menu → More Options → toggle **Custom Elytra (self-view only)**, then **Open
Elytra Folder** and drop in an `elytra.png` (standard elytra texture
dimensions).

Same self-view-only deal as the cape - **and less certain to fully work**,
because vanilla has no per-player elytra texture the way it does for capes.
See ElytraMixin's file comment for the specifics of what to check if it
doesn't apply cleanly on 1.21.11.

## Custom Crosshair

Menu → More Options → **Custom Crosshair** toggle, plus a scale slider.
Color is set via `crosshairColor` in `config/qolclient.json` directly
(packed `0xRRGGBB` as a decimal int) - there's no in-game color picker widget
for it yet. **Hide Crosshair When Not Targeting** makes it disappear unless
you're looking at a block or entity.

## Not fully solid / verify before relying on

Same policy as before - most of this mod uses stable, common APIs, but a
few pieces reach into newer or more obscure parts of the renderer and are
flagged accordingly in their file's comments:

- **CapeMixin / PlayerRenderStateAccessor** - this is the one most likely to
  need hand-fixing. It targets `PlayerEntityRenderState`, part of the
  1.21.2+ renderer refactor where per-frame data is precomputed onto a
  state object; the exact field name/type for the cape texture is the part
  I couldn't verify with full certainty for 1.21.11 specifically.
- **TotemScaleMixin** - targets `HeldItemRenderer#renderFirstPersonItem`'s
  parameter list, which can shift release to release.
- **CustomTextureManager**'s `ResourcePackManager` method names.
- **ElytraMixin** - the riskiest of the new additions. Unlike the cape (which
  vanilla already resolves per-player via the skin service), there's no
  per-player elytra texture concept in vanilla at all, so this mixin has to
  intercept a local variable inside `ElytraFeatureRenderer#render` rather
  than write into a state field. If it doesn't apply, or applies to *other*
  players' elytras instead of just yours, see the javadoc in the file -
  it's flagged there, not silently assumed to work.
- **ItemCooldownMixin-adjacent HUD code** (`renderItemCooldown` in
  `QolClient.java`) - shows percentage remaining rather than seconds, since
  `getCooldownProgress` doesn't expose the item's total cooldown duration.
- **CrosshairMixin** targets `InGameHud#renderCrosshair` - name has been
  stable recently but worth a quick check same as the others.

Everything else (hurt cam, fog, nausea, overlays, zoom, block outline,
keybind search, keystrokes HUD, session timer) uses long-stable APIs I'm
confident in.


## Custom textures: items, blocks, and entities

Open the menu (Right Shift) → **Custom Item Textures...**. Master on/off
switch, a checklist of every item/block you've imported, **Open Textures
Folder**, and **Reload**.

Three ways to add content, all under `config/qolclient/customtextures_source/`:

```
items/<item_id>/texture.png        (+ optional model.json for a real 3D shape)
blocks/<block_id>/texture.png
raw/assets/minecraft/...           <- mirrors real resource pack paths, merged in as-is
```

- **Items**: works like before - texture.png reskins it, model.json (optional)
  gives it a genuinely different 3D shape (this is how you'd do a proper 3D
  mace, for instance).
- **Blocks**: texture.png only works cleanly for blocks whose model uses one
  texture named after the block itself - dirt, stone, and most simple blocks
  qualify. Want dirt to look like stone? Put a copy of stone's texture in
  `blocks/dirt/texture.png`. Blocks with separate top/side/bottom textures
  (grass, logs) need the `raw/` folder instead, since those don't share one
  file name.
- **Entities** (and anything else): only reachable through `raw/`, since
  entity texture paths vary and aren't a simple "one file per entity"
  convention. Example: `raw/assets/minecraft/textures/entity/end_crystal/end_crystal.png`
  reskins the end crystal. Content under `raw/` is always active whenever
  the master switch is on (no per-file toggle for it).

**Read this before trying "make the end crystal look like a stick":**
texture swapping (what all of the above does) only changes *coloring*, not
*shape*. An end crystal will always render as an end-crystal-shaped model,
just potentially wearing different colors/patterns - vanilla resource packs
have no mechanism to make one entity use a completely different entity's
geometry. The only thing that can do that is OptiFine's CEM (Custom Entity
Models) format, which only OptiFine's own client reads - this mod doesn't
implement a CEM interpreter. If you specifically want an end crystal (or
any one entity) to render as a genuinely different shape, that would need a
custom Java entity renderer built just for that entity - tell me which
entity and what you want it to look like and I can build that as its own
feature, but it's not something the general drop-a-file system can do.

A worked example (mace item, dirt block, end crystal entity) with
instructions is included in `customtextures_source_example/` in this zip.


## On the FPS claim

I didn't include a "make FPS 2x Sodium" toggle because I can't honestly
promise that — Sodium is a from-scratch, heavily optimized rendering engine;
a handful of visual toggles can't replicate years of GPU-pipeline work.
Reduced Particles / Simplified Clouds here will genuinely help FPS a bit,
but for a real jump, run **Sodium alongside this mod** (they don't conflict)
rather than expecting this to replace it.

## Building

1. Check `gradle.properties` against
   https://fabricmc.net/develop/?mc_version=1.21.11 before building — Fabric
   API/Loom move fast and the versions here may already be a patch behind.
2. `./gradlew build` → jar lands in `build/libs/`.

## Heads up on the mixins

Four of the toggles (Hurt Cam, Fog, Nausea, Pumpkin/Fire overlay, Zoom) work
by injecting into specific vanilla renderer methods. I've written these
against method/field names that are accurate for recent 1.21.x snapshots as
far as I can verify without a live decompile of 1.21.11 specifically, and
each mixin file has a comment explaining exactly what to check and fix if it
doesn't compile or doesn't apply — mainly: open the target class
(`GameRenderer`, `FogRenderer`, `LivingEntity`, `InGameHud`) in a mapping
viewer like https://mappings.dev or Linkie for 1.21.11 and confirm the
method signature matches. This is the normal amount of "verify against the
exact version" work for any mixin-based mod — renderer internals shift
release to release.

## Not included, on purpose

No reach/hitbox modification, no killaura, no x-ray, no autototem/scaffold,
no fly, no ESP. Those change actual gameplay/packets and are what gets
accounts banned — this mod stays out of that territory entirely.
