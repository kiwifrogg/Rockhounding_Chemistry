package com.globbypotato.rockhounding_chemistry.machines.tileentity;

import javax.annotation.Nullable;

import com.globbypotato.rockhounding_chemistry.CommonProxy;
import com.globbypotato.rockhounding_chemistry.Utils;
import com.globbypotato.rockhounding_chemistry.handlers.EnumFluid;
import com.globbypotato.rockhounding_chemistry.handlers.ModArray;
import com.globbypotato.rockhounding_chemistry.handlers.ModRecipes;
import com.globbypotato.rockhounding_chemistry.items.ModItems;
import com.globbypotato.rockhounding_chemistry.machines.gui.GuiLabOven;
import com.globbypotato.rockhounding_chemistry.machines.recipe.LabOvenRecipe;
import com.globbypotato.rockhounding_chemistry.machines.tileentity.WrappedItemHandler.WriteMode;

import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.items.ItemStackHandler;

public class TileEntityLabOven extends TileEntityInvReceiver {

	public int redstoneCount;
	public int redstoneMax = 32000;

	public int recipeDisplayIndex;

	public static int cookingSpeed;
	public static int tankMax = 100;
	private int redstoneCharge = cookingSpeed;

	//Input handler slots
	public static final int SOLUTE_SLOT = 0;
	//				   FUEL_SLOT = 1;
	public static final int SOLVENT_SLOT = 2;
	public static final int OUTPUT_SLOT = 3; //the name is a bit confusing, but it's easier to handle it in input handler

	public static final int REDSTONE_SLOT = 4;

	private ItemStackHandler template = new TemplateStackHandler(1);
	private static int TEMPLATE_SLOT = 0;

	public boolean recipeScan;

	//----------------------- CUSTOM -----------------------

	public TileEntityLabOven() {
		super(7, 0, 1);
		this.recipeDisplayIndex = -1;


		input =  new MachineStackHandler(INPUT_SLOTS,this){
			@Override
			public ItemStack insertItem(int slot, ItemStack insertingStack, boolean simulate){
				if(slot == SOLUTE_SLOT && hasRecipe(insertingStack)){
					return super.insertItem(slot, insertingStack, simulate);
				}
				if(slot == FUEL_SLOT && Utils.isItemFuel(insertingStack)){
					return super.insertItem(slot, insertingStack, simulate);
				}
				if(slot == SOLVENT_SLOT && isSolvent(insertingStack)){
					return super.insertItem(slot, insertingStack, simulate);
				}
				if(slot == REDSTONE_SLOT &&
						(insertingStack.getItem() == Items.REDSTONE || (insertingStack.getItem() == ModItems.inductor))){
					return super.insertItem(slot, insertingStack, simulate);
				}
				if(slot == OUTPUT_SLOT && ItemStack.areItemsEqual(insertingStack, new ItemStack(ModItems.chemicalItems,1,0))){
					return super.insertItem(slot, insertingStack, simulate);
				}
				return insertingStack;
			}
		};

		automationInput = new WrappedItemHandler(input,WriteMode.IN_OUT);
	}

	public ItemStackHandler getTemplate(){
		return this.template;
	}

	public int getCookTime(@Nullable ItemStack stack){
		return this.machineSpeed();
	}

	public int machineSpeed(){
		return cookingSpeed;
	}

	public boolean hasRecipe(){
		return hasRecipe(input.getStackInSlot(SOLUTE_SLOT));
	}

	public static boolean hasRecipe(ItemStack stack){
		for(LabOvenRecipe recipe: ModRecipes.labOvenRecipes){
			if(ItemStack.areItemsEqual(recipe.getSolute(),stack)){
				return true;
			}
		}
		return false;
	}

	public EnumFluid getFluidOutput(){
		for(LabOvenRecipe recipe: ModRecipes.labOvenRecipes){
			if(ItemStack.areItemsEqual(recipe.getSolute(),input.getStackInSlot(SOLUTE_SLOT))){
				return recipe.getOutput();
			}
		}
		return null;
	}

	public static boolean isSolvent(ItemStack stack){
		if(stack.hasTagCompound()){
			for(LabOvenRecipe recipe: ModRecipes.labOvenRecipes){
				if(recipe.getSolvent().getName().equals(stack.getTagCompound().getString("Fluid"))){
					return true;
				}
			}
		}
		return false;
	}

	public int getFieldCount() {
		return 6;
	}

	public int getField(int id){
		switch (id){
		case 0: return this.powerCount;
		case 1: return this.powerMax;
		case 2: return this.cookTime;
		case 3: return this.totalCookTime;
		case 4: return this.redstoneCount;
		case 5: return this.redstoneMax;
		default:return 0;
		}
	}

	public void setField(int id, int value){
		switch (id){
		case 0: this.powerCount = value; break;
		case 1: this.powerMax = value; break;
		case 2: this.cookTime = value; break;
		case 3: this.totalCookTime = value; break;
		case 4: this.redstoneCount = value; break;
		case 5: this.redstoneMax = value;
		}
	}



	//----------------------- I/O -----------------------
	@Override
	public void readFromNBT(NBTTagCompound compound){
		super.readFromNBT(compound);
		this.recipeDisplayIndex = compound.getInteger("RecipeCount");
		this.redstoneCount = compound.getInteger("RedstoneCount");
		this.cookTime = compound.getInteger("CookTime");
		this.totalCookTime = compound.getInteger("CookTimeTotal");
	}

	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound compound){
		super.writeToNBT(compound);
		compound.setInteger("RecipeCount", this.recipeDisplayIndex);
		compound.setInteger("RedstoneCount", this.redstoneCount);
		compound.setInteger("CookTime", this.cookTime);
		compound.setInteger("CookTimeTotal", this.totalCookTime);
		return compound;
	}




	//----------------------- PROCESS -----------------------

	@Override
	public void update(){
		if(input.getStackInSlot(FUEL_SLOT) != null){fuelHandler();}
		if(input.getStackInSlot(REDSTONE_SLOT) != null){redstoneHandler();}
		if(!worldObj.isRemote){
			if(currentRecipeIndex() >= 0 && recipeScan){ showIngredients(currentRecipeIndex());}
			if(canSynthesize()){
				execute();
			}
		}
	}

	private void showIngredients(int countRecipes) {
		template.setStackInSlot(TEMPLATE_SLOT, ModRecipes.labOvenRecipes.get(currentRecipeIndex()).getSolute());
		recipeScan = false;
	}

	public void resetGrid(){
		template.setStackInSlot(TEMPLATE_SLOT, null);
	}

	private void execute() {
		cookTime++;
		powerCount--;
		redstoneCount --; 
		if(cookTime >= machineSpeed()) {
			cookTime = 0; 
			handleOutput(getFluidOutput());
		}
		this.markDirty();
	}

	private boolean canSynthesize() {
		if(this.recipeDisplayIndex >= 0){
			EnumFluid fluid = ModRecipes.labOvenRecipes.get(recipeDisplayIndex).getOutput();
			return     hasRecipe()
					&& (isTankEmpty(OUTPUT_SLOT) || stackHasFluid(input.getStackInSlot(OUTPUT_SLOT),fluid))
					&& (!isTankFull(OUTPUT_SLOT))
					&& powerCount >= machineSpeed()
					&& redstoneCount >= machineSpeed() * 2;
		}
		else return false;
	}

	private boolean isTankEmpty(int slot) {
		if(hasTank(slot)){
			if(input.getStackInSlot(slot).hasTagCompound()){
				return input.getStackInSlot(slot).getTagCompound().getString("Fluid").equals(EnumFluid.EMPTY.getName());
			}
		}
		return false;
	}

	private boolean stackHasFluid(ItemStack stack, EnumFluid fluid){
		if(stack != null){
			if(stack.hasTagCompound()){
				if(stack.getTagCompound().getString("Fluid").equals(fluid.getName())){
					return true;
				}
			}
		}
		return false;
	}

	private boolean isTankFull(int slot) {
		if(hasTank(slot)){
			if(input.getStackInSlot(slot).hasTagCompound()){
				return input.getStackInSlot(slot).getTagCompound().getInteger("Quantity") == tankMax;
			}
		}
		return false;
	}

	private boolean hasTank(int outputSlot) {
		return  input.getStackInSlot(OUTPUT_SLOT) != null
				&& input.getStackInSlot(OUTPUT_SLOT).getItem() == ModItems.chemicalItems
				&& input.getStackInSlot(OUTPUT_SLOT).getItemDamage() == 0 
				&& input.getStackInSlot(OUTPUT_SLOT).stackSize == 1;
	}

	private void handleOutput(EnumFluid fluidOutput) {
		int quantity = 0;
		input.decrementSlot(SOLUTE_SLOT);
		input.decrementFluid(SOLVENT_SLOT);
		//add output
		quantity = input.getStackInSlot(OUTPUT_SLOT).getTagCompound().getInteger(ModArray.chemTankQuantity) + 1;
		input.getStackInSlot(OUTPUT_SLOT).getTagCompound().setString(ModArray.chemTankName, fluidOutput.getName());
		input.getStackInSlot(OUTPUT_SLOT).getTagCompound().setInteger(ModArray.chemTankQuantity, quantity);
	}


	private void redstoneHandler() {
		if(input.getStackInSlot(REDSTONE_SLOT)!= null && input.getStackInSlot(REDSTONE_SLOT).getItem() == Items.REDSTONE && redstoneCount <= (redstoneMax - redstoneCharge)){
			redstoneCount += redstoneCharge;
			input.decrementSlot(REDSTONE_SLOT);
		}
	}

	@Override
	protected boolean canInduct() {
		return redstoneCount >= redstoneMax 
				&& input.getStackInSlot(REDSTONE_SLOT) != null 
				&& input.getStackInSlot(REDSTONE_SLOT).getItem() == ModItems.inductor;
	}

	@Override
	public int getGUIHeight() {
		return GuiLabOven.HEIGHT;
	}

	public int currentRecipeIndex(){
		return recipeDisplayIndex;
	}
}