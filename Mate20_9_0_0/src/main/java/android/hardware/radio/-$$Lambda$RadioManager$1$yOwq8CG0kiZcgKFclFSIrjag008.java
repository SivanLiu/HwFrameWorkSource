package android.hardware.radio;

import android.hardware.radio.Announcement.OnListUpdatedListener;
import java.util.List;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$RadioManager$1$yOwq8CG0kiZcgKFclFSIrjag008 implements Runnable {
    private final /* synthetic */ OnListUpdatedListener f$0;
    private final /* synthetic */ List f$1;

    public /* synthetic */ -$$Lambda$RadioManager$1$yOwq8CG0kiZcgKFclFSIrjag008(OnListUpdatedListener onListUpdatedListener, List list) {
        this.f$0 = onListUpdatedListener;
        this.f$1 = list;
    }

    public final void run() {
        this.f$0.onListUpdated(this.f$1);
    }
}
