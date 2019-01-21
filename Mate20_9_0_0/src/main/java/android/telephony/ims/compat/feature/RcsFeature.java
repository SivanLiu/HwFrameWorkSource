package android.telephony.ims.compat.feature;

import com.android.ims.internal.IImsRcsFeature;
import com.android.ims.internal.IImsRcsFeature.Stub;

public class RcsFeature extends ImsFeature {
    private final IImsRcsFeature mImsRcsBinder = new Stub() {
    };

    public void onFeatureReady() {
    }

    public void onFeatureRemoved() {
    }

    public final IImsRcsFeature getBinder() {
        return this.mImsRcsBinder;
    }
}
