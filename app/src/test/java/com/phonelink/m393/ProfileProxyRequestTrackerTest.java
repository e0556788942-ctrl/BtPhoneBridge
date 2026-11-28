package com.phonelink.m393;

import org.junit.Test;
import static org.junit.Assert.*;

public class ProfileProxyRequestTrackerTest {
    @Test public void waitsForBothAcceptedProfileCallbacks() {
        ProfileProxyRequestTracker tracker = new ProfileProxyRequestTracker();
        long generation = tracker.begin(true, true);

        assertTrue(tracker.hasPending());
        assertTrue(tracker.complete(generation, ProfileProxyRequestTracker.HEADSET));
        assertTrue(tracker.hasPending());
        assertTrue(tracker.pendingProfiles().contains(ProfileProxyRequestTracker.A2DP));

        assertTrue(tracker.complete(generation, ProfileProxyRequestTracker.A2DP));
        assertFalse(tracker.hasPending());
    }

    @Test public void tracksOnlyProfilesThatNeedARequest() {
        ProfileProxyRequestTracker tracker = new ProfileProxyRequestTracker();
        long generation = tracker.begin(false, true);

        assertTrue(tracker.hasPending());
        assertFalse(tracker.pendingProfiles().contains(ProfileProxyRequestTracker.HEADSET));
        assertTrue(tracker.pendingProfiles().contains(ProfileProxyRequestTracker.A2DP));

        assertTrue(tracker.complete(generation, ProfileProxyRequestTracker.A2DP));
        assertFalse(tracker.hasPending());
    }

    @Test public void rejectedRequestsLeaveTrackerReadyForRetry() {
        ProfileProxyRequestTracker tracker = new ProfileProxyRequestTracker();
        long emptyGeneration = tracker.begin(false, false);

        assertFalse(tracker.hasPending());
        assertTrue(tracker.isCurrent(emptyGeneration));

        long retryGeneration = tracker.begin(true, false);
        assertTrue(tracker.hasPending());
        assertFalse(tracker.isCurrent(emptyGeneration));
        assertTrue(tracker.isCurrent(retryGeneration));
    }

    @Test public void duplicateAndUnknownCallbacksAreIdempotent() {
        ProfileProxyRequestTracker tracker = new ProfileProxyRequestTracker();
        long generation = tracker.begin(true, false);

        assertFalse(tracker.complete(generation, ProfileProxyRequestTracker.A2DP));
        assertTrue(tracker.hasPending());
        assertTrue(tracker.complete(generation, ProfileProxyRequestTracker.HEADSET));
        assertFalse(tracker.complete(generation, ProfileProxyRequestTracker.HEADSET));
        assertFalse(tracker.hasPending());
    }

    @Test public void resetClearsPendingRequestsAndInvalidatesGeneration() {
        ProfileProxyRequestTracker tracker = new ProfileProxyRequestTracker();
        long generation = tracker.begin(true, true);

        assertTrue(tracker.reset(generation));
        assertFalse(tracker.isCurrent(generation));
        assertFalse(tracker.hasPending());
        assertTrue(tracker.pendingProfiles().isEmpty());
    }

    @Test public void lateCallbackFromTimedOutBatchCannotCompleteNewBatch() {
        ProfileProxyRequestTracker tracker = new ProfileProxyRequestTracker();
        long oldGeneration = tracker.begin(true, false);
        assertTrue(tracker.reset(oldGeneration));

        long currentGeneration = tracker.begin(true, true);
        assertFalse(tracker.isCurrent(oldGeneration));
        assertFalse(tracker.complete(oldGeneration, ProfileProxyRequestTracker.HEADSET));

        assertTrue(tracker.isCurrent(currentGeneration));
        assertTrue(tracker.pendingProfiles().contains(ProfileProxyRequestTracker.HEADSET));
        assertTrue(tracker.pendingProfiles().contains(ProfileProxyRequestTracker.A2DP));
    }

    @Test public void staleTimeoutCannotResetNewerBatch() {
        ProfileProxyRequestTracker tracker = new ProfileProxyRequestTracker();
        long oldGeneration = tracker.begin(true, false);
        assertTrue(tracker.reset(oldGeneration));

        long currentGeneration = tracker.begin(false, true);
        assertFalse(tracker.reset(oldGeneration));

        assertTrue(tracker.isCurrent(currentGeneration));
        assertTrue(tracker.hasPending());
        assertTrue(tracker.pendingProfiles().contains(ProfileProxyRequestTracker.A2DP));
    }
}
