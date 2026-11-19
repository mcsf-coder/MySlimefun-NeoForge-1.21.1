package com.zeta.myslimefun;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class SlimefunGuideScreen extends Screen {

    // GUI Dimensions (Classic 9x5 Chest-Style Slimefun Guide)
    private static final int GUI_WIDTH = 176;
    private static final int GUI_HEIGHT = 114;
    private static final int SLOT_SIZE = 18;

    private enum ViewMode {
        CATEGORIES,
        CATEGORY_ITEMS,
        RECIPE_DETAIL,
        SEARCH
    }

    private record GuideCategory(String id, String name, ChatFormatting color, ItemStack icon, List<GuideEntry> entries) {}

    private record GuideEntry(
            String recipeId,      // null if already unlocked by default (like Dusts/Machines)
            int xpCost,
            ItemStack result,
            ItemStack machineIcon,
            String machineName,
            List<ItemStack> grid9 // Always 9 slots (3x3)
    ) {}

    private ViewMode currentMode = ViewMode.CATEGORIES;
    private GuideCategory selectedCategory = null;
    private GuideEntry selectedEntry = null;
    private EditBox searchBox;

    private int leftPos;
    private int topPos;

    public SlimefunGuideScreen() {
        super(Component.literal("Slimefun Guide"));
    }

    @Override
    protected void init() {
        super.init();
        this.leftPos = (this.width - GUI_WIDTH) / 2;
        this.topPos = (this.height - GUI_HEIGHT) / 2;

        this.searchBox = new EditBox(this.font, this.leftPos + 45, this.topPos + 6, 86, 12, Component.literal("Search..."));
        this.searchBox.setMaxLength(32);
        this.searchBox.setVisible(currentMode == ViewMode.SEARCH);
        this.addRenderableWidget(this.searchBox);
        if (currentMode == ViewMode.SEARCH) {
            this.setInitialFocus(this.searchBox);
        }
    }

    private List<GuideCategory> buildCategories() {
        // 1. Basic Machines Category
        List<GuideEntry> machines = List.of(
                new GuideEntry(null, 0,
                        new ItemStack(Items.CRAFTING_TABLE),
                        new ItemStack(Items.CRAFTING_TABLE),
                        "Multiblock (Place Crafting Table above Dispenser)",
                        List.of(
                                ItemStack.EMPTY, new ItemStack(Items.CRAFTING_TABLE), ItemStack.EMPTY,
                                ItemStack.EMPTY, new ItemStack(Items.DISPENSER), ItemStack.EMPTY,
                                ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY
                        )),
                new GuideEntry(null, 0,
                        new ItemStack(Items.OAK_FENCE),
                        new ItemStack(Items.GRINDSTONE),
                        "Multiblock (Place Wooden Fence above Dispenser)",
                        List.of(
                                ItemStack.EMPTY, new ItemStack(Items.OAK_FENCE), ItemStack.EMPTY,
                                ItemStack.EMPTY, new ItemStack(Items.DISPENSER), ItemStack.EMPTY,
                                ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY
                        )),
                new GuideEntry(null, 0,
                        new ItemStack(Items.CAULDRON),
                        new ItemStack(Items.WATER_BUCKET),
                        "Multiblock (Fence -> Cauldron -> Dispenser)",
                        List.of(
                                ItemStack.EMPTY, new ItemStack(Items.OAK_FENCE), ItemStack.EMPTY,
                                ItemStack.EMPTY, new ItemStack(Items.CAULDRON), ItemStack.EMPTY,
                                ItemStack.EMPTY, new ItemStack(Items.DISPENSER), ItemStack.EMPTY
                        ))
        );

        // 2. Resources (Dusts & Alloys) Category
        List<GuideEntry> resources = new ArrayList<>();
        // Dusts (Obtained from Grind Stone or Ore Washer)
        resources.add(new GuideEntry(null, 0, new ItemStack(ModItems.IRON_DUST.get(), 2),
                new ItemStack(Items.OAK_FENCE), "Grind Stone / Ore Washer",
                gridCenter(new ItemStack(Items.RAW_IRON))));
        resources.add(new GuideEntry(null, 0, new ItemStack(ModItems.GOLD_DUST.get(), 2),
                new ItemStack(Items.OAK_FENCE), "Grind Stone / Ore Washer",
                gridCenter(new ItemStack(Items.RAW_GOLD))));
        resources.add(new GuideEntry(null, 0, new ItemStack(ModItems.COPPER_DUST.get(), 2),
                new ItemStack(Items.OAK_FENCE), "Grind Stone / Ore Washer",
                gridCenter(new ItemStack(Items.RAW_COPPER))));
        resources.add(new GuideEntry(null, 0, new ItemStack(ModItems.TIN_DUST.get()),
                new ItemStack(Items.CAULDRON), "Ore Washer (Wash Sand)",
                gridCenter(new ItemStack(Items.SAND))));
        resources.add(new GuideEntry(null, 0, new ItemStack(ModItems.SILVER_DUST.get()),
                new ItemStack(Items.CAULDRON), "Ore Washer (Wash Sand)",
                gridCenter(new ItemStack(Items.SAND))));
        resources.add(new GuideEntry(null, 0, new ItemStack(ModItems.LEAD_DUST.get()),
                new ItemStack(Items.CAULDRON), "Ore Washer (Wash Sand)",
                gridCenter(new ItemStack(Items.SAND))));
        resources.add(new GuideEntry(null, 0, new ItemStack(ModItems.ALUMINUM_DUST.get()),
                new ItemStack(Items.CAULDRON), "Ore Washer (Wash Sand)",
                gridCenter(new ItemStack(Items.SAND))));
        resources.add(new GuideEntry(null, 0, new ItemStack(ModItems.ZINC_DUST.get()),
                new ItemStack(Items.CAULDRON), "Ore Washer (Wash Sand)",
                gridCenter(new ItemStack(Items.SAND))));
        resources.add(new GuideEntry(null, 0, new ItemStack(ModItems.MAGNESIUM_DUST.get()),
                new ItemStack(Items.CAULDRON), "Ore Washer (Wash Sand)",
                gridCenter(new ItemStack(Items.SAND))));

        // Alloys from Enhanced Crafting Table (Linked to XP Research!)
        for (MultiblockHandler.EnhancedRecipe r : MultiblockHandler.getEnhancedRecipes()) {
            List<ItemStack> grid = new ArrayList<>();
            for (int i = 0; i < 9; i++) {
                if (i < r.ingredients().size()) {
                    grid.add(new ItemStack(r.ingredients().get(i)));
                } else {
                    grid.add(ItemStack.EMPTY);
                }
            }
            resources.add(new GuideEntry(
                    r.id(),
                    r.xpCost(),
                    r.result().copy(),
                    new ItemStack(Items.CRAFTING_TABLE),
                    "Enhanced Crafting Table",
                    grid
            ));
        }

        // Match the exact Category Slots from your screenshot!
        return List.of(
                new GuideCategory("weapons", "Weapons", ChatFormatting.RED, new ItemStack(Items.GOLDEN_SWORD), List.of()),
                new GuideCategory("useful", "Useful Items", ChatFormatting.GOLD, new ItemStack(Items.CHEST), List.of()),
                new GuideCategory("machines", "Basic Machines", ChatFormatting.YELLOW, new ItemStack(Items.CRAFTING_TABLE), machines),
                new GuideCategory("tools", "Tools", ChatFormatting.GREEN, new ItemStack(Items.DIAMOND_AXE), List.of()),
                new GuideCategory("resources", "Resources", ChatFormatting.AQUA, new ItemStack(Items.IRON_INGOT), resources),
                new GuideCategory("food", "Food", ChatFormatting.LIGHT_PURPLE, new ItemStack(Items.GOLDEN_APPLE), List.of()),
                new GuideCategory("magical", "Magical Items", ChatFormatting.DARK_PURPLE, new ItemStack(Items.ENCHANTED_BOOK), List.of()),
                new GuideCategory("armor", "Armor", ChatFormatting.BLUE, new ItemStack(Items.DIAMOND_CHESTPLATE), List.of()),
                new GuideCategory("tech", "Electricity & Machines", ChatFormatting.GOLD, new ItemStack(Items.REDSTONE_BLOCK), List.of()),
                new GuideCategory("cargo", "Cargo Management", ChatFormatting.GRAY, new ItemStack(Items.HOPPER), List.of())
        );
    }

    private List<ItemStack> gridCenter(ItemStack centerItem) {
        return List.of(
                ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY,
                ItemStack.EMPTY, centerItem, ItemStack.EMPTY,
                ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY
        );
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // Keep the chest panel sharp without 1.21's background blur covering it
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, this.width, this.height, 0x80101010);

        // 1. Draw Classic Minecraft Chest Panel Background
        drawChestPanel(graphics, leftPos, topPos, GUI_WIDTH, GUI_HEIGHT);

        // Title at top-left just like real Slimefun Guide!
        graphics.drawString(this.font, "Slimefun Guide", leftPos + 8, topPos + 6, 0x404040, false);

        // 2. Draw Top & Bottom Black Stained Glass Border Slots (Row 0 and Row 4)
        for (int col = 0; col < 9; col++) {
            drawGlassSlot(graphics, leftPos + 7 + col * SLOT_SIZE, topPos + 17); // Top row
            drawGlassSlot(graphics, leftPos + 7 + col * SLOT_SIZE, topPos + 89); // Bottom row
        }

        // Top Bar Controls: Back Button (Slot 1) & Search Button (Slot 7)
        if (currentMode != ViewMode.CATEGORIES) {
            renderSlotIcon(graphics, new ItemStack(Items.ENCHANTED_BOOK), leftPos + 8 + 1 * SLOT_SIZE, topPos + 18);
        }
        renderSlotIcon(graphics, new ItemStack(Items.NAME_TAG), leftPos + 8 + 7 * SLOT_SIZE, topPos + 18);
// Bottom Bar Page Arrows (Slots 37 & 43)
        renderSlotIcon(graphics, new ItemStack(Items.LIME_STAINED_GLASS_PANE), leftPos + 8 + 1 * SLOT_SIZE, topPos + 90);
        renderSlotIcon(graphics, new ItemStack(Items.LIME_STAINED_GLASS_PANE), leftPos + 8 + 7 * SLOT_SIZE, topPos + 90);
        super.render(graphics, mouseX, mouseY, partialTick);
// 3. Render Active Screen Content + Hover Tooltips
        if (currentMode == ViewMode.CATEGORIES) {
            renderCategoryGrid(graphics, mouseX, mouseY);
        } else if (currentMode == ViewMode.CATEGORY_ITEMS && selectedCategory != null) {
            renderItemGrid(graphics, selectedCategory.entries(), mouseX, mouseY);
        } else if (currentMode == ViewMode.SEARCH) {
            renderItemGrid(graphics, getFilteredEntries(), mouseX, mouseY);
        } else if (currentMode == ViewMode.RECIPE_DETAIL && selectedEntry != null) {
            renderRecipeDetail(graphics, selectedEntry, mouseX, mouseY);
        }
// Top Bar Hover Tooltips
        if (currentMode != ViewMode.CATEGORIES && isHoveringSlot(mouseX, mouseY, 1, 0)) {
            graphics.renderTooltip(this.font, List.of(
                    Component.literal("⇦ Back").withStyle(ChatFormatting.GREEN),
                    Component.literal("Click to go back").withStyle(ChatFormatting.GRAY)
            ), java.util.Optional.empty(), mouseX, mouseY);
        }
        if (isHoveringSlot(mouseX, mouseY, 7, 0)) {
            graphics.renderTooltip(this.font, List.of(
                    Component.literal("Search...").withStyle(ChatFormatting.WHITE),
                    Component.literal("> Click to search for an item").withStyle(ChatFormatting.GRAY)
            ), java.util.Optional.empty(), mouseX, mouseY);
        }
    }
    private void renderCategoryGrid(GuiGraphics graphics, int mouseX, int mouseY) {
        List<GuideCategory> categories = buildCategories();
        for (int i = 0; i < categories.size(); i++) {
            int col = i % 9;
            int row = 1 + (i / 9);
            int x = leftPos + 8 + col * SLOT_SIZE;
            int y = topPos + 18 + row * SLOT_SIZE;
            GuideCategory cat = categories.get(i);
            renderSlotIcon(graphics, cat.icon(), x, y);
            if (isHovering(mouseX, mouseY, x, y, 16, 16)) {
                graphics.fill(x, y, x + 16, y + 16, 0x80FFFFFF);
                graphics.renderTooltip(this.font, List.of(
                        Component.literal(cat.name()).withStyle(cat.color()),
                        Component.literal("> Click to open").withStyle(ChatFormatting.GRAY)
                ), java.util.Optional.empty(), mouseX, mouseY);
            }
        }
    }
    private void renderItemGrid(GuiGraphics graphics, List<GuideEntry> entries, int mouseX, int mouseY) {
        Player player = Minecraft.getInstance().player;
        List<String> unlocked = player != null ? player.getData(ModAttachments.UNLOCKED_RECIPES) : List.of();
        if (entries.isEmpty()) {
            graphics.drawCenteredString(this.font, Component.literal("Coming Soon!").withStyle(ChatFormatting.DARK_GRAY), leftPos + GUI_WIDTH / 2, topPos + 55, 0xFFFFFF);
            return;
        }
        for (int i = 0; i < Math.min(entries.size(), 27); i++) {
            int col = i % 9;
            int row = 1 + (i / 9);
            int x = leftPos + 8 + col * SLOT_SIZE;
            int y = topPos + 18 + row * SLOT_SIZE;
            GuideEntry entry = entries.get(i);
            boolean isLocked = entry.recipeId() != null && (player == null || (!player.isCreative() && !unlocked.contains(entry.recipeId())));
// Real Slimefun shows a BARRIER for locked items, and the real item icon when unlocked!
            ItemStack displayStack = isLocked ? new ItemStack(Items.BARRIER) : entry.result();
            renderSlotIcon(graphics, displayStack, x, y);
            if (isHovering(mouseX, mouseY, x, y, 16, 16)) {
                graphics.fill(x, y, x + 16, y + 16, 0x80FFFFFF);
                if (isLocked) {
                    graphics.renderTooltip(this.font, List.of(
                            entry.result().getHoverName().copy().withStyle(ChatFormatting.RED),
                            Component.literal("LOCKED").withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD),
                            Component.literal("> Click to unlock").withStyle(ChatFormatting.GRAY),
                            Component.literal("Cost: " + entry.xpCost() + " Levels").withStyle(ChatFormatting.YELLOW)
                    ), java.util.Optional.empty(), mouseX, mouseY);
                } else {
                    graphics.renderTooltip(this.font, List.of(
                            entry.result().getHoverName().copy().withStyle(ChatFormatting.GREEN),
                            Component.literal("> Click to view recipe").withStyle(ChatFormatting.GRAY)
                    ), java.util.Optional.empty(), mouseX, mouseY);
                }
            }
        }
    }
    private void renderRecipeDetail(GuiGraphics graphics, GuideEntry entry, int mouseX, int mouseY) {
// Left side (Slot Col 1, Row 2): Machine Icon
        int machineX = leftPos + 8 + 1 * SLOT_SIZE;
        int machineY = topPos + 18 + 2 * SLOT_SIZE;
        drawSlotBox(graphics, machineX - 1, machineY - 1);
        renderSlotIcon(graphics, entry.machineIcon(), machineX, machineY);
        if (isHovering(mouseX, mouseY, machineX, machineY, 16, 16)) {
            graphics.renderTooltip(this.font, List.of(
                    Component.literal("Crafted in:").withStyle(ChatFormatting.GRAY),
                    Component.literal(entry.machineName()).withStyle(ChatFormatting.AQUA)
            ), java.util.Optional.empty(), mouseX, mouseY);
        }
// Center (Cols 3, 4, 5 | Rows 1, 2, 3): 3x3 Crafting Grid!
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 3; c++) {
                int idx = r * 3 + c;
                int slotX = leftPos + 8 + (3 + c) * SLOT_SIZE;
                int slotY = topPos + 18 + (1 + r) * SLOT_SIZE;
                drawSlotBox(graphics, slotX - 1, slotY - 1);
                ItemStack ing = entry.grid9().get(idx);
                if (!ing.isEmpty()) {
                    renderSlotIcon(graphics, ing, slotX, slotY);
                    if (isHovering(mouseX, mouseY, slotX, slotY, 16, 16)) {
                        graphics.renderTooltip(this.font, ing, mouseX, mouseY);
                    }
                }
            }
        }
// Right side (Col 7, Row 2): Output Result Slot!
        int outX = leftPos + 8 + 7 * SLOT_SIZE;
        int outY = topPos + 18 + 2 * SLOT_SIZE;
        drawSlotBox(graphics, outX - 1, outY - 1);
        renderSlotIcon(graphics, entry.result(), outX, outY);
        if (isHovering(mouseX, mouseY, outX, outY, 16, 16)) {
            graphics.renderTooltip(this.font, entry.result(), mouseX, mouseY);
        }
    }
    private List<GuideEntry> getFilteredEntries() {
        String query = searchBox.getValue().toLowerCase(Locale.ROOT).trim();
        List all = new ArrayList<>();
        for (GuideCategory cat : buildCategories()) {
            for (GuideEntry entry : cat.entries()) {
                if (query.isEmpty() || entry.result().getHoverName().getString().toLowerCase(Locale.ROOT).contains(query)) {
                    all.add(entry);
                }
            }
        }
        return all;
    }
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            Player player = Minecraft.getInstance().player;
// Back Button (Slot 1, Row 0)
            if (currentMode != ViewMode.CATEGORIES && isHoveringSlot(mouseX, mouseY, 1, 0)) {
                playClickSound();
                if (currentMode == ViewMode.RECIPE_DETAIL) {
                    currentMode = (selectedCategory != null) ? ViewMode.CATEGORY_ITEMS : ViewMode.CATEGORIES;
                } else {
                    currentMode = ViewMode.CATEGORIES;
                    selectedCategory = null;
                }
                searchBox.setVisible(false);
                return true;
            }
// Search Button (Slot 7, Row 0)
            if (isHoveringSlot(mouseX, mouseY, 7, 0)) {
                playClickSound();
                currentMode = ViewMode.SEARCH;
                searchBox.setVisible(true);
                searchBox.setFocused(true);
                return true;
            }
// Category Clicks
            if (currentMode == ViewMode.CATEGORIES) {
                List<GuideCategory> categories = buildCategories();;
                for (int i = 0; i < categories.size(); i++) {
                    int col = i % 9;
                    int row = 1 + (i / 9);
                    if (isHoveringSlot(mouseX, mouseY, col, row)) {
                        playClickSound();
                        selectedCategory = categories.get(i);
                        currentMode = ViewMode.CATEGORY_ITEMS;
                        return true;
                    }
                }
            }
// Item Clicks (Unlock with XP or Open 3x3 Recipe!)
            else if ((currentMode == ViewMode.CATEGORY_ITEMS && selectedCategory != null) || currentMode == ViewMode.SEARCH) {
                List<GuideEntry> entries = (currentMode == ViewMode.SEARCH) ? getFilteredEntries() : selectedCategory.entries();
                List unlocked = player != null ? player.getData(ModAttachments.UNLOCKED_RECIPES) : List.of();
                for (int i = 0; i < Math.min(entries.size(), 27); i++) {
                    int col = i % 9;
                    int row = 1 + (i / 9);
                    if (isHoveringSlot(mouseX, mouseY, col, row)) {
                        GuideEntry entry = entries.get(i);
                        boolean isLocked = entry.recipeId() != null && (player == null || (!player.isCreative() && !unlocked.contains(entry.recipeId())));
                        if (isLocked) {
// Spend XP to unlock!
                            PacketDistributor.sendToServer(new SlimefunNetwork.UnlockRecipePayload(entry.recipeId()));
                        } else {
// Open 3x3 Recipe View!
                            playClickSound();
                            selectedEntry = entry;
                            currentMode = ViewMode.RECIPE_DETAIL;
                        }
                        return true;
                    }
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
    private void playClickSound() {
        if (Minecraft.getInstance().player != null) {
            Minecraft.getInstance().player.playSound(SoundEvents.UI_BUTTON_CLICK.value(), 0.5f, 1.0f);
        }
    }
    private void drawChestPanel(GuiGraphics graphics, int x, int y, int w, int h) {
        graphics.fill(x, y, x + w, y + h, 0xFFC6C6C6);
        graphics.fill(x, y, x + w - 1, y + 1, 0xFFFFFFFF);
        graphics.fill(x, y, x + 1, y + h - 1, 0xFFFFFFFF);
        graphics.fill(x + 1, y + h - 1, x + w, y + h, 0xFF555555);
        graphics.fill(x + w - 1, y + 1, x + w, y + h, 0xFF555555);
    }
    private void drawGlassSlot(GuiGraphics graphics, int x, int y) {
        graphics.fill(x, y, x + 18, y + 18, 0xFF222222);
        graphics.fill(x + 1, y + 1, x + 17, y + 17, 0xFF141414);
    }
    private void drawSlotBox(GuiGraphics graphics, int x, int y) {
        graphics.fill(x, y, x + 18, y + 18, 0xFF373737);
        graphics.fill(x + 1, y + 1, x + 18, y + 18, 0xFFFFFFFF);
        graphics.fill(x + 1, y + 1, x + 17, y + 17, 0xFF8B8B8B);
    }
    private void renderSlotIcon(GuiGraphics graphics, ItemStack stack, int x, int y) {
        graphics.renderItem(stack, x, y);
        graphics.renderItemDecorations(this.font, stack, x, y);
    }
    private boolean isHoveringSlot(double mouseX, double mouseY, int col, int row) {
        int x = leftPos + 8 + col * SLOT_SIZE;
        int y = topPos + 18 + row * SLOT_SIZE;
        return isHovering(mouseX, mouseY, x, y, 16, 16);
    }
    private boolean isHovering(double mouseX, double mouseY, int x, int y, int w, int h) {
        return mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h;
    }
    @Override
    public boolean isPauseScreen() {
        return false;
    }
}