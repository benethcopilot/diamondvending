package diamondvending.gametest;

import diamondvending.block.VendingMachineBlockEntity;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEventPacket;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockEventData;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.entity.BlockEntity;
import io.netty.buffer.Unpooled;
//? if >=1.20.5 {
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.component.WrittenBookContent;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import java.util.Optional;
//?} else {
/*import net.minecraft.core.NonNullList;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.entity.BlockEntityType;
*///?}

import java.util.ArrayList;
import java.util.List;

/** What the game tests do differently on 1.20.1, where items carry NBT instead of components and saving needs no registries. */
final class TestCompat {
    private TestCompat() {}

    /** A mock player in a game mode. 1.20.1's {@code makeMockPlayer} has none (it's always creative), so there it's a {@link RecordingPlayer}. */
    static Player mockPlayer(GameTestHelper helper, GameType gameType) {
        //? if >=1.20.5 {
        return helper.makeMockPlayer(gameType);
        //?} else {
        /*return new RecordingPlayer(helper, gameType);
        *///?}
    }

    /** The machine as the game saves it to disk. */
    static CompoundTag save(GameTestHelper helper, VendingMachineBlockEntity machine) {
        //? if >=1.20.5 {
        return machine.saveWithFullMetadata(helper.getLevel().registryAccess());
        //?} else {
        /*return machine.saveWithFullMetadata();
        *///?}
    }

    /** What the server sends clients about the machine. */
    static CompoundTag updateTag(GameTestHelper helper, VendingMachineBlockEntity machine) {
        //? if >=1.20.5 {
        return machine.getUpdateTag(helper.getLevel().registryAccess());
        //?} else {
        /*return machine.getUpdateTag();
        *///?}
    }

    /** Loads a saved block entity at the machine's spot. */
    static BlockEntity load(GameTestHelper helper, VendingMachineBlockEntity machine, CompoundTag tag) {
        //? if >=1.20.5 {
        return BlockEntity.loadStatic(machine.getBlockPos(), machine.getBlockState(), tag, helper.getLevel().registryAccess());
        //?} else {
        /*return BlockEntity.loadStatic(machine.getBlockPos(), machine.getBlockState(), tag);
        *///?}
    }

    /** {@code stack}, renamed like an anvil does. */
    static ItemStack named(ItemStack stack, String name) {
        //? if >=1.20.5 {
        stack.set(DataComponents.CUSTOM_NAME, Component.literal(name));
        //?} else {
        /*stack.setHoverName(Component.literal(name));
        *///?}
        return stack;
    }

    /** {@code box} (a shulker box) with {@code inside} packed in it. */
    static ItemStack packed(ItemStack box, ItemStack inside) {
        //? if >=1.20.5 {
        box.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(List.of(inside)));
        //?} else {
        /*CompoundTag contents = ContainerHelper.saveAllItems(new CompoundTag(), NonNullList.of(ItemStack.EMPTY, inside));
        BlockItem.setBlockEntityData(box, BlockEntityType.SHULKER_BOX, contents);
        *///?}
        return box;
    }

    /** Whether an enchanted book has any enchantment stored on it. */
    static boolean hasStoredEnchantments(ItemStack book) {
        //? if >=1.20.5 {
        return !book.getOrDefault(DataComponents.STORED_ENCHANTMENTS, ItemEnchantments.EMPTY).isEmpty();
        //?} else {
        /*return !EnchantedBookItem.getEnchantments(book).isEmpty();
        *///?}
    }

    /** What a 2 × 1 crafting grid holding these two items makes, or an empty stack. */
    static ItemStack craft(GameTestHelper helper, ItemStack left, ItemStack right) {
        //? if >=26.1 {
        CraftingInput input = CraftingInput.of(2, 1, List.of(left, right));
        Optional<RecipeHolder<CraftingRecipe>> recipe = helper.getLevel().recipeAccess().getRecipeFor(RecipeType.CRAFTING, input, helper.getLevel());
        return recipe.map(holder -> holder.value().assemble(input)).orElse(ItemStack.EMPTY);
        //?} else if >=1.20.5 {
        /*CraftingInput input = CraftingInput.of(2, 1, List.of(left, right));
        Optional<RecipeHolder<CraftingRecipe>> recipe = helper.getLevel().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, helper.getLevel());
        return recipe.map(holder -> holder.value().assemble(input, helper.getLevel().registryAccess())).orElse(ItemStack.EMPTY);
        *///?} else {
        /*// The grid is only read, never changed, so it needs no menu.
        TransientCraftingContainer grid = new TransientCraftingContainer(null, 2, 1, NonNullList.of(ItemStack.EMPTY, left, right));
        return helper.getLevel().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, grid, helper.getLevel())
                .map(recipe -> recipe.assemble(grid, helper.getLevel().registryAccess())).orElse(ItemStack.EMPTY);
        *///?}
    }

    /** A written book's title, author and pages; empty strings and no pages for anything else. */
    record Book(String title, String author, List<Component> pages) {}

    static Book book(ItemStack stack) {
        //? if >=1.20.5 {
        WrittenBookContent content = stack.get(DataComponents.WRITTEN_BOOK_CONTENT);
        if (content == null) return new Book("", "", List.of());
        return new Book(content.title().raw(), content.author(), content.getPages(false));
        //?} else {
        /*CompoundTag tag = stack.getTag();
        if (!stack.is(Items.WRITTEN_BOOK) || tag == null) return new Book("", "", List.of());
        List<Component> pages = new ArrayList<>();
        ListTag written = tag.getList("pages", Tag.TAG_STRING);
        for (int i = 0; i < written.size(); i++) pages.add(Component.Serializer.fromJson(written.getString(i)));
        return new Book(tag.getString("title"), tag.getString("author"), pages);
        *///?}
    }

    /** Delivers a block event the way the server does: in a packet, as bytes. */
    @SuppressWarnings("deprecation") // NeoForge 1.21.1 wants a connection type for the buffer; there's no connection here
    static ClientboundBlockEventPacket overTheNetwork(GameTestHelper helper, BlockEventData event) {
        ClientboundBlockEventPacket sent = new ClientboundBlockEventPacket(event.pos(), event.block(), event.paramA(), event.paramB());
        //? if >=1.20.5 {
        RegistryFriendlyByteBuf bytes = new RegistryFriendlyByteBuf(Unpooled.buffer(), helper.getLevel().registryAccess());
        ClientboundBlockEventPacket.STREAM_CODEC.encode(bytes, sent);
        return ClientboundBlockEventPacket.STREAM_CODEC.decode(bytes);
        //?} else {
        /*FriendlyByteBuf bytes = new FriendlyByteBuf(Unpooled.buffer());
        sent.write(bytes);
        return new ClientboundBlockEventPacket(bytes);
        *///?}
    }
}
