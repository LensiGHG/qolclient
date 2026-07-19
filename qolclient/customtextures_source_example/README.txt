Example structure:

customtextures_source/
  items/
    mace/
      texture.png      <- required
      model.json         <- optional, for a real 3D shape not just a new skin
  blocks/
    dirt/
      texture.png       <- required. Put a copy of stone's texture here and
                           dirt will render as stone. Only works for blocks
                           whose model references one texture named after
                           the block itself (true for dirt/stone/most simple
                           blocks, not grass/logs - see raw/ for those).
  raw/
    assets/minecraft/textures/entity/end_crystal/end_crystal.png
      <- anything placed under raw/assets/minecraft/... is merged straight
         into the generated pack at that exact path. This is how you reskin
         entities (find the entity's real texture path online or in the
         vanilla jar) and how you handle blocks with more than one texture
         (grass_block_top.png, grass_block_side.png, etc.)

Note on entities: this only changes an entity's TEXTURE (its skin/colors).
It cannot change an entity's actual SHAPE (e.g. making an end crystal
literally look like a stick model) - vanilla resource packs don't support
reshaping entity geometry at all; that needs OptiFine's CEM format (which
this mod doesn't implement) or a custom Java renderer built specifically
for that one entity.

After adding/removing files, open the in-game menu (Right Shift) ->
"Custom Item Textures..." -> tick items/blocks on -> click Reload.
Content in raw/ is always included whenever the master switch is on.
