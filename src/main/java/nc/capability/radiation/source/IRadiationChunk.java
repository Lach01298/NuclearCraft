package nc.capability.radiation.source;

import nc.Global;
import nc.capability.ICapability;
import nc.capability.radiation.IRadiation;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.capabilities.*;

public interface IRadiationChunk extends ICapability<IRadiationChunk> {

	@CapabilityInject(IRadiationChunk.class)
	public static Capability<IRadiationChunk> CAPABILITY_RADIATION_CHUNK = null;
	
	public static final ResourceLocation CAPABILITY_RADIATION_CHUNK_NAME = new ResourceLocation(Global.MOD_ID, "capability_radiation_chunk");
	
	public double getSourceRadiationLevel(int subChunkID); 	
	public double getFalloutRadiationLevel(int subChunkID);
	public void setSourceRadiationLevel(double newRads, int subChunkID);
	public void addSourceRadiationLevel(double newRads, int subChunkID);
	public void setFalloutRadiationLevel(double newRads, int subChunkID);
	public void addFalloutRadiationLevel(double newRads, int subChunkID);
	public int getSubChunkIDFromY(int y);
}
