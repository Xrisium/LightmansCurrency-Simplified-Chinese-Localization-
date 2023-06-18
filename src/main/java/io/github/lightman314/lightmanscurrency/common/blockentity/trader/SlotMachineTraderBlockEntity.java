package io.github.lightman314.lightmanscurrency.common.blockentity.trader;

import io.github.lightman314.lightmanscurrency.common.blockentity.TraderBlockEntity;
import io.github.lightman314.lightmanscurrency.common.core.ModBlockEntities;
import io.github.lightman314.lightmanscurrency.common.traders.slot_machine.SlotMachineTraderData;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.BlockState;

public class SlotMachineTraderBlockEntity extends TraderBlockEntity<SlotMachineTraderData> {

    public SlotMachineTraderBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SLOT_MACHINE_TRADER.get(), pos, state);
    }

    @Override
    protected SlotMachineTraderData buildNewTrader() { return new SlotMachineTraderData(this.level, this.worldPosition); }

    @Override
    protected SlotMachineTraderData createTraderFromOldData(CompoundTag compound) { return this.buildNewTrader(); }
}
