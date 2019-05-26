package ihamfp.exppipes.tileentities;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ihamfp.exppipes.common.Configs;
import ihamfp.exppipes.pipenetwork.BlockDimPos;
import ihamfp.exppipes.pipenetwork.ItemDirection;
import ihamfp.exppipes.pipenetwork.Request;
import ihamfp.exppipes.tileentities.pipeconfig.ConfigRoutingPipe;
import ihamfp.exppipes.tileentities.pipeconfig.FilterConfig;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.util.Constants.NBT;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;

public class TileEntityStockKeeperPipe extends TileEntityRoutingPipe {
	public ConfigRoutingPipe stockConfig = new ConfigRoutingPipe();
	public Map<Integer,Request> requests = new HashMap<Integer,Request>();
	public Map<Integer,Long> coolDown = new HashMap<Integer,Long>(); // prevents over-ordering
	
	@Override
	public void serverUpdate() {
		// remove all completed requests
		this.itemHandler.tick(this.world.getTotalWorldTime());
		List<Integer> rRemove = new ArrayList<Integer>();
		for (ItemDirection itemDir : itemHandler.storedItems) {
			for (Integer i : this.requests.keySet()) {
				Request r = this.requests.get(i);
				if (rRemove.contains(i)) break;
				if (itemDir.destinationPos != null && itemDir.destinationPos.isHere(this) && (this.world.getTotalWorldTime()-itemDir.insertTime)>=Configs.travelTime && r.filter.doesMatch(itemDir.itemStack)) {
					r.processingCount.addAndGet(-itemDir.itemStack.getCount());
					if (r.processingCount.get() < 0) r.processingCount.set(0);
					r.processedCount += itemDir.itemStack.getCount();
					if (r.processedCount >= r.requestedCount) {
						rRemove.add(i);
					}
					break;
				}
				if (this.network != null && !this.network.requests.contains(r)) {
					this.network.requests.add(r);
				}
			}
		}
		if (this.network != null) {
			rRemove.forEach(e -> this.network.requests.remove(this.requests.get(e)));
		}
		rRemove.forEach(e -> this.coolDown.put(e, this.world.getTotalWorldTime()+Configs.travelTime));
		this.requests.keySet().removeAll(rRemove);
		
		// Search for the connected item handler
		IItemHandler ih = null;
		EnumFacing foundFace = null;
		for (EnumFacing f : EnumFacing.VALUES) {
			TileEntity te = this.world.getTileEntity(this.pos.offset(f));
			if (te != null && !(te instanceof TileEntityPipe) && te.hasCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, f.getOpposite())) {
				ih = te.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, f.getOpposite());
				foundFace = f;
				break;
			}
		}
		
		if (ih != null) {
			// check for things to request
			for (int i=0;i<Integer.min(ih.getSlots(), stockConfig.filters.size());i++) {
				FilterConfig fi = this.stockConfig.filters.get(i);
				if (!fi.doesMatch(ih.getStackInSlot(i))) { // item doesn't match - extract it
					ItemStack is = ih.extractItem(i, ih.getSlotLimit(i), false);
					this.itemHandler.insertedItems.add(new ItemDirection(is, foundFace, null, this.world.getTotalWorldTime()));
				} else if (fi.reference.getCount() < ih.getStackInSlot(i).getCount()) { // too much items
					ItemStack is = ih.extractItem(i, ih.getStackInSlot(i).getCount()-fi.reference.getCount(), false);
					this.itemHandler.insertedItems.add(new ItemDirection(is, foundFace, null, this.world.getTotalWorldTime()));
				}
				if (ih.getStackInSlot(i).isEmpty() || (fi.doesMatch(ih.getStackInSlot(i)) && ih.getStackInSlot(i).getCount() < fi.reference.getCount())) { // order more things
					if (this.network != null && !this.requests.containsKey(i) && this.coolDown.getOrDefault(i, 0L) < this.world.getTotalWorldTime()) {
						this.requests.put(i, this.network.request(new BlockDimPos(this), fi, fi.reference.getCount()-ih.getStackInSlot(i).getCount()));
					}
				}
			}
		}
		
		super.serverUpdate();
	}
	
	@Override
	public void readFromNBT(NBTTagCompound compound) {
		this.stockConfig.deserializeNBT(compound.getCompoundTag("stockConfig"));
		if (this.network != null) this.network.requests.removeAll(this.requests.values());
		this.requests.clear();
		NBTTagList requests = compound.getTagList("requests", NBT.TAG_COMPOUND);
		for (int i=0; i<requests.tagCount();i++) {
			NBTTagCompound extReq = (NBTTagCompound) requests.get(i);
			this.requests.put(extReq.getInteger("stockFilter"), new Request(extReq));
			if (extReq.hasKey("cooldown")) this.coolDown.put(extReq.getInteger("stockFilter"), extReq.getLong("cooldown"));
		}
		if (this.network != null) this.network.requests.addAll(this.requests.values());
		super.readFromNBT(compound);
	}
	
	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound compound) {
		compound.setTag("stockConfig", this.stockConfig.serializeNBT());
		NBTTagList requests = new NBTTagList();
		for (int i=0; i<this.stockConfig.filters.size(); i++) {
			if (!this.requests.containsKey(i)) continue;
			Request req = this.requests.get(i);
			NBTTagCompound extReq = req.serializeNBT();
			extReq.setInteger("stockFilter", i);
			if (this.coolDown.containsKey(i)) extReq.setLong("cooldown", this.coolDown.get(i));
			requests.appendTag(extReq);
		}
		compound.setTag("requests", requests);
		return super.writeToNBT(compound);
	}
	
	@Override
	public NBTTagCompound getUpdateTag() {
		NBTTagCompound nbtTag = super.getUpdateTag();
		if (!this.world.isRemote) { // sending from the server
			nbtTag.setTag("stockConfig", this.stockConfig.serializeNBT());
		}
		return nbtTag;
	}
	
	@Override
	public void onDataPacket(NetworkManager net, SPacketUpdateTileEntity pkt) {
		NBTTagCompound nbtTag = pkt.getNbtCompound();
		if (this.world.isRemote) { // receiving from the client
			this.stockConfig.deserializeNBT(nbtTag.getCompoundTag("stockConfig"));
		}
		super.onDataPacket(net, pkt);
	}
}
