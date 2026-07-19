package net.qolclient;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.CheckboxWidget;
import net.minecraft.text.Text;

/**
 * Second page for the newer additions, so the main QolMenuScreen doesn't
 * outgrow the window. Reached via "More Options..." on the main menu.
 */
public class QolExtraMenuScreen extends Screen {

    private final Screen parent;

    protected QolExtraMenuScreen(Screen parent) {
        super(Text.literal("QoL Client - More Options"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        QolConfig cfg = QolClient.CONFIG;
        int x = this.width / 2 - 100;
        int y = 24;
        int spacing = 18;

        addToggle(x, y, "Custom Elytra (self-view only)", cfg.customElytraEnabled, v -> {
            cfg.customElytraEnabled = v;
            if (v) ElytraManager.loadAndRegister();
        }); y += spacing;

        addToggle(x, y, "Custom Crosshair", cfg.crosshairCustomEnabled, v -> cfg.crosshairCustomEnabled = v); y += spacing;
        addToggle(x, y, "Hide Crosshair When Not Targeting", cfg.crosshairDynamicHide, v -> cfg.crosshairDynamicHide = v); y += spacing;
        addToggle(x, y, "Chat Opacity Boost", cfg.chatOpacityBoost, v -> { cfg.chatOpacityBoost = v; QolClient.applyPassiveOptions(); }); y += spacing;
        addToggle(x, y, "Item Cooldown %", cfg.itemCooldownTextEnabled, v -> cfg.itemCooldownTextEnabled = v); y += spacing;
        addToggle(x, y, "Blocks Mined Counter", cfg.blocksMinedCounterEnabled, v -> cfg.blocksMinedCounterEnabled = v); y += spacing;
        addToggle(x, y, "World Border Warning", cfg.worldBorderWarningEnabled, v -> cfg.worldBorderWarningEnabled = v); y += spacing;

        this.addDrawableChild(net.minecraft.client.gui.widget.ButtonWidget.builder(
                Text.literal("Open Elytra Folder"),
                b -> net.minecraft.util.Util.getOperatingSystem().open(
                    ElytraManager.elytraFilePath().getParent().toUri()))
            .pos(x, y).size(200, 20).build());
        y += 24;

        this.addDrawableChild(new net.minecraft.client.gui.widget.SliderWidget(
            x, y, 200, 20, Text.literal("Crosshair Scale: " + String.format("%.2f", cfg.crosshairScale)), (cfg.crosshairScale - 0.5) / (3.0 - 0.5)) {
            @Override
            protected void updateMessage() {
                this.setMessage(Text.literal("Crosshair Scale: " + String.format("%.2f", cfg.crosshairScale)));
            }
            @Override
            protected void applyValue() {
                cfg.crosshairScale = (float) (0.5 + this.value * (3.0 - 0.5));
            }
        });
        y += spacing;

        this.addDrawableChild(new net.minecraft.client.gui.widget.SliderWidget(
            x, y, 200, 20, Text.literal("Border Warning Distance: " + String.format("%.0f", cfg.worldBorderWarningDistance)),
            (cfg.worldBorderWarningDistance - 5.0) / (200.0 - 5.0)) {
            @Override
            protected void updateMessage() {
                this.setMessage(Text.literal("Border Warning Distance: " + String.format("%.0f", cfg.worldBorderWarningDistance)));
            }
            @Override
            protected void applyValue() {
                cfg.worldBorderWarningDistance = (float) (5.0 + this.value * (200.0 - 5.0));
            }
        });

        // Note: crosshair color has no widget here - vanilla's Screen/widget set has no color
        // picker, and cramming a hex text box in wasn't worth it for one field. Edit
        // crosshairColor directly in config/qolclient.json (packed 0xRRGGBB, e.g. 16711680 = red)
        // until/unless a color picker gets added.
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
