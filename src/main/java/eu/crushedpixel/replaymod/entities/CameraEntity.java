package eu.crushedpixel.replaymod.entities;

import java.lang.reflect.Field;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.server.S03PacketTimeUpdate;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

import org.lwjgl.Sys;

import eu.crushedpixel.replaymod.holders.Position;
import eu.crushedpixel.replaymod.replay.LesserDataWatcher;
import eu.crushedpixel.replaymod.replay.ReplayHandler;
import eu.crushedpixel.replaymod.replay.TimeHandler;

public class CameraEntity extends EntityPlayer {

	private Vec3 direction;
	private double motion;

	private Field drawBlockOutline;

	private static final double SPEED = 10; //2 blocks per second

	private double decay = 6; //decays by 75% per second;

	private long lastCall = 0;

	//frac = time since last tick
	public void updateMovement() {
		Minecraft mc = Minecraft.getMinecraft();
		if(ReplayHandler.getCameraEntity() != null && mc.thePlayer != null 
				&& mc.getRenderViewEntity() != null) {
			//Aligns the particle rotation
			mc.thePlayer.rotationPitch = mc.getRenderViewEntity().rotationPitch;
			mc.thePlayer.rotationYaw = mc.getRenderViewEntity().rotationYaw;

			/*
			mc.thePlayer.posX = mc.getRenderViewEntity().posX;
			mc.thePlayer.posY = mc.getRenderViewEntity().posY;
			mc.thePlayer.posZ = mc.getRenderViewEntity().posZ;
			
			TimeHandler.setDesiredDaytime(18000);
			TimeHandler.setTimeOverridden(true);
			*/
			
			//removes water/suffocation/shadow overlays in screen
			mc.thePlayer.posX = 0;
			mc.thePlayer.posY = 500;
			mc.thePlayer.posZ = 0;
		}

		if(direction == null || motion < 0.1) {
			lastCall = Sys.getTime();
			return;
		}

		long frac = Sys.getTime() - lastCall;

		if(frac == 0) return;

		Vec3 movement = direction.normalize();
		double factor = motion*(frac/1000D);

		moveRelative(movement.xCoord*factor, movement.yCoord*factor, movement.zCoord*factor);

		double decFac = Math.max(0, 1-(decay*(frac/1000D)));
		motion *= decFac;

		lastCall = Sys.getTime();
	}

	public void setDirection(float pitch, float yaw) {
		this.setRotation(yaw, pitch);
	}

	public void setMovement(MoveDirection dir) {
		Vec3 oldDir = direction;

		switch(dir) {
		case BACKWARD:
			direction = this.getVectorForRotation(-rotationPitch, rotationYaw-180);
			break;
		case DOWN:
			direction = this.getVectorForRotation(90, 0);
			break;
		case FORWARD:
			direction = this.getVectorForRotation(rotationPitch, rotationYaw);
			break;
		case LEFT:
			direction = this.getVectorForRotation(0, rotationYaw-90);
			break;
		case RIGHT:
			direction = this.getVectorForRotation(0, rotationYaw+90);
			break;
		case UP:
			direction = this.getVectorForRotation(-90, 0);
			break;
		}

		if(oldDir != null) direction = direction.normalize().add(new Vec3(oldDir.xCoord*(motion/4f), oldDir.yCoord*(motion/4f), oldDir.zCoord*(motion/4f)).normalize());

		this.motion = SPEED;
	}

	public void moveAbsolute(double x, double y, double z) {
		if(ReplayHandler.isInPath()) return;
		this.lastTickPosX = this.prevPosX = this.posX = x;
		this.lastTickPosY = this.prevPosY = this.posY = y;
		this.lastTickPosZ = this.prevPosZ = this.posZ = z;
	}

	public void moveRelative(double x, double y, double z) {
		if(ReplayHandler.isInPath()) return;
		this.lastTickPosX = this.prevPosX = this.posX = this.posX+x;
		this.lastTickPosY = this.prevPosY = this.posY = this.posY+y;
		this.lastTickPosZ = this.prevPosZ = this.posZ = this.posZ+z;
	}

	public void movePath(Position pos) {
		this.prevRotationPitch = this.rotationPitch = pos.getPitch();
		this.prevRotationYaw = this.rotationYaw = pos.getYaw();
		this.lastTickPosX = this.prevPosX = this.posX = pos.getX();
		this.lastTickPosY = this.prevPosY = this.posY = pos.getY();
		this.lastTickPosZ = this.prevPosZ = this.posZ = pos.getZ();
	}

	@Override
	protected void entityInit() {
		this.dataWatcher = new LesserDataWatcher(this);
	}


	public CameraEntity(World worldIn) {
		//super(worldIn);
		super(worldIn, Minecraft.getMinecraft().getSession().getProfile());
	}

	@Override
	public void setAngles(float yaw, float pitch)
	{
		this.rotationYaw = (float)((double)this.rotationYaw + (double)yaw * 0.15D);
		this.rotationPitch = (float)((double)this.rotationPitch - (double)pitch * 0.15D);
		this.rotationPitch = MathHelper.clamp_float(this.rotationPitch, -90.0F, 90.0F);
		this.prevRotationPitch = this.rotationPitch;
		this.prevRotationYaw = this.rotationYaw;
	}


	@Override
	public MovingObjectPosition rayTrace(double p_174822_1_, float p_174822_3_) {
		return null;
	}

	@Override
	public boolean canBePushed() {
		return false;
	}

	@Override
	protected void createRunningParticles() {}

	@Override
	public boolean canBeCollidedWith() {
		return false;
	}
	@Override
	public boolean canRenderOnFire() {
		return false;
	}

	public enum MoveDirection {
		UP, DOWN, LEFT, RIGHT, FORWARD, BACKWARD;
	}

	@Override
	public void setCurrentItemOrArmor(int slotIn, ItemStack stack) {}

	@Override
	public ItemStack[] getInventory() {
		return null;
	}

	@Override
	public boolean isSpectator() {
		return true;
	}

}
