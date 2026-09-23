package diamondvending.core;

/** What a click on the front canvas hit. {@code button} is 0–11 for {@link Region#BUTTON}, otherwise -1. */
public record Hit(Region region, int button) {
    public static final Hit NONE = new Hit(Region.NONE, -1);

    public static Hit of(Region region) {
        return new Hit(region, -1);
    }

    public static Hit button(int index) {
        return new Hit(Region.BUTTON, index);
    }
}
