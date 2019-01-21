package com.huawei.nb.notification;

import android.os.RemoteException;
import com.huawei.nb.client.callback.CallbackManager;
import com.huawei.nb.client.callback.SubscribeCallback;
import com.huawei.nb.coordinator.common.CoordinatorJsonAnalyzer;
import com.huawei.nb.environment.Disposable;
import com.huawei.nb.notification.IModelObserver.Stub;
import com.huawei.nb.service.IDataServiceCall;
import com.huawei.nb.service.ServiceConnector;
import com.huawei.nb.utils.logger.DSLog;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

public class LocalObservable extends ServiceConnector<IDataServiceCall> implements Disposable {
    private static final Object PRESENT = new Object();
    private static final long TIMEOUT_MILLISECONDS = 1500;
    private final CallbackManager callbackManager;
    private boolean isDisposed = false;
    private final Object lock = new Object();
    private final Map<ModelObserverInfo, Map<ModelObserver, Object>> modelObserverMap = new HashMap();

    protected class RemoteModelObserver extends Stub {
        private ModelObserverInfo info;

        public RemoteModelObserver(ModelObserverInfo info) {
            this.info = info;
        }

        private ModelObserver[] copyObserver() {
            ModelObserver[] modelObserverArr;
            synchronized (LocalObservable.this.lock) {
                Map<ModelObserver, Object> set = (Map) LocalObservable.this.modelObserverMap.get(this.info);
                if (set == null || set.size() == 0) {
                    modelObserverArr = null;
                } else {
                    modelObserverArr = (ModelObserver[]) ((ModelObserver[]) set.keySet().toArray(new ModelObserver[set.size()])).clone();
                }
            }
            return modelObserverArr;
        }

        public void notify(final ChangeNotification changeNotification) throws RemoteException {
            if (changeNotification != null && changeNotification.getType() != null) {
                final ModelObserver[] observers = copyObserver();
                if (observers != null) {
                    new Thread(new Runnable() {
                        public void run() {
                            for (ModelObserver observer : observers) {
                                observer.onModelChanged(changeNotification);
                            }
                        }
                    }).start();
                }
            }
        }
    }

    public LocalObservable(CallbackManager callbackManager) {
        this.callbackManager = callbackManager;
    }

    private boolean registerModelRemoteObserver(ModelObserverInfo observerInfo) {
        if (getRemoteService() == null) {
            DSLog.e("Failed to register remote observer, error: not connected to data service.", new Object[0]);
            return false;
        }
        DSLog.d("Register observer for %s to remote service.", observerInfo.getModelClazz().getSimpleName());
        RemoteModelObserver remoteModelObserver = new RemoteModelObserver(observerInfo);
        SubscribeCallback subscribeCallback = this.callbackManager.createSubscribeCallback();
        try {
            return subscribeCallback.await(((IDataServiceCall) getRemoteService()).registerModelObserver(observerInfo, remoteModelObserver, subscribeCallback), (long) TIMEOUT_MILLISECONDS).booleanValue();
        } catch (RemoteException | RuntimeException e) {
            DSLog.e("Failed to register observer for %s, error: %s.", observerInfo, e.getMessage());
            return false;
        }
    }

    private boolean unregisterModelRemoteObserver(ModelObserverInfo observerInfo) {
        if (getRemoteService() == null) {
            DSLog.e("Failed to unregister remote observer, error: not connected to data service.", new Object[0]);
            return false;
        }
        String str = "Unregister %s observer for %s to remote service.";
        Object[] objArr = new Object[2];
        objArr[0] = observerInfo.getType() == ObserverType.OBSERVER_MODEL ? "model" : CoordinatorJsonAnalyzer.MSG_TYPE;
        objArr[1] = observerInfo.getModelClazz().getSimpleName();
        DSLog.d(str, objArr);
        RemoteModelObserver remoteModelObserver = new RemoteModelObserver(observerInfo);
        SubscribeCallback subscribeCallback = this.callbackManager.createSubscribeCallback();
        try {
            return subscribeCallback.await(((IDataServiceCall) getRemoteService()).unregisterModelObserver(observerInfo, remoteModelObserver, subscribeCallback), (long) TIMEOUT_MILLISECONDS).booleanValue();
        } catch (RemoteException | RuntimeException e) {
            DSLog.e("Failed to unregister model observer for %s, error: %s.", observerInfo, e.getMessage());
            return false;
        }
    }

    public void setRemoteService(IDataServiceCall remoteService) {
        if (remoteService == null) {
            super.unsetRemoteService();
            return;
        }
        super.setRemoteService(remoteService);
        registerAll();
    }

    /* JADX WARNING: Missing block: B:26:?, code skipped:
            return true;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public boolean registerObserver(ModelObserverInfo info, ModelObserver observer) {
        if (info == null || observer == null) {
            DSLog.e("Failed to register observer, error: null observer information.", new Object[0]);
            return false;
        }
        synchronized (this.lock) {
            Map<ModelObserver, Object> observers = (Map) this.modelObserverMap.get(info);
            if (observers != null) {
                if (observers.containsKey(observer)) {
                    DSLog.w("The same observer for %s has been registered already.", info.getModelClazz().getSimpleName());
                } else {
                    observers.put(observer, PRESENT);
                }
            } else if (registerModelRemoteObserver(info)) {
                observers = new HashMap();
                observers.put(observer, PRESENT);
                this.modelObserverMap.put(info, observers);
            } else {
                DSLog.e("Failed to register remote observer for %s.", info.getModelClazz().getSimpleName());
                return false;
            }
        }
    }

    public boolean unregisterObserver(ModelObserverInfo info, ModelObserver observer) {
        boolean result = false;
        if (info == null || observer == null) {
            DSLog.e("Failed to unregister observer, error: null observer information to unregister", new Object[0]);
        } else {
            synchronized (this.lock) {
                Map<ModelObserver, Object> observers = (Map) this.modelObserverMap.get(info);
                if (observers == null) {
                } else {
                    if (observers.remove(observer) != null) {
                        result = true;
                    }
                    if (observers.isEmpty()) {
                        unregisterModelRemoteObserver(info);
                        this.modelObserverMap.remove(info);
                    }
                }
            }
        }
        return result;
    }

    private void unregisterAll() {
        synchronized (this.lock) {
            for (Entry<ModelObserverInfo, Map<ModelObserver, Object>> entry : this.modelObserverMap.entrySet()) {
                unregisterModelRemoteObserver((ModelObserverInfo) entry.getKey());
                ((Map) entry.getValue()).clear();
            }
            this.modelObserverMap.clear();
        }
    }

    private void registerAll() {
        synchronized (this.lock) {
            for (ModelObserverInfo info : this.modelObserverMap.keySet()) {
                registerModelRemoteObserver(info);
            }
        }
    }

    public void dispose() {
        synchronized (this) {
            unregisterAll();
            unsetRemoteService();
            this.isDisposed = true;
        }
    }

    public boolean isDisposed() {
        return this.isDisposed;
    }
}
