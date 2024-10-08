package com.calculusmaster.difficultraids.entity.entities.elite;

import com.calculusmaster.difficultraids.entity.entities.component.VoldonFamiliarEntity;
import com.calculusmaster.difficultraids.entity.entities.core.AbstractEvokerVariant;
import com.calculusmaster.difficultraids.setup.DifficultRaidsConfig;
import com.calculusmaster.difficultraids.util.Compat;
import com.talhanation.recruits.entities.AbstractRecruitEntity;
import com.talhanation.workers.entities.AbstractWorkerEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.SmallFireball;
import net.minecraft.world.entity.raid.Raider;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import org.jetbrains.annotations.Nullable;
import tallestegg.guardvillagers.entities.Guard;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class VoldonEliteEntity extends AbstractEvokerVariant implements RangedAttackMob
{
    private final Component ELITE_NAME = Component.translatable("com.calculusmaster.difficultraids.elite_event.voldon");
    private final ServerBossEvent ELITE_EVENT = new ServerBossEvent(ELITE_NAME, BossEvent.BossBarColor.WHITE, BossEvent.BossBarOverlay.PROGRESS);

    private static final double FAMILIAR_CHECK_RADIUS = 50.0;

    private int totalFamiliars = 0;
    private List<LivingEntity> familiars = new ArrayList<>();
    private int familiarCooldown = 0;

    private boolean checkFamiliars = false;
    private String familiarTag;

    public VoldonEliteEntity(EntityType<? extends AbstractEvokerVariant> p_33724_, Level p_33725_)
    {
        super(p_33724_, p_33725_);

        this.familiarTag = IntStream.generate(() -> this.getRandom().nextInt(10)).limit(6).mapToObj(String::valueOf).collect(Collectors.joining());
    }

    @Override
    protected void registerGoals()
    {
        super.registerGoals();

        this.goalSelector.addGoal(0, new FloatGoal(this));

        this.goalSelector.addGoal(1, new VoldonCastSpellGoal());
        this.goalSelector.addGoal(2, new AvoidEntityGoal<>(this, Player.class, 4.0F, 0.6D, 0.75D));
        if(Compat.GUARD_VILLAGERS.isLoaded()) this.goalSelector.addGoal(2, new AvoidEntityGoal<>(this, Guard.class, 4.0F, 0.7D, 0.75D));
        if(Compat.RECRUITS.isLoaded()) this.goalSelector.addGoal(2, new AvoidEntityGoal<>(this, AbstractRecruitEntity.class, 4.0F, 0.7D, 0.75D));

        this.goalSelector.addGoal(3, new VoldonSummonFamiliarsSpellGoal());
        this.goalSelector.addGoal(4, new VoldonTeleportFamiliarSpellGoal());
        this.goalSelector.addGoal(4, new VoldonSacrificeFamiliarSpellGoal());
        this.goalSelector.addGoal(5, new RangedAttackGoal(this, 0.5, 120, 9.0F));

        this.goalSelector.addGoal(8, new RandomStrollGoal(this, 0.5D));
        this.goalSelector.addGoal(9, new LookAtPlayerGoal(this, Player.class, 3.0F, 1.0F));
        this.goalSelector.addGoal(10, new LookAtPlayerGoal(this, Mob.class, 8.0F));

        this.targetSelector.addGoal(1, (new HurtByTargetGoal(this, Raider.class)).setAlertOthers());
        this.targetSelector.addGoal(2, (new NearestAttackableTargetGoal<>(this, Player.class, true)).setUnseenMemoryTicks(300));
        if(Compat.GUARD_VILLAGERS.isLoaded()) this.targetSelector.addGoal(3, (new NearestAttackableTargetGoal<>(this, Guard.class, true)));
        if(Compat.RECRUITS.isLoaded()) this.targetSelector.addGoal(3, (new NearestAttackableTargetGoal<>(this, AbstractRecruitEntity.class, true)));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, IronGolem.class, true));
        this.targetSelector.addGoal(4, (new NearestAttackableTargetGoal<>(this, AbstractVillager.class, true)).setUnseenMemoryTicks(300));

        if(Compat.WORKERS.isLoaded()) this.targetSelector.addGoal(4, (new NearestAttackableTargetGoal<>(this, AbstractWorkerEntity.class, true)).setUnseenMemoryTicks(300));
    }

    @Override
    public void readAdditionalSaveData(CompoundTag pCompound)
    {
        super.readAdditionalSaveData(pCompound);

        for(int ID : pCompound.getIntArray("FamiliarIDs")) if(this.level().getEntity(ID) instanceof LivingEntity familiar) this.familiars.add(familiar);

        this.checkFamiliars = pCompound.getBoolean("CheckFamiliars");
        this.familiarTag = pCompound.getString("FamiliarTag");
        this.totalFamiliars = pCompound.getInt("TotalFamiliarCount");
    }

    @Override
    public void addAdditionalSaveData(CompoundTag pCompound)
    {
        super.addAdditionalSaveData(pCompound);

        pCompound.putBoolean("CheckFamiliars", !this.familiars.isEmpty());
        pCompound.putString("FamiliarTag", this.familiarTag);
        pCompound.putInt("TotalFamiliarCount", this.totalFamiliars);
    }

    @Override
    public void applyRaidBuffs(int p_37844_, boolean p_37845_)
    {

    }

    public boolean areFamiliarsDead()
    {
        return this.familiars.isEmpty();
    }

    public void removeFamiliar(VoldonFamiliarEntity familiar)
    {
        this.familiars.remove(familiar);

        if(this.areFamiliarsDead()) this.familiarCooldown = this.config().voldon.familiarSummonCooldown;
    }

    @Override
    protected void customServerAiStep()
    {
        super.customServerAiStep();
        this.ELITE_EVENT.setProgress(this.getHealth() / this.getMaxHealth());

        if(this.familiarCooldown > 0) this.familiarCooldown--;
    }

    @Override
    public boolean hurt(DamageSource pSource, float pAmount)
    {
        if(pSource.getEntity() instanceof IronGolem || (Compat.GUARD_VILLAGERS.isLoaded() && pSource.getEntity() instanceof Guard))
            pAmount *= this.config().voldon.friendlyDamageReduction;

        if(!this.areFamiliarsDead())
        {
            pAmount *= 0.025F;

            this.familiars.stream()
                    .filter(e -> !e.hasEffect(MobEffects.GLOWING))
                    .forEach(e -> e.addEffect(new MobEffectInstance(MobEffects.GLOWING, 10000, 1, false, false)));
        }

        return super.hurt(pSource, pAmount);
    }

    @Override
    public void die(DamageSource pCause)
    {
        super.die(pCause);

        BlockPos pos = this.blockPosition();

        int randomSpawnCount = this.random.nextInt(3, 6);
        for(int i = 0; i < randomSpawnCount; i++)
        {
            BlockPos spawnPos = pos.offset(5 - this.random.nextInt(1, 10), 1, 5 - this.random.nextInt(1, 10));

            Monster zombie = EntityType.ZOMBIE.create(this.level());
            zombie.moveTo(spawnPos, this.getYHeadRot(), this.getXRot());

            zombie.targetSelector.removeAllGoals(goal -> (true));
            zombie.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(zombie, Villager.class, true));
            zombie.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(zombie, Player.class, true));
            zombie.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(zombie, IronGolem.class, true));
            if(Compat.GUARD_VILLAGERS.isLoaded()) zombie.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(zombie, Guard.class, true));

            if(pCause.getEntity() instanceof LivingEntity living) zombie.setTarget(living);

            this.level().addFreshEntity(zombie);
        }
    }

    @Override
    public void tick()
    {
        super.tick();
        if(!this.level().isClientSide) this.familiars.removeIf(LivingEntity::isDeadOrDying);

        if(this.checkFamiliars && this.level() instanceof ServerLevel serverLevel)
        {
            this.familiars.addAll(serverLevel.getEntitiesOfClass(VoldonFamiliarEntity.class, this.getBoundingBox().inflate(FAMILIAR_CHECK_RADIUS), e -> e.getTags().contains(this.familiarTag)));

            this.checkFamiliars = this.tickCount <= 100 && this.familiars.isEmpty();
        }
    }

    @Override
    protected void dropCustomDeathLoot(DamageSource pSource, int pLooting, boolean pRecentlyHit)
    {

    }

    @Override
    public void performRangedAttack(LivingEntity pTarget, float pDistanceFactor)
    {
        double d0 = this.distanceToSqr(pTarget);
        double d1 = pTarget.getX() - this.getX();
        double d2 = pTarget.getY(0.5D) - this.getY(0.5D);
        double d3 = pTarget.getZ() - this.getZ();
        double d4 = Math.sqrt(Math.sqrt(d0)) * 0.5D;

        int count = this.config().voldon.fireballCount;

        for(int i = 0; i < count; i++)
        {
            SmallFireball fireball = new SmallFireball(this.level(), this, this.random.triangle(d1, 2.297D * d4), d2, this.random.triangle(d3, 2.297D * d4))
            {
                @Override
                protected void onHitEntity(EntityHitResult pResult)
                {
                    if(pResult.getEntity() instanceof Raider) this.discard();
                    else super.onHitEntity(pResult);
                }
            };

            fireball.setPos(fireball.getX(), this.getY(0.5D) + 0.5D, fireball.getZ());
            this.level().addFreshEntity(fireball);
        }

        this.level().addParticle(ParticleTypes.SMALL_FLAME, this.getX(), this.getEyeY() + 0.4, this.getZ(), 0.2, 0.0, 0.3);
    }

    private class VoldonCastSpellGoal extends SpellcastingIllagerCastSpellGoal
    {
        @Override
        public void tick()
        {
            if(VoldonEliteEntity.this.getTarget() != null)
                VoldonEliteEntity.this.getLookControl().setLookAt(VoldonEliteEntity.this.getTarget(), (float)VoldonEliteEntity.this.getMaxHeadYRot(), (float)VoldonEliteEntity.this.getMaxHeadXRot());
        }
    }

    private class VoldonSacrificeFamiliarSpellGoal extends SpellcastingIllagerUseSpellGoal
    {
        private VoldonSacrificeFamiliarSpellGoal() {}

        @Override
        protected void castSpell()
        {
            List<LivingEntity> familiars = new ArrayList<>(VoldonEliteEntity.this.familiars);
            LivingEntity sacrifice = null;
            for(LivingEntity e : familiars)
                if(e.getHealth() <= e.getMaxHealth() * 0.25F)
                {
                    sacrifice = e;
                    break;
                }

            if(!familiars.isEmpty() && sacrifice != null)
            {
                VoldonEliteEntity.this.getLookControl().setLookAt(sacrifice);
                ((Mob)sacrifice).getLookControl().setLookAt(VoldonEliteEntity.this);

                int effectDuration = VoldonEliteEntity.this.config().voldon.sacrificeBuffDuration;

                familiars.forEach(f ->
                {
                    f.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, effectDuration, 1, false, false));
                    f.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, effectDuration, 3, false, false));
                    f.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, effectDuration / 4, 1, false, false));
                });

                sacrifice.hurt(sacrifice.damageSources().starve(), sacrifice.getHealth() + 1.0F);
            }
        }

        @Override
        public boolean canUse()
        {
            return VoldonEliteEntity.this.tickCount >= this.spellCooldown
                    && !VoldonEliteEntity.this.areFamiliarsDead()
                    && VoldonEliteEntity.this.familiars.stream().anyMatch(e -> e.getHealth() <= e.getMaxHealth() * 0.25F)
                    && VoldonEliteEntity.this.familiars.size() > 2;
        }

        @Override
        protected int getCastingTime()
        {
            return 40;
        }

        @Override
        protected int getCastingInterval()
        {
            return 900;
        }

        @Override
        protected int getCastWarmupTime()
        {
            return 10;
        }

        @Nullable
        @Override
        protected SoundEvent getSpellPrepareSound()
        {
            return SoundEvents.WITCH_DRINK;
        }

        @Override
        protected SpellType getSpellType()
        {
            return SpellType.VOLDON_SACRIFICE_FAMILIAR;
        }
    }

    private class VoldonTeleportFamiliarSpellGoal extends SpellcastingIllagerUseSpellGoal
    {
        private VoldonTeleportFamiliarSpellGoal() {}

        @Override
        protected void castSpell()
        {
            List<LivingEntity> familiars = VoldonEliteEntity.this.familiars.stream().filter(LivingEntity::isAlive).toList();

            if(!familiars.isEmpty())
            {
                LivingEntity target = familiars.get(VoldonEliteEntity.this.random.nextInt(familiars.size()));

                VoldonEliteEntity.this.getLookControl().setLookAt(target);

                double yOffset = 0.3;
                BlockPos targetPos = target.blockPosition();
                BlockPos thisPos = VoldonEliteEntity.this.blockPosition();

                VoldonEliteEntity.this.teleportToWithTicket(targetPos.getX(), targetPos.getY(), targetPos.getZ());
                target.teleportToWithTicket(thisPos.getX(), thisPos.getY(), thisPos.getZ());

                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 20 * 7, 2, false, true));
            }
        }

        @Override
        public boolean canUse()
        {
            return VoldonEliteEntity.this.tickCount >= this.spellCooldown && !VoldonEliteEntity.this.areFamiliarsDead();
        }

        @Override
        protected int getCastingTime()
        {
            return 10;
        }

        @Override
        protected int getCastingInterval()
        {
            return 400;
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
            return SoundEvents.ENDERMAN_TELEPORT;
        }

        @Override
        protected SpellType getSpellType()
        {
            return SpellType.VOLDON_TELEPORT_FAMILIAR;
        }
    }

    private class VoldonSummonFamiliarsSpellGoal extends SpellcastingIllagerUseSpellGoal
    {
        private VoldonSummonFamiliarsSpellGoal() {}

        @Override
        protected void castSpell()
        {
            int familiarCount = VoldonEliteEntity.this.config().voldon.familiarSummonCount;

            VoldonEliteEntity.this.totalFamiliars = familiarCount;

            BlockPos sourcePos = VoldonEliteEntity.this.blockPosition();
            Supplier<BlockPos> familiarPos = () -> sourcePos.offset(VoldonEliteEntity.this.random.nextInt(2, 10), VoldonEliteEntity.this.random.nextInt(2, 4), VoldonEliteEntity.this.random.nextInt(2, 10));
            for(int i = 0; i < familiarCount; i++)
            {
                Monster familiar = new VoldonFamiliarEntity(VoldonEliteEntity.this.level(), VoldonEliteEntity.this);
                familiar.moveTo(familiarPos.get(), 0.0F, 0.0F);
                familiar.setOnGround(true);
                familiar.addTag(VoldonEliteEntity.this.familiarTag);

                familiar.addEffect(new MobEffectInstance(MobEffects.GLOWING, 10000, 1, false, false));

                VoldonEliteEntity.this.level().addFreshEntity(familiar);
                VoldonEliteEntity.this.familiars.add(familiar);
            }
        }

        @Override
        public boolean canUse()
        {
            return super.canUse() && VoldonEliteEntity.this.areFamiliarsDead() && VoldonEliteEntity.this.familiarCooldown <= 0;
        }

        @Override
        protected int getCastingTime()
        {
            return 40;
        }

        @Override
        protected int getCastingInterval()
        {
            return 100;
        }

        @Override
        protected int getCastWarmupTime()
        {
            return 20;
        }

        @Nullable
        @Override
        protected SoundEvent getSpellPrepareSound()
        {
            return SoundEvents.EVOKER_PREPARE_SUMMON;
        }

        @Override
        protected SpellType getSpellType()
        {
            return SpellType.VOLDON_SUMMON_FAMILIARS;
        }
    }

    @Override
    public void startSeenByPlayer(ServerPlayer pPlayer)
    {
        super.startSeenByPlayer(pPlayer);
        if(DifficultRaidsConfig.BOSS_BARS.get()) this.ELITE_EVENT.addPlayer(pPlayer);
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer pPlayer)
    {
        super.stopSeenByPlayer(pPlayer);
        this.ELITE_EVENT.removePlayer(pPlayer);
    }
}
