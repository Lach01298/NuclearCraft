package nc.radiation;

import static nc.config.NCConfig.*;

import java.util.List;

import baubles.api.BaubleType;
import baubles.api.cap.*;
import ic2.api.reactor.IReactor;
import nc.ModCheck;
import nc.capability.radiation.IRadiation;
import nc.capability.radiation.entity.IEntityRads;
import nc.capability.radiation.resistance.IRadiationResistance;
import nc.capability.radiation.source.IRadiationChunk;
import nc.capability.radiation.source.IRadiationSource;
import nc.init.NCItems;
import nc.tile.dummy.TileDummy;
import nc.tile.radiation.ITileRadiationEnvironment;
import nc.util.*;
import net.minecraft.entity.*;
import net.minecraft.entity.passive.EntityHorse;
import net.minecraft.entity.player.*;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.potion.PotionEffect;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.*;
import net.minecraftforge.items.*;

public class RadiationHelper {
	
	// ------ Capability Getters ------ 
	
	public static IEntityRads getEntityRadiation(EntityLivingBase entity) {
		if (entity == null || !entity.hasCapability(IEntityRads.CAPABILITY_ENTITY_RADS, null)) {
			return null;
		}
		return entity.getCapability(IEntityRads.CAPABILITY_ENTITY_RADS, null);
	}
	
	public static IRadiationSource getRadiationSource(ICapabilityProvider provider) {
		if (provider == null || !provider.hasCapability(IRadiationSource.CAPABILITY_RADIATION_SOURCE, null)) {
			return null;
		}
		return provider.getCapability(IRadiationSource.CAPABILITY_RADIATION_SOURCE, null);
	}
	
	
	public static IRadiationChunk getRadiationChunk(ICapabilityProvider provider) {
		if (provider == null || !provider.hasCapability(IRadiationChunk.CAPABILITY_RADIATION_CHUNK, null)) {
			return null;
		}
		return provider.getCapability(IRadiationChunk.CAPABILITY_RADIATION_CHUNK, null);
	}
	
	
	public static IRadiationResistance getRadiationResistance(ICapabilityProvider provider) {
		if (provider == null || !provider.hasCapability(IRadiationResistance.CAPABILITY_RADIATION_RESISTANCE, null)) {
			return null;
		}
		return provider.getCapability(IRadiationResistance.CAPABILITY_RADIATION_RESISTANCE, null);
	}
	
	public static IItemHandler getTileInventory(ICapabilityProvider provider, EnumFacing side) {
		if (!(provider instanceof TileEntity) || provider instanceof TileDummy || !provider.hasCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, side)) {
			return null;
		}
		return provider.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, side);
	}
	
	public static IFluidHandler getTileTanks(ICapabilityProvider provider, EnumFacing side) {
		if (!(provider instanceof TileEntity) || provider instanceof TileDummy || !provider.hasCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, side)) {
			return null;
		}
		return provider.getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, side);
	}
	
	
	// ------ Radiation Getters ------
	
	public static double getRadiationFromStack(ItemStack stack, double multiplier) {
		if (stack.isEmpty()) {
			return 0D;
		}
		IRadiationSource stackSource = getRadiationSource(stack);
		return stackSource == null ? 0D : stackSource.getRadiationLevel() * stack.getCount() * multiplier;
	}
	
	public static double getRadiationFromFluid(FluidStack stack, double multiplier) {
		if (stack == null || stack.getFluid() == null) {
			return 0D;
		}
		return RadSources.FLUID_MAP.getDouble(stack.getFluid().getName()) * stack.amount * multiplier / 1000D;
	}
	

	// ------ Radiation Transfer ------
	
	// X -> Chunk
	
	public static void transferRadsFromInventoryToChunk(InventoryPlayer inventory, IRadiationChunk radiationChunk, int y) {

		double rads = 0;		
		for (ItemStack stack : inventory.mainInventory) {
			if (!stack.isEmpty()) {
				rads += getRadiationFromStack(stack, 1D);
			}
		}
		for (ItemStack stack : inventory.armorInventory) {
			if (!stack.isEmpty()) {
				rads += getRadiationFromStack(stack, 1D);
			}
		}
		for (ItemStack stack : inventory.offHandInventory) {
			if (!stack.isEmpty()) {
				rads += getRadiationFromStack(stack, 1D);
			}
		}
			
		radiationChunk.addSourceRadiationLevel(rads, radiationChunk.getSubChunkIDFromY(y));		
	}
	
	public static void transferRadsFromStackToChunk(ItemStack stack, IRadiationChunk radiationChunk, int y) {
		if (radiationChunk == null) {
			return;
		}
		radiationChunk.addSourceRadiationLevel(getRadiationFromStack(stack, 1D), radiationChunk.getSubChunkIDFromY(y));

	}
	
	public static void transferRadsFromTileToChunk(ICapabilityProvider provider, EnumFacing side, IRadiationChunk radiationChunk, int y) {
		if (radiationChunk == null) {
			return;
		}	
		double rads = 0D;
		
		if (ModCheck.ic2Loaded()) {
			if (provider instanceof IReactor) {
				rads += ((IReactor) provider).getReactorEUEnergyOutput() * 0.00001D;
			}
		}
		
		if (radiation_hardcore_containers > 0D) {
			IItemHandler inventory = getTileInventory(provider, side);
			if (inventory != null) {
				for (int i = 0; i < inventory.getSlots(); i++) {
					ItemStack stack = inventory.getStackInSlot(i);
					rads += getRadiationFromStack(stack, radiation_hardcore_containers);
				}
			}
			
			IFluidHandler tanks = getTileTanks(provider, side);
			if (tanks != null) {
				IFluidTankProperties[] props = tanks.getTankProperties();
				if (props != null) {
					for (IFluidTankProperties prop : props) {
						FluidStack stack = prop.getContents();
						rads += getRadiationFromFluid(stack, radiation_hardcore_containers);
					}
				}
			}
		}
				
		radiationChunk.addSourceRadiationLevel(rads, radiationChunk.getSubChunkIDFromY(y));
	}
	
	// Chunk -> X 
	
	public static double transferRadsFromChunkToPlayer(IRadiationChunk radiationChunk, IEntityRads playerRads, EntityPlayer player, int updateRate) {
		if (radiationChunk == null) {
			return 0D;
		}
		int subChunkID = radiationChunk.getSubChunkIDFromY(player.chunkCoordY);
		
		
		return addRadsToEntity(playerRads, player, radiationChunk.getSourceRadiationLevel(subChunkID) + radiationChunk.getFalloutRadiationLevel(subChunkID), false, updateRate);
	}
	
	
	// ------ Fallout ------
	
	
	public static void decayFallout(IRadiationChunk radiationChunk, int subChunkID, double decayRate)
	{
		double decay = radiationChunk.getFalloutRadiationLevel(subChunkID) * decayRate;
		
		radiationChunk.addFalloutRadiationLevel(-decay, subChunkID);
		
	}
	
	
	
	
	// ------ Radiation Resistance ------
	
	public static double getArmorInventoryRadResistance(Entity entity) {
		if (entity == null) {
			return 0D;
		}
		double resistance = 0D;
		for (ItemStack armor : entity.getArmorInventoryList()) {
			resistance += getArmorRadResistance(armor);
		}
		
		if (ModCheck.baublesLoaded() && entity.hasCapability(BaublesCapabilities.CAPABILITY_BAUBLES, null)) {
			IBaublesItemHandler baublesHandler = entity.getCapability(BaublesCapabilities.CAPABILITY_BAUBLES, null);
			if (baublesHandler != null) {
				for (int i = 0; i < baublesHandler.getSlots(); i++) {
					resistance += getArmorRadResistance(baublesHandler.getStackInSlot(i));
				}
			}
		}
		
		return resistance;
	}
	
	private static double getArmorRadResistance(ItemStack armor) {
		if (armor.isEmpty()) {
			return 0D;
		}
		double resistance = 0D;
		IRadiationResistance armorResistance = getRadiationResistance(armor);
		if (armorResistance != null) {
			resistance += armorResistance.getTotalRadResistance();
		}
		if (armor.hasTagCompound() && armor.getTagCompound().hasKey("ncRadiationResistance")) {
			resistance += armor.getTagCompound().getDouble("ncRadiationResistance");
		}
		return resistance;
	}
	
	public static double getEntityArmorRadResistance(EntityLivingBase entity) {
		double resistance = getArmorInventoryRadResistance(entity);
		if (radiation_horse_armor_public && entity instanceof EntityHorse) {
			resistance += getHorseArmorRadResistance((EntityHorse) entity);
		}
		return resistance;
	}
	
	private static double getHorseArmorRadResistance(EntityHorse horse) {
		double resistance = 0D;
		NBTTagCompound compound = new NBTTagCompound();
		horse.writeEntityToNBT(compound);
		
		ItemStack armor = new ItemStack(compound.getCompoundTag("ArmorItem"));
		if (ArmorHelper.isHorseArmor(armor.getItem())) {
			resistance += getArmorRadResistance(armor);
		}
		return resistance;
	}
	
	
	
	// ------ Entity Radiation ------
	
	public static double addRadsToEntity(IEntityRads entityRads, EntityLivingBase entity, double rawRadiation, boolean ignoreResistance, int updateRate) {
		if (rawRadiation <= 0D) {
			return 0D;
		}

		double resistance = ignoreResistance ? Math.min(0D, entityRads.getInternalRadiationResistance()) : entityRads.getFullRadiationResistance();
		
		double resistanceEffectivness = 0.2; //TODO make a config. The amount of radiation blocked by each 1 resistance
		
		
		double addedRadiation = rawRadiation * Math.pow((1D - resistanceEffectivness),resistance);
		
		
		
		//double addedRadiation = resistance > 0D ? NCMath.sq(rawRadiation) / (rawRadiation + resistance) : rawRadiation * (1D - resistance);
		entityRads.setTotalRads(entityRads.getTotalRads() + addedRadiation * updateRate, true);
		return addedRadiation;
	}
	
	public static void applyPotionEffects(EntityLivingBase entity, IEntityRads entityRads, int durationMult, List<Double> radLevelList, List<List<PotionEffect>> potionList) {
		if (radLevelList.isEmpty() || radLevelList.size() != potionList.size()) {
			return;
		}
		double radPercentage = entityRads.getRadsPercentage();
		
		for (int i = 0; i < radLevelList.size(); i++) {
			final int j = radLevelList.size() - 1 - i;
			if (radPercentage >= radLevelList.get(j)) {
				for (PotionEffect potionEffect : potionList.get(j)) {
					entity.addPotionEffect(new PotionEffect(potionEffect.getPotion(), potionEffect.getDuration() * durationMult, potionEffect.getAmplifier(), potionEffect.getIsAmbient(), potionEffect.doesShowParticles()));
				}
				break;
			}
		}
	}
	
	
	// ------ Radiation UI ------
	
	// Radiation HUD
	
		public static boolean shouldShowHUD(EntityPlayer player) {
			if (!player.hasCapability(IEntityRads.CAPABILITY_ENTITY_RADS, null)) {
				return false;
			}
			if (!radiation_require_counter) {
				return true;
			}
			
			final ItemStack geiger_counter = new ItemStack(NCItems.geiger_counter), geiger_block = new ItemStack(NCItems.geiger_counter);
			
			if (ModCheck.baublesLoaded() && player.hasCapability(BaublesCapabilities.CAPABILITY_BAUBLES, null)) {
				IBaublesItemHandler baublesHandler = player.getCapability(BaublesCapabilities.CAPABILITY_BAUBLES, null);
				if (baublesHandler == null) {
					return false;
				}
				
				for (int slot : BaubleType.TRINKET.getValidSlots()) {
					if (baublesHandler.getStackInSlot(slot).isItemEqual(geiger_counter)) {
						return true;
					}
				}
			}
			
			return player.inventory.hasItemStack(geiger_counter) || player.inventory.hasItemStack(geiger_block);
		}
		
		// Text Colours
		
		public static TextFormatting getRadsTextColor(IEntityRads entityRads) {
			double radsPercent = entityRads.getRadsPercentage();
			return radsPercent < 30 ? TextFormatting.WHITE : radsPercent < 50 ? TextFormatting.YELLOW : radsPercent < 70 ? TextFormatting.GOLD : radsPercent < 90 ? TextFormatting.RED : TextFormatting.DARK_RED;
		}
		
		public static TextFormatting getRadiationTextColor(double radiation) {
			return radiation < 0.000000001D ? TextFormatting.WHITE : radiation < 0.001D ? TextFormatting.YELLOW : radiation < 0.1D ? TextFormatting.GOLD : radiation < 1D ? TextFormatting.RED : TextFormatting.DARK_RED;
		}
		
		public static TextFormatting getRadiationTextColor(IRadiation irradiated) {
			return getRadiationTextColor(irradiated.getRadiationLevel());
		}
		
		public static TextFormatting getRawRadiationTextColor(IEntityRads entityRads) {
			return getRadiationTextColor(entityRads.getRawRadiationLevel());
		}
		
		public static TextFormatting getFoodRadiationTextColor(double radiation) {
			return radiation <= -100D ? TextFormatting.LIGHT_PURPLE : radiation <= -10D ? TextFormatting.BLUE : radiation < 0D ? TextFormatting.AQUA : radiation < 0.1D ? TextFormatting.WHITE : radiation < 1D ? TextFormatting.YELLOW : radiation < 10D ? TextFormatting.GOLD : radiation < 100D ? TextFormatting.RED : TextFormatting.DARK_RED;
		}
		
		public static TextFormatting getFoodResistanceTextColor(double resistance) {
			return resistance < 0D ? TextFormatting.GRAY : TextFormatting.WHITE;
		}
		
		// Unit Prefixing
		
		public static String radsPrefix(double rads, boolean rate) {
			String unit = rate ? "Rad/t" : "Rad";
			return radiation_unit_prefixes > 0 ? NCMath.sigFigs(rads, radiation_unit_prefixes) + " " + unit : UnitHelper.prefix(rads, 3, unit);
		}
		
		public static String radsColoredPrefix(double rads, boolean rate) {
			return getRadiationTextColor(rads) + radsPrefix(rads, rate);
		}
		
		// Rad Resistance Sig Figs
		
		public static String resistanceSigFigs(double resistance) {
			return NCMath.sigFigs(resistance, Math.max(2, radiation_unit_prefixes));
		}
	
	
	
	
	
	// ------ END ------
	
	
	
	
	
	
	
	
	
	// ITileRadiationEnvironment -> ChunkBuffer
	
	public static void addScrubbingFractionToChunk(IRadiationSource chunkSource, ITileRadiationEnvironment tile) {
	/*	if (chunkSource == null) {
			return;
		}
		if (radiation_scrubber_non_linear) {
			if (tile.getRadiationContributionFraction() < 0D) {
				chunkSource.setEffectiveScrubberCount(chunkSource.getEffectiveScrubberCount() - tile.getRadiationContributionFraction());
			}
		}
		else {
			addToSourceBuffer(chunkSource, tile.getRadiationContributionFraction() * tile.getCurrentChunkRadiationBuffer());
			
			if (tile.getRadiationContributionFraction() < 0D) {
				chunkSource.setScrubbingFraction(chunkSource.getScrubbingFraction() - tile.getRadiationContributionFraction());
				chunkSource.setEffectiveScrubberCount(chunkSource.getEffectiveScrubberCount() - tile.getRadiationContributionFraction());
			}
		}*/
	}
	
	public static double getAltScrubbingFraction(double scrubbers) {
		return scrubbers <= 0D ? 0D : 1D - Math.pow(radiation_scrubber_param[0], -Math.pow(scrubbers / radiation_scrubber_param[1], Math.pow(scrubbers / radiation_scrubber_param[2] + 1D, 1D / radiation_scrubber_param[3])));
	}
	

	
	
	
	
	
	
	
	

	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	// Chunk Set Previous Radiation and Spread
	
	public static void spreadRadiationFromChunk(Chunk chunk, Chunk targetChunk) {
		if (chunk != null && chunk.isLoaded()) {
			IRadiationSource chunkSource = getRadiationSource(chunk);
			if (chunkSource == null) {
				return;
			}
			
			if (targetChunk != null && targetChunk.isLoaded()) {
				IRadiationSource targetChunkSource = getRadiationSource(targetChunk);
				if (targetChunkSource != null && !chunkSource.isRadiationNegligible() && (targetChunkSource.getRadiationLevel() == 0D || chunkSource.getRadiationLevel() / targetChunkSource.getRadiationLevel() > 1D + radiation_spread_gradient)) {
					double radiationSpread = (chunkSource.getRadiationLevel() - targetChunkSource.getRadiationLevel()) * radiation_spread_rate;
					chunkSource.setRadiationLevel(chunkSource.getRadiationLevel() - radiationSpread);
					targetChunkSource.setRadiationLevel(targetChunkSource.getRadiationLevel() + radiationSpread * (1D - targetChunkSource.getScrubbingFraction()));
				}
			}
			
			chunkSource.setRadiationBuffer(0D);
			if (chunkSource.isRadiationNegligible()) {
				chunkSource.setRadiationLevel(0D);
			}
		}
	}
	
	// Player Radiation Resistance
	
	
	
	
	
	// Entity Radiation Resistance
	
	
	
	
	
	// Inventory -> Player
	
	public static double transferRadsFromInventoryToPlayer(IEntityRads playerRads, EntityPlayer player, int updateRate) {
		double radiationLevel = 0D;
		InventoryPlayer inventory = player.inventory;
		for (ItemStack stack : inventory.mainInventory) {
			if (!stack.isEmpty()) {
				radiationLevel += transferRadsFromStackToPlayer(stack, playerRads, player, updateRate);
			}
		}
		for (ItemStack stack : inventory.armorInventory) {
			if (!stack.isEmpty()) {
				radiationLevel += transferRadsFromStackToPlayer(stack, playerRads, player, updateRate);
			}
		}
		for (ItemStack stack : inventory.offHandInventory) {
			if (!stack.isEmpty()) {
				radiationLevel += transferRadsFromStackToPlayer(stack, playerRads, player, updateRate);
			}
		}
		return radiationLevel;
	}
	
	private static double transferRadsFromStackToPlayer(ItemStack stack, IEntityRads playerRads, EntityPlayer player, int updateRate) {
		IRadiationSource stackSource = getRadiationSource(stack);
		if (stackSource == null) {
			return 0D;
		}
		return addRadsToEntity(playerRads, player, stackSource.getRadiationLevel() * stack.getCount(), false, updateRate);
	}
	
	
	

	
	// Biome -> Player
	
	/* public static double transferBackgroundRadsToPlayer(Biome biome, IEntityRads playerRads, EntityPlayer player, int updateRate) { Double biomeRadiation = RadBiomes.RAD_MAP.get(biome); if (biomeRadiation == null) { return 0D; } return addRadsToPlayer(playerRads, player, biomeRadiation, updateRate); } */
	
	// Source -> Entity
	
	public static void transferRadsFromSourceToEntity(IRadiationSource source, IEntityRads entityRads, EntityLivingBase entity, int updateRate) {
		if (source == null) {
			return;
		}
		entityRads.setRadiationLevel(addRadsToEntity(entityRads, entity, source.getRadiationLevel(), false, updateRate));
	}
	
	// Biome -> Entity
	
	/* public static void transferBackgroundRadsToEntity(Biome biome, IEntityRads entityRads, EntityLiving entityLiving, int updateRate) { Double biomeRadiation = RadBiomes.RAD_MAP.get(biome); if (biomeRadiation != null) { entityRads.setRadiationLevel(addRadsToEntity(entityRads, entityLiving, biomeRadiation, updateRate)); } } */
	
	// Entity Symptoms
	
	
	
	
}
