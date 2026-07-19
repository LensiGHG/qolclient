package net.qolclient;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.CheckboxWidget;
import net.minecraft.text.Text;

public class QolMenuScreen extends Screen {

    private final Screen parent;

    protected QolMenuScreen(Screen parent) {
        super(Text.literal("QoL Client"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        QolConfig cfg = QolClient.CONFIG;
        int x = this.width / 2 - 100;
        int y = 24;
        int spacing = 18;

        addToggle(x, y, "No Hurt Cam", cfg.noHurtCam, v -> cfg.noHurtCam = v); y += spacing;
        addToggle(x, y, "No Fog", cfg.noFog, v -> cfg.noFog = v); y += spacing;
        addToggle(x, y, "No Nausea", cfg.noNausea, v -> cfg.noNausea = v); y += spacing;
        addToggle(x, y, "Fullbright", cfg.fullbright, v -> { cfg.fullbright = v; QolClient.applyPassiveOptions(); }); y += spacing;
        addToggle(x, y, "No Pumpkin Overlay", cfg.noPumpkinOverlay, v -> cfg.noPumpkinOverlay = v); y += spacing;
        addToggle(x, y, "Low Fire Overlay", cfg.lowFireOverlay, v -> cfg.lowFireOverlay = v); y += spacing;
        addToggle(x, y, "Reduced Particles", cfg.reducedParticles, v -> { cfg.reducedParticles = v; QolClient.applyPassiveOptions(); }); y += spacing;
        addToggle(x, y, "Simplified Clouds", cfg.simplifiedClouds, v -> { cfg.simplifiedClouds = v; QolClient.applyPassiveOptions(); }); y += spacing;
        addToggle(x, y, "Toggle-Sprint Mode", cfg.toggleSprint, v -> cfg.toggleSprint = v); y += spacing;
        addToggle(x, y, "Show FPS", cfg.showFps, v -> cfg.showFps = v); y += spacing;
        addToggle(x, y, "Show Coordinates", cfg.showCoords, v -> cfg.showCoords = v); y += spacing;
        addToggle(x, y, "Block Outline Resize", cfg.blockOutlineEnabled, v -> cfg.blockOutlineEnabled = v); y += spacing;
        addToggle(x, y, "Totem Resize", cfg.totemScaleEnabled, v -> cfg.totemScaleEnabled = v); y += spacing;
        addToggle(x, y, "Keystrokes HUD", cfg.keystrokesHudEnabled, v -> cfg.keystrokesHudEnabled = v); y += spacing;
        addToggle(x, y, "Session Timer", cfg.sessionTimerEnabled, v -> cfg.sessionTimerEnabled = v); y += spacing;
        addToggle(x, y, "Custom Cape (self-view only)", cfg.customCapeEnabled, v -> {
            cfg.customCapeEnabled = v;
            if (v) net.qolclient.CapeManager.loadAndRegister();
        }); y += spacing;

        this.addDrawableChild(net.minecraft.client.gui.widget.ButtonWidget.builder(
                Text.literal("Search Keybinds..."),
                b -> { if (this.client != null) this.client.setScreen(new KeybindSearchScreen(this)); })
            .pos(x, y).size(200, 20).build());
        y += 24;

        this.addDrawableChild(net.minecraft.client.gui.widget.ButtonWidget.builder(
                Text.literal("Open Cape Folder"),
                b -> net.minecraft.util.Util.getOperatingSystem().open(
                    net.qolclient.CapeManager.capeFilePath().getParent().toUri()))
            .pos(x, y).size(200, 20).build());
        y += 24;

        this.addDrawableChild(net.minecraft.client.gui.widget.ButtonWidget.builder(
                Text.literal("Custom Item Textures..."),
                b -> { if (this.client != null) this.client.setScreen(new CustomTexturesScreen(this)); })
            .pos(x, y).size(200, 20).build());
        y += 24;

        this.addDrawableChild(net.minecraft.client.gui.widget.ButtonWidget.builder(
                Text.literal("More Options..."),
                b -> { if (this.client != null) this.client.setScreen(new QolExtraMenuScreen(this)); })
            .pos(x, y).size(200, 20).build());
        y += 24;

        this.addDrawableChild(new net.minecraft.client.gui.widget.SliderWidget(
            x, y, 200, 20, Text.literal("Totem Scale: " + String.format("%.2f", cfg.totemScale)), (cfg.totemScale - 0.3) / (3.0 - 0.3)) {
            @Override
            protected void updateMessage() {
                this.setMessage(Text.literal("Totem Scale: " + String.format("%.2f", cfg.totemScale)));
            }
            @Override
            protected void applyValue() {
                cfg.totemScale = (float) (0.3 + this.value * (3.0 - 0.3));
            }
        });
        y += spacing;

        this.addDrawableChild(new net.minecraft.client.gui.widget.SliderWidget(
            x, y, 200, 20, Text.literal("Outline Width: " + String.format("%.1f", cfg.blockOutlineWidth)), (cfg.blockOutlineWidth - 1.0) / (8.0 - 1.0)) {
            @Override
            protected void updateMessage() {
                this.setMessage(Text.literal("Outline Width: " + String.format("%.1f", cfg.blockOutlineWidth)));
            }
            @Override
            protected void applyValue() {
                cfg.blockOutlineWidth = (float) (1.0 + this.value * (8.0 - 1.0));
            }
        });
    }

    private void addToggle(int x, int y, String label, boolean initial, java.util.function.Consumer<Boolean> onChange) {
        this.addDrawableChild(CheckboxWidget.builder(Text.literal(label), this.textRenderer)
            .pos(x, y)
            .checked(initial)
            .callback((checkbox, checked) -> onChange.accept(checked))
            .build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 8, 0xFFFFFF);
    }

    @Override
    public void close() {
        QolClient.CONFIG.save();
        if (this.client != null) this.client.setScreen(parent);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
