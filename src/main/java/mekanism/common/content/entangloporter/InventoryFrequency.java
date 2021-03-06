package mekanism.common.content.entangloporter;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;
import mekanism.api.TileNetworkList;
import mekanism.api.gas.GasTank;
import mekanism.api.inventory.slot.IInventorySlot;
import mekanism.common.PacketHandler;
import mekanism.common.config.MekanismConfig;
import mekanism.common.frequency.Frequency;
import mekanism.common.inventory.slot.InternalInventorySlot;
import mekanism.common.tier.FluidTankTier;
import mekanism.common.tier.GasTankTier;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.common.util.Constants.NBT;
import net.minecraftforge.fluids.capability.templates.FluidTank;

public class InventoryFrequency extends Frequency {

    public static final String ENTANGLOPORTER = "Entangloporter";
    private static final Supplier<FluidTank> FLUID_TANK_SUPPLIER = () -> new FluidTank(MekanismConfig.general.quantumEntangloporterFluidBuffer.get());
    private static final Supplier<GasTank> GAS_TANK_SUPPLIER = () -> new GasTank(MekanismConfig.general.quantumEntangloporterGasBuffer.get());

    public double storedEnergy;
    public FluidTank storedFluid;
    public GasTank storedGas;
    public double temperature;

    //TODO: input: configComponent.getOutput(TransmissionType.ITEM, side, getDirection()).ioState == IOState.INPUT
    //TODO: output: configComponent.getOutput(TransmissionType.ITEM, side, getDirection()).ioState == IOState.OUTPUT
    //TODO: FIXME?? ideally we don't pass null as the inventory??
    //TODO: FIX INVENTORY PERSISTENCE
    public final List<IInventorySlot> inventorySlots = Collections.singletonList(InternalInventorySlot.create(null));

    public InventoryFrequency(String n, UUID uuid) {
        super(n, uuid);
        storedFluid = FLUID_TANK_SUPPLIER.get();
        storedGas = GAS_TANK_SUPPLIER.get();
    }

    public InventoryFrequency(CompoundNBT nbtTags) {
        super(nbtTags);
    }

    public InventoryFrequency(PacketBuffer dataStream) {
        super(dataStream);
    }

    @Override
    public void write(CompoundNBT nbtTags) {
        super.write(nbtTags);
        nbtTags.putDouble("storedEnergy", storedEnergy);
        if (!storedFluid.isEmpty()) {
            nbtTags.put("storedFluid", storedFluid.writeToNBT(new CompoundNBT()));
        }
        if (!storedGas.isEmpty()) {
            nbtTags.put("storedGas", storedGas.write(new CompoundNBT()));
        }
        ListNBT tagList = new ListNBT();
        for (int slotCount = 0; slotCount < inventorySlots.size(); slotCount++) {
            CompoundNBT tagCompound = inventorySlots.get(slotCount).serializeNBT();
            if (!tagCompound.isEmpty()) {
                tagCompound.putByte("Slot", (byte) slotCount);
                tagList.add(tagCompound);
            }
        }
        nbtTags.put("Items", tagList);
        nbtTags.putDouble("temperature", temperature);
    }

    @Override
    protected void read(CompoundNBT nbtTags) {
        super.read(nbtTags);
        storedFluid = FLUID_TANK_SUPPLIER.get();
        storedGas = GAS_TANK_SUPPLIER.get();
        storedEnergy = nbtTags.getDouble("storedEnergy");

        if (nbtTags.contains("storedFluid")) {
            storedFluid.readFromNBT(nbtTags.getCompound("storedFluid"));
        }
        if (nbtTags.contains("storedGas")) {
            storedGas.read(nbtTags.getCompound("storedGas"));
            storedGas.setCapacity(MekanismConfig.general.quantumEntangloporterGasBuffer.get());
        }

        ListNBT tagList = nbtTags.getList("Items", NBT.TAG_COMPOUND);
        for (int tagCount = 0; tagCount < tagList.size(); tagCount++) {
            CompoundNBT tagCompound = tagList.getCompound(tagCount);
            byte slotID = tagCompound.getByte("Slot");
            if (slotID >= 0 && slotID < inventorySlots.size()) {
                inventorySlots.get(slotID).deserializeNBT(tagCompound);
            }
        }
        temperature = nbtTags.getDouble("temperature");
    }

    @Override
    public void write(TileNetworkList data) {
        super.write(data);
        data.add(storedEnergy);
        data.add(storedFluid.getFluid());
        data.add(storedGas.getStack());
        data.add(temperature);
    }

    @Override
    protected void read(PacketBuffer dataStream) {
        super.read(dataStream);
        storedFluid = new FluidTank(FluidTankTier.ULTIMATE.getOutput());
        storedGas = new GasTank(GasTankTier.ULTIMATE.getOutput());
        storedEnergy = dataStream.readDouble();
        storedFluid.setFluid(dataStream.readFluidStack());
        storedGas.setStack(PacketHandler.readGasStack(dataStream));
        temperature = dataStream.readDouble();
    }
}