package android.privacy.internal.rappor;

import android.privacy.DifferentialPrivacyConfig;
import android.text.TextUtils;
import com.android.internal.util.Preconditions;

public class RapporConfig implements DifferentialPrivacyConfig {
    private static final String ALGORITHM_NAME = "Rappor";
    final String mEncoderId;
    final int mNumBits;
    final int mNumBloomHashes;
    final int mNumCohorts;
    final double mProbabilityF;
    final double mProbabilityP;
    final double mProbabilityQ;

    public RapporConfig(String encoderId, int numBits, double probabilityF, double probabilityP, double probabilityQ, int numCohorts, int numBloomHashes) {
        int i = numBits;
        double d = probabilityF;
        double d2 = probabilityP;
        double d3 = probabilityQ;
        int i2 = numCohorts;
        int i3 = numBloomHashes;
        Preconditions.checkArgument(TextUtils.isEmpty(encoderId) ^ 1, "encoderId cannot be empty");
        this.mEncoderId = encoderId;
        boolean z = false;
        Preconditions.checkArgument(i > 0, "numBits needs to be > 0");
        this.mNumBits = i;
        boolean z2 = d >= 0.0d && d <= 1.0d;
        Preconditions.checkArgument(z2, "probabilityF must be in range [0.0, 1.0]");
        this.mProbabilityF = d;
        boolean z3 = d2 >= 0.0d && d2 <= 1.0d;
        Preconditions.checkArgument(z3, "probabilityP must be in range [0.0, 1.0]");
        this.mProbabilityP = d2;
        z3 = d3 >= 0.0d && d3 <= 1.0d;
        Preconditions.checkArgument(z3, "probabilityQ must be in range [0.0, 1.0]");
        this.mProbabilityQ = d3;
        Preconditions.checkArgument(i2 > 0, "numCohorts needs to be > 0");
        this.mNumCohorts = i2;
        if (i3 > 0) {
            z = true;
        }
        Preconditions.checkArgument(z, "numBloomHashes needs to be > 0");
        this.mNumBloomHashes = i3;
    }

    public String getAlgorithm() {
        return ALGORITHM_NAME;
    }

    public String toString() {
        return String.format("EncoderId: %s, NumBits: %d, ProbabilityF: %.3f, ProbabilityP: %.3f, ProbabilityQ: %.3f, NumCohorts: %d, NumBloomHashes: %d", new Object[]{this.mEncoderId, Integer.valueOf(this.mNumBits), Double.valueOf(this.mProbabilityF), Double.valueOf(this.mProbabilityP), Double.valueOf(this.mProbabilityQ), Integer.valueOf(this.mNumCohorts), Integer.valueOf(this.mNumBloomHashes)});
    }
}
