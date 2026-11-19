package com.zeta.myslimefun;

import io.netty.buffer.ByteBuf;
import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.FireworkExplosion;
import net.minecraft.world.item.component.Fireworks;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import java.util.ArrayList;
import java.util.List;

@EventBusSubscriber(modid = MySlimefun.MODID)
public class SlimefunNetwork {

    // Packet 1: Client -> Server (Player clicked a locked recipe to spend XP)
    public record UnlockRecipePayload(String recipeId) implements CustomPacketPayload {
        public static final Type<UnlockRecipePayload> TYPE =
                new Type<>(ResourceLocation.fromNamespaceAndPath(MySlimefun.MODID, "unlock_recipe"));

        public static final StreamCodec<ByteBuf, UnlockRecipePayload> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, UnlockRecipePayload::recipeId,
                UnlockRecipePayload::new
        );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    // Packet 2: Server -> Client (Syncs unlocked list so the Guide UI updates live)
    public record SyncUnlocksPayload(List<String> unlockedIds) implements CustomPacketPayload {
        public static final Type<SyncUnlocksPayload> TYPE =
                new Type<>(ResourceLocation.fromNamespaceAndPath(MySlimefun.MODID, "sync_unlocks"));

        public static final StreamCodec<ByteBuf, SyncUnlocksPayload> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()), SyncUnlocksPayload::unlockedIds,
                SyncUnlocksPayload::new
        );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    @SubscribeEvent
    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToServer(UnlockRecipePayload.TYPE, UnlockRecipePayload.STREAM_CODEC, SlimefunNetwork::handleServerUnlock);
        registrar.playToClient(SyncUnlocksPayload.TYPE, SyncUnlocksPayload.STREAM_CODEC, SlimefunNetwork::handleClientSync);
    }

    private static void handleServerUnlock(UnlockRecipePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                MultiblockHandler.EnhancedRecipe recipe = MultiblockHandler.getRecipeById(payload.recipeId());
                if (recipe == null) return;

                List<String> unlocked = new ArrayList<>(player.getData(ModAttachments.UNLOCKED_RECIPES));
                if (unlocked.contains(recipe.id())) return;

                if (player.experienceLevel >= recipe.xpCost() || player.isCreative()) {
                    if (!player.isCreative()) {
                        player.giveExperienceLevels(-recipe.xpCost());
                    }
                    unlocked.add(recipe.id());
                    player.setData(ModAttachments.UNLOCKED_RECIPES, unlocked);

                    // Sync updated list back to client
                    PacketDistributor.sendToPlayer(player, new SyncUnlocksPayload(unlocked));

                    // Spawn celebratory Slimefun firework!
                    spawnUnlockFirework(player);

                    player.level().playSound(null, player.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.8f, 1.2f);
                    player.displayClientMessage(Component.literal("Unlocked research: ")
                            .withStyle(ChatFormatting.GREEN)
                            .append(recipe.result().getHoverName()), true);
                } else {
                    player.displayClientMessage(Component.literal("Not enough XP! Need " + recipe.xpCost() + " levels.")
                            .withStyle(ChatFormatting.RED), true);
                }
            }
        });
    }

    private static void spawnUnlockFirework(ServerPlayer player) {
        ItemStack fireworkStack = new ItemStack(Items.FIREWORK_ROCKET);
        FireworkExplosion explosion = new FireworkExplosion(
                FireworkExplosion.Shape.LARGE_BALL,
                IntList.of(0x55FF55, 0xFFFF55), // Slimefun Lime Green & Gold
                IntList.of(0xFFFFFF),
                true,
                true
        );
        fireworkStack.set(DataComponents.FIREWORKS, new Fireworks(1, List.of(explosion)));

        FireworkRocketEntity rocket = new FireworkRocketEntity(
                player.level(),
                player.getX(),
                player.getY() + 1.5,
                player.getZ(),
                fireworkStack
        );
        player.level().addFreshEntity(rocket);
    }

    private static void handleClientSync(SyncUnlocksPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            Player player = context.player();
            if (player != null) {
                player.setData(ModAttachments.UNLOCKED_RECIPES, new ArrayList<>(payload.unlockedIds()));
            }
        });
    }

    // Give Slimefun Guide on first join + sync unlocked recipes
    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            // Sync unlocked recipes
            PacketDistributor.sendToPlayer(serverPlayer, new SyncUnlocksPayload(serverPlayer.getData(ModAttachments.UNLOCKED_RECIPES)));

            // Give free Slimefun Guide on first join
            if (!serverPlayer.getData(ModAttachments.RECEIVED_GUIDE)) {
                ItemStack guide = new ItemStack(ModItems.SLIMEFUN_GUIDE.get());
                if (!serverPlayer.getInventory().add(guide)) {
                    serverPlayer.drop(guide, false);
                }
                serverPlayer.setData(ModAttachments.RECEIVED_GUIDE, true);
                serverPlayer.displayClientMessage(Component.literal("Welcome! You received your Slimefun Guide.")
                        .withStyle(ChatFormatting.GREEN), false);
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            PacketDistributor.sendToPlayer(serverPlayer, new SyncUnlocksPayload(serverPlayer.getData(ModAttachments.UNLOCKED_RECIPES)));
        }
    }
}
