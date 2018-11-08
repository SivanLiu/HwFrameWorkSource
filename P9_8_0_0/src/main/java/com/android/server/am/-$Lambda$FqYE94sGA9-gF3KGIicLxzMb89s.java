package com.android.server.am;

import android.app.ITaskStackListener;
import android.os.Message;
import com.android.server.am.TaskChangeNotificationController.TaskStackConsumer;

final /* synthetic */ class -$Lambda$FqYE94sGA9-gF3KGIicLxzMb89s implements TaskStackConsumer {
    private final /* synthetic */ void $m$0(ITaskStackListener arg0, Message arg1) {
        arg0.onTaskStackChanged();
    }

    public final void accept(ITaskStackListener iTaskStackListener, Message message) {
        $m$0(iTaskStackListener, message);
    }
}
