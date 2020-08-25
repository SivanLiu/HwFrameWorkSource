package com.huawei.nb.notification;

import com.huawei.nb.utils.logger.DSLog;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public abstract class LocalObservable<INFO, OBSERVER, REMOTE> {
    protected final Object lock = new Object();
    protected final Map<INFO, Set<OBSERVER>> modelObserverMap = new HashMap();
    private volatile REMOTE remoteService;

    /* access modifiers changed from: protected */
    public abstract boolean registerModelRemoteObserver(INFO info);

    /* access modifiers changed from: protected */
    public abstract boolean unregisterModelRemoteObserver(INFO info);

    public boolean registerObserver(INFO info, OBSERVER observer) {
        boolean z = false;
        if (info == null || observer == null) {
            DSLog.e("Failed to register observer, error: null observer information.", new Object[0]);
        } else {
            synchronized (this.lock) {
                Set<OBSERVER> observers = this.modelObserverMap.get(info);
                if (observers != null) {
                    if (observers.contains(observer)) {
                        DSLog.w("The same observer for has been registered already.", new Object[0]);
                    } else {
                        observers.add(observer);
                    }
                } else if (!registerModelRemoteObserver(info)) {
                    DSLog.e("Failed to register remote observer.", new Object[0]);
                } else {
                    Set<OBSERVER> observers2 = new HashSet<>();
                    observers2.add(observer);
                    this.modelObserverMap.put(info, observers2);
                }
                z = true;
            }
        }
        return z;
    }

    public boolean unregisterObserver(INFO info, OBSERVER observer) {
        boolean z = false;
        if (info == null || observer == null) {
            DSLog.e("Failed to unregister observer, error: null observer information to unregister.", new Object[0]);
        } else {
            synchronized (this.lock) {
                Set<OBSERVER> observers = this.modelObserverMap.get(info);
                if (observers != null) {
                    z = observers.remove(observer);
                    if (observers.isEmpty()) {
                        unregisterModelRemoteObserver(info);
                        this.modelObserverMap.remove(info);
                    }
                }
            }
        }
        return z;
    }

    public void start(REMOTE remoteService2) {
        this.remoteService = remoteService2;
        if (remoteService2 != null) {
            registerAllObservers();
        }
    }

    public void pause() {
        this.remoteService = null;
    }

    public void stop() {
        if (this.remoteService != null) {
            unregisterAllObservers();
            this.remoteService = null;
        }
        clearAllObservers();
    }

    /* access modifiers changed from: protected */
    public REMOTE getRemoteService() {
        return this.remoteService;
    }

    /* access modifiers changed from: protected */
    public List<OBSERVER> getObservers(INFO info) {
        ArrayList arrayList;
        synchronized (this.lock) {
            Set<OBSERVER> observers = this.modelObserverMap.get(info);
            arrayList = observers == null ? null : new ArrayList(observers);
        }
        return arrayList;
    }

    private void unregisterAllObservers() {
        synchronized (this.lock) {
            for (Map.Entry<INFO, Set<OBSERVER>> entry : this.modelObserverMap.entrySet()) {
                unregisterModelRemoteObserver(entry.getKey());
                entry.getValue().clear();
            }
        }
    }

    private void registerAllObservers() {
        synchronized (this.lock) {
            for (INFO info : this.modelObserverMap.keySet()) {
                registerModelRemoteObserver(info);
            }
        }
    }

    private void clearAllObservers() {
        synchronized (this.lock) {
            this.modelObserverMap.clear();
        }
    }
}
