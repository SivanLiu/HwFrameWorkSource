package com.android.server.am;

import android.app.ActivityManager.TaskDescription;
import android.app.ITaskStackListener;
import android.os.Message;
import com.android.server.am.TaskChangeNotificationController.TaskStackConsumer;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$TaskChangeNotificationController$bteC39aBoUFmJeWf3dk2BX1xZ6k implements TaskStackConsumer {
    public static final /* synthetic */ -$$Lambda$TaskChangeNotificationController$bteC39aBoUFmJeWf3dk2BX1xZ6k INSTANCE = new -$$Lambda$TaskChangeNotificationController$bteC39aBoUFmJeWf3dk2BX1xZ6k();

    private /* synthetic */ -$$Lambda$TaskChangeNotificationController$bteC39aBoUFmJeWf3dk2BX1xZ6k() {
    }

    public final void accept(ITaskStackListener iTaskStackListener, Message message) {
        iTaskStackListener.onTaskDescriptionChanged(message.arg1, (TaskDescription) message.obj);
    }
}
