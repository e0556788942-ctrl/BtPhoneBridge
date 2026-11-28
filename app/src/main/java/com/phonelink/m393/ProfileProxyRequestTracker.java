package com.phonelink.m393;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Tracks accepted profile-proxy requests and rejects callbacks from timed-out
 * or otherwise superseded request batches.
 */
final class ProfileProxyRequestTracker {
    // Public BluetoothProfile IDs: HEADSET=1, A2DP=2.
    static final int HEADSET = 1;
    static final int A2DP = 2;

    private final Set<Integer> pending = new HashSet<>();
    private long generation;

    long begin(boolean headsetPending, boolean a2dpPending) {
        generation++;
        pending.clear();
        if (headsetPending) pending.add(HEADSET);
        if (a2dpPending) pending.add(A2DP);
        return generation;
    }

    boolean isCurrent(long requestGeneration) {
        return generation == requestGeneration;
    }

    boolean complete(long requestGeneration, int profile) {
        if (!isCurrent(requestGeneration)) return false;
        return pending.remove(profile);
    }

    boolean hasPending() {
        return !pending.isEmpty();
    }

    Set<Integer> pendingProfiles() {
        return Collections.unmodifiableSet(new HashSet<>(pending));
    }

    /**
     * Reset only the batch that owns the supplied generation. A stale timeout
     * must never clear a newer request batch.
     */
    boolean reset(long requestGeneration) {
        if (!isCurrent(requestGeneration)) return false;
        reset();
        return true;
    }

    /** Invalidates every callback captured by the previous generation. */
    void reset() {
        generation++;
        pending.clear();
    }
}
