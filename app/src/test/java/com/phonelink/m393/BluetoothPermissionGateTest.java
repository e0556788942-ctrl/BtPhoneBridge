package com.phonelink.m393;

import org.junit.Test;
import static org.junit.Assert.*;

public class BluetoothPermissionGateTest {
    @Test public void connectAloneAllowsPairedDeviceDiagnostics() {
        BluetoothPermissionGate.Access access = BluetoothPermissionGate.evaluate(true, false);
        assertEquals(BluetoothPermissionGate.Access.DIAGNOSTICS_ONLY, access);
        assertTrue(BluetoothPermissionGate.canRunDiagnostics(access));
        assertFalse(BluetoothPermissionGate.canDiscover(access));
    }

    @Test public void connectAndScanAllowDiscovery() {
        BluetoothPermissionGate.Access access = BluetoothPermissionGate.evaluate(true, true);
        assertEquals(BluetoothPermissionGate.Access.DISCOVERY_READY, access);
        assertTrue(BluetoothPermissionGate.canRunDiagnostics(access));
        assertTrue(BluetoothPermissionGate.canDiscover(access));
    }

    @Test public void scanWithoutConnectDoesNotAllowEitherOperation() {
        BluetoothPermissionGate.Access access = BluetoothPermissionGate.evaluate(false, true);
        assertEquals(BluetoothPermissionGate.Access.CONNECT_REQUIRED, access);
        assertFalse(BluetoothPermissionGate.canRunDiagnostics(access));
        assertFalse(BluetoothPermissionGate.canDiscover(access));
    }

    @Test public void noPermissionsRequiresConnectFirst() {
        assertEquals(BluetoothPermissionGate.Access.CONNECT_REQUIRED,
            BluetoothPermissionGate.evaluate(false, false));
    }
}
