package com.android.server.am;

import android.app.ITaskStackListener;
import android.os.Message;
import com.android.server.am.TaskChangeNotificationController.TaskStackConsumer;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$TaskChangeNotificationController$iVGVcx2Ee37igl6ebl_htq_WO9o implements TaskStackConsumer {
    public static final /* synthetic */ -$$Lambda$TaskChangeNotificationController$iVGVcx2Ee37igl6ebl_htq_WO9o INSTANCE = new -$$Lambda$TaskChangeNotificationController$iVGVcx2Ee37igl6ebl_htq_WO9o();

    private /* synthetic */ -$$Lambda$TaskChangeNotificationController$iVGVcx2Ee37igl6ebl_htq_WO9o() {
    }

    public final void accept(ITaskStackListener iTaskStackListener, Message message) {
        iTaskStackListener.onPinnedStackAnimationStarted();
    }
}
