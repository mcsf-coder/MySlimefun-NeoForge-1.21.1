package com.zeta.myslimefun;

import net.minecraft.data.recipes.ShapelessRecipeBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.SimpleCookingRecipeBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.model.generators.ItemModelProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.data.event.GatherDataEvent;

import java.util.concurrent.CompletableFuture;

@EventBusSubscriber(modid = MySlimefun.MODID)
public class ModDataGenerators {

    @SubscribeEvent
    public static void gatherData(GatherDataEvent event) {
        DataGenerator generator = event.getGenerator();
        PackOutput packOutput = generator.getPackOutput();
        ExistingFileHelper existingFileHelper = event.getExistingFileHelper();
        CompletableFuture<HolderLookup.Provider> lookupProvider = event.getLookupProvider();

        // 1. Auto-generate item models using vanilla textures as starter visuals
        generator.addProvider(event.includeClient(), new ItemModelProvider(packOutput, MySlimefun.MODID, existingFileHelper) {
            @Override
            protected void registerModels() {
                // Guide Book
                withExistingParent("slimefun_guide", mcLoc("item/generated"))
                        .texture("layer0", mcLoc("item/enchanted_book"));

                // Dusts
                dustModel("iron_dust", "item/gunpowder");
                dustModel("gold_dust", "item/glowstone_dust");
                dustModel("copper_dust", "item/redstone");
                dustModel("tin_dust", "item/sugar");
                dustModel("silver_dust", "item/sugar");
                dustModel("lead_dust", "item/gunpowder");
                dustModel("aluminum_dust", "item/sugar");
                dustModel("zinc_dust", "item/gunpowder");
                dustModel("magnesium_dust", "item/glowstone_dust");

                // Ingots & Alloys
                ingotModel("tin_ingot", "item/iron_ingot");
                ingotModel("silver_ingot", "item/iron_ingot");
                ingotModel("lead_ingot", "item/netherite_ingot");
                ingotModel("aluminum_ingot", "item/iron_ingot");
                ingotModel("zinc_ingot", "item/iron_ingot");
                ingotModel("magnesium_ingot", "item/iron_ingot");
                ingotModel("steel_ingot", "item/iron_ingot");
                ingotModel("bronze_ingot", "item/copper_ingot");
                ingotModel("brass_ingot", "item/gold_ingot");
                ingotModel("duralumin_ingot", "item/iron_ingot");
                ingotModel("solder_ingot", "item/iron_ingot");
                ingotModel("damascus_steel_ingot", "item/netherite_ingot");
                ingotModel("hardened_metal", "item/netherite_ingot");
                ingotModel("reinforced_alloy_ingot", "item/diamond");
            }

            private void dustModel(String name, String vanillaTexture) {
                withExistingParent(name, mcLoc("item/generated"))
                        .texture("layer0", ResourceLocation.parse(vanillaTexture));
            }

            private void ingotModel(String name, String vanillaTexture) {
                withExistingParent(name, mcLoc("item/generated"))
                        .texture("layer0", ResourceLocation.parse(vanillaTexture));
            }
        });

        // 2. Auto-generate Smelting & Blasting recipes for all 9 dusts
        generator.addProvider(event.includeServer(), new RecipeProvider(packOutput, lookupProvider) {
            @Override
            protected void buildRecipes(RecipeOutput output) {
                smeltDust(output, ModItems.IRON_DUST.get(), Items.IRON_INGOT, "iron_from_dust");
                smeltDust(output, ModItems.GOLD_DUST.get(), Items.GOLD_INGOT, "gold_from_dust");
                smeltDust(output, ModItems.COPPER_DUST.get(), Items.COPPER_INGOT, "copper_from_dust");
                smeltDust(output, ModItems.TIN_DUST.get(), ModItems.TIN_INGOT.get(), "tin_ingot");
                smeltDust(output, ModItems.SILVER_DUST.get(), ModItems.SILVER_INGOT.get(), "silver_ingot");
                smeltDust(output, ModItems.LEAD_DUST.get(), ModItems.LEAD_INGOT.get(), "lead_ingot");
                smeltDust(output, ModItems.ALUMINUM_DUST.get(), ModItems.ALUMINUM_INGOT.get(), "aluminum_ingot");
                smeltDust(output, ModItems.ZINC_DUST.get(), ModItems.ZINC_INGOT.get(), "zinc_ingot");
                smeltDust(output, ModItems.MAGNESIUM_DUST.get(), ModItems.MAGNESIUM_INGOT.get(), "magnesium_ingot");
                // Craft Slimefun Guide in Survival: Book + Cobblestone
                ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModItems.SLIMEFUN_GUIDE.get())
                        .requires(Items.BOOK)
                        .requires(Items.COBBLESTONE)
                        .unlockedBy("has_book", has(Items.BOOK))
                        .save(output);

            }

            private void smeltDust(RecipeOutput output, net.minecraft.world.level.ItemLike input, net.minecraft.world.level.ItemLike result, String name) {
                SimpleCookingRecipeBuilder.smelting(Ingredient.of(input), RecipeCategory.MISC, result, 0.7f, 200)
                        .unlockedBy("has_dust", has(input))
                        .save(output, MySlimefun.MODID + ":" + name + "_smelting");

                SimpleCookingRecipeBuilder.blasting(Ingredient.of(input), RecipeCategory.MISC, result, 0.7f, 100)
                        .unlockedBy("has_dust", has(input))
                        .save(output, MySlimefun.MODID + ":" + name + "_blasting");
            }
        });
    }
}
