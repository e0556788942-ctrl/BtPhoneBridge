package com.phonelink.m393;

import org.junit.Test;
import static org.junit.Assert.*;

public class BluetoothServiceUuidTest {
    @Test public void identifiesHfpRoles() {
        assertTrue(BluetoothServiceUuid.isHfpAudioGateway("111F"));
        assertTrue(BluetoothServiceUuid.isHfpHandsFree("0000111e-0000-1000-8000-00805f9b34fb"));
        assertFalse(BluetoothServiceUuid.isHfpAudioGateway("111E"));
    }

    @Test public void identifiesHspRoles() {
        assertTrue(BluetoothServiceUuid.isHspAudioGateway("0x1112"));
        assertTrue(BluetoothServiceUuid.isHspHeadset("1131"));
        assertTrue(BluetoothServiceUuid.isHspHeadset("1108"));
        assertFalse(BluetoothServiceUuid.isHspAudioGateway("1131"));
    }

    @Test public void distinguishesA2dpSinkFromSource() {
        assertTrue(BluetoothServiceUuid.isA2dpSink("110B"));
        assertFalse(BluetoothServiceUuid.isA2dpSink("110A"));
        assertEquals("A2DP Audio Source (מקור מדיה)", BluetoothServiceUuid.label("110A"));
    }

    @Test public void normalizesShortAndFullUuidsCaseInsensitively() {
        assertEquals(BluetoothServiceUuid.normalize("111F"),
            BluetoothServiceUuid.normalize("0000111F-0000-1000-8000-00805F9B34FB"));
        assertEquals("שירות Bluetooth אחר", BluetoothServiceUuid.label(null));
        assertEquals("שירות Bluetooth אחר", BluetoothServiceUuid.label("abcd"));
    }
}
