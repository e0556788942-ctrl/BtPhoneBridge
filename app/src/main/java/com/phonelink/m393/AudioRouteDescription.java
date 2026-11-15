package com.phonelink.m393;

/** Pure formatter for the actual AudioTrack/AudioRecord routed-device snapshot. */
final class AudioRouteDescription {
    private AudioRouteDescription() {}

    static String describe(String productName, String typeLabel, boolean isSource, boolean isSink) {
        String name = productName == null || productName.trim().isEmpty()
            ? "התקן ללא שם" : productName.trim();
        String type = typeLabel == null || typeLabel.trim().isEmpty()
            ? "סוג שמע לא ידוע" : typeLabel.trim();
        String direction;
        if (isSource && isSink) direction = "כניסה ויציאה";
        else if (isSource) direction = "כניסה";
        else if (isSink) direction = "יציאה";
        else direction = "כיוון לא דווח";
        return name + " · " + type + " · " + direction;
    }

    static String unavailable(String direction) {
        String label = direction == null || direction.trim().isEmpty()
            ? "שמע" : direction.trim();
        return "Android לא דיווח על התקן " + label + " מנותב בזמן הדגימה.";
    }
}
