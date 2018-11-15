package com.android.server.am;

import android.app.ITaskStackListener;
import android.os.Message;
import com.android.server.am.TaskChangeNotificationController.TaskStackConsumer;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$TaskChangeNotificationController$K9kaSj6_p5pzfyRh9i93xiC9T3s implements TaskStackConsumer {
    public static final /* synthetic */ -$$Lambda$TaskChangeNotificationController$K9kaSj6_p5pzfyRh9i93xiC9T3s INSTANCE = new -$$Lambda$TaskChangeNotificationController$K9kaSj6_p5pzfyRh9i93xiC9T3s();

    private /* synthetic */ -$$Lambda$TaskChangeNotificationController$K9kaSj6_p5pzfyRh9i93xiC9T3s() {
    }

    public final void accept(ITaskStackListener iTaskStackListener, Message message) {
        iTaskStackListener.onTaskRemoved(message.arg1);
    }
}
