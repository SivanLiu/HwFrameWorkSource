package com.huawei.recsys.aidl;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
import android.service.notification.StatusBarNotification;
import java.util.List;

public interface HwRecSysAidlInterface extends IInterface {

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

        private static class Proxy implements HwRecSysAidlInterface {
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

            public void getInstantAppRecommendation(IHwRecSysCallBack callback) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(callback != null ? callback.asBinder() : null);
                    this.mRemote.transact(1, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public String getCurrentScene() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(2, _data, _reply, 0);
                    _reply.readException();
                    String _result = _reply.readString();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void setReportDirectService(int type, int value) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(type);
                    _data.writeInt(value);
                    this.mRemote.transact(3, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void setClickRecFeedBack(int id, int serviceType) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(id);
                    _data.writeInt(serviceType);
                    this.mRemote.transact(4, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void configFile(IHwRecSysCallBack hwRecSysCallBack, String path, int type) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(hwRecSysCallBack != null ? hwRecSysCallBack.asBinder() : null);
                    _data.writeString(path);
                    _data.writeInt(type);
                    this.mRemote.transact(5, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void requestRecRes(IHwRecSysCallBack callback, String jobName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(callback != null ? callback.asBinder() : null);
                    _data.writeString(jobName);
                    this.mRemote.transact(6, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int ruleManager(String jobName, String rule, int op) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(jobName);
                    _data.writeString(rule);
                    _data.writeInt(op);
                    this.mRemote.transact(7, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void registerCallBack(IHwRecSysCallBack callback, String packageName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(callback != null ? callback.asBinder() : null);
                    _data.writeString(packageName);
                    this.mRemote.transact(8, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void unregisterCallBack(IHwRecSysCallBack callback, String packageName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(callback != null ? callback.asBinder() : null);
                    _data.writeString(packageName);
                    this.mRemote.transact(9, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

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
                    this.mRemote.transact(10, _data, _reply, 0);
                    _reply.readException();
                    String _result = _reply.readString();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public List<String> clsRuleManger(String businessname, String ruleName, String key, int operator) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(businessname);
                    _data.writeString(ruleName);
                    _data.writeString(key);
                    _data.writeInt(operator);
                    this.mRemote.transact(11, _data, _reply, 0);
                    _reply.readException();
                    List<String> _result = _reply.createStringArrayList();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }
        }

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

        public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            String descriptor = DESCRIPTOR;
            if (code != 1598968902) {
                switch (code) {
                    case 1:
                        data.enforceInterface(descriptor);
                        getInstantAppRecommendation(com.huawei.recsys.aidl.IHwRecSysCallBack.Stub.asInterface(data.readStrongBinder()));
                        reply.writeNoException();
                        return true;
                    case 2:
                        data.enforceInterface(descriptor);
                        String _result = getCurrentScene();
                        reply.writeNoException();
                        reply.writeString(_result);
                        return true;
                    case 3:
                        data.enforceInterface(descriptor);
                        setReportDirectService(data.readInt(), data.readInt());
                        reply.writeNoException();
                        return true;
                    case 4:
                        data.enforceInterface(descriptor);
                        setClickRecFeedBack(data.readInt(), data.readInt());
                        reply.writeNoException();
                        return true;
                    case 5:
                        data.enforceInterface(descriptor);
                        configFile(com.huawei.recsys.aidl.IHwRecSysCallBack.Stub.asInterface(data.readStrongBinder()), data.readString(), data.readInt());
                        reply.writeNoException();
                        return true;
                    case 6:
                        data.enforceInterface(descriptor);
                        requestRecRes(com.huawei.recsys.aidl.IHwRecSysCallBack.Stub.asInterface(data.readStrongBinder()), data.readString());
                        reply.writeNoException();
                        return true;
                    case 7:
                        data.enforceInterface(descriptor);
                        int _result2 = ruleManager(data.readString(), data.readString(), data.readInt());
                        reply.writeNoException();
                        reply.writeInt(_result2);
                        return true;
                    case 8:
                        data.enforceInterface(descriptor);
                        registerCallBack(com.huawei.recsys.aidl.IHwRecSysCallBack.Stub.asInterface(data.readStrongBinder()), data.readString());
                        reply.writeNoException();
                        return true;
                    case 9:
                        data.enforceInterface(descriptor);
                        unregisterCallBack(com.huawei.recsys.aidl.IHwRecSysCallBack.Stub.asInterface(data.readStrongBinder()), data.readString());
                        reply.writeNoException();
                        return true;
                    case 10:
                        StatusBarNotification _arg0;
                        data.enforceInterface(descriptor);
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
                        data.enforceInterface(descriptor);
                        List<String> _result4 = clsRuleManger(data.readString(), data.readString(), data.readString(), data.readInt());
                        reply.writeNoException();
                        reply.writeStringList(_result4);
                        return true;
                    default:
                        return super.onTransact(code, data, reply, flags);
                }
            }
            reply.writeString(descriptor);
            return true;
        }
    }

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
}
