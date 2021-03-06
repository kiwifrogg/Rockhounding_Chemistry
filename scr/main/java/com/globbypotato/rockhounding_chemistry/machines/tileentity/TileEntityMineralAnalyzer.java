package com.globbypotato.rockhounding_chemistry.machines.tileentity;

import java.util.ArrayList;

import com.globbypotato.rockhounding_chemistry.enums.EnumFluid;
import com.globbypotato.rockhounding_chemistry.handlers.ModConfig;
import com.globbypotato.rockhounding_chemistry.machines.gui.GuiMineralAnalyzer;
import com.globbypotato.rockhounding_chemistry.machines.recipe.MachineRecipes;
import com.globbypotato.rockhounding_chemistry.machines.recipe.MineralAnalyzerRecipe;
import com.globbypotato.rockhounding_chemistry.utils.ToolUtils;
import com.globbypotato.rockhounding_core.machines.tileentity.MachineStackHandler;
import com.globbypotato.rockhounding_core.machines.tileentity.TemplateStackHandler;
import com.globbypotato.rockhounding_core.machines.tileentity.TileEntityMachineTank;
import com.globbypotato.rockhounding_core.machines.tileentity.WrappedItemHandler;
import com.globbypotato.rockhounding_core.machines.tileentity.WrappedItemHandler.WriteMode;
import com.globbypotato.rockhounding_core.utils.CoreUtils;
import com.globbypotato.rockhounding_core.utils.ProbabilityStack;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTank;
import net.minecraftforge.fluids.capability.templates.FluidHandlerConcatenate;
import net.minecraftforge.items.ItemStackHandler;

public class TileEntityMineralAnalyzer extends TileEntityMachineTank{

	private ItemStackHandler template = new TemplateStackHandler(4);
	private static final int SULFUR_SLOT = 3;
	private static final int CHLOR_SLOT = 4;
	private static final int FLUO_SLOT = 5;
	private static final int SPEED_SLOT = 6;

	public FluidTank sulfTank;
	public FluidTank chloTank;
	public FluidTank fluoTank;

	public boolean drainValve;
	public float gravity = 8.00F;
	public ArrayList<ItemStack> pickedShards = new ArrayList<ItemStack>();

	public TileEntityMineralAnalyzer() {
		super(7,1,1);

		sulfTank = new FluidTank(1000 + ModConfig.machineTank){
			@Override  
			public boolean canFillFluidType(FluidStack fluid){
		        return fluid.getFluid().equals(EnumFluid.pickFluid(EnumFluid.SULFURIC_ACID));
		    }

			@Override
		    public boolean canDrain(){
		        return canDrainAcids();
		    }
		};
		sulfTank.setTileEntity(this);

		chloTank = new FluidTank(1000 + ModConfig.machineTank){
			@Override  
			public boolean canFillFluidType(FluidStack fluid){
		        return fluid.getFluid().equals(EnumFluid.pickFluid(EnumFluid.HYDROCHLORIC_ACID));
		    }

			@Override
		    public boolean canDrain(){
		        return canDrainAcids();
		    }
		};
		chloTank.setTileEntity(this);

		fluoTank = new FluidTank(1000 + ModConfig.machineTank){
			@Override  
			public boolean canFillFluidType(FluidStack fluid){
		        return fluid.getFluid().equals(EnumFluid.pickFluid(EnumFluid.HYDROFLUORIC_ACID));
		    }

			@Override
		    public boolean canDrain(){
		        return canDrainAcids();
		    }
		};
		fluoTank.setTileEntity(this);

		input =  new MachineStackHandler(INPUT_SLOTS, this){
			@Override
			public ItemStack insertItem(int slot, ItemStack insertingStack, boolean simulate){
				if(slot == INPUT_SLOT && hasRecipe(insertingStack)){
					return super.insertItem(slot, insertingStack, simulate);
				}
				if(slot == FUEL_SLOT && isGatedPowerSource(insertingStack)){
					return super.insertItem(slot, insertingStack, simulate);
				}
				if(slot == CONSUMABLE_SLOT && CoreUtils.hasConsumable(ToolUtils.agitator, insertingStack)){
					return super.insertItem(slot, insertingStack, simulate);
				}
				if(slot == SULFUR_SLOT && handleBucket(insertingStack, EnumFluid.pickFluid(EnumFluid.SULFURIC_ACID)) ) {
					return super.insertItem(slot, insertingStack, simulate);
				}
				if(slot == CHLOR_SLOT && handleBucket(insertingStack, EnumFluid.pickFluid(EnumFluid.HYDROCHLORIC_ACID)) ){
					return super.insertItem(slot, insertingStack, simulate);
				}
				if(slot == FLUO_SLOT && handleBucket(insertingStack, EnumFluid.pickFluid(EnumFluid.HYDROFLUORIC_ACID)) ){
					return super.insertItem(slot, insertingStack, simulate);
				}
				if(slot == SPEED_SLOT && ToolUtils.isValidSpeedUpgrade(insertingStack)){
					return super.insertItem(slot, insertingStack, simulate);
				}
				return insertingStack;
			}
		};
		this.automationInput = new WrappedItemHandler(input, WriteMode.IN);
	}



	//----------------------- SLOTS -----------------------
	public ItemStack speedSlot(){
		return this.input.getStackInSlot(SPEED_SLOT);
	}



	//----------------------- HANDLER -----------------------
	public ItemStackHandler getTemplate(){
		return this.template;
	}

	@Override
	public int getGUIHeight() {
		return GuiMineralAnalyzer.HEIGHT;
	}

	public int speedAnalyzer() {
		return ToolUtils.isValidSpeedUpgrade(speedSlot()) ? ModConfig.speedAnalyzer / ToolUtils.speedUpgrade(speedSlot()): ModConfig.speedAnalyzer;
	}

	public int getCookTimeMax() {
		return speedAnalyzer();
	}



	//----------------------- CUSTOM -----------------------
	public boolean hasRecipe(ItemStack stack){
		return MachineRecipes.analyzerRecipes.stream().anyMatch(
				recipe -> stack != null && recipe.getInput() != null && stack.isItemEqual(recipe.getInput()));
	}

	private MineralAnalyzerRecipe getRecipe (int x){
		return MachineRecipes.analyzerRecipes.get(x);
	}

	public float getGravity(){
		if(isPowered() && hasGravity()){
			return ((float)getBlockPower() * 2) + 2.0F;
		}else if(!isPowered() && hasGravity()){
			return this.gravity;
		}else if(!hasGravity()){
			return 0F;
		}
		return 0F;
	}

	public boolean isPowered(){
		return worldObj.isBlockPowered(this.pos);
	}

	public int getBlockPower(){
		return isPowered() ? worldObj.isBlockIndirectlyGettingPowered(this.pos) : 0;
	}

	public boolean canDrainAcids(){
		return drainValve;
	}

	public MineralAnalyzerRecipe getRecipe(){
		for(int x = 0; x < MachineRecipes.analyzerRecipes.size(); x++){
			if(getRecipe(x).getInput() != null && ItemStack.areItemsEqual(getRecipe(x).getInput(), input.getStackInSlot(INPUT_SLOT))){
				return getRecipe(x);
			}
		}
		return null;
	}

	public boolean hasGravity(){
		return getRecipe() != null ? getRecipe().hasGravity() : false; 
	}

	public boolean isValidRecipe() {
		return getRecipe() != null;
	}

	public ArrayList<ItemStack> recipeOutput(){
		return isValidRecipe() ? getRecipe().getOutput() : null;
	}

	public ArrayList<Integer> recipeGravity(){
		return isValidRecipe() ? getRecipe().getProbability() : null;
	}



	//----------------------- I/O -----------------------
	@Override
	public void readFromNBT(NBTTagCompound compound){
		super.readFromNBT(compound);
		this.drainValve = compound.getBoolean("Drain");
		this.gravity = compound.getFloat("Gravity");
		this.sulfTank.readFromNBT(compound.getCompoundTag("SulfTank"));
		this.chloTank.readFromNBT(compound.getCompoundTag("ChloTank"));
		this.fluoTank.readFromNBT(compound.getCompoundTag("FluoTank"));
	}

	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound compound){
		super.writeToNBT(compound);
		compound.setBoolean("Drain", canDrainAcids());
		compound.setFloat("Gravity", this.gravity);

		NBTTagCompound sulfTankNBT = new NBTTagCompound();
		this.sulfTank.writeToNBT(sulfTankNBT);
		compound.setTag("SulfTank", sulfTankNBT);

		NBTTagCompound chloTankNBT = new NBTTagCompound();
		this.chloTank.writeToNBT(chloTankNBT);
		compound.setTag("ChloTank", chloTankNBT);

		NBTTagCompound fluoTankNBT = new NBTTagCompound();
		this.fluoTank.writeToNBT(fluoTankNBT);
		compound.setTag("FluoTank", fluoTankNBT);

		return compound;
	}

	@Override
	public FluidHandlerConcatenate getCombinedTank(){
		return new FluidHandlerConcatenate(lavaTank, sulfTank, chloTank, fluoTank);
	}



	//----------------------- PROCESS -----------------------
	@Override
	public void update(){
		acceptEnergy();
		fuelHandler(input.getStackInSlot(FUEL_SLOT));
		lavaHandler();
		if(!worldObj.isRemote){
			emptyContainer(SULFUR_SLOT, sulfTank);
			emptyContainer(CHLOR_SLOT, chloTank);
			emptyContainer(FLUO_SLOT, fluoTank);

			if(canAnalyze()){
				cookTime++; powerCount--;
				if(cookTime >= getCookTimeMax()) {
					cookTime = 0; 
					analyze();
				}
			}
			this.markDirtyClient();
		}
	}

	private boolean canAnalyze() {
		return isActive()
			&& output.getStackInSlot(OUTPUT_SLOT) == null
			&& isValidRange()
			&& hasRecipe(input.getStackInSlot(INPUT_SLOT))
			&& CoreUtils.hasConsumable(ToolUtils.agitator, input.getStackInSlot(CONSUMABLE_SLOT))
			&& getPower() >= getCookTimeMax()
			&& this.sulfTank.getFluidAmount() >= modSulf()
			&& this.chloTank.getFluidAmount() >= modChlo()
			&& this.fluoTank.getFluidAmount() >= modFluo();
	}

	public int modSulf() { return hasGravity() ? ModConfig.consumedSulf + ((int)getGravity()*2) : ModConfig.consumedSulf; }
	public int modChlo() { return hasGravity() ? ModConfig.consumedChlo + ((int)getGravity()*2) : ModConfig.consumedChlo; }
	public int modFluo() { return hasGravity() ? ModConfig.consumedFluo + ((int)getGravity()*2) : ModConfig.consumedFluo; }

	private void analyze(){
		if(getRecipe()!= null){
			if(getRecipe().getInput() != null && ItemStack.areItemsEqual(getRecipe().getInput(), input.getStackInSlot(INPUT_SLOT))){
				int mix = getRecipe().getOutput().size();
				if(!hasGravity()){
					if(mix > 1){
						output.setStackInSlot(OUTPUT_SLOT, ProbabilityStack.calculateProbability(getRecipe().getProbabilityStack()).copy());
						output.getStackInSlot(OUTPUT_SLOT).stackSize = rand.nextInt(ModConfig.maxMineral) + 1;
					}else{
						output.setStackInSlot(OUTPUT_SLOT, getRecipe().getOutput().get(0).copy());
					}
				}else{
					if(isValidRange()){
						output.setStackInSlot(OUTPUT_SLOT, pickedShards.get(rand.nextInt(pickedShards.size())).copy());
						output.getStackInSlot(OUTPUT_SLOT).stackSize = rand.nextInt(ModConfig.maxMineral) + 1;
					}
				}
			}
		}

		input.damageSlot(CONSUMABLE_SLOT);
		input.decrementSlot(INPUT_SLOT);
		input.drainOrClean(sulfTank, modSulf(), false);
		input.drainOrClean(chloTank, modChlo(), false);
		input.drainOrClean(fluoTank, modFluo(), false);
	}

	public boolean isValidRange() {
		if(hasGravity()){
			pickShards();
		}
		return !hasGravity() || (hasGravity() && !pickedShards.isEmpty() && pickedShards.size() > 0);
	}

	public void pickShards() {
		pickedShards = new ArrayList<ItemStack>();
		if(getRecipe()!= null){
			if(getRecipe().getInput() != null && ItemStack.areItemsEqual(getRecipe().getInput(), input.getStackInSlot(INPUT_SLOT))){
				int mix = getRecipe().getOutput().size();
				if(hasGravity()){
					if(mix > 1){
						for(int y = 0; y < mix; y++){
							if(currentGravity(y) >= minGravity() && currentGravity(y) <= maxGravity()){
								pickedShards.add(getRecipe().getOutput().get(y));
							}
						}
					}
				}
			}
		}
	}

	public float minGravity(){return getGravity() - 2.0F;}
	public float maxGravity(){return getGravity() + 2.0F;}
	public float currentGravity(int y){return (float)getRecipe().getProbability().get(y).intValue() / 100;}
}