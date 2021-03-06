package ihamfp.exppipes.common.network;

import ihamfp.exppipes.tileentities.TileEntityRoutingPipe;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class PacketSetDefaultRoute implements IMessage {
	BlockPos pos;
	
	public PacketSetDefaultRoute() {}
	
	public PacketSetDefaultRoute(BlockPos pos) {
		this.pos = pos;
	}

	@Override
	public void fromBytes(ByteBuf buf) {
		this.pos = new BlockPos(buf.readInt(), buf.readInt(), buf.readInt());
	}

	@Override
	public void toBytes(ByteBuf buf) {
		buf.writeInt(this.pos.getX());
		buf.writeInt(this.pos.getY());
		buf.writeInt(this.pos.getZ());
	}
	
	public static class Handler implements IMessageHandler<PacketSetDefaultRoute,IMessage> {

		@Override
		public IMessage onMessage(PacketSetDefaultRoute message, MessageContext ctx) {
			EntityPlayerMP serverPlayer = ctx.getServerHandler().player;
			if (message.pos == null) return null;
			serverPlayer.getServerWorld().addScheduledTask(() -> {
				if (!serverPlayer.getServerWorld().isBlockLoaded(message.pos)) return;
				TileEntity te = serverPlayer.getServerWorld().getTileEntity(message.pos);
				if (te == null || !(te instanceof TileEntityRoutingPipe)) return; // I'll be kind and send back even if the TE isn't right
				TileEntityRoutingPipe terp = (TileEntityRoutingPipe)te; // just a cast, really
				if (terp.network == null) return;
				
				terp.isDefaultRoute = true;
				terp.network.defaultRoute = terp;
			});
			return null;
		}
	}
}
