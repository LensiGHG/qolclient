package net.qolclient;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * A from-scratch keybind screen instead of hooking vanilla's Controls
 * screen, on purpose: vanilla's ControlsListWidget internals shift around
 * release to release and are fragile to mixin into for filtering. Since
 * client.options.allKeys already contains every registered KeyBinding -
 * vanilla's AND every mod's (this mod's included, since Fabric registers
 * through the same array) - a standalone list gets the "search across
 * everything" goal without depending on vanilla's private list-widget
 * plumbing.
 *
 * Rebinding writes straight into the real KeyBinding objects and calls
 * options.write(), so changes here show up in vanilla's Controls screen
 * too (and vice versa).
 */
public class KeybindSearchScreen extends Screen {

    private final Screen parent;
    private TextFieldWidget searchBox;
    private final List<ButtonWidget> rowButtons = new ArrayList<>();
    private KeyBinding listeningFor = null;
    private int scrollOffset = 0;

    private static final int LIST_TOP = 40;
    private static final int ROW_HEIGHT = 20;

    protected KeybindSearchScreen(Screen parent) {
        super(Text.literal("Search Keybinds"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        searchBox = new TextFieldWidget(this.textRenderer, this.width / 2 - 100, 12, 200, 20, Text.literal("Search"));
        searchBox.setChangedListener(s -> rebuildRows());
        this.addSelectableChild(searchBox);
        this.setInitialFocus(searchBox);

        this.addDrawableChild(ButtonWidget.builder(Text.literal("Done"), b -> this.close())
            .pos(this.width / 2 - 50, this.height - 26).size(100, 20).build());

        rebuildRows();
    }

    private List<KeyBinding> matchingKeys() {
        String query = searchBox == null ? "" : searchBox.getText().trim().toLowerCase(Locale.ROOT);
        KeyBinding[] all = MinecraftClient.getInstance().options.allKeys;
        List<KeyBinding> result = new ArrayList<>();
        for (KeyBinding kb : all) {
            String name = Text.translatable(kb.getTranslationKey()).getString().toLowerCase(Locale.ROOT);
            String category = Text.translatable(kb.getCategory()).getString().toLowerCase(Locale.ROOT);
            if (query.isEmpty() || name.contains(query) || category.contains(query)) {
                result.add(kb);
            }
        }
        result.sort(Comparator.comparing((KeyBinding kb) -> Text.translatable(kb.getCategory()).getString())
            .thenComparing(kb -> Text.translatable(kb.getTranslationKey()).getString()));
        return result;
    }

    private void rebuildRows() {
        for (ButtonWidget b : rowButtons) this.remove(b);
        rowButtons.clear();
        scrollOffset = 0;
        layoutRows();
    }

    private void layoutRows() {
        for (ButtonWidget b : rowButtons) this.remove(b);
        rowButtons.clear();

        List<KeyBinding> keys = matchingKeys();
        int y = LIST_TOP - scrollOffset;
        for (KeyBinding kb : keys) {
            int rowY = y;
            String label = Text.translatable(kb.getTranslationKey()).getString()
                + "  [" + Text.translatable(kb.getCategory()).getString() + "]";

            ButtonWidget nameButton = ButtonWidget.builder(Text.literal(label), b -> {})
                .pos(this.width / 2 - 200, rowY).size(260, ROW_HEIGHT - 2).build();
            nameButton.active = false; // label only

            ButtonWidget bindButton = ButtonWidget.builder(currentBindLabel(kb), b -> {
                    listeningFor = kb;
                    rebuildRows();
                })
                .pos(this.width / 2 + 62, rowY).size(138, ROW_HEIGHT - 2).build();

            if (rowY >= LIST_TOP - ROW_HEIGHT && rowY <= this.height - 50) {
                this.addDrawableChild(nameButton);
                this.addDrawableChild(bindButton);
                rowButtons.add(nameButton);
                rowButtons.add(bindButton);
            }
            y += ROW_HEIGHT;
        }
    }

    private Text currentBindLabel(KeyBinding kb) {
        if (listeningFor == kb) return Text.literal("> press a key <");
        return Text.literal(kb.getBoundKeyLocalizedText().getString());
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (listeningFor != null) {
            if (keyCode == InputUtil.GLFW_KEY_ESCAPE) {
                listeningFor.setBoundKey(InputUtil.UNKNOWN_KEY);
            } else {
                listeningFor.setBoundKey(InputUtil.Type.KEYSYM.createFromCode(keyCode));
            }
            KeyBinding.updateKeysByCode();
            listeningFor = null;
            rebuildRows();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (listeningFor != null) {
            listeningFor.setBoundKey(InputUtil.Type.MOUSE.createFromCode(button));
            KeyBinding.updateKeysByCode();
            listeningFor = null;
            rebuildRows();
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        int maxScroll = Math.max(0, matchingKeys().size() * ROW_HEIGHT - (this.height - LIST_TOP - 60));
        scrollOffset = (int) Math.max(0, Math.min(maxScroll, scrollOffset - verticalAmount * ROW_HEIGHT * 2));
        layoutRows();
        return true;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);
        context.enableScissor(0, LIST_TOP - ROW_HEIGHT, this.width, this.height - 50);
        for (var child : this.children()) {
            if (child instanceof net.minecraft.client.gui.Drawable d) d.render(context, mouseX, mouseY, delta);
        }
        context.disableScissor();
        searchBox.render(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(this.textRenderer,
            Text.literal("Search across ALL keybinds (vanilla + every installed mod)"),
            this.width / 2, this.height - 40, 0xAAAAAA);
    }

    @Override
    public void close() {
        MinecraftClient.getInstance().options.write();
        if (this.client != null) this.client.setScreen(parent);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
