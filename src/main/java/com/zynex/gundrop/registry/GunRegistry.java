package com.zynex.gundrop.registry;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.zynex.gundrop.GunData;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Loads gun definitions bundled as classpath resources under /guns/.
 * manifest.json lists every gun id; each id maps to guns/<id>.json.
 * This is intentionally data-driven so new guns can be added without
 * touching Java code at all.
 */
public final class GunRegistry {
	private static final Gson GSON = new Gson();
	private static final Map<String, GunData> GUNS = new LinkedHashMap<>();

	private GunRegistry() {}

	public static void load() {
		GUNS.clear();
		JsonArray manifest = readJsonArray("/guns/manifest.json");
		for (var el : manifest) {
			String id = el.getAsString();
			GunData data = readGun(id);
			if (data != null) {
				GUNS.put(id, data);
			}
		}
	}

	private static GunData readGun(String id) {
		JsonObject o = readJsonObject("/guns/" + id + ".json");
		if (o == null) return null;
		return new GunData(
				id,
				o.get("displayName").getAsString(),
				o.get("category").getAsString(),
				o.get("damage").getAsFloat(),
				o.has("pellets") ? o.get("pellets").getAsInt() : 1,
				o.get("magazineSize").getAsInt(),
				o.get("fireRateTicks").getAsInt(),
				o.get("reloadTicks").getAsInt(),
				o.get("projectileSpeed").getAsDouble(),
				o.get("spreadDegrees").getAsFloat(),
				o.get("automatic").getAsBoolean(),
				o.get("recoil").getAsFloat(),
				o.has("explosive") && o.get("explosive").getAsBoolean(),
				o.get("dropWeight").getAsInt()
		);
	}

	private static JsonObject readJsonObject(String path) {
		try (InputStream in = GunRegistry.class.getResourceAsStream(path)) {
			if (in == null) throw new IOException("Missing resource " + path);
			return GSON.fromJson(new InputStreamReader(in, StandardCharsets.UTF_8), JsonObject.class);
		} catch (IOException e) {
			throw new RuntimeException("Failed loading gun data " + path, e);
		}
	}

	private static JsonArray readJsonArray(String path) {
		try (InputStream in = GunRegistry.class.getResourceAsStream(path)) {
			if (in == null) throw new IOException("Missing resource " + path);
			return GSON.fromJson(new InputStreamReader(in, StandardCharsets.UTF_8), JsonArray.class);
		} catch (IOException e) {
			throw new RuntimeException("Failed loading gun manifest " + path, e);
		}
	}

	public static Map<String, GunData> all() {
		return GUNS;
	}
}
