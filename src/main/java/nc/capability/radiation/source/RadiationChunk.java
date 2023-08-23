package nc.capability.radiation.source;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;

public class RadiationChunk implements IRadiationChunk{

	private double[] sourceRadiation = new double[16], falloutRadiation = new double[16];
	
	
	public RadiationChunk(double[] sourceRadiation, double[] falloutRadiation) {
		this.sourceRadiation = sourceRadiation;
		this.falloutRadiation = falloutRadiation;
	}
	
	@Override
	public NBTTagCompound writeNBT(IRadiationChunk instance, EnumFacing side, NBTTagCompound nbt) {
		
		
		for (int i = 0; i < sourceRadiation.length; i++)
		{
			nbt.setDouble("sourceRadiation"+i, getSourceRadiationLevel(i));
		}
		
		for (int i = 0; i < falloutRadiation.length; i++)
		{
			nbt.setDouble("falloutRadiation"+i, getFalloutRadiationLevel(i));
		}

		return nbt;
	}
	
	@Override
	public void readNBT(IRadiationChunk instance, EnumFacing side, NBTTagCompound nbt) {
		
		for (int i = 0; i < sourceRadiation.length; i++)
		{
			setSourceRadiationLevel(nbt.getDouble("sourceRadiation"+i),i);
		}
		
		for (int i = 0; i < falloutRadiation.length; i++)
		{
			setFalloutRadiationLevel(nbt.getDouble("falloutRadiation"+i),i);
		}

	}
	
	@Override
	public double getSourceRadiationLevel(int subChunkID) 
	{
		return sourceRadiation[subChunkID];
	}
	
	@Override
	public double getFalloutRadiationLevel(int subChunkID) 
	{
		return falloutRadiation[subChunkID];
	}

	@Override
	public void setSourceRadiationLevel(double newRads, int subChunkID) {
		sourceRadiation[subChunkID] = Math.max(newRads, 0D);
		
	}
	
	@Override
	public void addSourceRadiationLevel(double newRads, int subChunkID) {
		sourceRadiation[subChunkID] += Math.max(newRads, 0D);
		
	}
	
	@Override
	public void setFalloutRadiationLevel(double newRads, int subChunkID) {
		falloutRadiation[subChunkID] = Math.max(newRads, 0D);
	}
	
	@Override
	public void addFalloutRadiationLevel(double newRads, int subChunkID) {
		falloutRadiation[subChunkID] += Math.max(newRads, 0D);
	}
	
	@Override
	public int getSubChunkIDFromY(int y) {
		return y/16;
	}


	
	
	
	
}
