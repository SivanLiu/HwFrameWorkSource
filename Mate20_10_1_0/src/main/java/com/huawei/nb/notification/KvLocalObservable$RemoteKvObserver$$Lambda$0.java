package com.huawei.nb.notification;

import com.huawei.nb.notification.KvLocalObservable;
import java.util.List;

final /* synthetic */ class KvLocalObservable$RemoteKvObserver$$Lambda$0 implements Runnable {
    private final List arg$1;
    private final ChangeNotification arg$2;

    KvLocalObservable$RemoteKvObserver$$Lambda$0(List list, ChangeNotification changeNotification) {
        this.arg$1 = list;
        this.arg$2 = changeNotification;
    }

    public void run() {
        KvLocalObservable.RemoteKvObserver.lambda$notify$1$KvLocalObservable$RemoteKvObserver(this.arg$1, this.arg$2);
    }
}
