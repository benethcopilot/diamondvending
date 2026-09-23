package diamondvending.platform.neoforge;

import diamondvending.DiamondVending;
import diamondvending.registry.Registrar;
import diamondvending.registry.RegistryCompat;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Function;
import java.util.function.Supplier;

/** NeoForge registers lazily through DeferredRegisters attached to the mod event bus. */
final class NeoForgeRegistrar implements Registrar {
    private final DeferredRegister<Block> blocks = DeferredRegister.create(Registries.BLOCK, DiamondVending.MOD_ID);
    private final DeferredRegister<Item> items = DeferredRegister.create(Registries.ITEM, DiamondVending.MOD_ID);
    private final DeferredRegister<BlockEntityType<?>> blockEntities = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, DiamondVending.MOD_ID);

    void registerAll(IEventBus modBus) {
        blocks.register(modBus);
        items.register(modBus);
        blockEntities.register(modBus);
    }

    @Override
    public <B extends Block> Supplier<B> block(String name, Function<BlockBehaviour.Properties, B> factory, BlockBehaviour.Properties properties) {
        return blocks.register(name, () -> factory.apply(RegistryCompat.blockProperties(properties, name)));
    }

    @Override
    public <I extends Item> Supplier<I> item(String name, Function<Item.Properties, I> factory, Item.Properties properties) {
        return items.register(name, () -> factory.apply(RegistryCompat.itemProperties(properties, name)));
    }

    @Override
    public <T extends BlockEntity> Supplier<BlockEntityType<T>> blockEntity(String name, BlockEntityFactory<T> factory,
                                                                            Supplier<? extends Block> block) {
        //? if >=26.1 {
        return blockEntities.register(name, () -> new BlockEntityType<>(factory::create, block.get()));
        //?} else {
        /*return blockEntities.register(name, () -> BlockEntityType.Builder.<T>of(factory::create, block.get()).build(null));
        *///?}
    }
}
