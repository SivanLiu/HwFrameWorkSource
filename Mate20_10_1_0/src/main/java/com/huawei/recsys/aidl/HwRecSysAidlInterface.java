package com.huawei.recsys.aidl;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
import android.service.notification.StatusBarNotification;
import com.huawei.recsys.aidl.IHwRecSysCallBack;
import java.util.List;

public interface HwRecSysAidlInterface extends IInterface {
    List<String> clsRuleManger(String str, String str2, String str3, int i) throws RemoteException;

    void configFile(IHwRecSysCallBack iHwRecSysCallBack, String str, int i) throws RemoteException;

    String doNotificationCollect(StatusBarNotification statusBarNotification) throws RemoteException;

    String getCurrentScene() throws RemoteException;

    void getInstantAppRecommendation(IHwRecSysCallBack iHwRecSysCallBack) throws RemoteException;

    void registerCallBack(IHwRecSysCallBack iHwRecSysCallBack, String str) throws RemoteException;

    void requestRecRes(IHwRecSysCallBack iHwRecSysCallBack, String str) throws RemoteException;

    int ruleManager(String str, String str2, int i) throws RemoteException;

    void setClickRecFeedBack(int i, int i2) throws RemoteException;

    void setReportDirectService(int i, int i2) throws RemoteException;

    void unregisterCallBack(IHwRecSysCallBack iHwRecSysCallBack, String str) throws RemoteException;

    public static class Default implements HwRecSysAidlInterface {
        @Override // com.huawei.recsys.aidl.HwRecSysAidlInterface
        public void getInstantAppRecommendation(IHwRecSysCallBack callback) throws RemoteException {
        }

        @Override // com.huawei.recsys.aidl.HwRecSysAidlInterface
        public String getCurrentScene() throws RemoteException {
            return null;
        }

        @Override // com.huawei.recsys.aidl.HwRecSysAidlInterface
        public void setReportDirectService(int type, int value) throws RemoteException {
        }

        @Override // com.huawei.recsys.aidl.HwRecSysAidlInterface
        public void setClickRecFeedBack(int id, int serviceType) throws RemoteException {
        }

        @Override // com.huawei.recsys.aidl.HwRecSysAidlInterface
        public void configFile(IHwRecSysCallBack hwRecSysCallBack, String path, int type) throws RemoteException {
        }

        @Override // com.huawei.recsys.aidl.HwRecSysAidlInterface
        public void requestRecRes(IHwRecSysCallBack callback, String jobName) throws RemoteException {
        }

        @Override // com.huawei.recsys.aidl.HwRecSysAidlInterface
        public int ruleManager(String jobName, String rule, int op) throws RemoteException {
            return 0;
        }

        @Override // com.huawei.recsys.aidl.HwRecSysAidlInterface
        public void registerCallBack(IHwRecSysCallBack callback, String packageName) throws RemoteException {
        }

        @Override // com.huawei.recsys.aidl.HwRecSysAidlInterface
        public void unregisterCallBack(IHwRecSysCallBack callback, String packageName) throws RemoteException {
        }

        @Override // com.huawei.recsys.aidl.HwRecSysAidlInterface
        public String doNotificationCollect(StatusBarNotification statusBarNotification) throws RemoteException {
            return null;
        }

        @Override // com.huawei.recsys.aidl.HwRecSysAidlInterface
        public List<String> clsRuleManger(String businessname, String ruleName, String key, int operator) throws RemoteException {
            return null;
        }

        public IBinder asBinder() {
            return null;
        }
    }

    public static abstract class Stub extends Binder implements HwRecSysAidlInterface {
        private static final String DESCRIPTOR = "com.huawei.recsys.aidl.HwRecSysAidlInterface";
        static final int TRANSACTION_clsRuleManger = 11;
        static final int TRANSACTION_configFile = 5;
        static final int TRANSACTION_doNotificationCollect = 10;
        static final int TRANSACTION_getCurrentScene = 2;
        static final int TRANSACTION_getInstantAppRecommendation = 1;
        static final int TRANSACTION_registerCallBack = 8;
        static final int TRANSACTION_requestRecRes = 6;
        static final int TRANSACTION_ruleManager = 7;
        static final int TRANSACTION_setClickRecFeedBack = 4;
        static final int TRANSACTION_setReportDirectService = 3;
        static final int TRANSACTION_unregisterCallBack = 9;

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static HwRecSysAidlInterface asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin == null || !(iin instanceof HwRecSysAidlInterface)) {
                return new Proxy(obj);
            }
            return (HwRecSysAidlInterface) iin;
        }

        public IBinder asBinder() {
            return this;
        }

        @Override // android.os.Binder
        public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            StatusBarNotification _arg0;
            if (code != 1598968902) {
                switch (code) {
                    case 1:
                        data.enforceInterface(DESCRIPTOR);
                        getInstantAppRecommendation(IHwRecSysCallBack.Stub.asInterface(data.readStrongBinder()));
                        reply.writeNoException();
                        return true;
                    case 2:
                        data.enforceInterface(DESCRIPTOR);
                        String _result = getCurrentScene();
                        reply.writeNoException();
                        reply.writeString(_result);
                        return true;
                    case 3:
                        data.enforceInterface(DESCRIPTOR);
                        setReportDirectService(data.readInt(), data.readInt());
                        reply.writeNoException();
                        return true;
                    case 4:
                        data.enforceInterface(DESCRIPTOR);
                        setClickRecFeedBack(data.readInt(), data.readInt());
                        reply.writeNoException();
                        return true;
                    case 5:
                        data.enforceInterface(DESCRIPTOR);
                        configFile(IHwRecSysCallBack.Stub.asInterface(data.readStrongBinder()), data.readString(), data.readInt());
                        reply.writeNoException();
                        return true;
                    case 6:
                        data.enforceInterface(DESCRIPTOR);
                        requestRecRes(IHwRecSysCallBack.Stub.asInterface(data.readStrongBinder()), data.readString());
                        reply.writeNoException();
                        return true;
                    case 7:
                        data.enforceInterface(DESCRIPTOR);
                        int _result2 = ruleManager(data.readString(), data.readString(), data.readInt());
                        reply.writeNoException();
                        reply.writeInt(_result2);
                        return true;
                    case 8:
                        data.enforceInterface(DESCRIPTOR);
                        registerCallBack(IHwRecSysCallBack.Stub.asInterface(data.readStrongBinder()), data.readString());
                        reply.writeNoException();
                        return true;
                    case 9:
                        data.enforceInterface(DESCRIPTOR);
                        unregisterCallBack(IHwRecSysCallBack.Stub.asInterface(data.readStrongBinder()), data.readString());
                        reply.writeNoException();
                        return true;
                    case 10:
                        data.enforceInterface(DESCRIPTOR);
                        if (data.readInt() != 0) {
                            _arg0 = (StatusBarNotification) StatusBarNotification.CREATOR.createFromParcel(data);
                        } else {
                            _arg0 = null;
                        }
                        String _result3 = doNotificationCollect(_arg0);
                        reply.writeNoException();
                        reply.writeString(_result3);
                        return true;
                    case 11:
                        data.enforceInterface(DESCRIPTOR);
                        List<String> _result4 = clsRuleManger(data.readString(), data.readString(), data.readString(), data.readInt());
                        reply.writeNoException();
                        reply.writeStringList(_result4);
                        return true;
                    default:
                        return super.onTransact(code, data, reply, flags);
                }
            } else {
                reply.writeString(DESCRIPTOR);
                return true;
            }
        }

        private static class Proxy implements HwRecSysAidlInterface {
            public static HwRecSysAidlInterface sDefaultImpl;
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

            @Override // com.huawei.recsys.aidl.HwRecSysAidlInterface
            public void getInstantAppRecommendation(IHwRecSysCallBack callback) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(callback != null ? callback.asBinder() : null);
                    if (this.mRemote.transact(1, _data, _reply, 0) || Stub.getDefaultImpl() == null) {
                        _reply.readException();
                        _reply.recycle();
                        _data.recycle();
                        return;
                    }
                    Stub.getDefaultImpl().getInstantAppRecommendation(callback);
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.huawei.recsys.aidl.HwRecSysAidlInterface
            public String getCurrentScene() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (!this.mRemote.transact(2, _data, _reply, 0) && Stub.getDefaultImpl() != null) {
                        return Stub.getDefaultImpl().getCurrentScene();
                    }
                    _reply.readException();
                    String _result = _reply.readString();
                    _reply.recycle();
                    _data.recycle();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.huawei.recsys.aidl.HwRecSysAidlInterface
            public void setReportDirectService(int type, int value) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(type);
                    _data.writeInt(value);
                    if (this.mRemote.transact(3, _data, _reply, 0) || Stub.getDefaultImpl() == null) {
                        _reply.readException();
                        _reply.recycle();
                        _data.recycle();
                        return;
                    }
                    Stub.getDefaultImpl().setReportDirectService(type, value);
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.huawei.recsys.aidl.HwRecSysAidlInterface
            public void setClickRecFeedBack(int id, int serviceType) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(id);
                    _data.writeInt(serviceType);
                    if (this.mRemote.transact(4, _data, _reply, 0) || Stub.getDefaultImpl() == null) {
                        _reply.readException();
                        _reply.recycle();
                        _data.recycle();
                        return;
                    }
                    Stub.getDefaultImpl().setClickRecFeedBack(id, serviceType);
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.huawei.recsys.aidl.HwRecSysAidlInterface
            public void configFile(IHwRecSysCallBack hwRecSysCallBack, String path, int type) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(hwRecSysCallBack != null ? hwRecSysCallBack.asBinder() : null);
                    _data.writeString(path);
                    _data.writeInt(type);
                    if (this.mRemote.transact(5, _data, _reply, 0) || Stub.getDefaultImpl() == null) {
                        _reply.readException();
                        _reply.recycle();
                        _data.recycle();
                        return;
                    }
                    Stub.getDefaultImpl().configFile(hwRecSysCallBack, path, type);
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.huawei.recsys.aidl.HwRecSysAidlInterface
            public void requestRecRes(IHwRecSysCallBack callback, String jobName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(callback != null ? callback.asBinder() : null);
                    _data.writeString(jobName);
                    if (this.mRemote.transact(6, _data, _reply, 0) || Stub.getDefaultImpl() == null) {
                        _reply.readException();
                        _reply.recycle();
                        _data.recycle();
                        return;
                    }
                    Stub.getDefaultImpl().requestRecRes(callback, jobName);
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.huawei.recsys.aidl.HwRecSysAidlInterface
            public int ruleManager(String jobName, String rule, int op) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(jobName);
                    _data.writeString(rule);
                    _data.writeInt(op);
                    if (!this.mRemote.transact(7, _data, _reply, 0) && Stub.getDefaultImpl() != null) {
                        return Stub.getDefaultImpl().ruleManager(jobName, rule, op);
                    }
                    _reply.readException();
                    int _result = _reply.readInt();
                    _reply.recycle();
                    _data.recycle();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.huawei.recsys.aidl.HwRecSysAidlInterface
            public void registerCallBack(IHwRecSysCallBack callback, String packageName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(callback != null ? callback.asBinder() : null);
                    _data.writeString(packageName);
                    if (this.mRemote.transact(8, _data, _reply, 0) || Stub.getDefaultImpl() == null) {
                        _reply.readException();
                        _reply.recycle();
                        _data.recycle();
                        return;
                    }
                    Stub.getDefaultImpl().registerCallBack(callback, packageName);
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.huawei.recsys.aidl.HwRecSysAidlInterface
            public void unregisterCallBack(IHwRecSysCallBack callback, String packageName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(callback != null ? callback.asBinder() : null);
                    _data.writeString(packageName);
                    if (this.mRemote.transact(9, _data, _reply, 0) || Stub.getDefaultImpl() == null) {
                        _reply.readException();
                        _reply.recycle();
                        _data.recycle();
                        return;
                    }
                    Stub.getDefaultImpl().unregisterCallBack(callback, packageName);
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.huawei.recsys.aidl.HwRecSysAidlInterface
            public String doNotificationCollect(StatusBarNotification statusBarNotification) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (statusBarNotification != null) {
                        _data.writeInt(1);
                        statusBarNotification.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    if (!this.mRemote.transact(10, _data, _reply, 0) && Stub.getDefaultImpl() != null) {
                        return Stub.getDefaultImpl().doNotificationCollect(statusBarNotification);
                    }
                    _reply.readException();
                    String _result = _reply.readString();
                    _reply.recycle();
                    _data.recycle();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.huawei.recsys.aidl.HwRecSysAidlInterface
            public List<String> clsRuleManger(String businessname, String ruleName, String key, int operator) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(businessname);
                    _data.writeString(ruleName);
                    _data.writeString(key);
                    _data.writeInt(operator);
                    if (!this.mRemote.transact(11, _data, _reply, 0) && Stub.getDefaultImpl() != null) {
                        return Stub.getDefaultImpl().clsRuleManger(businessname, ruleName, key, operator);
                    }
                    _reply.readException();
                    List<String> _result = _reply.createStringArrayList();
                    _reply.recycle();
                    _data.recycle();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }
        }

        public static boolean setDefaultImpl(HwRecSysAidlInterface impl) {
            if (Proxy.sDefaultImpl != null || impl == null) {
                return false;
            }
            Proxy.sDefaultImpl = impl;
            return true;
        }

        public static HwRecSysAidlInterface getDefaultImpl() {
            return Proxy.sDefaultImpl;
        }
    }
}
