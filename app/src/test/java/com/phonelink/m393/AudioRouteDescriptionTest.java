package com.phonelink.m393;

import org.junit.Test;
import static org.junit.Assert.*;

public class AudioRouteDescriptionTest {
    @Test public void describesBluetoothScoAsAnInputAndOutputDevice() {
        assertEquals("אוזניות · Bluetooth SCO/HFP · כניסה ויציאה",
            AudioRouteDescription.describe("אוזניות", "Bluetooth SCO/HFP", true, true));
    }

    @Test public void distinguishesInputFromOutput() {
        assertEquals("מיקרופון · Built-in mic · כניסה",
            AudioRouteDescription.describe("מיקרופון", "Built-in mic", true, false));
        assertEquals("רמקול · Built-in speaker · יציאה",
            AudioRouteDescription.describe("רמקול", "Built-in speaker", false, true));
    }

    @Test public void handlesMissingNamesAndMissingRoute() {
        assertEquals("התקן ללא שם · סוג שמע לא ידוע · כיוון לא דווח",
            AudioRouteDescription.describe(" ", null, false, false));
        assertTrue(AudioRouteDescription.unavailable("כניסה").contains("לא דיווח"));
    }
}
