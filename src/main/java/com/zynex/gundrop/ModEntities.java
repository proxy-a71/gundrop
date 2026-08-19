package com.zynex.gundrop;

import com.zynex.gundrop.entity.BulletEntity;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;

public final class ModEntities {
	public static EntityType<BulletEntity> BULLET;

	private ModEntities() {}

	public static void register() {
		Identifier id = Identifier.of(GunDrop.MOD_ID, "bullet");
		RegistryKey<EntityType<?>> key = RegistryKey.of(RegistryKeys.ENTITY_TYPE, id);
		BULLET = Registry.register(Registries.ENTITY_TYPE, id,
				EntityType.Builder.<BulletEntity>create(BulletEntity::new, SpawnGroup.MISC)
						.dimensions(EntityDimensions.fixed(0.25f, 0.25f))
						.maxTrackingRange(64)
						.trackingTickInterval(1)
						.build(key));
	}
}
