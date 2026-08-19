package com.zynex.gundrop;

/**
 * Immutable stat block for one firearm. Loaded from a resource JSON file
 * (see /guns/*.json). To add a new gun you do NOT need to touch any Java
 * code — just add another JSON entry and list it in guns/manifest.json.
 */
public record GunData(
		String id,
		String displayName,
		String category,
		float damage,
		int pellets,
		int magazineSize,
		int fireRateTicks,
		int reloadTicks,
		double projectileSpeed,
		float spreadDegrees,
		boolean automatic,
		float recoil,
		boolean explosive,
		int dropWeight
) {
	public String fireSoundId() {
		return "gundrop:fire_" + category;
	}

	public String reloadSoundId() {
		return "gundrop:reload_click";
	}

	public String emptySoundId() {
		return "gundrop:dry_fire";
	}
}
