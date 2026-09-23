package diamondvending.core;

/** The outcome of a purchase attempt. */
public sealed interface PurchaseDecision {
    /** The purchase goes ahead: take {@code fromCredit} from the buyer's credit, then {@code fromInventory} from their inventory. */
    record Approved(int fromCredit, int fromInventory) implements PurchaseDecision {}

    /** The purchase is refused and nothing changes. */
    record Denied(DenyReason reason) implements PurchaseDecision {}
}
