package com.zeta.myslimefun;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MySlimefun.MODID);

    public static final Supplier<CreativeModeTab> SLIMEFUN_TAB = CREATIVE_MODE_TABS.register("slimefun_tab",
            () -> CreativeModeTab.builder()
                    .icon(() -> new ItemStack(ModItems.SLIMEFUN_GUIDE.get()))
                    .title(Component.translatable("creativetab.myslimefun.slimefun_tab"))
                    .displayItems((parameters, output) -> {
                        output.accept(ModItems.SLIMEFUN_GUIDE.get());

                        output.accept(ModItems.IRON_DUST.get());
                        output.accept(ModItems.GOLD_DUST.get());
                        output.accept(ModItems.COPPER_DUST.get());
                        output.accept(ModItems.TIN_DUST.get());
                        output.accept(ModItems.SILVER_DUST.get());
                        output.accept(ModItems.LEAD_DUST.get());
                        output.accept(ModItems.ALUMINUM_DUST.get());
                        output.accept(ModItems.ZINC_DUST.get());
                        output.accept(ModItems.MAGNESIUM_DUST.get());

                        output.accept(ModItems.TIN_INGOT.get());
                        output.accept(ModItems.SILVER_INGOT.get());
                        output.accept(ModItems.LEAD_INGOT.get());
                        output.accept(ModItems.ALUMINUM_INGOT.get());
                        output.accept(ModItems.ZINC_INGOT.get());
                        output.accept(ModItems.MAGNESIUM_INGOT.get());
                        output.accept(ModItems.STEEL_INGOT.get());
                        output.accept(ModItems.BRONZE_INGOT.get());
                        output.accept(ModItems.BRASS_INGOT.get());
                        output.accept(ModItems.DURALUMIN_INGOT.get());
                        output.accept(ModItems.SOLDER_INGOT.get());
                        output.accept(ModItems.DAMASCUS_STEEL_INGOT.get());
                        output.accept(ModItems.HARDENED_METAL.get());
                        output.accept(ModItems.REINFORCED_ALLOY_INGOT.get());
                    })
                    .build());

    public static void register(IEventBus eventBus) {
        CREATIVE_MODE_TABS.register(eventBus);
    }
}
