package com.huawei.nb.service;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
import com.huawei.nb.callback.IDeleteCallback;
import com.huawei.nb.callback.IDeleteResInfoCallBack;
import com.huawei.nb.callback.IFetchCallback;
import com.huawei.nb.callback.IInsertCallback;
import com.huawei.nb.callback.ISendEventCallback;
import com.huawei.nb.callback.ISubscribeCallback;
import com.huawei.nb.callback.IUpdateCallback;
import com.huawei.nb.callback.IUpdatePackageCallBack;
import com.huawei.nb.callback.IUpdatePackageCheckCallBack;
import com.huawei.nb.container.ObjectContainer;
import com.huawei.nb.notification.IModelObserver;
import com.huawei.nb.notification.ModelObserverInfo;
import com.huawei.nb.query.QueryContainer;
import com.huawei.nb.query.bulkcursor.BulkCursorDescriptor;

public interface IDataServiceCall extends IInterface {

    public static abstract class Stub extends Binder implements IDataServiceCall {
        private static final String DESCRIPTOR = "com.huawei.nb.service.IDataServiceCall";
        static final int TRANSACTION_batchImport = 1;
        static final int TRANSACTION_clearUserData = 16;
        static final int TRANSACTION_deleteResInfoAgent = 22;
        static final int TRANSACTION_executeCursorQueryDirect = 7;
        static final int TRANSACTION_executeDelete = 5;
        static final int TRANSACTION_executeDeleteDirect = 9;
        static final int TRANSACTION_executeInsert = 3;
        static final int TRANSACTION_executeInsertDirect = 19;
        static final int TRANSACTION_executeInsertEfficiently = 25;
        static final int TRANSACTION_executeQuery = 2;
        static final int TRANSACTION_executeQueryDirect = 6;
        static final int TRANSACTION_executeUpdate = 4;
        static final int TRANSACTION_executeUpdateDirect = 8;
        static final int TRANSACTION_fetchRecommendations = 13;
        static final int TRANSACTION_getDatabaseVersion = 15;
        static final int TRANSACTION_getScheduledJobs = 14;
        static final int TRANSACTION_handleDataLifeCycleConfig = 26;
        static final int TRANSACTION_insertResInfoAgent = 23;
        static final int TRANSACTION_registerModelObserver = 10;
        static final int TRANSACTION_requestAiModel = 17;
        static final int TRANSACTION_requestAiModelAsync = 18;
        static final int TRANSACTION_sendEvent = 12;
        static final int TRANSACTION_unregisterModelObserver = 11;
        static final int TRANSACTION_updatePackageAgent = 21;
        static final int TRANSACTION_updatePackageCheckAgent = 20;
        static final int TRANSACTION_updateResInfoAgent = 24;

        private static class Proxy implements IDataServiceCall {
            private IBinder mRemote;

            Proxy(IBinder remote) {
                this.mRemote = remote;
            }

            public IBinder asBinder() {
                return this.mRemote;
            }

            public String getInterfaceDescriptor() {
                return Stub.DESCRIPTOR;
            }

            public int batchImport(String database, String table, String dataFile, int dataFmt) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(database);
                    _data.writeString(table);
                    _data.writeString(dataFile);
                    _data.writeInt(dataFmt);
                    this.mRemote.transact(1, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int executeQuery(QueryContainer query, IFetchCallback cb) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (query != null) {
                        _data.writeInt(1);
                        query.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeStrongBinder(cb != null ? cb.asBinder() : null);
                    this.mRemote.transact(2, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int executeInsert(ObjectContainer oc, IInsertCallback cb) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (oc != null) {
                        _data.writeInt(1);
                        oc.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeStrongBinder(cb != null ? cb.asBinder() : null);
                    this.mRemote.transact(3, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int executeUpdate(ObjectContainer oc, IUpdateCallback cb) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (oc != null) {
                        _data.writeInt(1);
                        oc.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeStrongBinder(cb != null ? cb.asBinder() : null);
                    this.mRemote.transact(4, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int executeDelete(ObjectContainer oc, boolean deleteAll, IDeleteCallback cb) throws RemoteException {
                int i = 1;
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (oc != null) {
                        _data.writeInt(1);
                        oc.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    if (!deleteAll) {
                        i = 0;
                    }
                    _data.writeInt(i);
                    _data.writeStrongBinder(cb != null ? cb.asBinder() : null);
                    this.mRemote.transact(5, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public ObjectContainer executeQueryDirect(QueryContainer query) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    ObjectContainer _result;
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (query != null) {
                        _data.writeInt(1);
                        query.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(6, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        _result = (ObjectContainer) ObjectContainer.CREATOR.createFromParcel(_reply);
                    } else {
                        _result = null;
                    }
                    _reply.recycle();
                    _data.recycle();
                    return _result;
                } catch (Throwable th) {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public BulkCursorDescriptor executeCursorQueryDirect(QueryContainer query) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    BulkCursorDescriptor _result;
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (query != null) {
                        _data.writeInt(1);
                        query.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(7, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        _result = (BulkCursorDescriptor) BulkCursorDescriptor.CREATOR.createFromParcel(_reply);
                    } else {
                        _result = null;
                    }
                    _reply.recycle();
                    _data.recycle();
                    return _result;
                } catch (Throwable th) {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int executeUpdateDirect(ObjectContainer oc) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (oc != null) {
                        _data.writeInt(1);
                        oc.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(8, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int executeDeleteDirect(ObjectContainer oc, boolean deleteAll) throws RemoteException {
                int i = 1;
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (oc != null) {
                        _data.writeInt(1);
                        oc.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    if (!deleteAll) {
                        i = 0;
                    }
                    _data.writeInt(i);
                    this.mRemote.transact(9, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int registerModelObserver(ModelObserverInfo info, IModelObserver observer, ISubscribeCallback cb) throws RemoteException {
                IBinder iBinder = null;
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (info != null) {
                        _data.writeInt(1);
                        info.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeStrongBinder(observer != null ? observer.asBinder() : null);
                    if (cb != null) {
                        iBinder = cb.asBinder();
                    }
                    _data.writeStrongBinder(iBinder);
                    this.mRemote.transact(10, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int unregisterModelObserver(ModelObserverInfo info, IModelObserver observer, ISubscribeCallback cb) throws RemoteException {
                IBinder iBinder = null;
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (info != null) {
                        _data.writeInt(1);
                        info.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeStrongBinder(observer != null ? observer.asBinder() : null);
                    if (cb != null) {
                        iBinder = cb.asBinder();
                    }
                    _data.writeStrongBinder(iBinder);
                    this.mRemote.transact(11, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int sendEvent(ObjectContainer oc, ISendEventCallback cb) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (oc != null) {
                        _data.writeInt(1);
                        oc.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeStrongBinder(cb != null ? cb.asBinder() : null);
                    this.mRemote.transact(12, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public ObjectContainer fetchRecommendations(String businessName, String ruleName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    ObjectContainer _result;
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(businessName);
                    _data.writeString(ruleName);
                    this.mRemote.transact(13, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        _result = (ObjectContainer) ObjectContainer.CREATOR.createFromParcel(_reply);
                    } else {
                        _result = null;
                    }
                    _reply.recycle();
                    _data.recycle();
                    return _result;
                } catch (Throwable th) {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public ObjectContainer getScheduledJobs() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    ObjectContainer _result;
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(14, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        _result = (ObjectContainer) ObjectContainer.CREATOR.createFromParcel(_reply);
                    } else {
                        _result = null;
                    }
                    _reply.recycle();
                    _data.recycle();
                    return _result;
                } catch (Throwable th) {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public String getDatabaseVersion(String databaseName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(databaseName);
                    this.mRemote.transact(15, _data, _reply, 0);
                    _reply.readException();
                    String _result = _reply.readString();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public boolean clearUserData(String databaseName, int type) throws RemoteException {
                boolean _result = false;
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(databaseName);
                    _data.writeInt(type);
                    this.mRemote.transact(16, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        _result = true;
                    }
                    _reply.recycle();
                    _data.recycle();
                    return _result;
                } catch (Throwable th) {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public ObjectContainer requestAiModel(ObjectContainer requestContainer) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    ObjectContainer _result;
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (requestContainer != null) {
                        _data.writeInt(1);
                        requestContainer.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(17, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        _result = (ObjectContainer) ObjectContainer.CREATOR.createFromParcel(_reply);
                    } else {
                        _result = null;
                    }
                    _reply.recycle();
                    _data.recycle();
                    return _result;
                } catch (Throwable th) {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int requestAiModelAsync(ObjectContainer requestContainer, IFetchCallback cb) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (requestContainer != null) {
                        _data.writeInt(1);
                        requestContainer.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeStrongBinder(cb != null ? cb.asBinder() : null);
                    this.mRemote.transact(18, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public ObjectContainer executeInsertDirect(ObjectContainer oc) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    ObjectContainer _result;
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (oc != null) {
                        _data.writeInt(1);
                        oc.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(19, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        _result = (ObjectContainer) ObjectContainer.CREATOR.createFromParcel(_reply);
                    } else {
                        _result = null;
                    }
                    _reply.recycle();
                    _data.recycle();
                    return _result;
                } catch (Throwable th) {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void updatePackageCheckAgent(ObjectContainer resources, IUpdatePackageCheckCallBack cb) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (resources != null) {
                        _data.writeInt(1);
                        resources.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeStrongBinder(cb != null ? cb.asBinder() : null);
                    this.mRemote.transact(20, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void updatePackageAgent(ObjectContainer resources, IUpdatePackageCallBack cb, long refreshInterval, long refreshBucketSize, boolean wifiOnly) throws RemoteException {
                int i = 1;
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (resources != null) {
                        _data.writeInt(1);
                        resources.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeStrongBinder(cb != null ? cb.asBinder() : null);
                    _data.writeLong(refreshInterval);
                    _data.writeLong(refreshBucketSize);
                    if (!wifiOnly) {
                        i = 0;
                    }
                    _data.writeInt(i);
                    this.mRemote.transact(21, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void deleteResInfoAgent(ObjectContainer resources, IDeleteResInfoCallBack cb) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (resources != null) {
                        _data.writeInt(1);
                        resources.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeStrongBinder(cb != null ? cb.asBinder() : null);
                    this.mRemote.transact(22, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public ObjectContainer insertResInfoAgent(ObjectContainer resource) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    ObjectContainer _result;
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (resource != null) {
                        _data.writeInt(1);
                        resource.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(Stub.TRANSACTION_insertResInfoAgent, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        _result = (ObjectContainer) ObjectContainer.CREATOR.createFromParcel(_reply);
                    } else {
                        _result = null;
                    }
                    _reply.recycle();
                    _data.recycle();
                    return _result;
                } catch (Throwable th) {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public boolean updateResInfoAgent(ObjectContainer resource) throws RemoteException {
                boolean _result = true;
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (resource != null) {
                        _data.writeInt(1);
                        resource.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(Stub.TRANSACTION_updateResInfoAgent, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() == 0) {
                        _result = false;
                    }
                    _reply.recycle();
                    _data.recycle();
                    return _result;
                } catch (Throwable th) {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int executeInsertEfficiently(ObjectContainer oc) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (oc != null) {
                        _data.writeInt(1);
                        oc.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(Stub.TRANSACTION_executeInsertEfficiently, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public ObjectContainer handleDataLifeCycleConfig(int actionCode, ObjectContainer resources) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    ObjectContainer _result;
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(actionCode);
                    if (resources != null) {
                        _data.writeInt(1);
                        resources.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(Stub.TRANSACTION_handleDataLifeCycleConfig, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        _result = (ObjectContainer) ObjectContainer.CREATOR.createFromParcel(_reply);
                    } else {
                        _result = null;
                    }
                    _reply.recycle();
                    _data.recycle();
                    return _result;
                } catch (Throwable th) {
                    _reply.recycle();
                    _data.recycle();
                }
            }
        }

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static IDataServiceCall asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin == null || !(iin instanceof IDataServiceCall)) {
                return new Proxy(obj);
            }
            return (IDataServiceCall) iin;
        }

        public IBinder asBinder() {
            return this;
        }

        public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            int i = 0;
            int _result;
            QueryContainer _arg0;
            ObjectContainer _arg02;
            boolean _arg1;
            ObjectContainer _result2;
            ModelObserverInfo _arg03;
            boolean _result3;
            switch (code) {
                case 1:
                    data.enforceInterface(DESCRIPTOR);
                    _result = batchImport(data.readString(), data.readString(), data.readString(), data.readInt());
                    reply.writeNoException();
                    reply.writeInt(_result);
                    return true;
                case 2:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        _arg0 = (QueryContainer) QueryContainer.CREATOR.createFromParcel(data);
                    } else {
                        _arg0 = null;
                    }
                    _result = executeQuery(_arg0, com.huawei.nb.callback.IFetchCallback.Stub.asInterface(data.readStrongBinder()));
                    reply.writeNoException();
                    reply.writeInt(_result);
                    return true;
                case 3:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        _arg02 = (ObjectContainer) ObjectContainer.CREATOR.createFromParcel(data);
                    } else {
                        _arg02 = null;
                    }
                    _result = executeInsert(_arg02, com.huawei.nb.callback.IInsertCallback.Stub.asInterface(data.readStrongBinder()));
                    reply.writeNoException();
                    reply.writeInt(_result);
                    return true;
                case 4:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        _arg02 = (ObjectContainer) ObjectContainer.CREATOR.createFromParcel(data);
                    } else {
                        _arg02 = null;
                    }
                    _result = executeUpdate(_arg02, com.huawei.nb.callback.IUpdateCallback.Stub.asInterface(data.readStrongBinder()));
                    reply.writeNoException();
                    reply.writeInt(_result);
                    return true;
                case 5:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        _arg02 = (ObjectContainer) ObjectContainer.CREATOR.createFromParcel(data);
                    } else {
                        _arg02 = null;
                    }
                    if (data.readInt() != 0) {
                        _arg1 = true;
                    } else {
                        _arg1 = false;
                    }
                    _result = executeDelete(_arg02, _arg1, com.huawei.nb.callback.IDeleteCallback.Stub.asInterface(data.readStrongBinder()));
                    reply.writeNoException();
                    reply.writeInt(_result);
                    return true;
                case 6:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        _arg0 = (QueryContainer) QueryContainer.CREATOR.createFromParcel(data);
                    } else {
                        _arg0 = null;
                    }
                    _result2 = executeQueryDirect(_arg0);
                    reply.writeNoException();
                    if (_result2 != null) {
                        reply.writeInt(1);
                        _result2.writeToParcel(reply, 1);
                        return true;
                    }
                    reply.writeInt(0);
                    return true;
                case 7:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        _arg0 = (QueryContainer) QueryContainer.CREATOR.createFromParcel(data);
                    } else {
                        _arg0 = null;
                    }
                    BulkCursorDescriptor _result4 = executeCursorQueryDirect(_arg0);
                    reply.writeNoException();
                    if (_result4 != null) {
                        reply.writeInt(1);
                        _result4.writeToParcel(reply, 1);
                        return true;
                    }
                    reply.writeInt(0);
                    return true;
                case 8:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        _arg02 = (ObjectContainer) ObjectContainer.CREATOR.createFromParcel(data);
                    } else {
                        _arg02 = null;
                    }
                    _result = executeUpdateDirect(_arg02);
                    reply.writeNoException();
                    reply.writeInt(_result);
                    return true;
                case 9:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        _arg02 = (ObjectContainer) ObjectContainer.CREATOR.createFromParcel(data);
                    } else {
                        _arg02 = null;
                    }
                    if (data.readInt() != 0) {
                        _arg1 = true;
                    } else {
                        _arg1 = false;
                    }
                    _result = executeDeleteDirect(_arg02, _arg1);
                    reply.writeNoException();
                    reply.writeInt(_result);
                    return true;
                case 10:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        _arg03 = (ModelObserverInfo) ModelObserverInfo.CREATOR.createFromParcel(data);
                    } else {
                        _arg03 = null;
                    }
                    _result = registerModelObserver(_arg03, com.huawei.nb.notification.IModelObserver.Stub.asInterface(data.readStrongBinder()), com.huawei.nb.callback.ISubscribeCallback.Stub.asInterface(data.readStrongBinder()));
                    reply.writeNoException();
                    reply.writeInt(_result);
                    return true;
                case 11:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        _arg03 = (ModelObserverInfo) ModelObserverInfo.CREATOR.createFromParcel(data);
                    } else {
                        _arg03 = null;
                    }
                    _result = unregisterModelObserver(_arg03, com.huawei.nb.notification.IModelObserver.Stub.asInterface(data.readStrongBinder()), com.huawei.nb.callback.ISubscribeCallback.Stub.asInterface(data.readStrongBinder()));
                    reply.writeNoException();
                    reply.writeInt(_result);
                    return true;
                case 12:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        _arg02 = (ObjectContainer) ObjectContainer.CREATOR.createFromParcel(data);
                    } else {
                        _arg02 = null;
                    }
                    _result = sendEvent(_arg02, com.huawei.nb.callback.ISendEventCallback.Stub.asInterface(data.readStrongBinder()));
                    reply.writeNoException();
                    reply.writeInt(_result);
                    return true;
                case 13:
                    data.enforceInterface(DESCRIPTOR);
                    _result2 = fetchRecommendations(data.readString(), data.readString());
                    reply.writeNoException();
                    if (_result2 != null) {
                        reply.writeInt(1);
                        _result2.writeToParcel(reply, 1);
                        return true;
                    }
                    reply.writeInt(0);
                    return true;
                case 14:
                    data.enforceInterface(DESCRIPTOR);
                    _result2 = getScheduledJobs();
                    reply.writeNoException();
                    if (_result2 != null) {
                        reply.writeInt(1);
                        _result2.writeToParcel(reply, 1);
                        return true;
                    }
                    reply.writeInt(0);
                    return true;
                case 15:
                    data.enforceInterface(DESCRIPTOR);
                    String _result5 = getDatabaseVersion(data.readString());
                    reply.writeNoException();
                    reply.writeString(_result5);
                    return true;
                case 16:
                    data.enforceInterface(DESCRIPTOR);
                    _result3 = clearUserData(data.readString(), data.readInt());
                    reply.writeNoException();
                    if (_result3) {
                        i = 1;
                    }
                    reply.writeInt(i);
                    return true;
                case 17:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        _arg02 = (ObjectContainer) ObjectContainer.CREATOR.createFromParcel(data);
                    } else {
                        _arg02 = null;
                    }
                    _result2 = requestAiModel(_arg02);
                    reply.writeNoException();
                    if (_result2 != null) {
                        reply.writeInt(1);
                        _result2.writeToParcel(reply, 1);
                        return true;
                    }
                    reply.writeInt(0);
                    return true;
                case 18:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        _arg02 = (ObjectContainer) ObjectContainer.CREATOR.createFromParcel(data);
                    } else {
                        _arg02 = null;
                    }
                    _result = requestAiModelAsync(_arg02, com.huawei.nb.callback.IFetchCallback.Stub.asInterface(data.readStrongBinder()));
                    reply.writeNoException();
                    reply.writeInt(_result);
                    return true;
                case 19:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        _arg02 = (ObjectContainer) ObjectContainer.CREATOR.createFromParcel(data);
                    } else {
                        _arg02 = null;
                    }
                    _result2 = executeInsertDirect(_arg02);
                    reply.writeNoException();
                    if (_result2 != null) {
                        reply.writeInt(1);
                        _result2.writeToParcel(reply, 1);
                        return true;
                    }
                    reply.writeInt(0);
                    return true;
                case 20:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        _arg02 = (ObjectContainer) ObjectContainer.CREATOR.createFromParcel(data);
                    } else {
                        _arg02 = null;
                    }
                    updatePackageCheckAgent(_arg02, com.huawei.nb.callback.IUpdatePackageCheckCallBack.Stub.asInterface(data.readStrongBinder()));
                    reply.writeNoException();
                    return true;
                case 21:
                    boolean _arg4;
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        _arg02 = (ObjectContainer) ObjectContainer.CREATOR.createFromParcel(data);
                    } else {
                        _arg02 = null;
                    }
                    IUpdatePackageCallBack _arg12 = com.huawei.nb.callback.IUpdatePackageCallBack.Stub.asInterface(data.readStrongBinder());
                    long _arg2 = data.readLong();
                    long _arg3 = data.readLong();
                    if (data.readInt() != 0) {
                        _arg4 = true;
                    } else {
                        _arg4 = false;
                    }
                    updatePackageAgent(_arg02, _arg12, _arg2, _arg3, _arg4);
                    reply.writeNoException();
                    return true;
                case 22:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        _arg02 = (ObjectContainer) ObjectContainer.CREATOR.createFromParcel(data);
                    } else {
                        _arg02 = null;
                    }
                    deleteResInfoAgent(_arg02, com.huawei.nb.callback.IDeleteResInfoCallBack.Stub.asInterface(data.readStrongBinder()));
                    reply.writeNoException();
                    return true;
                case TRANSACTION_insertResInfoAgent /*23*/:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        _arg02 = (ObjectContainer) ObjectContainer.CREATOR.createFromParcel(data);
                    } else {
                        _arg02 = null;
                    }
                    _result2 = insertResInfoAgent(_arg02);
                    reply.writeNoException();
                    if (_result2 != null) {
                        reply.writeInt(1);
                        _result2.writeToParcel(reply, 1);
                        return true;
                    }
                    reply.writeInt(0);
                    return true;
                case TRANSACTION_updateResInfoAgent /*24*/:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        _arg02 = (ObjectContainer) ObjectContainer.CREATOR.createFromParcel(data);
                    } else {
                        _arg02 = null;
                    }
                    _result3 = updateResInfoAgent(_arg02);
                    reply.writeNoException();
                    if (_result3) {
                        i = 1;
                    }
                    reply.writeInt(i);
                    return true;
                case TRANSACTION_executeInsertEfficiently /*25*/:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        _arg02 = (ObjectContainer) ObjectContainer.CREATOR.createFromParcel(data);
                    } else {
                        _arg02 = null;
                    }
                    _result = executeInsertEfficiently(_arg02);
                    reply.writeNoException();
                    reply.writeInt(_result);
                    return true;
                case TRANSACTION_handleDataLifeCycleConfig /*26*/:
                    ObjectContainer _arg13;
                    data.enforceInterface(DESCRIPTOR);
                    int _arg04 = data.readInt();
                    if (data.readInt() != 0) {
                        _arg13 = (ObjectContainer) ObjectContainer.CREATOR.createFromParcel(data);
                    } else {
                        _arg13 = null;
                    }
                    _result2 = handleDataLifeCycleConfig(_arg04, _arg13);
                    reply.writeNoException();
                    if (_result2 != null) {
                        reply.writeInt(1);
                        _result2.writeToParcel(reply, 1);
                        return true;
                    }
                    reply.writeInt(0);
                    return true;
                case 1598968902:
                    reply.writeString(DESCRIPTOR);
                    return true;
                default:
                    return super.onTransact(code, data, reply, flags);
            }
        }
    }

    int batchImport(String str, String str2, String str3, int i) throws RemoteException;

    boolean clearUserData(String str, int i) throws RemoteException;

    void deleteResInfoAgent(ObjectContainer objectContainer, IDeleteResInfoCallBack iDeleteResInfoCallBack) throws RemoteException;

    BulkCursorDescriptor executeCursorQueryDirect(QueryContainer queryContainer) throws RemoteException;

    int executeDelete(ObjectContainer objectContainer, boolean z, IDeleteCallback iDeleteCallback) throws RemoteException;

    int executeDeleteDirect(ObjectContainer objectContainer, boolean z) throws RemoteException;

    int executeInsert(ObjectContainer objectContainer, IInsertCallback iInsertCallback) throws RemoteException;

    ObjectContainer executeInsertDirect(ObjectContainer objectContainer) throws RemoteException;

    int executeInsertEfficiently(ObjectContainer objectContainer) throws RemoteException;

    int executeQuery(QueryContainer queryContainer, IFetchCallback iFetchCallback) throws RemoteException;

    ObjectContainer executeQueryDirect(QueryContainer queryContainer) throws RemoteException;

    int executeUpdate(ObjectContainer objectContainer, IUpdateCallback iUpdateCallback) throws RemoteException;

    int executeUpdateDirect(ObjectContainer objectContainer) throws RemoteException;

    ObjectContainer fetchRecommendations(String str, String str2) throws RemoteException;

    String getDatabaseVersion(String str) throws RemoteException;

    ObjectContainer getScheduledJobs() throws RemoteException;

    ObjectContainer handleDataLifeCycleConfig(int i, ObjectContainer objectContainer) throws RemoteException;

    ObjectContainer insertResInfoAgent(ObjectContainer objectContainer) throws RemoteException;

    int registerModelObserver(ModelObserverInfo modelObserverInfo, IModelObserver iModelObserver, ISubscribeCallback iSubscribeCallback) throws RemoteException;

    ObjectContainer requestAiModel(ObjectContainer objectContainer) throws RemoteException;

    int requestAiModelAsync(ObjectContainer objectContainer, IFetchCallback iFetchCallback) throws RemoteException;

    int sendEvent(ObjectContainer objectContainer, ISendEventCallback iSendEventCallback) throws RemoteException;

    int unregisterModelObserver(ModelObserverInfo modelObserverInfo, IModelObserver iModelObserver, ISubscribeCallback iSubscribeCallback) throws RemoteException;

    void updatePackageAgent(ObjectContainer objectContainer, IUpdatePackageCallBack iUpdatePackageCallBack, long j, long j2, boolean z) throws RemoteException;

    void updatePackageCheckAgent(ObjectContainer objectContainer, IUpdatePackageCheckCallBack iUpdatePackageCheckCallBack) throws RemoteException;

    boolean updateResInfoAgent(ObjectContainer objectContainer) throws RemoteException;
}
