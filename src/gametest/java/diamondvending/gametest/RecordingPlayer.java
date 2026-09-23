package diamondvending.gametest;

import com.mojang.authlib.GameProfile;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
//? if <26.1 {
/*import net.minecraft.core.BlockPos;
*///?}

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * A mock player like {@link GameTestHelper#makeMockPlayer} that also remembers the action-bar messages it's sent, so
 * tests can check exactly what a player was told. Not added to the world.
 */
public final class RecordingPlayer extends Player {
    private final GameType gameType;
    private final List<Component> messages = new ArrayList<>();

    public RecordingPlayer(GameTestHelper helper, GameType gameType) {
        this(helper, gameType, UUID.randomUUID(), "test-player");
    }

    public RecordingPlayer(GameTestHelper helper, GameType gameType, UUID id, String name) {
        //? if >=26.1 {
        super(helper.getLevel(), new GameProfile(id, name));
        //?} else {
        /*super(helper.getLevel(), BlockPos.ZERO, 0.0F, new GameProfile(id, name));
        *///?}
        this.gameType = gameType;
    }

    /** Every action-bar message so far, oldest first. */
    public List<Component> messages() {
        return messages;
    }

    /** The latest action-bar message, or null if there was none. */
    public Component lastMessage() {
        return messages.isEmpty() ? null : messages.getLast();
    }

    // gameType is still null while the Player constructor runs, so these must cope with that.
    //? if >=26.1 {
    @Override
    public GameType gameMode() {
        return gameType;
    }

    @Override
    public boolean isClientAuthoritative() {
        return false;
    }

    @Override
    public void sendOverlayMessage(Component message) {
        messages.add(message);
    }
    //?} else {
    /*@Override
    public boolean isSpectator() {
        return gameType == GameType.SPECTATOR;
    }

    @Override
    public boolean isCreative() {
        return gameType != null && gameType.isCreative();
    }

    @Override
    public boolean isLocalPlayer() {
        return true;
    }

    @Override
    public void displayClientMessage(Component message, boolean actionBar) {
        if (actionBar) messages.add(message);
    }
    *///?}
}
