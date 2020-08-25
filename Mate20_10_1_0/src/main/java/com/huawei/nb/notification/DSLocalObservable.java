package com.huawei.nb.notification;

import android.os.RemoteException;
import com.huawei.nb.notification.IModelObserver;
import com.huawei.nb.service.IDataServiceCall;
import com.huawei.nb.utils.logger.DSLog;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DSLocalObservable extends LocalObservable<ModelObserverInfo, ModelObserver, IDataServiceCall> {
    /* access modifiers changed from: private */
    public static final ExecutorService EXECUTOR_SERVICE = Executors.newSingleThreadExecutor();
    private static final int SUCCESS = 0;

    /* access modifiers changed from: protected */
    public boolean registerModelRemoteObserver(ModelObserverInfo observerInfo) {
        if (getRemoteService() == null) {
            DSLog.e("Failed to register observer, error: not connected to data service.", new Object[0]);
            return false;
        }
        try {
            if (((IDataServiceCall) getRemoteService()).registerModelObserver(observerInfo, new RemoteModelObserver(observerInfo), null) == 0) {
                DSLog.d("Register observer for %s.", observerInfo.getModelClazz().getSimpleName());
                return true;
            }
            DSLog.e("Failed to register observer for %s.", observerInfo.getModelClazz().getSimpleName());
            return false;
        } catch (RemoteException | RuntimeException e) {
            DSLog.e("Failed to register observer for %s, error: %s.", observerInfo, e.getMessage());
            return false;
        }
    }

    /* access modifiers changed from: protected */
    public boolean unregisterModelRemoteObserver(ModelObserverInfo observerInfo) {
        if (getRemoteService() == null) {
            DSLog.e("Failed to unregister observer, error: not connected to data service.", new Object[0]);
            return false;
        }
        try {
            if (((IDataServiceCall) getRemoteService()).unregisterModelObserver(observerInfo, new RemoteModelObserver(observerInfo), null) == 0) {
                DSLog.d("Unregister observer for %s.", observerInfo.getModelClazz().getSimpleName());
                return true;
            }
            DSLog.e("Failed to unregister observer for %s.", observerInfo.getModelClazz().getSimpleName());
            return false;
        } catch (RemoteException | RuntimeException e) {
            DSLog.e("Failed to unregister observer for %s, error: %s.", observerInfo, e.getMessage());
            return false;
        }
    }

    /* access modifiers changed from: private */
    public class RemoteModelObserver extends IModelObserver.Stub {
        private ModelObserverInfo info;

        private RemoteModelObserver(ModelObserverInfo info2) {
            this.info = info2;
        }

        @Override // com.huawei.nb.notification.IModelObserver
        public void notify(ChangeNotification changeNotification) {
            List<ModelObserver> observers;
            if (changeNotification != null && changeNotification.getType() != null && (observers = DSLocalObservable.this.getObservers(this.info)) != null) {
                DSLocalObservable.EXECUTOR_SERVICE.execute(new DSLocalObservable$RemoteModelObserver$$Lambda$0(observers, changeNotification));
            }
        }

        static final /* synthetic */ void lambda$notify$2$DSLocalObservable$RemoteModelObserver(List list, ChangeNotification changeNotification) {
            Iterator it = list.iterator();
            while (it.hasNext()) {
                ((ModelObserver) it.next()).onModelChanged(changeNotification);
            }
        }
    }
}
