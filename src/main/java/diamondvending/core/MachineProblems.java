package diamondvending.core;

import java.util.ArrayList;
import java.util.List;

/** Works out which persistent problems a machine has (spec §3.5 b). */
public final class MachineProblems {
    private MachineProblems() {}

    /** Active problems in priority order; empty when the machine is healthy. */
    public static List<Problem> of(MachineFacts f) {
        List<Problem> problems = new ArrayList<>();
        if (f.catalogMissing()) {
            problems.add(Problem.CATALOG_MISSING);
        } else if (f.selectionsSetUp() == 0) {
            problems.add(Problem.NOT_SET_UP);
        }
        if (!f.infinite() && !f.cashBoxHasEmptySlot()) {
            problems.add(Problem.CASH_BOX_FULL);
        }
        if (!f.infinite() && !f.catalogMissing() && f.selectionsSetUp() > 0 && f.selectionsInStock() == 0) {
            problems.add(Problem.SOLD_OUT);
        }
        if (!f.trayHasEmptySlot()) {
            problems.add(Problem.TRAY_FULL);
        }
        return List.copyOf(problems);
    }
}
