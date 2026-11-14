package com.phonelink.m393;

import android.bluetooth.BluetoothHeadset;
import android.bluetooth.BluetoothProfile;
import org.junit.Test;
import static org.junit.Assert.*;

public class BluetoothProfileStateTest {
    @Test public void labelsAllPublicProfileConnectionStates() {
        assertEquals("מחובר", BluetoothProfileState.connection(BluetoothProfile.STATE_CONNECTED));
        assertEquals("מתחבר", BluetoothProfileState.connection(BluetoothProfile.STATE_CONNECTING));
        assertEquals("מנותק", BluetoothProfileState.connection(BluetoothProfile.STATE_DISCONNECTED));
        assertEquals("מתנתק", BluetoothProfileState.connection(BluetoothProfile.STATE_DISCONNECTING));
    }

    @Test public void labelsHfpScoAudioStates() {
        assertEquals("SCO מחובר", BluetoothProfileState.hfpAudio(BluetoothHeadset.STATE_AUDIO_CONNECTED));
        assertEquals("SCO מתחבר", BluetoothProfileState.hfpAudio(BluetoothHeadset.STATE_AUDIO_CONNECTING));
        assertEquals("SCO מנותק", BluetoothProfileState.hfpAudio(BluetoothHeadset.STATE_AUDIO_DISCONNECTED));
    }

    @Test public void unknownStatesAreNotMisreportedAsConnected() {
        assertTrue(BluetoothProfileState.connection(999).contains("לא מוכר"));
        assertTrue(BluetoothProfileState.hfpAudio(999).contains("לא מוכר"));
    }
}
