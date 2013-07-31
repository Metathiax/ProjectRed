package mrtjp.projectred.multipart.wiring.wires;

import mrtjp.projectred.interfaces.wiring.IBundledEmitter;
import mrtjp.projectred.interfaces.wiring.IBundledUpdatable;
import mrtjp.projectred.interfaces.wiring.IInsulatedRedstoneWire;
import mrtjp.projectred.transmission.TileWire;
import mrtjp.projectred.utils.BasicWireUtils;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Icon;
import net.minecraft.world.World;

public class TileInsulatedRedAlloy extends TileRedAlloy implements IBundledUpdatable, IInsulatedRedstoneWire {
	{
		syncSignalStrength = true;
	}
	
	@Override
	protected boolean canConnectToWire(TileWire wire) {
		if (wire.getWireType() == EnumWire.RED_ALLOY || wire instanceof TileBundled) {
			return true;
		}
		return wire.getWireType() == getWireType();
	}

	@Override
	public boolean canProvideWeakPowerInDirection(int dir) {
		return connectsInDirection(dir) && super.canProvideWeakPowerInDirection(dir);
	}

	@Override
	public boolean canProvideStrongPowerInDirection(int dir) {
		return false;
	}

	@Override
	protected int getInputPowerStrength(World worldObj, int x, int y, int z, int dir, int side, boolean countWires) {
		int rv = BasicWireUtils.getPowerStrength(worldObj, x, y, z, dir, side, countWires);

		if (rv > 0) {
			return rv;
		}

		TileEntity te = worldObj.getBlockTileEntity(x, y, z);
		if (te instanceof IBundledEmitter) {
			int colour = getInsulatedWireColour();
			byte[] bcStrengthArray = ((IBundledEmitter) te).getBundledCableStrength(side, dir);
			if (bcStrengthArray != null) {
				int bcStrength = bcStrengthArray[colour] & 0xFF;
				rv = Math.max(rv, bcStrength);
			}
		}

		return rv;
	}

	@Override
	public void onBundledInputChanged() {
		updateSignal(null);
	}

	@Override
	public int getInsulatedWireColour() {
		if (getWireType() == null) {
			return 0;
		}
		return getWireType().ordinal() - EnumWire.INSULATED_0.ordinal();
	}
	
	@Override
	public Icon getSpecialIconForRender() {
		if (getRedstoneSignalStrength() > 0) {
			return this.getWireType().wireSprites[1];
		} else {
			return this.getWireType().wireSprites[0];
		}
	}
}
