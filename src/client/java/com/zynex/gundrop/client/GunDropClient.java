package com.zynex.gundrop.client;

import com.mojang.blaze3d.platform.InputUtil;
import com.zynex.gundrop.ModEntities;
import com.zynex.gundrop.item.GunItem;
import com.zynex.gundrop.network.ReloadPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.item.ItemStack;

public class GunDropClient implements ClientModInitializer {
	private static KeyBinding reloadKey;

	@Override
	public void onInitializeClient() {
		EntityRendererRegistry.register(ModEntities.BULLET, BulletEntityRenderer::new);

		reloadKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
				"key.gundrop.reload",
				InputUtil.Type.KEYSYM,
				InputUtil.GLFW_KEY_R,
				"category.gundrop.guns"
		));

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			ClientPlayerEntity player = client.player;
			if (player == null) return;
			while (reloadKey.wasPressed()) {
				ItemStack main = player.getMainHandStack();
				if (main.getItem() instanceof GunItem) {
					ClientPlayNetworking.send(new ReloadPayload());
				}
			}
		});
	}
}
