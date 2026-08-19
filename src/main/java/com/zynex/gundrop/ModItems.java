package com.zynex.gundrop;

import com.zynex.gundrop.item.GunItem;
import com.zynex.gundrop.registry.GunRegistry;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.LinkedHashMap;
import java.util.Map;

public final class ModItems {
	public static final Map<String, GunItem> GUNS = new LinkedHashMap<>();
	public static ItemGroup GUN_GROUP;

	private ModItems() {}

	public static void register() {
		for (GunData data : GunRegistry.all().values()) {
			Identifier id = Identifier.of(GunDrop.MOD_ID, data.id());
			RegistryKey<Item> key = RegistryKey.of(RegistryKeys.ITEM, id);
			GunItem item = new GunItem(data, new Item.Settings().registryKey(key).maxDamage(0));
			Registry.register(Registries.ITEM, id, item);
			GUNS.put(data.id(), item);
		}

		GUN_GROUP = Registry.register(Registries.ITEM_GROUP, Identifier.of(GunDrop.MOD_ID, "guns"),
				FabricItemGroup.builder()
						.displayName(Text.translatable("itemGroup.gundrop.guns"))
						.icon(() -> new ItemStack(GUNS.values().iterator().next()))
						.entries((ctx, entries) -> {
							for (GunItem gun : GUNS.values()) {
								entries.add(gun);
							}
						})
						.build());
	}
}
