package com.huawei.nb.notification;

import android.os.RemoteException;
import com.huawei.nb.notification.IKvObserver;
import com.huawei.nb.service.IKvServiceCall;
import com.huawei.nb.utils.logger.DSLog;
import java.util.Iterator;
import java.util.List;

public class KvLocalObservable extends LocalObservable<KeyObserverInfo, KeyObserver, IKvServiceCall> {
    private static final int SUCCESS = 0;
    private static final String TAG = "KvLocalObservable";

    /* access modifiers changed from: protected */
    public boolean registerModelRemoteObserver(KeyObserverInfo observerInfo) {
        if (getRemoteService() == null) {
            DSLog.e("Failed to register remote observer, error: not connected to data service.", new Object[0]);
            return false;
        }
        try {
            if (((IKvServiceCall) getRemoteService()).registerObserver(observerInfo, new RemoteKvObserver(observerInfo), null) == 0) {
                DSLog.d(TAG, "Register observer for %s.", observerInfo.getKey());
                return true;
            }
            DSLog.e(TAG, "Failed to register observer for %s.", observerInfo.getKey());
            return false;
        } catch (RemoteException | RuntimeException e) {
            DSLog.et(TAG, "Failed to register observer for %s, error: %s.", observerInfo, e.getMessage());
            return false;
        }
    }

    /* access modifiers changed from: protected */
    public boolean unregisterModelRemoteObserver(KeyObserverInfo observerInfo) {
        if (getRemoteService() == null) {
            DSLog.e("Failed to unregister remote observer, error: not connected to data service.", new Object[0]);
            return false;
        }
        try {
            if (((IKvServiceCall) getRemoteService()).unRegisterObserver(observerInfo, new RemoteKvObserver(observerInfo), null) == 0) {
                DSLog.d(TAG, "Unregister observer for %s.", observerInfo.getKey());
                return true;
            }
            DSLog.e(TAG, "Failed to unregister observer for %s.", observerInfo.getKey());
            return false;
        } catch (RemoteException | RuntimeException e) {
            DSLog.et(TAG, "Failed to unregister model observer for %s, error: %s.", observerInfo, e.getMessage());
            return false;
        }
    }

    protected class RemoteKvObserver extends IKvObserver.Stub {
        private KeyObserverInfo info;

        RemoteKvObserver(KeyObserverInfo info2) {
            this.info = info2;
        }

        @Override // com.huawei.nb.notification.IKvObserver
        public void notify(ChangeNotification changeNotification) throws RemoteException {
            List<KeyObserver> observers;
            if (changeNotification != null && changeNotification.getType() != null && (observers = KvLocalObservable.this.getObservers(this.info)) != null) {
                new Thread(new KvLocalObservable$RemoteKvObserver$$Lambda$0(observers, changeNotification)).start();
            }
        }

        static final /* synthetic */ void lambda$notify$1$KvLocalObservable$RemoteKvObserver(List list, ChangeNotification changeNotification) {
            Iterator it = list.iterator();
            while (it.hasNext()) {
                ((KeyObserver) it.next()).onKeyChanged(changeNotification);
            }
        }
    }
}
