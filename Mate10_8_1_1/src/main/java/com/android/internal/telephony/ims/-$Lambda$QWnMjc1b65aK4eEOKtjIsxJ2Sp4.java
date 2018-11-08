package com.android.internal.telephony.ims;

import android.content.ComponentName;
import android.util.Pair;
import com.android.internal.telephony.ims.ImsResolver.ImsServiceInfo;
import java.util.Objects;
import java.util.function.Predicate;

final /* synthetic */ class -$Lambda$QWnMjc1b65aK4eEOKtjIsxJ2Sp4 implements Predicate {
    private final /* synthetic */ byte $id;
    private final /* synthetic */ Object -$f0;

    private final /* synthetic */ boolean $m$0(Object arg0) {
        return Objects.equals(((ImsServiceController) arg0).getComponentName(), ((ImsServiceInfo) this.-$f0).name);
    }

    private final /* synthetic */ boolean $m$1(Object arg0) {
        return Objects.equals(((ImsServiceInfo) arg0).name, (ComponentName) this.-$f0);
    }

    private final /* synthetic */ boolean $m$2(Object arg0) {
        return Objects.equals(((ImsServiceInfo) arg0).name.getPackageName(), (String) this.-$f0);
    }

    private final /* synthetic */ boolean $m$3(Object arg0) {
        return ImsServiceController.lambda$-com_android_internal_telephony_ims_ImsServiceController_19011((Pair) this.-$f0, (ImsFeatureStatusCallback) arg0);
    }

    public /* synthetic */ -$Lambda$QWnMjc1b65aK4eEOKtjIsxJ2Sp4(byte b, Object obj) {
        this.$id = b;
        this.-$f0 = obj;
    }

    public final boolean test(Object obj) {
        switch (this.$id) {
            case (byte) 0:
                return $m$0(obj);
            case (byte) 1:
                return $m$1(obj);
            case (byte) 2:
                return $m$2(obj);
            case (byte) 3:
                return $m$3(obj);
            default:
                throw new AssertionError();
        }
    }
}
