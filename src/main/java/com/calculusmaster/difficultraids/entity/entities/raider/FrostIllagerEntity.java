package com.calculusmaster.difficultraids.entity.entities.raider;

import com.calculusmaster.difficultraids.entity.DifficultRaidsEntityTypes;
import com.calculusmaster.difficultraids.entity.entities.component.FrostSnowballEntity;
import com.calculusmaster.difficultraids.entity.entities.core.AbstractEvokerVariant;
import com.calculusmaster.difficultraids.util.Compat;
import com.talhanation.recruits.entities.AbstractRecruitEntity;
import com.talhanation.workers.entities.AbstractWorkerEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.raid.Raider;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;
import tallestegg.guardvillagers.entities.Guard;

public class FrostIllagerEntity extends AbstractEvokerVariant
{
    private int barrageTicks = 0;

    public FrostIllagerEntity(EntityType<? extends AbstractEvokerVariant> p_33724_, Level p_33725_)
    {
        super(p_33724_, p_33725_);
    }

    @Override
    protected void registerGoals()
    {
        super.registerGoals();

        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new FrostIllagerCastSpell());
        this.goalSelector.addGoal(2, new AvoidEntityGoal<>(this, Player.class, 8.0F, 0.6D, 1.0D));
        if(Compat.GUARD_VILLAGERS.isLoaded()) this.goalSelector.addGoal(2, new AvoidEntityGoal<>(this, Guard.class, 8.0F, 0.6D, 1.0D));
        if(Compat.RECRUITS.isLoaded()) this.goalSelector.addGoal(2, new AvoidEntityGoal<>(this, AbstractRecruitEntity.class, 8.0F, 0.6D, 1.0D));
        this.goalSelector.addGoal(4, new FrostIllagerFreezeSpellGoal());
        this.goalSelector.addGoal(4, new FrostIllagerBarrageSpellGoal());
        this.goalSelector.addGoal(5, new FrostIllagerSnowballBlastSpellGoal());
        this.goalSelector.addGoal(8, new RandomStrollGoal(this, 0.7D));
        this.goalSelector.addGoal(9, new LookAtPlayerGoal(this, Player.class, 3.0F, 1.0F));
        this.goalSelector.addGoal(10, new LookAtPlayerGoal(this, Mob.class, 8.0F));

        this.targetSelector.addGoal(1, (new HurtByTargetGoal(this, Raider.class)).setAlertOthers());
        this.targetSelector.addGoal(2, (new NearestAttackableTargetGoal<>(this, Player.class, true)).setUnseenMemoryTicks(300));
        this.targetSelector.addGoal(3, (new NearestAttackableTargetGoal<>(this, AbstractVillager.class, true)).setUnseenMemoryTicks(300));
        if(Compat.WORKERS.isLoaded()) this.targetSelector.addGoal(3, (new NearestAttackableTargetGoal<>(this, AbstractWorkerEntity.class, true)).setUnseenMemoryTicks(300));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, IronGolem.class, true));

        if(Compat.GUARD_VILLAGERS.isLoaded()) this.targetSelector.addGoal(3, (new NearestAttackableTargetGoal<>(this, Guard.class, true)).setUnseenMemoryTicks(300));
        if(Compat.RECRUITS.isLoaded()) this.targetSelector.addGoal(3, (new NearestAttackableTargetGoal<>(this, AbstractRecruitEntity.class, true)).setUnseenMemoryTicks(300));
    }

    @Override
    public void applyRaidBuffs(int p_37844_, boolean p_37845_)
    {

    }

    @Override
    public boolean hurt(DamageSource pSource, float pAmount)
    {
        return super.hurt(pSource, pSource.is(DamageTypeTags.IS_PROJECTILE) ? pAmount * 2 : pAmount);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag pCompound)
    {
        super.addAdditionalSaveData(pCompound);

        pCompound.putInt("SnowballBarrageTicks", this.barrageTicks);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag pCompound)
    {
        super.readAdditionalSaveData(pCompound);

        this.barrageTicks = pCompound.getInt("SnowballBarrageTicks");
    }

    @Override
    public void tick()
    {
        super.tick();

        //Slowness Aura
        if(this.tickCount % (20 * 4) == 0)
        {
            final AABB aabb = this.getBoundingBox().inflate(this.config().frostmage.slownessAuraRadius);

            this.level().getEntitiesOfClass(LivingEntity.class, aabb, e -> !e.isAlliedTo(this) && !e.hasEffect(MobEffects.MOVEMENT_SLOWDOWN) && (!(e instanceof Player p) || !p.isSpectator() && !p.isCreative())).forEach(e -> e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20 * 5, 3)));
        }

        //Barrage
        if(this.barrageTicks > 0 && this.barrageTicks-- % 10 == 0)
        {
            LivingEntity target = this.getTarget();

            if(target != null)
            {
                int size = this.random.nextInt(15, 31);

                double targetY = target.getEyeY() - 1.1D;
                double targetX = target.getX() - this.getX();
                double targetZ = target.getZ() - this.getZ();
                double distanceY = Math.sqrt(targetX * targetX + targetZ * targetZ) * (double)0.2F;

                for(int i = 0; i < size; i++)
                {
                    FrostSnowballEntity snowball = DifficultRaidsEntityTypes.FROST_SNOWBALL.get().create(this.level());
                    snowball.setOwner(this);
                    snowball.setPos(this.getEyePosition().x(), this.getEyePosition().y() - 0.2, this.getEyePosition().z());
                    snowball.setDamage(this.config().frostmage.barrageSnowballDamage);

                    //Helps with client lag a little
                    if(this.random.nextInt(100) < 40) snowball.setInvisible(true);

                    double targetSnowballY = targetY - snowball.getY();
                    snowball.shoot(targetX, targetSnowballY + distanceY, targetZ, 1.6F, 14.0F);

                    this.level().addFreshEntity(snowball);
                }

                this.playSound(SoundEvents.SNOW_GOLEM_SHOOT, 1.0F, 0.4F / (this.random.nextFloat() * 0.4F + 0.8F));

                if(target.isDeadOrDying()) this.barrageTicks = 0;
            }
            else this.barrageTicks = 0;
        }
    }

    private class FrostIllagerCastSpell extends SpellcastingIllagerCastSpellGoal
    {
        private FrostIllagerCastSpell() {}

        @Override
        public void tick()
        {
            if(FrostIllagerEntity.this.getTarget() != null)
                FrostIllagerEntity.this.getLookControl().setLookAt(FrostIllagerEntity.this.getTarget(), (float)FrostIllagerEntity.this.getMaxHeadYRot(), (float)FrostIllagerEntity.this.getMaxHeadXRot());
        }
    }

    private class FrostIllagerSnowballBlastSpellGoal extends SpellcastingIllagerUseSpellGoal
    {
        private FrostIllagerSnowballBlastSpellGoal() {}

        @Override
        protected void castSpell()
        {
            LivingEntity target = FrostIllagerEntity.this.getTarget();
            int size = 5;

            if(target != null)
            {
                float damage = FrostIllagerEntity.this.config().frostmage.snowballBlastDamage;

                for(int i = 0; i < size; i++)
                {
                    FrostSnowballEntity snowball = DifficultRaidsEntityTypes.FROST_SNOWBALL.get().create(FrostIllagerEntity.this.level());
                    snowball.setOwner(FrostIllagerEntity.this);
                    snowball.setPos(FrostIllagerEntity.this.getEyePosition().x(), FrostIllagerEntity.this.getEyePosition().y() - 0.2, FrostIllagerEntity.this.getEyePosition().z());
                    snowball.setDamage(damage);

                    double targetY = target.getEyeY() - 1.1D;
                    double targetX = target.getX() - FrostIllagerEntity.this.getX();
                    double targetSnowballY = targetY - snowball.getY();
                    double targetZ = target.getZ() - FrostIllagerEntity.this.getZ();
                    double distanceY = Math.sqrt(targetX * targetX + targetZ * targetZ) * (double)0.2F;

                    snowball.shoot(targetX, targetSnowballY + distanceY, targetZ, 1.6F, 3.0F);
                    FrostIllagerEntity.this.level().addFreshEntity(snowball);
                }
            }
        }

        @Override
        public boolean canUse()
        {
            return super.canUse() && FrostIllagerEntity.this.barrageTicks == 0;
        }

        @Override
        protected int getCastingTime()
        {
            return 15;
        }

        @Override
        protected int getCastingInterval()
        {
            return 60;
        }

        @Override
        protected int getCastWarmupTime()
        {
            return 5;
        }

        @Nullable
        @Override
        protected SoundEvent getSpellPrepareSound()
        {
            return SoundEvents.EVOKER_PREPARE_ATTACK;
        }

        @Override
        protected SpellType getSpellType()
        {
            return SpellType.FROST_SNOWBALL_BLAST;
        }
    }

    private class FrostIllagerBarrageSpellGoal extends SpellcastingIllagerUseSpellGoal
    {
        private FrostIllagerBarrageSpellGoal() {}

        @Override
        protected void castSpell()
        {
            FrostIllagerEntity.this.barrageTicks = FrostIllagerEntity.this.config().frostmage.barrageDuration;
        }

        @Override
        public boolean canUse()
        {
            return super.canUse() && FrostIllagerEntity.this.barrageTicks == 0;
        }

        @Override
        protected int getCastingTime()
        {
            return 20;
        }

        @Override
        protected int getCastingInterval()
        {
            return 600;
        }

        @Override
        protected int getCastWarmupTime()
        {
            return 50;
        }

        @Nullable
        @Override
        protected SoundEvent getSpellPrepareSound()
        {
            return SoundEvents.EVOKER_PREPARE_ATTACK;
        }

        @Override
        protected SpellType getSpellType()
        {
            return SpellType.FROST_BARRAGE;
        }
    }

    private class FrostIllagerFreezeSpellGoal extends SpellcastingIllagerUseSpellGoal
    {
        private FrostIllagerFreezeSpellGoal() {}

        @Override
        protected void castSpell()
        {
            LivingEntity target = FrostIllagerEntity.this.getTarget();

            if(target != null)
            {
                int duration = FrostIllagerEntity.this.config().frostmage.freezeDuration;

                if(target.canFreeze()) target.setTicksFrozen(target.getTicksFrozen() + duration);
            }
        }

        @Override
        protected int getCastingTime()
        {
            return 50;
        }

        @Override
        protected int getCastingInterval()
        {
            return 430;
        }

        @Override
        protected int getCastWarmupTime()
        {
            return 30;
        }

        @Nullable
        @Override
        protected SoundEvent getSpellPrepareSound()
        {
            return SoundEvents.EVOKER_CAST_SPELL;
        }

        @Override
        protected SpellType getSpellType()
        {
            return SpellType.FROST_FREEZE;
        }
    }
}
