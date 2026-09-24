package diamondvending.registry;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

import java.util.function.Function;
import java.util.function.Supplier;

/** Registers content with the running loader. Each loader provides one implementation. */
public interface Registrar {
    /** Creates a block entity. Our own interface: vanilla's {@code BlockEntitySupplier} is private on 26.1. */
    @FunctionalInterface
    interface BlockEntityFactory<T extends BlockEntity> {
        T create(BlockPos pos, BlockState state);
    }

    <B extends Block> Supplier<B> block(String name, Function<BlockBehaviour.Properties, B> factory, BlockBehaviour.Properties properties);

    <I extends Item> Supplier<I> item(String name, Function<Item.Properties, I> factory, Item.Properties properties);

    <T extends BlockEntity> Supplier<BlockEntityType<T>> blockEntity(String name, BlockEntityFactory<T> factory, Supplier<? extends Block> block);

    /** A data component type: a piece of data an item can carry, such as a machine's setup. */
    <T> Supplier<DataComponentType<T>> dataComponent(String name, Supplier<DataComponentType<T>> type);
}
