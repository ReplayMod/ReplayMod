package eu.crushedpixel.replaymod.replay;

import java.io.IOException;
import java.util.List;

import scala.actors.threadpool.Arrays;
import net.minecraft.entity.DataWatcher;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.Rotations;

public class LesserDataWatcher extends DataWatcher {

	public LesserDataWatcher(Entity owner) {
		super(owner);
	}

	@Override
	public void addObject(int id, Object object) {
	}

	@Override
	public void addObjectByDataType(int id, int type) {
	}

	@Override
	public byte getWatchableObjectByte(int id) {
		return 0;
	}

	@Override
	public short getWatchableObjectShort(int id) {
		return 0;
	}

	@Override
	public int getWatchableObjectInt(int id) {
		return 0;
	}

	@Override
	public float getWatchableObjectFloat(int id) {
		return 0;
	}

	@Override
	public String getWatchableObjectString(int id) {
		return null;
	}

	@Override
	public ItemStack getWatchableObjectItemStack(int id) {
		return null;
	}

	@Override
	public Rotations getWatchableObjectRotations(int id) {
		return null;
	}

	@Override
	public void updateObject(int id, Object newData) {
	}

	@Override
	public void setObjectWatched(int id) {
	}

	@Override
	public boolean hasObjectChanged() {
		return false;
	}

	@Override
	public List getChanged() {
		return null;
	}

	@Override
	public void writeTo(PacketBuffer buffer) throws IOException {
	}

	@Override
	public List getAllWatched() {
		return null;
	}

	@Override
	public void updateWatchedObjectsFromList(List p_75687_1_) {
	}

	@Override
	public boolean getIsBlank() {
		return true;
	}

	@Override
	public void func_111144_e() {
	}
	
	

	
}
