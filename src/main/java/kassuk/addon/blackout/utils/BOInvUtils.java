package kassuk.addon.blackout.utils;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import kassuk.addon.blackout.managers.Managers;
import kassuk.addon.blackout.mixins.ComponentHasherNetworkHandlerAccessor;
import meteordevelopment.meteorclient.mixininterface.IClientPlayerInteractionManager;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.ClickSlotC2SPacket;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.screen.sync.ComponentChangesHash;
import net.minecraft.screen.sync.ItemStackHash;

import static meteordevelopment.meteorclient.MeteorClient.mc;

/**
 * @author OLEPOSSU
 */

@SuppressWarnings("DataFlowIssue")
public class BOInvUtils {
    private static int[] slots;
    public static int pickSlot = -1;

    // Updated by H1ggsK
    public static boolean pickSwitch(int slot) {
        if (slot >= 0) {
            Managers.HOLDING.modifyStartTime = System.currentTimeMillis();
            pickSlot = slot;

            // Obtain the container ID and revision from the player's current screen handler.
            int syncId = mc.player.currentScreenHandler.syncId;
            // Some implementations offer a revision getter; if not, you might default to 0.
            int revision = mc.player.currentScreenHandler.getRevision();

            // Define the click parameters:
            // button: 0 for primary click (adjust if you need a different click type)
            int button = 0;
            // actionType: use SlotActionType.PICKUP for a normal click (or change as needed)
            SlotActionType actionType = SlotActionType.PICKUP;

            ItemStack stack = mc.player.currentScreenHandler.getSlot(slot).getStack();

            Int2ObjectMap<ItemStackHash> modifiedStacks = new Int2ObjectOpenHashMap<>();

            ComponentChangesHash.ComponentHasher componentHasher = ((ComponentHasherNetworkHandlerAccessor) mc.getNetworkHandler()).getComponentHasher();
            ItemStackHash stackHash = ItemStackHash.fromItemStack(stack, componentHasher);

            ClickSlotC2SPacket packet = new ClickSlotC2SPacket(
                syncId,
                revision,
                (short) slot,
                (byte) button,
                actionType,
                modifiedStacks,
                stackHash
            );

            mc.getNetworkHandler().sendPacket(packet);



            return true;
        }
        return false;
    }

    // Updated by H1ggsK
    public static void pickSwapBack() {
        if (pickSlot >= 0) {
            // Obtain the container (screen handler) ID and revision.
            int syncId = mc.player.currentScreenHandler.syncId;
            // Use the current revision, or default to 0 if not available.
            int revision = mc.player.currentScreenHandler.getRevision();

            // Set click parameters.
            int button = 0; // Typically 0 for left-click; adjust as needed.
            SlotActionType actionType = SlotActionType.PICKUP;

            // Retrieve the item stack from the stored pickSlot.
            ItemStack stack = mc.player.currentScreenHandler.getSlot(pickSlot).getStack();

            // Create an empty map for modified stacks (populate if needed).
            Int2ObjectMap<ItemStackHash> modifiedStacks = new Int2ObjectOpenHashMap<>();

            ComponentChangesHash.ComponentHasher componentHasher = ((ComponentHasherNetworkHandlerAccessor) mc.getNetworkHandler()).getComponentHasher();
            ItemStackHash stackHash = ItemStackHash.fromItemStack(stack, componentHasher);

            ClickSlotC2SPacket packet = new ClickSlotC2SPacket(
                syncId,
                revision,
                (short) pickSlot,
                (byte) button,
                actionType,
                modifiedStacks,
                stackHash
            );mc.getNetworkHandler().sendPacket(packet);

            // Reset pickSlot after sending the packet.
            pickSlot = -1;
        }
    }

    // Credits to rickyracuun
    public static boolean invSwitch(int slot) {
        if (slot >= 0) {
            ScreenHandler handler = mc.player.currentScreenHandler;
            ItemStack stack = handler.getSlot(slot).getStack();
            ComponentChangesHash.ComponentHasher componentHasher = ((ComponentHasherNetworkHandlerAccessor) mc.getNetworkHandler()).getComponentHasher();
            ItemStackHash stackHash = ItemStackHash.fromItemStack(stack, componentHasher);
            Int2ObjectMap<ItemStackHash> modifiedStacks = new Int2ObjectOpenHashMap<>();

            mc.getNetworkHandler().sendPacket(new ClickSlotC2SPacket(handler.syncId,
                handler.getRevision(), (short) (PlayerInventory.MAIN_SIZE + Managers.HOLDING.slot),
                (byte) slot, SlotActionType.SWAP, modifiedStacks, stackHash)
            );
            ((IClientPlayerInteractionManager) mc.interactionManager).meteor$syncSelected();
            slots = new int[]{slot, Managers.HOLDING.slot};
            return true;
        }
        return false;
    }

    public static void swapBack() {
        ScreenHandler handler = mc.player.currentScreenHandler;
        int slot = slots[0];
        ItemStack stack = handler.getSlot(slot).getStack();
        ComponentChangesHash.ComponentHasher componentHasher = ((ComponentHasherNetworkHandlerAccessor) mc.getNetworkHandler()).getComponentHasher();
        ItemStackHash stackHash = ItemStackHash.fromItemStack(stack, componentHasher);
        Int2ObjectMap<ItemStackHash> modifiedStacks = new Int2ObjectOpenHashMap<>();

        mc.getNetworkHandler().sendPacket(new ClickSlotC2SPacket(handler.syncId,
            handler.getRevision(), (short) (PlayerInventory.MAIN_SIZE + slots[1]),
            (byte) slots[0], SlotActionType.SWAP, modifiedStacks, stackHash)
        );
        ((IClientPlayerInteractionManager) mc.interactionManager).meteor$syncSelected();
    }
}
