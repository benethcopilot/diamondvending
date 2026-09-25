package diamondvending.gametest;

import diamondvending.DiamondVending;
import diamondvending.block.MachineItems;
import diamondvending.block.MachinePart;
import diamondvending.block.VendingMachineBlock;
import diamondvending.block.VendingMachineBlockEntity;
import diamondvending.core.Texts;
import diamondvending.registry.ModContent;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.WrittenBookContent;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.WallTorchBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
//? if >=26.1 {
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
//?}

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * The in-game tests, shared by every loader and version. Each test also needs a method in
 * {@code fabric/FabricGameTests} and (for 1.21.1) {@code neoforge/NeoForgeGameTests}; NeoForge 26.1 registers {@link #ALL}.
 *
 * <p>Mock players have yaw 0 (they face south), so a machine they place faces north and its right-hand column is at x − 1.
 */
public final class MachineTests {
    public static final String STRUCTURE_NAME = "gametest_platform";
    public static final String STRUCTURE = DiamondVending.MOD_ID + ":" + STRUCTURE_NAME;
    public static final int MAX_TICKS = 100;

    /** Every test by snake_case name. */
    public static final Map<String, Consumer<GameTestHelper>> ALL = Map.ofEntries(
            Map.entry("platform_is_ready", MachineTests::platformIsReady),
            Map.entry("places_all_four_parts", MachineTests::placesAllFourParts),
            Map.entry("placement_needs_room", MachineTests::placementNeedsRoom),
            Map.entry("places_in_the_items_color", MachineTests::placesInTheItemsColor),
            Map.entry("works_in_every_facing", MachineTests::worksInEveryFacing),
            Map.entry("breaking_any_part_removes_the_machine_and_drops_it", MachineTests::breakingAnyPartRemovesTheMachineAndDropsIt),
            Map.entry("creative_breaking_drops_nothing", MachineTests::creativeBreakingDropsNothing),
            Map.entry("only_owners_and_admins_can_mine_it", MachineTests::onlyOwnersAndAdminsCanMineIt),
            Map.entry("ownerless_machines_are_admin_only", MachineTests::ownerlessMachinesAreAdminOnly),
            Map.entry("a_broken_machine_cleans_itself_up", MachineTests::aBrokenMachineCleansItselfUp),
            Map.entry("neighbouring_machines_stay_separate", MachineTests::neighbouringMachinesStaySeparate),
            Map.entry("owners_can_dye_the_whole_machine", MachineTests::ownersCanDyeTheWholeMachine),
            Map.entry("strangers_cannot_dye_it", MachineTests::strangersCannotDyeIt),
            Map.entry("dyeing_the_same_color_uses_no_dye", MachineTests::dyeingTheSameColorUsesNoDye),
            Map.entry("creative_dyeing_keeps_the_dye", MachineTests::creativeDyeingKeepsTheDye),
            Map.entry("machine_recipe_loads", MachineTests::machineRecipeLoads),
            Map.entry("the_manual_recipe_makes_the_manual", MachineTests::theManualRecipeMakesTheManual),
            Map.entry("a_first_diamond_unlocks_the_machine_and_the_manual", MachineTests::aFirstDiamondUnlocksTheMachineAndTheManual),
            Map.entry("attached_blocks_fall_when_the_machine_is_broken", MachineTests::attachedBlocksFallWhenTheMachineIsBroken),
            Map.entry("cannot_be_placed_inside_creatures", MachineTests::cannotBePlacedInsideCreatures));

    static final BlockPos FLOOR = platform(3, 0, 3);
    static final BlockPos MASTER = FLOOR.above();
    static final BlockPos LOWER_RIGHT = MASTER.west();
    static final BlockPos UPPER_LEFT = MASTER.above();
    static final BlockPos UPPER_RIGHT = LOWER_RIGHT.above();

    private MachineTests() {}

    /**
     * Converts platform coordinates (floor at y = 0) to test-relative ones. 1.21.1 measures from the test's
     * structure block, which sits one block below the structure; 26.1 measures from the structure itself.
     */
    static BlockPos platform(int x, int y, int z) {
        //? if >=26.1 {
        return new BlockPos(x, y, z);
        //?} else {
        /*return new BlockPos(x, y + 1, z);
        *///?}
    }

    // ---- helpers -------------------------------------------------------------------------------------------------

    static ItemStack machineItem(int count) {
        return new ItemStack(ModContent.VENDING_MACHINE_ITEM.get(), count);
    }

    /**
     * Uses the stack on the top of {@code floor}, as a player would. The stack goes in the player's main hand first:
     * placement reads (and uses up) the item in hand, not the stack passed to {@code placeAt}.
     */
    static void placeOn(GameTestHelper helper, Player player, ItemStack stack, BlockPos floor) {
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        helper.placeAt(player, stack, floor, Direction.UP);
    }

    /** Breaks a block the way the server does for a player: playerWillDestroy, then remove. */
    static void breakAsPlayer(GameTestHelper helper, BlockPos relative, Player player) {
        ServerLevel level = helper.getLevel();
        BlockPos pos = helper.absolutePos(relative);
        BlockState state = level.getBlockState(pos);
        state.getBlock().playerWillDestroy(level, pos, state, player);
        level.removeBlock(pos, false);
    }

    static List<ItemEntity> droppedMachines(GameTestHelper helper) {
        AABB area = new AABB(helper.absolutePos(MASTER)).inflate(3);
        return helper.getLevel().getEntitiesOfClass(ItemEntity.class, area,
                entity -> entity.getItem().is(ModContent.VENDING_MACHINE_ITEM.get()));
    }

    static VendingMachineBlockEntity machineAt(GameTestHelper helper, BlockPos relative) {
        return helper.getLevel().getBlockEntity(helper.absolutePos(relative)) instanceof VendingMachineBlockEntity machine ? machine : null;
    }

    static void assertPart(GameTestHelper helper, BlockPos relative, MachinePart part, Direction facing, DyeColor color) {
        BlockState state = helper.getBlockState(relative);
        helper.assertTrue(state.is(ModContent.VENDING_MACHINE.get()), "expected a vending machine at " + relative + " but found " + state);
        helper.assertTrue(MachinePart.of(state) == part, "expected " + part + " at " + relative + " but found " + MachinePart.of(state));
        helper.assertTrue(state.getValue(VendingMachineBlock.FACING) == facing, "expected facing " + facing + " at " + relative);
        helper.assertTrue(state.getValue(VendingMachineBlock.COLOR) == color, "expected " + color + " at " + relative);
    }

    static void assertWholeMachine(GameTestHelper helper, DyeColor color) {
        assertPart(helper, MASTER, MachinePart.LOWER_LEFT, Direction.NORTH, color);
        assertPart(helper, LOWER_RIGHT, MachinePart.LOWER_RIGHT, Direction.NORTH, color);
        assertPart(helper, UPPER_LEFT, MachinePart.UPPER_LEFT, Direction.NORTH, color);
        assertPart(helper, UPPER_RIGHT, MachinePart.UPPER_RIGHT, Direction.NORTH, color);
    }

    static void assertAir(GameTestHelper helper, BlockPos... relatives) {
        for (BlockPos relative : relatives) {
            BlockState state = helper.getBlockState(relative);
            helper.assertTrue(state.isAir(), "expected air at " + relative + " but found " + state);
        }
    }

    // ---- tests ---------------------------------------------------------------------------------------------------

    /** Smoke test: the generated platform loaded — floor at y = 0, air above. */
    public static void platformIsReady(GameTestHelper helper) {
        helper.assertBlockPresent(Blocks.POLISHED_ANDESITE, platform(0, 0, 0));
        helper.assertBlockPresent(Blocks.AIR, platform(3, 1, 3));
        helper.succeed();
    }

    public static void placesAllFourParts(GameTestHelper helper) {
        Player owner = helper.makeMockPlayer(GameType.SURVIVAL);
        ItemStack stack = machineItem(2);
        placeOn(helper, owner, stack, FLOOR);
        assertWholeMachine(helper, DyeColor.RED);
        helper.assertTrue(stack.getCount() == 1, "placing should use one item, stack is now " + stack.getCount());
        VendingMachineBlockEntity machine = machineAt(helper, MASTER);
        helper.assertTrue(machine != null && owner.getUUID().equals(machine.getOwner()), "the placer should own the machine");
        helper.assertTrue(machineAt(helper, UPPER_RIGHT) == null, "only the master part has a block entity");
        helper.succeed();
    }

    public static void placementNeedsRoom(GameTestHelper helper) {
        helper.setBlock(UPPER_RIGHT, Blocks.STONE);
        Player owner = helper.makeMockPlayer(GameType.SURVIVAL);
        ItemStack stack = machineItem(1);
        placeOn(helper, owner, stack, FLOOR);
        assertAir(helper, MASTER, LOWER_RIGHT, UPPER_LEFT);
        helper.assertTrue(stack.getCount() == 1, "a blocked placement must not use the item");
        helper.succeed();
    }

    public static void placesInTheItemsColor(GameTestHelper helper) {
        placeOn(helper, helper.makeMockPlayer(GameType.SURVIVAL), MachineItems.forColor(DyeColor.BLUE), FLOOR);
        assertWholeMachine(helper, DyeColor.BLUE);
        helper.succeed();
    }

    /** Yaw → the player's facing → the machine faces back at them; its right column is on the viewer's right. */
    public static void worksInEveryFacing(GameTestHelper helper) {
        float[] yaws = {0, 90, 180, 270};
        Direction[] machineFacings = {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};
        BlockPos[] rightOffsets = {new BlockPos(-1, 0, 0), new BlockPos(0, 0, -1), new BlockPos(1, 0, 0), new BlockPos(0, 0, 1)};
        for (int i = 0; i < yaws.length; i++) {
            Player owner = helper.makeMockPlayer(GameType.SURVIVAL);
            owner.setYRot(yaws[i]);
            placeOn(helper, owner, machineItem(1), FLOOR);
            BlockPos right = MASTER.offset(rightOffsets[i]);
            assertPart(helper, MASTER, MachinePart.LOWER_LEFT, machineFacings[i], DyeColor.RED);
            assertPart(helper, right, MachinePart.LOWER_RIGHT, machineFacings[i], DyeColor.RED);
            assertPart(helper, MASTER.above(), MachinePart.UPPER_LEFT, machineFacings[i], DyeColor.RED);
            assertPart(helper, right.above(), MachinePart.UPPER_RIGHT, machineFacings[i], DyeColor.RED);
            breakAsPlayer(helper, right.above(), owner);
            assertAir(helper, MASTER, right, MASTER.above(), right.above());
        }
        helper.succeed();
    }

    public static void breakingAnyPartRemovesTheMachineAndDropsIt(GameTestHelper helper) {
        Player owner = helper.makeMockPlayer(GameType.SURVIVAL);
        placeOn(helper, owner, MachineItems.forColor(DyeColor.LIME), FLOOR);
        breakAsPlayer(helper, UPPER_RIGHT, owner);
        assertAir(helper, MASTER, LOWER_RIGHT, UPPER_LEFT, UPPER_RIGHT);
        List<ItemEntity> drops = droppedMachines(helper);
        helper.assertTrue(drops.size() == 1 && drops.getFirst().getItem().getCount() == 1,
                "expected exactly one machine item, found " + drops.size());
        helper.assertTrue(MachineItems.colorOf(drops.getFirst().getItem()) == DyeColor.LIME, "the dropped machine should stay lime");
        helper.succeed();
    }

    public static void creativeBreakingDropsNothing(GameTestHelper helper) {
        Player admin = helper.makeMockPlayer(GameType.CREATIVE);
        placeOn(helper, admin, machineItem(1), FLOOR);
        breakAsPlayer(helper, MASTER, admin);
        assertAir(helper, MASTER, LOWER_RIGHT, UPPER_LEFT, UPPER_RIGHT);
        helper.assertTrue(droppedMachines(helper).isEmpty(), "creative breaking must not drop the machine");
        helper.succeed();
    }

    public static void onlyOwnersAndAdminsCanMineIt(GameTestHelper helper) {
        Player owner = helper.makeMockPlayer(GameType.SURVIVAL);
        placeOn(helper, owner, machineItem(1), FLOOR);
        Player stranger = helper.makeMockPlayer(GameType.SURVIVAL);
        Player admin = helper.makeMockPlayer(GameType.CREATIVE);
        ServerLevel level = helper.getLevel();
        BlockPos pos = helper.absolutePos(UPPER_LEFT);
        BlockState state = level.getBlockState(pos);
        helper.assertTrue(state.getDestroyProgress(owner, level, pos) > 0, "the owner should be able to mine the machine");
        helper.assertTrue(state.getDestroyProgress(stranger, level, pos) == 0, "a stranger must not be able to mine the machine");
        helper.assertTrue(state.getDestroyProgress(admin, level, pos) > 0, "an admin should be able to mine the machine");
        helper.succeed();
    }

    /** A machine built without a player (e.g. /setblock) has no owner: only admins may mine it. */
    public static void ownerlessMachinesAreAdminOnly(GameTestHelper helper) {
        BlockState master = ModContent.VENDING_MACHINE.get().defaultBlockState();
        for (MachinePart part : MachinePart.values()) {
            helper.setBlock(part.posFrom(MASTER, Direction.NORTH), part.applyTo(master));
        }
        assertWholeMachine(helper, DyeColor.RED);
        ServerLevel level = helper.getLevel();
        BlockPos pos = helper.absolutePos(MASTER);
        BlockState state = level.getBlockState(pos);
        helper.assertTrue(state.getDestroyProgress(helper.makeMockPlayer(GameType.SURVIVAL), level, pos) == 0,
                "nobody but an admin may mine an ownerless machine");
        helper.assertTrue(state.getDestroyProgress(helper.makeMockPlayer(GameType.CREATIVE), level, pos) > 0,
                "an admin may mine an ownerless machine");
        helper.succeed();
    }

    /** Removing one part without a player (a world edit) must not leave ghost parts behind. */
    public static void aBrokenMachineCleansItselfUp(GameTestHelper helper) {
        placeOn(helper, helper.makeMockPlayer(GameType.SURVIVAL), machineItem(1), FLOOR);
        helper.setBlock(MASTER, Blocks.AIR);
        assertAir(helper, LOWER_RIGHT, UPPER_LEFT, UPPER_RIGHT);
        helper.succeed();
    }

    public static void neighbouringMachinesStaySeparate(GameTestHelper helper) {
        Player owner = helper.makeMockPlayer(GameType.SURVIVAL);
        placeOn(helper, owner, machineItem(1), FLOOR);                // occupies x = 3 and 2
        BlockPos otherFloor = FLOOR.east(2);
        placeOn(helper, owner, machineItem(1), otherFloor);           // occupies x = 5 and 4
        BlockPos otherMaster = otherFloor.above();
        breakAsPlayer(helper, MASTER, owner);
        assertAir(helper, MASTER, LOWER_RIGHT, UPPER_LEFT, UPPER_RIGHT);
        assertPart(helper, otherMaster, MachinePart.LOWER_LEFT, Direction.NORTH, DyeColor.RED);
        assertPart(helper, otherMaster.west(), MachinePart.LOWER_RIGHT, Direction.NORTH, DyeColor.RED);
        assertPart(helper, otherMaster.above(), MachinePart.UPPER_LEFT, Direction.NORTH, DyeColor.RED);
        assertPart(helper, otherMaster.west().above(), MachinePart.UPPER_RIGHT, Direction.NORTH, DyeColor.RED);
        helper.succeed();
    }

    public static void ownersCanDyeTheWholeMachine(GameTestHelper helper) {
        Player owner = helper.makeMockPlayer(GameType.SURVIVAL);
        placeOn(helper, owner, machineItem(1), FLOOR);
        ItemStack dye = new ItemStack(Items.BLUE_DYE, 2);
        owner.setItemInHand(InteractionHand.MAIN_HAND, dye);
        helper.useBlock(UPPER_RIGHT, owner);
        assertWholeMachine(helper, DyeColor.BLUE);
        helper.assertTrue(dye.getCount() == 1, "dyeing should use one dye, stack is now " + dye.getCount());
        helper.succeed();
    }

    public static void strangersCannotDyeIt(GameTestHelper helper) {
        placeOn(helper, helper.makeMockPlayer(GameType.SURVIVAL), machineItem(1), FLOOR);
        Player stranger = helper.makeMockPlayer(GameType.SURVIVAL);
        ItemStack dye = new ItemStack(Items.BLUE_DYE, 2);
        stranger.setItemInHand(InteractionHand.MAIN_HAND, dye);
        helper.useBlock(MASTER, stranger);
        assertWholeMachine(helper, DyeColor.RED);
        helper.assertTrue(dye.getCount() == 2, "a refused dye must not be used up");
        helper.succeed();
    }

    public static void dyeingTheSameColorUsesNoDye(GameTestHelper helper) {
        Player owner = helper.makeMockPlayer(GameType.SURVIVAL);
        placeOn(helper, owner, machineItem(1), FLOOR);
        ItemStack dye = new ItemStack(Items.RED_DYE, 2);
        owner.setItemInHand(InteractionHand.MAIN_HAND, dye);
        helper.useBlock(MASTER, owner);
        assertWholeMachine(helper, DyeColor.RED);
        helper.assertTrue(dye.getCount() == 2, "dyeing to the same color must not use a dye");
        helper.succeed();
    }

    public static void creativeDyeingKeepsTheDye(GameTestHelper helper) {
        Player admin = helper.makeMockPlayer(GameType.CREATIVE);
        placeOn(helper, admin, machineItem(1), FLOOR);
        ItemStack dye = new ItemStack(Items.GREEN_DYE, 1);
        admin.setItemInHand(InteractionHand.MAIN_HAND, dye);
        helper.useBlock(MASTER, admin);
        assertWholeMachine(helper, DyeColor.GREEN);
        helper.assertTrue(dye.getCount() == 1, "creative dyeing must not use up the dye");
        helper.succeed();
    }

    public static void machineRecipeLoads(GameTestHelper helper) {
        //? if >=26.1 {
        boolean found = helper.getLevel().recipeAccess()
                .byKey(ResourceKey.create(Registries.RECIPE, DiamondVending.id("vending_machine"))).isPresent();
        //?} else {
        /*boolean found = helper.getLevel().getRecipeManager().byKey(DiamondVending.id("vending_machine")).isPresent();
        *///?}
        helper.assertTrue(found, "the vending machine recipe did not load (check the log for recipe parse errors)");
        helper.succeed();
    }

    /** Spec §6.2: a book and a gold nugget craft the manual — a written book whose pages are the manual's lang keys. */
    public static void theManualRecipeMakesTheManual(GameTestHelper helper) {
        CraftingInput input = CraftingInput.of(2, 1, List.of(new ItemStack(Items.BOOK), new ItemStack(Items.GOLD_NUGGET)));
        //? if >=26.1 {
        Optional<RecipeHolder<CraftingRecipe>> recipe = helper.getLevel().recipeAccess().getRecipeFor(RecipeType.CRAFTING, input, helper.getLevel());
        ItemStack book = recipe.map(holder -> holder.value().assemble(input)).orElse(ItemStack.EMPTY);
        //?} else {
        /*Optional<RecipeHolder<CraftingRecipe>> recipe = helper.getLevel().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, helper.getLevel());
        ItemStack book = recipe.map(holder -> holder.value().assemble(input, helper.getLevel().registryAccess())).orElse(ItemStack.EMPTY);
        *///?}
        helper.assertTrue(book.is(Items.WRITTEN_BOOK), "a book and a gold nugget should craft the manual, got " + book);
        WrittenBookContent content = book.get(DataComponents.WRITTEN_BOOK_CONTENT);
        helper.assertTrue(content != null && content.title().raw().equals("Diamond Vending Manual")
                && content.author().equals("Diamond Vending HQ"), "the manual's title and author, got " + content);
        List<Component> pages = content.getPages(false);
        helper.assertTrue(pages.size() == Texts.MANUAL_PAGES.size(),
                "the manual should have " + Texts.MANUAL_PAGES.size() + " pages, it has " + pages.size());
        for (int i = 0; i < pages.size(); i++) {
            String page = Texts.MANUAL_PAGES.get(i);
            List<String> keys = translationKeys(pages.get(i));
            helper.assertTrue(keys.equals(List.of(Texts.manualTitle(page), Texts.manualText(page))),
                    "page " + (i + 1) + " should be the " + page + " page, it shows " + keys);
        }
        helper.succeed();
    }

    /** Every translation key in a text component, in reading order. */
    private static List<String> translationKeys(Component component) {
        List<String> keys = new ArrayList<>();
        if (component.getContents() instanceof TranslatableContents translatable) keys.add(translatable.getKey());
        for (Component sibling : component.getSiblings()) keys.addAll(translationKeys(sibling));
        return keys;
    }

    /** Spec §6.1–6.2: a player's first diamond puts both the machine and the manual in their recipe book. */
    public static void aFirstDiamondUnlocksTheMachineAndTheManual(GameTestHelper helper) {
        RecordingServerPlayer player = RecordingServerPlayer.create(helper, GameType.SURVIVAL);
        ItemStack diamond = new ItemStack(Items.DIAMOND);
        player.getInventory().add(diamond.copy());
        CriteriaTriggers.INVENTORY_CHANGED.trigger(player, player.getInventory(), diamond);
        for (String recipe : List.of("vending_machine", "manual")) {
            //? if >=26.1 {
            boolean unlocked = player.getRecipeBook().contains(ResourceKey.create(Registries.RECIPE, DiamondVending.id(recipe)));
            //?} else {
            /*boolean unlocked = player.getRecipeBook().contains(DiamondVending.id(recipe));
            *///?}
            helper.assertTrue(unlocked, "a first diamond should unlock the " + recipe + " recipe");
        }
        helper.succeed();
    }

    /** Blocks hanging on any part (signs, torches…) must pop off when the machine is broken, never float. */
    public static void attachedBlocksFallWhenTheMachineIsBroken(GameTestHelper helper) {
        Player owner = helper.makeMockPlayer(GameType.SURVIVAL);
        placeOn(helper, owner, machineItem(1), FLOOR);
        BlockPos torch = LOWER_RIGHT.west(); // hangs on the far side of the right-hand column
        helper.setBlock(torch, Blocks.WALL_TORCH.defaultBlockState().setValue(WallTorchBlock.FACING, Direction.WEST));
        breakAsPlayer(helper, MASTER, owner);
        assertAir(helper, MASTER, LOWER_RIGHT, UPPER_LEFT, UPPER_RIGHT, torch);
        helper.succeed();
    }

    /** A pet or villager where another part would go blocks placement — otherwise it would be entombed. */
    public static void cannotBePlacedInsideCreatures(GameTestHelper helper) {
        helper.spawnWithNoFreeWill(EntityType.PIG, LOWER_RIGHT);
        Player owner = helper.makeMockPlayer(GameType.SURVIVAL);
        ItemStack stack = machineItem(1);
        placeOn(helper, owner, stack, FLOOR);
        assertAir(helper, MASTER, UPPER_LEFT, UPPER_RIGHT);
        helper.assertTrue(stack.getCount() == 1, "a placement blocked by a creature must not use the item");
        helper.succeed();
    }
}
