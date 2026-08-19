package com.zynex.gundrop.client;

import com.zynex.gundrop.entity.BulletEntity;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.state.EntityRenderState;

/**
 * Minimal 1.21.11-compatible renderer using the EntityRenderState pipeline.
 * Visual tracers can be re-added later via OrderedRenderCommandQueue.
 */
public class BulletEntityRenderer extends EntityRenderer<BulletEntity, EntityRenderState> {

	public BulletEntityRenderer(EntityRendererFactory.Context context) {
		super(context);
	}

	@Override
	public EntityRenderState createRenderState() {
		return new EntityRenderState();
	}
}
