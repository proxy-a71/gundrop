package com.zynex.gundrop.network;

import com.zynex.gundrop.GunDrop;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/** Sent by the client when the reload key is pressed while holding a gun. */
public record ReloadPayload() implements CustomPayload {
	public static final CustomPayload.Id<ReloadPayload> ID =
			new CustomPayload.Id<>(Identifier.of(GunDrop.MOD_ID, "reload"));
	public static final PacketCodec<net.minecraft.network.PacketByteBuf, ReloadPayload> CODEC =
			PacketCodec.unit(new ReloadPayload());

	@Override
	public Id<? extends CustomPayload> getId() {
		return ID;
	}
}
