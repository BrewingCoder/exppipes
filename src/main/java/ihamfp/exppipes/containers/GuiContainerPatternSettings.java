package ihamfp.exppipes.containers;

import java.io.IOException;
import java.util.List;

import ihamfp.exppipes.ExppipesMod;
import ihamfp.exppipes.common.network.PacketCraftingPatternData;
import ihamfp.exppipes.common.network.PacketHandler;
import ihamfp.exppipes.items.ItemCraftingPattern;
import ihamfp.exppipes.tileentities.pipeconfig.FilterConfig;
import ihamfp.exppipes.tileentities.pipeconfig.Filters;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.inventory.Container;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextFormatting;

public class GuiContainerPatternSettings extends GuiContainerDecorated {
	public static final int WIDTH = 176;
	public static final int HEIGHT = 166;
	
	public static final ResourceLocation background = new ResourceLocation(ExppipesMod.MODID, "textures/gui/craftingpipe.png");

	int patternSlot;
	ItemStack pattern;
	List<ItemStack> results;
	List<FilterConfig> ingredients;
	boolean hasChanged = false;
	
	public GuiContainerPatternSettings(Container inventorySlotsIn, int patternSlot) {
		super(inventorySlotsIn);
		this.patternSlot = patternSlot;
		this.pattern = inventorySlotsIn.getSlot(patternSlot).getStack();
		this.results = ItemCraftingPattern.getPatternResults(this.pattern);
		for (int i=results.size(); i<9; i++) {
			results.add(ItemStack.EMPTY);
		}
		this.ingredients = ItemCraftingPattern.getPatternIngredients(this.pattern);
		for (int i=ingredients.size(); i<9; i++) {
			ingredients.add(null);
		}
	}
	
	@Override
	protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
		mc.getTextureManager().bindTexture(background);
		drawTexturedModalRect(guiLeft, guiTop, 0, 0, xSize, ySize);
		this.fontRenderer.drawString(this.pattern.getDisplayName(), guiLeft+8, guiTop+6, 0x7f7f7f);
	}
	
	@Override
	protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
		super.drawGuiContainerForegroundLayer(mouseX, mouseY);
		
		RenderHelper.disableStandardItemLighting();
		RenderHelper.enableGUIStandardItemLighting();
		
		List<String> hoverText = null;
		
		// draw ingredients
		for (int i=0; i<Integer.min(9, ingredients.size()); i++) {
			int ix = 8+(i%3)*18;
			int iy = 17+(i/3)*18;
			if (mouseX-guiLeft >= ix && mouseX-guiLeft <= ix+16 && mouseY-guiTop >= iy && mouseY-guiTop <= iy+16) {
				drawRect(ix, iy, ix + 16, iy + 16, -2130706433);
			}
			
			if (ingredients.get(i) == null) continue;
			ItemStack stackToDraw = ingredients.get(i).reference;
			this.itemRender.renderItemIntoGUI(stackToDraw, ix, iy);
			this.itemRender.renderItemOverlayIntoGUI(fontRenderer, stackToDraw, ix, iy, (stackToDraw.getCount()>1)?Integer.toString(stackToDraw.getCount()):"");
			if (mouseX-guiLeft >= ix && mouseX-guiLeft <= ix+16 && mouseY-guiTop >= iy && mouseY-guiTop <= iy+16) {
				hoverText = this.getItemToolTip(stackToDraw);//new ArrayList<String>();
				//hoverText.add(stackToDraw.getDisplayName());
				hoverText.add(TextFormatting.DARK_GRAY + "Filter: " + Filters.filters.get(ingredients.get(i).filterId).getLongName() + (ingredients.get(i).blacklist?" blacklist":"") + " (Shift-click to cycle)");
				//this.drawHoveringText(hoverText, mouseX-this.guiLeft, mouseY-this.guiTop);
			}
		}

		// draw results
		for (int i=0; i<Integer.min(9, results.size()); i++) {
			int ix = 116+(i%3)*18;
			int iy = 17+(i/3)*18;
			ItemStack stackToDraw = results.get(i);
			this.itemRender.renderItemAndEffectIntoGUI(stackToDraw, ix, iy);
			this.itemRender.renderItemOverlayIntoGUI(fontRenderer, stackToDraw, ix, iy, (stackToDraw.getCount()>1)?Integer.toString(stackToDraw.getCount()):"");
			if (mouseX-guiLeft >= ix && mouseX-guiLeft <= ix+16 && mouseY-guiTop >= iy && mouseY-guiTop <= iy+16) {
				drawRect(ix, iy, ix + 16, iy + 16, -2130706433);
				if (!stackToDraw.isEmpty()) {
					hoverText = this.getItemToolTip(stackToDraw);
					//this.renderToolTip(stackToDraw, mouseX-guiLeft, mouseY-guiTop);
				}
			}
		}
		RenderHelper.enableStandardItemLighting();
		
		if (hoverText != null) {
			this.drawHoveringText(hoverText, mouseX-this.guiLeft, mouseY-this.guiTop);
		}
	}

	@Override
	protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
		mouseX -= this.guiLeft;
		mouseY -= this.guiTop;
		if (mouseY>16 && mouseY <72) {
			ItemStack heldStack = this.mc.player.inventory.getItemStack();
			int iy = (mouseY-17)/18;
			if (mouseX > 7 && mouseX < 63) { // ingredients
				int ix = (mouseX-8)/18;
				int i = (ix+3*iy);
				if (isShiftKeyDown()) {
					ingredients.get(i).filterId = (ingredients.get(i).filterId+1)%Filters.filters.size();
				} else if (heldStack.isEmpty()) {
					ingredients.set(i, null);
				} else {
					ingredients.set(i, new FilterConfig(heldStack.copy(), 0, false));
				}
				this.hasChanged = true;
			} else if (mouseX > 115 && mouseX < 170) { // results
				int ix = (mouseX-116)/18;
				int i = (ix+3*iy);
				if (ItemStack.areItemsEqual(heldStack, results.get(i))) {
					results.get(i).grow(mouseButton==1?1:heldStack.getCount());
				} else if (heldStack.isEmpty() && !results.get(i).isEmpty() && mouseButton == 1) { // right click with empty hand
					results.get(i).shrink(1);
				} else {
					ItemStack putstack = heldStack.copy();
					if (mouseButton == 1) putstack.setCount(1); // right click, only put 1
					results.set(i, putstack);
				}
				this.hasChanged = true;
			}
		}
		mouseX += this.guiLeft;
		mouseY += this.guiTop;
		super.mouseClicked(mouseX, mouseY, mouseButton);
	}

	@Override
	public void onGuiClosed() {
		if (this.hasChanged) {
			// send pattern data to the server
			PacketHandler.INSTANCE.sendToServer(new PacketCraftingPatternData(this.results, this.ingredients));
		}
		
		super.onGuiClosed();
	}
}
