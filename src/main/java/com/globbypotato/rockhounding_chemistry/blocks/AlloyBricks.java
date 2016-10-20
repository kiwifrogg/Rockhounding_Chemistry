package com.globbypotato.rockhounding_chemistry.blocks;

import java.util.List;
import java.util.Random;

import com.globbypotato.rockhounding_chemistry.handlers.ModArray;
import com.globbypotato.rockhounding_chemistry.handlers.Reference;

import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class AlloyBricks extends Block implements IMetaBlockName {
	public static final PropertyEnum VARIANT = PropertyEnum.create("type", EnumAlloy.class);
	Random rand = new Random();
	
    public AlloyBricks(float hardness, float resistance, String name){
        super(Material.IRON);
		setRegistryName(name);
		setUnlocalizedName(getRegistryName().toString());
		setHarvestLevel("pickaxe", 0);
		setHardness(hardness); setResistance(resistance);	
		setSoundType(SoundType.METAL);
		setCreativeTab(Reference.RockhoundingChemistry);
		this.setDefaultState(this.blockState.getBaseState().withProperty(VARIANT, EnumAlloy.byMetadata(0)));
    }

	@Override
	public IBlockState getStateFromMeta(int meta) {
		return this.getDefaultState().withProperty(VARIANT, EnumAlloy.byMetadata(meta));
	}

	@Override
	public int getMetaFromState(IBlockState state) {
		EnumAlloy type = (EnumAlloy) state.getValue(VARIANT);
		return type.getMetadata();
	}

	@Override
	public BlockStateContainer createBlockState(){
		return new BlockStateContainer(this, new IProperty[] { VARIANT });
	}

	@Override
	public void getSubBlocks(Item itemIn, CreativeTabs tab, List list) {
		for (int i = 0; i < EnumAlloy.values().length; i++){
			list.add(new ItemStack(itemIn, 1, i));
		}
	}

	@Override
	public String getSpecialName(ItemStack stack) {
		return ModArray.alloyArray[stack.getItemDamage()];
	}

	@Override
    public Item getItemDropped(IBlockState state, Random rand, int fortune){
		return Item.getItemFromBlock(this) ;
	}

    public int damageDropped(IBlockState state){
    	return getMetaFromState(state);
    }

	public int quantityDropped(Random rand) {
		return 1;
	}

	@Override
    public void onEntityWalk(World world, BlockPos pos, Entity entity){
        IBlockState state = world.getBlockState(pos);
        EnumAlloy type = (EnumAlloy) state.getValue(VARIANT);
		if(type.getMetadata() == 0){
			EntityLivingBase player = (EntityLivingBase) entity;
			if(player!= null){
				if(player.getItemStackFromSlot(EntityEquipmentSlot.FEET) != null) {
		            double d0 = (double)pos.getX() + rand.nextDouble();
		            double d1 = (double)pos.getY() + 1D;
		            double d2 = (double)pos.getZ() + rand.nextDouble();
		            world.spawnParticle(EnumParticleTypes.FLAME, d0, d1, d2, 0.0D, 0.0D, 0.0D, new int[0]);
				}
			}
		}
    }
}