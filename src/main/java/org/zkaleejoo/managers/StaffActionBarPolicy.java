package org.zkaleejoo.managers;

final class StaffActionBarPolicy {

    enum ActionBarType {
        STAFF_MODE,
        VANISH,
        NONE
    }

    private StaffActionBarPolicy() {
    }

    static ActionBarType select(boolean staffMode, boolean vanished) {
        if (staffMode) {
            return ActionBarType.STAFF_MODE;
        }
        if (vanished) {
            return ActionBarType.VANISH;
        }
        return ActionBarType.NONE;
    }
}
