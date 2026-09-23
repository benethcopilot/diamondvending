package diamondvending.block;

import net.minecraft.util.StringRepresentable;

/** Which column a part is in, as seen by someone facing the machine's front. */
public enum MachineSide implements StringRepresentable {
    LEFT("left"), RIGHT("right");

    private final String name;

    MachineSide(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return name;
    }
}
