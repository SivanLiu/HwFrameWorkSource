package com.huawei.odmf.core;

import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.ArrayMap;
import com.huawei.odmf.exception.ODMFIllegalArgumentException;
import com.huawei.odmf.user.api.AllChangeToTarget;
import com.huawei.odmf.user.api.IListener;
import com.huawei.odmf.user.api.ObjectContext;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

class NotifyClient {
    static final int NOTIFY_OBJECTS_CHANGED = 1;
    static final int NOTIFY_TABLE_DELETE = 2;
    ConcurrentHashMap<String, List<IListener>> entityMap = new ConcurrentHashMap<>();
    private final Object listenerLock = new Object();
    private Uri mUri;
    ConcurrentHashMap<ObjectId, List<IListener>> manageObjectMap = new ConcurrentHashMap<>();
    private final Handler msgHandler;
    ConcurrentHashMap<ObjectContext, List<IListener>> objectContextMap = new ConcurrentHashMap<>();
    List<IListener> psChangeListeners = Collections.synchronizedList(new ArrayList());

    NotifyClient(Looper looper, ObjectContext objectContext, Uri uri) {
        this.msgHandler = new MessageHandler(looper, objectContext);
        this.mUri = uri;
    }

    /* access modifiers changed from: package-private */
    public void registerListener(Uri uri, IListener listener) {
        if (uri == null || listener == null) {
            throw new ODMFIllegalArgumentException("Uri or IListener should not be null");
        } else if (!uri.toString().equals(this.mUri.toString())) {
            throw new ODMFIllegalArgumentException("Uri error!!");
        } else {
            synchronized (this.listenerLock) {
                if (!this.psChangeListeners.contains(listener)) {
                    this.psChangeListeners.add(listener);
                }
            }
        }
    }

    /* access modifiers changed from: package-private */
    public void registerListener(ObjectContext objectContext, IListener listener) {
        if (objectContext == null || listener == null) {
            throw new ODMFIllegalArgumentException("ObjectContext or IListener should not be null");
        } else if (this.objectContextMap.containsKey(objectContext)) {
            List<IListener> objContextMapListeners = this.objectContextMap.get(objectContext);
            synchronized (this.listenerLock) {
                if (!objContextMapListeners.contains(listener)) {
                    objContextMapListeners.add(listener);
                }
            }
        } else {
            List<IListener> objContextMapListeners2 = new ArrayList<>();
            synchronized (this.listenerLock) {
                objContextMapListeners2.add(listener);
            }
            this.objectContextMap.put(objectContext, objContextMapListeners2);
        }
    }

    /* access modifiers changed from: package-private */
    public void registerListener(String entityName, IListener listener) {
        if (entityName == null || listener == null) {
            throw new ODMFIllegalArgumentException("EntityName or IListener should not be null");
        } else if (this.entityMap.containsKey(entityName)) {
            List<IListener> entityMapListeners = this.entityMap.get(entityName);
            synchronized (this.listenerLock) {
                if (!entityMapListeners.contains(listener)) {
                    entityMapListeners.add(listener);
                }
            }
        } else {
            List<IListener> entityListeners = new ArrayList<>();
            synchronized (this.listenerLock) {
                entityListeners.add(listener);
            }
            this.entityMap.put(entityName, entityListeners);
        }
    }

    /* access modifiers changed from: package-private */
    public void registerListener(ManagedObject managedObject, IListener listener) {
        if (managedObject == null || listener == null) {
            throw new ODMFIllegalArgumentException("ManageObject or IListener should not be null");
        }
        ObjectId objId = managedObject.getObjectId();
        if (this.manageObjectMap.containsKey(objId)) {
            List<IListener> objMapListeners = this.manageObjectMap.get(objId);
            synchronized (this.listenerLock) {
                if (!objMapListeners.contains(listener)) {
                    objMapListeners.add(listener);
                }
            }
            return;
        }
        List<IListener> entityListeners = new ArrayList<>();
        synchronized (this.listenerLock) {
            entityListeners.add(listener);
        }
        this.manageObjectMap.put(objId, entityListeners);
    }

    /* access modifiers changed from: package-private */
    public void unregisterListener(Uri uri, IListener listener) {
        if (uri == null || listener == null) {
            throw new ODMFIllegalArgumentException("Uri or IListener should not be null");
        } else if (!uri.toString().equals(this.mUri.toString())) {
            throw new ODMFIllegalArgumentException("Uri error!!");
        } else {
            synchronized (this.listenerLock) {
                if (this.psChangeListeners.contains(listener)) {
                    this.psChangeListeners.remove(listener);
                }
            }
        }
    }

    /* access modifiers changed from: package-private */
    public void unregisterListener(ObjectContext objectContext, IListener listener) {
        if (objectContext == null || listener == null) {
            throw new ODMFIllegalArgumentException("ManageObject or IListener should not be null");
        } else if (this.objectContextMap.containsKey(objectContext)) {
            List<IListener> objContextMapListeners = this.objectContextMap.get(objectContext);
            synchronized (this.listenerLock) {
                if (objContextMapListeners.contains(listener)) {
                    objContextMapListeners.remove(listener);
                    if (objContextMapListeners.size() == 0) {
                        this.objectContextMap.remove(objectContext);
                    }
                }
            }
        }
    }

    /* access modifiers changed from: package-private */
    public void unregisterListener(String entityName, IListener listener) {
        if (entityName == null || listener == null) {
            throw new ODMFIllegalArgumentException("EntityName or IListener should not be null");
        } else if (this.entityMap.containsKey(entityName)) {
            List<IListener> entityMapListeners = this.entityMap.get(entityName);
            synchronized (this.listenerLock) {
                if (entityMapListeners.contains(listener)) {
                    entityMapListeners.remove(listener);
                    if (entityMapListeners.size() == 0) {
                        this.entityMap.remove(entityName);
                    }
                }
            }
        }
    }

    /* access modifiers changed from: package-private */
    public void unregisterListener(ManagedObject manageObject, IListener listener) {
        if (manageObject == null || listener == null) {
            throw new ODMFIllegalArgumentException("ManageObject or IListener should not be null");
        }
        ObjectId objId = manageObject.getObjectId();
        if (this.manageObjectMap.containsKey(objId)) {
            List<IListener> objMapListeners = this.manageObjectMap.get(objId);
            synchronized (this.listenerLock) {
                if (objMapListeners.contains(listener)) {
                    objMapListeners.remove(listener);
                    if (objMapListeners.size() == 0) {
                        this.manageObjectMap.remove(objId);
                    }
                }
            }
        }
    }

    /* access modifiers changed from: package-private */
    public boolean hasPsListener() {
        boolean z;
        synchronized (this.listenerLock) {
            z = this.psChangeListeners.size() > 0;
        }
        return z;
    }

    /* access modifiers changed from: package-private */
    public boolean hasObjectContextListener() {
        return this.objectContextMap.size() > 0;
    }

    /* access modifiers changed from: package-private */
    public boolean hasEntityListener() {
        return this.entityMap.size() > 0;
    }

    /* access modifiers changed from: package-private */
    public boolean hasManageObjListener() {
        return this.manageObjectMap.size() > 0;
    }

    /* access modifiers changed from: package-private */
    public boolean hasPsListener(String uriString) {
        boolean z = false;
        if (uriString != null && uriString.equals(this.mUri.toString())) {
            synchronized (this.listenerLock) {
                if (this.psChangeListeners.size() > 0) {
                    z = true;
                }
            }
        }
        return z;
    }

    /* access modifiers changed from: package-private */
    public boolean hasObjectContextListener(ObjectContext ctx) {
        return this.objectContextMap.containsKey(ctx);
    }

    /* access modifiers changed from: package-private */
    public boolean hasEntityListener(String entityName) {
        return this.entityMap.containsKey(entityName);
    }

    /* access modifiers changed from: package-private */
    public boolean hasManageObjListener(ObjectId id) {
        return this.manageObjectMap.containsKey(id);
    }

    /* access modifiers changed from: package-private */
    public void sendMessage(int message, AllChangeToThread allChange) {
        Message msg = this.msgHandler.obtainMessage(message);
        msg.obj = allChange;
        this.msgHandler.sendMessage(msg);
    }

    private class MessageHandler extends Handler {
        private AObjectContext objContext;

        MessageHandler(Looper looper, ObjectContext context) {
            super(looper);
            this.objContext = (AObjectContext) context;
        }

        public void handleMessage(Message msg) {
            AllChangeToThread allChange = (AllChangeToThread) msg.obj;
            switch (msg.what) {
                case 1:
                    NotifyClient.this.handleChangedMessage(this.objContext, allChange);
                    return;
                case 2:
                    NotifyClient.this.handleEntityClearMessage(this.objContext, allChange);
                    return;
                default:
                    return;
            }
        }
    }

    /* access modifiers changed from: private */
    public void handleChangedMessage(AObjectContext objContext, AllChangeToThread allChange) {
        ObjectContext changeContext = allChange.getChangeContext();
        if (objContext.getNotifyClient().hasPsListener()) {
            handleChangedMessageToPsListener(allChange, changeContext);
        }
        if (allChange.getObjContextMap().size() > 0) {
            handleChangedMessageToContextListener(allChange, changeContext);
        }
        if (allChange.getEntityMap().size() > 0) {
            handleChangedMessageToEntityListener(allChange, changeContext);
        }
        if (allChange.getManageObjMap().size() > 0) {
            handleChangedMessageToManagedObjListener(allChange, changeContext);
        }
    }

    private void handleChangedMessageToManagedObjListener(AllChangeToThread allChange, ObjectContext changeContext) {
        ArrayMap<ObjectId, AllChangeToTarget> manageObjAllChange = allChange.getManageObjMap();
        for (Map.Entry<ObjectId, List<IListener>> entry : this.manageObjectMap.entrySet()) {
            ObjectId objId = entry.getKey();
            List<IListener> copyListeners = new ArrayList<>();
            synchronized (this.listenerLock) {
                copyList(entry.getValue(), copyListeners);
            }
            AllChangeToTarget subManageObjAllChange = manageObjAllChange.get(objId);
            if (subManageObjAllChange != null) {
                int size = copyListeners.size();
                for (int i = 0; i < size; i++) {
                    copyListeners.get(i).onObjectsChanged(changeContext, subManageObjAllChange);
                }
            }
            copyListeners.clear();
        }
    }

    private void handleChangedMessageToEntityListener(AllChangeToThread allChange, ObjectContext changeContext) {
        ArrayMap<String, AllChangeToTarget> entityAllChange = allChange.getEntityMap();
        for (Map.Entry<String, List<IListener>> entry : this.entityMap.entrySet()) {
            String entityName = entry.getKey();
            List<IListener> copyListeners = new ArrayList<>();
            synchronized (this.listenerLock) {
                copyList(entry.getValue(), copyListeners);
            }
            AllChangeToTarget subEntityAllChange = entityAllChange.get(entityName);
            if (subEntityAllChange != null) {
                int size = copyListeners.size();
                for (int i = 0; i < size; i++) {
                    copyListeners.get(i).onObjectsChanged(changeContext, subEntityAllChange);
                }
            }
            copyListeners.clear();
        }
    }

    private void handleChangedMessageToContextListener(AllChangeToThread allChange, ObjectContext changeContext) {
        ArrayMap<ObjectContext, AllChangeToTarget> objContextAllChange = allChange.getObjContextMap();
        for (Map.Entry<ObjectContext, List<IListener>> entry : this.objectContextMap.entrySet()) {
            ObjectContext objCon = entry.getKey();
            List<IListener> copyListeners = new ArrayList<>();
            synchronized (this.listenerLock) {
                copyList(entry.getValue(), copyListeners);
            }
            AllChangeToTarget subObjConAllChange = objContextAllChange.get(objCon);
            if (subObjConAllChange != null) {
                int size = copyListeners.size();
                for (int i = 0; i < size; i++) {
                    copyListeners.get(i).onObjectsChanged(changeContext, subObjConAllChange);
                }
            }
            copyListeners.clear();
        }
    }

    private void handleChangedMessageToPsListener(AllChangeToThread allChange, ObjectContext changeContext) {
        AllChangeToTarget subPsAllChange = allChange.getPsMap().get(this.mUri.toString());
        List<IListener> copyListeners = new ArrayList<>();
        synchronized (this.listenerLock) {
            copyList(this.psChangeListeners, copyListeners);
        }
        if (subPsAllChange != null) {
            for (IListener commListener : copyListeners) {
                commListener.onObjectsChanged(changeContext, subPsAllChange);
            }
        }
        copyListeners.clear();
    }

    /* access modifiers changed from: private */
    public void handleEntityClearMessage(AObjectContext objContext, AllChangeToThread deleteMessage) {
        ObjectContext deleteContext = deleteMessage.getChangeContext();
        List<IListener> copyListeners = new ArrayList<>();
        synchronized (this.listenerLock) {
            copyList(this.psChangeListeners, copyListeners);
        }
        if (objContext.getNotifyClient().hasPsListener()) {
            for (IListener commListener : copyListeners) {
                commListener.onObjectsChanged(deleteContext, new AllChangeToTargetImpl(true));
            }
        }
        copyListeners.clear();
        if (objContext.getNotifyClient().hasEntityListener(deleteMessage.getEntityName())) {
            for (Map.Entry<String, List<IListener>> entry : this.entityMap.entrySet()) {
                String entityName = entry.getKey();
                List<IListener> copyEntityListeners = new ArrayList<>();
                synchronized (this.listenerLock) {
                    copyList(entry.getValue(), copyEntityListeners);
                }
                if (deleteMessage.getEntityName().equals(entityName)) {
                    int size = copyEntityListeners.size();
                    for (int i = 0; i < size; i++) {
                        copyEntityListeners.get(i).onObjectsChanged(deleteContext, new AllChangeToTargetImpl(true));
                    }
                }
                copyEntityListeners.clear();
            }
        }
    }

    private void copyList(List src, List dest) {
        if (src != null && dest != null) {
            int size = src.size();
            for (int i = 0; i < size; i++) {
                dest.add(src.get(i));
            }
        }
    }
}
