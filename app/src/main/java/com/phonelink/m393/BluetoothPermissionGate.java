package com.phonelink.m393;

/**
 * Keeps Android 12+ Bluetooth permissions scoped to the operation that needs them.
 * CONNECT is enough for paired-device diagnostics; SCAN is additionally required
 * only for active discovery.
 */
final class BluetoothPermissionGate {
    enum Access {
        CONNECT_REQUIRED,
        DIAGNOSTICS_ONLY,
        DISCOVERY_READY
    }

    private BluetoothPermissionGate() {}

    static Access evaluate(boolean connectGranted, boolean scanGranted) {
        if (!connectGranted) return Access.CONNECT_REQUIRED;
        return scanGranted ? Access.DISCOVERY_READY : Access.DIAGNOSTICS_ONLY;
    }

    static boolean canRunDiagnostics(Access access) {
        return access != Access.CONNECT_REQUIRED;
    }

    static boolean canDiscover(Access access) {
        return access == Access.DISCOVERY_READY;
    }
}
