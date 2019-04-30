package ihamfp.exppipes.client.render;

import ihamfp.exppipes.common.Configs;
import ihamfp.exppipes.pipenetwork.ItemDirection;
import ihamfp.exppipes.tileentities.TileEntityPipe;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderItem;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;

public class TESRPipe extends TileEntitySpecialRenderer<TileEntityPipe> {

	@Override
	public void render(TileEntityPipe te, double x, double y, double z, float partialTicks, int destroyStage, float alpha) {
		if (!te.itemHandler.storedItems.isEmpty()) {
			GlStateManager.pushAttrib();
			GlStateManager.pushMatrix();
			GlStateManager.translate(x, y, z);
			GlStateManager.disableRescaleNormal();
			
			RenderItem renderItem = Minecraft.getMinecraft().getRenderItem();
			
			//RenderHelper.disableStandardItemLighting();
			GlStateManager.enableLighting();
			GlStateManager.pushMatrix();
			
			GlStateManager.translate(0.5, 0.5, 0.5);
			GlStateManager.scale(5/16D, 5/16D, 5/16D); // slightly smaller that 6/16
			
			for (ItemDirection stackDir : te.itemHandler.storedItems) {
				float fTimer = (float)(te.getWorld().getTotalWorldTime() - stackDir.insertTime) + partialTicks;
				
				float partial = (fTimer / (float)Configs.travelTime)-0.5f;
				if (partial > 1.0f || partial < -1.0f) partial = 0.0f;
				partial *= 16/5D;
				float mvx = 0.0f;
				float mvy = 0.0f;
				float mvz = 0.0f;
				if (partial < 0.0f || stackDir.to == null) {
					partial = -partial;
					mvx = partial*stackDir.from.getXOffset();
					mvy = partial*stackDir.from.getYOffset();
					mvz = partial*stackDir.from.getZOffset();
				} else {
					mvx = partial*stackDir.to.getXOffset();
					mvy = partial*stackDir.to.getYOffset();
					mvz = partial*stackDir.to.getZOffset();
				}
				GlStateManager.translate(mvx, mvy, mvz);
				renderItem.renderItem(stackDir.itemStack, ItemCameraTransforms.TransformType.NONE);
				GlStateManager.translate(-mvx, -mvy, -mvz);
			}
			
			GlStateManager.popMatrix();
			GlStateManager.popMatrix();
			GlStateManager.popAttrib();
		}
		
		super.render(te, x, y, z, partialTicks, destroyStage, alpha);
	}
	
	/*@Override
	public void renderTileEntityFast(TileEntityPipe te, double x, double y, double z, float partialTicks, int destroyStage, float partial, BufferBuilder buffer) {
		if (te.itemHandler.storedItems.isEmpty()) return;
		RenderItem renderItem = Minecraft.getMinecraft().getRenderItem();
		for (ItemDirection stackDir : te.itemHandler.storedItems) {
			IBakedModel stackModel = renderItem.getItemModelMesher().getItemModel(stackDir.itemStack);
			
			
		}
	}*/

}
