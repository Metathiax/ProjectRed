package mrtjp.projectred.transportation;

import codechicken.core.IGuiPacketSender;
import codechicken.core.ServerUtils;
import codechicken.lib.packet.PacketCustom;
import mrtjp.projectred.core.BasicGuiUtils;
import mrtjp.projectred.core.inventory.GhostContainer2;
import mrtjp.projectred.core.inventory.GhostContainer2.ISlotController;
import mrtjp.projectred.core.inventory.GhostContainer2.SlotExtended;
import mrtjp.projectred.core.inventory.SimpleInventory;
import mrtjp.projectred.core.utils.ItemKey;
import mrtjp.projectred.core.utils.ItemKeyStack;
import mrtjp.projectred.core.utils.Pair2;
import mrtjp.projectred.transportation.ItemRoutingChip.EnumRoutingChip;
import mrtjp.projectred.transportation.RequestBranchNode.DeliveryPromise;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.Container;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.MovingObjectPosition;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class RoutedInterfacePipePart extends RoutedJunctionPipePart implements IWorldBroadcaster
{
    public SimpleInventory chipSlots = new SimpleInventory(4, "chips", 1) {
        @Override
        public void onInventoryChanged()
        {
            super.onInventoryChanged();
            refreshChips();
        }

        @Override
        public boolean isItemValidForSlot(int i, ItemStack stack)
        {
            return stack != null
                    && stack.getItem() instanceof ItemRoutingChip
                    && stack.hasTagCompound()
                    && stack.getTagCompound().hasKey("chipROM")
                    && EnumRoutingChip.getForStack(stack).isInterfaceChip();
        }
    };

    public RoutingChipset[] chips = new RoutingChipset[4];

    @Override
    public String getType()
    {
        return "pr_rinterface";
    }

    @Override
    public void save(NBTTagCompound tag)
    {
        super.save(tag);
        chipSlots.save(tag);
    }

    @Override
    public void load(NBTTagCompound tag)
    {
        super.load(tag);
        chipSlots.load(tag);
    }

    @Override
    public void onRemoved()
    {
        super.onRemoved();

        if (!world().isRemote)
        {
            for (RoutingChipset r : chips)
                if (r != null)
                    r.onPipeBroken();

            List<ItemStack> list = new ArrayList<ItemStack>();
            for (int i = 0; i < chipSlots.getSizeInventory(); i++)
            {
                ItemStack stack = chipSlots.getStackInSlot(i);
                if (stack != null)
                    list.add(stack);
            }
            tile().dropItems(list);
        }
    }

    @Override
    public void updateServer()
    {
        super.updateServer();
        for (RoutingChipset s : chips)
            if (s != null)
                s.update();
    }

    @Override
    public boolean activate(EntityPlayer player, MovingObjectPosition hit, ItemStack item)
    {
        if (super.activate(player, hit, item))
            return true;

        if (item != null && item.getItem() instanceof ItemRoutingChip)
        {
            for (int i = 0; i < chipSlots.getSizeInventory(); i++)
                if (chipSlots.getStackInSlot(i) == null && chipSlots.isItemValidForSlot(i, item))
                {
                    ItemStack chip = item.splitStack(1);
                    chipSlots.setInventorySlotContents(i, chip);
                    chipSlots.onInventoryChanged();
                    return true;
                }
        }

        openGui(player);
        return true;
    }

    public void openGui(EntityPlayer player)
    {
        if (world().isRemote)
            return;

        ServerUtils.openSMPContainer((EntityPlayerMP) player, createContainer(player), new IGuiPacketSender() {
            @Override
            public void sendPacket(EntityPlayerMP player, int windowId)
            {
                PacketCustom p = new PacketCustom(TransportationSPH.channel(), NetConstants.gui_InterfacePipe_open);
                p.writeCoord(x(), y(), z());
                p.writeByte(windowId);
                p.sendToPlayer(player);
            }
        });
    }

    public Container createContainer(EntityPlayer player)
    {
        GhostContainer2 ghost = new GhostContainer2(player.inventory);
        ISlotController sc = ISlotController.InventoryRulesController.instance;

        int slot = 0;
        for (Pair2<Integer, Integer> p : BasicGuiUtils.createSlotArray(24, 12, 1, 4, 0, 8))
            ghost.addCustomSlot(new SlotExtended(chipSlots, slot++, p.getValue1(), p.getValue2()).setCheck(sc));

        ghost.addPlayerInventory(8, 118);
        return ghost;
    }

    public void refreshChips()
    {
        for (int i = 0; i < chipSlots.getSizeInventory(); i++)
        {
            ItemStack stack = chipSlots.getStackInSlot(i);
            RoutingChipset c = ItemRoutingChip.loadChipFromItemStack(stack);
            if (c != null)
                c.setEnvironment(this, this, i);
            if (chips[i] != c)
                chips[i] = c;
        }
    }

    @Override
    public SyncResponse getSyncResponse(ItemKey item, SyncResponse rival)
    {
        SyncResponse best = rival;
        boolean found = false;

        for (RoutingChipset r : chips)
            if (r != null)
            {
                SyncResponse response = r.getSyncResponse(item, best);
                if (response != null)
                    if (response.priority.ordinal() > best.priority.ordinal() || response.priority.ordinal() == best.priority.ordinal() && response.customPriority > best.customPriority)
                    {
                        best = response;
                        found = true;
                    }
            }

        if (found)
        {
            best.itemCount -= countInTransit(item);
            return best;
        }

        return null;
    }

    @Override
    public void requestPromises(RequestBranchNode request, int existingPromises)
    {
        for (RoutingChipset r : chips)
            if (r != null)
                r.requestPromises(request, existingPromises);
    }

    @Override
    public void deliverPromises(DeliveryPromise promise, IWorldRequester requestor)
    {
        for (RoutingChipset r : chips)
            if (r != null)
                r.deliverPromises(promise, requestor);
    }

    @Override
    public void trackedItemLost(ItemKeyStack s)
    {
        for (RoutingChipset r : chips)
            if (r != null)
                r.trackedItemLost(s);

    }

    @Override
    public void trackedItemReceived(ItemKeyStack s)
    {
        for (RoutingChipset r : chips)
            if (r != null)
                r.trackedItemReceived(s);
    }

    @Override
    public void getBroadcastedItems(Map<ItemKey, Integer> map)
    {
        for (RoutingChipset r : chips)
            if (r != null)
                r.getProvidedItems(map);
    }

    @Override
    public int getPriority()
    {
        int high = Integer.MIN_VALUE;
        for (RoutingChipset r : chips)
            if (r != null)
            {
                int priority = r.getPriority();
                if (priority > high)
                    high = priority;
            }
        return high;
    }

    @Override
    public double getWorkLoad()
    {
        double high = 0;
        for (RoutingChipset r : chips)
            if (r != null)
            {
                double load = r.getWorkLoad();
                if (load > high)
                    high = load;
            }

        return high;
    }

    @Override
    public void onNeighborTileChanged(int side, boolean weak)
    {
        for (RoutingChipset r : chips)
            if (r != null)
                r.onNeighborTileChanged(side, weak);
    }

    @Override
    public boolean weakTileChanges()
    {
        for (RoutingChipset r : chips)
            if (r != null)
                if (r.weakTileChanges())
                    return true;
        return false;
    }
}
