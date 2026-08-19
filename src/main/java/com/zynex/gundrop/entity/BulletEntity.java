package com.zynex.gundrop.entity;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.util.Uuids;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;

import java.util.Optional;
import java.util.UUID;

/**
 * A fast, gravity-affected bullet. Deliberately NOT extending
 * PersistentProjectileEntity to avoid depending on arrow-specific
 * item/pickup plumbing that varies between Minecraft versions.
 */
public class BulletEntity extends Entity {
	private float damage = 4f;
	private boolean explosive = false;
	private int age = 0;
	private static final int MAX_AGE = 60; // 3 seconds of flight, then despawn
	private static final double GRAVITY = 0.02;

	private UUID ownerUuid;
	private transient LivingEntity ownerCache;

	public BulletEntity(EntityType<? extends BulletEntity> type, World world) {
		super(type, world);
	}

	public void setOwner(LivingEntity owner) {
		this.ownerCache = owner;
		this.ownerUuid = owner.getUuid();
	}

	public void setDamage(float damage) {
		this.damage = damage;
	}

	public void setExplosive(boolean explosive) {
		this.explosive = explosive;
	}

	/** speed in blocks/tick, spreadDegrees is a cone of random inaccuracy */
	public void setVelocity(double dx, double dy, double dz, float speed, float spreadDegrees) {
		Vec3d dir = new Vec3d(dx, dy, dz).normalize();
		if (spreadDegrees > 0) {
			double rad = Math.toRadians(spreadDegrees);
			double ox = (this.random.nextDouble() - 0.5) * 2 * rad;
			double oy = (this.random.nextDouble() - 0.5) * 2 * rad;
			dir = dir.rotateY((float) ox).rotateX((float) oy);
		}
		Vec3d vel = dir.multiply(speed);
		this.setVelocity(vel);
		this.velocityDirty = true;
		double horizontalDist = Math.sqrt(vel.x * vel.x + vel.z * vel.z);
		this.setYaw((float) (Math.toDegrees(Math.atan2(vel.x, vel.z))));
		this.setPitch((float) (Math.toDegrees(Math.atan2(vel.y, horizontalDist))));
	}

	@Override
	protected void initDataTracker(DataTracker.Builder builder) {
		// no synced fields needed; position/velocity sync automatically
	}

	@Override
	protected void readCustomData(ReadView view) {
		this.damage = view.getFloat("Damage", 4f);
		this.explosive = view.getBoolean("Explosive", false);
		Optional<UUID> opt = view.read("Owner", Uuids.INT_STREAM_CODEC);
		this.ownerUuid = opt.orElse(null);
	}

	@Override
	protected void writeCustomData(WriteView view) {
		view.putFloat("Damage", damage);
		view.putBoolean("Explosive", explosive);
		if (ownerUuid != null) {
			view.put("Owner", Uuids.INT_STREAM_CODEC, ownerUuid);
		}
	}

	@Override
	public boolean damage(ServerWorld world, DamageSource source, float amount) {
		return false; // bullets are not damageable
	}

	@Override
	public void tick() {
		super.tick();
		age++;
		if (age > MAX_AGE) {
			this.discard();
			return;
		}

		Vec3d start = this.getEntityPos();
		Vec3d motion = this.getVelocity();
		Vec3d end = start.add(motion);

		World world = this.getEntityWorld();
		if (world.isClient()) {
			world.addParticleClient(ParticleTypes.CRIT, start.x, start.y, start.z, 0.0, 0.0, 0.0);
		}

		HitResult hit = raycast(start, end);
		if (hit != null && hit.getType() != HitResult.Type.MISS) {
			onHit(hit);
			return;
		}

		this.setPosition(end.x, end.y, end.z);
		this.setVelocity(motion.x * 0.995, motion.y - GRAVITY, motion.z * 0.995);
		this.velocityDirty = true;
	}

	private HitResult raycast(Vec3d start, Vec3d end) {
		World world = this.getEntityWorld();
		BlockHitResult blockHit = world.raycast(new RaycastContext(
				start, end, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, this));
		double blockDist = blockHit.getType() == HitResult.Type.MISS ? Double.MAX_VALUE : blockHit.getPos().distanceTo(start);

		EntityHitResult entityHit = null;
		double entityDist = Double.MAX_VALUE;
		Box searchBox = this.getBoundingBox().stretch(this.getVelocity()).expand(1.0);
		for (Entity other : world.getOtherEntities(this, searchBox,
				e -> !e.isSpectator() && e.isAlive() && (e instanceof LivingEntity) && e != getOwner())) {
			Box box = other.getBoundingBox().expand(0.3);
			var opt = box.raycast(start, end);
			if (opt.isPresent()) {
				double d = opt.get().distanceTo(start);
				if (d < entityDist) {
					entityDist = d;
					entityHit = new EntityHitResult(other, opt.get());
				}
			}
		}

		if (entityHit != null && entityDist <= blockDist) return entityHit;
		if (blockHit.getType() != HitResult.Type.MISS) return blockHit;
		return null;
	}

	private LivingEntity getOwner() {
		if (ownerCache != null) return ownerCache;
		if (ownerUuid != null && this.getEntityWorld() instanceof ServerWorld sw) {
			Entity e = sw.getEntity(ownerUuid);
			if (e instanceof LivingEntity le) {
				ownerCache = le;
				return le;
			}
		}
		return null;
	}

	private void onHit(HitResult hit) {
		if (this.getEntityWorld() instanceof ServerWorld serverWorld) {
			if (explosive) {
				serverWorld.createExplosion(this, this.getX(), this.getY(), this.getZ(), 2.5f,
						World.ExplosionSourceType.MOB);
			} else if (hit instanceof EntityHitResult entityHit && entityHit.getEntity() instanceof LivingEntity target) {
				LivingEntity owner = getOwner();
				DamageSource source = owner instanceof PlayerEntity p
						? this.getDamageSources().playerAttack(p)
						: this.getDamageSources().generic();
				target.damage(serverWorld, source, damage);
				target.setVelocity(target.getVelocity().add(this.getVelocity().multiply(0.15)));
			}
			serverWorld.spawnParticles(ParticleTypes.CRIT, hit.getPos().x, hit.getPos().y, hit.getPos().z,
					6, 0.1, 0.1, 0.1, 0.02);
		}
		this.discard();
	}

	@Override
	public boolean shouldRender(double distance) {
		return distance < 4096.0;
	}
}
