package net.qolclient;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * How this works, in short:
 *
 *  1. Drop files into one of three places under:
 *       .minecraft/config/qolclient/customtextures_source/
 *
 *     items/<item_id>/texture.png (+ optional model.json)
 *       -> re-skins/reshapes an item icon. e.g. items/mace/texture.png
 *
 *     blocks/<block_id>/texture.png
 *       -> re-skins a block's texture. e.g. blocks/dirt/texture.png with a
 *          copy of stone's texture in it makes dirt LOOK like stone.
 *          Caveat: this only works cleanly for blocks whose model uses one
 *          texture referenced by the block's own id (true for dirt, most
 *          simple blocks). Blocks with separate top/side/bottom textures
 *          (grass, logs, etc.) need each of those texture files placed via
 *          the "raw" folder below instead, since they don't share one name.
 *
 *     raw/assets/minecraft/...
 *       -> ANYTHING you drop here gets merged straight into the generated
 *          pack at that exact path, no re-naming. This is how you handle:
 *            - entity textures, e.g.
 *              raw/assets/minecraft/textures/entity/end_crystal/end_crystal.png
 *            - multi-texture blocks (grass_block_top.png, etc.)
 *            - any block/item model.json you want full control over
 *
 *  IMPORTANT - what this can't do: reskinning an entity (changing its
 *  texture) works fine through "raw", but reshaping an entity's actual
 *  geometry (e.g. making an end crystal LOOK LIKE a stick, not just be
 *  colored differently) is NOT something vanilla resource packs support at
 *  all - entity models are hardcoded Java shapes, not swappable JSON like
 *  item models are. OptiFine's CEM (Custom Entity Models) format can do
 *  this but only OptiFine itself reads that format; this mod doesn't
 *  implement a CEM interpreter. If you want a specific entity's actual
 *  shape changed, that needs a bespoke Java entity renderer for that one
 *  entity - ask if you want that built for a specific entity and I can do
 *  it as its own thing, but it's not a "drop a file in" general feature.
 *
 *  2. This class copies whichever items/blocks are toggled ON (everything
 *     in "raw" is always included when the master switch is on) into a real
 *     resource pack folder at .minecraft/resourcepacks/QoLClientTextures/.
 *
 *  3. It enables that pack via vanilla's own ResourcePackManager and calls
 *     client.reloadResources().
 */
public class CustomTextureManager {

    public enum Category { ITEM, BLOCK }

    public record Entry(Category category, String id) {
        public String key() { return category.name().toLowerCase(Locale.ROOT) + ":" + id; }
    }

    private static final Path SOURCE_DIR = FabricLoader.getInstance()
        .getConfigDir().resolve("qolclient").resolve("customtextures_source");

    private static final Path ITEMS_DIR = SOURCE_DIR.resolve("items");
    private static final Path BLOCKS_DIR = SOURCE_DIR.resolve("blocks");
    private static final Path RAW_DIR = SOURCE_DIR.resolve("raw");

    private static final String PACK_NAME = "QoLClientTextures";

    public static Path sourceDir() {
        return SOURCE_DIR;
    }

    /** Scans items/ and blocks/ and returns everything that has a texture.png present. */
    public static List<Entry> discoverEntries() {
        List<Entry> result = new ArrayList<>();
        result.addAll(discoverIn(ITEMS_DIR, Category.ITEM));
        result.addAll(discoverIn(BLOCKS_DIR, Category.BLOCK));
        result.sort(Comparator.comparing(Entry::key));
        return result;
    }

    private static List<Entry> discoverIn(Path dir, Category category) {
        try {
            if (!Files.isDirectory(dir)) {
                Files.createDirectories(dir);
                return List.of();
            }
            try (Stream<Path> children = Files.list(dir)) {
                return children
                    .filter(Files::isDirectory)
                    .filter(d -> Files.exists(d.resolve("texture.png")))
                    .map(d -> new Entry(category, d.getFileName().toString()))
                    .collect(Collectors.toList());
            }
        } catch (IOException e) {
            e.printStackTrace();
            return List.of();
        }
    }

    public static Path openableSourceFolder() {
        try {
            Files.createDirectories(ITEMS_DIR);
            Files.createDirectories(BLOCKS_DIR);
            Files.createDirectories(RAW_DIR.resolve("assets").resolve("minecraft"));
        } catch (IOException ignored) { }
        return SOURCE_DIR;
    }

    /**
     * Rebuilds the live resource pack from whichever discovered entries are
     * NOT in the disabled set (plus everything under raw/, unconditionally,
     * when the master switch is on), enables/disables it in the pack
     * manager, and reloads resources.
     */
    public static void rebuildAndApply() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) return;

        Path packRoot = client.runDirectory.toPath().resolve("resourcepacks").resolve(PACK_NAME);

        try {
            deleteRecursive(packRoot);

            if (QolClient.CONFIG.customTexturesEnabled) {
                List<Entry> enabled = discoverEntries().stream()
                    .filter(e -> !QolClient.CONFIG.disabledCustomItems.contains(e.key()))
                    .collect(Collectors.toList());

                boolean rawHasContent = Files.isDirectory(RAW_DIR) && Files.list(RAW_DIR).findAny().isPresent();

                if (!enabled.isEmpty() || rawHasContent) {
                    buildPack(packRoot, enabled);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        var packManager = client.getResourcePackManager();
        // NOTE: ResourcePackManager's exact method names (scanPacks/getEnabledIds/
        // setEnabledProfiles below) are accurate for recent 1.21.x as far as I could
        // verify without a live 1.21.11 decompile. If this fails to compile, open
        // net.minecraft.resource.ResourcePackManager in a mapping viewer and swap
        // in whatever the "rescan available packs" / "get enabled pack ids" /
        // "set enabled pack ids" methods are actually called there.
        packManager.scanPacks();
        String packId = "file/" + PACK_NAME;

        List<String> enabledIds = new ArrayList<>(packManager.getEnabledIds());
        boolean shouldBeEnabled = QolClient.CONFIG.customTexturesEnabled && Files.exists(packRoot);

        boolean currentlyEnabled = enabledIds.contains(packId);
        if (shouldBeEnabled && !currentlyEnabled) {
            enabledIds.add(packId);
            packManager.setEnabledProfiles(enabledIds);
        } else if (!shouldBeEnabled && currentlyEnabled) {
            enabledIds.remove(packId);
            packManager.setEnabledProfiles(enabledIds);
        }

        client.reloadResources();
    }

    private static void buildPack(Path packRoot, List<Entry> entries) throws IOException {
        Path assetsRoot = packRoot.resolve("assets").resolve("minecraft");
        Files.createDirectories(assetsRoot.resolve("textures").resolve("item"));
        Files.createDirectories(assetsRoot.resolve("textures").resolve("block"));
        Files.createDirectories(assetsRoot.resolve("models").resolve("item"));

        // 1.21.11 = pack_format 75. Using a min/max range so it keeps working
        // on nearby patch versions too; bump if Minecraft complains at launch.
        String mcmeta = "{\n" +
            "  \"pack\": {\n" +
            "    \"pack_format\": 75,\n" +
            "    \"description\": \"QoL Client - generated custom textures\",\n" +
            "    \"supported_formats\": { \"min_inclusive\": 60, \"max_inclusive\": 100 }\n" +
            "  }\n" +
            "}\n";
        Files.writeString(packRoot.resolve("pack.mcmeta"), mcmeta, StandardCharsets.UTF_8);

        for (Entry entry : entries) {
            Path sourceDir = (entry.category() == Category.ITEM ? ITEMS_DIR : BLOCKS_DIR).resolve(entry.id());
            Path texture = sourceDir.resolve("texture.png");
            Path model = sourceDir.resolve("model.json");

            String texFolder = entry.category() == Category.ITEM ? "item" : "block";
            if (Files.exists(texture)) {
                Files.copy(texture, assetsRoot.resolve("textures").resolve(texFolder).resolve(entry.id() + ".png"),
                    StandardCopyOption.REPLACE_EXISTING);
            }
            if (entry.category() == Category.ITEM && Files.exists(model)) {
                Files.copy(model, assetsRoot.resolve("models").resolve("item").resolve(entry.id() + ".json"),
                    StandardCopyOption.REPLACE_EXISTING);
            }
        }

        // Merge everything under raw/ straight into the pack, path-for-path.
        if (Files.isDirectory(RAW_DIR)) {
            try (Stream<Path> walk = Files.walk(RAW_DIR)) {
                for (Path src : (Iterable<Path>) walk::iterator) {
                    if (Files.isDirectory(src)) continue;
                    Path relative = RAW_DIR.relativize(src);
                    Path dest = packRoot.resolve(relative);
                    Files.createDirectories(dest.getParent());
                    Files.copy(src, dest, StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }
    }

    private static void deleteRecursive(Path path) throws IOException {
        if (!Files.exists(path)) return;
        try (Stream<Path> walk = Files.walk(path)) {
            walk.sorted(Comparator.reverseOrder()).forEach(p -> {
                try {
                    Files.delete(p);
                } catch (IOException ignored) { }
            });
        }
    }
}
