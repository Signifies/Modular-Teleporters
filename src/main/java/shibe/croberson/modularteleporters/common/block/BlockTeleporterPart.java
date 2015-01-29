package shibe.croberson.modularteleporters.common.block;

import net.minecraft.block.BlockContainer;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import shibe.croberson.beefcore.core.multiblock.IMultiblockPart;
import shibe.croberson.beefcore.core.multiblock.MultiblockControllerBase;
import shibe.croberson.modularteleporters.common.creativeTab.MTCreativeTab;
import shibe.croberson.modularteleporters.common.multiblock.MultiblockTeleporter;
import shibe.croberson.modularteleporters.common.multiblock.tileentity.TileEntityTeleporterFluidPort;
import shibe.croberson.modularteleporters.common.multiblock.tileentity.TileEntityTeleporterPart;
import shibe.croberson.modularteleporters.common.multiblock.tileentity.TileEntityTeleporterPartBase;
import shibe.croberson.modularteleporters.common.multiblock.tileentity.TileEntityTeleporterFluidPort.FluidFlow;
import shibe.croberson.modularteleporters.reference.Reference;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class BlockTeleporterPart extends BlockContainer implements ITileEntityProvider{
	
	public static final int TELEPORTER_CASING = 0;
	public static final int TELEPORTER_CONTROLLER = 1;
	public static final int TELEPORTER_FLUID_PORT = 2;
	public static final int TELEPORTER_ROTOR_BEARING = 3;
	
	private static final String[] subBlocks = {"casing", "controller", "fluidPort", "bearing"};
	
	private static final int SUBICON_NONE = -1;
	private static final int SUBICON_CASING_TOP = 0;
	private static final int SUBICON_CASING_BOTTOM = 1;
	private static final int SUBICON_CASING_LEFT = 2;
	private static final int SUBICON_CASING_RIGHT = 3;
	private static final int SUBICON_CASING_FACE = 4;
	private static final int SUBICON_CASING_CORNER = 5;
	private static final int SUBICON_FLUIDPORT_OUTPUT = 6;
	
	private static final String[] subIconNames = {"casing.edge.0", "casing.edge.1", "casing.edge.2", "casing.edge.3", "casing.edge.4", "casing.face", "casing.corner", "casing.controller.active", "casing.controller.idle", "fluidPort.outlet" };
	
	
	private IIcon[] icons = new IIcon[subBlocks.length];
	private IIcon[] subIcons = new IIcon[subIconNames.length];
	
	public BlockTeleporterPart(Material material) {
		super(material);
		setStepSound(soundTypeMetal);
		setHardness(2.0F);
		setBlockName("blockTeleporterPart");
		setBlockTextureName(Reference.resourcePrefix + "blockTeleporterPart");
		setCreativeTab(MTCreativeTab.MODULAR_TELEPORTER_TAB);
	}
	
	@Override
	@SideOnly(Side.CLIENT)
	public void registerBlockIcons(IIconRegister registry) {
		for(int i = 0; i < subBlocks.length; i++) {
			icons[i] = registry.registerIcon(Reference.resourcePrefix + getUnlocalizedName() + "." + subBlocks[i]);
		}
		
		for(int i = 0; i < subIcons.length; i++) {
			icons[i] = registry.registerIcon(Reference.resourcePrefix + getUnlocalizedName() + "." + subIcons[i]);
		}
		this.blockIcon = icons[0];
	}
	
	@Override
	public IIcon getIcon(IBlockAccess blockAccess, int x, int y, int z, int side) {
		TileEntity te = blockAccess.getTileEntity(x, y, z);
		int metadata = blockAccess.getBlockMetadata(x, y, z);
		//insert meta-data texture mapping here
		if(te instanceof TileEntityTeleporterPartBase) { 
			TileEntityTeleporterPartBase part = (TileEntityTeleporterPartBase)te;
			MultiblockTeleporter teleporter = part.getTeleporter();
			
			if(metadata == TELEPORTER_FLUID_PORT) {//first check the fluid port
				if (te instanceof TileEntityTeleporterFluidPort) {
					if(teleporter == null || !teleporter.isAssembled() || part.getOutwardsDir().ordinal() == side) {
						if(((TileEntityTeleporterFluidPort)te).getFlowDirection() == FluidFlow.out) {
							return subIcons[SUBICON_FLUIDPORT_OUTPUT];
						} else {
							return icons[TELEPORTER_FLUID_PORT];
						}
						
					}else if(teleporter.isAssembled() && part.getOutwardsDir().ordinal() != side) {
						return subIcons[TELEPORTER_CASING];
					}
				}
				return getIcon(side, metadata); 
			} else if(!part.isConnected() || teleporter == null || !teleporter.isAssembled()) {
				return getIcon(side, metadata);
			} else {
				
			}
			
			if(metadata == TELEPORTER_CONTROLLER){
				
			}
			
		}
		
		
		//and end with this 
		return getIcon(side, metadata);
	}
	
	@Override
	public IIcon getIcon(int side, int metadata) {
		metadata = Math.max(0, Math.min(metadata, subBlocks.length-1));
		return icons[metadata];
		
	}
	@Override
	public TileEntity createNewTileEntity(World world, int metadata) {
		switch(metadata) {
		case TELEPORTER_FLUID_PORT:
			return new TileEntityTeleporterFluidPort();
		
		default: 
			return new TileEntityTeleporterPart();
		
		}
	}
	
	//checks what tileentity this block belongs to and routes it to the correct functionality
	@Override
	public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player, int par6, float par7, float par8, float par9) {
		if(player.isSneaking()) {
			return false;
		}
		
		int metadata = world.getBlockMetadata(x, y, z);
		
		if(world.isRemote) {
			return true;
		}
		
		// If the player's hands are empty and they rightclick on a multiblock, they get a 
		// multiblock-debugging message if the machine is not assembled.
		if(player.getCurrentEquippedItem() == null) {
			TileEntity te = world.getTileEntity(x, y, z);
			if(te instanceof IMultiblockPart) {
				MultiblockControllerBase controller = ((IMultiblockPart)te).getMultiblockController();

				if(controller == null) {
					player.addChatMessage(new ChatComponentText(String.format("SERIOUS ERROR - server part @ %d, %d, %d has no controller!", x, y, z))); //TODO Localize
				}
				else {
					Exception e = controller.getLastValidationException();
					if(e != null) {
						player.addChatMessage(new ChatComponentText(e.getMessage() + " - controller " + Integer.toString(controller.hashCode()))); //Would it be worth it to localize one word?
						return true;
					}
				}
			}
		}
		
		if(metadata != TELEPORTER_CONTROLLER){ return false; }
		
		//check if machine is assembled
		TileEntity te = world.getTileEntity(x, y, z);
		if(!(te instanceof IMultiblockPart)){
			return false;
		}
		
		IMultiblockPart part = (IMultiblockPart)te;
		if(!part.isConnected() || !part.getMultiblockController().isAssembled()){
			return false;
		}
		//insert openGuiMethod call here
		return true;
	}
	
	
	
}
