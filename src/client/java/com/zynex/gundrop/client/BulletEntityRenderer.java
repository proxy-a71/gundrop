package com.zynex.gundrop.client;

import com.zynex.gundrop.entity.BulletEntity;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

/**
 * Minimal renderer: a thin glowing streak so the bullet reads clearly at
 * speed. Swap the texture / add a proper tracer model later if you want.
 */
public class BulletEntityRenderer extends EntityRenderer<BulletEntity> {
	private static final Identifier TEXTURE = Identifier.of("gundrop", "textures/entity/bullet.png");

	public BulletEntityRenderer(EntityRendererFactory.Context context) {
		super(context);
	}

	@Override
	public Identifier getTexture(BulletEntity entity) {
		return TEXTURE;
	}

	@Override
	public void render(BulletEntity entity, float yaw, float tickDelta, MatrixStack matrices,
						VertexConsumerProvider vertexConsumers, int light) {
		matrices.push();
		matrices.multiply(new Quaternionf().rotationYXZ(
				(float) Math.toRadians(-entity.getYaw()),
				(float) Math.toRadians(entity.getPitch()),
				0));

		VertexConsumer buffer = vertexConsumers.getBuffer(RenderLayer.getEntityCutout(TEXTURE));
		float len = 0.5f;
		float w = 0.03f;
		Matrix4f mat = matrices.peek().getPositionMatrix();

		vertex(buffer, mat, -w, -w, 0, 0, 0, light);
		vertex(buffer, mat, w, -w, 0, 1, 0, light);
		vertex(buffer, mat, w, w, len, 1, 1, light);
		vertex(buffer, mat, -w, w, len, 0, 1, light);

		matrices.pop();
		super.render(entity, yaw, tickDelta, matrices, vertexConsumers, light);
	}

	private static void vertex(VertexConsumer buffer, Matrix4f mat, float x, float y, float z,
								float u, float v, int light) {
		buffer.vertex(mat, x, y, z)
				.color(255, 240, 200, 255)
				.texture(u, v)
				.overlay(OverlayTexture.DEFAULT_UV)
				.light(light)
				.normal(0, 1, 0);
	}
}
