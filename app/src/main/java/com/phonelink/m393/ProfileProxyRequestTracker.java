package com.phonelink.m393;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Tracks only profile-proxy requests that Android accepted.
 * A rejected request must not leave the diagnostic screen permanently busy.
 */
final class ProfileProxyRequestTracker {
    // Public BluetoothProfile IDs: HEADSET=1, A2DP=2.
    static final int HEADSET = 1;
    static final int A2DP = 2;

    private final Set<Integer> pending = new HashSet<>();

    void begin(boolean headsetPending, boolean a2dpPending) {
        pending.clear();
        if (headsetPending) pending.add(HEADSET);
        if (a2dpPending) pending.add(A2DP);
    }

    void complete(int profile) {
        pending.remove(profile);
    }

    boolean hasPending() {
        return !pending.isEmpty();
    }

    Set<Integer> pendingProfiles() {
        return Collections.unmodifiableSet(new HashSet<>(pending));
    }

    void reset() {
        pending.clear();
    }
}
