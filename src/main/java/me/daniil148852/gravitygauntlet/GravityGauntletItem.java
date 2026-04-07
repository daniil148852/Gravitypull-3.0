package me.daniil148852.gravitygauntlet;

import me.daniil148852.gravitygauntlet.OrbitingFallingBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.FallingBlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

public class GravityGauntletItem extends Item {
	public static final List<OrbitingBlock> ORBITING_BLOCKS = new ArrayList<>();

	public GravityGauntletItem(Settings settings) {
		super(settings);
	}

	@Override
	public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
		if (user.isSneaking()) {
			shootBlocks(user);
			return TypedActionResult.success(user.getStackInHand(hand));
		}
		return TypedActionResult.pass(user.getStackInHand(hand));
	}

	@Override
	public TypedActionResult<ItemStack> useOnBlock(ItemStack stack, PlayerEntity user, Hand hand, BlockHitResult hitResult) {
		if (!user.isSneaking()) {
			World world = user.getWorld();
			BlockPos pos = hitResult.getBlockPos();
			BlockState state = world.getBlockState(pos);

			if (!state.isAir() && state.getBlock() != Blocks.BEDROCK) {
				world.removeBlock(pos, false);

				FallingBlockEntity fallingBlock = new FallingBlockEntity(world,
					pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, state);
				fallingBlock.setNoGravity(true);
				fallingBlock.dropItem = false;
				fallingBlock.hurtEntities = true;
				fallingBlock.fallDistance = 8.0f;

			world.spawnEntity(fallingBlock);

			((OrbitingFallingBlock) fallingBlock).gravityGauntlet$setOrbiting(
				user.getUuid(), Math.random() * Math.PI * 2
			);

			OrbitingBlock orbitingBlock = new OrbitingBlock();
			orbitingBlock.entity = fallingBlock;
			orbitingBlock.owner = user.getUuid();
			orbitingBlock.angle = Math.random() * Math.PI * 2;
			ORBITING_BLOCKS.add(orbitingBlock);

				return TypedActionResult.success(stack);
			}
		}
		return super.useOnBlock(stack, user, hand, hitResult);
	}

	private void shootBlocks(PlayerEntity user) {
		Iterator<OrbitingBlock> iterator = ORBITING_BLOCKS.iterator();
		Vec3d look = user.getRotationVec(1.0f);

		while (iterator.hasNext()) {
			OrbitingBlock orbitingBlock = iterator.next();
			if (orbitingBlock.owner.equals(user.getUuid())) {
				FallingBlockEntity entity = orbitingBlock.entity;
				if (entity != null && !entity.isRemoved()) {
					entity.setNoGravity(false);
					entity.setVelocity(
						look.x * 2.0,
						look.y * 2.0 + 0.3,
						look.z * 2.0
					);
					entity.velocityModified = true;
					entity.hurtEntities = true;
					entity.fallDistance = 15.0f;

					NbtCompound nbt = new NbtCompound();
					nbt.putBoolean("Launched", true);
					entity.writeCustomDataToNbt(nbt);
				}
				iterator.remove();
			}
		}
	}

	public static class OrbitingBlock {
		public FallingBlockEntity entity;
		public UUID owner;
		public double angle;
	}
}
