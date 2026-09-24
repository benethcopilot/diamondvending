package diamondvending.shop;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;

/** The machine's sounds (spec §7): all vanilla for v1. */
public final class MachineSounds {
    private MachineSounds() {}

    public static void button(Level level, BlockPos pos) {
        play(level, pos, SoundEvents.STONE_BUTTON_CLICK_ON, 0.6F, 1.0F);
    }

    public static void credit(Level level, BlockPos pos) {
        play(level, pos, SoundEvents.CHAIN_PLACE, 1.0F, 1.2F);
    }

    public static void vend(Level level, BlockPos pos) {
        play(level, pos, SoundEvents.DISPENSER_DISPENSE, 1.0F, 1.0F);
    }

    public static void thankYou(Level level, BlockPos pos) {
        play(level, pos, SoundEvents.EXPERIENCE_ORB_PICKUP, 0.25F, 1.0F);
    }

    public static void error(Level level, BlockPos pos) {
        play(level, pos, SoundEvents.NOTE_BLOCK_BASS.value(), 1.0F, 0.5F);
    }

    /** Coin return and taking items from the tray. */
    public static void pickup(Level level, BlockPos pos) {
        play(level, pos, SoundEvents.ITEM_PICKUP, 0.4F, 1.0F);
    }

    private static void play(Level level, BlockPos pos, SoundEvent sound, float volume, float pitch) {
        level.playSound(null, pos, sound, SoundSource.BLOCKS, volume, pitch);
    }
}
