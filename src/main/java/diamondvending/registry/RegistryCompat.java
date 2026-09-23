package diamondvending.registry;

import diamondvending.DiamondVending;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.state.BlockBehaviour;

/** Registration details that differ between Minecraft versions. */
public final class RegistryCompat {
    private RegistryCompat() {}

    /** 26.1 requires blocks to know their id when constructed. */
    public static BlockBehaviour.Properties blockProperties(BlockBehaviour.Properties properties, String name) {
        //? if >=26.1 {
        return properties.setId(ResourceKey.create(Registries.BLOCK, DiamondVending.id(name)));
        //?} else {
        /*return properties;
        *///?}
    }

    /** 26.1 requires items to know their id when constructed. */
    public static Item.Properties itemProperties(Item.Properties properties, String name) {
        //? if >=26.1 {
        return properties.setId(ResourceKey.create(Registries.ITEM, DiamondVending.id(name)));
        //?} else {
        /*return properties;
        *///?}
    }

    /** Properties for a block's item: its name comes from the block's translation key. */
    public static Item.Properties blockItemProperties() {
        //? if >=26.1 {
        return new Item.Properties().useBlockDescriptionPrefix();
        //?} else {
        /*return new Item.Properties();
        *///?}
    }
}
