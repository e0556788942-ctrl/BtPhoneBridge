package com.phonelink.m393;

import org.junit.Test;
import static org.junit.Assert.*;

public class ProfileProxyRequestTrackerTest {
    @Test public void waitsForBothAcceptedProfileCallbacks() {
        ProfileProxyRequestTracker tracker = new ProfileProxyRequestTracker();
        tracker.begin(true, true);

        assertTrue(tracker.hasPending());
        tracker.complete(ProfileProxyRequestTracker.HEADSET);
        assertTrue(tracker.hasPending());
        assertTrue(tracker.pendingProfiles().contains(ProfileProxyRequestTracker.A2DP));

        tracker.complete(ProfileProxyRequestTracker.A2DP);
        assertFalse(tracker.hasPending());
    }

    @Test public void tracksOnlyProfilesThatNeedARequest() {
        ProfileProxyRequestTracker tracker = new ProfileProxyRequestTracker();
        tracker.begin(false, true);

        assertTrue(tracker.hasPending());
        assertFalse(tracker.pendingProfiles().contains(ProfileProxyRequestTracker.HEADSET));
        assertTrue(tracker.pendingProfiles().contains(ProfileProxyRequestTracker.A2DP));

        tracker.complete(ProfileProxyRequestTracker.A2DP);
        assertFalse(tracker.hasPending());
    }

    @Test public void rejectedRequestsLeaveTrackerReadyForRetry() {
        ProfileProxyRequestTracker tracker = new ProfileProxyRequestTracker();
        tracker.begin(false, false);

        assertFalse(tracker.hasPending());
        tracker.begin(true, false);
        assertTrue(tracker.hasPending());
    }

    @Test public void duplicateAndUnknownCallbacksAreIdempotent() {
        ProfileProxyRequestTracker tracker = new ProfileProxyRequestTracker();
        tracker.begin(true, false);

        tracker.complete(ProfileProxyRequestTracker.A2DP);
        assertTrue(tracker.hasPending());
        tracker.complete(ProfileProxyRequestTracker.HEADSET);
        tracker.complete(ProfileProxyRequestTracker.HEADSET);
        assertFalse(tracker.hasPending());
    }

    @Test public void resetClearsPendingRequests() {
        ProfileProxyRequestTracker tracker = new ProfileProxyRequestTracker();
        tracker.begin(true, true);
        tracker.reset();
        assertFalse(tracker.hasPending());
        assertTrue(tracker.pendingProfiles().isEmpty());
    }
}
