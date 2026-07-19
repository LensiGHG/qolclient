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
 * IMPORTANT - read before you get excited: this makes a custom cape appear
 * on YOUR OWN client only. Other players (even other people running this
 * exact mod) will NOT see it, because it's just a locally-bound texture,
 * not something synced to a server or a shared cape service the way Optifine
 * capes / MinecraftCapes work (those need a server component). If you want
 * everyone to see the same cape, you'd need to run a small web service other
 * players' clients check - out of scope for what's here.
 */
public class CapeManager {

    public static final Identifier CUSTOM_CAPE_ID = Identifier.of("qolclient", "custom_cape");

    private static final Path CAPE_PATH = FabricLoader.getInstance()
        .getConfigDir().resolve("qolclient").resolve("cape.png");

    private static boolean registered = false;

    public static Path capeFilePath() {
        try {
            Files.createDirectories(CAPE_PATH.getParent());
        } catch (IOException ignored) { }
        return CAPE_PATH;
    }

    public static boolean capeFileExists() {
        return Files.exists(CAPE_PATH);
    }

    /** Loads/reloads cape.png into the texture manager. Call whenever the toggle is flipped on. */
    public static boolean loadAndRegister() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || !Files.exists(CAPE_PATH)) return false;

        try (InputStream in = Files.newInputStream(CAPE_PATH)) {
            NativeImage image = NativeImage.read(in);
            NativeImageBackedTexture texture = new NativeImageBackedTexture(() -> "qolclient custom cape", image);
            client.getTextureManager().registerTexture(CUSTOM_CAPE_ID, texture);
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
