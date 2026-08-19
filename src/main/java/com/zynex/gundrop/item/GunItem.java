package com.zynex.gundrop.item;

import com.zynex.gundrop.GunData;
import com.zynex.gundrop.ModEntities;
import com.zynex.gundrop.ModSounds;
import com.zynex.gundrop.entity.BulletEntity;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.UseAction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;

import java.util.List;

public class GunItem extends Item {
	public static final String NBT_AMMO = "GunDropAmmo";
	public static final String NBT_RELOADING = "GunDropReloading";

	private final GunData data;

	public GunItem(GunData data, Settings settings) {
		super(settings.maxCount(1).component(
				DataComponentTypes.CUSTOM_DATA,
				NbtComponent.of(defaultNbt(data))
		));
		this.data = data;
	}

	private static NbtCompound defaultNbt(GunData data) {
		NbtCompound nbt = new NbtCompound();
		nbt.putInt(NBT_AMMO, data.magazineSize());
		nbt.putBoolean(NBT_RELOADING, false);
		return nbt;
	}

	public GunData getData() {
		return data;
	}

	public int getAmmo(ItemStack stack) {
		NbtComponent c = stack.get(DataComponentTypes.CUSTOM_DATA);
		if (c == null) return data.magazineSize();
		return c.copyNbt().getInt(NBT_AMMO).orElse(data.magazineSize());
	}

	public void setAmmo(ItemStack stack, int ammo) {
		NbtComponent c = stack.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT);
		NbtCompound nbt = c.copyNbt();
		nbt.putInt(NBT_AMMO, Math.max(0, ammo));
		stack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(nbt));
	}

	public boolean isReloading(ItemStack stack) {
		NbtComponent c = stack.get(DataComponentTypes.CUSTOM_DATA);
		if (c == null) return false;
		return c.copyNbt().getBoolean(NBT_RELOADING).orElse(false);
	}

	public void setReloading(ItemStack stack, boolean reloading) {
		NbtComponent c = stack.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT);
		NbtCompound nbt = c.copyNbt();
		nbt.putBoolean(NBT_RELOADING, reloading);
		stack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(nbt));
	}

	@Override
	public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
		ItemStack stack = user.getStackInHand(hand);
		if (data.automatic()) {
			user.setCurrentHand(hand);
			return TypedActionResult.consume(stack);
		} else {
			fire(world, user, stack, hand);
			return TypedActionResult.success(stack, world.isClient());
		}
	}

	@Override
	public void usageTick(World world, LivingEntity user, ItemStack stack, int remainingUseTicks) {
		if (!(user instanceof PlayerEntity player)) return;
		if (!data.automatic()) return;
		int elapsed = getMaxUseTime(stack, user) - remainingUseTicks;
		if (elapsed >= 0 && data.fireRateTicks() > 0 && elapsed % data.fireRateTicks() == 0) {
			fire(world, player, stack, player.getActiveHand());
		}
	}

	@Override
	public int getMaxUseTime(ItemStack stack, LivingEntity user) {
		return 72000;
	}

	@Override
	public UseAction getUseAction(ItemStack stack) {
		return data.automatic() ? UseAction.SPEAR : UseAction.NONE;
	}

	private void fire(World world, PlayerEntity player, ItemStack stack, Hand hand) {
		if (isReloading(stack)) return;
		if (player.getItemCooldownManager().isCoolingDown(stack)) return;

		int ammo = getAmmo(stack);
		if (ammo <= 0) {
			if (!world.isClient()) {
				world.playSound(null, player.getBlockPos(), ModSounds.event(data.emptySoundId()),
						SoundCategory.PLAYERS, 0.8f, 1.1f);
			}
			player.getItemCooldownManager().set(stack, 6);
			return;
		}

		player.getItemCooldownManager().set(stack, Math.max(1, data.fireRateTicks()));
		setAmmo(stack, ammo - 1);
		player.swingHand(hand, true);

		if (!world.isClient() && world instanceof ServerWorld serverWorld) {
			world.playSound(null, player.getBlockPos(), ModSounds.event(data.fireSoundId()),
					SoundCategory.PLAYERS, 1.4f, 1.0f);

			Random random = world.getRandom();
			int pellets = Math.max(1, data.pellets());
			for (int i = 0; i < pellets; i++) {
				BulletEntity bullet = new BulletEntity(ModEntities.BULLET, serverWorld);
				bullet.setOwner(player);
				bullet.setDamage(data.damage());
				bullet.setExplosive(data.explosive());
				bullet.setPosition(player.getX(), player.getEyeY() - 0.1, player.getZ());

				float spread = data.spreadDegrees();
				double dx = -Math.sin(Math.toRadians(player.getYaw())) * Math.cos(Math.toRadians(player.getPitch()));
				double dy = -Math.sin(Math.toRadians(player.getPitch()));
				double dz = Math.cos(Math.toRadians(player.getYaw())) * Math.cos(Math.toRadians(player.getPitch()));
				bullet.setVelocity(dx, dy, dz, (float) data.projectileSpeed(), spread);
				serverWorld.spawnEntity(bullet);
			}

			if (data.recoil() > 0) {
				player.addVelocity(
						-Math.sin(Math.toRadians(player.getYaw())) * data.recoil() * 0.01,
						0.0,
						Math.cos(Math.toRadians(player.getYaw())) * data.recoil() * -0.01
				);
				player.velocityModified = true;
			}
		}
	}

	public boolean tryReload(World world, PlayerEntity player, ItemStack stack) {
		if (isReloading(stack)) return false;
		if (getAmmo(stack) >= data.magazineSize()) return false;
		setReloading(stack, true);
		player.getItemCooldownManager().set(stack, data.reloadTicks());
		if (!world.isClient()) {
			world.playSound(null, player.getBlockPos(), ModSounds.event(data.reloadSoundId()),
					SoundCategory.PLAYERS, 1.0f, 1.0f);
		}
		return true;
	}

	@Override
	public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
		tooltip.add(Text.translatable("item.gundrop.tooltip.category", data.category()));
		tooltip.add(Text.translatable("item.gundrop.tooltip.ammo", getAmmo(stack), data.magazineSize()));
		tooltip.add(Text.translatable("item.gundrop.tooltip.damage", data.damage()));
		if (data.automatic()) {
			tooltip.add(Text.translatable("item.gundrop.tooltip.auto"));
		}
		tooltip.add(Text.translatable("item.gundrop.tooltip.reload"));
	}
}
