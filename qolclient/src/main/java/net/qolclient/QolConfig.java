package net.qolclient;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class QolConfig {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve("qolclient.json");

    public boolean noHurtCam = true;
    public boolean noFog = true;
    public boolean noNausea = true;
    public boolean fullbright = false;
    public boolean noPumpkinOverlay = true;
    public boolean lowFireOverlay = true;
    public boolean reducedParticles = false;
    public boolean simplifiedClouds = false;
    public boolean toggleSprint = false;
    public boolean showFps = true;
    public boolean showCoords = true;
    public double zoomFovMultiplier = 0.25; // lower = more zoom while zoom key held

    public boolean blockOutlineEnabled = true;
    public float blockOutlineWidth = 3.0f; // vanilla default ~2px; try 1-6

    public boolean totemScaleEnabled = false;
    public float totemScale = 1.0f; // 0.3 = tiny, 3.0 = huge

    public boolean customTexturesEnabled = false;
    public java.util.Set<String> disabledCustomItems = new java.util.HashSet<>();

    public boolean customCapeEnabled = false;

    public boolean keystrokesHudEnabled = false;
    public boolean sessionTimerEnabled = false;

    // --- New additions ---

    public boolean customElytraEnabled = false;

    public boolean crosshairCustomEnabled = false;
    public float crosshairScale = 1.0f; // 0.5 = tiny, 3.0 = huge
    public int crosshairColor = 0xFFFFFF; // packed RGB, alpha ignored
    public boolean crosshairDynamicHide = false; // hide crosshair unless looking at a block/entity

    public boolean chatOpacityBoost = false; // forces chat text opacity to fully readable

    public boolean itemCooldownTextEnabled = false; // seconds-remaining text over a cooling-down held item

    public boolean blocksMinedCounterEnabled = false; // session block-break counter in the HUD group

    public boolean worldBorderWarningEnabled = false;
    public float worldBorderWarningDistance = 50.0f; // blocks from the border before the warning shows

    public boolean showWaypoints = true;
    public java.util.List<Waypoint> waypoints = new java.util.ArrayList<>();

    public static class Waypoint {
        public String name;
        public int x, y, z;
        public int color = 0xFFFF55; // ARGB-ish, alpha ignored on text draw

        public Waypoint() { }
        public Waypoint(String name, int x, int y, int z) {
            this.name = name; this.x = x; this.y = y; this.z = z;
        }
    }

    public static QolConfig load() {
        try {
            if (Files.exists(PATH)) {
                String json = Files.readString(PATH, StandardCharsets.UTF_8);
                QolConfig cfg = GSON.fromJson(json, QolConfig.class);
                if (cfg != null) return cfg;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        QolConfig fresh = new QolConfig();
        fresh.save();
        return fresh;
    }

    public void save() {
        try {
            Files.createDirectories(PATH.getParent());
            Files.writeString(PATH, GSON.toJson(this), StandardCharsets.UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
