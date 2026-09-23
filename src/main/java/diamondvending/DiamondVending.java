package diamondvending;

import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Shared constants and setup used by both loaders. */
public final class DiamondVending {
    public static final String MOD_ID = "diamondvending";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    public static final String VERSION = /*$ mod_version*/ "0.1.0";
    public static final String MINECRAFT = /*$ minecraft*/ "26.1.2";

    private DiamondVending() {}

    /** An id in this mod's namespace, e.g. {@code id("vending_machine")}. */
    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }

    /** Common setup, called once by each loader's entrypoint. */
    public static void init() {
        LOGGER.info("Diamond Vending {} loaded for Minecraft {}", VERSION, MINECRAFT);
    }
}
