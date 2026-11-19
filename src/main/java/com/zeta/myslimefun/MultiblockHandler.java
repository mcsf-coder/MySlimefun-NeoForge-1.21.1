package com.zeta.myslimefun;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.DispenserBlockEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

import java.util.List;

@EventBusSubscriber(modid = MySlimefun.MODID)
public class MultiblockHandler {

    public record EnhancedRecipe(String id, int xpCost, List<Item> ingredients, ItemStack result) {}
    private record GrindRecipe(Item input, ItemStack result) {}

    private static List<EnhancedRecipe> cachedEnhancedRecipes = null;
    private static List<GrindRecipe> cachedGrindRecipes = null;

    public static List<EnhancedRecipe> getEnhancedRecipes() {
        if (cachedEnhancedRecipes == null) {
            cachedEnhancedRecipes = List.of(
                    new EnhancedRecipe("steel_ingot", 2, List.of(ModItems.IRON_DUST.get(), Items.COAL, Items.IRON_INGOT),
                            new ItemStack(ModItems.STEEL_INGOT.get(), 1)),
                    new EnhancedRecipe("bronze_ingot", 2, List.of(ModItems.COPPER_DUST.get(), ModItems.TIN_DUST.get()),
                            new ItemStack(ModItems.BRONZE_INGOT.get(), 1)),
                    new EnhancedRecipe("brass_ingot", 2, List.of(ModItems.COPPER_DUST.get(), ModItems.ZINC_DUST.get()),
                            new ItemStack(ModItems.BRASS_INGOT.get(), 1)),
                    new EnhancedRecipe("duralumin_ingot", 3, List.of(ModItems.ALUMINUM_DUST.get(), ModItems.COPPER_DUST.get()),
                            new ItemStack(ModItems.DURALUMIN_INGOT.get(), 1)),
                    new EnhancedRecipe("solder_ingot", 3, List.of(ModItems.LEAD_DUST.get(), ModItems.TIN_DUST.get()),
                            new ItemStack(ModItems.SOLDER_INGOT.get(), 1)),
                    new EnhancedRecipe("damascus_steel", 5, List.of(ModItems.STEEL_INGOT.get(), ModItems.IRON_DUST.get(), Items.IRON_INGOT),
                            new ItemStack(ModItems.DAMASCUS_STEEL_INGOT.get(), 1)),
                    new EnhancedRecipe("hardened_metal", 8, List.of(ModItems.DAMASCUS_STEEL_INGOT.get(), ModItems.DURALUMIN_INGOT.get(), Items.COAL_BLOCK),
                            new ItemStack(ModItems.HARDENED_METAL.get(), 1)),
                    new EnhancedRecipe("reinforced_alloy", 12, List.of(ModItems.HARDENED_METAL.get(), ModItems.BRONZE_INGOT.get(), ModItems.SOLDER_INGOT.get()),
                            new ItemStack(ModItems.REINFORCED_ALLOY_INGOT.get(), 1))
            );
        }
        return cachedEnhancedRecipes;
    }

    public static EnhancedRecipe getRecipeById(String id) {
        for (EnhancedRecipe r : getEnhancedRecipes()) {
            if (r.id().equals(id)) return r;
        }
        return null;
    }

    private static List<GrindRecipe> getGrindRecipes() {
        if (cachedGrindRecipes == null) {
            cachedGrindRecipes = List.of(
                    new GrindRecipe(Items.COBBLESTONE, new ItemStack(Items.GRAVEL, 1)),
                    new GrindRecipe(Items.GRAVEL, new ItemStack(Items.SAND, 1)),
                    new GrindRecipe(Items.RAW_IRON, new ItemStack(ModItems.IRON_DUST.get(), 2)),
                    new GrindRecipe(Items.RAW_GOLD, new ItemStack(ModItems.GOLD_DUST.get(), 2)),
                    new GrindRecipe(Items.RAW_COPPER, new ItemStack(ModItems.COPPER_DUST.get(), 2))
            );
        }
        return cachedGrindRecipes;
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        Level level = event.getLevel();
        BlockPos pos = event.getPos();
        Player player = event.getEntity();

        if (player.isShiftKeyDown()) return;

        // 1. Enhanced Crafting Table: Crafting Table on top of Dispenser
        if (level.getBlockState(pos).is(Blocks.CRAFTING_TABLE)) {
            BlockPos belowPos = pos.below();
            if (level.getBlockState(belowPos).is(Blocks.DISPENSER)) {
                event.setCanceled(true);
                event.setCancellationResult(InteractionResult.SUCCESS);

                if (!level.isClientSide && level.getBlockEntity(belowPos) instanceof DispenserBlockEntity dispenser) {
                    handleEnhancedCraftingTable(level, pos, player, dispenser);
                }
            }
        }

        // 2. Fence Multiblocks (Grind Stone OR Ore Washer)
        if (level.getBlockState(pos).is(BlockTags.FENCES)) {
            BlockPos belowPos = pos.below();

            // Grind Stone: Fence directly on top of Dispenser
            if (level.getBlockState(belowPos).is(Blocks.DISPENSER)) {
                event.setCanceled(true);
                event.setCancellationResult(InteractionResult.SUCCESS);

                if (!level.isClientSide && level.getBlockEntity(belowPos) instanceof DispenserBlockEntity dispenser) {
                    handleGrindStone(level, pos, player, dispenser);
                }
            }
            // Ore Washer: Fence -> Cauldron -> Dispenser
            else if (level.getBlockState(belowPos).is(Blocks.CAULDRON) || level.getBlockState(belowPos).is(Blocks.WATER_CAULDRON)) {
                BlockPos dispenserPos = belowPos.below();
                if (level.getBlockState(dispenserPos).is(Blocks.DISPENSER)) {
                    event.setCanceled(true);
                    event.setCancellationResult(InteractionResult.SUCCESS);

                    if (!level.isClientSide && level.getBlockEntity(dispenserPos) instanceof DispenserBlockEntity dispenser) {
                        handleOreWasher(level, pos, player, dispenser);
                    }
                }
            }
        }
    }

    private static void handleEnhancedCraftingTable(Level level, BlockPos pos, Player player, DispenserBlockEntity dispenser) {
        for (EnhancedRecipe recipe : getEnhancedRecipes()) {
            if (hasAllIngredients(dispenser, recipe.ingredients())) {
                List<String> unlocked = player.getData(ModAttachments.UNLOCKED_RECIPES);
                if (!player.isCreative() && !unlocked.contains(recipe.id())) {
                    level.playSound(null, pos, SoundEvents.VILLAGER_NO, SoundSource.BLOCKS, 0.7f, 1.0f);
                    player.displayClientMessage(Component.literal("Locked! Unlock this in your Slimefun Guide for " + recipe.xpCost() + " XP levels.")
                            .withStyle(ChatFormatting.RED), true);
                    return;
                }

                consumeIngredients(dispenser, recipe.ingredients());

                ItemStack output = recipe.result().copy();
                if (!insertIntoDispenser(dispenser, output)) {
                    player.drop(output, false);
                }

                dispenser.setChanged();
                level.playSound(null, pos, SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 0.7f, 1.2f);
                player.displayClientMessage(Component.literal("Crafted: ")
                        .withStyle(ChatFormatting.GREEN)
                        .append(output.getHoverName()), true);
                return;
            }
        }

        level.playSound(null, pos, SoundEvents.VILLAGER_NO, SoundSource.BLOCKS, 0.6f, 1.0f);
        player.displayClientMessage(Component.literal("Enhanced Crafting Table: Invalid recipe in Dispenser!")
                .withStyle(ChatFormatting.RED), true);
    }

    private static void handleGrindStone(Level level, BlockPos pos, Player player, DispenserBlockEntity dispenser) {
        for (GrindRecipe recipe : getGrindRecipes()) {
            if (hasAllIngredients(dispenser, List.of(recipe.input()))) {
                consumeIngredients(dispenser, List.of(recipe.input()));

                ItemStack output = recipe.result().copy();
                if (!insertIntoDispenser(dispenser, output)) {
                    player.drop(output, false);
                }

                dispenser.setChanged();
                level.playSound(null, pos, SoundEvents.GRINDSTONE_USE, SoundSource.BLOCKS, 0.8f, 1.0f);
                player.displayClientMessage(Component.literal("Ground into: ")
                        .withStyle(ChatFormatting.AQUA)
                        .append(output.getHoverName()), true);
                return;
            }
        }

        level.playSound(null, pos, SoundEvents.VILLAGER_NO, SoundSource.BLOCKS, 0.6f, 1.0f);
        player.displayClientMessage(Component.literal("Grind Stone: Put Cobblestone, Gravel, or Raw Ore in Dispenser!")
                .withStyle(ChatFormatting.RED), true);
    }

    private static void handleOreWasher(Level level, BlockPos pos, Player player, DispenserBlockEntity dispenser) {
        if (hasAllIngredients(dispenser, List.of(Items.SAND))) {
            consumeIngredients(dispenser, List.of(Items.SAND));

            List<Item> allDusts = List.of(
                    ModItems.IRON_DUST.get(),
                    ModItems.GOLD_DUST.get(),
                    ModItems.COPPER_DUST.get(),
                    ModItems.TIN_DUST.get(),
                    ModItems.SILVER_DUST.get(),
                    ModItems.LEAD_DUST.get(),
                    ModItems.ALUMINUM_DUST.get(),
                    ModItems.ZINC_DUST.get(),
                    ModItems.MAGNESIUM_DUST.get()
            );

            RandomSource random = level.getRandom();
            Item washedDust = allDusts.get(random.nextInt(allDusts.size()));
            ItemStack output = new ItemStack(washedDust, 1);
            if (!insertIntoDispenser(dispenser, output)) {
                player.drop(output, false);
            }
            dispenser.setChanged();
            level.playSound(null, pos, SoundEvents.BUCKET_EMPTY, SoundSource.BLOCKS, 0.8f, 1.2f);
            player.displayClientMessage(Component.literal("Washed Sand -> ")
                    .withStyle(ChatFormatting.BLUE)
                    .append(output.getHoverName()), true);
            return;
        }
        level.playSound(null, pos, SoundEvents.VILLAGER_NO, SoundSource.BLOCKS, 0.6f, 1.0f);
        player.displayClientMessage(Component.literal("Ore Washer: Put Sand inside the bottom Dispenser!")
                .withStyle(ChatFormatting.RED), true);
    }
    private static boolean hasAllIngredients(DispenserBlockEntity dispenser, List<Item> required) {
        for (Item req : required) {
            boolean found = false;
            for (int i = 0; i < dispenser.getContainerSize(); i++) {
                ItemStack stack = dispenser.getItem(i);
                if (!stack.isEmpty() && stack.is(req)) {
                    found = true;
                    break;
                }
            }
            if (!found) return false;
        }
        return true;
    }
    private static void consumeIngredients(DispenserBlockEntity dispenser, List<Item> required) {
        for (Item req : required) {
            for (int i = 0; i < dispenser.getContainerSize(); i++) {
                ItemStack stack = dispenser.getItem(i);
                if (!stack.isEmpty() && stack.is(req)) {
                    stack.shrink(1);
                    break;
                }
            }
        }
    }
    private static boolean insertIntoDispenser(DispenserBlockEntity dispenser, ItemStack output) {
        for (int i = 0; i < dispenser.getContainerSize(); i++) {
            ItemStack slot = dispenser.getItem(i);
            if (!slot.isEmpty() && ItemStack.isSameItemSameComponents(slot, output) && slot.getCount() < slot.getMaxStackSize()) {
                slot.grow(output.getCount());
                return true;
            }
        }
        for (int i = 0; i < dispenser.getContainerSize(); i++) {
            if (dispenser.getItem(i).isEmpty()) {
                dispenser.setItem(i, output);
                return true;
            }
        }
        return false;
    }
}