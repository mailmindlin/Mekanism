package mekanism.common.block;

import javax.annotation.Nonnull;
import mekanism.api.RelativeSide;
import mekanism.api.block.IBlockElectric;
import mekanism.api.block.IHasInventory;
import mekanism.api.block.IHasSecurity;
import mekanism.api.block.IHasTileEntity;
import mekanism.api.block.ISupportsComparator;
import mekanism.api.block.ISupportsRedstone;
import mekanism.api.tier.BaseTier;
import mekanism.api.transmitters.TransmissionType;
import mekanism.common.MekanismLang;
import mekanism.common.base.ILangEntry;
import mekanism.common.block.interfaces.IHasDescription;
import mekanism.common.block.interfaces.IHasGui;
import mekanism.common.block.interfaces.ITieredBlock;
import mekanism.common.block.interfaces.IUpgradeableBlock;
import mekanism.common.block.states.BlockStateHelper;
import mekanism.common.block.states.IStateFacing;
import mekanism.common.block.states.IStateFluidLoggable;
import mekanism.common.inventory.container.ContainerProvider;
import mekanism.common.inventory.container.tile.EnergyCubeContainer;
import mekanism.common.item.block.ItemBlockEnergyCube;
import mekanism.common.registries.MekanismBlocks;
import mekanism.common.registries.MekanismTileEntityTypes;
import mekanism.common.tier.EnergyCubeTier;
import mekanism.common.tile.TileEntityEnergyCube;
import mekanism.common.tile.base.TileEntityMekanism;
import mekanism.common.tile.base.WrenchResult;
import mekanism.common.tile.component.config.ConfigInfo;
import mekanism.common.tile.component.config.DataType;
import mekanism.common.tile.component.config.slot.ISlotInfo;
import mekanism.common.util.MekanismUtils;
import mekanism.common.util.SecurityUtils;
import mekanism.common.util.VoxelShapeUtils;
import mekanism.common.util.text.TextComponentUtil;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.material.Material;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.container.INamedContainerProvider;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.state.DirectionProperty;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Direction;
import net.minecraft.util.Hand;
import net.minecraft.util.NonNullList;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.shapes.ISelectionContext;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.util.math.shapes.VoxelShapes;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.World;

/**
 * Block class for handling multiple energy cube block IDs. 0: Basic Energy Cube 1: Advanced Energy Cube 2: Elite Energy Cube 3: Ultimate Energy Cube 4: Creative Energy
 * Cube
 *
 * @author AidanBrady
 */
public class BlockEnergyCube extends BlockMekanism implements IHasGui<TileEntityEnergyCube>, IStateFacing, ITieredBlock<EnergyCubeTier>, IBlockElectric, IHasInventory,
      IHasSecurity, ISupportsRedstone, IHasTileEntity<TileEntityEnergyCube>, ISupportsComparator, IStateFluidLoggable, IHasDescription, IUpgradeableBlock {

    private static final VoxelShape[] bounds = new VoxelShape[256];

    static {
        VoxelShape frame = VoxelShapeUtils.combine(
              makeCuboidShape(0, 0, 0, 3, 3, 16),
              makeCuboidShape(0, 3, 0, 3, 16, 3),
              makeCuboidShape(0, 3, 13, 3, 16, 16),
              makeCuboidShape(0, 13, 3, 3, 16, 13),
              makeCuboidShape(3, 0, 0, 16, 3, 3),
              makeCuboidShape(3, 0, 13, 16, 3, 16),
              makeCuboidShape(3, 13, 0, 16, 16, 3),
              makeCuboidShape(3, 13, 13, 16, 16, 16),
              makeCuboidShape(13, 0, 3, 16, 3, 13),
              makeCuboidShape(13, 3, 0, 16, 13, 3),
              makeCuboidShape(13, 3, 13, 16, 13, 16),
              makeCuboidShape(13, 13, 3, 16, 16, 13),
              makeCuboidShape(12.5, 14.9, 7.5, 13.5, 15.9, 8.5),//ledTop1
              makeCuboidShape(2.5, 14.9, 7.5, 3.5, 15.9, 8.5),//ledTop2
              makeCuboidShape(12.5, 7.5, 0.1, 13.5, 8.5, 1.1),//ledBack1
              makeCuboidShape(2.5, 7.5, 0.1, 3.5, 8.5, 1.1),//ledBack2
              makeCuboidShape(2.5, 0.1, 7.5, 3.5, 1.1, 8.5),//ledBottom2
              makeCuboidShape(12.5, 0.1, 7.5, 13.5, 1.1, 8.5),//ledBottom1
              makeCuboidShape(12.5, 7.5, 14.9, 13.5, 8.5, 15.9),//ledFront1
              makeCuboidShape(2.5, 7.5, 14.9, 3.5, 8.5, 15.9),//ledFront2
              makeCuboidShape(0.1, 7.5, 2.5, 1.1, 8.5, 3.5),//ledRight2
              makeCuboidShape(0.1, 7.5, 12.5, 1.1, 8.5, 13.5),//ledRight1
              makeCuboidShape(14.9, 7.5, 2.5, 15.9, 8.5, 3.5),//ledLeft1
              makeCuboidShape(14.9, 7.5, 12.5, 15.9, 8.5, 13.5)//ledLeft2
        );
        VoxelShape frontPanel = VoxelShapeUtils.combine(
              makeCuboidShape(3, 5, 14, 13, 11, 15),//connectorFrontToggle
              makeCuboidShape(4, 4, 15, 12, 12, 16)//portFrontToggle
        );
        VoxelShape rightPanel = VoxelShapeUtils.combine(
              makeCuboidShape(1, 5, 3, 2, 11, 13),//connectorRightToggle
              makeCuboidShape(0, 4, 4, 1, 12, 12)//portRightToggle
        );
        VoxelShape leftPanel = VoxelShapeUtils.combine(
              makeCuboidShape(14, 5, 3, 15, 11, 13),//connectorLeftToggle
              makeCuboidShape(15, 4, 4, 16, 12, 12)//portLeftToggle
        );
        VoxelShape backPanel = VoxelShapeUtils.combine(
              makeCuboidShape(3, 5, 1, 13, 11, 2),//connectorBackToggle
              makeCuboidShape(4, 4, 0, 12, 12, 1)//portBackToggle
        );
        VoxelShape topPanel = VoxelShapeUtils.combine(
              makeCuboidShape(3, 14, 5, 13, 15, 11),//connectorTopToggle
              makeCuboidShape(4, 15, 4, 12, 16, 12)//portTopToggle
        );
        VoxelShape bottomPanel = VoxelShapeUtils.combine(
              makeCuboidShape(3, 1, 5, 13, 2, 11),//connectorBottomToggle
              makeCuboidShape(4, 0, 4, 12, 1, 12)//portBottomToggle
        );
        VoxelShape frameRotated = VoxelShapeUtils.rotate(frame, Rotation.CLOCKWISE_90);
        VoxelShape topRotated = VoxelShapeUtils.rotate(topPanel, Rotation.CLOCKWISE_90);
        VoxelShape bottomRotated = VoxelShapeUtils.rotate(bottomPanel, Rotation.CLOCKWISE_90);
        VoxelShape frameRotatedAlt = VoxelShapeUtils.rotate(frame, Direction.NORTH);
        VoxelShape rightRotated = VoxelShapeUtils.rotate(rightPanel, Direction.NORTH);
        VoxelShape leftRotated = VoxelShapeUtils.rotate(leftPanel, Direction.NORTH);
        for (int rotated = 0; rotated < 3; rotated++) {
            //If we don't need to rotate anything, this is zero
            // If we need to rotate the top and bottom frames, this is one
            // If we need to rotate the left and right frames, this is two
            boolean rotateVertical = rotated == 1;
            boolean rotateHorizontal = rotated == 2;
            VoxelShape baseFrame = rotateVertical ? frameRotated : rotateHorizontal ? frameRotatedAlt : frame;
            for (int top = 0; top < 2; top++) {
                VoxelShape withTop = top == 0 ? baseFrame : VoxelShapes.or(baseFrame, rotateVertical ? topRotated : topPanel);
                for (int bottom = 0; bottom < 2; bottom++) {
                    VoxelShape withBottom = bottom == 0 ? withTop : VoxelShapes.or(withTop, rotateVertical ? bottomRotated : bottomPanel);
                    for (int front = 0; front < 2; front++) {
                        VoxelShape withFront = front == 0 ? withBottom : VoxelShapes.or(withBottom, frontPanel);
                        for (int back = 0; back < 2; back++) {
                            VoxelShape withBack = back == 0 ? withFront : VoxelShapes.or(withFront, backPanel);
                            for (int left = 0; left < 2; left++) {
                                VoxelShape withLeft = left == 0 ? withBack : VoxelShapes.or(withBack, rotateHorizontal ? leftRotated : leftPanel);
                                for (int right = 0; right < 2; right++) {
                                    VoxelShape withRight = right == 0 ? withLeft : VoxelShapes.or(withLeft, rotateHorizontal ? rightRotated : rightPanel);
                                    bounds[getIndex(top, bottom, front, back, left, right, rotateVertical, rotateHorizontal)] = withRight;
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * 0 for an input is equivalent to false, 1 is equivalent to true
     */
    private static int getIndex(int top, int bottom, int front, int back, int left, int right, boolean rotateVertical, boolean rotateHorizontal) {
        return ((((((top | bottom << 1) | front << 2) | back << 3) | left << 4) | right << 5) | (rotateVertical ? 1 : 0) << 6) | (rotateHorizontal ? 1 : 0) << 7;
    }

    private final EnergyCubeTier tier;

    public BlockEnergyCube(EnergyCubeTier tier) {
        //Note: We require setting variable opacity so that the block state does not cache the ability of if blocks can be placed on top of the energy cube
        // this may change based on what sides are enabled
        //TODO: We still need to fix trying to place things like torches on the side
        super(Block.Properties.create(Material.IRON).hardnessAndResistance(2F, 4F).variableOpacity());
        this.tier = tier;
    }

    @Override
    public EnergyCubeTier getTier() {
        return tier;
    }

    @Nonnull
    @Override
    public DirectionProperty getFacingProperty() {
        return BlockStateHelper.facingProperty;
    }

    @Override
    @Deprecated
    public void neighborChanged(BlockState state, World world, BlockPos pos, Block neighborBlock, BlockPos neighborPos, boolean isMoving) {
        if (!world.isRemote) {
            TileEntityMekanism tile = MekanismUtils.getTileEntity(TileEntityMekanism.class, world, pos);
            if (tile != null) {
                tile.onNeighborChange(neighborBlock);
            }
        }
    }

    @Override
    public void setTileData(World world, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack, @Nonnull TileEntityMekanism tile) {
        if (tile instanceof TileEntityEnergyCube) {
            if (tier == EnergyCubeTier.CREATIVE) {
                ConfigInfo energyConfig = ((TileEntityEnergyCube) tile).configComponent.getConfig(TransmissionType.ENERGY);
                if (energyConfig != null) {
                    energyConfig.fill(((ItemBlockEnergyCube) stack.getItem()).getEnergy(stack) > 0 ? DataType.OUTPUT : DataType.INPUT);
                }
            }
        }
    }

    @Override
    public void fillItemGroup(@Nonnull ItemGroup group, @Nonnull NonNullList<ItemStack> items) {
        super.fillItemGroup(group, items);
        //Charged
        ItemStack charged = new ItemStack(this);
        ((ItemBlockEnergyCube) charged.getItem()).setEnergy(charged, tier.getMaxEnergy());
        items.add(charged);
    }

    @Override
    @Deprecated
    public float getPlayerRelativeBlockHardness(BlockState state, @Nonnull PlayerEntity player, @Nonnull IBlockReader world, @Nonnull BlockPos pos) {
        return SecurityUtils.canAccess(player, MekanismUtils.getTileEntity(world, pos)) ? super.getPlayerRelativeBlockHardness(state, player, world, pos) : 0.0F;
    }

    @Nonnull
    @Override
    public ActionResultType onBlockActivated(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockRayTraceResult hit) {
        if (world.isRemote) {
            return ActionResultType.SUCCESS;
        }
        TileEntityMekanism tile = MekanismUtils.getTileEntity(TileEntityMekanism.class, world, pos);
        if (tile == null) {
            return ActionResultType.PASS;
        }
        if (tile.tryWrench(state, player, hand, hit) != WrenchResult.PASS) {
            return ActionResultType.SUCCESS;
        }
        return tile.openGui(player);
    }

    @Override
    public double getStorage() {
        return tier.getMaxEnergy();
    }

    @Nonnull
    @Override
    @Deprecated
    public VoxelShape getShape(BlockState state, IBlockReader world, BlockPos pos, ISelectionContext context) {
        TileEntityEnergyCube energyCube = MekanismUtils.getTileEntity(TileEntityEnergyCube.class, world, pos, true);
        int index;
        if (energyCube == null) {
            //Default to facing north all enabled
            index = getIndex(1, 1, 1, 1, 1, 1, false, false);
        } else {
            ConfigInfo energyConfig = energyCube.configComponent.getConfig(TransmissionType.ENERGY);
            if (energyConfig == null) {
                //Default to facing north all enabled
                index = getIndex(1, 1, 1, 1, 1, 1, false, false);
            } else {
                Direction facing = getDirection(state);
                index = getIndex(
                      isSideEnabled(energyConfig, facing, Direction.UP),//top
                      isSideEnabled(energyConfig, facing, Direction.DOWN),//bottom
                      isSideEnabled(energyConfig, facing, Direction.SOUTH),//front
                      isSideEnabled(energyConfig, facing, Direction.NORTH),//back
                      isSideEnabled(energyConfig, facing, Direction.EAST),//left
                      isSideEnabled(energyConfig, facing, Direction.WEST),//right
                      facing == Direction.EAST || facing == Direction.WEST,
                      facing == Direction.DOWN || facing == Direction.UP
                );
            }
        }
        return bounds[index];
    }

    /**
     * @return 1 if the side is enabled, 0 otherwise
     */
    private static int isSideEnabled(ConfigInfo energyConfig, Direction facing, Direction side) {
        ISlotInfo slotInfo = energyConfig.getSlotInfo(RelativeSide.fromDirections(facing, side));
        return slotInfo != null && slotInfo.isEnabled() ? 1 : 0;
    }

    @Override
    public INamedContainerProvider getProvider(TileEntityEnergyCube tile) {
        return new ContainerProvider(TextComponentUtil.translate(getTranslationKey()), (i, inv, player) -> new EnergyCubeContainer(i, inv, tile));
    }

    @Override
    public TileEntityType<TileEntityEnergyCube> getTileType() {
        switch (tier) {
            case ADVANCED:
                return MekanismTileEntityTypes.ADVANCED_ENERGY_CUBE.getTileEntityType();
            case ELITE:
                return MekanismTileEntityTypes.ELITE_ENERGY_CUBE.getTileEntityType();
            case ULTIMATE:
                return MekanismTileEntityTypes.ULTIMATE_ENERGY_CUBE.getTileEntityType();
            case CREATIVE:
                return MekanismTileEntityTypes.CREATIVE_ENERGY_CUBE.getTileEntityType();
            case BASIC:
            default:
                return MekanismTileEntityTypes.BASIC_ENERGY_CUBE.getTileEntityType();
        }
    }

    @Nonnull
    @Override
    public ILangEntry getDescription() {
        return MekanismLang.DESCRIPTION_ENERGY_CUBE;
    }

    @Nonnull
    @Override
    public BlockState upgradeResult(@Nonnull BlockState current, @Nonnull BaseTier tier) {
        switch (tier) {
            case BASIC:
                return BlockStateHelper.copyStateData(current, MekanismBlocks.BASIC_ENERGY_CUBE.getBlock().getDefaultState());
            case ADVANCED:
                return BlockStateHelper.copyStateData(current, MekanismBlocks.ADVANCED_ENERGY_CUBE.getBlock().getDefaultState());
            case ELITE:
                return BlockStateHelper.copyStateData(current, MekanismBlocks.ELITE_ENERGY_CUBE.getBlock().getDefaultState());
            case ULTIMATE:
                return BlockStateHelper.copyStateData(current, MekanismBlocks.ULTIMATE_ENERGY_CUBE.getBlock().getDefaultState());
            case CREATIVE:
                return BlockStateHelper.copyStateData(current, MekanismBlocks.CREATIVE_ENERGY_CUBE.getBlock().getDefaultState());
        }
        return current;
    }
}