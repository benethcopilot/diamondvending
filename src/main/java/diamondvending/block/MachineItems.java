package diamondvending.block;

import diamondvending.registry.ModContent;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.ItemStack;
//? if >=1.20.5 {
import net.minecraft.core.component.DataComponents;
//?} else {
/*import com.mojang.serialization.DataResult;
import diamondvending.DiamondVending;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
*///?}
//? if <26.1 && >=1.20.5 {
/*import net.minecraft.world.item.component.CustomModelData;
*///?}

/** Machine items: their color, and the setup a broken machine keeps (spec §5.4). */
public final class MachineItems {
    //? if <1.20.5 {
    /*// 1.20.1 items carry an NBT tag instead of components (Forge 1.20.1 spec §5.1–5.2).
    private static final String COLOR_TAG = "diamondvending:color";
    private static final String SETUP_TAG = "diamondvending:machine_setup";
    *///?}

    private MachineItems() {}

    /** A machine item that places a machine of this color. */
    public static ItemStack forColor(DyeColor color) {
        ItemStack stack = new ItemStack(ModContent.VENDING_MACHINE_ITEM.get());
        //? if >=26.1 {
        stack.set(DataComponents.BASE_COLOR, color);
        //?} else if >=1.20.5 {
        /*stack.set(DataComponents.BASE_COLOR, color);
        // 1.21.1 item models can only switch on custom_model_data: dye id + 1
        stack.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(color.getId() + 1));
        *///?} else {
        /*stack.getOrCreateTag().putString(COLOR_TAG, color.getName());
        // 1.20.1 item models switch on CustomModelData too: dye id + 1
        stack.getOrCreateTag().putInt("CustomModelData", color.getId() + 1);
        *///?}
        return stack;
    }

    /** The item a broken machine drops (spec §5.4): its color, and its setup when it has one. */
    public static ItemStack forMachine(DyeColor color, VendingMachineBlockEntity machine) {
        ItemStack stack = forColor(color);
        if (machine != null) {
            MachineSetup setup = MachineSetup.of(machine);
            if (!setup.isEmpty()) {
                //? if >=1.20.5 {
                stack.set(ModContent.MACHINE_SETUP.get(), setup);
                //?} else {
                /*MachineSetup.CODEC.encodeStart(NbtOps.INSTANCE, setup).result()
                        .ifPresent(tag -> stack.getOrCreateTag().put(SETUP_TAG, tag));
                *///?}
            }
        }
        return stack;
    }

    /** The setup a machine item kept, or null for a plain item. */
    public static MachineSetup setupOf(ItemStack stack) {
        //? if >=1.20.5 {
        return stack.get(ModContent.MACHINE_SETUP.get());
        //?} else {
        /*CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(SETUP_TAG)) return null;
        // Spec §9: a setup that can't be read is left out, so the machine is placed empty rather than half set up.
        DataResult<MachineSetup> read = MachineSetup.CODEC.parse(NbtOps.INSTANCE, tag.get(SETUP_TAG));
        read.error().ifPresent(error -> DiamondVending.LOGGER.warn(
                "A vending machine item's kept setup can't be read, so it places an empty machine: {}", error.message()));
        return read.result().orElse(null);
        *///?}
    }

    /** The color a machine item places; items without a color place red machines. */
    public static DyeColor colorOf(ItemStack stack) {
        //? if >=1.20.5 {
        return stack.getOrDefault(DataComponents.BASE_COLOR, DyeColor.RED);
        //?} else {
        /*CompoundTag tag = stack.getTag();
        return tag == null ? DyeColor.RED : DyeColor.byName(tag.getString(COLOR_TAG), DyeColor.RED);
        *///?}
    }

    /** The dye color of a dye item, or null if the stack isn't a dye. */
    public static DyeColor dyeColorOf(ItemStack stack) {
        //? if >=26.1 {
        return stack.getItem() instanceof DyeItem ? stack.get(DataComponents.DYE) : null;
        //?} else {
        /*return stack.getItem() instanceof DyeItem dye ? dye.getDyeColor() : null;
        *///?}
    }
}
