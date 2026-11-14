package com.phonelink.m393;

import android.bluetooth.BluetoothHeadset;
import android.bluetooth.BluetoothProfile;

/** Human-readable labels for public Bluetooth profile and HFP audio states. */
final class BluetoothProfileState {
    private BluetoothProfileState() {}

    static String connection(int state) {
        switch (state) {
            case BluetoothProfile.STATE_CONNECTED: return "מחובר";
            case BluetoothProfile.STATE_CONNECTING: return "מתחבר";
            case BluetoothProfile.STATE_DISCONNECTED: return "מנותק";
            case BluetoothProfile.STATE_DISCONNECTING: return "מתנתק";
            default: return "מצב לא מוכר (" + state + ")";
        }
    }

    static String hfpAudio(int state) {
        switch (state) {
            case BluetoothHeadset.STATE_AUDIO_CONNECTED: return "SCO מחובר";
            case BluetoothHeadset.STATE_AUDIO_CONNECTING: return "SCO מתחבר";
            case BluetoothHeadset.STATE_AUDIO_DISCONNECTED: return "SCO מנותק";
            default: return "מצב שמע לא מוכר (" + state + ")";
        }
    }
}
