package diamondvending.platform.forge;

import diamondvending.DiamondVending;
import diamondvending.registry.MenuHandle;
import diamondvending.registry.Registrar;
import diamondvending.registry.RegistryCompat;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.network.NetworkHooks;
import net.minecraftforge.registries.DeferredRegister;

import java.util.function.Function;
import java.util.function.Supplier;

/** Forge registers lazily through DeferredRegisters attached to the mod event bus. */
final class ForgeRegistrar implements Registrar {
    private final DeferredRegister<Block> blocks = DeferredRegister.create(Registries.BLOCK, DiamondVending.MOD_ID);
    private final DeferredRegister<Item> items = DeferredRegister.create(Registries.ITEM, DiamondVending.MOD_ID);
    private final DeferredRegister<BlockEntityType<?>> blockEntities = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, DiamondVending.MOD_ID);
    private final DeferredRegister<MenuType<?>> menus = DeferredRegister.create(Registries.MENU, DiamondVending.MOD_ID);

    void registerAll(IEventBus modBus) {
        blocks.register(modBus);
        items.register(modBus);
        blockEntities.register(modBus);
        menus.register(modBus);
    }

    @Override
    public <M extends AbstractContainerMenu> MenuHandle<M> menu(String name, BlockMenuFactory<M> factory) {
        Supplier<MenuType<M>> type = menus.register(name,
                () -> IForgeMenuType.<M>create((id, inventory, data) -> factory.create(id, inventory, data.readBlockPos())));
        return new MenuHandle<>() {
            @Override
            public MenuType<M> type() {
                return type.get();
            }

            @Override
            public void open(ServerPlayer player, Component title, BlockPos pos) {
                // The position rides along with the "open screen" packet, so the client's menu finds the machine.
                NetworkHooks.openScreen(player, new SimpleMenuProvider((id, inventory, opener) -> factory.create(id, inventory, pos), title), pos);
            }
        };
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
        return blockEntities.register(name, () -> BlockEntityType.Builder.<T>of(factory::create, block.get()).build(null));
    }
}
