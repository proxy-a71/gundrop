package com.zynex.gundrop;

import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.Map;

/**
 * Registers one SoundEvent per gun category (see assets/gundrop/sounds.json
 * for the matching .ogg files) plus two shared utility sounds.
 */
public final class ModSounds {
	private static final Map<String, SoundEvent> EVENTS = new HashMap<>();

	private ModSounds() {}

	public static final String[] CATEGORIES = {
			"pistol", "revolver", "smg", "rifle", "shotgun", "sniper", "rocket"
	};

	public static void register() {
		for (String category : CATEGORIES) {
			registerOne("fire_" + category);
		}
		registerOne("reload_click");
		registerOne("dry_fire");
	}

	private static void registerOne(String path) {
		Identifier id = Identifier.of(GunDrop.MOD_ID, path);
		SoundEvent event = SoundEvent.of(id);
		Registry.register(Registries.SOUND_EVENT, id, event);
		EVENTS.put("gundrop:" + path, event);
	}

	public static SoundEvent event(String fullId) {
		SoundEvent e = EVENTS.get(fullId);
		if (e == null) throw new IllegalArgumentException("Unknown sound " + fullId);
		return e;
	}
}
