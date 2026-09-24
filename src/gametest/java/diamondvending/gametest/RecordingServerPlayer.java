package diamondvending.gametest;

import com.mojang.authlib.GameProfile;
import io.netty.channel.embedded.EmbeddedChannel;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.GameType;

import java.util.ArrayList;
import java.util.List;
import java.util.OptionalInt;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * A real server player — menus open for it, and its clicks can go through the game's own click handling, loader events
 * included — that also remembers its action-bar messages. Made like {@link GameTestHelper#makeMockServerPlayerInLevel},
 * but in any game mode, standing two blocks in front of the test machine and facing south like the mock players (so a
 * machine it places faces north).
 */
public final class RecordingServerPlayer extends ServerPlayer {
    private final List<Component> messages = new ArrayList<>();

    private RecordingServerPlayer(MinecraftServer server, ServerLevel level, GameProfile profile, ClientInformation information) {
        super(server, level, profile, information);
    }

    public static RecordingServerPlayer create(GameTestHelper helper, GameType gameType) {
        ServerLevel level = helper.getLevel();
        CommonListenerCookie cookie = CommonListenerCookie.createInitial(new GameProfile(UUID.randomUUID(), "test-player"), false);
        RecordingServerPlayer player = new RecordingServerPlayer(level.getServer(), level, cookie.gameProfile(), cookie.clientInformation());
        // Spelled out: inside a ServerPlayer, "Connection" means a waypoint type it inherits (26.1).
        net.minecraft.network.Connection connection = new net.minecraft.network.Connection(PacketFlow.SERVERBOUND);
        new EmbeddedChannel(connection);
        level.getServer().getPlayerList().placeNewPlayer(connection, player, cookie);
        player.setGameMode(gameType);
        BlockPos spot = helper.absolutePos(MachineTests.MASTER.north(2));
        player.teleportTo(spot.getX() + 0.5, spot.getY(), spot.getZ() + 0.5);
        player.setYRot(0.0F);
        return player;
    }

    /** The latest action-bar message, or null if there was none. */
    public Component lastMessage() {
        return messages.isEmpty() ? null : messages.getLast();
    }

    // The test connection never agreed on the loaders' own packets, so NeoForge refuses to send its "open screen with
    // extra data" packet over it. Menus open here the way the server opens them, minus the packet; the real packet path
    // is covered by the client screenshot test.
    @Override
    public OptionalInt openMenu(MenuProvider provider) {
        return openWithoutPacket(provider);
    }

    // NeoForge's two-argument openMenu (an override there; on Fabric, where it doesn't exist, just an unused method).
    public OptionalInt openMenu(MenuProvider provider, Consumer<RegistryFriendlyByteBuf> extraData) {
        return openWithoutPacket(provider);
    }

    private OptionalInt openWithoutPacket(MenuProvider provider) {
        AbstractContainerMenu menu = provider.createMenu(1, getInventory(), this);
        if (menu == null) return OptionalInt.empty();
        containerMenu = menu;
        return OptionalInt.of(menu.containerId);
    }

    //? if >=26.1 {
    @Override
    public void sendOverlayMessage(Component message) {
        messages.add(message);
        super.sendOverlayMessage(message);
    }
    //?} else {
    /*@Override
    public void displayClientMessage(Component message, boolean actionBar) {
        if (actionBar) messages.add(message);
        super.displayClientMessage(message, actionBar);
    }
    *///?}
}
