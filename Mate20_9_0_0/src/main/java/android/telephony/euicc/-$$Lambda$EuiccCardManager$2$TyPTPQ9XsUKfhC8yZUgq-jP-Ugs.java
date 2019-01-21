package android.telephony.euicc;

import android.service.euicc.EuiccProfileInfo;
import android.telephony.euicc.EuiccCardManager.ResultCallback;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$EuiccCardManager$2$TyPTPQ9XsUKfhC8yZUgq-jP-Ugs implements Runnable {
    private final /* synthetic */ ResultCallback f$0;
    private final /* synthetic */ int f$1;
    private final /* synthetic */ EuiccProfileInfo f$2;

    public /* synthetic */ -$$Lambda$EuiccCardManager$2$TyPTPQ9XsUKfhC8yZUgq-jP-Ugs(ResultCallback resultCallback, int i, EuiccProfileInfo euiccProfileInfo) {
        this.f$0 = resultCallback;
        this.f$1 = i;
        this.f$2 = euiccProfileInfo;
    }

    public final void run() {
        this.f$0.onComplete(this.f$1, this.f$2);
    }
}
