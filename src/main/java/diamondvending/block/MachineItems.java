package diamondvending.block;

import diamondvending.registry.ModContent;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.ItemStack;
//? if <26.1 {
/*import net.minecraft.world.item.component.CustomModelData;
*///?}

/** Machine items and their color. */
public final class MachineItems {
    private MachineItems() {}

    /** A machine item that places a machine of this color. */
    public static ItemStack forColor(DyeColor color) {
        ItemStack stack = new ItemStack(ModContent.VENDING_MACHINE_ITEM.get());
        stack.set(DataComponents.BASE_COLOR, color);
        //? if <26.1 {
        /*// 1.21.1 item models can only switch on custom_model_data: dye id + 1
        stack.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(color.getId() + 1));
        *///?}
        return stack;
    }

    /** The color a machine item places; items without a color place red machines. */
    public static DyeColor colorOf(ItemStack stack) {
        return stack.getOrDefault(DataComponents.BASE_COLOR, DyeColor.RED);
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
