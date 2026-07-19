package net.qolclient;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.option.ParticlesOption;
import net.minecraft.client.option.CloudRenderMode;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.lwjgl.glfw.GLFW;

public class QolClient implements ClientModInitializer {

    public static QolConfig CONFIG = QolConfig.load();

    private static KeyBinding menuKey;
    private static KeyBinding zoomKey;
    private static KeyBinding toggleSprintKey;
    private static long sessionTicks = 0;
    private static long blocksMined = 0;

    // Remember the player's normal FOV / gamma / particle / cloud settings so we can restore them.
    private static Double savedGamma = null;
    private static Double savedChatOpacity = null;
    private static boolean zoomActive = false;

    @Override
    public void onInitializeClient() {
        menuKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.qolclient.menu", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT_SHIFT, "category.qolclient"));

        zoomKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.qolclient.zoom", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_C, "category.qolclient"));

        toggleSprintKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.qolclient.togglesprint", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_UNKNOWN, "category.qolclient"));

        applyPassiveOptions();
        if (CONFIG.customCapeEnabled) {
            CapeManager.loadAndRegister();
        }
        if (CONFIG.customElytraEnabled) {
            ElytraManager.loadAndRegister();
        }

        ClientTickEvents.END_CLIENT_TICK.register(this::onTick);
        HudRenderCallback.EVENT.register(this::onHud);
    }

    private void onTick(MinecraftClient client) {
        if (client.player == null) return;
        sessionTicks++;

        if (menuKey.wasPressed()) {
            client.setScreen(new QolMenuScreen(client.currentScreen));
        }

        // Zoom: hold key -> lower FOV multiplier -> release -> restore.
        boolean zoomHeld = zoomKey.isPressed();
        if (zoomHeld && !zoomActive) {
            zoomActive = true;
        } else if (!zoomHeld && zoomActive) {
            zoomActive = false;
        }

        if (toggleSprintKey.wasPressed() && CONFIG.toggleSprint) {
            client.player.setSprinting(!client.player.isSprinting());
        }
    }

    private void onHud(net.minecraft.client.gui.DrawContext context, net.minecraft.client.render.RenderTickCounter tickCounter) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.options.hudHidden) return;

        int y = 4;
        if (CONFIG.showFps) {
            context.drawTextWithShadow(client.textRenderer,
                Text.literal(client.getCurrentFps() + " fps").formatted(Formatting.YELLOW), 4, y, 0xFFFFFF);
            y += 10;
        }
        if (CONFIG.showCoords) {
            var pos = client.player.getBlockPos();
            context.drawTextWithShadow(client.textRenderer,
                Text.literal(String.format("%d, %d, %d", pos.getX(), pos.getY(), pos.getZ()))
                    .formatted(Formatting.AQUA), 4, y, 0xFFFFFF);
            y += 10;
        }
        if (CONFIG.sessionTimerEnabled) {
            long seconds = sessionTicks / 20;
            context.drawTextWithShadow(client.textRenderer,
                Text.literal(String.format("Session: %02d:%02d:%02d", seconds / 3600, (seconds / 60) % 60, seconds % 60))
                    .formatted(Formatting.LIGHT_PURPLE), 4, y, 0xFFFFFF);
            y += 10;
        }
        if (CONFIG.blocksMinedCounterEnabled) {
            context.drawTextWithShadow(client.textRenderer,
                Text.literal("Blocks mined: " + blocksMined).formatted(Formatting.GREEN), 4, y, 0xFFFFFF);
            y += 10;
        }

        if (CONFIG.keystrokesHudEnabled) {
            renderKeystrokes(context, client);
        }
        if (CONFIG.itemCooldownTextEnabled) {
            renderItemCooldown(context, client);
        }
        if (CONFIG.worldBorderWarningEnabled) {
            renderWorldBorderWarning(context, client);
        }
    }

    /**
     * Draws the held item's remaining cooldown as a percentage over the hotbar
     * (e.g. ender pearl, chorus fruit). Uses ItemCooldownManager#getCooldownProgress,
     * whose vanilla semantics are 1.0 = cooldown just started (fully remaining),
     * ramping down to 0.0 = ready - i.e. the opposite of "how ready is it". We
     * show it as a percentage rather than seconds because the cooldown manager
     * doesn't expose the item's total cooldown duration/remaining ticks through
     * this method, only the 0-1 fraction; converting that fraction to real
     * seconds would need the per-item cooldown length, which varies by item and
     * isn't retrievable from here without extra lookups.
     */
    private void renderItemCooldown(net.minecraft.client.gui.DrawContext context, MinecraftClient client) {
        if (client.player == null) return;
        var stack = client.player.getMainHandStack();
        if (stack.isEmpty()) return;

        float progress = client.player.getItemCooldownManager().getCooldownProgress(stack, 0f);
        if (progress <= 0.0f) return; // ready, nothing to show

        int centerX = client.getWindow().getScaledWidth() / 2;
        int hotbarY = client.getWindow().getScaledHeight() - 42;
        Text text = Text.literal(String.format("%.0f%%", progress * 100)).formatted(Formatting.RED);
        context.drawCenteredTextWithShadow(client.textRenderer, text, centerX, hotbarY, 0xFFFFFF);
    }

    private void renderWorldBorderWarning(net.minecraft.client.gui.DrawContext context, MinecraftClient client) {
        if (client.player == null || client.world == null) return;
        var border = client.world.getWorldBorder();
        double distance = border.getDistanceInsideBorder(client.player);
        if (distance < CONFIG.worldBorderWarningDistance) {
            int centerX = client.getWindow().getScaledWidth() / 2;
            Text text = Text.literal(String.format("World border: %.0f blocks away", distance))
                .formatted(Formatting.RED, Formatting.BOLD);
            context.drawCenteredTextWithShadow(client.textRenderer, text, centerX, 20, 0xFFFFFF);
        }
    }

    public static void incrementBlocksMined() {
        blocksMined++;
    }

    private void renderKeystrokes(net.minecraft.client.gui.DrawContext context, MinecraftClient client) {
        long handle = client.getWindow().getHandle();
        int baseX = client.getWindow().getScaledWidth() - 90;
        int baseY = client.getWindow().getScaledHeight() - 90;

        drawKey(context, client, baseX + 30, baseY, "W", GLFW.GLFW_KEY_W, handle);
        drawKey(context, client, baseX, baseY + 20, "A", GLFW.GLFW_KEY_A, handle);
        drawKey(context, client, baseX + 30, baseY + 20, "S", GLFW.GLFW_KEY_S, handle);
        drawKey(context, client, baseX + 60, baseY + 20, "D", GLFW.GLFW_KEY_D, handle);
        drawKey(context, client, baseX, baseY + 40, "LMB", -1, handle,
            org.lwjgl.glfw.GLFW.glfwGetMouseButton(handle, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS);
        drawKey(context, client, baseX + 45, baseY + 40, "RMB", -1, handle,
            org.lwjgl.glfw.GLFW.glfwGetMouseButton(handle, GLFW.GLFW_MOUSE_BUTTON_RIGHT) == GLFW.GLFW_PRESS);
    }

    private void drawKey(net.minecraft.client.gui.DrawContext context, MinecraftClient client,
                          int x, int y, String label, int glfwKey, long handle) {
        boolean pressed = glfwKey >= 0 && org.lwjgl.glfw.GLFW.glfwGetKey(handle, glfwKey) == GLFW.GLFW_PRESS;
        drawKey(context, client, x, y, label, glfwKey, handle, pressed);
    }

    private void drawKey(net.minecraft.client.gui.DrawContext context, MinecraftClient client,
                          int x, int y, String label, int glfwKey, long handle, boolean pressed) {
        int size = 18;
        int bg = pressed ? 0xAA55FF55 : 0x66222222;
        context.fill(x, y, x + size, y + size, bg);
        context.drawCenteredTextWithShadow(client.textRenderer, Text.literal(label), x + size / 2, y + 5, 0xFFFFFF);
    }

    /** Whether the zoom key is currently held - used by a mixin/FOV hook. */
    public static boolean isZoomActive() {
        return zoomActive;
    }

    /** Applies the toggles that ride on existing vanilla options (no mixin needed). Call after any menu change. */
    public static void applyPassiveOptions() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.options == null) return;

        if (CONFIG.fullbright) {
            if (savedGamma == null) savedGamma = client.options.getGamma().getValue();
            client.options.getGamma().setValue(100.0); // vanilla gamma option is unbounded internally past the GUI's 1.0 slider cap
        } else if (savedGamma != null) {
            client.options.getGamma().setValue(savedGamma);
            savedGamma = null;
        }

        if (CONFIG.reducedParticles) {
            client.options.getParticles().setValue(ParticlesOption.MINIMAL);
        }

        if (CONFIG.simplifiedClouds) {
            client.options.getCloudRenderMode().setValue(CloudRenderMode.FAST);
        }

        if (CONFIG.chatOpacityBoost) {
            if (savedChatOpacity == null) savedChatOpacity = client.options.getChatOpacity().getValue();
            client.options.getChatOpacity().setValue(1.0);
        } else if (savedChatOpacity != null) {
            client.options.getChatOpacity().setValue(savedChatOpacity);
            savedChatOpacity = null;
        }
    }
}
