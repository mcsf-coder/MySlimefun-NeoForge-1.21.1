package com.zeta.myslimefun;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.List;

public class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MySlimefun.MODID);

    // Slimefun Guide Book
    public static final DeferredItem<Item> SLIMEFUN_GUIDE = ITEMS.register("slimefun_guide",
            () -> new Item(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON)) {
                @Override
                public net.minecraft.world.InteractionResultHolder<ItemStack> use(net.minecraft.world.level.Level level, net.minecraft.world.entity.player.Player player, net.minecraft.world.InteractionHand usedHand) {
                    if (level.isClientSide) {
                        net.minecraft.client.Minecraft.getInstance().setScreen(new SlimefunGuideScreen());
                    }
                    return net.minecraft.world.InteractionResultHolder.sidedSuccess(player.getItemInHand(usedHand), level.isClientSide());
                }

                @Override
                public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
                    tooltipComponents.add(Component.literal("Your essential guide to Slimefun!").withStyle(ChatFormatting.GRAY));
                    tooltipComponents.add(Component.literal("Right-click to open Research & Recipes").withStyle(ChatFormatting.YELLOW));
                    super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
                }
            });


    // Basic Dusts
    public static final DeferredItem<Item> IRON_DUST = ITEMS.registerSimpleItem("iron_dust");
    public static final DeferredItem<Item> GOLD_DUST = ITEMS.registerSimpleItem("gold_dust");
    public static final DeferredItem<Item> COPPER_DUST = ITEMS.registerSimpleItem("copper_dust");
    public static final DeferredItem<Item> TIN_DUST = ITEMS.registerSimpleItem("tin_dust");
    public static final DeferredItem<Item> SILVER_DUST = ITEMS.registerSimpleItem("silver_dust");
    public static final DeferredItem<Item> LEAD_DUST = ITEMS.registerSimpleItem("lead_dust");
    public static final DeferredItem<Item> ALUMINUM_DUST = ITEMS.registerSimpleItem("aluminum_dust");
    public static final DeferredItem<Item> ZINC_DUST = ITEMS.registerSimpleItem("zinc_dust");
    public static final DeferredItem<Item> MAGNESIUM_DUST = ITEMS.registerSimpleItem("magnesium_dust");

    // Basic Ingots & Alloys
    public static final DeferredItem<Item> TIN_INGOT = ITEMS.registerSimpleItem("tin_ingot");
    public static final DeferredItem<Item> SILVER_INGOT = ITEMS.registerSimpleItem("silver_ingot");
    public static final DeferredItem<Item> LEAD_INGOT = ITEMS.registerSimpleItem("lead_ingot");
    public static final DeferredItem<Item> ALUMINUM_INGOT = ITEMS.registerSimpleItem("aluminum_ingot");
    public static final DeferredItem<Item> ZINC_INGOT = ITEMS.registerSimpleItem("zinc_ingot");
    public static final DeferredItem<Item> MAGNESIUM_INGOT = ITEMS.registerSimpleItem("magnesium_ingot");
    public static final DeferredItem<Item> STEEL_INGOT = ITEMS.registerSimpleItem("steel_ingot");
    public static final DeferredItem<Item> BRONZE_INGOT = ITEMS.registerSimpleItem("bronze_ingot");
    public static final DeferredItem<Item> BRASS_INGOT = ITEMS.registerSimpleItem("brass_ingot");
    public static final DeferredItem<Item> DURALUMIN_INGOT = ITEMS.registerSimpleItem("duralumin_ingot");
    public static final DeferredItem<Item> SOLDER_INGOT = ITEMS.registerSimpleItem("solder_ingot");
    public static final DeferredItem<Item> DAMASCUS_STEEL_INGOT = ITEMS.registerSimpleItem("damascus_steel_ingot");
    public static final DeferredItem<Item> HARDENED_METAL = ITEMS.registerSimpleItem("hardened_metal");
    public static final DeferredItem<Item> REINFORCED_ALLOY_INGOT = ITEMS.registerSimpleItem("reinforced_alloy_ingot");

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
