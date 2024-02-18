package io.github.lightman314.lightmanscurrency.common.enchantments;

import java.util.Map.Entry;

import io.github.lightman314.lightmanscurrency.LCConfig;
import io.github.lightman314.lightmanscurrency.LightmansCurrency;
import io.github.lightman314.lightmanscurrency.api.money.MoneyAPI;
import io.github.lightman314.lightmanscurrency.api.money.value.MoneyValue;
import io.github.lightman314.lightmanscurrency.api.money.value.MoneyView;
import io.github.lightman314.lightmanscurrency.api.money.value.holder.IMoneyHolder;
import io.github.lightman314.lightmanscurrency.common.core.ModEnchantments;
import io.github.lightman314.lightmanscurrency.integration.curios.LCCurios;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentCategory;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;

import javax.annotation.Nonnull;

@SuppressWarnings("removal")
public class MoneyMendingEnchantment extends Enchantment {

	public MoneyMendingEnchantment(Rarity rarity, EquipmentSlot... slots) { super(rarity, EnchantmentCategory.BREAKABLE, slots); }
	
	public int getMinCost(int level) { return level * 25; }

	public int getMaxCost(int level) { return this.getMinCost(level) + 50; }

	public boolean isTreasureOnly() { return true; }

	public int getMaxLevel() { return 1; }
	
	protected boolean checkCompatibility(@Nonnull Enchantment otherEnchant) {
		return otherEnchant != Enchantments.MENDING && super.checkCompatibility(otherEnchant);
	}
	
	public static MoneyValue getRepairCost() { return LCConfig.SERVER.moneyMendingRepairCost.get(); }
	
	public static void runEntityTick(Player player)
	{
		//Go through the players inventory searching for items with the money mending enchantment
		Entry<EquipmentSlot,ItemStack> entry = EnchantmentHelper.getRandomItemWith(ModEnchantments.MONEY_MENDING.get(), player, ItemStack::isDamaged);
		ItemStack item;
		if(entry == null)
		{
			//If we failed to get a vanilla entry, check the curios slots (if applicable)
			if(LightmansCurrency.isCuriosValid(player))
				item = LCCurios.getMoneyMendingItem(player);
			else
				item = null;
		}
		else
			item = entry.getValue();
		if(item != null)
		{
			//Only bother calculating the repair cost until we have a confirmed mending target to reduce lag
			MoneyValue repairCost = MoneyMendingEnchantment.getRepairCost();
			IMoneyHolder handler = MoneyAPI.API.GetPlayersMoneyHandler(player);
			MoneyView availableFunds = handler.getStoredMoney();
			if(!availableFunds.containsValue(repairCost))
				return;

			//Repair the item
			MoneyValue nextCost = repairCost;
			MoneyValue finalCost = MoneyValue.empty();
			int currentDamage = item.getDamageValue();
			int repairAmount = 0;
			while (availableFunds.containsValue(nextCost) && repairAmount < currentDamage) {
				repairAmount++;
				finalCost = nextCost;
				nextCost = nextCost.addValue(repairCost);
			}
			//Take the payment from the player
			if(handler.extractMoney(finalCost, true).isEmpty())
			{
				//If payment was successful, repair the item
				handler.extractMoney(finalCost, false);
				item.setDamageValue(currentDamage - repairAmount);
			}
		}
	}
	
}
