package net.qolclient;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.CheckboxWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Util;

import java.util.List;

public class CustomTexturesScreen extends Screen {

    private final Screen parent;

    protected CustomTexturesScreen(Screen parent) {
        super(Text.literal("Custom Item Textures"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        QolConfig cfg = QolClient.CONFIG;
        int x = this.width / 2 - 100;
        int y = 40;

        this.addDrawableChild(CheckboxWidget.builder(Text.literal("Custom Textures Enabled (master switch)"), this.textRenderer)
            .pos(x, y)
            .checked(cfg.customTexturesEnabled)
            .callback((cb, checked) -> cfg.customTexturesEnabled = checked)
            .build());
        y += 24;

        List<CustomTextureManager.Entry> entries = CustomTextureManager.discoverEntries();
        if (entries.isEmpty()) {
            y += 10;
        } else {
            for (CustomTextureManager.Entry entry : entries) {
                boolean enabled = !cfg.disabledCustomItems.contains(entry.key());
                int rowY = y;
                String label = "[" + entry.category().name() + "] " + entry.id();
                this.addDrawableChild(CheckboxWidget.builder(Text.literal(label), this.textRenderer)
                    .pos(x, rowY)
                    .checked(enabled)
                    .callback((cb, checked) -> {
                        if (checked) cfg.disabledCustomItems.remove(entry.key());
                        else cfg.disabledCustomItems.add(entry.key());
                    })
                    .build());
                y += 20;
                if (y > this.height - 60) break; // simple overflow guard, no scrolling in this version
            }
        }

        this.addDrawableChild(ButtonWidget.builder(Text.literal("Open Textures Folder"), b ->
                Util.getOperatingSystem().open(CustomTextureManager.openableSourceFolder().toUri()))
            .dimensions(this.width / 2 - 154, this.height - 48, 150, 20)
            .build());

        this.addDrawableChild(ButtonWidget.builder(Text.literal("Reload"), b -> {
                cfg.save();
                CustomTextureManager.rebuildAndApply();
                if (this.client != null) this.client.setScreen(new CustomTexturesScreen(parent));
            })
            .dimensions(this.width / 2 + 4, this.height - 48, 150, 20)
            .build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 15, 0xFFFFFF);
        if (CustomTextureManager.discoverEntries().isEmpty()) {
            context.drawCenteredTextWithShadow(this.textRenderer,
                Text.literal("No items/blocks imported yet. Click 'Open Textures Folder' and read the README."),
                this.width / 2, 70, 0xAAAAAA);
        }
        context.drawCenteredTextWithShadow(this.textRenderer,
            Text.literal("Entity textures & multi-texture blocks go in the raw/ folder instead - see README."),
            this.width / 2, this.height - 74, 0x808080);
        context.drawCenteredTextWithShadow(this.textRenderer,
            Text.literal("Click Reload after adding/removing files or toggling checkboxes."),
            this.width / 2, this.height - 62, 0x808080);
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
