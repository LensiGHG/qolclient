package net.qolclient;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Same self-view-only deal as CapeManager: this swaps the elytra texture on
 * YOUR OWN client only. Unlike capes (which vanilla already fetches from a
 * per-player state field populated by the Mojang skin service), vanilla has
 * no per-player elytra texture concept at all - every player's elytra uses
 * the same fixed "textures/entity/elytra.png". So there's no session-service
 * equivalent to piggyback on here; ElytraMixin has to redirect the texture
 * lookup itself rather than just writing into a render-state field like
 * CapeMixin does. See ElytraMixin's javadoc for that part.
 */
public class ElytraManager {

    public static final Identifier CUSTOM_ELYTRA_ID = Identifier.of("qolclient", "custom_elytra");

    private static final Path ELYTRA_PATH = FabricLoader.getInstance()
        .getConfigDir().resolve("qolclient").resolve("elytra.png");

    private static boolean registered = false;

    public static Path elytraFilePath() {
        try {
            Files.createDirectories(ELYTRA_PATH.getParent());
        } catch (IOException ignored) { }
        return ELYTRA_PATH;
    }

    public static boolean elytraFileExists() {
        return Files.exists(ELYTRA_PATH);
    }

    /** Loads/reloads elytra.png into the texture manager. Call whenever the toggle is flipped on. */
    public static boolean loadAndRegister() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || !Files.exists(ELYTRA_PATH)) return false;

        try (InputStream in = Files.newInputStream(ELYTRA_PATH)) {
            NativeImage image = NativeImage.read(in);
            NativeImageBackedTexture texture = new NativeImageBackedTexture(() -> "qolclient custom elytra", image);
            client.getTextureManager().registerTexture(CUSTOM_ELYTRA_ID, texture);
            registered = true;
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            registered = false;
            return false;
        }
    }

    public static boolean isRegistered() {
        return registered;
    }
}
