package com.android.server.wifi.hotspot2;

import android.util.Log;
import com.android.internal.annotations.VisibleForTesting;
import com.android.server.wifi.Clock;
import com.android.server.wifi.hotspot2.anqp.Constants.ANQPElementType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ANQPRequestManager {
    @VisibleForTesting
    public static final int BASE_HOLDOFF_TIME_MILLISECONDS = 10000;
    @VisibleForTesting
    public static final int MAX_HOLDOFF_COUNT = 6;
    private static final List<ANQPElementType> R1_ANQP_BASE_SET = Arrays.asList(new ANQPElementType[]{ANQPElementType.ANQPVenueName, ANQPElementType.ANQPIPAddrAvailability, ANQPElementType.ANQPNAIRealm, ANQPElementType.ANQP3GPPNetwork, ANQPElementType.ANQPDomName});
    private static final List<ANQPElementType> R2_ANQP_BASE_SET = Arrays.asList(new ANQPElementType[]{ANQPElementType.HSFriendlyName, ANQPElementType.HSWANMetrics, ANQPElementType.HSConnCapability, ANQPElementType.HSOSUProviders});
    private static final String TAG = "ANQPRequestManager";
    private final Clock mClock;
    private final Map<Long, HoldOffInfo> mHoldOffInfo = new HashMap();
    private final PasspointEventHandler mPasspointHandler;
    private final Map<Long, ANQPNetworkKey> mPendingQueries = new HashMap();

    private class HoldOffInfo {
        public int holdOffCount;
        public long holdOffExpirationTime;

        private HoldOffInfo() {
        }
    }

    public ANQPRequestManager(PasspointEventHandler handler, Clock clock) {
        this.mPasspointHandler = handler;
        this.mClock = clock;
    }

    public boolean requestANQPElements(long bssid, ANQPNetworkKey anqpNetworkKey, boolean rcOIs, boolean hsReleaseR2) {
        if (!canSendRequestNow(bssid) || !this.mPasspointHandler.requestANQP(bssid, getRequestElementIDs(rcOIs, hsReleaseR2))) {
            return false;
        }
        updateHoldOffInfo(bssid);
        this.mPendingQueries.put(Long.valueOf(bssid), anqpNetworkKey);
        return true;
    }

    public ANQPNetworkKey onRequestCompleted(long bssid, boolean success) {
        if (success) {
            this.mHoldOffInfo.remove(Long.valueOf(bssid));
        }
        return (ANQPNetworkKey) this.mPendingQueries.remove(Long.valueOf(bssid));
    }

    private boolean canSendRequestNow(long bssid) {
        long currentTime = this.mClock.getElapsedSinceBootMillis();
        HoldOffInfo info = (HoldOffInfo) this.mHoldOffInfo.get(Long.valueOf(bssid));
        if (info == null || info.holdOffExpirationTime <= currentTime) {
            return true;
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Not allowed to send ANQP request to ");
        stringBuilder.append(bssid);
        stringBuilder.append(" for another ");
        stringBuilder.append((info.holdOffExpirationTime - currentTime) / 1000);
        stringBuilder.append(" seconds");
        Log.d(str, stringBuilder.toString());
        return false;
    }

    private void updateHoldOffInfo(long bssid) {
        HoldOffInfo info = (HoldOffInfo) this.mHoldOffInfo.get(Long.valueOf(bssid));
        if (info == null) {
            info = new HoldOffInfo();
            this.mHoldOffInfo.put(Long.valueOf(bssid), info);
        }
        info.holdOffExpirationTime = this.mClock.getElapsedSinceBootMillis() + ((long) (10000 * (1 << info.holdOffCount)));
        if (info.holdOffCount < 6) {
            info.holdOffCount++;
        }
    }

    private static List<ANQPElementType> getRequestElementIDs(boolean rcOIs, boolean hsReleaseR2) {
        List<ANQPElementType> requestList = new ArrayList();
        requestList.addAll(R1_ANQP_BASE_SET);
        if (rcOIs) {
            requestList.add(ANQPElementType.ANQPRoamingConsortium);
        }
        if (hsReleaseR2) {
            requestList.addAll(R2_ANQP_BASE_SET);
        }
        return requestList;
    }
}
