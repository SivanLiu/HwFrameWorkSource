package com.android.server.am;

import android.app.ITaskStackListener;
import android.os.Message;
import com.android.server.am.TaskChangeNotificationController.TaskStackConsumer;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$TaskChangeNotificationController$sw023kIrIGSeLwYwKC0ioKX3zEA implements TaskStackConsumer {
    public static final /* synthetic */ -$$Lambda$TaskChangeNotificationController$sw023kIrIGSeLwYwKC0ioKX3zEA INSTANCE = new -$$Lambda$TaskChangeNotificationController$sw023kIrIGSeLwYwKC0ioKX3zEA();

    private /* synthetic */ -$$Lambda$TaskChangeNotificationController$sw023kIrIGSeLwYwKC0ioKX3zEA() {
    }

    public final void accept(ITaskStackListener iTaskStackListener, Message message) {
        iTaskStackListener.onActivityForcedResizable((String) message.obj, message.arg1, message.arg2);
    }
}
