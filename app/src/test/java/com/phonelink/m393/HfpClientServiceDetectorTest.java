package com.phonelink.m393;

import org.junit.Test;
import static org.junit.Assert.*;

public class HfpClientServiceDetectorTest {
    @Test public void detectsAospServiceClassAndPackageNames() {
        assertTrue(HfpClientServiceDetector.matches("com.android.bluetooth.hfpclient.HeadsetClientService"));
        assertTrue(HfpClientServiceDetector.matches("vendor.bluetooth.HeadsetClientService"));
    }

    @Test public void ignoresUnrelatedAndNullServices() {
        assertFalse(HfpClientServiceDetector.matches("com.android.bluetooth.hfp.HeadsetService"));
        assertFalse(HfpClientServiceDetector.matches("com.android.bluetooth.a2dp.A2dpService"));
        assertFalse(HfpClientServiceDetector.matches(null));
    }
}
