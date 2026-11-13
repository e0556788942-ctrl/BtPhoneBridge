package com.phonelink.m393;

import java.util.Locale;

/** Pure SDP service-class UUID classifier. It does not infer an active connection. */
final class BluetoothServiceUuid {
    private static final String BASE = "-0000-1000-8000-00805f9b34fb";

    private BluetoothServiceUuid() {}

    static String normalize(String raw) {
        if (raw == null) return "";
        String value = raw.trim().toLowerCase(Locale.US);
        if (value.startsWith("0x")) value = value.substring(2);
        if (value.length() == 4) return "0000" + value + BASE;
        return value;
    }

    static String label(String raw) {
        String id = normalize(raw);
        switch (id) {
            case "0000111f" + BASE: return "HFP Audio Gateway (תפקיד טלפון/AG)";
            case "0000111e" + BASE: return "HFP Hands-Free (תפקיד דיבורית/HF)";
            case "00001112" + BASE: return "HSP Audio Gateway (AG)";
            case "00001131" + BASE: return "HSP Headset - HS (תפקיד אוזנייה)";
            case "00001108" + BASE: return "Headset service class (0x1108; אינו מזהה לבדו תפקיד HS מדויק)";
            case "0000110b" + BASE: return "A2DP Audio Sink (יעד מדיה, לרוב אוזניות)";
            case "0000110a" + BASE: return "A2DP Audio Source (מקור מדיה)";
            default: return "שירות Bluetooth אחר";
        }
    }

    static boolean isHfpAudioGateway(String raw) { return normalize(raw).equals("0000111f" + BASE); }
    static boolean isHfpHandsFree(String raw) { return normalize(raw).equals("0000111e" + BASE); }
    static boolean isHspAudioGateway(String raw) { return normalize(raw).equals("00001112" + BASE); }
    static boolean isHspHeadset(String raw) { return normalize(raw).equals("00001131" + BASE); }
    static boolean isHeadsetServiceClass(String raw) { return normalize(raw).equals("00001108" + BASE); }
    static boolean isA2dpSink(String raw) { return normalize(raw).equals("0000110b" + BASE); }
}
