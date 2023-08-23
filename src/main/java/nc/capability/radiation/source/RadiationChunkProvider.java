package nc.capability.radiation.source;

import javax.annotation.*;

import net.minecraft.nbt.NBTBase;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.*;

public class RadiationChunkProvider implements ICapabilitySerializable {
	
	private final IRadiationChunk radiationChunk;
	
	public RadiationChunkProvider(double[] sourceRadiation, double[] falloutRadiation) {
		radiationChunk = new RadiationChunk(sourceRadiation,falloutRadiation);
		
	}
	
	@Override
	public boolean hasCapability(@Nonnull Capability<?> capability, @Nullable EnumFacing facing) {
		return capability == IRadiationChunk.CAPABILITY_RADIATION_CHUNK;
	}
	
	@Override
	public <T> T getCapability(@Nonnull Capability<T> capability, @Nullable EnumFacing facing) {
		if (capability == IRadiationChunk.CAPABILITY_RADIATION_CHUNK) {
			return IRadiationChunk.CAPABILITY_RADIATION_CHUNK.cast(radiationChunk);
		}
		return null;
	}
	
	@Override
	public NBTBase serializeNBT() {
		return IRadiationChunk.CAPABILITY_RADIATION_CHUNK.writeNBT(radiationChunk, null);
	}
	
	@Override
	public void deserializeNBT(NBTBase nbt) {
		IRadiationChunk.CAPABILITY_RADIATION_CHUNK.readNBT(radiationChunk, null, nbt);
	}
}