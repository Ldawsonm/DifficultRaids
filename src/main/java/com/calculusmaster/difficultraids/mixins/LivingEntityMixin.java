package com.calculusmaster.difficultraids.mixins;

import com.calculusmaster.difficultraids.setup.DifficultRaidsItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.projectile.Fireball;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin extends Entity
{
    private LivingEntityMixin(EntityType<?> pEntityType, Level pLevel)
    {
        super(pEntityType, pLevel);
    }

    @Shadow public abstract ItemStack getMainHandItem();
    @Shadow public abstract ItemStack getOffhandItem();
    @Shadow public abstract void setHealth(float pHealth);
    @Shadow public abstract boolean removeAllEffects();
    @Shadow public abstract boolean addEffect(MobEffectInstance pEffectInstance);
    @Shadow public abstract boolean removeEffect(MobEffect pEffect);
    @Shadow public abstract boolean randomTeleport(double p_20985_, double p_20986_, double p_20987_, boolean p_20988_);

    @Inject(at = @At("HEAD"), method = "addEffect(Lnet/minecraft/world/effect/MobEffectInstance;)Z", cancellable = true)
    private void difficultraids_checkTotemImmunities(MobEffectInstance pEffectInstance, CallbackInfoReturnable<Boolean> callbackInfoReturnable)
    {
        final Item totemOfSpeed = DifficultRaidsItems.TOTEM_OF_SPEED.get();
        if((this.getMainHandItem().is(totemOfSpeed) || this.getOffhandItem().is(totemOfSpeed))
                && pEffectInstance.getEffect().equals(MobEffects.MOVEMENT_SLOWDOWN))
            callbackInfoReturnable.setReturnValue(false);
    }

    @Inject(at = @At("HEAD"), method = "checkTotemDeathProtection", cancellable = true)
    private void difficultraids_useCustomTotem(DamageSource damageSource, CallbackInfoReturnable<Boolean> callback)
    {
        if(damageSource.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) callback.setReturnValue(false);

        final Item totemInvisibility = DifficultRaidsItems.TOTEM_OF_INVISIBILITY.get();
        final Item totemLightning = DifficultRaidsItems.TOTEM_OF_LIGHTNING.get();
        final Item totemPoison = DifficultRaidsItems.TOTEM_OF_POISON.get();
        final Item totemSpeed = DifficultRaidsItems.TOTEM_OF_SPEED.get();
        final Item totemVengeance = DifficultRaidsItems.TOTEM_OF_VENGEANCE.get();
        final Item totemDestiny = DifficultRaidsItems.TOTEM_OF_DESTINY.get();
        final Item totemLevitation = DifficultRaidsItems.TOTEM_OF_LEVITATION.get();
        final Item totemProtection = DifficultRaidsItems.TOTEM_OF_PROTECTION.get();
        final Item totemFreezing = DifficultRaidsItems.TOTEM_OF_FREEZING.get();
        final Item totemPersistence = DifficultRaidsItems.TOTEM_OF_PERSISTENCE.get();
        final Item totemTeleportation = DifficultRaidsItems.TOTEM_OF_TELEPORTATION.get();
        final Item totemFireball = DifficultRaidsItems.TOTEM_OF_FIREBALLS.get();

        ItemStack mainHand = this.getMainHandItem();
        ItemStack offHand = this.getOffhandItem();

        Predicate<Item> checkTotem = i -> List.of(totemInvisibility, totemLightning, totemPoison, totemSpeed, totemVengeance, totemDestiny, totemLevitation, totemProtection, totemFreezing, totemPersistence, totemTeleportation, totemFireball).contains(i);
        ItemStack hand;
        if(checkTotem.test(mainHand.getItem())) hand = mainHand;
        else if(checkTotem.test(offHand.getItem())) hand = offHand;
        else hand = null;

        if(hand != null)
        {
            final ItemStack totem = hand.copy();

            //Totem of Persistence
            boolean persistenceProc = totem.is(totemPersistence) && this.random.nextInt(100) < 40;
            if(!persistenceProc) hand.shrink(1);

            //Default Revive Effect
            this.setHealth(1.0F);
            this.removeAllEffects();

            this.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 900, 1));
            this.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 100, 1));
            this.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 800, 0));

            //Totem of Invisibility
            if(totem.is(totemInvisibility))
            {
                this.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 600, 1));

                this.level()
                        .getEntitiesOfClass(Monster.class, new AABB(this.blockPosition()).inflate(5.0D))
                        .stream()
                        .filter(Monster::isAlive)
                        .forEach(m -> m.setTarget(null));
            }

            //Totem of Lightning
            if(totem.is(totemLightning))
            {
                this.level()
                        .getEntitiesOfClass(Monster.class, new AABB(this.blockPosition()).inflate(6.0D))
                        .stream()
                        .filter(Monster::isAlive)
                        .map(Entity::blockPosition)
                        .forEach(pos -> {
                            LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(this.level());
                            bolt.setDamage(this.random.nextFloat() * 5.0F);
                            bolt.moveTo(pos, 0.0F, 0.0F);
                            this.level().addFreshEntity(bolt);
                        });
            }

            //Totem of Poison
            if(totem.is(totemPoison))
            {
                this.level()
                        .getEntitiesOfClass(Monster.class, new AABB(this.blockPosition()).inflate(5.0D))
                        .stream()
                        .filter(Monster::isAlive)
                        .forEach(m -> m.addEffect(new MobEffectInstance(MobEffects.POISON, 120, 2)));
            }

            //Totem of Speed
            if(totem.is(totemSpeed))
            {
                this.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 200, 4));
            }

            //Totem of Vengeance
            if(totem.is(totemVengeance))
            {
                this.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 200, 3));
                this.removeEffect(MobEffects.REGENERATION);
                this.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 750, 3));
            }

            //Totem of Destiny
            if(totem.is(totemDestiny))
            {
                if(damageSource.getEntity() instanceof LivingEntity attacker)
                    attacker.hurt(attacker.damageSources().magic(), attacker.getHealth() + 1.0F);

                this.level().getEntitiesOfClass(Monster.class, new AABB(this.blockPosition()).inflate(10.0D))
                        .stream()
                        .filter(Monster::isAlive)
                        .forEach(m -> {
                            double distance = Math.pow(m.blockPosition().distSqr(this.blockPosition()), 0.5);
                            float damage = (float)(Math.max(0.1F, 6.0F - (distance / 2.0F)));

                            m.hurt(m.damageSources().magic(), damage);
                        });
            }

            //Totem of Levitation
            if(totem.is(totemLevitation))
            {
                this.level()
                        .getEntitiesOfClass(Monster.class, new AABB(this.blockPosition()).inflate(6.0D))
                        .stream()
                        .filter(Monster::isAlive)
                        .forEach(m -> m.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 30, 3)));

                this.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 30, 4));
            }

            //Totem of Protection
            if(totem.is(totemProtection))
            {
                this.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 120, 1));
                this.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 120, 2));

                BlockPos offset1 = this.blockPosition().offset(this.random.nextInt(2, 6), 2, this.random.nextInt(2, 6));
                BlockPos offset2 = this.blockPosition().offset(this.random.nextInt(2, 6) * -1, 2, this.random.nextInt(2, 6) * -1);

                IronGolem golem1 = EntityType.IRON_GOLEM.create(this.level());
                IronGolem golem2 = EntityType.IRON_GOLEM.create(this.level());

                golem1.moveTo(offset1, 0.0F, 0.0F);
                golem2.moveTo(offset2, 0.0F, 0.0F);

                if(damageSource.getEntity() instanceof LivingEntity attacker)
                {
                    golem1.setTarget(attacker);
                    golem1.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(golem1, attacker.getClass(), true));
                    golem2.setTarget(attacker);
                    golem2.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(golem2, attacker.getClass(), true));
                }

                this.level().addFreshEntity(golem1);
                this.level().addFreshEntity(golem2);
            }

            //Totem of Freezing
            if(totem.is(totemFreezing))
            {
                this.level()
                        .getEntitiesOfClass(Monster.class, new AABB(this.blockPosition()).inflate(6.0D))
                        .stream()
                        .filter(Monster::isAlive)
                        .filter(LivingEntity::canFreeze)
                        .forEach(m -> {
                            m.setTicksFrozen(m.getTicksFrozen() + 20 * 8);
                            m.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20 * 7, 2));
                        });
            }

            //Totem of Teleportation
            if(totem.is(totemTeleportation))
            {
                Vec3i eye_pos = new Vec3i((int) this.getEyePosition().x, (int) this.getEyePosition().y, (int) this.getEyePosition().z);
                BlockPos current = new BlockPos(eye_pos);
                BlockPos target;

                int tries = 0;
                int maxTries = 10;
                Predicate<BlockPos> isInvalidPos = pos -> this.level().getBlockState(pos).isAir() || this.level().getBlockState(pos.offset(0, (int)this.getEyeHeight(), 0)).isAir();
                do
                {
                    int x = (this.random.nextInt(2) == 0 ? 1 : -1) * this.random.nextInt(5, 16);
                    int z = (this.random.nextInt(2) == 0 ? 1 : -1) * this.random.nextInt(5, 16);
                    target = current.offset(x, this.random.nextInt(4), z);
                    tries++;
                }
                while(tries <= maxTries && isInvalidPos.test(target));

                if(tries == maxTries)
                {
                    target = current.offset(0, 20, 0);
                    this.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 80, 1));
                }
                else this.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 10));

                this.randomTeleport(target.getX(), target.getY(), target.getZ(), true);
            }

            //Totem of Fireballs
            if(totem.is(totemFireball))
            {
                Vec3i eye_pos = new Vec3i((int) this.getEyePosition().x, (int) this.getEyePosition().y, (int) this.getEyePosition().z);
                BlockPos source = new BlockPos(eye_pos);
                Stream.of(
                        new Vec3(1, 0.33, 0),
                        new Vec3(1, 0.33, 1),
                        new Vec3(0, 0.33, 1),
                        new Vec3(-1, 0.33, 0),
                        new Vec3(-1, 0.33, -1),
                        new Vec3(0, 0.33, -1),
                        new Vec3(-1, 0.33, 1),
                        new Vec3(1, 0.33, -1)
                ).forEach(v -> {
                    Fireball fireball = EntityType.FIREBALL.create(this.level());
                    fireball.moveTo(source, 0.0F, 0.0F);
                    fireball.shoot(v.x, v.y, v.z, this.random.nextFloat() * 6 + 2.0F, 2.0F);
                    this.level().addFreshEntity(fireball);
                });

                this.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 100, 1));
            }

            this.level().broadcastEntityEvent(this, (byte)35);
            callback.setReturnValue(true);
        }
    }
}
