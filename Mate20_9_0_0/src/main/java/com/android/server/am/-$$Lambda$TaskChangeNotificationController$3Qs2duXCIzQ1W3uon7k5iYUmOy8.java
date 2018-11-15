package com.android.server.am;

import android.app.ITaskStackListener;
import android.os.Message;
import com.android.server.am.TaskChangeNotificationController.TaskStackConsumer;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$TaskChangeNotificationController$3Qs2duXCIzQ1W3uon7k5iYUmOy8 implements TaskStackConsumer {
    public static final /* synthetic */ -$$Lambda$TaskChangeNotificationController$3Qs2duXCIzQ1W3uon7k5iYUmOy8 INSTANCE = new -$$Lambda$TaskChangeNotificationController$3Qs2duXCIzQ1W3uon7k5iYUmOy8();

    private /* synthetic */ -$$Lambda$TaskChangeNotificationController$3Qs2duXCIzQ1W3uon7k5iYUmOy8() {
    }

    public final void accept(ITaskStackListener iTaskStackListener, Message message) {
        iTaskStackListener.onActivityUnpinned();
    }
}
