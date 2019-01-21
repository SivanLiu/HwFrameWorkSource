package android.telephony.euicc;

import android.telephony.euicc.EuiccCardManager.ResultCallback;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$EuiccCardManager$19$Gjn1FcgJf1Gqq6yBzVQLrvqlyg0 implements Runnable {
    private final /* synthetic */ ResultCallback f$0;
    private final /* synthetic */ int f$1;
    private final /* synthetic */ EuiccNotification[] f$2;

    public /* synthetic */ -$$Lambda$EuiccCardManager$19$Gjn1FcgJf1Gqq6yBzVQLrvqlyg0(ResultCallback resultCallback, int i, EuiccNotification[] euiccNotificationArr) {
        this.f$0 = resultCallback;
        this.f$1 = i;
        this.f$2 = euiccNotificationArr;
    }

    public final void run() {
        this.f$0.onComplete(this.f$1, this.f$2);
    }
}
