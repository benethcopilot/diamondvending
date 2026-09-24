package diamondvending.registry;

import diamondvending.block.MachineSetup;
import diamondvending.block.VendingMachineBlock;
import diamondvending.block.VendingMachineBlockEntity;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.entity.BlockEntityType;

import java.util.function.Supplier;

/** Everything this mod registers. Filled in once by {@link #register}, called from each loader's entrypoint. */
public final class ModContent {
    public static Supplier<DataComponentType<MachineSetup>> MACHINE_SETUP;
    public static Supplier<VendingMachineBlock> VENDING_MACHINE;
    public static Supplier<BlockItem> VENDING_MACHINE_ITEM;
    public static Supplier<BlockEntityType<VendingMachineBlockEntity>> VENDING_MACHINE_BLOCK_ENTITY;

    private ModContent() {}

    public static void register(Registrar registrar) {
        MACHINE_SETUP = registrar.dataComponent("machine_setup", () -> DataComponentType.<MachineSetup>builder()
                .persistent(MachineSetup.CODEC)
                .networkSynchronized(ByteBufCodecs.fromCodecWithRegistries(MachineSetup.CODEC))
                .build());
        VENDING_MACHINE = registrar.block("vending_machine", VendingMachineBlock::new, VendingMachineBlock.defaultProperties());
        VENDING_MACHINE_ITEM = registrar.item("vending_machine",
                properties -> new BlockItem(VENDING_MACHINE.get(), properties), RegistryCompat.blockItemProperties());
        VENDING_MACHINE_BLOCK_ENTITY = registrar.blockEntity("vending_machine", VendingMachineBlockEntity::new, VENDING_MACHINE);
    }
}
