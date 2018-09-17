package com.replaymod.recording.events;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.ClickType;
import net.minecraft.inventory.ContainerPlayer;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraftforge.client.event.GuiContainerEvent;
import net.minecraftforge.fml.common.eventhandler.Event;
/**
 * Event class for handling player interaction with guiContainers.
 */
public class GuiContainerActionEvent extends Event {
    //    private final GuiContainer guiContainer;
//    public GuiContainerActionEvent(GuiContainer guiContainer)
//    {
//        this.guiContainer = guiContainer;
//    }
//    public GuiContainer getGuiContainer()
//    {
//        return guiContainer;
//    }
    public static class SlotUpdate extends Event{
        private  int guiWindowId;
        private int slotId;
        private int mouseButton;
        private ClickType type;
        private ItemStack itemStack;
        private short transactionID;
        /**
         * Called directly after the GuiContainer has determined the effect of a keyboardInputEvent or mouseInputEvent
         *
         * @param guiWindowId The gui window id of the active container.
         * @param itemStack    The item stack of current mouse click event.
         * @param slotId       The slot id of current mouse click event.
         * @param mouseButton  The id of the current mouse button for this event, 0 for left click 1 for right.
         * @param type         The click type of the current event.
         */
        public SlotUpdate(int guiWindowId, ItemStack itemStack, int slotId, int mouseButton, ClickType type, short transactionID)
        {
//            super(guiContainer);
            this.guiWindowId = guiWindowId;
            this.itemStack = itemStack;
            this.mouseButton = mouseButton;
            this.slotId = slotId;
            this.type = type;
            this.transactionID = transactionID;
        }
        public int getGuiWindowId() { return guiWindowId; }
        public ClickType getClickType() { return type; }
        public int getMouseButton() { return mouseButton; }
        public int getSlotId() { return slotId; }
        public ItemStack getItemStack() { return itemStack; }
        public short getTransationID() {return  transactionID; }
        @Override
        public String toString()
        {
            String message = "";
            message += "Item Stack " + itemStack.toString() + "\n";
            message += "Slot id:" + Integer.toString(slotId) + "\t";
            message += "Mouse button:" + Integer.toString(mouseButton) + "\t";
            message += "Click type:" + type.toString() + "\t";
            message += "Transaction ID" + Short.toString(transactionID) + "\t";
            return message;
        }
    }
    public static class RecipeClicked extends Event{
        @Getter int windowID;
        @Getter IRecipe recipe;
        @Getter boolean shiftPressed;
        @Getter EntityPlayer player;
        /**
         * Called directly after the GuiContainer has determined the effect of a keyboardInputEvent or mouseInputEvent
         *
         * @param windowID     The current window id of the container
         * @param recipe       The recipe clicked on by the player
         * @param shiftPressed  The state of the shift key (true iff pressed)
         * @param player       The current player context
         */
        public RecipeClicked(int windowID, IRecipe recipe, boolean shiftPressed, EntityPlayer player)
        {
//            super(guiContainer);
            this.windowID = windowID;
            this.recipe = recipe;
            this.shiftPressed = shiftPressed;
            this.player = player;
        }
        @Override
        public String toString()
        {
            String message = "";
            message += "Num: " + Integer.toString(windowID) + "\t";
            message += "Recipe: " + recipe.getGroup() + "\t";
            message += "Output: " + recipe.getRecipeOutput().getDisplayName() + "\t";
            message += "Inputs: " + recipe.getIngredients().toString() + "\t";
            message += "Can Craft: " + Boolean.toString(player.inventoryContainer.getCanCraft(player)) + "\t";
            if (player.inventoryContainer instanceof  ContainerPlayer) {
                ContainerPlayer foo = (ContainerPlayer) player.inventoryContainer;
                message += "Remaining: " + recipe.getRemainingItems(foo.craftMatrix) + "\t";
            }
            message += "Shift: " + Boolean.toString(shiftPressed) + "\t";
            message += "Player Window: " + Integer.toString(player.inventoryContainer.windowId);
            return message;
        }
    }

    public static class WindowClosed extends Event{
        @Getter int windowID;
        @Getter ItemStack heldStack;

        /**
         * Called directly after the GuiContainer has determined the effect of a keyboardInputEvent or mouseInputEvent
         *
         * @param windowID     The current window id of the container
         * @param heldStack    The stack held when the window was closed
         */
        public WindowClosed(int windowID, ItemStack heldStack)
        {
//            super(guiContainer);
            this.windowID = windowID;
            this.heldStack = heldStack;
        }
        @Override
        public String toString()
        {
            String message = "";
            message += "Window ID: " + Integer.toString(windowID) + "\t";
            message += "Held Stack: " + heldStack.getDisplayName() + "\t";
            message += " x" + heldStack.getCount() + "\t";
            return message;
        }
    }
    //public RecipeClick
    public class InterestingButtonEvent {

    }
}