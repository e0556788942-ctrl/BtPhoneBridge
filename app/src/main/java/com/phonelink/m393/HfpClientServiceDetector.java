package com.phonelink.m393;

import java.util.Locale;

/** Name-only matcher for known AOSP HFP Client service components. */
final class HfpClientServiceDetector {
    private HfpClientServiceDetector() {}

    static boolean matches(String serviceName) {
        if (serviceName == null) return false;
        String value = serviceName.toLowerCase(Locale.US);
        return value.contains("hfpclient") || value.contains("headsetclient");
    }
}
