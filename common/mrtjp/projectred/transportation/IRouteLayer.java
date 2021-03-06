package mrtjp.projectred.transportation;

import java.util.BitSet;

import mrtjp.projectred.core.utils.ItemKey;
import mrtjp.projectred.transportation.RoutedPayload.SendPriority;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import codechicken.lib.vec.BlockCoord;

public interface IRouteLayer 
{
    public void queueStackToSend(ItemStack stack, int dirOfExtraction, SyncResponse path);
    public void queueStackToSend(ItemStack stack, int dirOfExtraction, SendPriority priority, int destination);
    public SyncResponse getLogisticPath(ItemKey stack, BitSet exclusions, boolean excludeStart);

    public Router getRouter();
    public IWorldRouter getWorldRouter();
    public IWorldBroadcaster getBroadcaster();
    public IWorldRequester getRequester();
    
    public World getWorld();
    public BlockCoord getCoords();
}