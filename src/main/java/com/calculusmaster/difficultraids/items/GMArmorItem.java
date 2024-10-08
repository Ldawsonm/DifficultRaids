package com.calculusmaster.difficultraids.items;

import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class GMArmorItem extends ArmorItem
{
    private static final Properties DEFAULT_PROPERTIES = new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant();

    public GMArmorItem(ArmorItem.Type pSlot)
    {
//        ArmorItem.Type[] types = {Type.HELMET, Type.CHESTPLATE, Type.LEGGINGS, Type.BOOTS};
//        for(ArmorItem.Type type: types) {
//            if (pSlot.equals(type.getSlot())) {
//                super(ArmorMaterials.NETHERITE, type, DEFAULT_PROPERTIES);
//            }
//        }
        super(ArmorMaterials.NETHERITE, pSlot, DEFAULT_PROPERTIES);
    }

    @Override
    public void onInventoryTick(ItemStack stack, Level level, Player player, int slotIndex, int selectedIndex)
    {
        if(this.hasFullSet(player))
        {
            if(!player.hasEffect(MobEffects.REGENERATION))
                player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 1));

            if(!player.hasEffect(MobEffects.MOVEMENT_SPEED))
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 100, 0));
        }
    }

    @Override
    public void appendHoverText(ItemStack pStack, @Nullable Level pLevel, List<Component> pTooltipComponents, TooltipFlag pIsAdvanced)
    {
        pTooltipComponents.add(Component.literal("Reduces damage taken from Raiders by 10%.\nFull Set Bonus: Applies Regeneration II & Speed I constantly."));
    }

    public float getRaiderDamageReduction()
    {
        return 0.1F;
    }

    private boolean hasFullSet(Player player)
    {
        for(ItemStack is : player.getArmorSlots()) if(!(is.getItem() instanceof GMArmorItem)) return false;
        return true;
    }
}
