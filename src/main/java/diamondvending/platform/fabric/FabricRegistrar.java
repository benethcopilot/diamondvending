package diamondvending.platform.fabric;

import diamondvending.DiamondVending;
import diamondvending.registry.Registrar;
import diamondvending.registry.RegistryCompat;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;

import java.util.function.Function;
import java.util.function.Supplier;

/** Fabric registers immediately during mod initialization. */
final class FabricRegistrar implements Registrar {
    @Override
    public <B extends Block> Supplier<B> block(String name, Function<BlockBehaviour.Properties, B> factory, BlockBehaviour.Properties properties) {
        B block = Registry.register(BuiltInRegistries.BLOCK, DiamondVending.id(name), factory.apply(RegistryCompat.blockProperties(properties, name)));
        return () -> block;
    }

    @Override
    public <I extends Item> Supplier<I> item(String name, Function<Item.Properties, I> factory, Item.Properties properties) {
        I item = Registry.register(BuiltInRegistries.ITEM, DiamondVending.id(name), factory.apply(RegistryCompat.itemProperties(properties, name)));
        return () -> item;
    }

    /** Vanilla's BlockEntityType constructor is private on 26.1; Fabric API's builder works on every version. */
    @Override
    public <T extends BlockEntity> Supplier<BlockEntityType<T>> blockEntity(String name, BlockEntityFactory<T> factory,
                                                                            Supplier<? extends Block> block) {
        BlockEntityType<T> type = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, DiamondVending.id(name),
                FabricBlockEntityTypeBuilder.<T>create(factory::create, block.get()).build());
        return () -> type;
    }
}
