package com.huawei.nb.notification;

import com.huawei.nb.notification.DSLocalObservable;
import java.util.List;

final /* synthetic */ class DSLocalObservable$RemoteModelObserver$$Lambda$0 implements Runnable {
    private final List arg$1;
    private final ChangeNotification arg$2;

    DSLocalObservable$RemoteModelObserver$$Lambda$0(List list, ChangeNotification changeNotification) {
        this.arg$1 = list;
        this.arg$2 = changeNotification;
    }

    public void run() {
        DSLocalObservable.RemoteModelObserver.lambda$notify$2$DSLocalObservable$RemoteModelObserver(this.arg$1, this.arg$2);
    }
}
