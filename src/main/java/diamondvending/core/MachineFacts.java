package diamondvending.core;

/**
 * Measured facts about a machine, used to work out its problems.
 *
 * @param selectionsSetUp     selections that have an item (0–12)
 * @param selectionsInStock   set-up selections with enough stock for one purchase (owned machines)
 * @param cashBoxHasEmptySlot whether the cash box has at least one empty slot (owned machines)
 * @param trayHasEmptySlot    whether the tray has at least one empty slot
 */
public record MachineFacts(
        boolean infinite,
        boolean catalogMissing,
        int selectionsSetUp,
        int selectionsInStock,
        boolean cashBoxHasEmptySlot,
        boolean trayHasEmptySlot) {

    public MachineFacts {
        if (selectionsSetUp < 0 || selectionsSetUp > MachineLayout.SELECTIONS
                || selectionsInStock < 0 || selectionsInStock > selectionsSetUp) {
            throw new IllegalArgumentException("impossible selection counts: setUp=" + selectionsSetUp
                    + ", inStock=" + selectionsInStock);
        }
    }
}
