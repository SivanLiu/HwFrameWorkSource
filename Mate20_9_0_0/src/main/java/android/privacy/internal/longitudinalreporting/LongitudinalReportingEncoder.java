package android.privacy.internal.longitudinalreporting;

import android.privacy.DifferentialPrivacyEncoder;
import android.privacy.internal.rappor.RapporConfig;
import android.privacy.internal.rappor.RapporEncoder;
import com.android.internal.annotations.VisibleForTesting;

public class LongitudinalReportingEncoder implements DifferentialPrivacyEncoder {
    private static final boolean DEBUG = false;
    private static final String PRR1_ENCODER_ID = "prr1_encoder_id";
    private static final String PRR2_ENCODER_ID = "prr2_encoder_id";
    private static final String TAG = "LongitudinalEncoder";
    private final LongitudinalReportingConfig mConfig;
    private final Boolean mFakeValue;
    private final RapporEncoder mIRREncoder;
    private final boolean mIsSecure;

    public static LongitudinalReportingEncoder createEncoder(LongitudinalReportingConfig config, byte[] userSecret) {
        return new LongitudinalReportingEncoder(config, true, userSecret);
    }

    @VisibleForTesting
    public static LongitudinalReportingEncoder createInsecureEncoderForTest(LongitudinalReportingConfig config) {
        return new LongitudinalReportingEncoder(config, false, null);
    }

    private LongitudinalReportingEncoder(LongitudinalReportingConfig config, boolean secureEncoder, byte[] userSecret) {
        RapporEncoder createEncoder;
        this.mConfig = config;
        this.mIsSecure = secureEncoder;
        boolean ignoreOriginalInput = config.getProbabilityP();
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(config.getEncoderId());
        stringBuilder.append(PRR1_ENCODER_ID);
        if (getLongTermRandomizedResult(ignoreOriginalInput, secureEncoder, userSecret, stringBuilder.toString())) {
            double probabilityQ = config.getProbabilityQ();
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append(config.getEncoderId());
            stringBuilder2.append(PRR2_ENCODER_ID);
            this.mFakeValue = Boolean.valueOf(getLongTermRandomizedResult(probabilityQ, secureEncoder, userSecret, stringBuilder2.toString()));
        } else {
            this.mFakeValue = null;
        }
        RapporConfig irrConfig = config.getIRRConfig();
        if (secureEncoder) {
            createEncoder = RapporEncoder.createEncoder(irrConfig, userSecret);
        } else {
            createEncoder = RapporEncoder.createInsecureEncoderForTest(irrConfig);
        }
        this.mIRREncoder = createEncoder;
    }

    public byte[] encodeString(String original) {
        throw new UnsupportedOperationException();
    }

    public byte[] encodeBoolean(boolean original) {
        if (this.mFakeValue != null) {
            original = this.mFakeValue.booleanValue();
        }
        return this.mIRREncoder.encodeBoolean(original);
    }

    public byte[] encodeBits(byte[] bits) {
        throw new UnsupportedOperationException();
    }

    public LongitudinalReportingConfig getConfig() {
        return this.mConfig;
    }

    public boolean isInsecureEncoderForTest() {
        return this.mIsSecure ^ 1;
    }

    @VisibleForTesting
    public static boolean getLongTermRandomizedResult(double p, boolean secureEncoder, byte[] userSecret, String encoderId) {
        RapporEncoder encoder;
        double effectiveF = p < 0.5d ? p * 2.0d : (1.0d - p) * 2.0d;
        boolean prrInput = p >= 0.5d;
        RapporConfig rapporConfig = new RapporConfig(encoderId, 1, effectiveF, 0.0d, 1.0d, 1, 1);
        if (secureEncoder) {
            encoder = RapporEncoder.createEncoder(rapporConfig, userSecret);
        } else {
            byte[] bArr = userSecret;
            encoder = RapporEncoder.createInsecureEncoderForTest(rapporConfig);
        }
        if (encoder.encodeBoolean(prrInput)[0] > (byte) 0) {
            return true;
        }
        return false;
    }
}
