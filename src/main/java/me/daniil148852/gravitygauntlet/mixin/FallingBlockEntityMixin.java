package me.daniil148852.gravitygauntlet.mixin;

import me.daniil148852.gravitygauntlet.GravityGauntletItem;
import me.daniil148852.gravitygauntlet.GravityGauntletItem.OrbitingBlock;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.FallingBlockEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Iterator;
import java.util.UUID;

@Mixin(FallingBlockEntity.class)
public abstract class FallingBlockEntityMixin extends Entity {
	private UUID gravityGauntlet$owner;
	private double gravityGauntlet$angle;
	private boolean gravityGauntlet$isOrbiting;

	public FallingBlockEntityMixin(EntityType<?> type, World world) {
		super(type, world);
	}

	public static void initialize() {
	}

	@Inject(method = "tick", at = @At("HEAD"), cancellable = true)
	private void gravityGauntlet$onTick(CallbackInfo ci) {
		if (gravityGauntlet$isOrbiting && gravityGauntlet$owner != null) {
			World world = this.getWorld();
			if (world.isClient) return;

			PlayerEntity owner = world.getPlayerByUuid(gravityGauntlet$owner);
			if (owner == null || owner.isRemoved()) {
				this.discard();
				return;
			}

			gravityGauntlet$angle += 0.05;

			double radius = 3.0;
			double x = owner.getX() + Math.cos(gravityGauntlet$angle) * radius;
			double y = owner.getY() + 1.5;
			double z = owner.getZ() + Math.sin(gravityGauntlet$angle) * radius;

			this.setPosition(x, y, z);
			this.setVelocity(0, 0, 0);
			this.setNoGravity(true);

			ci.cancel();
		}
	}

	@Inject(method = "writeCustomDataToNbt", at = @At("RETURN"))
	private void gravityGauntlet$writeCustomNbt(NbtCompound nbt, CallbackInfo ci) {
		if (gravityGauntlet$owner != null) {
			nbt.putUuid("Owner", gravityGauntlet$owner);
			nbt.putDouble("Angle", gravityGauntlet$angle);
			nbt.putBoolean("IsOrbiting", gravityGauntlet$isOrbiting);
		}
	}

	@Inject(method = "readCustomDataFromNbt", at = @At("RETURN"))
	private void gravityGauntlet$readCustomNbt(NbtCompound nbt, CallbackInfo ci) {
		if (nbt.containsUuid("Owner")) {
			gravityGauntlet$owner = nbt.getUuid("Owner");
			gravityGauntlet$angle = nbt.getDouble("Angle");
			gravityGauntlet$isOrbiting = nbt.getBoolean("IsOrbiting");
		}
	}

	@Inject(method = "onEntityCollision", at = @At("HEAD"))
	private void gravityGauntlet$onCollision(World world, FallingBlockEntity entity, Entity other, CallbackInfo ci) {
		if (!world.isClient && other instanceof LivingEntity target) {
			NbtCompound nbt = entity.getNbt();
			if (nbt != null && nbt.getBoolean("Launched")) {
				target.damage(world.getDamageSources().fallingBlock(entity), 10.0f);
			}
		}
	}

	public void setOrbiting(UUID owner, double angle) {
		this.gravityGauntlet$owner = owner;
		this.gravityGauntlet$angle = angle;
		this.gravityGauntlet$isOrbiting = true;
	}
}
