package com.android.server.net.watchlist;

import android.privacy.DifferentialPrivacyEncoder;
import android.privacy.internal.longitudinalreporting.LongitudinalReportingConfig;
import android.privacy.internal.longitudinalreporting.LongitudinalReportingEncoder;
import com.android.internal.annotations.VisibleForTesting;
import com.android.server.net.watchlist.WatchlistReportDbHelper.AggregatedResult;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class PrivacyUtils {
    private static final boolean DEBUG = false;
    private static final String ENCODER_ID_PREFIX = "watchlist_encoder:";
    private static final double PROB_F = 0.469d;
    private static final double PROB_P = 0.28d;
    private static final double PROB_Q = 1.0d;
    private static final String TAG = "PrivacyUtils";

    private PrivacyUtils() {
    }

    @VisibleForTesting
    static DifferentialPrivacyEncoder createInsecureDPEncoderForTest(String appDigest) {
        return LongitudinalReportingEncoder.createInsecureEncoderForTest(createLongitudinalReportingConfig(appDigest));
    }

    @VisibleForTesting
    static DifferentialPrivacyEncoder createSecureDPEncoder(byte[] userSecret, String appDigest) {
        return LongitudinalReportingEncoder.createEncoder(createLongitudinalReportingConfig(appDigest), userSecret);
    }

    private static LongitudinalReportingConfig createLongitudinalReportingConfig(String appDigest) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(ENCODER_ID_PREFIX);
        stringBuilder.append(appDigest);
        return new LongitudinalReportingConfig(stringBuilder.toString(), PROB_F, PROB_P, PROB_Q);
    }

    @VisibleForTesting
    static Map<String, Boolean> createDpEncodedReportMap(boolean isSecure, byte[] userSecret, List<String> appDigestList, AggregatedResult aggregatedResult) {
        int appDigestListSize = appDigestList.size();
        HashMap<String, Boolean> resultMap = new HashMap(appDigestListSize);
        for (int i = 0; i < appDigestListSize; i++) {
            DifferentialPrivacyEncoder encoder;
            String appDigest = (String) appDigestList.get(i);
            if (isSecure) {
                encoder = createSecureDPEncoder(userSecret, appDigest);
            } else {
                encoder = createInsecureDPEncoderForTest(appDigest);
            }
            boolean z = true;
            if ((encoder.encodeBoolean(aggregatedResult.appDigestList.contains(appDigest))[0] & 1) != 1) {
                z = false;
            }
            resultMap.put(appDigest, Boolean.valueOf(z));
        }
        return resultMap;
    }
}
