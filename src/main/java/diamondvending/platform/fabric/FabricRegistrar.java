package diamondvending.platform.fabric;

import diamondvending.DiamondVending;
import diamondvending.registry.MenuHandle;
import diamondvending.registry.Registrar;
import diamondvending.registry.RegistryCompat;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
//? if >=26.1 {
import net.fabricmc.fabric.api.menu.v1.ExtendedMenuProvider;
import net.fabricmc.fabric.api.menu.v1.ExtendedMenuType;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
//?} else {
/*import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType;
*///?}
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
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

    /** 26.1 makes vanilla's BlockEntityType constructor private, so Fabric API's builder is the way; on 1.21.1 that builder is deprecated in favour of vanilla's. */
    @Override
    public <T extends BlockEntity> Supplier<BlockEntityType<T>> blockEntity(String name, BlockEntityFactory<T> factory,
                                                                            Supplier<? extends Block> block) {
        //? if >=26.1 {
        BlockEntityType<T> built = FabricBlockEntityTypeBuilder.<T>create(factory::create, block.get()).build();
        //?} else {
        /*BlockEntityType<T> built = BlockEntityType.Builder.<T>of(factory::create, block.get()).build(null);
        *///?}
        BlockEntityType<T> type = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, DiamondVending.id(name), built);
        return () -> type;
    }

    @Override
    public <T> Supplier<DataComponentType<T>> dataComponent(String name, Supplier<DataComponentType<T>> type) {
        DataComponentType<T> registered = Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, DiamondVending.id(name), type.get());
        return () -> registered;
    }

    /** Fabric API's "extended" menu types send the block's position with the "open screen" packet. */
    @Override
    public <M extends AbstractContainerMenu> MenuHandle<M> menu(String name, BlockMenuFactory<M> factory) {
        //? if >=26.1 {
        MenuType<M> type = Registry.register(BuiltInRegistries.MENU, DiamondVending.id(name),
                new ExtendedMenuType<M, BlockPos>(factory::create, BlockPos.STREAM_CODEC));
        //?} else {
        /*MenuType<M> type = Registry.register(BuiltInRegistries.MENU, DiamondVending.id(name),
                new ExtendedScreenHandlerType<M, BlockPos>(factory::create, BlockPos.STREAM_CODEC));
        *///?}
        return new MenuHandle<>() {
            @Override
            public MenuType<M> type() {
                return type;
            }

            @Override
            public void open(ServerPlayer player, Component title, BlockPos pos) {
                player.openMenu(new PositionedMenu<>(factory, title, pos));
            }
        };
    }

    //? if >=26.1 {
    private record PositionedMenu<M extends AbstractContainerMenu>(BlockMenuFactory<M> factory, Component title, BlockPos pos)
            implements ExtendedMenuProvider<BlockPos> {
    //?} else {
    /*private record PositionedMenu<M extends AbstractContainerMenu>(BlockMenuFactory<M> factory, Component title, BlockPos pos)
            implements ExtendedScreenHandlerFactory<BlockPos> {
    *///?}
        @Override
        public BlockPos getScreenOpeningData(ServerPlayer player) {
            return pos;
        }

        @Override
        public Component getDisplayName() {
            return title;
        }

        @Override
        public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
            return factory.create(id, inventory, pos);
        }
    }
}
