package diamondvending.core;

/**
 * Everything the purchase decision needs, already measured by the caller.
 *
 * @param quantity          items handed out per purchase
 * @param stockCount        matching items in stock (ignored for infinite machines)
 * @param cashBoxHasRoom    whether the cash box can take {@code price} currency items (ignored for infinite machines)
 * @param credit            the buyer's credit on this machine, in currency items
 * @param inventoryCurrency currency items in the buyer's inventory, hotbar and offhand
 */
public record PurchaseInput(
        boolean catalogMissing,
        boolean selectionSetUp,
        boolean infinite,
        int quantity,
        int stockCount,
        boolean trayHasRoom,
        boolean cashBoxHasRoom,
        int price,
        int credit,
        int inventoryCurrency) {

    public PurchaseInput {
        if (quantity < 0 || stockCount < 0 || price < 0 || credit < 0 || inventoryCurrency < 0) {
            throw new IllegalArgumentException("counts must not be negative (quantity=" + quantity + ", stock=" + stockCount
                    + ", price=" + price + ", credit=" + credit + ", inventory=" + inventoryCurrency + ")");
        }
    }
}
