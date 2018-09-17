package com.replaymod.recording.utils;

import com.replaymod.recording.packet.PacketListener;
import lombok.AllArgsConstructor;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IContainerListener;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.SlotCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.server.SPacketSetSlot;
import net.minecraft.network.play.server.SPacketWindowItems;
import net.minecraft.network.play.server.SPacketWindowProperty;
import net.minecraft.util.NonNullList;

import org.apache.logging.log4j.Logger;

@AllArgsConstructor
public class ContainerListener implements IContainerListener {

    Logger logger;
    PacketListener listener;

    public void sendSlotContents(Container p_sendSlotContents_1_, int p_sendSlotContents_2_, ItemStack p_sendSlotContents_3_) {
        if (!(p_sendSlotContents_1_.getSlot(p_sendSlotContents_2_) instanceof SlotCrafting)) {
            if (p_sendSlotContents_1_ == this.inventoryContainer) {
                CriteriaTriggers.INVENTORY_CHANGED.trigger(this, this.inventory);
            }

            if (!this.isChangingQuantityOnly) {
                this.connection.sendPacket(new SPacketSetSlot(p_sendSlotContents_1_.windowId, p_sendSlotContents_2_, p_sendSlotContents_3_));
            }
        }

    }

    @Override
    public void sendAllContents(Container containerToSend, NonNullList<ItemStack> itemsList) {

        listener.save(new SPacketWindowItems(containerToSend.windowId, itemsList));
        listener.save(new SPacketSetSlot(-1, -1, this.inventory.getItemStack());
        logger.info("Send all contents");
    }

    @Override
    public void sendSlotContents(Container containerToSend, int slotInd, ItemStack stack) {
        if (!(containerToSend.getSlot(slotInd) instanceof SlotCrafting)) {
            if (containerToSend == this.inventoryContainer) {
                CriteriaTriggers.INVENTORY_CHANGED.trigger(this, this.inventory);
            }

            if (!this.isChangingQuantityOnly) {
                listener.save(new SPacketSetSlot(containerToSend.windowId, slotInd, stack));
            }
        }
        logger.info("Send slot Contents");
    }

    @Override
    public void sendWindowProperty(Container containerIn, int varToUpdate, int newValue) {
        listener.save(new SPacketWindowProperty(containerIn.windowId, varToUpdate, newValue));
        logger.info("Send window poperty");
    }

    @Override
    public void sendAllWindowProperties(Container containerIn, IInventory inventory) {
        SPacketWindowProperty windowProperty;
        for(int i = 0; i < inventory.getFieldCount(); ++i) {
            listener.save(new SPacketWindowProperty(containerIn.windowId, i, inventory.getField(i)));
        }
        logger.info("Send all window poperties");
    }
}
