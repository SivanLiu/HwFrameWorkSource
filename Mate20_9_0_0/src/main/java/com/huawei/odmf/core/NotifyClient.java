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
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

class NotifyClient {
    static final int NOTIFY_OBJECTS_CHANGED = 1;
    static final int NOTIFY_TABLE_DELETE = 2;
    ConcurrentHashMap<String, List<IListener>> entityMap = new ConcurrentHashMap();
    private final Object listenerLock = new Object();
    private Uri mUri;
    ConcurrentHashMap<ObjectId, List<IListener>> manageObjectMap = new ConcurrentHashMap();
    private final Handler msgHandler;
    ConcurrentHashMap<ObjectContext, List<IListener>> objectContextMap = new ConcurrentHashMap();
    List<IListener> psChangeListeners = Collections.synchronizedList(new ArrayList());

    private class MessageHandler extends Handler {
        private AObjectContext objContext;

        MessageHandler(Looper looper, ObjectContext context) {
            super(looper);
            this.objContext = (AObjectContext) context;
        }

        public void handleMessage(Message msg) {
            AllChangeToThread allChange = msg.obj;
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

    NotifyClient(Looper looper, ObjectContext objectContext, Uri uri) {
        this.msgHandler = new MessageHandler(looper, objectContext);
        this.mUri = uri;
    }

    void registerListener(Uri uri, IListener listener) {
        if (uri == null || listener == null) {
            throw new ODMFIllegalArgumentException("Uri or IListener should not be null");
        } else if (uri.toString().equals(this.mUri.toString())) {
            synchronized (this.listenerLock) {
                if (!this.psChangeListeners.contains(listener)) {
                    this.psChangeListeners.add(listener);
                }
            }
        } else {
            throw new ODMFIllegalArgumentException("Uri error!!");
        }
    }

    void registerListener(ObjectContext objectContext, IListener listener) {
        List<IListener> objContextMapListener;
        if (objectContext == null || listener == null) {
            throw new ODMFIllegalArgumentException("ObjectContext or IListener should not be null");
        } else if (this.objectContextMap.containsKey(objectContext)) {
            objContextMapListener = (List) this.objectContextMap.get(objectContext);
            synchronized (this.listenerLock) {
                if (!objContextMapListener.contains(listener)) {
                    objContextMapListener.add(listener);
                }
            }
        } else {
            objContextMapListener = new ArrayList();
            synchronized (this.listenerLock) {
                objContextMapListener.add(listener);
            }
            this.objectContextMap.put(objectContext, objContextMapListener);
        }
    }

    void registerListener(String entityName, IListener listener) {
        if (entityName == null || listener == null) {
            throw new ODMFIllegalArgumentException("EntityName or IListener should not be null");
        } else if (this.entityMap.containsKey(entityName)) {
            List<IListener> entityMapListener = (List) this.entityMap.get(entityName);
            synchronized (this.listenerLock) {
                if (!entityMapListener.contains(listener)) {
                    entityMapListener.add(listener);
                }
            }
        } else {
            List<IListener> entityListener = new ArrayList();
            synchronized (this.listenerLock) {
                entityListener.add(listener);
            }
            this.entityMap.put(entityName, entityListener);
        }
    }

    void registerListener(ManagedObject managedObject, IListener listener) {
        if (managedObject == null || listener == null) {
            throw new ODMFIllegalArgumentException("ManageObject or IListener should not be null");
        }
        ObjectId objID = managedObject.getObjectId();
        if (this.manageObjectMap.containsKey(objID)) {
            List<IListener> objMapListener = (List) this.manageObjectMap.get(objID);
            synchronized (this.listenerLock) {
                if (!objMapListener.contains(listener)) {
                    objMapListener.add(listener);
                }
            }
            return;
        }
        List<IListener> entityListener = new ArrayList();
        synchronized (this.listenerLock) {
            entityListener.add(listener);
        }
        this.manageObjectMap.put(objID, entityListener);
    }

    void unregisterListener(Uri uri, IListener listener) {
        if (uri == null || listener == null) {
            throw new ODMFIllegalArgumentException("Uri or IListener should not be null");
        } else if (uri.toString().equals(this.mUri.toString())) {
            synchronized (this.listenerLock) {
                if (this.psChangeListeners.contains(listener)) {
                    this.psChangeListeners.remove(listener);
                }
            }
        } else {
            throw new ODMFIllegalArgumentException("Uri error!!");
        }
    }

    void unregisterListener(ObjectContext objectContext, IListener listener) {
        if (objectContext == null || listener == null) {
            throw new ODMFIllegalArgumentException("ManageObject or IListener should not be null");
        } else if (this.objectContextMap.containsKey(objectContext)) {
            List<IListener> objContextMapListener = (List) this.objectContextMap.get(objectContext);
            synchronized (this.listenerLock) {
                if (objContextMapListener.contains(listener)) {
                    objContextMapListener.remove(listener);
                    if (objContextMapListener.size() == 0) {
                        this.objectContextMap.remove(objectContext);
                    }
                }
            }
        }
    }

    void unregisterListener(String entityName, IListener listener) {
        if (entityName == null || listener == null) {
            throw new ODMFIllegalArgumentException("EntityName or IListener should not be null");
        } else if (this.entityMap.containsKey(entityName)) {
            List<IListener> entityMapListener = (List) this.entityMap.get(entityName);
            synchronized (this.listenerLock) {
                if (entityMapListener.contains(listener)) {
                    entityMapListener.remove(listener);
                    if (entityMapListener.size() == 0) {
                        this.entityMap.remove(entityName);
                    }
                }
            }
        }
    }

    void unregisterListener(ManagedObject manageObject, IListener listener) {
        if (manageObject == null || listener == null) {
            throw new ODMFIllegalArgumentException("ManageObject or IListener should not be null");
        }
        ObjectId objID = manageObject.getObjectId();
        if (this.manageObjectMap.containsKey(objID)) {
            List<IListener> objMapListener = (List) this.manageObjectMap.get(objID);
            synchronized (this.listenerLock) {
                if (objMapListener.contains(listener)) {
                    objMapListener.remove(listener);
                    if (objMapListener.size() == 0) {
                        this.manageObjectMap.remove(objID);
                    }
                }
            }
        }
    }

    boolean hasPsListener() {
        return this.psChangeListeners.size() > 0;
    }

    boolean hasObjectContextListener() {
        return this.objectContextMap.size() > 0;
    }

    boolean hasEntityListener() {
        return this.entityMap.size() > 0;
    }

    boolean hasManageObjListener() {
        return this.manageObjectMap.size() > 0;
    }

    boolean hasPsListener(String uriString) {
        if (uriString == null || !uriString.equals(this.mUri.toString()) || this.psChangeListeners.size() <= 0) {
            return false;
        }
        return true;
    }

    boolean hasObjectContextListener(ObjectContext ctx) {
        return this.objectContextMap.containsKey(ctx);
    }

    boolean hasEntityListener(String entityName) {
        return this.entityMap.containsKey(entityName);
    }

    boolean hasManageObjListener(ObjectId id) {
        return this.manageObjectMap.containsKey(id);
    }

    void sendMessage(int message, AllChangeToThread allChange) {
        Message msg = this.msgHandler.obtainMessage(message);
        msg.obj = allChange;
        this.msgHandler.sendMessage(msg);
    }

    private void handleChangedMessage(AObjectContext objContext, AllChangeToThread allChange) {
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
        for (Entry<ObjectId, List<IListener>> entry : this.manageObjectMap.entrySet()) {
            ObjectId objID = (ObjectId) entry.getKey();
            List<IListener> copyListener = new ArrayList();
            synchronized (this.listenerLock) {
                copyList((List) entry.getValue(), copyListener);
            }
            AllChangeToTarget subManageObjAllChange = (AllChangeToTarget) manageObjAllChange.get(objID);
            if (subManageObjAllChange != null) {
                int size = copyListener.size();
                for (int i = 0; i < size; i++) {
                    ((IListener) copyListener.get(i)).onObjectsChanged(changeContext, subManageObjAllChange);
                }
            }
            copyListener.clear();
        }
    }

    private void handleChangedMessageToEntityListener(AllChangeToThread allChange, ObjectContext changeContext) {
        ArrayMap<String, AllChangeToTarget> entityAllChange = allChange.getEntityMap();
        for (Entry<String, List<IListener>> entry : this.entityMap.entrySet()) {
            String entityName = (String) entry.getKey();
            List<IListener> copyListener = new ArrayList();
            synchronized (this.listenerLock) {
                copyList((List) entry.getValue(), copyListener);
            }
            AllChangeToTarget subEntityAllChange = (AllChangeToTarget) entityAllChange.get(entityName);
            if (subEntityAllChange != null) {
                int size = copyListener.size();
                for (int i = 0; i < size; i++) {
                    ((IListener) copyListener.get(i)).onObjectsChanged(changeContext, subEntityAllChange);
                }
            }
            copyListener.clear();
        }
    }

    private void handleChangedMessageToContextListener(AllChangeToThread allChange, ObjectContext changeContext) {
        ArrayMap<ObjectContext, AllChangeToTarget> objContextAllChange = allChange.getObjContextMap();
        for (Entry<ObjectContext, List<IListener>> entry : this.objectContextMap.entrySet()) {
            ObjectContext objCon = (ObjectContext) entry.getKey();
            List<IListener> copyListener = new ArrayList();
            synchronized (this.listenerLock) {
                copyList((List) entry.getValue(), copyListener);
            }
            AllChangeToTarget subObjConAllChange = (AllChangeToTarget) objContextAllChange.get(objCon);
            if (subObjConAllChange != null) {
                int size = copyListener.size();
                for (int i = 0; i < size; i++) {
                    ((IListener) copyListener.get(i)).onObjectsChanged(changeContext, subObjConAllChange);
                }
            }
            copyListener.clear();
        }
    }

    private void handleChangedMessageToPsListener(AllChangeToThread allChange, ObjectContext changeContext) {
        AllChangeToTarget subPsAllChange = (AllChangeToTarget) allChange.getPsMap().get(this.mUri.toString());
        List<IListener> copyListener = new ArrayList();
        synchronized (this.listenerLock) {
            copyList(this.psChangeListeners, copyListener);
        }
        if (subPsAllChange != null) {
            for (IListener commListener : copyListener) {
                commListener.onObjectsChanged(changeContext, subPsAllChange);
            }
        }
        copyListener.clear();
    }

    private void handleEntityClearMessage(AObjectContext objContext, AllChangeToThread deleteMessage) {
        ObjectContext deleteContext = deleteMessage.getChangeContext();
        List<IListener> copyListener = new ArrayList();
        synchronized (this.listenerLock) {
            copyList(this.psChangeListeners, copyListener);
        }
        if (objContext.getNotifyClient().hasPsListener()) {
            for (IListener commListener : copyListener) {
                commListener.onObjectsChanged(deleteContext, new AllChangeToTargetImpl(true));
            }
        }
        copyListener.clear();
        if (objContext.getNotifyClient().hasEntityListener(deleteMessage.getEntityName())) {
            for (Entry<String, List<IListener>> entry : this.entityMap.entrySet()) {
                String entityName = (String) entry.getKey();
                List<IListener> copyEntityListener = new ArrayList();
                synchronized (this.listenerLock) {
                    copyList((List) entry.getValue(), copyEntityListener);
                }
                if (deleteMessage.getEntityName().equals(entityName)) {
                    int size = copyEntityListener.size();
                    for (int i = 0; i < size; i++) {
                        ((IListener) copyEntityListener.get(i)).onObjectsChanged(deleteContext, new AllChangeToTargetImpl(true));
                    }
                }
                copyEntityListener.clear();
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
