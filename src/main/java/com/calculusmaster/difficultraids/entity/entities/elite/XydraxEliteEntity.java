package com.calculusmaster.difficultraids.entity.entities.elite;

import com.calculusmaster.difficultraids.config.RaidDifficultyConfig;
import com.calculusmaster.difficultraids.entity.entities.component.WindColumnObject;
import com.calculusmaster.difficultraids.entity.entities.core.AbstractEvokerVariant;
import com.calculusmaster.difficultraids.raids.RaidDifficulty;
import com.calculusmaster.difficultraids.setup.DifficultRaidsConfig;
import com.calculusmaster.difficultraids.setup.DifficultRaidsEffects;
import com.calculusmaster.difficultraids.util.Compat;
import com.talhanation.recruits.entities.AbstractRecruitEntity;
import com.talhanation.workers.entities.AbstractWorkerEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Position;
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
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.entity.raid.Raider;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import tallestegg.guardvillagers.entities.Guard;

import java.util.ArrayList;
import java.util.List;

public class XydraxEliteEntity extends AbstractEvokerVariant
{
    private final Component ELITE_NAME = Component.translatable("com.calculusmaster.difficultraids.elite_event.xydrax");
    private final ServerBossEvent ELITE_EVENT = new ServerBossEvent(ELITE_NAME, BossEvent.BossBarColor.BLUE, BossEvent.BossBarOverlay.PROGRESS);

    private boolean isHealing = false;
    private final List<WindColumnObject> windColumns = new ArrayList<>();
    private int vortexTicks = 0;
    private BlockPos vortexFloor = BlockPos.ZERO;
    private AABB vortexAABB = new AABB(0, 0, 0, 0, 0, 0);

    public XydraxEliteEntity(EntityType<? extends AbstractEvokerVariant> p_33724_, Level p_33725_)
    {
        super(p_33724_, p_33725_);
    }

    @Override
    protected void registerGoals()
    {
        super.registerGoals();

        this.goalSelector.addGoal(0, new FloatGoal(this));

        this.goalSelector.addGoal(1, new XydraxCastSpellGoal());
        this.goalSelector.addGoal(2, new XydraxAvoidEntityGoal( 4.0F, 0.7D, 0.8D));
        if(Compat.GUARD_VILLAGERS.isLoaded()) this.goalSelector.addGoal(2, new AvoidEntityGoal<>(this, Guard.class, 4.0F, 0.7D, 0.8D));
        if(Compat.RECRUITS.isLoaded()) this.goalSelector.addGoal(2, new AvoidEntityGoal<>(this, AbstractRecruitEntity.class, 4.0F, 0.7D, 0.8D));

        this.goalSelector.addGoal(3, new XydraxVortexSpellGoal());
        this.goalSelector.addGoal(3, new XydraxWindColumnSpellGoal());
        this.goalSelector.addGoal(4, new XydraxBarrageSpellGoal());
        this.goalSelector.addGoal(4, new XydraxHealSpellGoal());

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
    public void applyRaidBuffs(int p_37844_, boolean p_37845_)
    {

    }

    @Override
    public void addAdditionalSaveData(CompoundTag pCompound)
    {
        super.addAdditionalSaveData(pCompound);

        pCompound.putBoolean("IsHealing", this.isHealing);
        pCompound.putIntArray("WindColumnData", this.serializeWindColumns());
        pCompound.putInt("VortexTicks", this.vortexTicks);
        pCompound.putIntArray("VortexFloorPos", new int[]{this.vortexFloor.getX(), this.vortexFloor.getY(), this.vortexFloor.getZ()});
    }

    @Override
    public void readAdditionalSaveData(CompoundTag pCompound)
    {
        super.readAdditionalSaveData(pCompound);

        this.isHealing = pCompound.getBoolean("IsHealing");
        this.deserializeWindColumns(pCompound.getIntArray("WindColumnData"));
        this.vortexTicks = pCompound.getInt("VortexTicks");

        int[] vortexFloorPos = pCompound.getIntArray("VortexFloorPos");
        this.vortexFloor = vortexFloorPos.length == 0 ? BlockPos.ZERO : new BlockPos(vortexFloorPos[0], vortexFloorPos[1], vortexFloorPos[2]);
        if(this.vortexTicks > 0) this.createVortexAABB();
    }

    @Override
    protected void customServerAiStep()
    {
        super.customServerAiStep();
        this.ELITE_EVENT.setProgress(this.getHealth() / this.getMaxHealth());
    }

    @Override
    public boolean hurt(DamageSource pSource, float pAmount)
    {
        if(pSource.getEntity() instanceof IronGolem || (Compat.GUARD_VILLAGERS.isLoaded() && pSource.getEntity() instanceof Guard))
            pAmount *= this.config().xydrax.friendlyDamageReduction;

        if(pSource.getDirectEntity() instanceof LivingEntity living && this.random.nextFloat() < 0.2)
            living.push(0.0D, this.random.nextFloat() * 0.7, 0.0D);

        return super.hurt(pSource, pAmount);
    }

    @Override
    protected void dropCustomDeathLoot(DamageSource pSource, int pLooting, boolean pRecentlyHit)
    {

    }

    @Override
    public boolean causeFallDamage(float pFallDistance, float pMultiplier, DamageSource pSource)
    {
        return false;
    }

    @Override
    public void tick()
    {
        super.tick();

        RaidDifficulty raidDifficulty = this.isInDifficultRaid() ? this.getRaidDifficulty() : RaidDifficulty.DEFAULT;
        RaidDifficultyConfig cfg = this.config();

        if(this.isHealing())
        {
            //Slow Descent
            Vec3 deltaMove = this.getDeltaMovement();
            if(!this.isVortexActive() && !this.onGround() && deltaMove.y() < 0) this.setDeltaMovement(deltaMove.multiply(1.0D, 0.55D, 1.0D));

            //Healing Checks
            if(this.onGround()) this.isHealing = false;
            else if(this.random.nextBoolean())
            {
                float currentHealth = this.getHealth();
                float maxHealth = this.getMaxHealth();

                if(maxHealth - currentHealth > 0.5F)
                {
                    float healAmount = this.random.nextFloat() / 1.5F;
                    this.heal(healAmount);
                }
            }
        }

        if(!this.windColumns.isEmpty())
        {
            this.windColumns.removeIf(WindColumnObject::isComplete);

            this.windColumns.forEach(WindColumnObject::tick);
        }

        if(this.isVortexActive())
        {
            this.vortexTicks--;

            //Extremely slow descent
            Vec3 deltaMove = this.getDeltaMovement();
            if(!this.onGround() && deltaMove.y() < 0) this.setDeltaMovement(deltaMove.multiply(1.0D, 0.1D, 1.0D));

            //Particles
            for(int i = this.vortexFloor.getY(); i < this.blockPosition().getY(); i++)
            {
                Vec3 pos = new Vec3(this.vortexFloor.getX() + 0.5, i, this.vortexFloor.getZ() + 0.5);
                ((ServerLevel)this.level()).sendParticles(ParticleTypes.FALLING_OBSIDIAN_TEAR, pos.x(), pos.y(), pos.z(), 1, 0.05, 0, 0.05, 1.0);
            }

            //Vortex Pull
            if(this.vortexTicks % cfg.xydrax.vortexPullInterval == 0)
            {
                this.getVortexTargets().forEach(e -> {
                    Vec3 targetPos = new Vec3(e.position().x() + 0.5, e.position().y(), e.position().z() + 0.5);
                    Vec3 targetVector = new Vec3(this.vortexFloor.getX() - targetPos.x(), this.vortexFloor.getY() - targetPos.y(), this.vortexFloor.getZ() - targetPos.z()).normalize();

                    double force = cfg.xydrax.vortexForce;

                    //Modifier from entity's Knockback Resistance (0 -> 1, percent of some max value)
                    double kbresistanceModifier = 1 - e.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE) * 0.5F;
                    force = Math.max(force * 0.25F, force * kbresistanceModifier);

                    e.push(targetVector.x * force, targetVector.y, targetVector.z * force);
                    e.hurtMarked = true;
                });
            }

            //DoT
            if(this.vortexTicks % cfg.xydrax.vortexDamageInterval == 0)
            {
                this.getVortexTargets().forEach(e -> {
                    double distance = Math.pow(e.distanceToSqr(this.vortexFloor.getX(), this.vortexFloor.getY(), this.vortexFloor.getZ()), 0.5);

                    List<Double> base = cfg.xydrax.vortexBaseDamageThresholds;
                    double damage;
                    if(distance < 1) damage = base.get(0);
                    else if(distance < 2) damage = base.get(1);
                    else if(distance < 5) damage = base.get(2);
                    else damage = base.get(3);

                    damage *= cfg.xydrax.vortexDamageMultiplier;

                    if(damage != 0.0F) e.hurt(this.damageSources().mobAttack(this), (float)damage);
                });
            }
        }

        //Movement Slowdown while Wind Columns Active
        if(this.isWindColumnActive() && !this.hasEffect(MobEffects.MOVEMENT_SLOWDOWN))
            this.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20 * 60, 5, false, true));
        else if(!this.isWindColumnActive() && this.hasEffect(MobEffects.MOVEMENT_SLOWDOWN))
            this.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
    }

    public boolean isHealing()
    {
        return this.isHealing;
    }

    public boolean isWindColumnActive()
    {
        return this.windColumns.size() > 3;
    }

    public boolean isVortexActive()
    {
        return this.vortexTicks > 0;
    }

    private int[] serializeWindColumns()
    {
        int[] data = new int[this.windColumns.size() * 4];
        for(int i = 0; i < data.length; i += 4)
        {
            WindColumnObject column = this.windColumns.get(i / 4);
            data[i] = column.getPosition().getX();
            data[i + 1] = column.getPosition().getY();
            data[i + 2] = column.getPosition().getZ();
            data[i + 3] = column.getLife();
        }
        return data;
    }

    private void deserializeWindColumns(int[] data)
    {
        for(int i = 0; i < data.length; i += 4)
        {
            BlockPos pos = new BlockPos(data[i], data[i + 1], data[i + 2]);
            int life = data[i + 3];
            this.windColumns.add(new WindColumnObject(this, pos, life));
        }
    }

    private void createVortexAABB()
    {
        this.vortexAABB = new AABB(this.vortexFloor)
                .inflate(10.0, 0, 10.0)
                .setMaxY(this.vortexFloor.getY() + 10)
                .setMinY(this.vortexFloor.getY() - 1);
    }

    private void summonWindColumns()
    {
        BlockPos center = new BlockPos(this.blockPosition());

        RaidDifficulty raidDifficulty = this.isInDifficultRaid() ? this.getRaidDifficulty() : RaidDifficulty.DEFAULT;

        List<BlockPos> windColumnSpawns = switch(raidDifficulty) {
            case DEFAULT -> List.of(
                    center.offset(4, 0, 0),
                    center.offset(-4, 0, 0)
            );
            case HERO -> List.of(
                    center.offset(4, 0, 0),
                    center.offset(-4, 0, 0),
                    center.offset(0, 0, 4),
                    center.offset(0, 0, -4)
            );
            case LEGEND -> List.of(
                    center.offset(4, 0, 0),
                    center.offset(-4, 0, 0),
                    center.offset(0, 0, 4),
                    center.offset(0, 0, -4),
                    center.offset(4, 0, 4),
                    center.offset(4, 0, -4)
            );
            case MASTER -> List.of(
                    center.offset(4, 0, 0),
                    center.offset(-4, 0, 0),
                    center.offset(0, 0, 4),
                    center.offset(0, 0, -4),
                    center.offset(4, 0, 4),
                    center.offset(4, 0, -4),
                    center.offset(-4, 0, 4),
                    center.offset(-4, 0, -4)
            );
            case GRANDMASTER -> List.of(
                    center.offset(4, 0, 0),
                    center.offset(-4, 0, 0),
                    center.offset(0, 0, 4),
                    center.offset(0, 0, -4),
                    center.offset(4, 0, 4),
                    center.offset(4, 0, -4),
                    center.offset(-4, 0, 4),
                    center.offset(-4, 0, -4),
                    center.offset(6, 0, 6),
                    center.offset(-6, 0, 6),
                    center.offset(6, 0, -6),
                    center.offset(-6, 0, -6)
            );
        };

        windColumnSpawns.forEach(pos -> {
            int life = this.random.nextInt(
                    raidDifficulty.config().xydrax.windColumnLifetime.get(0),
                    raidDifficulty.config().xydrax.windColumnLifetime.get(1) + 1
            );

            WindColumnObject column = new WindColumnObject(this, pos, life);
            this.windColumns.add(column);
        });
    }

    private List<LivingEntity> getVortexTargets()
    {
        return this.level().getEntitiesOfClass(LivingEntity.class, this.vortexAABB, e ->
        {
            if(e.isAlliedTo(this)) return false;
            else if(e instanceof Player player) return !player.isCreative() && !player.isSpectator();
            else return true;
        });
    }

    //For spells that last beyond their goals, like healing or wind column
    private boolean isInExtendedSpellState()
    {
        return this.isHealing() || this.isWindColumnActive() || this.isVortexActive();
    }

    private class XydraxAvoidEntityGoal extends AvoidEntityGoal<LivingEntity>
    {
        public XydraxAvoidEntityGoal(float pMaxDistance, double pWalkSpeedModifier, double pSprintSpeedModifier)
        {
            super(XydraxEliteEntity.this, LivingEntity.class, pMaxDistance, pWalkSpeedModifier, pSprintSpeedModifier, e -> (e instanceof Player player && !player.isCreative() && !player.isSpectator()) || (Compat.GUARD_VILLAGERS.isLoaded() && e instanceof Guard));
        }

        @Override
        public boolean canUse()
        {
            return super.canUse() && !XydraxEliteEntity.this.isInExtendedSpellState() && !XydraxEliteEntity.this.isCastingSpell();
        }
    }

    private class XydraxCastSpellGoal extends SpellcastingIllagerCastSpellGoal
    {
        private XydraxCastSpellGoal() {}

        @Override
        public void tick()
        {
            if(XydraxEliteEntity.this.getTarget() != null)
                XydraxEliteEntity.this.getLookControl().setLookAt(XydraxEliteEntity.this.getTarget(), (float)XydraxEliteEntity.this.getMaxHeadYRot(), (float)XydraxEliteEntity.this.getMaxHeadXRot());
        }
    }

    private class XydraxVortexSpellGoal extends SpellcastingIllagerUseSpellGoal
    {
        private XydraxVortexSpellGoal() {}

        private boolean isEntityNearby()
        {
            XydraxEliteEntity xydrax = XydraxEliteEntity.this;
            AABB search = new AABB(xydrax.blockPosition().offset(0, 1, 0)).inflate(10.0);
            return !xydrax.level().getEntitiesOfClass(LivingEntity.class, search, e -> !e.isAlliedTo(XydraxEliteEntity.this)).isEmpty();
        }

        @Override
        protected void castSpell()
        {
            XydraxEliteEntity xydrax = XydraxEliteEntity.this;

            //Set up vortex bounds
            xydrax.vortexFloor = XydraxEliteEntity.this.blockPosition();
            xydrax.createVortexAABB();

            //Push Xydrax into air
            xydrax.push(0, 1.5F, 0);
            xydrax.setOnGround(false);

            //Initiate vortex
            xydrax.vortexTicks = xydrax.config().xydrax.vortexLifetime;
        }

        @Override
        public boolean canUse()
        {
            return super.canUse() && !XydraxEliteEntity.this.isInExtendedSpellState() && this.isEntityNearby();
        }

        @Override
        protected int getCastingTime()
        {
            return 80;
        }

        @Override
        protected int getCastingInterval()
        {
            return 700 + XydraxEliteEntity.this.vortexTicks;
        }

        @Override
        protected int getCastWarmupTime()
        {
            return 40;
        }

        @Nullable
        @Override
        protected SoundEvent getSpellPrepareSound()
        {
            return SoundEvents.BUCKET_FILL;
        }

        @Override
        protected SpellType getSpellType()
        {
            return SpellType.XYDRAX_VORTEX;
        }
    }

    private class XydraxWindColumnSpellGoal extends SpellcastingIllagerUseSpellGoal
    {
        private XydraxWindColumnSpellGoal() {}

        @Override
        protected void castSpell()
        {
            XydraxEliteEntity xydrax = XydraxEliteEntity.this;

            xydrax.getNavigation().stop();

            xydrax.summonWindColumns();
        }

        @Override
        public boolean canUse()
        {
            return super.canUse() && !XydraxEliteEntity.this.isInExtendedSpellState() && XydraxEliteEntity.this.onGround();
        }

        @Override
        protected int getCastingTime()
        {
            return 60;
        }

        @Override
        protected int getCastingInterval()
        {
            return 900 + XydraxEliteEntity.this.windColumns.size() * 2 * 20;
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
            return SoundEvents.GLASS_BREAK;
        }

        @Override
        protected SpellType getSpellType()
        {
            return SpellType.XYDRAX_WIND_COLUMN;
        }
    }

    private class XydraxHealSpellGoal extends SpellcastingIllagerUseSpellGoal
    {
        private XydraxHealSpellGoal() {}

        @Override
        protected void castSpell()
        {
            XydraxEliteEntity xydrax = XydraxEliteEntity.this;

            xydrax.push(0, 1 + xydrax.random.nextFloat(), 0);
            xydrax.setOnGround(false);

            xydrax.isHealing = true;
        }

        @Override
        public boolean canUse()
        {
            XydraxEliteEntity e = XydraxEliteEntity.this;
            return XydraxEliteEntity.this.tickCount >= this.spellCooldown && !XydraxEliteEntity.this.isCastingSpell() && !e.isInExtendedSpellState() && e.getHealth() < e.getMaxHealth() * 0.5;
        }

        @Override
        protected int getCastingTime()
        {
            return 40;
        }

        @Override
        protected int getCastingInterval()
        {
            return 640;
        }

        @Override
        protected int getCastWarmupTime()
        {
            return 6;
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
            return SpellType.XYDRAX_HEAL;
        }
    }

    private class XydraxBarrageSpellGoal extends SpellcastingIllagerUseSpellGoal
    {
        private XydraxBarrageSpellGoal() {}

        @Override
        protected void castSpell()
        {
            LivingEntity target = XydraxEliteEntity.this.getTarget();

            if(target != null)
            {
                int curseDuration = XydraxEliteEntity.this.config().xydrax.barrageCurseDuration;
                int curseAmplifier = XydraxEliteEntity.this.config().xydrax.barrageCurseAmplifier;

                for(int i = 0; i < 12; i++)
                {
                    Arrow arrow = new Arrow(XydraxEliteEntity.this.level(), XydraxEliteEntity.this)
                    {
                        @Override
                        protected void onHitBlock(BlockHitResult p_36755_) { super.onHitBlock(p_36755_); this.discard(); }

                        @Override
                        protected void onHitEntity(EntityHitResult pResult)
                        {
                            if(!(pResult.getEntity() instanceof Raider) || !XydraxEliteEntity.this.isAlliedTo(pResult.getEntity()))
                            {
                                super.onHitEntity(pResult);

                                if(pResult.getEntity() instanceof LivingEntity living)
                                    living.addEffect(new MobEffectInstance(DifficultRaidsEffects.WIND_CURSE_EFFECT.get(), curseDuration, curseAmplifier));
                            }
                        }
                    };

                    arrow.setPos(XydraxEliteEntity.this.getEyePosition().x(), XydraxEliteEntity.this.getEyePosition().y() - 0.2, XydraxEliteEntity.this.getEyePosition().z());

                    double targetY = target.getEyeY() - 1.1D;
                    double targetX = target.getX() - XydraxEliteEntity.this.getX();
                    double targetArrowY = targetY - arrow.getY();
                    double targetZ = target.getZ() - XydraxEliteEntity.this.getZ();
                    double distanceY = Math.sqrt(targetX * targetX + targetZ * targetZ) * (double)0.2F;

                    arrow.shoot(targetX, targetArrowY + distanceY, targetZ, 1.5F, 25.0F);
                    XydraxEliteEntity.this.level().addFreshEntity(arrow);
                }
            }
        }

        @Override
        public boolean canUse()
        {
            return super.canUse() && !XydraxEliteEntity.this.isInExtendedSpellState();
        }

        @Override
        protected int getCastingTime()
        {
            return 50;
        }

        @Override
        protected int getCastingInterval()
        {
            return 250;
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
            return SoundEvents.EVOKER_FANGS_ATTACK;
        }

        @Override
        protected SpellType getSpellType()
        {
            return SpellType.XYDRAX_ARROW_BARRAGE;
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
