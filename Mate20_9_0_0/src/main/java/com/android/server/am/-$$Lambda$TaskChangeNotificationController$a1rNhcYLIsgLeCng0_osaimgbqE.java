package com.android.server.am;

import android.app.ITaskStackListener;
import android.os.Message;
import com.android.server.am.TaskChangeNotificationController.TaskStackConsumer;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$TaskChangeNotificationController$a1rNhcYLIsgLeCng0_osaimgbqE implements TaskStackConsumer {
    public static final /* synthetic */ -$$Lambda$TaskChangeNotificationController$a1rNhcYLIsgLeCng0_osaimgbqE INSTANCE = new -$$Lambda$TaskChangeNotificationController$a1rNhcYLIsgLeCng0_osaimgbqE();

    private /* synthetic */ -$$Lambda$TaskChangeNotificationController$a1rNhcYLIsgLeCng0_osaimgbqE() {
    }

    public final void accept(ITaskStackListener iTaskStackListener, Message message) {
        iTaskStackListener.onActivityPinned((String) message.obj, message.sendingUid, message.arg1, message.arg2);
    }
}
