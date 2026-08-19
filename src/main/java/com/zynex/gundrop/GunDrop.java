package com.zynex.gundrop;

import com.zynex.gundrop.item.GunItem;
import com.zynex.gundrop.network.ReloadPayload;
import com.zynex.gundrop.registry.GunRegistry;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.loot.v3.LootTableEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.entry.EmptyEntry;
import net.minecraft.loot.entry.ItemEntry;
import net.minecraft.loot.provider.number.ConstantLootNumberProvider;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GunDrop implements ModInitializer {
	public static final String MOD_ID = "gundrop";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	private static final Identifier DIRT_LOOT = Identifier.ofVanilla("blocks/dirt");
	private static final Identifier GRASS_LOOT = Identifier.ofVanilla("blocks/grass_block");

	/** Total loot-table "weight" given to nothing dropping. Guns share a small slice against this. */
	private static final int NOTHING_WEIGHT = 9800;

	@Override
	public void onInitialize() {
		LOGGER.info("[GunDrop] Loading gun definitions...");
		GunRegistry.load();
		LOGGER.info("[GunDrop] Loaded {} guns", GunRegistry.all().size());

		ModSounds.register();
		ModEntities.register();
		ModItems.register();

		hookLootTables();
		hookReloadCompletion();
		hookReloadNetworking();

		LOGGER.info("[GunDrop] Ready. Break dirt or grass for a chance at a firearm.");
	}

	private void hookLootTables() {
		LootTableEvents.MODIFY.register((key, tableBuilder, source, wrapperLookup) -> {
			if (key.getRegistryKey() != RegistryKeys.LOOT_TABLE) return;
			Identifier id = key.getValue();
			if (!id.equals(DIRT_LOOT) && !id.equals(GRASS_LOOT)) return;

			var pool = net.minecraft.loot.LootPool.builder()
					.rolls(ConstantLootNumberProvider.create(1))
					.with(EmptyEntry.builder().weight(NOTHING_WEIGHT));

			for (GunItem gun : ModItems.GUNS.values()) {
				pool.with(ItemEntry.builder(gun).weight(gun.getData().dropWeight()));
			}

			tableBuilder.pool(pool);
		});
	}

	/** Every server tick, finish reloads whose cooldown has elapsed. */
	private void hookReloadCompletion() {
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
				checkHand(player, Hand.MAIN_HAND);
				checkHand(player, Hand.OFF_HAND);
			}
		});
	}

	private void hookReloadNetworking() {
		PayloadTypeRegistry.playC2S().register(ReloadPayload.ID, ReloadPayload.CODEC);
		ServerPlayNetworking.registerGlobalReceiver(ReloadPayload.ID, (payload, context) -> {
			context.server().execute(() -> {
				ServerPlayerEntity player = context.player();
				ItemStack stack = player.getMainHandStack();
				if (stack.getItem() instanceof GunItem gun) {
					gun.tryReload(player.getWorld(), player, stack);
				}
			});
		});
	}

	private void checkHand(PlayerEntity player, Hand hand) {
		ItemStack stack = player.getStackInHand(hand);
		if (!(stack.getItem() instanceof GunItem gun)) return;
		if (!gun.isReloading(stack)) return;
		if (player.getItemCooldownManager().isCoolingDown(stack)) return;

		gun.setAmmo(stack, gun.getData().magazineSize());
		gun.setReloading(stack, false);
	}
}
