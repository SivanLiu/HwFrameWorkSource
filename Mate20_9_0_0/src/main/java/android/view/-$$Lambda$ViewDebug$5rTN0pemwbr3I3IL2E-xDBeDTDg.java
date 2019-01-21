package android.view;

import java.util.concurrent.CountDownLatch;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$ViewDebug$5rTN0pemwbr3I3IL2E-xDBeDTDg implements Runnable {
    private final /* synthetic */ ViewOperation f$0;
    private final /* synthetic */ long[] f$1;
    private final /* synthetic */ CountDownLatch f$2;

    public /* synthetic */ -$$Lambda$ViewDebug$5rTN0pemwbr3I3IL2E-xDBeDTDg(ViewOperation viewOperation, long[] jArr, CountDownLatch countDownLatch) {
        this.f$0 = viewOperation;
        this.f$1 = jArr;
        this.f$2 = countDownLatch;
    }

    public final void run() {
        ViewDebug.lambda$profileViewOperation$3(this.f$0, this.f$1, this.f$2);
    }
}
