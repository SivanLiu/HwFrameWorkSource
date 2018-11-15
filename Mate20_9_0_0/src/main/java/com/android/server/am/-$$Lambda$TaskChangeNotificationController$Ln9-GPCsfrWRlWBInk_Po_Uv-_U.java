package com.android.server.am;

import android.app.ITaskStackListener;
import android.os.Message;
import com.android.server.am.TaskChangeNotificationController.TaskStackConsumer;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$TaskChangeNotificationController$Ln9-GPCsfrWRlWBInk_Po_Uv-_U implements TaskStackConsumer {
    public static final /* synthetic */ -$$Lambda$TaskChangeNotificationController$Ln9-GPCsfrWRlWBInk_Po_Uv-_U INSTANCE = new -$$Lambda$TaskChangeNotificationController$Ln9-GPCsfrWRlWBInk_Po_Uv-_U();

    private /* synthetic */ -$$Lambda$TaskChangeNotificationController$Ln9-GPCsfrWRlWBInk_Po_Uv-_U() {
    }

    public final void accept(ITaskStackListener iTaskStackListener, Message message) {
        iTaskStackListener.onActivityLaunchOnSecondaryDisplayFailed();
    }
}
